package com.example.shedulytic.model;

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
    private String repeatFrequency;
    private String priority;
    private int currentStreak;
    private String parentTaskId;

    public Task(String taskId, String userId, String type, String title, String description,
                String startTime, String endTime, String dueDate, String status,
                String repeatFrequency, String priority) {
        this.taskId = taskId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dueDate = dueDate;
        this.status = status;
        this.repeatFrequency = repeatFrequency;
        this.priority = priority;
        this.currentStreak = 0;
    }

    public String getTaskId() { return taskId; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public String getRepeatFrequency() { return repeatFrequency; }
    public String getPriority() { return priority; }
    public int getCurrentStreak() { return currentStreak; }
    public String getParentTaskId() { return parentTaskId; }

    public void setTaskId(String taskId) { this.taskId = taskId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setStatus(String status) { this.status = status; }
    public void setRepeatFrequency(String repeatFrequency) { this.repeatFrequency = repeatFrequency; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }

    public boolean isHabit() { return "habit".equalsIgnoreCase(type); }
    public boolean isCompleted() { return "completed".equalsIgnoreCase(status); }
}