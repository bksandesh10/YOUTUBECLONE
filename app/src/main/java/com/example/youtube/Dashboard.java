package com.example.youtube;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Dashboard extends AppCompatActivity {


    private ImageView profile;


    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);




        profile = findViewById(R.id.user_profile_page);





        String userName = getIntent().getStringExtra("USER_NAME");
        int userAge = getIntent().getIntExtra("USER_AGE", 0);
        String userImage = getIntent().getStringExtra("Image");
        String userLoginImage  = getIntent().getStringExtra("image_login");
//        String loginUserImage = getIntent().getStringExtra("IMAGE_URL");



        if (userImage != null) {
            Bitmap bitmap = convertBase64ToBitmap(userImage);
            profile.setImageBitmap(bitmap); // Set the Bitmap to ImageView
        }

        if(userLoginImage != null) {
            byte[] decodedBytes = Base64.decode(userLoginImage, Base64.DEFAULT);
            Bitmap loginbitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            profile.setImageBitmap(loginbitmap);
        }



        profile.setOnClickListener(view ->  {
            Intent intentUser = new Intent(Dashboard.this, UserProfilePage.class);
            intentUser.putExtra("USER_NAME", userName);
            intentUser.putExtra("USER_AGE", userAge);
            intentUser.putExtra("Image" , userImage);
            intentUser.putExtra("image_login" , userLoginImage);
            startActivity(intentUser);
        });



    };

    private Bitmap convertBase64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}
