package com.example.youtube;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class CodePage extends AppCompatActivity {

    private EditText codeInput;
    private Button submitBtn;
    private String u_Name;
    private String u_Email;
    private String u_Age;
    private String u_Password;

    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.codepage);

        // Retrieve data from Intent
        u_Name = getIntent().getStringExtra("Name");
        u_Email = getIntent().getStringExtra("Email");
        u_Age = getIntent().getStringExtra("Age");
        u_Password = getIntent().getStringExtra("Password");

        codeInput = findViewById(R.id.code_Input);
        submitBtn = findViewById(R.id.submit);

        submitBtn.setOnClickListener(view -> {
            String verificationCode = codeInput.getText().toString();
            verifyAndCreateAccount(verificationCode);
        });
    }

    private void verifyAndCreateAccount(String verificationCode) {
        Log.d("TAG", "Email: " + u_Email);
        Log.d("TAG", "Verification Code: " + verificationCode);

        new Thread(() -> {
            try {
                String verificationApiUrl = createVerificationUrl(u_Email, verificationCode);
                Log.d("TAG", "Verification API URL: " + verificationApiUrl);

                HttpURLConnection verifyConnection = openConnection(verificationApiUrl, "GET");

                int responseCode = verifyConnection.getResponseCode();
                StringBuilder response = new StringBuilder();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(verifyConnection.getInputStream()))) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                    }
                    Log.d("Verification Response", response.toString());
                    runOnUiThread(() -> showToast("Verification successful! Account created."));
                    createAccount();
                } else {
                    // Read error response
                    StringBuilder errorResponse = new StringBuilder();
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(verifyConnection.getErrorStream()))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                    }
                    Log.e("Error Response", errorResponse.toString());
                    runOnUiThread(() -> showToast("Verification failed. " + errorResponse.toString()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("An error occurred during verification."));
            }
        }).start();
    }

    private void createAccount() {
        new Thread(() -> {
            try {
                String jsonInputString = String.format(
                        "{\"Name\": \"%s\", \"Email\": \"%s\", \"Age\": \"%s\", \"Password\": \"%s\"}",
                        u_Name, u_Email, u_Age, u_Password
                );
                HttpURLConnection connection = openConnection("http://192.168.1.77:5104/api/appapi", "POST");
                sendJsonRequest(connection, jsonInputString);
//                192.168.1.77:5104

                if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                    runOnUiThread(() -> {
                        showToast("Account created successfully!");

                        Intent dashboardNavigation = new Intent(CodePage.this, ImageUpload.class);
                        dashboardNavigation.putExtra("USER_NAME", u_Name);
                        dashboardNavigation.putExtra("USER_AGE", Integer.parseInt(u_Age));
                        dashboardNavigation.putExtra("USER_EMAIL" , u_Email);
                        startActivity(dashboardNavigation);

                        ; // Close CodePage activity
                    });
                } else {
                    runOnUiThread(() -> showToast("Account creation failed."));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("An error occurred during account creation."));
            }
        }).start();
    }


    private String createVerificationUrl(String email, String code) throws IOException {
        return "http://192.168.1.77:5104/api/apptoken/verify-code?email="
                + URLEncoder.encode(email, "UTF-8") + "&code="
                + URLEncoder.encode(code, "UTF-8");
    }

    private HttpURLConnection openConnection(String urlString, String method) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        if ("POST".equalsIgnoreCase(method)) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
        }
        return connection;
    }

    private void sendJsonRequest(HttpURLConnection connection, String jsonInputString) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }

    private void showToast(String message) {
        Toast.makeText(CodePage.this, message, Toast.LENGTH_SHORT).show();
    }
}
