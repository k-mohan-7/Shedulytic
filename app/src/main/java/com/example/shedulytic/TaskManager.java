package com.example.shedulytic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import android.text.TextUtils;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

public class TaskManager {
    private static final String TAG = "TaskManager";
    private final Context context;
    private final TaskListener listener;
    private final VolleyNetworkManager networkManager;
    private final NotificationHandler notificationHandler;
    private final ReminderNotificationManager reminderNotificationManager;

    public interface TaskListener {
        void onTasksLoaded(java.util.List<Task> tasks);
        void onTaskAdded(Task task);
        void onTaskUpdated(Task task);
        void onTaskDeleted(String taskId);
        void onHabitStreakUpdated(String taskId, int newStreak);
        void onError(String message);
    }    public TaskManager(Context context, TaskListener listener) {
        this.context = context;
        this.listener = listener;
        this.networkManager = VolleyNetworkManager.getInstance(context);
        this.notificationHandler = new NotificationHandler(context);
        this.reminderNotificationManager = new ReminderNotificationManager(context);
    }

    /**
     * Get the user ID from SharedPreferences
     * @return The user ID, or an empty string if not found
     */
    private String getUserId() {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        return prefs.getString("user_id", "");
    }

    /**
     * Get current date in YYYY-MM-DD format
     */
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    /**
     * Formats time for server in the format required by the backend
     * @param time Time string in 12-hour format (e.g., "08:00 AM")
     * @param date Date string in YYYY-MM-DD format
     * @return Formatted datetime string
     */
    private String formatTimeForServer(String time, String date) {
        if (time == null || time.isEmpty()) {
            return null;
        }
        
        try {
            // Parse the time components
            String[] timeParts = time.split(" ");
            if (timeParts.length < 2) {
                Log.e(TAG, "Invalid time format: " + time);
                return date + " 00:00:00"; // Default fallback
            }
            
            String[] hourMinute = timeParts[0].split(":");
            int hour = Integer.parseInt(hourMinute[0]);
            int minute = Integer.parseInt(hourMinute[1]);
            String amPm = timeParts[1];
            
            // Convert to 24-hour format
            if (amPm.equalsIgnoreCase("PM") && hour < 12) {
                hour += 12;
            } else if (amPm.equalsIgnoreCase("AM") && hour == 12) {
                hour = 0;
            }
            
            // Format as YYYY-MM-DD HH:MM:SS
            return String.format(Locale.US, "%s %02d:%02d:00", date, hour, minute);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time: " + e.getMessage());
            return date + " 00:00:00"; // Default fallback
        }
    }
    
    private String findBestWorkingAddTaskUrl() {
        // Use VolleyNetworkManager to get the add task URL
        VolleyNetworkManager networkManager = VolleyNetworkManager.getInstance(context);
        String addTaskUrl = networkManager.getAddTaskUrl();
        Log.d(TAG, "Using add task URL: " + addTaskUrl);
        return addTaskUrl;
    }

