/*
 * Copyright 2026 Switch Consulting (https://switch-consulting.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.switchconsulting.aintlistening.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Handles the downloading and extraction of speech model zip files.
 * Uses a single-thread executor for background processing and reports progress via callbacks.
 */
public class ModelDownloader {

    private static final String TAG = "ModelDownloader";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture;
    private volatile boolean isCancelled = false;

    /**
     * Downloads a model file from the specified URL and optionally extracts it.
     *
     * @param info          The model information.
     * @param targetBaseDir The directory where the model should be saved/extracted.
     * @param callback      The callback to receive status updates.
     */
    public void downloadAndExtract(@NonNull ModelInfo info, @NonNull File targetBaseDir, @NonNull ModelDownloadCallback callback) {
        isCancelled = false;
        Handler handler = new Handler(Looper.getMainLooper());

        currentFuture = executor.submit(() -> {
            Log.d(TAG, "Starting download task for: " + info.url);
            HttpURLConnection connection = null;
            File targetFile = new File(targetBaseDir, info.isZip ? "model_temp.zip" : info.name);
            try {
                String currentUrl = info.url;
                int redirectCount = 0;
                while (redirectCount < 5) {
                    if (isCancelled) throw new InterruptedException();
                    
                    URL url = new URL(currentUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", "AintListening-ModelDownloader");
                    
                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "URL: " + currentUrl + " -> Response: " + responseCode);

                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308) {
                        
                        currentUrl = connection.getHeaderField("Location");
                        redirectCount++;
                        connection.disconnect();
                        continue;
                    }
                    break;
                }

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Failed to connect or server returned error: " + 
                            connection.getResponseCode());
                }

                int fileLength = connection.getContentLength();
                Log.d(TAG, "File size: " + fileLength);

                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     OutputStream output = new FileOutputStream(targetFile)) {

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    int lastProgress = -1;
                    
                    while ((count = input.read(data)) != -1) {
                        if (isCancelled) throw new InterruptedException();
                        
                        total += count;
                        if (fileLength > 0) {
                            int progress = (int) (total * 100 / fileLength);
                            if (progress != lastProgress) {
                                lastProgress = progress;
                                handler.post(() -> callback.onProgress(progress));
                            }
                        }
                        output.write(data, 0, count);
                    }
                }

                if (isCancelled) throw new InterruptedException();

                // Check if it's a Git LFS pointer
                if (targetFile.length() < 500) {
                    try (Scanner scanner = new Scanner(targetFile)) {
                        if (scanner.hasNextLine() && scanner.nextLine().startsWith("version https://git-lfs")) {
                            throw new Exception("Downloaded file is a Git LFS pointer. Check LFS quota or URL.");
                        }
                    }
                }

                if (info.isZip) {
                    handler.post(callback::onExtracting);
                    extractZip(targetFile, targetBaseDir);
                    
                    if (!targetFile.delete()) {
                        Log.w(TAG, "Failed to delete temporary zip file: " + targetFile.getAbsolutePath());
                    }
                }

                if (isCancelled) throw new InterruptedException();
                handler.post(callback::onSuccess);

            } catch (InterruptedException e) {
                Log.d(TAG, "Download cancelled.");
                if (targetFile.exists() && !targetFile.delete()) {
                    Log.w(TAG, "Failed to delete temp file after cancellation.");
                }
                handler.post(callback::onCancelled);
            } catch (Exception e) {
                Log.e(TAG, "Download error", e);
                if (targetFile.exists() && !targetFile.delete()) {
                    Log.w(TAG, "Failed to delete temp file after error.");
                }
                handler.post(() -> callback.onError(e));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Cancels the current download and extraction task.
     */
    public void cancel() {
        isCancelled = true;
        if (currentFuture != null) {
            currentFuture.cancel(true);
        }
    }

    /**
     * Extracts the contents of a zip file into the specified directory.
     *
     * @param zipFile   The zip file to extract.
     * @param targetDir The directory to extract into.
     * @throws Exception If an error occurs during extraction.
     */
    private void extractZip(File zipFile, File targetDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry ze;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                if (isCancelled) throw new InterruptedException();
                
                File file = new File(targetDir, ze.getName());
                if (ze.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) {
                        throw new Exception("Failed to create directory: " + file.getAbsolutePath());
                    }
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new Exception("Failed to create directory: " + parent.getAbsolutePath());
                    }
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            if (isCancelled) throw new InterruptedException();
                            fos.write(buffer, 0, count);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
