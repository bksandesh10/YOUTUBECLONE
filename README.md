package com.example.myapplication;

import android.content.ContentResolver;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Uri videoUri;
    private final OkHttpClient client = new OkHttpClient();

    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    videoUri = result.getData().getData();
                    if (videoUri != null) {
                        Log.d("Video Selection", "Video URI: " + videoUri.toString());
                        Toast.makeText(this, "Video selected successfully!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Video selection failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button videoSelectButton = findViewById(R.id.btnPickVideo);
        Button uploadVideoButton = findViewById(R.id.btnUploadVideo);

        videoSelectButton.setOnClickListener(v -> videoPicker());

        uploadVideoButton.setOnClickListener(v -> {
            if (videoUri != null) {
                // Assuming you already have the username for the upload
                String username = "user123";  // Example username
                VideoSubmitToBackend(username, videoUri);
            } else {
                Toast.makeText(MainActivity.this, "Please select a video first.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void videoPicker() {
        Intent videoSelection = new Intent(Intent.ACTION_GET_CONTENT);
        videoSelection.setType("video/*");
        videoSelection.addCategory(Intent.CATEGORY_OPENABLE);
        videoPickerLauncher.launch(videoSelection);
    }

    private void VideoSubmitToBackend(String username, Uri videoUri) {
        // Extract the file name from the Uri asynchronously
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String fileName = getFileName(videoUri);
            if (fileName == null) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to get the file name.", Toast.LENGTH_SHORT).show());
                return;
            }

            // Backend URL
            String backendUrl = "http://192.168.1.121:5104/video/uploadvideo";

            // Create JSON body with just the file name (video.mp4)
            String jsonBody = String.format("{\"username\":\"%s\", \"video\":\"%s\"}", username, fileName);

            // Log the JSON body for debugging
            Log.d("Request JSON", jsonBody);

            // Create the request body
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            // Build the request
            Request request = new Request.Builder()
                    .url(backendUrl)
                    .post(body)
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("MainActivity", "Request failed", e);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Request failed", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body() != null ? response.body().string() : "No response body";
                    Log.d("MainActivity", "Response Code: " + response.code());
                    Log.d("MainActivity", "Response: " + responseData);

                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Request successful", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Request Unsuccessful", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }

    // Helper method to extract the file name from Uri asynchronously
    private String getFileName(Uri uri) {
        String fileName = null;

        // Check if the Uri is a content Uri or a file Uri
        if (uri.getScheme().equals("content")) {
            ContentResolver contentResolver = getContentResolver();
            String[] projection = {MediaStore.Video.Media.DISPLAY_NAME};
            try (Cursor cursor = contentResolver.query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error getting file name", e);
            }
        } else if (uri.getScheme().equals("file")) {
            File file = new File(uri.getPath());
            fileName = file.getName();
        }

        return fileName;
    }
}
