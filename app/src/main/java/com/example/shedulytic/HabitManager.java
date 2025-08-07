package com.example.shedulytic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.time.LocalDate;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.widget.TextView;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manager for handling habit streaks and user activity tracking
 */
public class HabitManager {
    private static final String TAG = "HabitManager";
    private final DatabaseHelper dbHelper;
    private final HabitListener listener;
    private final VolleyNetworkManager networkManager;
    private final Context context;

    // User streak data cache
    private final Map<String, Integer> streakCache = new HashMap<>();
    
    /**
     * Interface for habit streak callbacks
     */
    public interface HabitListener {
        void onHabitCompleted(String taskId, int newStreak);
        void onHabitUncompleted(String taskId, int newStreak);
        void onHabitStreakUpdated(String taskId, int newStreak);
        void onError(String message);
    }

    public HabitManager(Context context, HabitListener listener) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.listener = listener;
        this.networkManager = VolleyNetworkManager.getInstance(context);
        this.context = context;
    }

    /**
     * Toggle the completion status of a habit task
     * @param taskId ID of the habit task
     * @param isCompleted New completion status
     */
    public void toggleHabitCompletion(String taskId, boolean isCompleted) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");
            
            if (userId.isEmpty()) {
                if (listener != null) {
                    listener.onError("User ID not available");
                }
                return;
            }
            
            // Get current date
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Prepare request data
            JSONObject requestData = new JSONObject();
            requestData.put("task_id", taskId);
            requestData.put("user_id", userId);
            requestData.put("date", currentDate);
            requestData.put("completed", isCompleted ? 1 : 0);
            
            // Update habit completion status on server
            networkManager.makePostRequest(
                "update_task.php",
                requestData,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            Log.d(TAG, "Habit update response: " + response.toString());
                            
                            // Get current streak from response
                            int currentStreak = 0;
                            if (response.has("current_streak")) {
                                currentStreak = response.getInt("current_streak");
                            } else if (response.has("data") && response.getJSONObject("data").has("current_streak")) {
                                currentStreak = response.getJSONObject("data").getInt("current_streak");
                            }
                            
                            // Update streak cache
                            streakCache.put(taskId, currentStreak);
                            
                            // Notify listeners
                            if (listener != null) {
                                if (isCompleted) {
                                    listener.onHabitCompleted(taskId, currentStreak);
                                } else {
                                    listener.onHabitUncompleted(taskId, currentStreak);
                                }
                            }
                            
                            // Also update user login streak
                            updateUserLoginStreak(userId);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing habit response: " + e.getMessage());
                            if (listener != null) {
                                listener.onError("Error processing habit response: " + e.getMessage());
                            }
                        }
                    }
                    
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error updating habit: " + message);
                        if (listener != null) {
                            listener.onError("Error updating habit: " + message);
                        }
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error toggling habit completion: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error toggling habit: " + e.getMessage());
            }
        }
    }

    private int calculateStreak(SQLiteDatabase db, String taskId) {
        int streak = 0;
        LocalDate currentDate = LocalDate.now();
        
        String query = "SELECT COUNT(*) FROM task_completions " +
                      "WHERE task_id = ? AND completion_date = ?";
                      
        while (true) {
            try (Cursor cursor = db.rawQuery(query, 
                    new String[]{taskId, currentDate.toString()})) {
                if (cursor.moveToFirst()) {
                    int completions = cursor.getInt(0);
                    if (completions == 0) break;
                    streak++;
                }
            }
            currentDate = currentDate.minusDays(1);
        }
        
        return streak;
    }

    /**
     * Update user login streak whenever the app is used
     */
    public void updateUserLoginStreak(String userId) {
        try {
            // Current date for the streak
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Prepare data
            JSONObject requestData = new JSONObject();
            requestData.put("user_id", userId);
            requestData.put("date", currentDate);
            requestData.put("activity_type", "login");
            
            // Send to server
            networkManager.makePostRequest(
                "update_user_activity.php",
                requestData,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d(TAG, "Login streak update: " + response.toString());
                        // Update the streak count in UI
                        fetchUserStreakData(userId);
                    }
                    
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error updating login streak: " + message);
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error updating login streak: " + e.getMessage());
        }
    }
    
    /**
     * Fetch user streak data to update the UI
     */
    public void fetchUserStreakData(String userId) {
        try {
            // Get date range (last 30 days)
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String endDate = dateFormat.format(calendar.getTime());
            
            calendar.add(Calendar.DAY_OF_MONTH, -30); // Go back 30 days
            String startDate = dateFormat.format(calendar.getTime());
            
            // Get streak data using parallel requests for reliability
            String[] endpoints = {
                "get_user_streak.php?user_id=" + userId + "&start_date=" + startDate + "&end_date=" + endDate,
                "streak.php?user_id=" + userId
            };
            
            networkManager.makeBroadcastGetRequest(
                endpoints,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            Log.d(TAG, "Streak data: " + response.toString());
                            
                            // Parse streak count
                            int streakCount = 0;
                            if (response.has("streak_count")) {
                                streakCount = response.getInt("streak_count");
                            } else if (response.has("data") && response.getJSONObject("data").has("streak_count")) {
                                streakCount = response.getJSONObject("data").getInt("streak_count");
                            } else if (response.has("streak")) {
                                streakCount = response.getInt("streak");
                            }
                            
                            // Parse active days
                            JSONArray activeDaysArray = null;
                            if (response.has("active_days")) {
                                activeDaysArray = response.getJSONArray("active_days");
                            } else if (response.has("data") && response.getJSONObject("data").has("active_days")) {
                                activeDaysArray = response.getJSONObject("data").getJSONArray("active_days");
                            }
                            
                            // Save streak data to shared preferences for offline access
                            SharedPreferences streakPrefs = context.getSharedPreferences("StreakPrefs", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = streakPrefs.edit();
                            editor.putInt("streak_count", streakCount);
                            if (activeDaysArray != null) {
                                editor.putString("active_days", activeDaysArray.toString());
                            }
                            editor.apply();
                            
                            // Update UI
                            updateStreakUI(streakCount, activeDaysArray);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing streak data: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error fetching streak data: " + message);
                        // Try to use cached data
                        useOfflineStreakData();
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error fetching streak data: " + e.getMessage());
            useOfflineStreakData();
        }
    }
    
    /**
     * Use offline streak data from SharedPreferences when network is unavailable
     */
    private void useOfflineStreakData() {
        try {
            SharedPreferences streakPrefs = context.getSharedPreferences("StreakPrefs", Context.MODE_PRIVATE);
            int streakCount = streakPrefs.getInt("streak_count", 0);
            String activeDaysStr = streakPrefs.getString("active_days", null);
            
            JSONArray activeDaysArray = null;
            if (activeDaysStr != null) {
                activeDaysArray = new JSONArray(activeDaysStr);
            }
            
            updateStreakUI(streakCount, activeDaysArray);
        } catch (Exception e) {
            Log.e(TAG, "Error using offline streak data: " + e.getMessage());
        }
    }
    
    /**
     * Update the UI with streak information
     */
    private void updateStreakUI(int streakCount, JSONArray activeDays) {
        try {
            // Update the streak count TextView in fragmented_home
            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                activity.runOnUiThread(() -> {
                    try {
                        // Find the streak count TextView
                        TextView streakCountView = activity.findViewById(R.id.textView_days_count);
                        if (streakCountView != null) {
                            streakCountView.setText(String.valueOf(streakCount));
                        }
                        
                        // Update calendar with fire icons for active days
                        if (activeDays != null) {
                            for (int i = 0; i < activeDays.length(); i++) {
                                String activeDay = activeDays.getString(i);
                                markDayWithFireIcon(activity, activeDay);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating streak UI: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating streak UI: " + e.getMessage());
        }
    }
    
    /**
     * Mark a specific day in the calendar with a fire icon
     */
    private void markDayWithFireIcon(MainActivity activity, String dateStr) {
        try {
            // This implementation would depend on your specific calendar view
            // Send a message to highlight the specific date in the calendar
            activity.runOnUiThread(() -> {
                // Implementation depends on your calendar view
                // This is a placeholder for the actual implementation
                Log.d(TAG, "Marking day with fire icon: " + dateStr);
                
                // Find calendar view and update it
                View calendarView = activity.findViewById(R.id.monthCalenderRecyclerview);
                if (calendarView != null && calendarView instanceof CalendarViewWithFireIcons) {
                    ((CalendarViewWithFireIcons) calendarView).markDateWithFireIcon(dateStr);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error marking day with fire icon: " + e.getMessage());
        }
    }
    
    /**
     * Interface that your calendar view should implement
     */
    public interface CalendarViewWithFireIcons {
        void markDateWithFireIcon(String dateStr);
    }
}