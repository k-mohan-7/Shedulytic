package com.simats.schedulytic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for notification management and preferences
 */
public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_WORKFLOW_NOTIFICATIONS = "workflow_notifications_enabled";
    private static final String KEY_REMINDER_NOTIFICATIONS = "reminder_notifications_enabled";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound_enabled";
    private static final String KEY_NOTIFICATION_VIBRATION = "notification_vibration_enabled";
    private static final String KEY_EARLY_REMINDERS = "early_reminders_enabled";
    private static final String KEY_SNOOZE_DURATION = "snooze_duration_minutes";
    
    /**
     * Check if workflow notifications are enabled
     */
    public static boolean areWorkflowNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_WORKFLOW_NOTIFICATIONS, true);
    }
    
    /**
     * Check if reminder notifications are enabled
     */
    public static boolean areReminderNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_REMINDER_NOTIFICATIONS, true);
    }
    
    /**
     * Check if notification sound is enabled
     */
    public static boolean isNotificationSoundEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATION_SOUND, true);
    }
    
    /**
     * Check if notification vibration is enabled
     */
    public static boolean isNotificationVibrationEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATION_VIBRATION, true);
    }
    
    /**
     * Check if early reminders are enabled
     */
    public static boolean areEarlyRemindersEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_EARLY_REMINDERS, true);
    }
    
    /**
     * Get snooze duration in minutes
     */
    public static int getSnoozeDuration(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SNOOZE_DURATION, 15); // Default 15 minutes
    }
    
    /**
     * Set workflow notification preference
     */
    public static void setWorkflowNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_WORKFLOW_NOTIFICATIONS, enabled).apply();
    }
    
    /**
     * Set reminder notification preference
     */
    public static void setReminderNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_REMINDER_NOTIFICATIONS, enabled).apply();
    }
    
    /**
     * Set snooze duration preference
     */
    public static void setSnoozeDuration(Context context, int minutes) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_SNOOZE_DURATION, minutes).apply();
    }
    
    /**
     * Calculate if a task time has passed
     */
    public static boolean hasTaskTimePassed(Task task) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            Date taskDate = dateFormat.parse(task.getDueDate());
            Date endTime = timeFormat.parse(task.getEndTime());
            
            Calendar taskEndTime = Calendar.getInstance();
            taskEndTime.setTime(taskDate);
            
            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(endTime);
            
            taskEndTime.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            taskEndTime.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            taskEndTime.set(Calendar.SECOND, 0);
            
            return System.currentTimeMillis() > taskEndTime.getTimeInMillis();
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking if task time has passed", e);
            return false;
        }
    }
    
    /**
     * Get time until task in minutes
     */
    public static long getMinutesUntilTask(Task task) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            Date taskDate = dateFormat.parse(task.getDueDate());
            Date startTime = timeFormat.parse(task.getStartTime());
            
            Calendar taskStartTime = Calendar.getInstance();
            taskStartTime.setTime(taskDate);
            
            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(startTime);
            
            taskStartTime.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            taskStartTime.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            taskStartTime.set(Calendar.SECOND, 0);
            
            long diffMs = taskStartTime.getTimeInMillis() - System.currentTimeMillis();
            return diffMs / (1000 * 60); // Convert to minutes
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating minutes until task", e);
            return 0;
        }
    }
    
    /**
     * Format time until task for display
     */
    public static String formatTimeUntilTask(Task task) {
        long minutes = getMinutesUntilTask(task);
        
        if (minutes < 0) {
            return "Overdue";
        } else if (minutes == 0) {
            return "Now";
        } else if (minutes < 60) {
            return minutes + " min";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + "h";
            } else {
                return hours + "h " + remainingMinutes + "m";
            }
        }
    }
    
    /**
     * Generate unique notification ID for a task
     */
    public static int generateNotificationId(String taskId, String notificationType) {
        try {
            int baseId = Integer.parseInt(taskId);
            switch (notificationType) {
                case NotificationHandler.TYPE_WORKFLOW_START:
                    return baseId * 10;
                case NotificationHandler.TYPE_WORKFLOW_END:
                    return baseId * 10 + 1;
                case NotificationHandler.TYPE_REMINDER:
                    return baseId * 10 + 2;
                case NotificationHandler.TYPE_WORKFLOW_REMINDER:
                    return baseId * 10 + 5;
                case NotificationHandler.TYPE_REMINDER_SNOOZE:
                    return baseId * 10 + 8;
                default:
                    return baseId;
            }
        } catch (NumberFormatException e) {
            return taskId.hashCode();
        }
    }
}
