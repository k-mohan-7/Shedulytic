package com.example.shedulytic.ui.login;

public class IpV4Connection {
    private static final String BASE_URL = "http://192.168.56.64/schedlytic/";

    // Method to get Base URL
    public static String getBaseUrl() {
        return BASE_URL;
    }
    
    // Method to get view tasks URL
    public static String getViewTasksUrl(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        return BASE_URL + "view_tasks.php?user_id=" + userId;
    }
    
    // Method to get add task URL
    public static String getAddTaskUrl() {
        return BASE_URL + "add_task.php";
    }
    
    // Method to get update task URL
    public static String getUpdateTaskUrl() {
        return BASE_URL + "update_task.php";
    }
    
    // Method to get delete task URL
    public static String getDeleteTaskUrl() {
        return BASE_URL + "delete_task.php";
    }
}
