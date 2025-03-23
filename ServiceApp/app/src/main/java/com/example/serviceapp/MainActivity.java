package com.example.serviceapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 100;
    private EditText urlField1, urlField2, urlField3, urlField4, urlField5;
    private Button downloadButton;
    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Download completed successfully.", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("PDF Download Activity");

        urlField1 = findViewById(R.id.pdfUrl1);
        urlField2 = findViewById(R.id.pdfUrl2);
        urlField3 = findViewById(R.id.pdfUrl3);
        urlField4 = findViewById(R.id.pdfUrl4);
        urlField5 = findViewById(R.id.pdfUrl5);
        downloadButton = findViewById(R.id.startDownloadBtn);

        IntentFilter filter = new IntentFilter("com.example.serviceapp.DOWNLOAD_COMPLETE");

        // Ensure proper export status based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }

        downloadButton.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                requestStoragePermission();
            } else {
                initiateDownloadService();
            }
        });
    }


    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE
            );
        } else {
            initiateDownloadService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateDownloadService();
            } else {
                Toast.makeText(this, "Permission denied. Cannot proceed with download.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initiateDownloadService() {
        ArrayList<String> downloadUrls = new ArrayList<>();

        if (!urlField1.getText().toString().trim().isEmpty()) {
            downloadUrls.add(urlField1.getText().toString().trim());
        }
        if (!urlField2.getText().toString().trim().isEmpty()) {
            downloadUrls.add(urlField2.getText().toString().trim());
        }
        if (!urlField3.getText().toString().trim().isEmpty()) {
            downloadUrls.add(urlField3.getText().toString().trim());
        }
        if (!urlField4.getText().toString().trim().isEmpty()) {
            downloadUrls.add(urlField4.getText().toString().trim());
        }
        if (!urlField5.getText().toString().trim().isEmpty()) {
            downloadUrls.add(urlField5.getText().toString().trim());
        }

        if (!downloadUrls.isEmpty()) {
            Intent serviceIntent = new Intent(this, DownloadService.class);
            serviceIntent.putStringArrayListExtra("pdfUrls", downloadUrls);
            startService(serviceIntent);
        } else {
            Toast.makeText(this, "Please provide at least one URL to proceed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadReceiver);
    }
}