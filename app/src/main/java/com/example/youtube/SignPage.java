package com.example.youtube;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SignPage extends AppCompatActivity {

    private Button BackToLog;
    private EditText user_Name;
    private EditText user_Email;
    private EditText user_Age;
    private EditText user_Password;
    private Button sign_Button;
    private TextView check_Name;
    private OkHttpClient client;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private View loadingView; // Loading screen view

    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign);

        BackToLog = findViewById(R.id.logpage);
        user_Name = findViewById(R.id.name_Input);
        user_Email = findViewById(R.id.email_Input);
        user_Age = findViewById(R.id.age_Input);
        user_Password = findViewById(R.id.password_input);
        sign_Button = findViewById(R.id.btnSign);
        check_Name = findViewById(R.id.nameChecker);

        // Initialize loading view (inflate from XML)
        loadingView = getLayoutInflater().inflate(R.layout.loading, null);
        addContentView(loadingView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        client = new OkHttpClient();

        // Initially hide the loading view
        loadingView.setVisibility(View.GONE);

        BackToLog.setOnClickListener(view -> {
            Intent intent = new Intent(SignPage.this, LoginPage.class);
            startActivity(intent);
        });

        sign_Button.setOnClickListener(view -> {
            String Name = user_Name.getText().toString();
            String Email = user_Email.getText().toString();
            String Age = user_Age.getText().toString();
            String password = user_Password.getText().toString();

            // Call method to check if the name is unique
            checkNameFromAPI(Name, Email, Age, password);
        });
    }

    private void checkNameFromAPI(String enteredName, String email, String age, String password) {
        // Run network request in background thread
        executorService.execute(() -> {
            // Create the HTTP request
            Request request = new Request.Builder()
                    .url("http://192.168.1.77:5104/api/appapi")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    // Get the JSON response body as a string
                    String jsonResponse = response.body().string();

                    // Parse the JSON response to a list of names
                    List<String> names = parseNamesFromJson(jsonResponse);

                    // Compare entered name with fetched names (ignoring case)
//                    boolean isUnique = names.stream().noneMatch(name -> name.equalsIgnoreCase(enteredName));
                    boolean isUnique = names.stream()
                            .noneMatch(name -> name.trim().equalsIgnoreCase(enteredName.trim()));


                    // Update UI on the main thread
                    mainHandler.post(() -> {
                        if (!isUnique) {
                            // Hide loading if the username is not unique
                            showLoading(false);
                            int color = getResources().getColor(R.color.red, null);
                            check_Name.setText("Sorry, this name is not available");
                            check_Name.setTextColor(color);
                        } else {
                            // Show loading only if the name is unique
                            showLoading(true);
                            int color = getResources().getColor(R.color.green, null);
                            check_Name.setText("This name is available");
                            check_Name.setTextColor(color);
                            sendVerificationCode(email, age, password);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        showLoading(false);
                        Toast.makeText(SignPage.this, "Request failed with status: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(SignPage.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<String> parseNamesFromJson(String jsonResponse) {
        List<String> names = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject userObject = jsonArray.getJSONObject(i);
                String name = userObject.getString("name");
                names.add(name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return names;
    }

    private void sendVerificationCode(String email, String age, String password) {
        new Thread(() -> {
            try {
                String verificationUrl = "http://192.168.1.77:5104/api/apptoken/send-token?recipientEmail=" + URLEncoder.encode(email, "UTF-8");

                HttpURLConnection v_connection = (HttpURLConnection) new URL(verificationUrl).openConnection();
                v_connection.setRequestMethod("GET");

                int responseCode = v_connection.getResponseCode();
                if (responseCode == 200) {
                    // Run this on the UI thread
                    runOnUiThread(() -> {
                        Toast.makeText(SignPage.this, "Verification code sent to your email!", Toast.LENGTH_SHORT).show();

                        // Start CodePage activity only after successful request
                        Intent intentCodePage = new Intent(SignPage.this, CodePage.class);
                        intentCodePage.putExtra("Name", user_Name.getText().toString());
                        intentCodePage.putExtra("Email", email);
                        intentCodePage.putExtra("Age", age);
                        intentCodePage.putExtra("Password", password);
                        showLoading(false); // Hide loading after code is sent
                        startActivity(intentCodePage);
                    });
                } else {
                    runOnUiThread(() -> {
                        showLoading(false); // Hide loading if code sending failed
                        Toast.makeText(SignPage.this, "Failed to send verification code.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showLoading(false); // Hide loading if there is an error
                    Toast.makeText(SignPage.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Method to show or hide loading screen
    private void showLoading(boolean show) {
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
