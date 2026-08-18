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
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                }

                int fileLength = connection.getContentLength();
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
                handler.post(() -> callback.onError(e));
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
