package com.simats.schedulytic;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Comprehensive Reminder Notification Manager
 * Handles:
 * - 5-minute pre-notification
 * - On-time notification with vibration and sound
 * - Task completion/skip actions
 * - Reward (+2 XP) / Penalty (-0.5 XP) system
 */
public class ReminderNotificationManager {
    private static final String TAG = "ReminderNotifManager";
    
    // Notification Channels
    private static final String PRE_REMINDER_CHANNEL_ID = "pre_reminder_channel";
    private static final String REMINDER_CHANNEL_ID = "reminder_main_channel";
    
    // Notification Types
    public static final String TYPE_PRE_REMINDER = "pre_reminder";      // 5 min before
    public static final String TYPE_EXACT_REMINDER = "exact_reminder";  // On time
    
    // Actions
    public static final String ACTION_COMPLETE_TASK = "com.simats.schedulytic.COMPLETE_TASK";
    public static final String ACTION_CANT_COMPLETE = "com.simats.schedulytic.CANT_COMPLETE";
    public static final String ACTION_SNOOZE_TASK = "com.simats.schedulytic.SNOOZE_TASK";
    public static final String ACTION_DISMISS = "com.simats.schedulytic.DISMISS_REMINDER";
    
    // Reward/Penalty values
    public static final double REWARD_ON_TIME_COMPLETION = 2.0;
    public static final double REWARD_WORKFLOW_COMPLETION = 2.5;  // Higher reward for workflow tasks
    public static final double PENALTY_SKIP = -0.5;
    public static final double NO_REWARD_EXTENDED = 0.0;  // No reward if extended
    
    private Context context;
    private NotificationManager notificationManager;
    private AlarmManager alarmManager;
    
