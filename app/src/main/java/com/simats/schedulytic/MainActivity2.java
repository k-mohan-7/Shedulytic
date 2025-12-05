package com.simats.schedulytic;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity2 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the status bar color to yellow
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        setContentView(R.layout.startup_page_1);

        // Get references to the "Next" and "Skip" buttons
        TextView nextButton = findViewById(R.id.nextButton);
        TextView skipButton = findViewById(R.id.skipButton);

        // Set click listener for the "Next" button
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the MainActivity3 activity (assuming startup_page_2 is associated with MainActivity3)
                Intent intent = new Intent(MainActivity2.this, MainActivity3.class); // Use MainActivity3.class
                startActivity(intent);
                finish(); // Optional: Close MainActivity2 if you don't want to go back
            }
        });

        // Set click listener for the "Skip" button
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the HomePage activity (assuming homepage.xml is associated with HomePage)
                Intent intent = new Intent(MainActivity2.this, AuthorizationActivity.class); // Use HomePage.class
                startActivity(intent);
                finish(); // Optional: Close MainActivity2
            }
        });
    }
}
