package com.simats.schedulytic.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.simats.schedulytic.R;
import com.simats.schedulytic.model.Habit;
import com.simats.schedulytic.service.HabitManagerService;
import com.simats.schedulytic.service.LocationVerificationService;

/**
 * Utility class for verifying location-based habits
 */
public class HabitLocationVerifier {
    private static final String TAG = "HabitLocationVerifier";
    private static final float DEFAULT_RADIUS_METERS = 100f;
    
    public interface LocationVerificationListener {
        void onVerificationSuccess(Habit habit);
        void onVerificationFailure(Habit habit, float distance);
    }
    
    private Context context;
    private LocationVerificationService locationService;
    private HabitManagerService habitManager;
    private LocationVerificationListener listener;
    
    public HabitLocationVerifier(Context context, LocationVerificationService locationService, 
                                 HabitManagerService habitManager) {
        this.context = context;
        this.locationService = locationService;
        this.habitManager = habitManager;
    }
    
    public void setListener(LocationVerificationListener listener) {
        this.listener = listener;
    }
    
    /**
     * Verify a location-based habit
     * @param habit The habit to verify
     */
    public void verifyHabit(Habit habit) {
        if (locationService == null) {
            Log.e(TAG, "Location service not available");
            showErrorDialog("Location service not available");
            return;
        }
        
        if (habit == null) {
            Log.e(TAG, "Habit is null");
            showErrorDialog("Invalid habit");
            return;
        }
        
        // Start location tracking
        locationService.startLocationTracking();
        
        // Get current location
        Location currentLocation = locationService.getCurrentLocation();
        if (currentLocation == null) {
            Log.e(TAG, "Current location not available");
            showErrorDialog("Cannot get your current location. Please try again later.");
            return;
        }
        
        // Get habit location
        double habitLatitude = habit.getLatitude();
        double habitLongitude = habit.getLongitude();
        float radiusMeters = (float) habit.getRadiusMeters();
        
        if (radiusMeters <= 0) {
            radiusMeters = DEFAULT_RADIUS_METERS;
        }
        
        // Calculate distance
        float distance = locationService.getDistanceToTarget(habitLatitude, habitLongitude);
        
        if (distance < 0) {
            Log.e(TAG, "Error calculating distance");
            showErrorDialog("Error calculating distance to habit location");
            return;
        }
        
        Log.d(TAG, "Distance to habit location: " + distance + " meters (radius: " + radiusMeters + " meters)");
        
        // Check if within radius
        if (distance <= radiusMeters) {
            // Success - verify the habit
            if (habitManager != null) {
                habitManager.verifyHabitWithLocation(habit.getHabitId());
            }
            
            // Show success dialog
            showSuccessDialog(habit, distance);
            
            // Notify listener
            if (listener != null) {
                listener.onVerificationSuccess(habit);
            }
        } else {
            // Failure - show dialog
            showFailureDialog(habit, distance);
            
            // Notify listener
            if (listener != null) {
                listener.onVerificationFailure(habit, distance);
            }
        }
    }
    
    /**
     * Show verification success dialog
     */
    public void showSuccessDialog(Habit habit, float distance) {
        if (context == null) return;
        
        View view = LayoutInflater.from(context).inflate(R.layout.location_verification_layout, null);
        
        TextView titleText = view.findViewById(R.id.location_verification_title);
        TextView descriptionText = view.findViewById(R.id.location_verification_description);
        Button verifyButton = view.findViewById(R.id.verify_location_button);
        
        titleText.setText("Location Verified!");
        descriptionText.setText("You are within the required distance of your habit location.");
        verifyButton.setText("Great!");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true);
        
        final AlertDialog dialog = builder.create();
        verifyButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    
    /**
     * Show verification failure dialog
     */
    public void showFailureDialog(Habit habit, float distance) {
        if (context == null) return;
        
        View view = LayoutInflater.from(context).inflate(R.layout.location_verification_layout, null);
        
        TextView titleText = view.findViewById(R.id.location_verification_title);
        TextView descriptionText = view.findViewById(R.id.location_verification_description);
        Button verifyButton = view.findViewById(R.id.verify_location_button);
        
        titleText.setText("Location Not Verified");
        descriptionText.setText(String.format(
                "You are %.1f meters away from your habit location. You need to be within %d meters.", 
                distance, (int)habit.getRadiusMeters()));
        verifyButton.setText("Try Again");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true);
        
        final AlertDialog dialog = builder.create();
        verifyButton.setOnClickListener(v -> {
            dialog.dismiss();
            verifyHabit(habit);
        });
        dialog.show();
    }
    
    /**
     * Show error dialog
     */
    private void showErrorDialog(String message) {
        if (context == null) return;
        
        new AlertDialog.Builder(context)
                .setTitle("Location Verification Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Open map with directions to habit location
     */
    private void openMap(Habit habit) {
        if (context == null) return;
        
        try {
            // Create intent for opening Google Maps with directions
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(
                    String.format("geo:%f,%f?q=%f,%f(%s)", 
                            habit.getLatitude(), habit.getLongitude(),
                            habit.getLatitude(), habit.getLongitude(),
                            android.net.Uri.encode(habit.getTitle()))));
            
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening map: " + e.getMessage());
            showErrorDialog("Could not open map. Make sure you have a maps app installed.");
        }
    }
} 