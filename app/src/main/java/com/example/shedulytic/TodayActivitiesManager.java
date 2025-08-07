package com.example.shedulytic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.TextView;
import android.app.Activity;

public class TodayActivitiesManager {
    private static final String TAG = "TodayActivitiesManager";
    private final Context context;
    private final TodayActivitiesListener listener;
    private final String userId;
    private final VolleyNetworkManager networkManager;

    public interface TodayActivitiesListener {
        void onActivitiesLoaded(List<TaskItem> activities);
        void onUserProfileLoaded(String name, int streakCount, String avatarUrl);
        void onStreakDataLoaded(Map<String, Boolean> streakData);
        void onError(String message);
    }

    public TodayActivitiesManager(Context context, String userId, TodayActivitiesListener listener) {
        this.context = context;
        this.userId = userId;
        this.listener = listener;
        this.networkManager = VolleyNetworkManager.getInstance(context);

        // Load user profile data when initialized
        loadUserProfile();
        
        // Load streak data for calendar
        loadStreakData();
    }
    
    /**
     * Loads streak data for the calendar display
     */
    public void loadStreakData() {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot load streak data: User ID is null or empty");
            listener.onError("User ID is not available");
            return;
        }
        
        // Check if network is available using NetworkUtils instead of IpV4Connection
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.e(TAG, "Cannot load streak data: Network is unavailable");
            // Use cached data if available
            createDefaultStreakData(getCurrentDate(), getCurrentDate());
            return;
        }
        
        // Calculate date range for streak data (last 30 days)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = new Date();
        Date startDate = new Date(today.getTime() - 29 * 24 * 60 * 60 * 1000L); // 30 days ago
        
        String startDateStr = dateFormat.format(startDate);
        String endDateStr = dateFormat.format(today);
        
        // Get the streak URL using VolleyNetworkManager
        String endpoint = networkManager.getUserStreakUrl(userId, startDateStr, endDateStr);
        
        Log.d(TAG, "Loading streak data with endpoint: " + endpoint);
        
        // Make the server request with Volley
        networkManager.makeGetRequest(endpoint, new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "Streak data response: " + response.toString());
                processStreakData(response, startDateStr, endDateStr);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading streak data: " + errorMessage);
                createDefaultStreakData(startDateStr, endDateStr);
            }
        });
    }
    
    /**
     * Process streak data from JSON response with enhanced error handling
     */
    private void processStreakData(JSONObject response, String startDateStr, String endDateStr) {
        try {
            Map<String, Boolean> streakMap = new HashMap<>();
            final int streakCount;
            
            Log.d(TAG, "Processing streak data response: " + response.toString());
            
            // Extract streak count
            if (response.has("streak_count")) {
                streakCount = response.getInt("streak_count");
                Log.d(TAG, "Found streak_count in root: " + streakCount);
            } else if (response.has("data") && response.getJSONObject("data").has("streak_count")) {
                streakCount = response.getJSONObject("data").getInt("streak_count");
                Log.d(TAG, "Found streak_count in data object: " + streakCount);
            } else if (response.has("count")) {
                streakCount = response.getInt("count");
                Log.d(TAG, "Found count in root: " + streakCount);
            } else if (response.has("streak")) {
                streakCount = response.getInt("streak");
                Log.d(TAG, "Found streak in root: " + streakCount);
            } else {
                streakCount = 0;
            }
            
            // Look for streak data in various formats
            JSONArray streakDataArray = null;
            
            if (response.has("streak_data")) {
                streakDataArray = response.getJSONArray("streak_data");
                Log.d(TAG, "Found streak_data in root, entries: " + streakDataArray.length());
            } else if (response.has("data") && response.getJSONObject("data").has("streak_data")) {
                streakDataArray = response.getJSONObject("data").getJSONArray("streak_data");
                Log.d(TAG, "Found streak_data in data object, entries: " + streakDataArray.length());
            } else if (response.has("days")) {
                streakDataArray = response.getJSONArray("days");
                Log.d(TAG, "Found days in root, entries: " + streakDataArray.length());
            } else if (response.has("data") && response.getJSONObject("data").has("days")) {
                streakDataArray = response.getJSONObject("data").getJSONArray("days");
                Log.d(TAG, "Found days in data object, entries: " + streakDataArray.length());
            } else if (response.has("dates")) {
                streakDataArray = response.getJSONArray("dates");
                Log.d(TAG, "Found dates in root, entries: " + streakDataArray.length());
            } else if (response.has("calendar")) {
                streakDataArray = response.getJSONArray("calendar");
                Log.d(TAG, "Found calendar in root, entries: " + streakDataArray.length());
            }
            
            // Process streak data array if found
            if (streakDataArray != null && streakDataArray.length() > 0) {
                for (int i = 0; i < streakDataArray.length(); i++) {
                    JSONObject dayData = streakDataArray.getJSONObject(i);
                    
                    // Extract date
                    String date = null;
                    if (dayData.has("date")) {
                        date = dayData.getString("date");
                    } else if (dayData.has("completion_date")) {
                        date = dayData.getString("completion_date");
                    } else if (dayData.has("day")) {
                        date = dayData.getString("day");
                    } else if (dayData.has("calendar_date")) {
                        date = dayData.getString("calendar_date");
                    }
                    
                    if (date != null) {
                        boolean hasActivity = false;
                        
                        // Check various fields for activity status
                        if (dayData.has("has_activity")) {
                            hasActivity = dayData.getBoolean("has_activity");
                        } else if (dayData.has("activity_count")) {
                            hasActivity = dayData.getInt("activity_count") > 0;
                        } else if (dayData.has("count")) {
                            hasActivity = dayData.getInt("count") > 0;
                        } else if (dayData.has("completed")) {
                            hasActivity = dayData.getBoolean("completed");
                        } else if (dayData.has("has_streak")) {
                            hasActivity = dayData.getBoolean("has_streak");
                        } else if (dayData.has("active")) {
                            hasActivity = dayData.getBoolean("active");
                        } else {
                            // Default to true if the date exists in the streak data but has no activity flag
                            hasActivity = true;
                        }
                        
                        streakMap.put(date, hasActivity);
                        Log.d(TAG, "Added streak data for date: " + date + " hasActivity: " + hasActivity);
                    }
                }
            }
            
            // If no streak data was found, try parsing from object format
            if (streakMap.isEmpty() && response.has("data")) {
                JSONObject dataObj = response.getJSONObject("data");
                Iterator<String> keys = dataObj.keys();
                
                while (keys.hasNext()) {
                    String key = keys.next();
                    
                    // Skip the streak_count key
                    if (key.equals("streak_count") || key.equals("streak_data") || key.equals("days")) {
                        continue;
                    }
                    
                    // Check if this key might be a date (simple check)
                    if (key.contains("-") || key.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        try {
                            // Try to extract the value as a boolean or number
                            if (dataObj.optBoolean(key, false)) {
                                streakMap.put(key, true);
                                Log.d(TAG, "Added streak data from object for date: " + key + " hasActivity: true");
                            } else if (dataObj.optInt(key, 0) > 0) {
                                streakMap.put(key, true);
                                Log.d(TAG, "Added streak data from object for date: " + key + " hasActivity: true (from count)");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing potential date key: " + key, e);
                        }
                    }
                }
            }
            
            // Ensure we have data for today if streak count > 0
            String today = getCurrentDate();
            if (streakCount > 0 && !streakMap.containsKey(today)) {
                // If we have a streak count > 0, today should have activity
                streakMap.put(today, true);
                Log.d(TAG, "Added today's date with activity due to positive streak count");
            } else if (!streakMap.containsKey(today)) {
                // If today isn't in the map but should be in our date range, add it
                streakMap.put(today, false);
                Log.d(TAG, "Added today's date with no activity");
            }
            
            // Fill in any missing dates
            fillMissingDates(streakMap, startDateStr, endDateStr);
            
            // Store the streak count in SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            prefs.edit().putInt("streak_count", streakCount).apply();
            
            // Notify the listener
            if (listener != null) {
                Activity activity = context instanceof Activity ? (Activity) context : null;
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        try {
                            listener.onStreakDataLoaded(streakMap);
                            if (streakCount >= 0) {
                                listener.onUserProfileLoaded("", streakCount, "");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error notifying listener about streak data", e);
                        }
                    });
                } else {
                    listener.onStreakDataLoaded(streakMap);
                    if (streakCount >= 0) {
                        listener.onUserProfileLoaded("", streakCount, "");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing streak data: " + e.getMessage(), e);
            
            // Create default data on error
            createDefaultStreakData(startDateStr, endDateStr);
        }
    }
    
    /**
     * Create default streak data when server requests fail
     */
    private void createDefaultStreakData(String startDateStr, String endDateStr) {
        try {
            Log.d(TAG, "Creating default streak data from " + startDateStr + " to " + endDateStr);
            
            Map<String, Boolean> streakMap = new HashMap<>();
            
            // Parse the start and end dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);
            
            if (startDate == null || endDate == null) {
                throw new Exception("Unable to parse dates");
            }
            
            // Calculate the current day streak from local data or SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            final int streakCount = prefs.getInt("streak_count", 0);
            
            // Create a calendar instance
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            
            // Fill the map with default values
            while (!calendar.getTime().after(endDate)) {
                String date = dateFormat.format(calendar.getTime());
                
                // Default to false
                boolean hasActivity = false;
                
                // If we have a streak, mark the last 'streakCount' days as having activity
                if (streakCount > 0) {
                    // Get today's date
                    String today = dateFormat.format(new Date());
                    
                    // Calculate the date 'streakCount' days ago
                    Calendar streakStartCal = Calendar.getInstance();
                    streakStartCal.add(Calendar.DAY_OF_MONTH, -(streakCount - 1));
                    String streakStartDate = dateFormat.format(streakStartCal.getTime());
                    
                    // If this date is within the streak range, mark it as having activity
                    if ((date.compareTo(streakStartDate) >= 0 && date.compareTo(today) <= 0)) {
                        hasActivity = true;
                    }
                }
                
                streakMap.put(date, hasActivity);
                
                // Move to the next day
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            // Notify the listener
            if (listener != null) {
                Activity activity = context instanceof Activity ? (Activity) context : null;
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        try {
                            listener.onStreakDataLoaded(streakMap);
                            if (streakCount >= 0) {
                                listener.onUserProfileLoaded("", streakCount, "");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error notifying listener about default streak data", e);
                        }
                    });
                } else {
                    listener.onStreakDataLoaded(streakMap);
                    if (streakCount >= 0) {
                        listener.onUserProfileLoaded("", streakCount, "");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating default streak data: " + e.getMessage(), e);
            
            if (listener != null) {
                listener.onError("Could not load streak data: " + e.getMessage());
            }
        }
    }
    
    /**
     * Fill in any missing dates in the streak map with false values
     */
    private void fillMissingDates(Map<String, Boolean> streakMap, String startDateStr, String endDateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);
            
            if (startDate != null && endDate != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);
                
                while (!calendar.getTime().after(endDate)) {
                    String date = dateFormat.format(calendar.getTime());
                    
                    // Only add if not already present
                    if (!streakMap.containsKey(date)) {
                        streakMap.put(date, false);
                    }
                    
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filling missing dates: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get current date in yyyy-MM-dd format
     */
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }
    
    /**
     * Load user profile data
     */
    public void loadUserProfile() {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot load user profile: User ID is null or empty");
            useDefaultUserProfile();
            return;
        }
        
        String endpoint = networkManager.getUserProfileUrl(userId);
        
        Log.d(TAG, "Loading user profile with endpoint: " + endpoint);
        
        networkManager.makeGetRequest(endpoint, new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "User profile response: " + response.toString());
                try {
                    // Extract user name
                    String userName = "";
                    if (response.has("name")) {
                        userName = response.getString("name");
                    } else if (response.has("username")) {
                        userName = response.getString("username");
                    } else if (response.has("user") && response.getJSONObject("user").has("name")) {
                        userName = response.getJSONObject("user").getString("name");
                    }
                    
                    // Extract streak count
                    int streakCount = 0;
                    if (response.has("streak_count")) {
                        streakCount = response.getInt("streak_count");
                    } else if (response.has("streak")) {
                        streakCount = response.getInt("streak");
                    } else if (response.has("user") && response.getJSONObject("user").has("streak_count")) {
                        streakCount = response.getJSONObject("user").getInt("streak_count");
                    }
                    
                    // Extract avatar URL
                    String avatarUrl = "";
                    if (response.has("avatar")) {
                        avatarUrl = response.getString("avatar");
                    } else if (response.has("avatar_url")) {
                        avatarUrl = response.getString("avatar_url");
                    } else if (response.has("user") && response.getJSONObject("user").has("avatar")) {
                        avatarUrl = response.getJSONObject("user").getString("avatar");
                    }
                    
                    if (listener != null) {
                        listener.onUserProfileLoaded(userName, streakCount, avatarUrl);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing user profile: " + e.getMessage());
                    useDefaultUserProfile();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading user profile: " + errorMessage);
                useDefaultUserProfile();
            }
        });
    }
    
    /**
     * Use default user profile when server data is unavailable
     */
    private void useDefaultUserProfile() {
        String name = "User";
        int streakCount = 0;
        String avatarUrl = "";
        
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        name = prefs.getString("user_name", "User");
        
        listener.onUserProfileLoaded(name, streakCount, avatarUrl);
    }
    
    /**
     * Load today's activities
     */
    public void loadTodayActivities() {
        if (userId == null || userId.isEmpty()) {
            listener.onError("User ID is not available");
            return;
        }
        
        // Get today's date
        String currentDate = getCurrentDate();
        
        // First check if we have cached data and show it immediately
        TaskManager taskManager = new TaskManager(context, new TaskManager.TaskListener() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                // Convert Task objects to TaskItem objects
                List<TaskItem> taskItems = new ArrayList<>();
                for (Task task : tasks) {
                    if (currentDate.equals(task.getDueDate())) {
                        TaskItem item = new TaskItem(
                            task.getTaskId(),
                            task.getTitle(),
                            task.getDescription(),
                            formatTimeRange(task.getStartTime(), task.getEndTime()),
                            calculateDuration(task.getStartTime(), task.getEndTime()),
                            task.isCompleted() ? "completed" : "pending",
                            task.getPriority(),
                            task.getCurrentStreak()
                        );
                        
                        // Set the task type
                        item.setType(task.getType());
                        
                        // Store the start/end time on the object for use elsewhere
                        item.setStartTime(task.getStartTime());
                        item.setEndTime(task.getEndTime());
                        
                        taskItems.add(item);
                    }
                }
                
                // Update UI with cached data first
                if (listener != null && !taskItems.isEmpty()) {
                    listener.onActivitiesLoaded(taskItems);
                }
                
                // Then fetch fresh data from server
                fetchActivitiesFromServer(currentDate);
            }
            
            @Override public void onTaskAdded(Task task) {}
            @Override public void onTaskUpdated(Task task) {}
            @Override public void onTaskDeleted(String taskId) {}
            @Override public void onHabitStreakUpdated(String taskId, int newStreak) {}
            @Override public void onError(String message) {
                // If local cache fails, try server directly
                fetchActivitiesFromServer(currentDate);
            }
        });
        
        // Use the faster loading method
        taskManager.fastLoadTasks(currentDate);
    }
    
    /**
     * Fetch activities from server with optimized network request (tasks only, no habits)
     */
    private void fetchActivitiesFromServer(String currentDate) {
        // Get today's tasks URL using VolleyNetworkManager
        String tasksEndpoint = networkManager.getTodayTasksUrl(userId, currentDate);
        
        Log.d(TAG, "Loading today's activities with tasks endpoint: " + tasksEndpoint);
        
        // Load only tasks (no habits)
        networkManager.makeGetRequest(tasksEndpoint, new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "Today's tasks response: " + response.toString());
                
                List<TaskItem> tasks = new ArrayList<>();
                
                try {
                    // Look for tasks in various formats
                    JSONArray tasksArray = null;
                    
                    if (response.has("tasks")) {
                        tasksArray = response.getJSONArray("tasks");
                    } else if (response.has("data")) {
                        // Check if the data field contains a tasks array
                        if (response.getJSONObject("data").has("tasks")) {
                        tasksArray = response.getJSONObject("data").getJSONArray("tasks");
                        } else {
                            // The data field might be the tasks array directly
                            tasksArray = response.getJSONArray("data");
                        }
                    } else if (response.has("activities")) {
                        tasksArray = response.getJSONArray("activities");
                    } else if (response.has("items")) {
                        tasksArray = response.getJSONArray("items");
                    }
                    
                    // Process the tasks if we found an array
                    if (tasksArray != null) {
                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskObject = tasksArray.getJSONObject(i);
                            
                            // Extract task type and skip habits (shouldn't be here anymore, but safety check)
                            String taskType = taskObject.optString("type", "");
                            if (taskType.isEmpty() && taskObject.has("task_type")) {
                                taskType = taskObject.getString("task_type");
                            }
                            if ("habit".equalsIgnoreCase(taskType)) {
                                continue; // Skip habits
                            }
                            
                            // Extract task ID
                            String taskId = taskObject.optString("task_id", "");
                            if (taskId.isEmpty() && taskObject.has("id")) {
                                taskId = taskObject.getString("id");
                            }
                            
                            // Extract basic task info
                            String title = taskObject.optString("title", "Unnamed Task");
                            String description = taskObject.optString("description", "");
                            
                            // Normalize task type for workflow and remainder only
                            taskType = taskType.toLowerCase();
                            if (!taskType.equals("workflow") && !taskType.equals("remainder")) {
                                taskType = "remainder"; // Default to remainder for tasks
                            }
                            
                            // Extract time info
                            String startTime = taskObject.optString("start_time", "00:00");
                            String endTime = taskObject.optString("end_time", "23:59");
                            String duration = calculateDuration(startTime, endTime);
                            String timeRange = formatTimeRange(startTime, endTime);
                            
                            // Get status (default to "pending")
                            String status = taskObject.optString("status", "pending");
                            
                            // Get priority (default to "medium") 
                            String priority = taskObject.optString("priority", "medium");
                            
                            // Get streak (default to 0)
                            int streak = taskObject.optInt("current_streak", 0);
                            
                            // Create TaskItem and add to list
                            TaskItem task = new TaskItem(
                                taskId,
                                title,
                                description,
                                timeRange,
                                duration,
                                status,
                                priority,
                                streak
                            );
                            
                            // Set the task type
                            task.setType(taskType);
                            
                            // Store the start/end time on the object for use elsewhere
                            task.setStartTime(startTime);
                            task.setEndTime(endTime);
                            
                            tasks.add(task);
                        }
                    }
                    
                    // Notify listener with tasks only
                    if (listener != null) {
                        listener.onActivitiesLoaded(tasks);
                    }
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing tasks: " + e.getMessage());
                    if (listener != null) {
                        listener.onError("Error loading today's activities");
                    }
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading today's tasks: " + errorMessage);
                if (listener != null) {
                    listener.onError("Error: " + errorMessage);
                }
            }
        });
    }
    
    /**
     * Format time range for display (e.g., "9:00 AM - 10:30 AM")
     */
    private String formatTimeRange(String startTime, String endTime) {
        try {
            // Check if we have a full datetime string (YYYY-MM-DD HH:MM:SS)
            if (startTime.contains(" ") && endTime.contains(" ")) {
                // Extract just the time part
                startTime = startTime.split(" ")[1];
                endTime = endTime.split(" ")[1];
            }
            
            // Handle case when time doesn't include seconds
            if (startTime.split(":").length < 3) {
                startTime = startTime + ":00";
            }
            if (endTime.split(":").length < 3) {
                endTime = endTime + ":00";
            }
            
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            
            Date startDate = inputFormat.parse(startTime);
            Date endDate = inputFormat.parse(endTime);
            
            if (startDate != null && endDate != null) {
                return outputFormat.format(startDate) + " - " + outputFormat.format(endDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time range: " + e.getMessage(), e);
        }
        
        return startTime + " - " + endTime;
    }
    
    /**
     * Calculate duration between start and end times (e.g., "1h 30m")
     */
    private String calculateDuration(String startTime, String endTime) {
        try {
            // Check if we have a full datetime string (YYYY-MM-DD HH:MM:SS)
            if (startTime.contains(" ") && endTime.contains(" ")) {
                // Extract just the time part
                startTime = startTime.split(" ")[1];
                endTime = endTime.split(" ")[1];
            }
            
            // Handle case when time doesn't include seconds
            if (startTime.split(":").length < 3) {
                startTime = startTime + ":00";
            }
            if (endTime.split(":").length < 3) {
                endTime = endTime + ":00";
            }
            
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date startDate = format.parse(startTime);
            Date endDate = format.parse(endTime);
            
            if (startDate != null && endDate != null) {
                long durationMillis = endDate.getTime() - startDate.getTime();
                long hours = durationMillis / (60 * 60 * 1000);
                long minutes = (durationMillis % (60 * 60 * 1000)) / (60 * 1000);
                
                if (hours > 0) {
                    return hours + "h " + (minutes > 0 ? minutes + "m" : "");
                } else {
                    return minutes + "m";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating duration: " + e.getMessage(), e);
        }
        
        return "1h"; // Default duration if calculation fails
    }
    
    /**
     * Loads today's activities more quickly with optimized performance
     */
    public void loadTodayActivitiesQuickly() {
        if (userId == null || userId.isEmpty()) {
            listener.onError("User ID is not available");
            return;
        }
        
        // Get today's date
        String currentDate = getCurrentDate();
        
        // First, try to load from local cache for immediate display
        TaskManager localCache = new TaskManager(context, new TaskManager.TaskListener() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                // Convert tasks to TaskItems for consistent interface
                List<TaskItem> taskItems = new ArrayList<>();
                for (Task task : tasks) {
                    // Get task type, ensure it's lowercase
                    String taskType = task.getType().toLowerCase();
                    if (!taskType.equals("workflow") && !taskType.equals("remainder") && !taskType.equals("habit")) {
                        taskType = "remainder"; // Default to remainder if unknown
                    }
                    
                    // Use the constructor that takes a type parameter
                    TaskItem item = new TaskItem(
                        task.getTaskId(),
                        task.getTitle(),
                        taskType, // Pass the type explicitly
                        task.getDescription(),
                        formatTimeRange(task.getStartTime(), task.getEndTime()),
                        calculateDuration(task.getStartTime(), task.getEndTime()),
                        task.getStatus(),
                        task.getPriority(),
                        task.getCurrentStreak()
                    );
                    
                    // Store the start/end time on the object for use elsewhere
                    item.setStartTime(task.getStartTime());
                    item.setEndTime(task.getEndTime());
                    
                    taskItems.add(item);
                }
                
                // Always notify with the local cache first for immediate display
                if (!taskItems.isEmpty()) {
                    listener.onActivitiesLoaded(taskItems);
                    
                    // Then fetch server data in the background with a slightly longer timeout
                    fetchTodayActivitiesWithTimeout(currentDate, 3000);
                } else {
                    // If no cached data, fetch with a faster timeout
                    fetchTodayActivitiesWithTimeout(currentDate, 1500);
                }
            }
            
            @Override
            public void onTaskAdded(Task task) {}
            @Override
            public void onTaskUpdated(Task task) {}
            @Override
            public void onTaskDeleted(String taskId) {}
            @Override
            public void onHabitStreakUpdated(String taskId, int newStreak) {}
            @Override
            public void onError(String message) {
                // If local cache fails, try server directly
                fetchTodayActivitiesWithTimeout(currentDate, 1500);
            }
        });
        
        // Set a very short timeout for local cache to ensure UI is responsive
        localCache.fastLoadTasks(currentDate);
    }
    
    /**
     * Helper method to fetch activities from server with timeout
     */
    private void fetchTodayActivitiesWithTimeout(String currentDate, int timeoutMs) {
        // Get today's tasks URL using VolleyNetworkManager
        String endpoint = networkManager.getTodayTasksUrl(userId, currentDate);
        
        Log.d(TAG, "Fetching today's activities from server with endpoint: " + endpoint);
        
        networkManager.makeGetRequestWithTimeout(
            endpoint, 
            new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    List<TaskItem> activities = new ArrayList<>();
                    
                    // Look for tasks in various formats
                    JSONArray tasksArray = null;
                    
                    if (response.has("tasks")) {
                        tasksArray = response.getJSONArray("tasks");
                    } else if (response.has("data")) {
                        // Check if the data field contains a tasks array
                        JSONObject dataObj = response.getJSONObject("data");
                        if (dataObj.has("tasks")) {
                            tasksArray = dataObj.getJSONArray("tasks");
                        } else {
                            // The data field might be the tasks array directly
                            tasksArray = response.getJSONArray("data");
                        }
                    } else if (response.has("activities")) {
                        tasksArray = response.getJSONArray("activities");
                    } else if (response.has("items")) {
                        tasksArray = response.getJSONArray("items");
                    }                            // Process the tasks if we found an array
                            if (tasksArray != null) {
                                for (int i = 0; i < tasksArray.length(); i++) {
                                    JSONObject taskObject = tasksArray.getJSONObject(i);
                                    
                                    // Extract task ID
                                    String taskId = taskObject.optString("id", "");
                                    if (taskId.isEmpty() && taskObject.has("task_id")) {
                                        taskId = taskObject.getString("task_id");
                                    }
                                    
                                    // Extract basic task info
                                    String title = taskObject.optString("title", "Unnamed Task");
                                    String description = taskObject.optString("description", "");
                                    
                                    // Extract task type (ensure it's workflow or remainder only)
                                    String taskType = taskObject.optString("type", "");
                                    if (taskType.isEmpty() && taskObject.has("task_type")) {
                                        taskType = taskObject.getString("task_type");
                                    }
                                    // Normalize type to workflow or remainder only (no habits)
                                    taskType = taskType.toLowerCase();
                                    if (!taskType.equals("workflow") && !taskType.equals("remainder")) {
                                        taskType = "remainder"; // Default to remainder
                                    }
                                    
                                    // Extract time info
                                    String startTime = taskObject.optString("start_time", "00:00");
                                    String endTime = taskObject.optString("end_time", "23:59");
                                    String duration = calculateDuration(startTime, endTime);
                                    String timeRange = formatTimeRange(startTime, endTime);
                                    
                                    // Get status (default to "pending")
                                    String status = taskObject.optString("status", "pending");
                                    
                                    // Get priority (default to "medium") 
                                    String priority = taskObject.optString("priority", "medium");
                                    
                                    // Get streak (default to 0)
                                    int streak = taskObject.optInt("current_streak", 0);
                                    if (streak == 0 && taskObject.has("streak")) {
                                        streak = taskObject.getInt("streak");
                                    }
                                    
                                    // Create TaskItem with the explicit type
                                    TaskItem task = new TaskItem(
                                        taskId,
                                        title,
                                        taskType, // Use explicit type
                                        description,
                                        timeRange,
                                        duration,
                                        status,
                                        priority,
                                        streak
                                    );
                                    
                                    // Store the start/end time on the object for use elsewhere
                                    task.setStartTime(startTime);
                                    task.setEndTime(endTime);
                                    
                                    activities.add(task);
                                }
                            }
                            
                            // Notify the listener with the latest data only if we got tasks
                            if (listener != null && !activities.isEmpty()) {
                                listener.onActivitiesLoaded(activities);
                            }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing server activities: " + e.getMessage(), e);
                    // We don't notify the listener on error since we've already shown cached data
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Server error loading today's activities: " + errorMessage);
                // No need to notify listener - we've already shown cached data
            }
            },
            timeoutMs
        );
    }
    
    /**
     * Extract the time portion from a datetime string
     * Handles various datetime formats like "2025-05-13 00:00:00" or "00:00:00"
     * @param dateTime The datetime string to process
     * @return The time portion in "HH:mm:ss" format
     */
    private String extractTimeFromDateTime(String dateTime) {
        // If the string is empty or null, return a default time
        if (dateTime == null || dateTime.isEmpty()) {
            return "00:00:00";
        }
        
        // If it already contains a space (like "2025-05-13 00:00:00"), extract the time part
        if (dateTime.contains(" ")) {
            String[] parts = dateTime.split(" ");
            if (parts.length > 1) {
                return parts[1];
            }
            return "00:00:00";
        }
        
        // If it's just a time (like "14:30" or "14:30:00"), ensure it has seconds
        if (dateTime.matches("\\d{1,2}:\\d{1,2}")) {
            return dateTime + ":00";
        }
        
        // Return as is if it seems to be just a time with seconds
        return dateTime;
    }
    
    /**
     * Format a single time for display (e.g., "9:00 AM")
     */
    private String formatSingleTime(String time) {
        // Handle null or empty time
        if (time == null || time.trim().isEmpty()) {
            return "All Day";
        }
        
        try {
            // Check if we have a full datetime string (YYYY-MM-DD HH:MM:SS)
            if (time.contains(" ")) {
                // Extract just the time part
                time = time.split(" ")[1];
            }
            
            // Handle case when time doesn't include seconds
            if (time.split(":").length < 3) {
                time = time + ":00";
            }
            
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            
            Date timeDate = inputFormat.parse(time);
            
            if (timeDate != null) {
                return outputFormat.format(timeDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting single time: " + e.getMessage(), e);
        }
        
        return time;
    }
}
