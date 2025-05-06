package com.example.shedulytic;

public class TaskItem {
    private String title;
    private String time; // e.g., "08:10 am - 08:50 am"
    private String duration; // e.g., "40 mins"
    private String type; // e.g., "HABIT", "REMAINDER", "WORKFLOW"
    // Add other relevant fields if needed, like ID, description, etc.

    public TaskItem(String title, String time, String duration, String type) {
        this.title = title;
        this.time = time;
        this.duration = duration;
        this.type = type;
    }

    // Getters
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

    // Setters (optional, depending on if you need to modify tasks after creation)
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
        this.type = type;
    }
}