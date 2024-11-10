package com.example.youtube;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginPage extends AppCompatActivity {

    private Button signBtn;
    private EditText user_Name;
    private EditText user_Password;
    private OkHttpClient client;
    private Button loginbtn;

    // HashMap to store usernames and their corresponding base64 images
    private HashMap<String, String> userDataMap = new HashMap<>();

    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Linking XML UI components
        signBtn = findViewById(R.id.SignInLogin);
        user_Name = findViewById(R.id.login_email_Input);
        user_Password = findViewById(R.id.login_password_input);
        loginbtn = findViewById(R.id.btnLogin);

        // Initialize OkHttpClient
        client = new OkHttpClient();

        loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = user_Name.getText().toString().trim();
                String password = user_Password.getText().toString().trim();

                if (name.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginPage.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                } else {
                    // First validate the user
                    loginUser(name, password);
                }
            }
        });

        signBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginPage.this, SignPage.class);
                startActivity(intent);
            }
        });
    }

    // Validate user credentials
    private void loginUser(String Name, String Password) {
        Request request = new Request.Builder()
                .url("http://192.168.1.77:5104/api/appapi")  // API for user details
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e("LoginPage", "Login failed: " + e.getMessage());
                    Toast.makeText(LoginPage.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("LoginPage", "Response: " + responseBody);

                    try {
                        // Parse the JSON array response from the GET request
                        JSONArray jsonArray = new JSONArray(responseBody);

                        boolean isValidUser = false;
                        String userName = null;
                        int userAge = 0;

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String apiName = jsonObject.getString("name");
                            String apiPassword = jsonObject.getString("password");

                            // Validate the credentials
                            if (Name.equalsIgnoreCase(apiName) && Password.equalsIgnoreCase(apiPassword)) {
                                isValidUser = true;
                                userName = jsonObject.getString("name");  // Get user's name
                                userAge = jsonObject.getInt("age");       // Get user's age
                                break;
                            }
                        }

                        if (isValidUser) {
                            // Successful login, fetch image URL
                            fetchImageUrl(userName, userAge);
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(LoginPage.this, "Invalid credentials.", Toast.LENGTH_SHORT).show();
                            });
                        }

                    } catch (JSONException e) {
                        Log.e("LoginPage", "JSON parsing error: " + e.getMessage());
                    }
                } else {
                    runOnUiThread(() -> {
                        Log.e("LoginPage", "Login failed: " + response.code());
                        Toast.makeText(LoginPage.this, "Login failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // Fetch image URL from a different API based on the username
    private void fetchImageUrl(String LoginUsername, Integer age) {
        String api = "http://192.168.1.77:5104/api/imageitem";
        Request imageUrlRequest = new Request.Builder()
                .url(api)  // API for fetching image URL
                .get()
                .build();

        client.newCall(imageUrlRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e("LoginPage", "Image URL fetch failed: " + e.getMessage());
                    proceedWithoutImage(LoginUsername, age);  // Proceed with login even if the image fetch fails
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("LoginPage", "Image URL Response data: " + responseData);

                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        boolean imageFound = false;

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String username = jsonObject.getString("name");
                            String base64Image = jsonObject.getString("image");  // Image is now in Base64

                            // Add username and Base64 image to HashMap
                            userDataMap.put(username, base64Image);
                            Log.d("LoginPage", "Added to HashMap: " + username + " -> " + base64Image.length() + " characters");

                            if (LoginUsername.equalsIgnoreCase(username)) {
                                imageFound = true;
                                setImageViewFromBase64(base64Image, LoginUsername, age);
                                break;
                            }
                        }

                        if (!imageFound) {
                            proceedWithoutImage(LoginUsername, age);  // Proceed without image if not found
                        }

                    } catch (JSONException e) {
                        Log.e("LoginPage", "JSON parsing error: " + e.getMessage());
                        proceedWithoutImage(LoginUsername, age);  // Proceed without image in case of JSON error
                    }
                } else {
                    Log.e("LoginPage", "API response unsuccessful");
                    proceedWithoutImage(LoginUsername, age);  // Proceed without image if API response is unsuccessful
                }
            }
        });
    }

    // Proceed without an image
    private void proceedWithoutImage(String username, Integer age) {
        Intent upload = new Intent(LoginPage.this, Dashboard.class);
        upload.putExtra("USER_NAME", username);
        upload.putExtra("USER_AGE", age);
        upload.putExtra("image_login", "");  // No image found, pass empty string
        startActivity(upload);
    }

    private void setImageViewFromBase64(String base64Image, String username, Integer age) {
        try {
            // Decode Base64 string
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            // Prepare to send user data to the Dashboard
            Intent upload = new Intent(LoginPage.this, Dashboard.class);
            upload.putExtra("USER_NAME", username);
            upload.putExtra("USER_AGE", age);
            upload.putExtra("image_login", base64Image);
            startActivity(upload);
        } catch (Exception e) {
            Log.e("LoginPage", "Error decoding Base64 image: " + e.getMessage());
            proceedWithoutImage(username, age);  // If decoding fails, proceed without image
        }
    }
}
