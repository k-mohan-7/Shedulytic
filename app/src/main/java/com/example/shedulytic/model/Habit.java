package com.example.shedulytic.model;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;
import java.util.Iterator;
import java.io.Serializable;
import java.util.Objects;

/**
 * Model class for Habits with various verification methods
 */
public class Habit implements Parcelable, Serializable {
    // Constants for verification methods
    public static final String VERIFICATION_CHECKBOX = "checkbox";
    public static final String VERIFICATION_LOCATION = "location";
    public static final String VERIFICATION_POMODORO = "pomodoro";
    
    private String habitId;
    private String userId;
    private String title;
    private String description;
    private String frequency; // daily, weekly, weekdays, weekends, etc.
    private String verificationMethod; // checkbox, location, pomodoro
    private boolean completed;
    private int currentStreak;
    private int totalCompletions;
    
    // Location-based verification properties
    private double latitude;
    private double longitude;
    private double radiusMeters; // Radius in meters for location verification
    
    // Pomodoro-based verification properties
    private int pomodoroCount; // Number of pomodoros required
    private int pomodoroLength; // Length of each pomodoro in minutes
    
    // Reminder properties
    private String reminderTime; // Format: HH:MM
    
    // Extra properties for flexibility with backend
    private JSONObject extraProperties;
    
    // Default constructor
    public Habit() {
        this.habitId = UUID.randomUUID().toString();
        this.userId = "";
        this.title = "";
        this.description = "";
        this.frequency = "daily";
        this.verificationMethod = VERIFICATION_CHECKBOX;
        this.completed = false;
        this.currentStreak = 0;
        this.totalCompletions = 0;
        this.latitude = 0;
        this.longitude = 0;
        this.radiusMeters = 100;
        this.pomodoroCount = 1;
        this.pomodoroLength = 25;
        this.extraProperties = new JSONObject();
    }
    
    // Constructor with required fields
    public Habit(String userId, String title, String description, String frequency, String verificationMethod) {
        this();
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.frequency = frequency;
        this.verificationMethod = verificationMethod;
        this.completed = false;
        this.currentStreak = 0;
        this.totalCompletions = 0;
        this.latitude = 0;
        this.longitude = 0;
        this.radiusMeters = 100;
        this.pomodoroCount = 1;
        this.pomodoroLength = 25;
    }
    
    // Getters and setters
    public String getHabitId() {
        return habitId;
    }
    
    public void setHabitId(String habitId) {
        this.habitId = habitId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
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
    
    public String getFrequency() {
        return frequency;
    }
    
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
    
    public String getVerificationMethod() {
        return verificationMethod;
    }
    
    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public int getCurrentStreak() {
        return currentStreak;
    }
    
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }
    
    public int getTotalCompletions() {
        return totalCompletions;
    }
    
    public void setTotalCompletions(int totalCompletions) {
        this.totalCompletions = totalCompletions;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public double getRadiusMeters() {
        return radiusMeters;
    }
    
    public void setRadiusMeters(double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }
    
    public int getPomodoroCount() {
        return pomodoroCount;
    }
    
    public void setPomodoroCount(int pomodoroCount) {
        this.pomodoroCount = pomodoroCount;
    }
    
    public int getPomodoroLength() {
        return pomodoroLength;
    }
    
    public void setPomodoroLength(int pomodoroLength) {
        this.pomodoroLength = pomodoroLength;
    }
    
    public String getReminderTime() {
        return reminderTime;
    }
    
    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }
    
    // Extra properties getters and setters
    public JSONObject getExtraProperties() { 
        if (extraProperties == null) {
            extraProperties = new JSONObject();
        }
        return extraProperties; 
    }
    
    public void setExtraProperties(JSONObject extraProperties) { 
        this.extraProperties = extraProperties; 
    }
    
