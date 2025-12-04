package com.example.shedulytic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the status bar color to yellow
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        setContentView(R.layout.splash_screen);

        // Check login status and handle navigation
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, "");
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        boolean onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
        
        Log.d(TAG, "Checking login status - userId: " + userId + ", isLoggedIn: " + isLoggedIn + ", onboardingCompleted: " + onboardingCompleted);

        // If user is already logged in, go directly to main app
        if (isLoggedIn && userId != null && !userId.isEmpty()) {
            Log.d(TAG, "User already logged in, going to MainNavigationActivity");
            // Initialize streak tracking for logged-in user
            initializeStreakTracking();
            // Log user activity for today
            logUserActivity();
            
            // Navigate to main app after splash delay
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, MainNavigationActivity.class);
                intent.putExtra("user_id", userId);
                startActivity(intent);
                finish();
            }, 2000); // Shorter delay for returning users
        } else if (onboardingCompleted) {
            // Onboarding completed but not logged in - go to login
            Log.d(TAG, "Onboarding completed, going to LoginActivity");
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }, 2000);
        } else {
            // First time user - show onboarding
            Log.d(TAG, "First time user, showing onboarding");
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                startActivity(intent);
                finish();
            }, 3000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        logUserActivity();
    }

    private void logUserActivity() {
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "");
        if (!userId.isEmpty()) {
            // Use the new update_user_activity.php endpoint that properly updates streak
            String url = IpV4Connection.getBaseUrl() + "/update_user_activity.php";
            
            StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getString("status").equals("success")) {
                            // Activity logged and streak updated successfully
                            int currentStreak = jsonResponse.optInt("streak_count", 0);
                            Log.d("MainActivity", "User activity logged. Current streak: " + currentStreak);
                        }
                    } catch (JSONException e) {
                        Log.e("MainActivity", "Error parsing activity log response: " + e.getMessage());
                    }
                },
                error -> Log.e("MainActivity", "Error logging activity: " + error.getMessage())
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", userId);
                    params.put("activity_type", "login");
                    return params;
                }
            };

            Volley.newRequestQueue(this).add(request);
        }
    }

    /**
     * Initialize streak tracking when app starts
     */
    private void initializeStreakTracking() {
        try {
            // Get user ID
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");
            
            if (userId != null && !userId.isEmpty()) {
                // Create HabitManager instance
                HabitManager habitManager = new HabitManager(this, new HabitManager.HabitListener() {
                    @Override
                    public void onHabitCompleted(String taskId, int newStreak) {
                        Log.d("MainActivity", "Habit completed: " + taskId + ", New streak: " + newStreak);
                    }

                    @Override
                    public void onHabitUncompleted(String taskId, int newStreak) {
                        Log.d("MainActivity", "Habit uncompleted: " + taskId + ", New streak: " + newStreak);
                    }

                    @Override
                    public void onHabitStreakUpdated(String taskId, int newStreak) {
                        Log.d("MainActivity", "Habit streak updated: " + taskId + ", New streak: " + newStreak);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("MainActivity", "Habit Manager error: " + message);
                    }
                });
                
                // Record login activity
                habitManager.updateUserLoginStreak(userId);
                
                // Fetch current streak data
                habitManager.fetchUserStreakData(userId);
                
                Log.d("MainActivity", "Streak tracking initialized for user: " + userId);
            } else {
                Log.e("MainActivity", "Cannot initialize streak tracking: User ID is empty");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing streak tracking: " + e.getMessage());
        }
    }
}