package com.simats.schedulytic.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.annotation.NonNull;

import com.simats.schedulytic.DatabaseHelper;
import com.simats.schedulytic.VolleyNetworkManager;
import com.simats.schedulytic.model.Habit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage habits, their verification, and progress tracking
 */
public class HabitManagerService {
    private static final String TAG = "HabitManagerService";
    private static HabitManagerService instance;
    
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final VolleyNetworkManager networkManager;
    
    // Cache for habits to minimize database access
    private final Map<String, Habit> habitsCache = new ConcurrentHashMap<>();
    
    // Habit streak data
    private final Map<String, Integer> streakCache = new HashMap<>();
    
    // Listener interface for habit events
    public interface HabitListener {
        void onHabitsLoaded(List<Habit> habits);
        void onHabitAdded(Habit habit);
        void onHabitUpdated(Habit habit);
        void onHabitVerified(Habit habit, boolean isCompleted);
        void onHabitStreakUpdated(String habitId, int newStreak);
        void onProgressUpdated(float overallProgress, Map<String, Float> habitProgress);
        void onError(String message);
    }
    
    private HabitListener listener;
    
    /**
     * Get singleton instance of HabitManagerService
     */
    public static synchronized HabitManagerService getInstance(Context context) {
        if (instance == null) {
            instance = new HabitManagerService(context.getApplicationContext());
        }
        return instance;
    }
    
    private HabitManagerService(Context context) {
        this.context = context;
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.networkManager = VolleyNetworkManager.getInstance(context);
    }
    
    /**
     * Set listener for habit events
     */
    public void setListener(HabitListener listener) {
        this.listener = listener;
    }
    
    /**
     * Get user ID from shared preferences
     */
    private String getUserId() {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        return prefs.getString("user_id", "");
    }
    