    public ReminderNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        createNotificationChannels();
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Pre-reminder channel (gentle notification)
            NotificationChannel preReminderChannel = new NotificationChannel(
                    PRE_REMINDER_CHANNEL_ID,
                    "Pre-Reminder Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            preReminderChannel.setDescription("5-minute advance reminders for tasks");
            preReminderChannel.enableVibration(false);
            preReminderChannel.setSound(null, null);
            notificationManager.createNotificationChannel(preReminderChannel);
            
            // Main reminder channel (urgent with sound + vibration)
            NotificationChannel reminderChannel = new NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            reminderChannel.setDescription("On-time task reminders with alerts");
            reminderChannel.enableVibration(true);
            reminderChannel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            reminderChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes);
            reminderChannel.enableLights(true);
            reminderChannel.setLightColor(0xFFFF5722); // Orange light
            notificationManager.createNotificationChannel(reminderChannel);
        }
    }
    
    /**
     * Schedule both pre-reminder (5 min before) and exact reminder for a task
     */
    public void scheduleReminderNotifications(Task task) {
        try {
            if (task == null || task.getStartTime() == null || task.getDueDate() == null) {
                Log.e(TAG, "Invalid task data for scheduling");
                return;
            }
            
            // Only schedule for remainder type tasks
            if (!task.isRemainder()) {
                Log.d(TAG, "Task is not a reminder type, skipping: " + task.getTitle());
                return;
            }
            
            long exactTimeMillis = parseTaskDateTime(task);
            if (exactTimeMillis <= 0) {
                Log.e(TAG, "Failed to parse task time");
                return;
            }
            
            // Schedule 5-minute pre-reminder
            long preReminderTime = exactTimeMillis - (5 * 60 * 1000); // 5 minutes before
            if (preReminderTime > System.currentTimeMillis()) {
                schedulePreReminder(task, preReminderTime);
            }
            
            // Schedule exact time reminder
            if (exactTimeMillis > System.currentTimeMillis()) {
                scheduleExactReminder(task, exactTimeMillis);
            }
            
            Log.d(TAG, "Scheduled reminders for task: " + task.getTitle());
            
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder notifications", e);
        }
    }
    
    private long parseTaskDateTime(Task task) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            String startTimeStr = task.getStartTime();
            String dueDateStr = task.getDueDate();
            
            Calendar calendar = Calendar.getInstance();
            
            if (startTimeStr != null && startTimeStr.contains(" ")) {
                // Full datetime format
                Date fullDate = fullFormat.parse(startTimeStr);
                return fullDate.getTime();
            } else {
                // Time only format - combine with due date
                Date dueDate = dateFormat.parse(dueDateStr);
                Date startTime = timeFormat.parse(startTimeStr);
                
                calendar.setTime(dueDate);
                Calendar timeCal = Calendar.getInstance();
                timeCal.setTime(startTime);
                
                calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                calendar.set(Calendar.SECOND, 0);
                
                return calendar.getTimeInMillis();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing task datetime", e);
            return -1;
        }
    }
    
    private void schedulePreReminder(Task task, long triggerTime) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(TYPE_PRE_REMINDER);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_description", task.getDescription());
        intent.putExtra("notification_type", TYPE_PRE_REMINDER);
        
        int requestCode = (task.getTaskId() + "_pre").hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        scheduleExactAlarm(triggerTime, pendingIntent);
        Log.d(TAG, "Scheduled pre-reminder for: " + task.getTitle() + " at " + new Date(triggerTime));
    }
    
    private void scheduleExactReminder(Task task, long triggerTime) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(TYPE_EXACT_REMINDER);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_description", task.getDescription());
        intent.putExtra("notification_type", TYPE_EXACT_REMINDER);
        intent.putExtra("start_time", task.getStartTime());
        intent.putExtra("due_date", task.getDueDate());
        
        int requestCode = (task.getTaskId() + "_exact").hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        scheduleExactAlarm(triggerTime, pendingIntent);
        Log.d(TAG, "Scheduled exact reminder for: " + task.getTitle() + " at " + new Date(triggerTime));
    }
    
    private void scheduleExactAlarm(long triggerTime, PendingIntent pendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling exact alarm", e);
        }
    }
    
    /**
     * Cancel all reminder notifications for a task
     */
    public void cancelReminderNotifications(String taskId) {
        try {
            // Cancel pre-reminder
            Intent preIntent = new Intent(context, ReminderReceiver.class);
            int preRequestCode = (taskId + "_pre").hashCode();
            PendingIntent prePendingIntent = PendingIntent.getBroadcast(
                    context, preRequestCode, preIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(prePendingIntent);
            
            // Cancel exact reminder
            Intent exactIntent = new Intent(context, ReminderReceiver.class);
            int exactRequestCode = (taskId + "_exact").hashCode();
            PendingIntent exactPendingIntent = PendingIntent.getBroadcast(
                    context, exactRequestCode, exactIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(exactPendingIntent);
            
            // Cancel active notifications
            notificationManager.cancel(taskId.hashCode());
            notificationManager.cancel((taskId + "_pre").hashCode());
            notificationManager.cancel((taskId + "_exact").hashCode());
            
            Log.d(TAG, "Cancelled all reminders for task: " + taskId);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling reminders", e);
        }
    }
    
    /**
     * Show pre-reminder notification (5 min before)
     */
    public void showPreReminderNotification(String taskId, String title, String description) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PRE_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_remainder)
                .setContentTitle("⏰ Upcoming: " + title)
                .setContentText("Starting in 5 minutes - Get ready!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your task \"" + title + "\" starts in 5 minutes.\n" + 
                                (description != null && !description.isEmpty() ? description : "Prepare to begin!")))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setColor(0xFF4CAF50); // Green color
        
        // Open app when tapped
        Intent tapIntent = new Intent(context, MainNavigationActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tapPendingIntent = PendingIntent.getActivity(context, taskId.hashCode(), tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(tapPendingIntent);
        
        notificationManager.notify((taskId + "_pre").hashCode(), builder.build());
        Log.d(TAG, "Showed pre-reminder for: " + title);
    }
    
    /**
     * Show exact time reminder notification with vibration and sound
     */
    public void showExactReminderNotification(String taskId, String title, String description) {
        // Trigger vibration
        triggerVibration();
        
        // Cancel any existing pre-reminder notification for this task
        notificationManager.cancel((taskId + "_pre").hashCode());
        
        int notificationId = (taskId + "_exact").hashCode();
        
        // Create action intents - Use Activity PendingIntents for Android 12+ compatibility
        // Complete action - Direct to TaskCompletionActivity
        Intent completeIntent = new Intent(context, TaskCompletionActivity.class);
        completeIntent.putExtra("task_id", taskId);
        completeIntent.putExtra("task_title", title);
        completeIntent.putExtra("notification_id", notificationId);
        completeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent completePendingIntent = PendingIntent.getActivity(context, 
                (taskId + "_complete").hashCode(), completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Can't Complete - Direct Activity intent (Android 12+ compatible)
        Intent cantCompleteIntent = new Intent(context, CantCompleteTaskActivity.class);
        cantCompleteIntent.setAction(ACTION_CANT_COMPLETE);
        cantCompleteIntent.putExtra("task_id", taskId);
        cantCompleteIntent.putExtra("task_title", title);
        cantCompleteIntent.putExtra("notification_id", notificationId);
        cantCompleteIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent cantCompletePendingIntent = PendingIntent.getActivity(context,
                (taskId + "_cant").hashCode(), cantCompleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Snooze - Direct Activity intent (Android 12+ compatible) 
        Intent snoozeIntent = new Intent(context, SnoozePickerActivity.class);
        snoozeIntent.putExtra("task_id", Integer.parseInt(taskId));
        snoozeIntent.putExtra("task_title", title);
        snoozeIntent.putExtra("notification_id", notificationId);
        snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent snoozePendingIntent = PendingIntent.getActivity(context,
                (taskId + "_snooze").hashCode(), snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_remainder)
                .setContentTitle("🔔 Time for: " + title)
                .setContentText("It's time to complete your task!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your scheduled task is due NOW!\n\n" +
                                "📌 " + title + "\n" +
                                (description != null && !description.isEmpty() ? "📝 " + description : "")))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true) // Makes it persistent
                .setColor(0xFFFF5722) // Orange color
                .setVibrate(new long[]{0, 500, 200, 500, 200, 500})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .addAction(R.drawable.ic_check, "✅ Completed", completePendingIntent)
                .addAction(R.drawable.ic_close, "❌ Can't Do", cantCompletePendingIntent)
                .addAction(R.drawable.ic_snooze, "⏰ Snooze", snoozePendingIntent);
        
        // Open app when tapped
        Intent tapIntent = new Intent(context, MainNavigationActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tapPendingIntent = PendingIntent.getActivity(context, taskId.hashCode(), tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(tapPendingIntent);
        
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Showed exact reminder for: " + title);
    }
    
    private void triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    Vibrator vibrator = vibratorManager.getDefaultVibrator();
                    vibrator.vibrate(VibrationEffect.createWaveform(
                            new long[]{0, 500, 200, 500, 200, 500}, -1));
                }
            } else {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(
                                new long[]{0, 500, 200, 500, 200, 500}, -1));
                    } else {
                        vibrator.vibrate(new long[]{0, 500, 200, 500, 200, 500}, -1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering vibration", e);
        }
    }
    
    /**
     * Update task status and apply rewards/penalties
     */
    public void handleTaskCompletion(String taskId, String status, TaskCompletionCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        
        if (userId.isEmpty()) {
            Log.e(TAG, "No user ID found");
            if (callback != null) callback.onError("User not logged in");
            return;
        }
        
        double xpChange = 0;
        String completionStatus = "";
        
        switch (status) {
            case "completed":
                xpChange = REWARD_ON_TIME_COMPLETION;
                completionStatus = "completed";
                break;
            case "skipped":
            case "cant_complete":
                xpChange = PENALTY_SKIP;
                completionStatus = "skipped";
                break;
            default:
                completionStatus = status;
                break;
        }
        
        // Update task status in database
        updateTaskStatusInDatabase(taskId, completionStatus, xpChange, callback);
    }
    
    /**
     * Handle workflow task completion with extended flag
     * @param taskId Task ID
     * @param status Status (completed, skipped, cant_complete)
     * @param wasExtended True if task was extended (no reward on completion)
     * @param callback Callback for success/error
     */
    public void handleWorkflowCompletion(String taskId, String status, boolean wasExtended, TaskCompletionCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        
        if (userId.isEmpty()) {
            Log.e(TAG, "No user ID found");
            if (callback != null) callback.onError("User not logged in");
            return;
        }
        
        double xpChange = 0;
        String completionStatus = "";
        
        switch (status) {
            case "completed":
                if (wasExtended) {
                    xpChange = NO_REWARD_EXTENDED;  // No reward if task was extended
                } else {
                    xpChange = REWARD_WORKFLOW_COMPLETION;  // +2.5 XP for on-time workflow completion
                }
                completionStatus = "completed";
                break;
            case "skipped":
            case "cant_complete":
                xpChange = PENALTY_SKIP;  // -0.5 XP for skipping
                completionStatus = "skipped";
                break;
            default:
                completionStatus = status;
                break;
        }
        
        // Update task status in database
        updateTaskStatusInDatabase(taskId, completionStatus, xpChange, callback);
    }
    
    private void updateTaskStatusInDatabase(String taskId, String status, double xpChange, TaskCompletionCallback callback) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("task_id", taskId);
            requestBody.put("user_id", userId);
            requestBody.put("status", status);
            requestBody.put("xp_change", xpChange);
            requestBody.put("completed_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            
            String url = VolleyNetworkManager.getInstance(context).getBaseUrl() + "complete_task_with_reward.php";
            
            VolleyNetworkManager.getInstance(context).makePostRequest(
                    url,
                    requestBody,
                    new VolleyNetworkManager.JsonResponseListener() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d(TAG, "Task status updated: " + response.toString());
                            
                            // Update SharedPreferences with new XP from server
                            try {
                                if (response.has("data")) {
                                    JSONObject data = response.getJSONObject("data");
                                    if (data.has("new_xp")) {
                                        double newXp = data.getDouble("new_xp");
                                        updateLocalXPAfterCompletion(newXp, xpChange, status);
                                        Log.d(TAG, "Updated local XP to: " + newXp);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating local XP: " + e.getMessage());
                            }
                            
                            if (callback != null) {
                                callback.onSuccess(xpChange);
                            }
                        }
                        
                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "Error updating task status: " + message);
                            if (callback != null) {
                                callback.onError(message);
                            }
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating request", e);
            if (callback != null) callback.onError(e.getMessage());
        }
    }
    
    /**
     * Update local XP in SharedPreferences after task completion
     * This syncs the XP across HomeFragment and ProfileFragment
     */
    private void updateLocalXPAfterCompletion(double newXp, double xpChange, String status) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Update total XP (all keys used by different fragments)
        editor.putInt("xp_points", (int) newXp);
        editor.putInt("xp_coins", (int) newXp);
        editor.putFloat("xp_points_float", (float) newXp);
        
        // Update reminder/workflow-specific XP breakdown if it was a reward
        if (xpChange > 0 && "completed".equals(status)) {
            // Determine if this was a workflow or reminder task based on XP value
            // Workflow gives 2.5 XP, Reminder gives 2.0 XP
            // Use threshold of 2.25 to reliably distinguish between them
            if (xpChange > 2.25) {
                // Workflow task (2.5 XP)
                float workflowXP = prefs.getFloat("workflow_xp", 0f);
                editor.putFloat("workflow_xp", workflowXP + (float) xpChange);
                
                int workflowCount = prefs.getInt("workflow_completed_count", 0);
                editor.putInt("workflow_completed_count", workflowCount + 1);
                Log.d(TAG, "Updated workflow XP: +" + xpChange + " (New total: " + (workflowXP + xpChange) + ")");
            } else {
                // Reminder task (2.0 XP)
                float remainderXP = prefs.getFloat("remainder_xp", 0f);
                editor.putFloat("remainder_xp", remainderXP + (float) xpChange);
                
                int remainderCount = prefs.getInt("remainder_completed_count", 0);
                editor.putInt("remainder_completed_count", remainderCount + 1);
                Log.d(TAG, "Updated reminder XP: +" + xpChange + " (New total: " + (remainderXP + xpChange) + ")");
            }
        }
        
        editor.apply();
        Log.d(TAG, "Updated local XP - Total: " + newXp + ", Change: " + xpChange);
    }
    
    public interface TaskCompletionCallback {
        void onSuccess(double xpChange);
        void onError(String message);
    }
    
    /**
     * BroadcastReceiver for reminder alarms
     */
    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String notificationType = intent.getStringExtra("notification_type");
            
            // Handle task_id as either String or Integer
            String taskId = intent.getStringExtra("task_id");
            if (taskId == null) {
                int taskIdInt = intent.getIntExtra("task_id", -1);
                if (taskIdInt != -1) {
                    taskId = String.valueOf(taskIdInt);
                }
            }
            
            String taskTitle = intent.getStringExtra("task_title");
            String taskDescription = intent.getStringExtra("task_description");
            boolean isSnooze = intent.getBooleanExtra("is_snooze", false);
            
            Log.d(TAG, "Received reminder: " + notificationType + " for task: " + taskTitle + " isSnooze: " + isSnooze);
            
            // Safety check for taskId
            if (taskId == null || taskId.equals("null")) {
                Log.e(TAG, "Task ID is null, cannot show notification");
                return;
            }
            
            ReminderNotificationManager manager = new ReminderNotificationManager(context);
            
            // Handle snooze reminder (treat as exact reminder)
            if (isSnooze || "com.simats.schedulytic.SNOOZE_REMINDER".equals(action)) {
                manager.showExactReminderNotification(taskId, taskTitle, taskDescription);
                return;
            }
            
            if (TYPE_PRE_REMINDER.equals(notificationType)) {
                manager.showPreReminderNotification(taskId, taskTitle, taskDescription);
            } else if (TYPE_EXACT_REMINDER.equals(notificationType)) {
                manager.showExactReminderNotification(taskId, taskTitle, taskDescription);
            }
        }
    }
    
    /**
     * BroadcastReceiver for notification action buttons (legacy/fallback)
     * Most actions are now handled directly via Activity PendingIntents
     */
    public static class ReminderActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String taskId = intent.getStringExtra("task_id");
            int notificationId = intent.getIntExtra("notification_id", 0);
            
            Log.d(TAG, "Action received (legacy): " + action + " for task: " + taskId);
            
            NotificationManager notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Dismiss notifications
            if (notifManager != null) {
                if (notificationId != 0) {
                    notifManager.cancel(notificationId);
                }
                if (taskId != null) {
                    notifManager.cancel((taskId + "_exact").hashCode());
                    notifManager.cancel((taskId + "_pre").hashCode());
                }
            }
            
            // Note: Most actions are now handled via direct Activity intents
            // This receiver is kept for backward compatibility and dismiss actions
            if (ACTION_DISMISS.equals(action)) {
                Log.d(TAG, "Notification dismissed for task: " + taskId);
            }
        }
    }
}
