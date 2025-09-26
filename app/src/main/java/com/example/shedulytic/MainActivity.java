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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the status bar color to yellow
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        setContentView(R.layout.splash_screen);

        // Initialize the HabitManager to track user login streaks
        initializeStreakTracking();

        // Simulate a splash screen delay (e.g., 3 seconds)
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
            finish();
        }, 3000); // 3000 milliseconds = 3 seconds
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
            String url = IpV4Connection.getBaseUrl() + "/log_user_activity.php";
            
            StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getString("status").equals("success")) {
                            // Activity logged successfully
                            Log.d("MainActivity", "User activity logged");
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