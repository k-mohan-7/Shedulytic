package com.example.shedulytic;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class IpV4Connection {
    private static final String TAG = "IpV4Connection";
    
    // Use direct server connection URLs prioritizing your actual IP
    private static final String BASE_URL = "http://10.95.189.64/shedulytic/";
    // Additional server URLs with your actual IP as priority
    private static final String[] SERVER_URLS = {
        "http://10.95.189.64/shedulytic/",
        "http://localhost/shedulytic/",
        "http://10.0.2.2/shedulytic/"
    };

    private static String cachedWorkingUrl = null;
    private static long lastConnectionCheck = 0;
    private static final long CONNECTION_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(30); // Check every 30 seconds
    private static int currentUrlIndex = 0;

    /**
     * Get a working server URL, checking multiple options
     */
    public static String getBaseUrl() {
        long currentTime = System.currentTimeMillis();
        
        // Use cached URL only for a short time to avoid stale connections
        if (cachedWorkingUrl != null && (currentTime - lastConnectionCheck) < CONNECTION_CHECK_INTERVAL) {
            return cachedWorkingUrl;
        }

        // Try all URLs to find a working one
        for (int i = 0; i < SERVER_URLS.length; i++) {
            int index = (currentUrlIndex + i) % SERVER_URLS.length;
            String url = SERVER_URLS[index];
            
            if (isUrlReachable(url)) {
                cachedWorkingUrl = url;
                lastConnectionCheck = currentTime;
                currentUrlIndex = index;
                Log.i(TAG, "Found working server: " + url);
                return url;
            }
        }

        // If no URLs worked, return the default but force a new URL next time
        Log.e(TAG, "No servers reachable, using default URL");
        currentUrlIndex = (currentUrlIndex + 1) % SERVER_URLS.length;
        return BASE_URL;
    }

    /**
     * Get a fallback URL different from the current base URL
     */
    public static String getFallbackUrl() {
        // Force a server check
        lastConnectionCheck = 0;
        // Get a fresh working URL
        return getBaseUrl();
    }

    /**
     * Check if a server URL is reachable
     */
    private static boolean isUrlReachable(String urlString) {
        HttpURLConnection connection = null;
        try {
            // Try with direct endpoint and ping.php
            String[] pathsToTry = {
                "ping.php",
                ""
            };
            
            for (String path : pathsToTry) {
                if (connection != null) {
                    connection.disconnect();
                }
                
                String fullUrl = urlString + path;
                
                URL url = new URL(fullUrl);
                Log.d(TAG, "Testing connection to: " + fullUrl);
                
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000); // Quick timeout for testing
                connection.setReadTimeout(3000);
                connection.setRequestMethod("GET");
                connection.setDoOutput(false);
                connection.setUseCaches(false);
                connection.setInstanceFollowRedirects(true);
                
                // Set required headers
                connection.setRequestProperty("Connection", "close");
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("Cache-Control", "no-cache");
                
                try {
                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "Response code from " + fullUrl + ": " + responseCode);
                    
                    // Accept any standard successful response
                    if (responseCode >= 200 && responseCode < 400) {
                        Log.i(TAG, "Successfully connected to " + fullUrl);
                        return true;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Connection error for " + fullUrl + ": " + e.getMessage());
                    // Continue trying other paths
                }
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking URL: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // --- URL generation methods ---

    /**
     * Get URL for user streak data
     */
    public static String getUserStreakUrl(String userId, String startDate, String endDate) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        String url = getBaseUrl() + "get_user_streak.php?user_id=" + userId 
                + "&start_date=" + startDate + "&end_date=" + endDate;
        Log.d(TAG, "User streak URL: " + url);
        return url;
    }

    /**
     * Get URL for user activity logging
     */
    public static String getLogUserActivityUrl() {
        return getBaseUrl() + "log_user_activity.php";
    }

    /**
     * Get URL for user profile
     */
    public static String getUserProfileUrl(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        String url = getBaseUrl() + "get_user_profile.php?user_id=" + userId;
        Log.d(TAG, "User profile URL: " + url);
        return url;
    }

    /**
     * Get URL for habit streak
     */
    public static String getHabitStreakUrl(String taskId, String userId) {
        String url = getBaseUrl() + "habit_streaks.php?task_id=" + taskId + "&user_id=" + userId;
        Log.d(TAG, "Habit streak URL: " + url);
        return url;
    }

    /**
     * Get URL for updating habit completion
     */
    public static String getUpdateHabitCompletionUrl() {
        return getBaseUrl() + "habit_streaks.php";
    }

    /**
     * Get URL for adding a task
     */
    public static String getAddTaskUrl() {
        String url = getBaseUrl() + "add_task.php";
        Log.d(TAG, "Add task URL: " + url);
        return url;
    }

    /**
     * Get URL for updating a task
     */
    public static String getUpdateTaskUrl() {
        return getBaseUrl() + "update_task.php";
    }

    /**
     * Get URL for deleting a task
     */
    public static String getDeleteTaskUrl() {
        return getBaseUrl() + "delete_task.php";
    }

    /**
     * Get URL for today's tasks
     */
    public static String getTodayTasksUrl(String userId, String date) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Ensure date is properly formatted
        if (date == null || date.isEmpty()) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", 
                    java.util.Locale.getDefault());
            date = dateFormat.format(new java.util.Date());
        }
        
        String url = getBaseUrl() + "get_today_tasks.php?user_id=" + userId + "&date=" + date;
        Log.d(TAG, "Today tasks URL: " + url);
        return url;
    }
    
    /**
     * Get URL for all tasks
     */
    public static String getAllTasksUrl(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        String url = getBaseUrl() + "get_all_tasks.php?user_id=" + userId;
        Log.d(TAG, "All tasks URL: " + url);
        return url;
    }
    
    /**
     * Check if network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        return isDeviceNetworkAvailable(context);
    }
    
    /**
     * Check if device network is available
     */
    public static boolean isDeviceNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}