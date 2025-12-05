package com.simats.schedulytic;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.TextView;

public class MainActivity4 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.startup_page_3);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get references to the "Next" and "Skip" buttons
        TextView nextButton = findViewById(R.id.nextButton); // Ensure this ID is in startup_page_3.xml
        TextView skipButton = findViewById(R.id.skipButton); // Ensure this ID is in startup_page_3.xml

        // Set click listener for the "Next" button
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the HomePage activity
                Intent intent = new Intent(MainActivity4.this, MainActivity5.class);
                startActivity(intent);
                finish();
            }
        });

        // Set click listener for the "Skip" button
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the HomePage activity
                Intent intent = new Intent(MainActivity4.this, AuthorizationActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