    public void addExtraProperty(String key, Object value) {
        try {
            if (extraProperties == null) {
                extraProperties = new JSONObject();
            }
            extraProperties.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public Object getExtraProperty(String key) {
        if (extraProperties == null) return null;
        return extraProperties.opt(key);
    }
    
    public Object getExtraProperty(String key, Object defaultValue) {
        if (extraProperties == null) return defaultValue;
        return extraProperties.opt(key) != null ? extraProperties.opt(key) : defaultValue;
    }
    
    // Helper methods
    
    public boolean isLocationBased() {
        return VERIFICATION_LOCATION.equals(verificationMethod);
    }
    
    public boolean isPomodoroBased() {
        return VERIFICATION_POMODORO.equals(verificationMethod);
    }
    
    public boolean isCheckboxBased() {
        return VERIFICATION_CHECKBOX.equals(verificationMethod);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Habit habit = (Habit) o;
        return Objects.equals(habitId, habit.habitId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(habitId);
    }
    
    @Override
    public String toString() {
        return "Habit{" +
                "habitId='" + habitId + '\'' +
                ", title='" + title + '\'' +
                ", completed=" + completed +
                ", verificationMethod='" + verificationMethod + '\'' +
                '}';
    }
    
    // Convert to JSON for storage and network transmission
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("habit_id", habitId);
        json.put("user_id", userId);
        json.put("title", title);
        json.put("description", description);
        
        // Map verification method to backend trust_type enum values
        String backendTrustType = verificationMethod;
        if (VERIFICATION_LOCATION.equals(verificationMethod)) {
            backendTrustType = "map"; // Backend uses 'map' for location verification
        }
        json.put("trust_type", backendTrustType); // Using correct backend enum values
        json.put("verification_method", verificationMethod);
        json.put("completed", completed ? 1 : 0);
        json.put("current_streak", currentStreak);
        json.put("total_completions", totalCompletions);
        json.put("frequency", frequency);
        
        // Add method-specific fields
        if (VERIFICATION_LOCATION.equals(verificationMethod)) {
            json.put("map_lat", latitude); // For backend compatibility
            json.put("map_lon", longitude); // For backend compatibility
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            json.put("radius_meters", radiusMeters);
        } else if (VERIFICATION_POMODORO.equals(verificationMethod)) {
            json.put("pomodoro_count", pomodoroCount);
            json.put("pomodoro_duration", pomodoroLength);
        }
        
        // Add time-specific fields
        if (reminderTime != null) json.put("reminder_time", reminderTime);
        
        // Add any extra properties
        if (extraProperties != null) {
            Iterator<String> keys = extraProperties.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!json.has(key)) { // Avoid overwriting existing properties
                    json.put(key, extraProperties.opt(key));
                }
            }
        }
        
        return json;
    }
    
