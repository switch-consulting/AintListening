package de.switchconsulting.aintlistening;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModelDownloader {

    public interface Callback {
        void onProgress(int percentage);
        void onExtracting();
        void onSuccess();
        void onError(Exception e);
    }

    public static void downloadAndExtract(@NonNull String downloadUrl, @NonNull File targetBaseDir, @NonNull Callback callback) {
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            Log.d("ModelDownloader", "Starting download thread for: " + downloadUrl);
            HttpURLConnection connection = null;
            try {
                String currentUrl = downloadUrl;
                int redirectCount = 0;
                while (redirectCount < 5) {
                    URL url = new URL(currentUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setInstanceFollowRedirects(true);
                    
                    int responseCode = connection.getResponseCode();
                    Log.d("ModelDownloader", "URL: " + currentUrl + " -> Response: " + responseCode);

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
                Log.d("ModelDownloader", "File size: " + fileLength);
                File tempZip = new File(targetBaseDir, "model_temp.zip");

                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     OutputStream output = new FileOutputStream(tempZip)) {

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        if (fileLength > 0) {
                            int progress = (int) (total * 100 / fileLength);
                            handler.post(() -> callback.onProgress(progress));
                        }
                        output.write(data, 0, count);
                    }
                }

                handler.post(callback::onExtracting);
                extractZip(tempZip, targetBaseDir);
                if (!tempZip.delete()) {
                    Log.w("ModelDownloader", "Failed to delete temporary zip file: " + tempZip.getAbsolutePath());
                }

                handler.post(callback::onSuccess);

            } catch (Exception e) {
                Log.e("ModelDownloader", "Download error", e);
                handler.post(() -> callback.onError(e));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private static void extractZip(File zipFile, File targetDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry ze;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
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
                            fos.write(buffer, 0, count);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
