package com.example.serviceapp;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";

    public DownloadService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        List<String> fileUrls = intent.getStringArrayListExtra("pdfUrls");
        if (fileUrls != null && !fileUrls.isEmpty()) {
            new Thread(() -> {
                processDownloads(fileUrls);
                // Notify that downloads are complete
                Intent completionIntent = new Intent("com.example.serviceapp.DOWNLOAD_COMPLETE");
                sendBroadcast(completionIntent);
                stopSelf();
            }).start();
        } else {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void processDownloads(List<String> fileUrls) {
        for (String fileUrl : fileUrls) {
            HttpURLConnection httpConnection = null;
            InputStream inputStream = null;
            FileOutputStream fileOutputStream = null;
            try {
                Log.d(TAG, "Starting download: " + fileUrl);
                URL url = new URL(fileUrl);
                httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setInstanceFollowRedirects(true);
                httpConnection.connect();

                int responseStatus = httpConnection.getResponseCode();
                // Handle redirect responses (301/302)
                if (responseStatus == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseStatus == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String redirectedUrl = httpConnection.getHeaderField("Location");
                    Log.d(TAG, "Redirecting to: " + redirectedUrl);
                    httpConnection.disconnect();
                    url = new URL(redirectedUrl);
                    httpConnection = (HttpURLConnection) url.openConnection();
                    httpConnection.setRequestMethod("GET");
                    httpConnection.connect();
                    responseStatus = httpConnection.getResponseCode();
                }

                if (responseStatus == HttpURLConnection.HTTP_OK) {
                    // Generate filename from URL
                    String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                    if (fileName.isEmpty()) {
                        fileName = "default_download.pdf";
                    }

                    // Save file in the app’s Documents directory
                    File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    if (directory != null && !directory.exists()) {
                        directory.mkdirs();
                    }
                    File outputFile = new File(directory, fileName);

                    inputStream = new BufferedInputStream(httpConnection.getInputStream());
                    fileOutputStream = new FileOutputStream(outputFile);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }
                    fileOutputStream.flush();

                    Log.d(TAG, "File saved at: " + outputFile.getAbsolutePath());
                } else {
                    Log.e(TAG, "Failed to download, HTTP Response: " + responseStatus + " for " + fileUrl);
                }
            } catch (Exception e) {
                Log.e(TAG, "Download error: " + fileUrl, e);
            } finally {
                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This service is not meant for binding
        return null;
    }
}