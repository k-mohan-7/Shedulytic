package com.example.shedulytic;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationHandler {
    private static final String TAG = "NotificationHandler";
    private static final String CHANNEL_ID = "schedlytic_channel";
    private static final String CHANNEL_NAME = "Schedlytic Notifications";
    private static final String WORKFLOW_CHANNEL_ID = "workflow_channel";
    private static final String REMINDER_CHANNEL_ID = "reminder_channel";
    private static final int NOTIFICATION_ID = 1000;
    
    // Notification types
    public static final String TYPE_WORKFLOW_START = "workflow_start";
    public static final String TYPE_WORKFLOW_END = "workflow_end";
    public static final String TYPE_WORKFLOW_REMINDER = "workflow_reminder";
    public static final String TYPE_REMINDER = "remainder";
    public static final String TYPE_REMINDER_SNOOZE = "reminder_snooze";
    
    // Actions
    public static final String ACTION_START_TASK = "start_task";
    public static final String ACTION_COMPLETE_TASK = "complete_task";
    public static final String ACTION_EXTEND_TASK = "extend_task";
    public static final String ACTION_SNOOZE_REMINDER = "snooze_reminder";
    public static final String ACTION_DISMISS_REMINDER = "dismiss_reminder";
    public static final String ACTION_CANT_COMPLETE = "cant_complete";
    
    private Context context;
    private NotificationManager notificationManager;
      public NotificationHandler(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
        checkAndRequestExactAlarmPermission();
    }
      private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main channel
            NotificationChannel mainChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            mainChannel.setDescription("Main channel for Schedlytic app notifications");
            mainChannel.enableLights(true);
            mainChannel.enableVibration(true);
            notificationManager.createNotificationChannel(mainChannel);
            
            // Workflow channel - High priority for time-sensitive tasks
            NotificationChannel workflowChannel = new NotificationChannel(
                    WORKFLOW_CHANNEL_ID,
                    "Workflow Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            workflowChannel.setDescription("Notifications for workflow tasks");
            workflowChannel.enableLights(true);
            workflowChannel.enableVibration(true);
            workflowChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
            notificationManager.createNotificationChannel(workflowChannel);
            
            // Reminder channel - Medium priority for general reminders
            NotificationChannel reminderChannel = new NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Reminder Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);            reminderChannel.setDescription("Notifications for reminder tasks");
            reminderChannel.enableLights(false);
            reminderChannel.enableVibration(true);
            notificationManager.createNotificationChannel(reminderChannel);
        }
    }

    /**
     * Check and request exact alarm permission for Android 12+ (API 31+)
     */
    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            
            // Check if we can schedule exact alarms
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission not granted. Requesting permission...");
                
                // Show toast to user about alarm permission
                Toast.makeText(context, "Please allow exact alarms for notifications to work properly", 
                    Toast.LENGTH_LONG).show();
                
                // Intent to open exact alarm settings
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open exact alarm settings", e);
                    
                    // Fallback: open app settings
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception fallbackE) {
                        Log.e(TAG, "Failed to open app settings", fallbackE);
                    }
                }
            } else if (alarmManager != null) {
                Log.d(TAG, "Exact alarm permission already granted");
            }
        }
    }
    
    /**
     * Check if exact alarms can be scheduled
     */
    public boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // Always true for Android < 12
    }
      public void scheduleTaskNotification(Task task) {
        try {
            Log.d(TAG, "Scheduling notification for task: " + task.getTitle() + " Type: " + task.getType());
            
            // Schedule based on task type
            if (task.isWorkflow()) {
                // For workflow tasks, schedule notifications at start and end times
                scheduleWorkflowStartNotification(task);
                scheduleWorkflowEndNotification(task);
                
                // Schedule intermediate reminders for longer workflows
                scheduleWorkflowReminders(task);
                
            } else if (task.isRemainder()) {
                // For remainder tasks, schedule a single notification at the start time
                scheduleRemainderNotification(task);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling notification: ", e);
        }
    }
    
    /**
     * Schedule intermediate reminders for long workflow tasks
     */    private void scheduleWorkflowReminders(Task task) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        Date taskDate = dateFormat.parse(task.getDueDate());
        
        // Parse start and end times properly
        String startTimeStr = task.getStartTime();
        String endTimeStr = task.getEndTime();
        Date startDateTime, endDateTime;
        
        if (startTimeStr != null && startTimeStr.contains(" ")) {
            // Full datetime format: "YYYY-MM-DD HH:MM:SS"
            startDateTime = datetimeFormat.parse(startTimeStr);
            endDateTime = datetimeFormat.parse(endTimeStr);
        } else {
            // Fallback for time-only format: "HH:MM"
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startTime = timeFormat.parse(startTimeStr);
            Date endTime = timeFormat.parse(endTimeStr);
            
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(taskDate);
            Calendar tempTimeCal = Calendar.getInstance();
            tempTimeCal.setTime(startTime);
            startCal.set(Calendar.HOUR_OF_DAY, tempTimeCal.get(Calendar.HOUR_OF_DAY));
            startCal.set(Calendar.MINUTE, tempTimeCal.get(Calendar.MINUTE));
            startCal.set(Calendar.SECOND, 0);
            startDateTime = startCal.getTime();
            
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(taskDate);
            tempTimeCal.setTime(endTime);
            endCal.set(Calendar.HOUR_OF_DAY, tempTimeCal.get(Calendar.HOUR_OF_DAY));
            endCal.set(Calendar.MINUTE, tempTimeCal.get(Calendar.MINUTE));
            endCal.set(Calendar.SECOND, 0);
            endDateTime = endCal.getTime();
        }
        
        // Calculate task duration in minutes
        long durationMs = endDateTime.getTime() - startDateTime.getTime();
        long durationMinutes = durationMs / (1000 * 60);
          // Schedule reminders for workflows longer than 30 minutes
        if (durationMinutes > 30) {
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.setTime(startDateTime);
            
            // Add reminder at halfway point
            reminderTime.add(Calendar.MINUTE, (int)(durationMinutes / 2));
            
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.putExtra("task_id", task.getTaskId());
            intent.putExtra("task_title", task.getTitle());
            intent.putExtra("notification_type", TYPE_WORKFLOW_REMINDER);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    Integer.parseInt(task.getTaskId()) * 10 + 5,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
              scheduleExactNotification(intent, pendingIntent, reminderTime.getTimeInMillis());
            
            Log.d(TAG, "Scheduled workflow reminder for task: " + task.getTitle());
        }
    }
      /**
     * Cancel all notifications for a specific task
     */
    public void cancelTaskNotifications(String taskId) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            int taskIdInt = Integer.parseInt(taskId);
            
            // Cancel all scheduled notification alarms using the same PendingIntent patterns used in scheduling
            
            // Cancel start notification (ID: taskId * 10)
            Intent startIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent startPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskIdInt * 10,
                    startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(startPendingIntent);
            
            // Cancel end notification (ID: taskId * 10 + 1)
            Intent endIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent endPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskIdInt * 10 + 1,
                    endIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(endPendingIntent);
            
            // Cancel reminder notification (ID: taskId * 10 + 2)
            Intent reminderIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent reminderPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskIdInt * 10 + 2,
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(reminderPendingIntent);
            
            // Cancel workflow reminder notification (ID: taskId * 10 + 5)
            Intent workflowReminderIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent workflowReminderPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskIdInt * 10 + 5,
                    workflowReminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(workflowReminderPendingIntent);
            
            // Cancel early reminder notification (ID: taskId * 10 + 3)
            Intent earlyReminderIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent earlyReminderPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskIdInt * 10 + 3,
                    earlyReminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(earlyReminderPendingIntent);
            
            // Cancel warning reminder notification (ID: taskId * 10 + 7)
            Intent warningReminderIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent warningReminderPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskIdInt * 10 + 7,
                    warningReminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(warningReminderPendingIntent);
            
            // Cancel snooze reminder notification (ID: taskId * 10 + 8)
            Intent snoozeReminderIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent snoozeReminderPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskIdInt * 10 + 8,
                    snoozeReminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(snoozeReminderPendingIntent);
            
            // Dismiss ALL active notifications for this task using different possible notification IDs
            
            // Main notification using taskId
            notificationManager.cancel(taskIdInt);
            
            // Action-based notification IDs used by TaskActionReceiver
            notificationManager.cancel((taskId + "_complete").hashCode());
            notificationManager.cancel((taskId + "_extend").hashCode());
            notificationManager.cancel((taskId + "_cant_complete").hashCode());
            
            // Notification IDs used by action buttons (taskId * 100 + offset pattern)
            for (int offset = 0; offset <= 10; offset++) {
                notificationManager.cancel(taskIdInt * 100 + offset);
            }
            
            Log.d(TAG, "Cancelled all notifications and alarms for task: " + taskId);
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid task ID format for cancellation: " + taskId, e);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notifications for task: " + taskId, e);
        }
    }private void scheduleWorkflowStartNotification(Task task) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        Date taskDate = dateFormat.parse(task.getDueDate());
        
        // Parse the full datetime format from getStartTime()
        String startTimeStr = task.getStartTime();
        Date startDateTime;
        
        if (startTimeStr != null && startTimeStr.contains(" ")) {
            // Full datetime format: "YYYY-MM-DD HH:MM:SS"
            startDateTime = datetimeFormat.parse(startTimeStr);
        } else {
            // Fallback for time-only format: "HH:MM"
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startTime = timeFormat.parse(startTimeStr);
            
            Calendar tempCal = Calendar.getInstance();
            tempCal.setTime(taskDate);
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(startTime);
            
            tempCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            tempCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            tempCal.set(Calendar.SECOND, 0);
            
            startDateTime = tempCal.getTime();        }
          Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDateTime);
        
        // Schedule 5 minutes before start time as well
        Calendar earlyReminder = (Calendar) calendar.clone();
        earlyReminder.add(Calendar.MINUTE, -5);
        
        // Create intent for early reminder
        Intent earlyIntent = new Intent(context, NotificationReceiver.class);
        earlyIntent.putExtra("task_id", task.getTaskId());
        earlyIntent.putExtra("task_title", task.getTitle());
        earlyIntent.putExtra("notification_type", TYPE_WORKFLOW_REMINDER);
        earlyIntent.putExtra("reminder_text", "Starting in 5 minutes");
        
        PendingIntent earlyPendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(task.getTaskId()) * 10 + 6,
                earlyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Create intent for start notification
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("notification_type", TYPE_WORKFLOW_START);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(task.getTaskId()) * 10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
          // Schedule early reminder if it's in the future
        if (earlyReminder.getTimeInMillis() > System.currentTimeMillis()) {
            scheduleExactNotification(earlyIntent, earlyPendingIntent, earlyReminder.getTimeInMillis());
        }
        
        // Schedule start notification
        scheduleExactNotification(intent, pendingIntent, calendar.getTimeInMillis());
        
        Log.d(TAG, "Scheduled workflow start notification for: " + task.getTitle() + " at " + calendar.getTime());
    }    private void scheduleWorkflowEndNotification(Task task) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        Date taskDate = dateFormat.parse(task.getDueDate());
        
        // Parse the full datetime format from getEndTime()
        String endTimeStr = task.getEndTime();
        Date endDateTime;
        
        if (endTimeStr != null && endTimeStr.contains(" ")) {
            // Full datetime format: "YYYY-MM-DD HH:MM:SS"
            endDateTime = datetimeFormat.parse(endTimeStr);
        } else {
            // Fallback for time-only format: "HH:MM"
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date endTime = timeFormat.parse(endTimeStr);
            
            Calendar tempCal = Calendar.getInstance();
            tempCal.setTime(taskDate);
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(endTime);
            
            tempCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            tempCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            tempCal.set(Calendar.SECOND, 0);
            
            endDateTime = tempCal.getTime();
        }
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDateTime);
        
        // Schedule 10 minutes before end time as well
        Calendar warningReminder = (Calendar) calendar.clone();
        warningReminder.add(Calendar.MINUTE, -10);
        
        // Create intent for warning reminder
        Intent warningIntent = new Intent(context, NotificationReceiver.class);
        warningIntent.putExtra("task_id", task.getTaskId());
        warningIntent.putExtra("task_title", task.getTitle());
        warningIntent.putExtra("notification_type", TYPE_WORKFLOW_REMINDER);
        warningIntent.putExtra("reminder_text", "Ending in 10 minutes");
        
        PendingIntent warningPendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(task.getTaskId()) * 10 + 7,
                warningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Create intent for end notification
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("notification_type", TYPE_WORKFLOW_END);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(task.getTaskId()) * 10 + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
          // Schedule warning reminder if it's in the future
        if (warningReminder.getTimeInMillis() > System.currentTimeMillis()) {
            scheduleExactNotification(warningIntent, warningPendingIntent, warningReminder.getTimeInMillis());
        }
        
        // Schedule end notification
        scheduleExactNotification(intent, pendingIntent, calendar.getTimeInMillis());
        
        Log.d(TAG, "Scheduled workflow end notification for: " + task.getTitle() + " at " + calendar.getTime());
    }    private void scheduleRemainderNotification(Task task) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        Date taskDate = dateFormat.parse(task.getDueDate());
        
        // Parse the full datetime format from getStartTime()
        String startTimeStr = task.getStartTime();
        Date startDateTime;
        
        if (startTimeStr != null && startTimeStr.contains(" ")) {
            // Full datetime format: "YYYY-MM-DD HH:MM:SS"
            startDateTime = datetimeFormat.parse(startTimeStr);
        } else {
            // Fallback for time-only format: "HH:MM"
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startTime = timeFormat.parse(startTimeStr);
            
            Calendar tempCal = Calendar.getInstance();
            tempCal.setTime(taskDate);
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(startTime);
            
            tempCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            tempCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            tempCal.set(Calendar.SECOND, 0);
            
            startDateTime = tempCal.getTime();
        }
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDateTime);
        
        // Create intent for notification
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_description", task.getDescription());
        intent.putExtra("notification_type", TYPE_REMINDER);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(task.getTaskId()) * 10 + 2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
          scheduleExactNotification(intent, pendingIntent, calendar.getTimeInMillis());
        
        Log.d(TAG, "Scheduled reminder notification for: " + task.getTitle() + " at " + calendar.getTime());
    }
    
    /**
     * Schedule a snooze reminder for the given task
     */
    public void scheduleSnoozeReminder(String taskId, String taskTitle, int snoozeMinutes) {
        Calendar snoozeTime = Calendar.getInstance();
        snoozeTime.add(Calendar.MINUTE, snoozeMinutes);
        
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", taskId);
        intent.putExtra("task_title", taskTitle);
        intent.putExtra("notification_type", TYPE_REMINDER_SNOOZE);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(taskId) * 10 + 8,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
          scheduleExactNotification(intent, pendingIntent, snoozeTime.getTimeInMillis());
        
        Log.d(TAG, "Scheduled snooze reminder for: " + taskTitle + " in " + snoozeMinutes + " minutes");
    }
      // BroadcastReceiver to handle notifications
    public static class NotificationReceiver extends BroadcastReceiver {
        @SuppressLint("NotificationTrampoline")
        @Override
        public void onReceive(Context context, Intent intent) {
            String taskId = intent.getStringExtra("task_id");
            String taskTitle = intent.getStringExtra("task_title");
            String taskDescription = intent.getStringExtra("task_description");
            String notificationType = intent.getStringExtra("notification_type");
            String reminderText = intent.getStringExtra("reminder_text");
            
            Log.d("NotificationReceiver", "Received notification: " + notificationType + " for task: " + taskTitle);
            
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Add vibration for important notifications
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            
            // Create notification based on type
            NotificationCompat.Builder builder;
            
            if (TYPE_WORKFLOW_START.equals(notificationType)) {
                builder = createWorkflowStartNotification(context, taskId, taskTitle);
                
                // Strong vibration for workflow start
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
                    } else {
                        vibrator.vibrate(new long[]{0, 500, 200, 500}, -1);
                    }
                }
                
            } else if (TYPE_WORKFLOW_END.equals(notificationType)) {
                builder = createWorkflowEndNotification(context, taskId, taskTitle);
                
                // Medium vibration for workflow end
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(300);
                    }
                }
                
            } else if (TYPE_WORKFLOW_REMINDER.equals(notificationType)) {
                builder = createWorkflowReminderNotification(context, taskId, taskTitle, reminderText);
                
                // Light vibration for reminders
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(200);
                    }
                }
                
            } else if (TYPE_REMINDER.equals(notificationType) || TYPE_REMINDER_SNOOZE.equals(notificationType)) {
                builder = createReminderNotification(context, taskId, taskTitle, taskDescription, TYPE_REMINDER_SNOOZE.equals(notificationType));
                
                // Light vibration for reminders
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(200);
                    }
                }
                
            } else {
                // Default notification
                builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notifications)
                        .setContentTitle("Schedlytic")
                        .setContentText(taskTitle)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            }
            
            // Set notification as ongoing for workflow tasks
            if (TYPE_WORKFLOW_START.equals(notificationType) || TYPE_WORKFLOW_REMINDER.equals(notificationType)) {
                builder.setOngoing(true);
            }
            
            notificationManager.notify(Integer.parseInt(taskId), builder.build());
        }
          private NotificationCompat.Builder createWorkflowStartNotification(Context context, String taskId, String taskTitle) {
            // Create RemoteViews for rich notification layout
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_workflow_start_layout);
            
            // Set content
            remoteViews.setTextViewText(R.id.task_title, taskTitle);
            remoteViews.setTextViewText(R.id.task_message, "Time to start: " + taskTitle);
            remoteViews.setTextViewText(R.id.notification_time, getCurrentTimeString());
            
            // Create action intents
            Intent startIntent = new Intent(context, TaskActionReceiver.class);
            startIntent.putExtra("task_id", taskId);
            startIntent.putExtra("action", ACTION_START_TASK);
            PendingIntent startPendingIntent = PendingIntent.getBroadcast(
                    context, Integer.parseInt(taskId) * 100, startIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            Intent cantIntent = new Intent(context, TaskActionReceiver.class);
            cantIntent.putExtra("task_id", taskId);
            cantIntent.putExtra("action", ACTION_CANT_COMPLETE);
            PendingIntent cantPendingIntent = PendingIntent.getBroadcast(
                    context, Integer.parseInt(taskId) * 100 + 1, cantIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            // Set button click listeners
            remoteViews.setOnClickPendingIntent(R.id.button_left, startPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.button_right, cantPendingIntent);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WORKFLOW_CHANNEL_ID)
                    .setSmallIcon(R.drawable.workflow_logo)
                    .setCustomContentView(remoteViews)
                    .setCustomBigContentView(remoteViews)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(false)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
            
            // Add fallback action buttons for devices that don't support custom views
            builder.addAction(R.drawable.ic_play_arrow, "Start Now", startPendingIntent);
            builder.addAction(R.drawable.ic_close, "Can't Do", cantPendingIntent);
            
            return builder;
        }
          private NotificationCompat.Builder createWorkflowEndNotification(Context context, String taskId, String taskTitle) {
            // Create RemoteViews for rich notification layout
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_workflow_end_layout);
            
            // Set content
            remoteViews.setTextViewText(R.id.task_title, taskTitle);
            remoteViews.setTextViewText(R.id.task_message, "Time to finish: " + taskTitle);
            remoteViews.setTextViewText(R.id.notification_time, getCurrentTimeString());
            
            // Create action intents
            Intent completeIntent = new Intent(context, TaskActionReceiver.class);
            completeIntent.putExtra("task_id", taskId);
            completeIntent.putExtra("action", ACTION_COMPLETE_TASK);
            PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                    context, Integer.parseInt(taskId) * 100 + 2, completeIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            Intent extendIntent = new Intent(context, TaskActionReceiver.class);
            extendIntent.putExtra("task_id", taskId);
            extendIntent.putExtra("action", ACTION_EXTEND_TASK);
            PendingIntent extendPendingIntent = PendingIntent.getBroadcast(
                    context, Integer.parseInt(taskId) * 100 + 3, extendIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            // Set button click listeners
            remoteViews.setOnClickPendingIntent(R.id.button_left, completePendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.button_right, extendPendingIntent);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WORKFLOW_CHANNEL_ID)
                    .setSmallIcon(R.drawable.workflow_logo)
                    .setCustomContentView(remoteViews)
                    .setCustomBigContentView(remoteViews)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(false)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
            
            // Add fallback action buttons for devices that don't support custom views
            builder.addAction(R.drawable.ic_check, "Completed", completePendingIntent);
            builder.addAction(R.drawable.ic_schedule, "Extend Time", extendPendingIntent);
            
            return builder;
        }
        
        private NotificationCompat.Builder createWorkflowReminderNotification(Context context, String taskId, String taskTitle, String reminderText) {
            String displayText = reminderText != null ? reminderText : "In progress";
            
            return new NotificationCompat.Builder(context, WORKFLOW_CHANNEL_ID)
                    .setSmallIcon(R.drawable.workflow_logo)
                    .setContentTitle("📋 " + taskTitle)
                    .setContentText(displayText)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(false);
        }
          private NotificationCompat.Builder createReminderNotification(Context context, String taskId, String taskTitle, String taskDescription, boolean isSnooze) {
            String title = isSnooze ? "⏰ Reminder (Snoozed)" : "📝 Reminder";
            String description = taskDescription != null && !taskDescription.isEmpty() ? taskDescription : "Don't forget: " + taskTitle;
            
            // Create RemoteViews for rich notification layout
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_reminder_layout);
            
            // Set content
            remoteViews.setTextViewText(R.id.notification_type, title);
            remoteViews.setTextViewText(R.id.task_title, taskTitle);
            remoteViews.setTextViewText(R.id.task_message, description);
            remoteViews.setTextViewText(R.id.notification_time, getCurrentTimeString());
            
            // Create action intents
            Intent gotItIntent = new Intent(context, TaskActionReceiver.class);
            gotItIntent.putExtra("task_id", taskId);
            gotItIntent.putExtra("action", ACTION_DISMISS_REMINDER);
            PendingIntent gotItPendingIntent = PendingIntent.getBroadcast(
                    context, Integer.parseInt(taskId) * 100 + 4, gotItIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            Intent skipIntent = new Intent(context, TaskActionReceiver.class);
            skipIntent.putExtra("task_id", taskId);
            skipIntent.putExtra("action", "skip_for_today");
            PendingIntent skipPendingIntent = PendingIntent.getBroadcast(
                    context, Integer.parseInt(taskId) * 100 + 5, skipIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            Intent tomorrowIntent = new Intent(context, TaskActionReceiver.class);
            tomorrowIntent.putExtra("task_id", taskId);
            tomorrowIntent.putExtra("action", "reschedule_tomorrow");
            PendingIntent tomorrowPendingIntent = PendingIntent.getBroadcast(
                    context, Integer.parseInt(taskId) * 100 + 6, tomorrowIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            // Set button click listeners
            remoteViews.setOnClickPendingIntent(R.id.button_left, gotItPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.button_center, skipPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.button_right, tomorrowPendingIntent);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.remainder_logo)
                    .setCustomContentView(remoteViews)
                    .setCustomBigContentView(remoteViews)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
            
            // Add fallback action buttons for devices that don't support custom views
            builder.addAction(R.drawable.ic_check, "Got it", gotItPendingIntent);
            builder.addAction(R.drawable.ic_close, "Skip", skipPendingIntent);
            builder.addAction(R.drawable.ic_schedule, "Tomorrow", tomorrowPendingIntent);
            
            return builder;
        }
    }
      // BroadcastReceiver to handle task action buttons
    public static class TaskActionReceiver extends BroadcastReceiver {        @Override
        public void onReceive(Context context, Intent intent) {
            String taskId = intent.getStringExtra("task_id");
            String action = intent.getStringExtra("action");
            
            Log.d("TaskActionReceiver", "Received action: " + action + " for task: " + taskId);
              // Check for null values to prevent crashes
            if (action == null) {
                Log.w("TaskActionReceiver", "Received null action for task: " + (taskId != null ? taskId : "unknown") + ". Ignoring.");
                return;
            }
            
            if (taskId == null) {
                Log.w("TaskActionReceiver", "Received null taskId for action: " + action + ". Ignoring.");
                return;
            }
              
            // Handle different actions
            switch (action) {
                case ACTION_START_TASK:
                    handleStartTask(context, taskId);
                    break;
                case ACTION_COMPLETE_TASK:
                    handleCompleteTask(context, taskId);
                    break;
                case ACTION_EXTEND_TASK:
                    handleExtendTask(context, taskId);
                    break;
                case ACTION_SNOOZE_REMINDER:
                    handleSnoozeReminder(context, taskId);
                    break;
                case ACTION_DISMISS_REMINDER:
                    handleDismissReminder(context, taskId);
                    break;
                case ACTION_CANT_COMPLETE:
                    handleCantComplete(context, taskId);
                    break;
                case "skip_for_today":
                    handleSkipForToday(context, taskId);
                    break;
                case "reschedule_tomorrow":
                    handleRescheduleTomorrow(context, taskId);
                    break;
                default:
                    // Legacy actions for backward compatibility
                    handleLegacyActions(context, taskId, action);
                    break;
            }
        }
          private void handleStartTask(Context context, String taskId) {
            // Update task status to "in_progress"
            TaskManager taskManager = new NotificationHandler(context).createTaskManager(context, taskId, "Task started successfully");
            taskManager.updateTaskStatus(taskId, "in_progress");
            
            // Dismiss the start notification and show ongoing notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Integer.parseInt(taskId));
            
            // Show ongoing notification
            new NotificationHandler(context).showTaskInProgressNotification(context, taskId);
        }
        
        private void handleCompleteTask(Context context, String taskId) {
            TaskManager taskManager = new NotificationHandler(context).createTaskManager(context, taskId, "Task completed successfully");
            taskManager.updateTaskCompletion(taskId, true);
            
            // Dismiss notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Integer.parseInt(taskId));
            
            // Show completion toast
            android.widget.Toast.makeText(context, "Task marked as completed", android.widget.Toast.LENGTH_SHORT).show();
        }
        
        private void handleExtendTask(Context context, String taskId) {
            // Show extend dialog
            Intent dialogIntent = new Intent(context, ExtendTaskActivity.class);
            dialogIntent.putExtra("task_id", taskId);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dialogIntent);
        }
        
        private void handleSnoozeReminder(Context context, String taskId) {
            // Dismiss current notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Integer.parseInt(taskId));
            
            // Schedule snooze reminder (15 minutes)
            NotificationHandler handler = new NotificationHandler(context);
            handler.scheduleSnoozeReminder(taskId, "Reminder", 15);
            
            android.widget.Toast.makeText(context, "Reminder snoozed for 15 minutes", android.widget.Toast.LENGTH_SHORT).show();
        }
          private void handleDismissReminder(Context context, String taskId) {
            // Dismiss notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Integer.parseInt(taskId));
            
            // Optionally mark reminder as acknowledged
            TaskManager taskManager = new NotificationHandler(context).createTaskManager(context, taskId, "Reminder acknowledged");
            taskManager.updateTaskCompletion(taskId, true);
        }
        
        private void handleCantComplete(Context context, String taskId) {
            // Show dialog for rescheduling or canceling task
            Intent dialogIntent = new Intent(context, CantCompleteTaskActivity.class);
            dialogIntent.putExtra("task_id", taskId);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dialogIntent);
        }
        
        private void handleLegacyActions(Context context, String taskId, String action) {
            TaskManager taskManager = new NotificationHandler(context).createTaskManager(context, taskId, null);
            
            if ("extend".equals(action)) {
                handleExtendTask(context, taskId);
            } else if ("skip_for_today".equals(action)) {
                handleSkipForToday(context, taskId);
            } else if ("reschedule_tomorrow".equals(action)) {
                handleRescheduleTomorrow(context, taskId);
            } else {
                // Update task completion directly
                taskManager.updateTaskCompletion(taskId, true);
            }
        }
        
        private void handleSkipForToday(Context context, String taskId) {
            // Dismiss notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Integer.parseInt(taskId));
            
            // Update task status
            TaskManager taskManager = new NotificationHandler(context).createTaskManager(context, taskId, "Task skipped for today");
            taskManager.updateTaskStatus(taskId, "skipped_today");
            
            android.widget.Toast.makeText(context, "Task skipped for today", android.widget.Toast.LENGTH_SHORT).show();
        }
        
        private void handleRescheduleTomorrow(Context context, String taskId) {
            // Dismiss current notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Integer.parseInt(taskId));
            
            try {
                // Schedule for tomorrow at the same time
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                
                // Create rescheduled task
                Task rescheduledTask = new Task();
                rescheduledTask.setTaskId(taskId);
                rescheduledTask.setTitle("Rescheduled Task");
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                
                rescheduledTask.setDueDate(dateFormat.format(tomorrow.getTime()));
                rescheduledTask.setStartTime(datetimeFormat.format(tomorrow.getTime()));
                  // Schedule new reminder
                NotificationHandler handler = new NotificationHandler(context);
                handler.scheduleReminderNotification(
                    rescheduledTask.getTaskId(),
                    rescheduledTask.getTitle(),
                    rescheduledTask.getDescription(),
                    rescheduledTask.getStartTime(),
                    rescheduledTask.getDueDate()
                );
                
                android.widget.Toast.makeText(context, "Task rescheduled for tomorrow", android.widget.Toast.LENGTH_SHORT).show();
                
            } catch (Exception e) {
                Log.e("TaskActionReceiver", "Error rescheduling for tomorrow: " + e.getMessage());
                android.widget.Toast.makeText(context, "Error rescheduling task", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }    // Activity to show extend dialog
    public static class ExtendTaskActivity extends android.app.Activity {
        private android.app.AlertDialog extendDialog;
          @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            String taskId = getIntent().getStringExtra("task_id");
            if (taskId == null) {
                finish();
                return;
            }
            
            // Check if activity is finishing or destroyed before showing dialog
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            // Create custom dialog with improved UI
            android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this);
            android.view.LayoutInflater inflater = getLayoutInflater();
            android.view.View dialogView = inflater.inflate(R.layout.dialog_extend_time, null);
            
            // Set up click listeners for time option buttons
            setupTimeOptionButtons(dialogView, taskId);
            
            dialogBuilder.setView(dialogView);
            extendDialog = dialogBuilder.create();
            
            // Make dialog background transparent to show custom rounded corners
            if (extendDialog.getWindow() != null) {
                extendDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            
            extendDialog.setCancelable(true);
            extendDialog.setOnCancelListener(d -> finish());
            
            // Add safety check before showing dialog to prevent window leak
            if (!isFinishing() && !isDestroyed()) {
                extendDialog.show();
            }
        }        @Override
        protected void onDestroy() {
            // Properly dismiss dialog to prevent window leak
            dismissDialogSafely();
            super.onDestroy();
        }
        
        @Override
        protected void onPause() {
            // Dismiss dialog when activity is paused to prevent window leak
            dismissDialogSafely();
            super.onPause();
        }
        
        @Override
        protected void onStop() {
            // Ensure dialog is dismissed when activity stops
            dismissDialogSafely();
            super.onStop();
        }
          private void setupTimeOptionButtons(android.view.View dialogView, String taskId) {
            // Time option buttons with updated values: 5, 15, 20, 30 minutes, 1 hour
            dialogView.findViewById(R.id.btn_extend_5).setOnClickListener(v -> {
                dismissDialogSafely();
                extendTaskByMinutes(taskId, 5);
            });
            
            dialogView.findViewById(R.id.btn_extend_15).setOnClickListener(v -> {
                dismissDialogSafely();
                extendTaskByMinutes(taskId, 15);
            });
            
            dialogView.findViewById(R.id.btn_extend_20).setOnClickListener(v -> {
                dismissDialogSafely();
                extendTaskByMinutes(taskId, 20);
            });
            
            dialogView.findViewById(R.id.btn_extend_30).setOnClickListener(v -> {
                dismissDialogSafely();
                extendTaskByMinutes(taskId, 30);
            });
            
            dialogView.findViewById(R.id.btn_extend_60).setOnClickListener(v -> {
                dismissDialogSafely();
                extendTaskByMinutes(taskId, 60);
            });
            
            // Custom time button
            dialogView.findViewById(R.id.btn_custom_time).setOnClickListener(v -> {
                dismissDialogSafely();
                showCustomTimeDialog(taskId);
            });
            
            // Cancel button
            dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
                dismissDialogSafely();
                finish();
            });
        }
          private void dismissDialogSafely() {
            try {
                if (extendDialog != null && extendDialog.isShowing()) {
                    extendDialog.dismiss();
                }
            } catch (Exception e) {
                Log.e("ExtendTaskActivity", "Error dismissing dialog", e);
            } finally {
                extendDialog = null;
            }
        }
          private void showCustomTimeDialog(String taskId) {
            // Create a more modern custom time input dialog
            android.widget.EditText input = new android.widget.EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setHint("Enter minutes (1-240)");
            input.setPadding(40, 40, 40, 40);
            input.setTextSize(16);
            input.setBackground(getResources().getDrawable(R.drawable.rounded_edit_text_background));
            
            // Create container for input with margins
            android.widget.LinearLayout container = new android.widget.LinearLayout(this);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setPadding(60, 40, 60, 20);
            
            // Add description text
            android.widget.TextView description = new android.widget.TextView(this);
            description.setText("Enter the exact number of minutes you need to extend this task.");
            description.setTextSize(14);
            description.setTextColor(getResources().getColor(android.R.color.darker_gray));
            description.setPadding(0, 0, 0, 20);
            
            container.addView(description);
            container.addView(input);
            
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("⏱️ Custom Extension Time")
                    .setView(container)
                    .setPositiveButton("Extend Task", (dialog, which) -> {
                        try {
                            String inputText = input.getText().toString().trim();
                            if (inputText.isEmpty()) {
                                android.widget.Toast.makeText(this, "Please enter a number", android.widget.Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                            
                            int minutes = Integer.parseInt(inputText);
                            if (minutes > 0 && minutes <= 240) {
                                extendTaskByMinutes(taskId, minutes);
                            } else {
                                android.widget.Toast.makeText(this, "Please enter a value between 1-240 minutes", android.widget.Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } catch (NumberFormatException e) {
                            android.widget.Toast.makeText(this, "Please enter a valid number", android.widget.Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> finish())
                    .setCancelable(true);
            
            android.app.AlertDialog dialog = builder.create();
            
            // Style the dialog
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            
            dialog.setOnCancelListener(d -> finish());
            dialog.show();
            
            // Focus on input and show keyboard
            input.requestFocus();
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }        private void extendTaskByMinutes(String taskId, int extendMinutes) {
            // Check network connectivity first
            if (!IpV4Connection.isNetworkAvailable(this)) {
                android.widget.Toast.makeText(this, "❌ Network unavailable. Please check your connection.", android.widget.Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // Show immediate feedback to user
            String timeText = extendMinutes == 60 ? "1 hour" : extendMinutes + " minutes";
            android.widget.Toast.makeText(this, "⏱️ Extending task by " + timeText + "...", android.widget.Toast.LENGTH_SHORT).show();
            
            // Try to find the task directly by ID first (more reliable)
            TaskManager taskManager = new TaskManager(this, new TaskManager.TaskListener() {
                @Override
                public void onTasksLoaded(java.util.List<Task> tasks) {
                    // This will be called by findTaskById with the specific task
                }
                
                @Override
                public void onTaskAdded(Task task) {}
                
                @Override
                public void onTaskUpdated(Task task) {
                    String successTimeText = extendMinutes == 60 ? "1 hour" : extendMinutes + " minutes";
                    android.widget.Toast.makeText(ExtendTaskActivity.this, "✅ Task extended by " + successTimeText + " successfully!", android.widget.Toast.LENGTH_LONG).show();
                    
                    // Reschedule notifications for the extended task
                    NotificationHandler handler = new NotificationHandler(ExtendTaskActivity.this);
                    handler.cancelTaskNotifications(taskId);
                    handler.scheduleTaskNotification(task);
                    
                    finish();
                }
                
                @Override
                public void onTaskDeleted(String taskId) {}
                
                @Override
                public void onError(String message) {
                    android.widget.Toast.makeText(ExtendTaskActivity.this, "❌ Error: " + message, android.widget.Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onHabitStreakUpdated(String taskId, int newStreak) {}
            });

            // Use a more robust approach to find the task
            findTaskByIdAndExtend(taskManager, taskId, extendMinutes);
        }
        
        private void findTaskByIdAndExtend(TaskManager taskManager, String taskId, int extendMinutes) {
            // First try to find the task in local storage
            Task localTask = taskManager.getTaskById(taskId);
            if (localTask != null) {
                android.util.Log.d("ExtendTaskActivity", "Found task locally: " + localTask.getTitle());
                extendTaskTime(localTask, extendMinutes);
                return;
            }
            
            // If not found locally, try loading tasks from multiple dates
            // Tasks might not be on today's date, so we need broader search
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            
            // Search from 7 days ago to 7 days ahead to find the task
            for (int dayOffset = -7; dayOffset <= 7; dayOffset++) {
                calendar.setTime(new java.util.Date());
                calendar.add(java.util.Calendar.DAY_OF_MONTH, dayOffset);
                String searchDate = dateFormat.format(calendar.getTime());
                
                android.util.Log.d("ExtendTaskActivity", "Searching for task on date: " + searchDate);
                
                TaskManager searchManager = new TaskManager(this, new TaskManager.TaskListener() {
                    @Override
                    public void onTasksLoaded(java.util.List<Task> tasks) {
                        for (Task task : tasks) {
                            if (task.getTaskId().equals(taskId)) {
                                android.util.Log.d("ExtendTaskActivity", "Found task: " + task.getTitle() + " on date: " + searchDate);
                                extendTaskTime(task, extendMinutes);
                                return;
                            }
                        }
                        
                        // If this is the last search attempt and no task found
                        if (dayOffset == 7) {
                            android.widget.Toast.makeText(ExtendTaskActivity.this, "❌ Task not found. It may have been deleted or moved.", android.widget.Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                    
                    @Override
                    public void onTaskAdded(Task task) {}
                    
                    @Override
                    public void onTaskUpdated(Task task) {
                        String successTimeText = extendMinutes == 60 ? "1 hour" : extendMinutes + " minutes";
                        android.widget.Toast.makeText(ExtendTaskActivity.this, "✅ Task extended by " + successTimeText + " successfully!", android.widget.Toast.LENGTH_LONG).show();
                        
                        // Reschedule notifications for the extended task
                        NotificationHandler handler = new NotificationHandler(ExtendTaskActivity.this);
                        handler.cancelTaskNotifications(taskId);
                        handler.scheduleTaskNotification(task);
                        
                        finish();
                    }
                    
                    @Override
                    public void onTaskDeleted(String taskId) {}
                    
                    @Override
                    public void onError(String message) {
                        android.util.Log.e("ExtendTaskActivity", "Error searching for task: " + message);
                        // Continue searching other dates
                    }

                    @Override
                    public void onHabitStreakUpdated(String taskId, int newStreak) {}
                });
                
                searchManager.loadTasks(searchDate);
                
                // If we find the task, we'll exit early in onTasksLoaded
                // For now, we break after starting the search to avoid overwhelming the system
                break;
            }
        }
          private void extendTaskTime(Task task, int extendMinutes) {
            try {
                // Parse the end time properly - could be full datetime or time-only format
                String endTimeStr = task.getEndTime();
                Date endTime;
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                
                if (endTimeStr != null && endTimeStr.contains(" ")) {
                    // Full datetime format: "YYYY-MM-DD HH:MM:SS" - extract time part
                    String timePart = endTimeStr.split(" ")[1];
                    if (timePart.contains(":") && timePart.split(":").length > 2) {
                        // Has seconds, use HH:mm:ss format
                        SimpleDateFormat fullTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                        endTime = fullTimeFormat.parse(timePart);
                    } else {
                        // No seconds, use HH:mm format
                        endTime = timeFormat.parse(timePart);
                    }
                } else {
                    // Time-only format: "HH:MM"
                    endTime = timeFormat.parse(endTimeStr);
                }
                
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endTime);
                calendar.add(Calendar.MINUTE, extendMinutes);
                
                String newEndTime = timeFormat.format(calendar.getTime());
                
                TaskManager taskManager = new TaskManager(this, new TaskManager.TaskListener() {
                    @Override
                    public void onTasksLoaded(java.util.List<Task> tasks) {}
                    
                    @Override
                    public void onTaskAdded(Task task) {}
                    
                    @Override
                    public void onTaskUpdated(Task task) {
                        // Dismiss notification
                        NotificationManager notificationManager = 
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.cancel(Integer.parseInt(task.getTaskId()));
                        
                        // Show updated notification for workflow end
                        if (task.isWorkflow()) {
                            NotificationHandler handler = new NotificationHandler(ExtendTaskActivity.this);
                            try {
                                handler.scheduleWorkflowEndNotification(task);
                            } catch (ParseException e) {
                                Log.e("ExtendTaskActivity", "Error rescheduling notification", e);
                            }
                        }
                        finish();
                    }
                    
                    @Override
                    public void onTaskDeleted(String taskId) {}
                    
                    @Override
                    public void onError(String message) {
                        android.widget.Toast.makeText(ExtendTaskActivity.this, "Error extending task: " + message, android.widget.Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onHabitStreakUpdated(String taskId, int newStreak) {}
                });
                
                taskManager.updateTaskTime(task.getTaskId(), task.getStartTime(), newEndTime);
                taskManager.updateTaskCompletion(task.getTaskId(), false); // Task extended, so not completed
                
            } catch (Exception e) {
                Log.e("ExtendTaskActivity", "Error extending task time", e);
                android.widget.Toast.makeText(this, "Error extending task time", android.widget.Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    // Activity to handle when user can't complete a task
    public static class CantCompleteTaskActivity extends android.app.Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            String taskId = getIntent().getStringExtra("task_id");
            
            String[] options = {"Reschedule for later today", "Reschedule for tomorrow", "Cancel task", "Snooze 30 minutes"};
            
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Can't Complete Task")
                    .setMessage("What would you like to do with this task?")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                rescheduleTaskForToday(taskId);
                                break;
                            case 1:
                                rescheduleTaskForTomorrow(taskId);
                                break;
                            case 2:
                                cancelTask(taskId);
                                break;
                            case 3:
                                snoozeTask(taskId, 30);
                                break;
                        }
                        finish();
                    })
                    .setOnCancelListener(dialog -> finish())
                    .show();
        }
        
        private void rescheduleTaskForToday(String taskId) {
            // Add 2 hours to current time
            Calendar newTime = Calendar.getInstance();
            newTime.add(Calendar.HOUR_OF_DAY, 2);
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String newStartTime = timeFormat.format(newTime.getTime());
            
            newTime.add(Calendar.HOUR_OF_DAY, 1); // 1 hour duration
            String newEndTime = timeFormat.format(newTime.getTime());
            
            updateTaskTime(taskId, newStartTime, newEndTime);
            android.widget.Toast.makeText(this, "Task rescheduled for later today", android.widget.Toast.LENGTH_SHORT).show();
        }
        
        private void rescheduleTaskForTomorrow(String taskId) {
            // Schedule for same time tomorrow
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String newDate = dateFormat.format(tomorrow.getTime());
            
            TaskManager taskManager = new TaskManager(this, new TaskManager.TaskListener() {
                @Override
                public void onTasksLoaded(java.util.List<Task> tasks) {}
                @Override
                public void onTaskAdded(Task task) {}
                @Override
                public void onTaskUpdated(Task task) {
                    android.widget.Toast.makeText(CantCompleteTaskActivity.this, "Task rescheduled for tomorrow", android.widget.Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onTaskDeleted(String taskId) {}
                @Override
                public void onError(String message) {}
                @Override
                public void onHabitStreakUpdated(String taskId, int newStreak) {}
            });
            
            // This would need implementation in TaskManager
            // taskManager.updateTaskDate(taskId, newDate);
        }
        
        private void cancelTask(String taskId) {
            TaskManager taskManager = new TaskManager(this, new TaskManager.TaskListener() {
                @Override
                public void onTasksLoaded(java.util.List<Task> tasks) {}
                @Override
                public void onTaskAdded(Task task) {}
                @Override
                public void onTaskUpdated(Task task) {}
                @Override
                public void onTaskDeleted(String deletedTaskId) {
                    android.widget.Toast.makeText(CantCompleteTaskActivity.this, "Task cancelled", android.widget.Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(String message) {}
                @Override
                public void onHabitStreakUpdated(String taskId, int newStreak) {}
            });
            
            taskManager.deleteTask(taskId);
            
            // Cancel notifications
            NotificationHandler handler = new NotificationHandler(this);
            handler.cancelTaskNotifications(taskId);
        }
        
        private void snoozeTask(String taskId, int minutes) {
            NotificationHandler handler = new NotificationHandler(this);
            handler.scheduleSnoozeReminder(taskId, "Task Reminder", minutes);
            android.widget.Toast.makeText(this, "Task snoozed for " + minutes + " minutes", android.widget.Toast.LENGTH_SHORT).show();
        }
        
        private void updateTaskTime(String taskId, String newStartTime, String newEndTime) {
            TaskManager taskManager = new TaskManager(this, new TaskManager.TaskListener() {
                @Override
                public void onTasksLoaded(java.util.List<Task> tasks) {}
                @Override
                public void onTaskAdded(Task task) {}
                @Override
                public void onTaskUpdated(Task task) {
                    // Reschedule notifications
                    NotificationHandler handler = new NotificationHandler(CantCompleteTaskActivity.this);
                    handler.cancelTaskNotifications(taskId);
                    handler.scheduleTaskNotification(task);
                }
                @Override
                public void onTaskDeleted(String taskId) {}
                @Override
                public void onError(String message) {}
                @Override
                public void onHabitStreakUpdated(String taskId, int newStreak) {}
            });
            
            taskManager.updateTaskTime(taskId, newStartTime, newEndTime);
        }
    }
    
    /**
     * Schedule workflow notifications with individual parameters (called by TaskManager)
     */    public void scheduleWorkflowNotifications(String taskId, String title, String description, 
                                            String startTime, String endTime, String dueDate) {
        try {
            // Create a temporary Task object for scheduling
            // Task constructor: (taskId, userId, type, title, description, startTime, endTime, dueDate, status, repeat, priority)
            Task tempTask = new Task(taskId, "", "workflow", title, description, startTime, endTime, dueDate, 
                                   "pending", "none", "medium");
            scheduleTaskNotification(tempTask);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling workflow notifications: " + e.getMessage(), e);
        }
    }
    
    /**
     * Schedule reminder notification with individual parameters (called by TaskManager)
     */    public void scheduleReminderNotification(String taskId, String title, String description, 
                                           String startTime, String dueDate) {
        try {
            // Create a temporary Task object for scheduling
            // Task constructor: (taskId, userId, type, title, description, startTime, endTime, dueDate, status, repeat, priority)
            Task tempTask = new Task(taskId, "", "remainder", title, description, startTime, startTime, dueDate, 
                                   "pending", "none", "medium");
            scheduleTaskNotification(tempTask);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get current time as formatted string for notifications
     */    private static String getCurrentTimeString() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return timeFormat.format(new Date());
    }

    /**
     * Fix timing precision by using setExactAndAllowWhileIdle for better accuracy
     */
    private void scheduleExactNotification(Intent intent, PendingIntent pendingIntent, long triggerTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        try {
            // Use setExactAndAllowWhileIdle for better timing precision
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            
            Log.d(TAG, "Scheduled exact notification for: " + new Date(triggerTime));
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling exact notification: " + e.getMessage());
            // Fallback to regular set if exact scheduling fails
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
    
    /**
     * Create a TaskManager instance for notification actions
     * @param context The context
     * @param taskId The task ID
     * @param successMessage Message to log on success
     * @return TaskManager instance
     */
    private TaskManager createTaskManager(Context context, String taskId, String successMessage) {
        return new TaskManager(context, new TaskManager.TaskListener() {
            @Override
            public void onTasksLoaded(java.util.List<Task> tasks) {
                // Not needed for notification actions
            }
            
            @Override
            public void onTaskAdded(Task task) {
                // Not needed for notification actions
            }
            
            @Override
            public void onTaskUpdated(Task task) {
                if (successMessage != null && !successMessage.isEmpty()) {
                    Log.d(TAG, successMessage);
                }
            }
            
            @Override
            public void onTaskDeleted(String taskId) {
                // Not needed for notification actions
            }
            
            @Override
            public void onHabitStreakUpdated(String taskId, int newStreak) {
                // Not needed for notification actions
            }
            
            @Override
            public void onError(String message) {
                Log.e(TAG, "TaskManager error for task " + taskId + ": " + message);
            }
        });
    }
    
    /**
     * Show an ongoing notification for a task in progress
     * @param context The context
     * @param taskId The task ID
     */    private void showTaskInProgressNotification(Context context, String taskId) {
        try {
            new NotificationHandler(context).createNotificationChannels();
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Create actions for the ongoing notification
            Intent completeIntent = new Intent(context, TaskActionReceiver.class);
            completeIntent.setAction(ACTION_COMPLETE_TASK);
            completeIntent.putExtra("task_id", taskId);
            PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context, 
                (taskId + "_complete").hashCode(), 
                completeIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            Intent extendIntent = new Intent(context, TaskActionReceiver.class);
            extendIntent.setAction(ACTION_EXTEND_TASK);
            extendIntent.putExtra("task_id", taskId);
            PendingIntent extendPendingIntent = PendingIntent.getBroadcast(
                context, 
                (taskId + "_extend").hashCode(), 
                extendIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            Intent cantCompleteIntent = new Intent(context, TaskActionReceiver.class);
            cantCompleteIntent.setAction(ACTION_CANT_COMPLETE);
            cantCompleteIntent.putExtra("task_id", taskId);
            PendingIntent cantCompletePendingIntent = PendingIntent.getBroadcast(
                context, 
                (taskId + "_cant_complete").hashCode(), 
                cantCompleteIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
              // Build the ongoing notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WORKFLOW_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Task in Progress")
                .setContentText("Task is currently running")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true) // Makes it an ongoing notification
                .setAutoCancel(false)
                .addAction(R.drawable.ic_check, "Complete", completePendingIntent)
                .addAction(R.drawable.ic_extend, "Extend", extendPendingIntent)
                .addAction(R.drawable.ic_close, "Can't Complete", cantCompletePendingIntent);
            
            notificationManager.notify(Integer.parseInt(taskId), builder.build());
            Log.d(TAG, "Showed task in progress notification for: " + taskId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing task in progress notification: " + e.getMessage());
        }
    }
}