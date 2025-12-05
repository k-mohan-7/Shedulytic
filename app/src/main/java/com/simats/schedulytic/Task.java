package com.simats.schedulytic;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;

public class Task implements Serializable {
    private String taskId;
    private String userId;
    private String type;
    private String title;
    private String description;
    private String startTime;
    private String endTime;
    private String dueDate;
    private String status;
    private String repeat;
    private String priority;
    private boolean completed;
    
    // Habit-specific fields
    private double latitude;
    private double longitude;
    private int pomodoroCount;
    private int pomodoroLength;
    private String verificationType; // "checkbox", "location", "pomodoro"
    private String color; // Added to support custom task colors
    private int currentStreak; // Added for compatibility

    public Task(String taskId, String userId, String type, String title, String description, String startTime, String endTime, String dueDate, String status, String repeat, String priority) {
        this.taskId = taskId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dueDate = dueDate;
        this.status = status;
        this.repeat = repeat;
        this.priority = priority;
        this.completed = "completed".equalsIgnoreCase(status);
        this.verificationType = "checkbox"; // Default verification type
        this.pomodoroCount = 1;
        this.pomodoroLength = 25;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    // Default constructor for testing purposes
    public Task() {
        this("", "", "remainder", "", "", "", "", "", "pending", "none", "medium");
    }

    // Existing getters and setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    // Alias for type setter to maintain compatibility
    public void setTaskType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.completed = "completed".equalsIgnoreCase(status);
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.status = completed ? "completed" : "pending";
    }
    
    // Getter and setter for color
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
    
    // Getter and setter for latitude
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    // Getter and setter for longitude
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    // Getter and setter for pomodoroCount
    public int getPomodoroCount() {
        return pomodoroCount;
    }
    
    public void setPomodoroCount(int pomodoroCount) {
        this.pomodoroCount = pomodoroCount;
    }
    
    // Getter and setter for pomodoroLength
    public int getPomodoroLength() {
        return pomodoroLength;
    }
    
    public void setPomodoroLength(int pomodoroLength) {
        this.pomodoroLength = pomodoroLength;
    }
    
    // Getter and setter for verificationType
    public String getVerificationType() {
        return verificationType;
    }
    
    public void setVerificationType(String verificationType) {
        this.verificationType = verificationType;
    }
    
    // Helper method to check if this is a location-based habit
    public boolean isLocationVerification() {
        return "location".equalsIgnoreCase(verificationType);
    }
    
    // Helper method to check if this is a pomodoro-based habit
    public boolean isPomodoroVerification() {
        return "pomodoro".equalsIgnoreCase(verificationType);
    }
    
    // Helper method to check if this is a habit
    public boolean isHabit() {
        return "habit".equalsIgnoreCase(type);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonTask = new JSONObject();
        jsonTask.put("task_id", taskId);
        jsonTask.put("user_id", userId);
        jsonTask.put("task_type", type);  // Use task_type instead of type for server compatibility
        jsonTask.put("title", title);
        jsonTask.put("description", description);
        jsonTask.put("start_time", startTime);
        jsonTask.put("end_time", endTime);
        jsonTask.put("due_date", dueDate);
        jsonTask.put("status", status);
        jsonTask.put("repeat_frequency", repeat);  // Use repeat_frequency instead of repeat
        jsonTask.put("priority", priority);
        
        // Add habit-specific fields
        if (isHabit()) {
            jsonTask.put("verification_type", verificationType);
            jsonTask.put("trust_type", verificationType);  // Backend expects trust_type for verification method
            
            if (isLocationVerification()) {
                jsonTask.put("latitude", latitude);
                jsonTask.put("longitude", longitude);
                jsonTask.put("map_lat", latitude);  // Backend compatibility
                jsonTask.put("map_lon", longitude); // Backend compatibility
            } else if (isPomodoroVerification()) {
                jsonTask.put("pomodoro_count", pomodoroCount);
                jsonTask.put("pomodoro_length", pomodoroLength);
                jsonTask.put("pomodoro_duration", pomodoroLength); // Backend compatibility
            }
            
            if (color != null && !color.isEmpty()) {
                jsonTask.put("color", color);
            }
        }
        
        return jsonTask;
    }

    // Method for backward compatibility
    public String getTaskType() {
        return type;
    }
    
    // Method for backward compatibility
    public String getRepeatFrequency() {
        return repeat;
    }
    
    // Method for backward compatibility
    public int getCurrentStreak() {
        return currentStreak;
    }
    
    // Method for backward compatibility
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }
    
    // Helper methods for compatibility with older code
    public boolean isWorkflow() {
        return "workflow".equalsIgnoreCase(type);
    }
    
    public boolean isRemainder() {
        return "remainder".equalsIgnoreCase(type);
    }
}