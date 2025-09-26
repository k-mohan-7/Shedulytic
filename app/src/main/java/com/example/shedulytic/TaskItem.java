package com.example.shedulytic;

import org.json.JSONObject;

public class TaskItem {
    private String id;
    private String title;
    private String time;
    private String duration;
    private String type;
    private String description;
    private String startTime;
    private String endTime;
    private String repeatFrequency;
    private boolean completed;
    private int streak;
    private String status;
    private String priority;
    private JSONObject extraProperties; // For storing additional properties like verification info
    private String verificationType; // Type of verification (checkbox, location, pomodoro)

    public TaskItem(String title, String time, String duration, String type) {
        this.title = title;
        this.time = time;
        this.duration = duration;
        setTypeWithDefault(type);
        this.completed = false;
        this.streak = 0;
        this.status = "pending";
        this.priority = "medium";
        this.verificationType = "checkbox"; // Default verification type
    }

    // Constructor with more parameters
    public TaskItem(String id, String title, String description, String startTime, String endTime, String type, String repeatFrequency, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        setTypeWithDefault(type);
        this.repeatFrequency = repeatFrequency;
        this.completed = completed;
        this.streak = 0;
        this.status = completed ? "completed" : "pending";
        this.priority = "medium";
        this.verificationType = "checkbox"; // Default verification type
    }
    
    // Constructor used in TodayActivitiesManager
    public TaskItem(String id, String title, String description, String time, String duration, String status, String priority, int streak) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.time = time;
        this.duration = duration;
        this.status = status;
        this.priority = priority;
        this.streak = streak;
        this.completed = "completed".equals(status);
        setTypeWithDefault("remainder"); // Default to remainder
    }
    
    // Constructor with type parameter
    public TaskItem(String id, String title, String type, String description, String time, String duration, String status, String priority, int streak) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.time = time;
        this.duration = duration;
        this.status = status;
        this.priority = priority;
        this.streak = streak;
        this.completed = "completed".equals(status);
        setTypeWithDefault(type);
    }

    // Helper method to ensure type is either workflow, remainder, or habit
    private void setTypeWithDefault(String inputType) {
        String normalizedType = (inputType != null) ? inputType.toLowerCase() : "";
        if (normalizedType.equals("workflow") || normalizedType.equals("remainder") || normalizedType.equals("habit")) {
            this.type = normalizedType;
        } else {
            this.type = "remainder"; // Default to remainder for any other value
        }
    }

    // Basic getters and setters
    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }

    public String getDuration() {
        return duration;
    }

    public String getType() {
        return type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setType(String type) {
        setTypeWithDefault(type);
    }

    // Additional getters and setters needed by HomeFragment
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return id;
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

    public String getRepeatFrequency() {
        return repeatFrequency;
    }

    public void setRepeatFrequency(String repeatFrequency) {
        this.repeatFrequency = repeatFrequency;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.status = completed ? "completed" : "pending";
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public boolean isWorkflow() {
        return "workflow".equalsIgnoreCase(type);
    }
    
    public boolean isRemainder() {
        return "remainder".equalsIgnoreCase(type);
    }
    
    public boolean isHabit() {
        return "habit".equalsIgnoreCase(type);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
        this.completed = "completed".equals(status);
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }

    // Extra properties for habits
    public JSONObject getExtraProperties() {
        return extraProperties;
    }
    
    public void setExtraProperties(JSONObject extraProperties) {
        this.extraProperties = extraProperties;
    }
    
    // Verification type methods
    public String getVerificationType() {
        return verificationType;
    }
    
    public void setVerificationType(String verificationType) {
        this.verificationType = verificationType;
    }
    
    // Methods for habit-specific properties
    public double getLatitude() {
        if (extraProperties != null) {
            try {
                return extraProperties.optDouble("map_lat", 0.0);
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    public double getLongitude() {
        if (extraProperties != null) {
            try {
                return extraProperties.optDouble("map_lon", 0.0);
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    public int getPomodoroCount() {
        if (extraProperties != null) {
            try {
                return extraProperties.optInt("pomodoro_count", 1);
            } catch (Exception e) {
                return 1;
            }
        }
        return 1;
    }
    
    public int getPomodoroLength() {
        if (extraProperties != null) {
            try {
                return extraProperties.optInt("pomodoro_duration", 25);
            } catch (Exception e) {
                return 25;
            }
        }
        return 25;
    }
}