    // Create from JSON for retrieval from storage or network
    public static Habit fromJson(JSONObject json) throws JSONException {
        Habit habit = new Habit();
        habit.habitId = json.optString("habit_id", UUID.randomUUID().toString());
        habit.userId = json.optString("user_id", "");
        habit.title = json.optString("title", "");
        habit.description = json.optString("description", "");
        
        // Log the raw JSON to debug
        android.util.Log.d("Habit", "Parsing habit from JSON: " + json.toString());
        
        // Check for trust_type first (backend format), then verification_method
        String trustType = "";
        if (json.has("trust_type")) {
            trustType = json.optString("trust_type", VERIFICATION_CHECKBOX);
            android.util.Log.d("Habit", "Found trust_type: " + trustType);
            // Map backend trust_type enum values to app verification method constants
            if ("map".equals(trustType)) {
                habit.verificationMethod = VERIFICATION_LOCATION;
                android.util.Log.d("Habit", "Mapped 'map' to VERIFICATION_LOCATION");
            } else if ("checkbox".equals(trustType)) {
                habit.verificationMethod = VERIFICATION_CHECKBOX;
                android.util.Log.d("Habit", "Mapped 'checkbox' to VERIFICATION_CHECKBOX");
            } else if ("pomodoro".equals(trustType)) {
                habit.verificationMethod = VERIFICATION_POMODORO;
                android.util.Log.d("Habit", "Mapped 'pomodoro' to VERIFICATION_POMODORO");
            } else {
                habit.verificationMethod = trustType; // fallback for compatibility
                android.util.Log.d("Habit", "Using fallback mapping: " + trustType);
            }
        } else {
            habit.verificationMethod = json.optString("verification_method", VERIFICATION_CHECKBOX);
            android.util.Log.d("Habit", "Using verification_method: " + habit.verificationMethod);
        }
        
        android.util.Log.d("Habit", "Final verification method for " + habit.title + ": " + habit.verificationMethod);
        
        // Check completed_today first (from server with habit_completions join), fallback to completed
        if (json.has("completed_today")) {
            habit.completed = json.optInt("completed_today", 0) == 1;
            android.util.Log.d("Habit", "Using completed_today: " + habit.completed);
        } else {
            habit.completed = json.optInt("completed", 0) == 1;
        }
        habit.currentStreak = json.optInt("current_streak", 0);
        habit.totalCompletions = json.optInt("total_completions", 0);
        habit.frequency = json.optString("frequency", "daily");
        
        // Parse method-specific fields with backend compatibility
        if (VERIFICATION_LOCATION.equals(habit.verificationMethod)) {
            // Check for map_lat/map_lon first (backend format), then latitude/longitude
            if (json.has("map_lat") && json.has("map_lon")) {
                habit.latitude = json.optDouble("map_lat", 0);
                habit.longitude = json.optDouble("map_lon", 0);
            } else {
                habit.latitude = json.optDouble("latitude", 0);
                habit.longitude = json.optDouble("longitude", 0);
            }
            habit.radiusMeters = json.optDouble("radius_meters", 100);
        } else if (VERIFICATION_POMODORO.equals(habit.verificationMethod)) {
            habit.pomodoroCount = json.optInt("pomodoro_count", 1);
            habit.pomodoroLength = json.optInt("pomodoro_duration", 25);
        }
        
        // Parse time-specific fields
        if (json.has("reminder_time")) habit.reminderTime = json.getString("reminder_time");
        
        // Store all fields as extra properties for full backend compatibility
        habit.extraProperties = new JSONObject();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            habit.extraProperties.put(key, json.opt(key));
        }
        
        return habit;
    }
    
    // Parcelable implementation
    protected Habit(Parcel in) {
        habitId = in.readString();
        userId = in.readString();
        title = in.readString();
        description = in.readString();
        verificationMethod = in.readString();
        completed = in.readByte() != 0;
        currentStreak = in.readInt();
        totalCompletions = in.readInt();
        frequency = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        radiusMeters = in.readDouble();
        pomodoroCount = in.readInt();
        pomodoroLength = in.readInt();
        reminderTime = in.readString();
        try {
            String extraPropsJson = in.readString();
            if (extraPropsJson != null && !extraPropsJson.isEmpty()) {
                extraProperties = new JSONObject(extraPropsJson);
            } else {
                extraProperties = new JSONObject();
            }
        } catch (JSONException e) {
            extraProperties = new JSONObject();
        }
    }
    
    public static final Creator<Habit> CREATOR = new Creator<Habit>() {
        @Override
        public Habit createFromParcel(Parcel in) {
            return new Habit(in);
        }
        
        @Override
        public Habit[] newArray(int size) {
            return new Habit[size];
        }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(habitId);
        dest.writeString(userId);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(verificationMethod);
        dest.writeByte((byte) (completed ? 1 : 0));
        dest.writeInt(currentStreak);
        dest.writeInt(totalCompletions);
        dest.writeString(frequency);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(radiusMeters);
        dest.writeInt(pomodoroCount);
        dest.writeInt(pomodoroLength);
        dest.writeString(reminderTime);
        dest.writeString(extraProperties != null ? extraProperties.toString() : "");
    }
} 