    /**
     * Add a new habit
     */
    public void addHabit(@NonNull Habit habit) {
        try {
            // Set user ID
            habit.setUserId(getUserId());
            
            // Add to local cache immediately
            habitsCache.put(habit.getHabitId(), habit);
            
            // Save to local database
            saveHabitToLocalDb(habit);
            
            // Notify listener
            if (listener != null) {
                listener.onHabitAdded(habit);
            }
            
            // Upload to server
            uploadHabitToServer(habit);
        } catch (Exception e) {
            Log.e(TAG, "Error adding habit: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error adding habit: " + e.getMessage());
            }
        }
    }
    
    /**
     * Save habit to local database
     */
    private void saveHabitToLocalDb(Habit habit) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            // Check if habit already exists
            String[] columns = {"habit_id"};
            String selection = "habit_id = ?";
            String[] selectionArgs = {habit.getHabitId()};
            Cursor cursor = db.query("habits", columns, selection, selectionArgs, null, null, null);
            boolean exists = cursor.getCount() > 0;
            cursor.close();
            
            // Prepare values
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("habit_id", habit.getHabitId());
            values.put("user_id", habit.getUserId());
            values.put("title", habit.getTitle());
            values.put("description", habit.getDescription());
            values.put("verification_method", habit.getVerificationMethod());
            values.put("is_completed", habit.isCompleted() ? 1 : 0);
            values.put("current_streak", habit.getCurrentStreak());
            values.put("total_completions", habit.getTotalCompletions());
            values.put("frequency", habit.getFrequency());
            
            // Method-specific data as JSON
            JSONObject methodData = new JSONObject();
            if (Habit.VERIFICATION_LOCATION.equals(habit.getVerificationMethod())) {
                methodData.put("latitude", habit.getLatitude());
                methodData.put("longitude", habit.getLongitude());
                methodData.put("radius_meters", habit.getRadiusMeters());
            } else if (Habit.VERIFICATION_POMODORO.equals(habit.getVerificationMethod())) {
                methodData.put("pomodoro_count", habit.getPomodoroCount());
                methodData.put("pomodoro_length", habit.getPomodoroLength());
            }
            values.put("method_data", methodData.toString());
            
            // Insert or update
            if (exists) {
                db.update("habits", values, "habit_id = ?", new String[]{habit.getHabitId()});
                Log.d(TAG, "Updated habit in local database: " + habit.getTitle());
            } else {
                db.insert("habits", null, values);
                Log.d(TAG, "Inserted habit in local database: " + habit.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving habit to local database: " + e.getMessage());
        }
    }
    
    /**
     * Upload habit to server
     */
    private void uploadHabitToServer(Habit habit) {
        try {
            JSONObject habitJson = habit.toJson();
            
            networkManager.makePostRequest(
                "add_habit.php",
                habitJson,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d(TAG, "Habit uploaded successfully: " + response.toString());
                        // Update with server ID if provided
                        try {
                            if (response.has("habit_id")) {
                                String serverId = response.getString("habit_id");
                                if (!serverId.equals(habit.getHabitId())) {
                                    // Update local ID with server ID
                                    habitsCache.remove(habit.getHabitId());
                                    habit.setHabitId(serverId);
                                    habitsCache.put(serverId, habit);
                                    saveHabitToLocalDb(habit);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing server response: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error uploading habit: " + errorMessage);
                        // Habit is still saved locally, so we can retry later
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error preparing habit for upload: " + e.getMessage());
        }
    }
    
    /**
     * Load habits from local database and server
     */
    public void loadHabits() {
        // Load from local database first for immediate display
        List<Habit> localHabits = loadHabitsFromLocalDb();
        
        if (!localHabits.isEmpty()) {
            // Update cache
            for (Habit habit : localHabits) {
                habitsCache.put(habit.getHabitId(), habit);
            }
            
            // Notify listener
            if (listener != null) {
                listener.onHabitsLoaded(localHabits);
            }
        }
        
        // Then fetch from server for the latest data
        loadHabitsFromServer();
    }
    
    /**
     * Load habits from local database
     */
    private List<Habit> loadHabitsFromLocalDb() {
        List<Habit> habits = new ArrayList<>();
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String[] projection = {
                "habit_id", "user_id", "title", "description", "verification_method",
                "is_completed", "current_streak", "total_completions", "frequency", "method_data"
            };
            
            String selection = "user_id = ?";
            String[] selectionArgs = {getUserId()};
            
            Cursor cursor = db.query(
                "habits",
                projection,
                selection,
                selectionArgs,
                null,
                null,
                "title ASC"
            );
            
            while (cursor.moveToNext()) {
                try {
                    Habit habit = new Habit();
                    habit.setHabitId(cursor.getString(cursor.getColumnIndex("habit_id")));
                    habit.setUserId(cursor.getString(cursor.getColumnIndex("user_id")));
                    habit.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                    habit.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                    habit.setVerificationMethod(cursor.getString(cursor.getColumnIndex("verification_method")));
                    habit.setCompleted(cursor.getInt(cursor.getColumnIndex("is_completed")) == 1);
                    habit.setCurrentStreak(cursor.getInt(cursor.getColumnIndex("current_streak")));
                    habit.setTotalCompletions(cursor.getInt(cursor.getColumnIndex("total_completions")));
                    habit.setFrequency(cursor.getString(cursor.getColumnIndex("frequency")));
                    
                    // Parse method-specific data
                    String methodDataStr = cursor.getString(cursor.getColumnIndex("method_data"));
                    if (methodDataStr != null && !methodDataStr.isEmpty()) {
                        JSONObject methodData = new JSONObject(methodDataStr);
                        
                        if (Habit.VERIFICATION_LOCATION.equals(habit.getVerificationMethod())) {
                            habit.setLatitude(methodData.optDouble("latitude", 0));
                            habit.setLongitude(methodData.optDouble("longitude", 0));
                            habit.setRadiusMeters(methodData.optInt("radius_meters", 100));
                        } else if (Habit.VERIFICATION_POMODORO.equals(habit.getVerificationMethod())) {
                            habit.setPomodoroCount(methodData.optInt("pomodoro_count", 1));
                            habit.setPomodoroLength(methodData.optInt("pomodoro_length", 25));
                        }
                    }
                    
                    habits.add(habit);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing habit from database: " + e.getMessage());
                }
            }
            
            cursor.close();
            Log.d(TAG, "Loaded " + habits.size() + " habits from local database");
        } catch (Exception e) {
            Log.e(TAG, "Error loading habits from local database: " + e.getMessage());
        }
        
        return habits;
    }
     /**
     * Load habits from server
     */
    private void loadHabitsFromServer() {
        String userId = getUserId();
        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty, cannot load habits from server");
            return;
        }

        String endpoint = "get_habits.php?user_id=" + userId;
        
        // Use ArrayResponseListener since server returns JSONArray directly
        networkManager.makeArrayGetRequest(
            endpoint,
            new VolleyNetworkManager.ArrayResponseListener() {
                @Override
                public void onSuccess(org.json.JSONArray habitsArray) {
                    try {
                        Log.d(TAG, "Server returned " + habitsArray.length() + " habits");
                        
                        List<Habit> serverHabits = new ArrayList<>();
                        
                        for (int i = 0; i < habitsArray.length(); i++) {
                            JSONObject habitJson = habitsArray.getJSONObject(i);
                            Log.d(TAG, "Processing habit JSON: " + habitJson.toString());
                            
                            Habit habit = Habit.fromJson(habitJson);
                            Log.d(TAG, "Parsed habit: " + habit.getTitle() + " with verification method: " + habit.getVerificationMethod());
                            
                            serverHabits.add(habit);
                            
                            // Update cache
                            habitsCache.put(habit.getHabitId(), habit);
                            
                            // Save to local database
                            saveHabitToLocalDb(habit);
                        }
                        
                        Log.d(TAG, "Loaded " + serverHabits.size() + " habits from server");
                        
                        // Notify listener
                        if (listener != null && !serverHabits.isEmpty()) {
                            listener.onHabitsLoaded(serverHabits);
                        }
                        
                        // Calculate overall progress
                        calculateHabitProgress();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing habits from server: " + e.getMessage());
                        onError("Error parsing server response: " + e.getMessage());
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error loading habits from server: " + errorMessage);
                }
            }
        );
    }
    
    /**
     * Verify a habit as completed using checkbox method
     * This is the primary method called from UI for checkbox-type habits
     */
    public void verifyHabitWithCheckbox(String habitId, boolean isCompleted) {
        Habit habit = habitsCache.get(habitId);
        if (habit == null) {
            Log.e(TAG, "Habit not found in cache: " + habitId);
            // Try to load from local database
            habit = loadHabitFromLocalDb(habitId);
            if (habit == null) {
                Log.e(TAG, "Habit not found in local database either: " + habitId);
                if (listener != null) {
                    listener.onError("Habit not found");
                }
                return;
            }
        }
        
        // For checkbox type, accept any verification method (fallback behavior)
        // This ensures habits work even if verification method isn't set correctly
        String verificationMethod = habit.getVerificationMethod();
        if (verificationMethod == null || verificationMethod.isEmpty()) {
            verificationMethod = Habit.VERIFICATION_CHECKBOX;
            habit.setVerificationMethod(verificationMethod);
        }
        
        Log.d(TAG, "Verifying habit: " + habitId + " with method: " + verificationMethod + ", completed: " + isCompleted);
        
        // Update habit completion status
        habit.setCompleted(isCompleted);
        
        // If completed, increment total completions
        if (isCompleted) {
            habit.setTotalCompletions(habit.getTotalCompletions() + 1);
            // Update streak
            habit.setCurrentStreak(habit.getCurrentStreak() + 1);
        }
        
        // Update in cache
        habitsCache.put(habitId, habit);
        
        // Save to local database
        saveHabitToLocalDb(habit);
        
        // Notify listener
        if (listener != null) {
            listener.onHabitVerified(habit, isCompleted);
        }
        
        // Update streak on server
        updateHabitStreak(habitId, isCompleted);
        
        // Update overall progress
        calculateHabitProgress();
        
        // Upload to server and update XP
        updateHabitOnServer(habit, isCompleted);
    }
    
    /**
     * Load habit from local database
     */
    private Habit loadHabitFromLocalDb(String habitId) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String[] columns = {"habit_id", "user_id", "title", "description", "verification_method", 
                               "is_completed", "current_streak", "total_completions", "frequency"};
            String selection = "habit_id = ?";
            String[] selectionArgs = {habitId};
            Cursor cursor = db.query("habits", columns, selection, selectionArgs, null, null, null);
            
            if (cursor.moveToFirst()) {
                Habit habit = new Habit();
                habit.setHabitId(cursor.getString(cursor.getColumnIndexOrThrow("habit_id")));
                habit.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("user_id")));
                habit.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                habit.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                habit.setVerificationMethod(cursor.getString(cursor.getColumnIndexOrThrow("verification_method")));
                habit.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1);
                habit.setCurrentStreak(cursor.getInt(cursor.getColumnIndexOrThrow("current_streak")));
                habit.setTotalCompletions(cursor.getInt(cursor.getColumnIndexOrThrow("total_completions")));
                habit.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow("frequency")));
                cursor.close();
                
                // Add to cache
                habitsCache.put(habitId, habit);
                return habit;
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error loading habit from local DB: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Verify a habit as completed using location method
     */
    public void verifyHabitWithLocation(String habitId) {
        Habit habit = habitsCache.get(habitId);
        if (habit == null) {
            Log.e(TAG, "Habit not found in cache: " + habitId);
            habit = loadHabitFromLocalDb(habitId);
            if (habit == null) {
                if (listener != null) {
                    listener.onError("Habit not found");
                }
                return;
            }
        }
        
        Log.d(TAG, "Verifying location habit: " + habitId);
        
        // Mark as completed
        habit.setCompleted(true);
        habit.setTotalCompletions(habit.getTotalCompletions() + 1);
        habit.setCurrentStreak(habit.getCurrentStreak() + 1);
        
        // Update in cache
        habitsCache.put(habitId, habit);
        
        // Save to local database
        saveHabitToLocalDb(habit);
        
        // Notify listener
        if (listener != null) {
            listener.onHabitVerified(habit, true);
        }
        
        // Update streak
        updateHabitStreak(habitId, true);
        
        // Update overall progress
        calculateHabitProgress();
        
        // Upload to server
        updateHabitOnServer(habit, true);
    }
    
    /**
     * Verify a habit as completed using Pomodoro method
     */
    public void verifyHabitWithPomodoro(String habitId) {
        Habit habit = habitsCache.get(habitId);
        if (habit == null) {
            Log.e(TAG, "Habit not found in cache: " + habitId);
            habit = loadHabitFromLocalDb(habitId);
            if (habit == null) {
                if (listener != null) {
                    listener.onError("Habit not found");
                }
                return;
            }
        }
        
        Log.d(TAG, "Verifying pomodoro habit: " + habitId);
        
        // Mark as completed
        habit.setCompleted(true);
        habit.setTotalCompletions(habit.getTotalCompletions() + 1);
        habit.setCurrentStreak(habit.getCurrentStreak() + 1);
        
        // Update in cache
        habitsCache.put(habitId, habit);
        
        // Save to local database
        saveHabitToLocalDb(habit);
        
        // Notify listener
        if (listener != null) {
            listener.onHabitVerified(habit, true);
        }
        
        // Update streak
        updateHabitStreak(habitId, true);
        
        // Update overall progress
        calculateHabitProgress();
        
        // Upload to server
        updateHabitOnServer(habit, true);
    }
    
    /**
     * Update habit streak
     */
    private void updateHabitStreak(String habitId, boolean isCompleted) {
        try {
            Habit habit = habitsCache.get(habitId);
            if (habit == null) return;
            
            // Get today's date for the streak
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            
            // Prepare request data
            JSONObject requestData = new JSONObject();
            requestData.put("habit_id", habitId);
            requestData.put("user_id", getUserId());
            requestData.put("date", currentDate);
            requestData.put("completed", isCompleted ? 1 : 0);
            
            // Send to server
            networkManager.makePostRequest(
                "update_habit_streak.php",
                requestData,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            // Parse streak from response
                            int newStreak = 0;
                            if (response.has("current_streak")) {
                                newStreak = response.getInt("current_streak");
                            } else if (response.has("data") && response.getJSONObject("data").has("current_streak")) {
                                newStreak = response.getJSONObject("data").getInt("current_streak");
                            }
                            
                            // Update streak in habit
                            Habit updatedHabit = habitsCache.get(habitId);
                            if (updatedHabit != null) {
                                updatedHabit.setCurrentStreak(newStreak);
                                habitsCache.put(habitId, updatedHabit);
                                saveHabitToLocalDb(updatedHabit);
                            }
                            
                            // Update streak cache
                            streakCache.put(habitId, newStreak);
                            
                            // Notify listener
                            if (listener != null) {
                                listener.onHabitStreakUpdated(habitId, newStreak);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing streak response: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error updating streak: " + errorMessage);
                        
                        // Use local calculation as fallback
                        int newStreak = calculateLocalStreak(habitId, isCompleted);
                        
                        // Update streak in habit
                        Habit updatedHabit = habitsCache.get(habitId);
                        if (updatedHabit != null) {
                            updatedHabit.setCurrentStreak(newStreak);
                            habitsCache.put(habitId, updatedHabit);
                            saveHabitToLocalDb(updatedHabit);
                        }
                        
                        // Update streak cache
                        streakCache.put(habitId, newStreak);
                        
                        // Notify listener
                        if (listener != null) {
                            listener.onHabitStreakUpdated(habitId, newStreak);
                        }
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error updating habit streak: " + e.getMessage());
        }
    }
    
    /**
     * Calculate habit streak locally as a fallback
     */
    private int calculateLocalStreak(String habitId, boolean isCompleted) {
        Habit habit = habitsCache.get(habitId);
        if (habit == null) return 0;
        
        int currentStreak = habit.getCurrentStreak();
        
        if (isCompleted) {
            // Increment streak if completed
            return currentStreak + 1;
        } else {
            // Reset streak if not completed
            return 0;
        }
    }
    
    /**
     * Update habit on server and update local XP
     */
    private void updateHabitOnServer(Habit habit, boolean isCompleted) {
        try {
            // Prepare update data
            JSONObject updateData = new JSONObject();
            updateData.put("habit_id", habit.getHabitId());
            updateData.put("user_id", habit.getUserId());
            updateData.put("completed", isCompleted ? 1 : 0);
            updateData.put("current_streak", habit.getCurrentStreak());
            updateData.put("total_completions", habit.getTotalCompletions());
            updateData.put("verification_type", habit.getVerificationMethod());
            
            // Current date
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            updateData.put("date", currentDate);
            
            // Send to server
            networkManager.makePostRequest(
                "update_habit.php",
                updateData,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d(TAG, "Habit updated on server: " + response.toString());
                        
                        // Update local XP if successful
                        try {
                            if (response.has("data")) {
                                JSONObject data = response.getJSONObject("data");
                                if (data.has("xp_earned")) {
                                    float xpEarned = (float) data.getDouble("xp_earned");
                                    if (xpEarned > 0) {
                                        updateLocalXP(xpEarned, habit.getVerificationMethod());
                                    }
                                }
                                if (data.has("streak")) {
                                    int newStreak = data.getInt("streak");
                                    habit.setCurrentStreak(newStreak);
                                    habitsCache.put(habit.getHabitId(), habit);
                                    if (listener != null) {
                                        listener.onHabitStreakUpdated(habit.getHabitId(), newStreak);
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing server response: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error updating habit on server: " + errorMessage);
                        // Still update local XP even if server fails (offline support)
                        if (isCompleted) {
                            float xpEarned = getXPForVerificationType(habit.getVerificationMethod());
                            updateLocalXP(xpEarned, habit.getVerificationMethod());
                        }
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error preparing habit update: " + e.getMessage());
            // Still update local XP on error
            if (isCompleted) {
                float xpEarned = getXPForVerificationType(habit.getVerificationMethod());
                updateLocalXP(xpEarned, habit.getVerificationMethod());
            }
        }
    }
    
    /**
     * Get XP reward for verification type
     */
    private float getXPForVerificationType(String verificationType) {
        switch (verificationType) {
            case Habit.VERIFICATION_POMODORO:
                return 2.0f;
            case Habit.VERIFICATION_LOCATION:
                return 1.5f;
            case Habit.VERIFICATION_CHECKBOX:
            default:
                return 1.0f;
        }
    }
    
    /**
     * Update local XP in SharedPreferences
     */
    private void updateLocalXP(float xpEarned, String verificationType) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Update total XP
        float totalXP = prefs.getFloat("xp_points_float", 0f);
        totalXP += xpEarned;
        editor.putFloat("xp_points_float", totalXP);
        editor.putInt("xp_points", (int) totalXP);
        editor.putInt("xp_coins", (int) totalXP); // Also update xp_coins for HomeFragment
        
        // Update habit-specific XP
        float habitXP = prefs.getFloat("habit_xp", 0f);
        habitXP += xpEarned;
        editor.putFloat("habit_xp", habitXP);
        
        // Update habit completion count
        int habitCount = prefs.getInt("habit_completed_count", 0);
        editor.putInt("habit_completed_count", habitCount + 1);
        
        editor.apply();
        
        Log.d(TAG, "Updated local XP: +" + xpEarned + " (Total: " + totalXP + ", Habit XP: " + habitXP + ")");
    }
    
    /**
     * Calculate progress for all habits
     */
    public void calculateHabitProgress() {
        int totalHabits = 0;
        int completedHabits = 0;
        Map<String, Float> habitProgress = new HashMap<>();
        
        // Get habits from cache
        for (Habit habit : habitsCache.values()) {
            totalHabits++;
            if (habit.isCompleted()) {
                completedHabits++;
            }
            
            // Calculate individual habit progress based on streak and total completions
            float habitScore = calculateHabitScore(habit);
            habitProgress.put(habit.getHabitId(), habitScore);
        }
        
        // Calculate overall progress percentage
        float overallProgress = totalHabits > 0 ? ((float) completedHabits / totalHabits) * 100 : 0;
        
        // Notify listener
        if (listener != null) {
            listener.onProgressUpdated(overallProgress, habitProgress);
        }
    }
    
    /**
     * Calculate a habit's score based on streak and completions
     */
    private float calculateHabitScore(Habit habit) {
        // Base score from completion status
        float score = habit.isCompleted() ? 100 : 0;
        
        // Bonus for streak
        if (habit.getCurrentStreak() > 0) {
            // Add bonus points for streak up to a maximum of 100 points total
            float streakBonus = Math.min(habit.getCurrentStreak() * 5, 50);
            score = Math.min(100, score + streakBonus);
        }
        
        return score;
    }
    
    /**
     * Get a habit by ID
     */
    public Habit getHabit(String habitId) {
        return habitsCache.get(habitId);
    }
    
    /**
     * Delete a habit
     */
    public void deleteHabit(String habitId) {
        try {
            // Remove from cache
            habitsCache.remove(habitId);
            
            // Remove from local database
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("habits", "habit_id = ?", new String[]{habitId});
            
            // Delete from server
            JSONObject deleteData = new JSONObject();
            deleteData.put("habit_id", habitId);
            deleteData.put("user_id", getUserId());
            
            networkManager.makePostRequest(
                "delete_habit.php",
                deleteData,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d(TAG, "Habit deleted from server: " + response.toString());
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error deleting habit from server: " + errorMessage);
                    }
                }
            );
            
            // Recalculate progress
            calculateHabitProgress();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting habit: " + e.getMessage());
        }
    }
    
    /**
     * Clear local habit cache and reload from server to fix trust_type issues
     * This method forces a fresh reload of all habits from the server to ensure
     * that verification methods are correctly applied based on current server data
     */
    public void clearCacheAndReload() {
        try {
            Log.d(TAG, "Clearing habit cache and reloading from server...");
            
            // Clear in-memory cache
            habitsCache.clear();
            streakCache.clear();
            
            // Clear local database
            clearHabitsFromLocalDb();
            
            // Force reload from server
            loadHabitsFromServer();
            
            Log.d(TAG, "Habit cache cleared and reload initiated");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing habit cache: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error refreshing habits: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clear all habits from local database
     */
    private void clearHabitsFromLocalDb() {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String userId = getUserId();
            
            if (!userId.isEmpty()) {
                // Delete habits for the current user
                int deletedCount = db.delete("habits", "user_id = ?", new String[]{userId});
                Log.d(TAG, "Cleared " + deletedCount + " habits from local database");
            } else {
                Log.w(TAG, "User ID is empty, cannot clear habits from local database");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing habits from local database: " + e.getMessage());
        }
    }
    
    /**
     * Refresh habits by clearing cache and reloading from server
     * Public method for external use to trigger cache refresh
     */
    public void refreshHabits() {
        Log.d(TAG, "refreshHabits() called - initiating cache clear and reload");
        clearCacheAndReload();
    }
}