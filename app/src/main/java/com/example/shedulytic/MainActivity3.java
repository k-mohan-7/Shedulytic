package com.example.shedulytic;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.TextView; // Import TextView

public class MainActivity3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.startup_page_2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get reference to the "Skip" button
        TextView skipButton = findViewById(R.id.skipButton); //  Correct ID to skipButton
        TextView nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the MainActivity3 activity (assuming startup_page_2 is associated with MainActivity3)
                Intent intent = new Intent(MainActivity3.this, MainActivity4.class); // Use MainActivity3.class
                startActivity(intent);
                finish(); // Optional: Close MainActivity2 if you don't want to go back
            }
        });
        // Set click listener for the "Skip" button
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the HomePage activity
                Intent intent = new Intent(MainActivity3.this, AuthorizationActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
