package com.example.shedulytic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.Locale;

public class LocationVerificationActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private static final float VERIFICATION_RADIUS_METERS = 100; // 100 meters tolerance

    private TextView habitTitleTextView, habitDescriptionTextView, targetLocationTextView, currentLocationTextView, statusTextView;
    private Button verifyButton;

    private FusedLocationProviderClient fusedLocationClient;

    private String habitId;
    private String habitTitle;
    private String habitDescription;
    private double targetLatitude;
    private double targetLongitude;

    public static final String EXTRA_HABIT_ID = "com.example.shedulytic.EXTRA_HABIT_ID_LOC";
    public static final String EXTRA_HABIT_TITLE = "com.example.shedulytic.EXTRA_HABIT_TITLE_LOC";
    public static final String EXTRA_HABIT_DESCRIPTION = "com.example.shedulytic.EXTRA_HABIT_DESC_LOC";
    public static final String EXTRA_TARGET_LATITUDE = "com.example.shedulytic.EXTRA_TARGET_LAT_LOC";
    public static final String EXTRA_TARGET_LONGITUDE = "com.example.shedulytic.EXTRA_TARGET_LON_LOC";

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_verification);
        
        habitTitleTextView = findViewById(R.id.loc_verify_habit_title);
        habitDescriptionTextView = findViewById(R.id.loc_verify_habit_description);
        targetLocationTextView = findViewById(R.id.loc_verify_target_location_text);
        currentLocationTextView = findViewById(R.id.loc_verify_current_location_text);
        statusTextView = findViewById(R.id.loc_verify_status_text);
        verifyButton = findViewById(R.id.loc_verify_button_fullscreen);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Intent intent = getIntent();
        habitId = intent.getStringExtra(EXTRA_HABIT_ID);
        habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE);
        habitDescription = intent.getStringExtra(EXTRA_HABIT_DESCRIPTION);
        targetLatitude = intent.getDoubleExtra(EXTRA_TARGET_LATITUDE, 0);
        targetLongitude = intent.getDoubleExtra(EXTRA_TARGET_LONGITUDE, 0);

        if (habitId == null) {
            Toast.makeText(this, "Error: Habit details not provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        populateHabitDetails();

        verifyButton.setOnClickListener(v -> checkLocationAndVerify());
        
        // Attempt to get location on activity start
        requestLocationAndVerify();
    }
    
    private void populateHabitDetails() {
        habitTitleTextView.setText(habitTitle);
        if (habitDescription != null && !habitDescription.isEmpty()) {
            habitDescriptionTextView.setText(habitDescription);
        } else {
            habitDescriptionTextView.setText("Verify your presence at the designated location.");
        }
        targetLocationTextView.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", targetLatitude, targetLongitude));
    }
    
    private void requestLocationAndVerify() {
        currentLocationTextView.setText("Fetching current location...");
        statusTextView.setVisibility(View.INVISIBLE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fetchLastLocation();
    }


    private void checkLocationAndVerify() {
        currentLocationTextView.setText("Fetching current location...");
        statusTextView.setVisibility(View.INVISIBLE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            Toast.makeText(this, "Location permission needed to verify.", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
       fetchLastLocation();
    }
    
    private void fetchLastLocation() {
         if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Should not happen if checkLocationAndVerify was called
        }
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocationTextView.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f (Accuracy: %.0fm)", 
                        location.getLatitude(), location.getLongitude(), location.getAccuracy()));
                
                    float[] distanceResults = new float[1];
                    Location.distanceBetween(targetLatitude, targetLongitude, 
                                            location.getLatitude(), location.getLongitude(), 
                                            distanceResults);
                    float distanceInMeters = distanceResults[0];

                    statusTextView.setVisibility(View.VISIBLE);
                    if (distanceInMeters <= VERIFICATION_RADIUS_METERS) {
                        statusTextView.setText(String.format(Locale.getDefault(), "Verified! You are %.1fm from target.", distanceInMeters));
                        statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        // TODO: Communicate success back to the calling fragment/activity
                        // Mark habit as completed, update database etc.
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("habitId", habitId);
                        resultIntent.putExtra("verified", true);
                        setResult(RESULT_OK, resultIntent);
                        verifyButton.setText("Verified! Close");
                        verifyButton.setEnabled(false); 
                        // Optionally finish after a delay
                        // finish(); 
            } else {
                        statusTextView.setText(String.format(Locale.getDefault(), "Not yet there. You are %.1fm away. Required: %.0fm", distanceInMeters, VERIFICATION_RADIUS_METERS));
                        statusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        verifyButton.setText("Re-check Location");
                    }
                } else {
                    currentLocationTextView.setText("Could not get current location. Make sure GPS is on.");
                    statusTextView.setText("Verification failed: No location.");
                    statusTextView.setVisibility(View.VISIBLE);
                }
            })
            .addOnFailureListener(this, e -> {
                currentLocationTextView.setText("Failed to get location.");
                statusTextView.setText("Verification failed: Location error.");
                statusTextView.setVisibility(View.VISIBLE);
                Toast.makeText(LocationVerificationActivity.this, "Error fetching location: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Try verifying again.", Toast.LENGTH_SHORT).show();
                fetchLastLocation(); // Try fetching location again
            } else {
                Toast.makeText(this, "Location permission is required to verify this habit.", Toast.LENGTH_LONG).show();
                currentLocationTextView.setText("Permission denied.");
            }
        }
    }
} 