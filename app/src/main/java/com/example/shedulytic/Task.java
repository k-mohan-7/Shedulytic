package com.example.shedulytic;

import java.io.Serializable;

public class Task implements Serializable {
    private String taskId;
    private String userId;
    private String taskType; // "workflow" or "remainder"
    private String title;
    private String description;
    private String startTime;
    private String endTime;
    private String dueDate;
    private String status; // "pending", "completed", "in_progress", "extended", "cant_complete"
    private String repeatFrequency; // "none", "daily", "weekly", "monthly"
    private String priority; // "low", "medium", "high"

    public Task(String taskId, String userId, String taskType, String title, String description,
                String startTime, String endTime, String dueDate, String status,
                String repeatFrequency, String priority) {
        this.taskId = taskId;
        this.userId = userId;
        this.taskType = taskType;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dueDate = dueDate;
        this.status = status;
        this.repeatFrequency = repeatFrequency;
        this.priority = priority;
    }
    
    // Alternative constructor for TimelineActivity compatibility
    public Task(int id, String taskType, String title, String description,
                String startTime, String endTime, String dueDate,
                String repeatFrequency, String status) {
        this.taskId = String.valueOf(id);
        this.userId = ""; // Will be set separately
        this.taskType = taskType;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dueDate = dueDate;
        this.status = status;
        this.repeatFrequency = repeatFrequency;
        this.priority = "medium"; // Default priority
    }

    // Getters
    public String getTaskId() {
        return taskId;
    }
    
    // Helper method for backward compatibility
    public int getId() {
        try {
            return Integer.parseInt(taskId);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    public String getRepeatFrequency() {
        return repeatFrequency;
    }
    
    public String getDuration() {
        // This method is added to support the timeline view
        // It returns an empty string as the duration will be calculated in HomeFragment
        return "";
    }

    public String getPriority() {
        return priority;
    }

    // Setters
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRepeatFrequency(String repeatFrequency) {
        this.repeatFrequency = repeatFrequency;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    // Helper methods
    public boolean isWorkflow() {
        return "workflow".equalsIgnoreCase(taskType);
    }

    public boolean isRemainder() {
        return "remainder".equalsIgnoreCase(taskType);
    }

    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isInProgress() {
        return "in_progress".equalsIgnoreCase(status);
    }

    public boolean isExtended() {
        return "extended".equalsIgnoreCase(status);
    }

    public boolean isCantComplete() {
        return "cant_complete".equalsIgnoreCase(status);
    }
}