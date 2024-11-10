package com.example.youtube;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UserProfilePage extends AppCompatActivity {



    private TextView user_Name;
    private TextView userProfileInfo;
    private Button logoutButton;
    private ImageView userImage;



    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        user_Name = findViewById(R.id.user_profile_name);
        userProfileInfo = findViewById(R.id.user_Info);
        logoutButton = findViewById(R.id.logoutButton);
        userImage = findViewById(R.id.user_Image_section);



        String userName = getIntent().getStringExtra("USER_NAME");
        int userAge = getIntent().getIntExtra("USER_AGE", 0);
        String user_Image = getIntent().getStringExtra("Image");
        String userloginImage = getIntent().getStringExtra("image_login");

        // Convert the Base64 string to Bitmap and set it to ImageView
        if (user_Image != null) {
            Bitmap bitmap = convertBase64ToBitmap(user_Image);
            userImage.setImageBitmap(bitmap); // Set the Bitmap to ImageView
        }

        if(userloginImage != null) {
            byte[] decodedBytes = Base64.decode(userloginImage, Base64.DEFAULT);
            Bitmap loginbitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            userImage.setImageBitmap(loginbitmap);
        }




        user_Name.setText("Welcome, " + userName + "!");
        userProfileInfo.setText("Age: " + userAge);
//        userImage.g



        logoutButton.setOnClickListener(view -> {
            // Go back to the login page
            Intent intent = new Intent(UserProfilePage.this, LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();  // Close the current activity
        });

    }

    private Bitmap convertBase64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}
