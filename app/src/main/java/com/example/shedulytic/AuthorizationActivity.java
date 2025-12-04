package com.example.shedulytic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AuthorizationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.authorization);
        
        // Mark onboarding as completed when user reaches this screen
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_completed", true).apply();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get references to the buttons and TextView
        Button loginButton = findViewById(R.id.loginButton);
        Button registerButton = findViewById(R.id.registerBtn);
        TextView guestButton = findViewById(R.id.guestButton);

        // Set click listener for the Login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AuthorizationActivity.this, LoginActivity.class);
                startActivity(intent);
                //  Do NOT call finish() here
            }
        });

        // Set click listener for the Register button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AuthorizationActivity.this, SignupActivity.class);
                startActivity(intent);
                //  Do NOT call finish() here
            }
        });

        // Set click listener for the "Continue as a Guest" TextView
        guestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AuthorizationActivity.this, MainNavigationActivity.class);
                startActivity(intent);
                //  Do NOT call finish() here
            }
        });
    }
}