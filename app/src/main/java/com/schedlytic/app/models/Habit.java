package com.schedlytic.app.models;

public class Habit {
    private int id;
    private String title;
    private String frequency;
    private String trustType;
    private String reminderDate;
    private String reminderTime;
    private double mapLat;
    private double mapLon;
    private int pomodoroMinutes;
    private boolean isCompleted;
    private int streak;

    public Habit() {
        // Default constructor
    }

    public Habit(int id, String title, String frequency, String trustType, String reminderDate, String reminderTime) {
        this.id = id;
        this.title = title;
        this.frequency = frequency;
        this.trustType = trustType;
        this.reminderDate = reminderDate;
        this.reminderTime = reminderTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getTrustType() {
        return trustType;
    }

    public void setTrustType(String trustType) {
        this.trustType = trustType;
    }

    public String getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(String reminderDate) {
        this.reminderDate = reminderDate;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public double getMapLat() {
        return mapLat;
    }

    public void setMapLat(double mapLat) {
        this.mapLat = mapLat;
    }

    public double getMapLon() {
        return mapLon;
    }

    public void setMapLon(double mapLon) {
        this.mapLon = mapLon;
    }

    public int getPomodoroMinutes() {
        return pomodoroMinutes;
    }

    public void setPomodoroMinutes(int pomodoroMinutes) {
        this.pomodoroMinutes = pomodoroMinutes;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }
} 