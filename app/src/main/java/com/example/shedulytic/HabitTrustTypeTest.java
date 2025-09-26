package com.example.shedulytic;

import android.content.Context;
import android.util.Log;

import com.example.shedulytic.model.Habit;
import com.example.shedulytic.service.HabitManagerService;

import org.json.JSONObject;

import java.util.List;

/**
 * Test utility to verify trust_type to verification method mapping
 * This helps debug the habit verification issue
 */
public class HabitTrustTypeTest {
    private static final String TAG = "HabitTrustTypeTest";
    
    /**
     * Test the trust_type parsing with sample JSON data
     */
    public static void testTrustTypeParsing() {
        Log.d(TAG, "=== Starting Trust Type Parsing Test ===");
        
        try {
            // Test case 1: Pomodoro trust_type
            String pomodoroJson = "{"
                + "\"habit_id\":\"test_1\","
                + "\"user_id\":\"user_123\","
                + "\"title\":\"Work Focus\","
                + "\"description\":\"Focus for 25 minutes\","
                + "\"trust_type\":\"pomodoro\","
                + "\"current_streak\":0,"
                + "\"total_completions\":0,"
                + "\"frequency\":\"daily\","
                + "\"pomodoro_count\":1,"
                + "\"pomodoro_length\":25"
                + "}";
            
            JSONObject pomodoroObj = new JSONObject(pomodoroJson);
            Habit pomodoroHabit = Habit.fromJson(pomodoroObj);
            
            Log.d(TAG, "Pomodoro Test:");
            Log.d(TAG, "  trust_type in JSON: " + pomodoroObj.optString("trust_type", "none"));
            Log.d(TAG, "  parsed verification_method: " + pomodoroHabit.getVerificationMethod());
            Log.d(TAG, "  expected: " + Habit.VERIFICATION_POMODORO);
            Log.d(TAG, "  result: " + (Habit.VERIFICATION_POMODORO.equals(pomodoroHabit.getVerificationMethod()) ? "PASS" : "FAIL"));
            
            // Test case 2: Location trust_type
            String locationJson = "{"
                + "\"habit_id\":\"test_2\","
                + "\"user_id\":\"user_123\","
                + "\"title\":\"Gym Workout\","
                + "\"description\":\"Go to the gym\","
                + "\"trust_type\":\"location\","
                + "\"current_streak\":0,"
                + "\"total_completions\":0,"
                + "\"frequency\":\"daily\","
                + "\"latitude\":40.7128,"
                + "\"longitude\":-74.0060,"
                + "\"radius_meters\":100"
                + "}";
            
            JSONObject locationObj = new JSONObject(locationJson);
            Habit locationHabit = Habit.fromJson(locationObj);
            
            Log.d(TAG, "Location Test:");
            Log.d(TAG, "  trust_type in JSON: " + locationObj.optString("trust_type", "none"));
            Log.d(TAG, "  parsed verification_method: " + locationHabit.getVerificationMethod());
            Log.d(TAG, "  expected: " + Habit.VERIFICATION_LOCATION);
            Log.d(TAG, "  result: " + (Habit.VERIFICATION_LOCATION.equals(locationHabit.getVerificationMethod()) ? "PASS" : "FAIL"));
            
            // Test case 3: Map trust_type (alternative location)
            String mapJson = "{"
                + "\"habit_id\":\"test_3\","
                + "\"user_id\":\"user_123\","
                + "\"title\":\"Coffee Shop\","
                + "\"description\":\"Work from coffee shop\","
                + "\"trust_type\":\"map\","
                + "\"current_streak\":0,"
                + "\"total_completions\":0,"
                + "\"frequency\":\"daily\","
                + "\"latitude\":40.7589,"
                + "\"longitude\":-73.9851,"
                + "\"radius_meters\":50"
                + "}";
            
            JSONObject mapObj = new JSONObject(mapJson);
            Habit mapHabit = Habit.fromJson(mapObj);
            
            Log.d(TAG, "Map Test:");
            Log.d(TAG, "  trust_type in JSON: " + mapObj.optString("trust_type", "none"));
            Log.d(TAG, "  parsed verification_method: " + mapHabit.getVerificationMethod());
            Log.d(TAG, "  expected: " + Habit.VERIFICATION_LOCATION);
            Log.d(TAG, "  result: " + (Habit.VERIFICATION_LOCATION.equals(mapHabit.getVerificationMethod()) ? "PASS" : "FAIL"));
            
            // Test case 4: Default/checkbox trust_type
            String checkboxJson = "{"
                + "\"habit_id\":\"test_4\","
                + "\"user_id\":\"user_123\","
                + "\"title\":\"Drink Water\","
                + "\"description\":\"Drink 8 glasses of water\","
                + "\"trust_type\":\"checkbox\","
                + "\"current_streak\":0,"
                + "\"total_completions\":0,"
                + "\"frequency\":\"daily\""
                + "}";
            
            JSONObject checkboxObj = new JSONObject(checkboxJson);
            Habit checkboxHabit = Habit.fromJson(checkboxObj);
            
            Log.d(TAG, "Checkbox Test:");
            Log.d(TAG, "  trust_type in JSON: " + checkboxObj.optString("trust_type", "none"));
            Log.d(TAG, "  parsed verification_method: " + checkboxHabit.getVerificationMethod());
            Log.d(TAG, "  expected: " + Habit.VERIFICATION_CHECKBOX);
            Log.d(TAG, "  result: " + (Habit.VERIFICATION_CHECKBOX.equals(checkboxHabit.getVerificationMethod()) ? "PASS" : "FAIL"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error in trust type test: " + e.getMessage(), e);
        }
        
        Log.d(TAG, "=== Trust Type Parsing Test Complete ===");
    }
    
    /**
     * Test the cache clearing functionality
     */
    public static void testCacheClear(Context context) {
        Log.d(TAG, "=== Starting Cache Clear Test ===");
        
        HabitManagerService habitManager = HabitManagerService.getInstance(context);
        
        // Set up listener to track cache refresh
        habitManager.setListener(new HabitManagerService.HabitListener() {
            @Override
            public void onHabitsLoaded(List<Habit> habits) {
                Log.d(TAG, "Cache refresh completed. Loaded " + habits.size() + " habits:");
                for (Habit habit : habits) {
                    Log.d(TAG, "  - " + habit.getTitle() + " (" + habit.getVerificationMethod() + ")");
                }
            }
            
            @Override
            public void onHabitAdded(Habit habit) {}
            
            @Override
            public void onHabitUpdated(Habit habit) {}
            
            @Override
            public void onHabitVerified(Habit habit, boolean isCompleted) {}
            
            @Override
            public void onHabitStreakUpdated(String habitId, int newStreak) {}
            
            @Override
            public void onProgressUpdated(float overallProgress, java.util.Map<String, Float> habitProgress) {}
            
            @Override
            public void onError(String message) {
                Log.e(TAG, "Cache refresh error: " + message);
            }
        });
        
        // Trigger cache refresh
        Log.d(TAG, "Triggering cache refresh...");
        habitManager.refreshHabits();
        
        Log.d(TAG, "=== Cache Clear Test Initiated ===");
    }
}