    // Function to add a new task with parallel URL attempts for high reliability
    public void addTask(Task task) {
        try {
            // Convert task to JSON
            JSONObject jsonTask = task.toJson();
            
            // Get the best working URL for adding tasks
            String directUrl = findBestWorkingAddTaskUrl();
            
            // Log the request with detailed information
            Log.d(TAG, "Adding task: " + jsonTask.toString());
            Log.d(TAG, "Task details - Date: " + task.getDueDate() + 
                    ", Time: " + task.getStartTime() + " to " + task.getEndTime() + 
                    ", Type: " + task.getType());
            
            // Use VolleyNetworkManager for the request
            VolleyNetworkManager networkManager = VolleyNetworkManager.getInstance(context);
            networkManager.makePostRequest(
                // Using the endpoint "add_task.php"
                "add_task.php",
                jsonTask,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            Log.d(TAG, "Task added successfully: " + response.toString());
                            
                            // Parse task ID from response if available
                            String taskId = task.getTaskId();
                            if (response.has("task_id")) {
                                taskId = response.getString("task_id");
                            } else if (response.has("id")) {
                                taskId = response.getString("id");
                            } else if (response.has("data") && response.getJSONObject("data").has("task_id")) {
                                taskId = response.getJSONObject("data").getString("task_id");
                            }
                            task.setTaskId(taskId);
                              // Save to local database for offline access
                            saveTaskToLocalDb(task);
                            
                            // Schedule notifications for the task
                            scheduleTaskNotifications(task);
                            
                            // Notify listener of successful add
                            if (listener != null) {
                                listener.onTaskAdded(task);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing add task response: " + e.getMessage());
                            if (listener != null) {
                                listener.onError("Error adding task: " + e.getMessage());
                            }
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error adding task: " + errorMessage);
                        
                        // Try direct IP address as fallback
                        tryAddTaskWithDirectIp(task, errorMessage);
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error adding task: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error adding task: " + e.getMessage());
            }
        }
    }

    /**
     * Update task completion status using the new completion system
     * @param taskId The ID of the task to update
     * @param isCompleted True if task is completed, false if uncompleted
     */
    public void updateTaskCompletion(String taskId, boolean isCompleted) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");
            String currentDate = getCurrentDate();

            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("task_id", taskId);
            requestBody.put("user_id", userId);
            requestBody.put("date", currentDate);

            String endpoint;
            if (isCompleted) {
                endpoint = networkManager.getTaskCompletionUrl();
                Log.d(TAG, "✓ Adding completion for task ID: " + taskId);
            } else {
                endpoint = networkManager.getTaskCompletionUrl() + "?action=remove";
                requestBody.put("action", "remove");
                Log.d(TAG, "✗ Removing completion for task ID: " + taskId);
            }

            networkManager.makePostRequest(
                endpoint,
                requestBody,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            Log.d(TAG, "Task completion update response: " + response.toString());
                              if (response.has("status") && response.getString("status").equals("success")) {
                                // Update local storage
                                updateLocalTaskCompletion(taskId, isCompleted ? "completed" : "pending", null);
                                
                                // Create updated task for notification
                                Task updatedTask = createTaskFromId(taskId);
                                if (updatedTask != null) {
                                    updatedTask.setStatus(isCompleted ? "completed" : "pending");
                                    
                                    // Cancel notifications if task is completed, reschedule if uncompleted
                                    if (isCompleted) {
                                        notificationHandler.cancelTaskNotifications(taskId);
                                        Log.d(TAG, "Cancelled notifications for completed task: " + taskId);
                                    } else {
                                        // Task is being marked as uncompleted, reschedule notifications
                                        scheduleTaskNotifications(updatedTask);
                                        Log.d(TAG, "Rescheduled notifications for uncompleted task: " + taskId);
                                    }
                                    
                                    // Notify listener
                                    if (listener != null) {
                                        listener.onTaskUpdated(updatedTask);
                                    }
                                }
                                
                                String action = isCompleted ? "completed" : "uncompleted";
                                Log.d(TAG, "✓ Task " + action + " successfully: " + taskId);
                            } else {
                                String errorMessage = response.optString("message", "Unknown error");
                                Log.e(TAG, "Server error updating task completion: " + errorMessage);
                                if (listener != null) {
                                    listener.onError("Error updating task completion: " + errorMessage);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing completion response: " + e.getMessage(), e);
                            if (listener != null) {
                                listener.onError("Error processing completion response: " + e.getMessage());
                            }
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Network error updating task completion: " + errorMessage);
                        if (listener != null) {
                            listener.onError("Network error updating task completion: " + errorMessage);
                        }
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error updating task completion: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Error updating task completion: " + e.getMessage());
            }
        }
    }

    /**
     * Get a task by its ID from local storage
     * This method provides public access to task lookup functionality
     * @param taskId The ID of the task to retrieve
     * @return The task if found, null otherwise
     */
    public Task getTaskById(String taskId) {
        return createTaskFromId(taskId);
    }

    /**
     * Create a task object from local storage using task ID
     */
    private Task createTaskFromId(String taskId) {
        try {
            SharedPreferences taskPrefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE);
            String taskJson = taskPrefs.getString("task_" + taskId, null);
            
            if (taskJson != null) {
                JSONObject taskObj = new JSONObject(taskJson);
                return new Task(
                    taskObj.getString("task_id"),
                    taskObj.optString("user_id", ""),
                    taskObj.optString("task_type", "remainder"),
                    taskObj.optString("title", ""),
                    taskObj.optString("description", ""),
                    taskObj.optString("start_time", ""),
                    taskObj.optString("end_time", ""),
                    taskObj.optString("due_date", ""),
                    taskObj.optString("status", "pending"),
                    taskObj.optString("repeat_frequency", "none"),
                    taskObj.optString("priority", "medium")
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating task from ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Update a local task with a new task object (used to replace temporary IDs with server IDs)
     */
    private void updateLocalTask(String oldTaskId, Task newTask) {
        try {
            SharedPreferences taskPrefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = taskPrefs.edit();
            
            // Remove old task
            editor.remove("task_" + oldTaskId);
            
            // Update task ID list
            String taskIdList = taskPrefs.getString("task_id_list", "");
            if (!taskIdList.isEmpty()) {
                // Replace old ID with new ID
                taskIdList = taskIdList.replace(oldTaskId, newTask.getTaskId());
                editor.putString("task_id_list", taskIdList);
            }
            
            // Store new task
            JSONObject taskJson = new JSONObject();
            taskJson.put("task_id", newTask.getTaskId());
            taskJson.put("user_id", newTask.getUserId());
            taskJson.put("task_type", newTask.getType());
            taskJson.put("title", newTask.getTitle());
            taskJson.put("description", newTask.getDescription());
            taskJson.put("start_time", newTask.getStartTime());
            taskJson.put("end_time", newTask.getEndTime());
            taskJson.put("due_date", newTask.getDueDate());
            taskJson.put("status", newTask.getStatus());
            taskJson.put("repeat_frequency", newTask.getRepeatFrequency());
            taskJson.put("priority", newTask.getPriority());
            taskJson.put("creation_timestamp", System.currentTimeMillis());
            
            editor.putString("task_" + newTask.getTaskId(), taskJson.toString());
            editor.apply();
            
            Log.d(TAG, "Updated local task ID from " + oldTaskId + " to " + newTask.getTaskId());
        } catch (Exception e) {
            Log.e(TAG, "Error updating local task: " + e.getMessage());
        }
    }
    
    /**
     * Update local task completion status while preserving task type
     */
    private void updateLocalTaskCompletion(String taskId, String newStatus, String preservedTaskType) {
        try {
            SharedPreferences taskPrefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE);
            String taskJson = taskPrefs.getString("task_" + taskId, null);
            
            if (taskJson != null) {
                JSONObject taskObj = new JSONObject(taskJson);
                taskObj.put("status", newStatus);
                
                // Ensure task type is preserved and not changed
                if (preservedTaskType != null && !preservedTaskType.isEmpty()) {
                    taskObj.put("task_type", preservedTaskType);
                }
                
                SharedPreferences.Editor editor = taskPrefs.edit();
                editor.putString("task_" + taskId, taskObj.toString());
                editor.apply();
                
                Log.d(TAG, "Updated local task completion: " + taskId + " status: " + newStatus + " type: " + preservedTaskType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating local task completion: " + e.getMessage());
        }
    }

    private Task createTaskFromJson(JSONObject taskObj) throws JSONException {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        
        // Handle different task_id field names
        String taskId;
        if (taskObj.has("task_id")) {
            taskId = taskObj.getString("task_id");
        } else if (taskObj.has("id")) {
            taskId = taskObj.getString("id");
        } else {
            // Generate a temporary ID if none is found
            taskId = "temp_" + System.currentTimeMillis();
            Log.w(TAG, "Task had no ID, generated temporary ID: " + taskId);
        }
        
        // Handle task type (may be missing)
        String taskType = taskObj.optString("task_type", "");
        if (taskType.isEmpty()) {
            taskType = "task"; // Default task type
        }
        
        return new Task(
            taskId,
            userId,
            taskType,
            taskObj.optString("title", "Untitled Task"),
            taskObj.optString("description", ""),
            taskObj.optString("start_time", ""),
            taskObj.optString("end_time", ""),
            taskObj.optString("due_date", ""),
            taskObj.optString("status", "pending"),
            taskObj.optString("repeat_frequency", "none"),
            taskObj.optString("priority", "medium")
        );
    }
    
    /**
     * Load tasks from local storage
     */
    public java.util.List<Task> loadLocalTasks(String date) {
        java.util.List<Task> localTasks = new ArrayList<>();
        
        try {
            SharedPreferences taskPrefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE);
            String taskIdList = taskPrefs.getString("task_id_list", "");
            
            if (!taskIdList.isEmpty()) {
                String[] taskIds = taskIdList.split(",");
                for (String taskId : taskIds) {
                    String taskJson = taskPrefs.getString("task_" + taskId, null);
                    if (taskJson != null) {
                        try {
                            JSONObject taskObj = new JSONObject(taskJson);
                            String taskDueDate = taskObj.optString("due_date", "");
                            
                            // If a specific date is provided, filter by that date
                            if (date == null || date.isEmpty() || taskDueDate.equals(date)) {
                                Task task = new Task(
                                    taskObj.getString("task_id"),
                                    taskObj.optString("user_id", ""),
                                    taskObj.optString("task_type", "task"),
                                    taskObj.optString("title", ""),
                                    taskObj.optString("description", ""),
                                    taskObj.optString("start_time", ""),
                                    taskObj.optString("end_time", ""),
                                    taskDueDate,
                                    taskObj.optString("status", "pending"),
                                    taskObj.optString("repeat_frequency", "none"),
                                    taskObj.optString("priority", "medium")
                                );
                                
                                // Set streak if available
                                if (taskObj.has("current_streak")) {
                                    task.setCurrentStreak(taskObj.getInt("current_streak"));
                                }
                                
                                localTasks.add(task);
                                Log.d(TAG, "Loaded local task: " + task.getTitle() + " (" + task.getTaskId() + ")");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing local task JSON: " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading local tasks: " + e.getMessage(), e);
        }
        
        return localTasks;
    }
    
    /**
     * Load tasks from server with local fallback - improved with faster loading
     */
    public void loadTasks(String date) {
        // Always try to load from local cache first for immediate display
        List<Task> cachedTasks = loadLocalTasks(date);
        if (!cachedTasks.isEmpty()) {
            // If we have cached tasks, show them immediately
            Log.d(TAG, "Showing " + cachedTasks.size() + " cached tasks immediately");
            if (listener != null) {
                listener.onTasksLoaded(cachedTasks);
            }
        }
        
        // Then fetch from server to get the latest data
        String endpoint = networkManager.getTodayTasksUrl(getUserId(), date);
        Log.d(TAG, "Loading tasks from: " + endpoint);
        
        VolleyNetworkManager.getInstance(context).makeGetRequestWithTimeout(
            endpoint,
            new VolleyNetworkManager.JsonResponseListener() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        Log.d(TAG, "Server response: " + response.toString().substring(0, Math.min(200, response.toString().length())));
                        List<Task> tasks = new ArrayList<>();
                        
                        if (response.has("tasks")) {
                            JSONArray tasksArray = response.getJSONArray("tasks");
                            for (int i = 0; i < tasksArray.length(); i++) {
                                JSONObject taskObject = tasksArray.getJSONObject(i);
                                
                                // Extract task data ensuring the correct type is preserved
                                String taskId = taskObject.optString("id", taskObject.optString("task_id", ""));
                                String taskType = taskObject.optString("type", "remainder").toLowerCase(); // Ensure lowercase
                                String title = taskObject.optString("title", "Untitled Task");
                                String description = taskObject.optString("description", "");
                                String startTime = taskObject.optString("start_time", "");
                                String endTime = taskObject.optString("end_time", "");
                                String dueDate = taskObject.optString("due_date", date);
                                String status = taskObject.optString("status", "pending");
                                String repeatFrequency = taskObject.optString("repeat_frequency", "none");
                                String priority = taskObject.optString("priority", "medium");
                                
                                // Force type to be either "workflow" or "remainder" - never "task"
                                if (!taskType.equals("workflow") && !taskType.equals("remainder")) {
                                    taskType = "remainder"; // Default to remainder if unknown
                                }
                                
                                Task task = new Task(
                                    taskId,
                                    getUserId(),
                                    taskType,
                                    title,
                                    description,
                                    startTime,
                                    endTime,
                                    dueDate,
                                    status,
                                    repeatFrequency,
                                    priority
                                );
                                
                                // Save task to local cache for offline access
                                storeTaskLocally(task);
                                
                                // Only add tasks for the requested date
                                if (date.equals(dueDate)) {
                                    tasks.add(task);
                                }
                            }
                        }
                        
                        if (listener != null && (!tasks.isEmpty() || cachedTasks.isEmpty())) {
                            // Only notify if we have tasks or didn't already show cached tasks
                            listener.onTasksLoaded(tasks);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing tasks: " + e.getMessage());
                        if (listener != null && cachedTasks.isEmpty()) {
                            // Only show error if we didn't already show cached tasks
                            listener.onError("Failed to load tasks: " + e.getMessage());
                        }
                    }
                }
                
                @Override
                public void onError(String message) {
                    Log.e(TAG, "Error loading tasks from server: " + message);
                    // We already displayed cached tasks, so don't show error unless no cached tasks
                    if (listener != null && cachedTasks.isEmpty()) {
                        listener.onError("Failed to load tasks from server. Using cached data.");
                    }
                }
            },
            5000 // Reduce timeout to 5 seconds for faster response
        );
    }
    
    /**
     * Load all tasks from server without date filter for timeline filtering
     */
    public void loadAllTasks() {
        String endpoint = networkManager.getAllTasksUrl(getUserId());
        Log.d(TAG, "Loading all tasks from: " + endpoint);
        
        VolleyNetworkManager.getInstance(context).makeGetRequestWithTimeout(
            endpoint,
            new VolleyNetworkManager.JsonResponseListener() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        Log.d(TAG, "All tasks response: " + response.toString().substring(0, Math.min(200, response.toString().length())));
                        List<Task> tasks = new ArrayList<>();
                        
                        if (response.has("tasks")) {
                            JSONArray tasksArray = response.getJSONArray("tasks");
                            for (int i = 0; i < tasksArray.length(); i++) {
                                JSONObject taskObject = tasksArray.getJSONObject(i);
                                
                                String taskId = taskObject.optString("id", taskObject.optString("task_id", ""));
                                String taskType = taskObject.optString("type", "remainder").toLowerCase();
                                String title = taskObject.optString("title", "Untitled Task");
                                String description = taskObject.optString("description", "");
                                String startTime = taskObject.optString("start_time", "");
                                String endTime = taskObject.optString("end_time", "");
                                String dueDate = taskObject.optString("due_date", "");
                                String status = taskObject.optString("status", "pending");
                                String repeatFrequency = taskObject.optString("repeat_frequency", "none");
                                String priority = taskObject.optString("priority", "medium");
                                
                                if (!taskType.equals("workflow") && !taskType.equals("remainder")) {
                                    taskType = "remainder";
                                }
                                
                                Task task = new Task(
                                    taskId,
                                    getUserId(),
                                    taskType,
                                    title,
                                    description,
                                    startTime,
                                    endTime,
                                    dueDate,
                                    status,
                                    repeatFrequency,
                                    priority
                                );
                                
                                tasks.add(task);
                            }
                        }
                        
                        if (listener != null) {
                            listener.onTasksLoaded(tasks);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing all tasks: " + e.getMessage());
                        if (listener != null) {
                            listener.onError("Failed to load all tasks: " + e.getMessage());
                        }
                    }
                }
                
                @Override
                public void onError(String message) {
                    Log.e(TAG, "Error loading all tasks: " + message);
                    if (listener != null) {
                        listener.onError("Failed to load all tasks from server.");
                    }
                }
            },
            8000
        );
    }

    // Update clearDeletedTasks method to be more effective
    public void clearDeletedTasks() {
        // Get all tasks from local storage
        SharedPreferences taskPrefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE);
        String taskIdList = taskPrefs.getString("task_id_list", "");
        
        if (taskIdList.isEmpty()) {
            return; // No tasks to clear
        }
        
        // Prepare to sync with server
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        
        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty, cannot sync deleted tasks");
            return;
        }
        
        // Use current date to sync with today's tasks
        String endpoint = networkManager.getTodayTasksUrl(userId, getCurrentDate());
        Log.d(TAG, "Syncing tasks with endpoint: " + endpoint);
        
        networkManager.makeGetRequestWithTimeout(
            endpoint,
            new VolleyNetworkManager.JsonResponseListener() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        Set<String> serverTaskIds = new HashSet<>();
                        
                        // Extract all task IDs from server response
                        if (response.has("tasks")) {
                            JSONArray taskArray = response.getJSONArray("tasks");
                            for (int i = 0; i < taskArray.length(); i++) {
                                JSONObject taskObj = taskArray.getJSONObject(i);
                                if (taskObj.has("task_id")) {
                                    serverTaskIds.add(taskObj.getString("task_id"));
                                } else if (taskObj.has("id")) {
                                    serverTaskIds.add(taskObj.getString("id"));
                                }
                            }
                        } else if (response.has("data") && response.getJSONObject("data").has("tasks")) {
                            JSONArray taskArray = response.getJSONObject("data").getJSONArray("tasks");
                            for (int i = 0; i < taskArray.length(); i++) {
                                JSONObject taskObj = taskArray.getJSONObject(i);
                                if (taskObj.has("task_id")) {
                                    serverTaskIds.add(taskObj.getString("task_id"));
                                } else if (taskObj.has("id")) {
                                    serverTaskIds.add(taskObj.getString("id"));
                                }
                            }
                        }
                        
                        Log.d(TAG, "Found " + serverTaskIds.size() + " tasks on server");
                        
                        // Compare local tasks with server tasks
                        String[] localTaskIds = taskIdList.split(",");
                        SharedPreferences.Editor editor = taskPrefs.edit();
                        boolean hasChanges = false;
                        List<String> updatedTaskIdList = new ArrayList<>();
                        
                        for (String taskId : localTaskIds) {
                            // Skip temporary tasks that haven't been synced yet
                            if (taskId.startsWith("temp_")) {
                                updatedTaskIdList.add(taskId);
                                continue;
                            }
                            
                            // If task is on server, keep it locally
                            if (serverTaskIds.contains(taskId)) {
                                updatedTaskIdList.add(taskId);
                            } else {
                                // Task is not on server, remove it locally
                                editor.remove("task_" + taskId);
                                hasChanges = true;
                                Log.d(TAG, "Removing deleted task from local storage: " + taskId);
                            }
                        }
                        
                        // Update task ID list if needed
                        if (hasChanges) {
                            if (updatedTaskIdList.isEmpty()) {
                                editor.remove("task_id_list");
                            } else {
                                editor.putString("task_id_list", TextUtils.join(",", updatedTaskIdList));
                            }
                            editor.apply();
                            Log.d(TAG, "Cleared " + (localTaskIds.length - updatedTaskIdList.size()) + " deleted tasks from local storage");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing deleted tasks: " + e.getMessage(), e);
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error syncing deleted tasks: " + errorMessage);
                }
            },
            2000 // 2 second timeout
        );
    }    /**
     * Updates the start and end time of a task with improved error handling and offline support
     * @param taskId ID of the task to update
     * @param newStartTime New start time
     * @param newEndTime New end time
     */
    public void updateTaskTime(String taskId, String newStartTime, String newEndTime) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");
            String currentDate = getCurrentDate();

            // Format time properly to ensure correct time is sent to server
            String formattedStartTime = formatTimeForServer(newStartTime, currentDate);
            String formattedEndTime = formatTimeForServer(newEndTime, currentDate);

            Log.d(TAG, "Updating task time: " + formattedStartTime + " to " + formattedEndTime);

            // Update local storage immediately for responsive UI
            Task existingTask = createTaskFromId(taskId);
            if (existingTask != null) {
                existingTask.setStartTime(formattedStartTime);
                existingTask.setEndTime(formattedEndTime);
                storeTaskLocally(existingTask);
                
                // Cancel existing notifications immediately
                notificationHandler.cancelTaskNotifications(taskId);
                
                // Notify listener immediately for UI update
                if (listener != null) {
                    listener.onTaskUpdated(existingTask);
                }
                
                // Reschedule notifications with new times
                scheduleTaskNotifications(existingTask);
                Log.d(TAG, "Local task time updated and notifications rescheduled for task: " + taskId);
            }

            // Check network connectivity before server update
            if (!IpV4Connection.isNetworkAvailable(context)) {
                Log.w(TAG, "No network connection - task time updated locally only");
                if (listener != null) {
                    listener.onError("No network connection. Task updated locally and will sync when online.");
                }
                return;
            }

            // Attempt server update
            String url = networkManager.getUpdateTaskUrl();
            JSONObject requestBody = new JSONObject();
            requestBody.put("task_id", taskId);
            requestBody.put("user_id", userId);
            requestBody.put("start_time", formattedStartTime);
            requestBody.put("end_time", formattedEndTime);

            networkManager.makePostRequestWithTimeout(
                url,
                requestBody,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            if (response.has("status") && "success".equals(response.getString("status"))) {
                                Log.d(TAG, "✅ Task time updated successfully on server: " + taskId);
                                
                                // Server update successful - ensure local data is consistent
                                if (existingTask != null) {
                                    // Re-store with any server-provided updates
                                    if (response.has("task")) {
                                        JSONObject serverTask = response.getJSONObject("task");
                                        // Update any additional fields from server if needed
                                        if (serverTask.has("start_time")) {
                                            existingTask.setStartTime(serverTask.getString("start_time"));
                                        }
                                        if (serverTask.has("end_time")) {
                                            existingTask.setEndTime(serverTask.getString("end_time"));
                                        }
                                        storeTaskLocally(existingTask);
                                    }
                                }
                            } else {
                                String errorMessage = response.optString("message", "Unknown server error");
                                Log.e(TAG, "❌ Server error updating task time: " + errorMessage);
                                if (listener != null) {
                                    listener.onError("Server error: " + errorMessage + ". Changes saved locally.");
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing server response: " + e.getMessage());
                            if (listener != null) {
                                listener.onError("Error processing server response. Changes saved locally.");
                            }
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "❌ Network error updating task time: " + errorMessage);
                        
                        // Try direct IP fallback for critical time updates
                        tryUpdateTaskTimeWithDirectIp(taskId, formattedStartTime, formattedEndTime, errorMessage);
                    }
                },
                7000 // 7 second timeout for time updates
            );
        } catch (Exception e) {
            Log.e(TAG, "Error in updateTaskTime: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Error updating task time: " + e.getMessage());
            }
        }
    }
    
    /**
     * Fallback method to update task time using direct IP addresses
     */
    private void tryUpdateTaskTimeWithDirectIp(String taskId, String startTime, String endTime, String previousError) {
        try {
            String[] ipUrls = {
                IpV4Connection.getBaseUrl() + "update_task.php",  // Use centralized IP
                "http://10.0.2.2/schedlytic/update_task.php"
            };
            
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("task_id", taskId);
            requestBody.put("user_id", userId);
            requestBody.put("start_time", startTime);
            requestBody.put("end_time", endTime);
            
            Log.d(TAG, "Trying direct IP fallback for task time update: " + taskId);
            
            boolean[] succeeded = {false};
            
            for (String url : ipUrls) {
                if (succeeded[0]) break; // Stop if already succeeded
                
                JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        if (!succeeded[0]) {
                            succeeded[0] = true;
                            Log.d(TAG, "✅ Task time updated successfully via direct IP: " + url);
                            // Success - no need to notify listener as local update already happened
                        }
                    },
                    error -> {
                        Log.e(TAG, "Direct IP failed " + url + ": " + error.getMessage());
                        if (!succeeded[0] && url.equals(ipUrls[ipUrls.length - 1])) {
                            // This was the last attempt
                            if (listener != null) {
                                listener.onError("Network error: " + previousError + ". Task updated locally only.");
                            }
                        }
                    }
                );
                
                // Short timeout for direct IP attempts
                request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    3000, // 3 second timeout
                    1,    // 1 retry
                    1.0f  // no backoff
                ));
                
                networkManager.makeCustomRequest(request);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in direct IP fallback for task time update: " + e.getMessage());
            if (listener != null) {
                listener.onError("Network error: " + previousError + ". Task updated locally only.");
            }
        }
    }

    /**
     * Store a task in SharedPreferences for offline access
     */
    private void storeTaskLocally(Task task) {
        try {
            SharedPreferences taskPrefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = taskPrefs.edit();
            
            // Convert task to JSON string
            JSONObject taskJson = new JSONObject();
            taskJson.put("task_id", task.getTaskId());
            taskJson.put("user_id", task.getUserId());
            taskJson.put("task_type", task.getTaskType());
            taskJson.put("title", task.getTitle());
            taskJson.put("description", task.getDescription());
            taskJson.put("start_time", task.getStartTime());
            taskJson.put("end_time", task.getEndTime());
            taskJson.put("due_date", task.getDueDate());
            taskJson.put("status", task.getStatus());
            taskJson.put("repeat_frequency", task.getRepeatFrequency());
            taskJson.put("priority", task.getPriority());
            // Store current streak if needed for habit tasks
            if (task.getCurrentStreak() > 0) {
                taskJson.put("current_streak", task.getCurrentStreak());
            }
            taskJson.put("creation_timestamp", System.currentTimeMillis());
            
            // Store the task JSON
            editor.putString("task_" + task.getTaskId(), taskJson.toString());
            
            // Also maintain a list of task IDs
            String taskIdList = taskPrefs.getString("task_id_list", "");
            if (taskIdList.isEmpty()) {
                taskIdList = task.getTaskId();
            } else {
                // Add to the list if not already present
                if (!taskIdList.contains(task.getTaskId())) {
                    taskIdList += "," + task.getTaskId();
                }
            }
            editor.putString("task_id_list", taskIdList);
            
            // Apply the changes
            editor.apply();
            
            Log.d(TAG, "Task stored locally with ID: " + task.getTaskId());
        } catch (Exception e) {
            Log.e(TAG, "Error storing task locally: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fast load tasks method that prioritizes UI responsiveness
     * Shows cached data immediately then updates with server data
     */
    public void fastLoadTasks(String date) {
        // Clear deleted tasks from cache first
        clearDeletedTasks();
        
        // Show cached data immediately
        List<Task> cachedTasks = loadLocalTasks(date);
        if (!cachedTasks.isEmpty()) {
            Log.d(TAG, "Fast-loading " + cachedTasks.size() + " cached tasks");
            if (listener != null) {
                listener.onTasksLoaded(cachedTasks);
            }
        }
        
        // Then quickly refresh from server
        loadTasks(date);
    }

    /**
     * Add a task with the given parameters
     */
    public void addTask(String title, String description, String startTime, String endTime,
                        String dueDate, String taskType, String repeatFrequency) {
        try {
            // Generate a temporary ID for immediate local storage
            String tempTaskId = "temp_" + System.currentTimeMillis();
            
            // Format times properly to ensure correct format is sent to server
            String formattedStartTime = formatTimeForServer(startTime, dueDate);
            String formattedEndTime = formatTimeForServer(endTime, dueDate);
            
            Log.d(TAG, "Adding task with formatted times: " + formattedStartTime + " to " + formattedEndTime);
            
            // Create task object - store both formatted and original time
            Task newTask = new Task(
                tempTaskId,
                getUserId(),
                taskType.toLowerCase(), // Ensure lowercase
                title,
                description,
                formattedStartTime, // Use formatted time with date
                formattedEndTime,   // Use formatted time with date
                dueDate,
                "pending",
                repeatFrequency,
                "medium"
            );
            
            // Use the new implementation
            addTask(newTask);
        } catch (Exception e) {
            Log.e(TAG, "Error creating task: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error creating task: " + e.getMessage());
            }
        }
    }

    /**
     * Save a task to the local database
     */
    private void saveTaskToLocalDb(Task task) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put("task_id", task.getTaskId());
            values.put("user_id", task.getUserId());
            values.put("title", task.getTitle());
            values.put("description", task.getDescription());
            values.put("start_time", task.getStartTime());
            values.put("end_time", task.getEndTime());
            values.put("due_date", task.getDueDate());
            values.put("status", task.getStatus());
            values.put("task_type", task.getTaskType());
            values.put("repeat_frequency", task.getRepeatFrequency());
            values.put("priority", task.getPriority());
            values.put("timestamp", System.currentTimeMillis());
            
            // Add or replace the task
            db.insertWithOnConflict("tasks", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            
            Log.d(TAG, "Task saved to local database: " + task.getTaskId());
        } catch (Exception e) {
            Log.e(TAG, "Error saving task to local database: " + e.getMessage());
        }
    }

    /**
     * Schedule notifications for a task based on its type
     */
    private void scheduleTaskNotifications(Task task) {
        try {
            if (task == null || task.getTaskId() == null) {
                Log.w(TAG, "Cannot schedule notifications for null task or task with null ID");
                return;
            }

            // Cancel any existing notifications for this task first
            notificationHandler.cancelTaskNotifications(task.getTaskId());
            reminderNotificationManager.cancelReminderNotifications(task.getTaskId());

            String taskType = task.getType() != null ? task.getType().toLowerCase() : "remainder";
            
            // Schedule appropriate notifications based on task type
            if ("workflow".equals(taskType)) {
                // For workflow tasks: use old NotificationHandler for start/end notifications
                notificationHandler.scheduleWorkflowNotifications(
                    task.getTaskId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getStartTime(),
                    task.getEndTime(),
                    task.getDueDate()
                );
                Log.d(TAG, "Scheduled workflow notifications for task: " + task.getTitle());
            } else if (task.isRemainder()) {
                // For remainder tasks: use new ReminderNotificationManager
                reminderNotificationManager.scheduleReminderNotifications(task);
                Log.d(TAG, "Scheduled reminder notifications for task: " + task.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling notifications for task: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a task from server and local storage
     */
    public void deleteTask(String taskId) {
        try {
            // Cancel all notifications for this task first
            notificationHandler.cancelTaskNotifications(taskId);
            
            // Remove from local storage immediately
            removeTaskFromLocalStorage(taskId);
            
            // Notify listener immediately for UI update
            if (listener != null) {
                listener.onTaskDeleted(taskId);
            }
            
            // Try to delete from server in background
            String url = networkManager.getDeleteTaskUrl();
            JSONObject requestBody = new JSONObject();
            requestBody.put("task_id", taskId);
            requestBody.put("user_id", getUserId());
            
            networkManager.makePostRequest(
                url,
                requestBody,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            if (response.has("status") && "success".equals(response.getString("status"))) {
                                Log.d(TAG, "Task deleted successfully from server: " + taskId);
                            } else {
                                Log.w(TAG, "Server response indicates deletion may have failed: " + response.toString());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing delete response: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error deleting task from server: " + errorMessage);
                        // Task already removed locally and notifications cancelled, so we don't need to show error
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error deleting task: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error deleting task: " + e.getMessage());
            }
        }
    }

    /**
     * Remove a task from local storage
     */
    private void removeTaskFromLocalStorage(String taskId) {
        try {
            SharedPreferences taskPrefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = taskPrefs.edit();
            
            // Remove the task
            editor.remove("task_" + taskId);
            
            // Update task ID list
            String taskIdList = taskPrefs.getString("task_id_list", "");
            if (!taskIdList.isEmpty()) {
                String[] taskIds = taskIdList.split(",");
                List<String> updatedTaskIds = new ArrayList<>();
                
                for (String id : taskIds) {
                    if (!id.equals(taskId)) {
                        updatedTaskIds.add(id);
                    }
                }
                
                if (updatedTaskIds.isEmpty()) {
                    editor.remove("task_id_list");
                } else {
                    editor.putString("task_id_list", TextUtils.join(",", updatedTaskIds));
                }
            }
            
            editor.apply();
            Log.d(TAG, "Removed task from local storage: " + taskId);
        } catch (Exception e) {
            Log.e(TAG, "Error removing task from local storage: " + e.getMessage());
        }
    }

    /**
     * Try to add a task using direct IP addresses as a fallback
     */
    private void tryAddTaskWithDirectIp(Task task, String previousError) {
        try {            // Get direct IP addresses to try - use centralized IP configuration
            String[] ipAddresses = {
                IpV4Connection.getBaseUrl() + "add_task.php",  // Use centralized IP
                "http://10.0.2.2/schedlytic/add_task.php"
            };
            
            // Convert task to JSON
            JSONObject jsonTask = task.toJson();
            
            // Log fallback attempt
            Log.d(TAG, "Trying direct IP fallback for task: " + task.getTitle());
            
            // Use a flag to track if any request succeeded
            final boolean[] succeeded = {false};
            
            // Try each IP address
            for (String url : ipAddresses) {
                JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, 
                    url, 
                    jsonTask,
                    response -> {
                        // Mark as succeeded
                        succeeded[0] = true;
                        
                        try {
                            Log.d(TAG, "Task added successfully via direct IP: " + response.toString());
                            
                            // Parse task ID from response if available
                            String taskId = task.getTaskId();
                            if (response.has("task_id")) {
                                taskId = response.getString("task_id");
                            } else if (response.has("id")) {
                                taskId = response.getString("id");
                            } else if (response.has("data") && response.getJSONObject("data").has("task_id")) {
                                taskId = response.getJSONObject("data").getString("task_id");
                            }
                            task.setTaskId(taskId);
                              // Save to local database for offline access
                            saveTaskToLocalDb(task);
                            
                            // Schedule notifications for the task
                            scheduleTaskNotifications(task);
                            
                            // Notify listener of successful add
                            if (listener != null) {
                                listener.onTaskAdded(task);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing direct IP response: " + e.getMessage());
                            tryLocalFallback(task, e.getMessage());
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error with direct IP " + url + ": " + error.getMessage());
                        
                        // Only use local fallback if all IP addresses fail
                        if (!succeeded[0]) {
                            tryLocalFallback(task, previousError);
                        }
                    }
                );
                
                // Set short timeout
                request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    5000, // 5 second timeout
                    1,    // 1 retry
                    1.0f  // no backoff
                ));
                
                // Add to queue using the networkManager instead of calling addToRequestQueue directly
                VolleyNetworkManager.getInstance(context).makeCustomRequest(request);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in direct IP fallback: " + e.getMessage());
            tryLocalFallback(task, previousError);
        }
    }
      /**
     * Final fallback - save locally when all network attempts fail
     */
    private void tryLocalFallback(Task task, String previousError) {
        // Save locally anyway for offline access
        saveTaskToLocalDb(task);
        
        // Schedule notifications for the task even if server failed
        scheduleTaskNotifications(task);
        
        if (listener != null) {
            listener.onTaskAdded(task); // Still notify as added since it's saved locally
            listener.onError("Error adding task to server: " + previousError + ". Task saved locally.");
        }
    }

    /**
     * Update task status on server and locally
     * @param taskId The ID of the task to update
     * @param status The new status for the task
     */
    public void updateTaskStatus(String taskId, String status) {
        try {
            Log.d(TAG, "Updating task status: " + taskId + " to " + status);
            
            // Update local storage first
            SharedPreferences taskPrefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE);
            String taskJson = taskPrefs.getString("task_" + taskId, null);
            
            if (taskJson != null) {
                JSONObject taskObj = new JSONObject(taskJson);
                taskObj.put("status", status);
                taskPrefs.edit().putString("task_" + taskId, taskObj.toString()).apply();
            }
            
            // Create JSON payload for server update
            JSONObject updateData = new JSONObject();
            updateData.put("task_id", taskId);
            updateData.put("status", status);
            updateData.put("user_id", getUserId());
            
            // Send update to server
            String url = networkManager.getBaseUrl() + "update_task_status.php";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, updateData,
                response -> {
                    Log.d(TAG, "Task status updated successfully on server");
                    if (listener != null) {
                        // Create a temporary task object for the update callback
                        Task updatedTask = createTaskFromId(taskId);
                        if (updatedTask != null) {
                            updatedTask.setStatus(status);
                            updatedTask.setCompleted("completed".equalsIgnoreCase(status));
                            listener.onTaskUpdated(updatedTask);
                        }
                    }
                },
                error -> {
                    Log.e(TAG, "Error updating task status on server: " + error.getMessage());
                    // Status is already updated locally, so we can continue
                    if (listener != null) {
                        Task updatedTask = createTaskFromId(taskId);
                        if (updatedTask != null) {
                            updatedTask.setStatus(status);
                            updatedTask.setCompleted("completed".equalsIgnoreCase(status));
                            listener.onTaskUpdated(updatedTask);
                        }
                    }                });
            
            networkManager.makeCustomRequest(request);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating task status: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error updating task status: " + e.getMessage());
            }
        }
    }
}
