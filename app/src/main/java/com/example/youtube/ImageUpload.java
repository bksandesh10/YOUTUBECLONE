package com.example.youtube;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageUpload extends AppCompatActivity {

    private TextView skipButton;
    private ImageView imageView;
    private Button uploadButton;
    private ImageButton imageButton;
    private Uri imageUri; // To store the selected image URI
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // Executor for background tasks

    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_selector);

        skipButton = findViewById(R.id.skip_btn);
        imageView = findViewById(R.id.image_pic);
        uploadButton = findViewById(R.id.uploadButton);
        imageButton = findViewById(R.id.image_button); // Assuming you have an ImageButton in your layout

        String userName = getIntent().getStringExtra("USER_NAME");
        int userAge = getIntent().getIntExtra("USER_AGE", 0);
        String userEmail = getIntent().getStringExtra("USER_EMAIL");

        // Launch the gallery to select an image
        imageButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);


        });

        skipButton.setOnClickListener(view -> {
            Intent intent = new Intent(ImageUpload.this, Dashboard.class);
            intent.putExtra("USER_NAME", userName);
            intent.putExtra("USER_AGE", userAge);
            startActivity(intent);
        });

        uploadButton.setOnClickListener(view -> {
            if (imageUri != null) {
                // Convert the selected image to Base64 and upload it
                uploadImage(imageUri , userName , userAge);
            } else {
                Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show();
            }

//            Intent intent1 = new Intent(ImageUpload.this, Dashboard.class);
//            intent1.putExtra("USER_NAME", userName);
//            intent1.putExtra("USER_AGE", userAge);
//            startActivity(intent1);
//            UserImage(userEmail);
        });




    }

    // Activity result launcher for picking an image
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    imageView.setImageURI(imageUri); // Display the selected image in the ImageView
                }
            });

    // Method to upload image
    private void uploadImage(Uri uri , String name , int age) {
        executorService.execute(() -> {
            String base64Image = convertImageToBase64(uri);
            if (base64Image != null) {
                sendImageToServer(base64Image  , name , age);
            } else {
                runOnUiThread(() -> Toast.makeText(ImageUpload.this, "Image conversion failed.", Toast.LENGTH_SHORT).show());
            }
        });
    }


    // Convert the selected image to Base64
    private String convertImageToBase64(Uri uri) {
        try {
            // Load the bitmap from the URI
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // Resize the bitmap (optional, you can adjust width and height)
            int width = 800; // Set your desired width
            int height = (int) (bitmap.getHeight() * (800.0 / bitmap.getWidth())); // Maintain aspect ratio
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

            // Convert Bitmap to Base64 with compression
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Compress with quality (0-100), lower number results in smaller size
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream); // Adjust quality (50 is an example)
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP); // Use NO_WRAP to avoid line breaks
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Return null if an error occurs
        }
    }


    private void sendImageToServer(String base64Image  , String userN , int userA) {

        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.1.77:5104/api/imageitem"); // Ensure this URL is correct
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                // Prepare the JSON input string
                String jsonInputString = "{ \"Name\" : \"" + userN + "\",\"Image\": \"" + base64Image + "\" }";
                Log.d("Upload", "JSON Input: " + jsonInputString); // Log the JSON input

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d("Upload", "Response Code: " + responseCode); // Log the response code

                runOnUiThread(() -> {
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Toast.makeText(ImageUpload.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        Intent sentImageToDashboard = new Intent(ImageUpload.this , Dashboard.class);
                        sentImageToDashboard.putExtra("Image" , base64Image);
                        sentImageToDashboard.putExtra("USER_NAME" , userN);
                        sentImageToDashboard.putExtra("USER_AGE" , userA);
                        startActivity(sentImageToDashboard);

                    } else {
                        Toast.makeText(ImageUpload.this, "Failed to upload image. Response Code: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ImageUpload.this, "Invalid URL.", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ImageUpload.this, "Error occurred while uploading.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


}
