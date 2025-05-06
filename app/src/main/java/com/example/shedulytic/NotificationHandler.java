package com.example.shedulytic;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationHandler {
    private static final String CHANNEL_ID = "schedlytic_channel";
    private static final String CHANNEL_NAME = "Schedlytic Notifications";
    private static final int NOTIFICATION_ID = 1000;
    
    private Context context;
    private NotificationManager notificationManager;
    
    public NotificationHandler(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Channel for Schedlytic app notifications");
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public void scheduleTaskNotification(Task task) {
        try {
            // Schedule based on task type
            if (task.isWorkflow()) {
                // For workflow tasks, schedule notifications at start and end times
                scheduleWorkflowStartNotification(task);
                scheduleWorkflowEndNotification(task);
            } else if (task.isRemainder()) {
                // For remainder tasks, schedule a single notification at the start time
                scheduleRemainderNotification(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void scheduleWorkflowStartNotification(Task task) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        Date taskDate = dateFormat.parse(task.getDueDate());
        Date startTime = timeFormat.parse(task.getStartTime());
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(taskDate);
        
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(startTime);
        
        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0);
        
        // Create intent for notification
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("notification_type", "workflow_start");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(task.getTaskId()) * 10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
    
    private void scheduleWorkflowEndNotification(Task task) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        Date taskDate = dateFormat.parse(task.getDueDate());
        Date endTime = timeFormat.parse(task.getEndTime());
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(taskDate);
        
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(endTime);
        
        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0);
        
        // Create intent for notification
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("notification_type", "workflow_end");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(task.getTaskId()) * 10 + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
    
    private void scheduleRemainderNotification(Task task) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        Date taskDate = dateFormat.parse(task.getDueDate());
        Date startTime = timeFormat.parse(task.getStartTime());
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(taskDate);
        
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(startTime);
        
        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0);
        
        // Create intent for notification
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("notification_type", "remainder");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                Integer.parseInt(task.getTaskId()) * 10 + 2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
    
    // BroadcastReceiver to handle notifications
    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String taskId = intent.getStringExtra("task_id");
            String taskTitle = intent.getStringExtra("task_title");
            String notificationType = intent.getStringExtra("notification_type");
            
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Create notification based on type
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
            
            if ("workflow_start".equals(notificationType)) {
                // Workflow start notification
                builder.setSmallIcon(R.drawable.workflow_logo)
                       .setContentTitle("Workflow Starting")
                       .setContentText("Time to start: " + taskTitle)
                       .setPriority(NotificationCompat.PRIORITY_HIGH);
                
                // Add action buttons for workflow start
                Intent startIntent = new Intent(context, TaskActionReceiver.class);
                startIntent.putExtra("task_id", taskId);
                startIntent.putExtra("action", "start");
                PendingIntent startPendingIntent = PendingIntent.getBroadcast(
                        context, Integer.parseInt(taskId) * 100, startIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                
                Intent cantIntent = new Intent(context, TaskActionReceiver.class);
                cantIntent.putExtra("task_id", taskId);
                cantIntent.putExtra("action", "cant_complete");
                PendingIntent cantPendingIntent = PendingIntent.getBroadcast(
                        context, Integer.parseInt(taskId) * 100 + 1, cantIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                
                builder.addAction(android.R.drawable.ic_media_play, "Start", startPendingIntent);
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Can't Complete", cantPendingIntent);
                
            } else if ("workflow_end".equals(notificationType)) {
                // Workflow end notification
                builder.setSmallIcon(R.drawable.workflow_logo)
                       .setContentTitle("Workflow Ending")
                       .setContentText("Time to finish: " + taskTitle)
                       .setPriority(NotificationCompat.PRIORITY_HIGH);
                
                // Add action buttons for workflow end
                Intent completeIntent = new Intent(context, TaskActionReceiver.class);
                completeIntent.putExtra("task_id", taskId);
                completeIntent.putExtra("action", "completed");
                PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                        context, Integer.parseInt(taskId) * 100 + 2, completeIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                
                Intent extendIntent = new Intent(context, TaskActionReceiver.class);
                extendIntent.putExtra("task_id", taskId);
                extendIntent.putExtra("action", "extend");
                PendingIntent extendPendingIntent = PendingIntent.getBroadcast(
                        context, Integer.parseInt(taskId) * 100 + 3, extendIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                
                builder.addAction(android.R.drawable.ic_menu_save, "Completed", completePendingIntent);
                builder.addAction(android.R.drawable.ic_menu_recent_history, "Extend", extendPendingIntent);
                
            } else if ("remainder".equals(notificationType)) {
                // Remainder notification
                builder.setSmallIcon(R.drawable.remainder_logo)
                       .setContentTitle("Reminder")
                       .setContentText("Don't forget: " + taskTitle)
                       .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            }
            
            notificationManager.notify(Integer.parseInt(taskId), builder.build());
        }
    }
    
    // BroadcastReceiver to handle task action buttons
    public static class TaskActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String taskId = intent.getStringExtra("task_id");
            String action = intent.getStringExtra("action");
            
            // Update task status based on action
            TaskManager taskManager = new TaskManager(context, new TaskManager.TaskListener() {
                @Override
                public void onTasksLoaded(java.util.List<Task> tasks) {}
                
                @Override
                public void onTaskAdded(Task task) {}
                
                @Override
                public void onTaskUpdated(Task task) {
                    // Dismiss notification after action
                    NotificationManager notificationManager = 
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(Integer.parseInt(taskId));
                }
                
                @Override
                public void onTaskDeleted(String taskId) {}
                
                @Override
                public void onError(String message) {}
            });
            
            if ("extend".equals(action)) {
                // Show extend dialog
                Intent dialogIntent = new Intent(context, ExtendTaskActivity.class);
                dialogIntent.putExtra("task_id", taskId);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dialogIntent);
            } else {
                // Update task status directly
                taskManager.updateTaskStatus(taskId, action);
            }
        }
    }
    
    // Activity to show extend dialog
    public static class ExtendTaskActivity extends android.app.Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            String taskId = getIntent().getStringExtra("task_id");
            
            String[] options = {"10 minutes", "20 minutes", "30 minutes", "Custom"};
            
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Extend Task Time")
                    .setItems(options, (dialog, which) -> {
                        int extendMinutes;
                        switch (which) {
                            case 0: extendMinutes = 10; break;
                            case 1: extendMinutes = 20; break;
                            case 2: extendMinutes = 30; break;
                            case 3: extendMinutes = 15; break; // Custom (simplified)
                            default: extendMinutes = 10; break;
                        }
                        
                        // Get task details and extend
                        TaskManager taskManager = new TaskManager(this, new TaskManager.TaskListener() {
                            @Override
                            public void onTasksLoaded(java.util.List<Task> tasks) {
                                for (Task task : tasks) {
                                    if (task.getTaskId().equals(taskId)) {
                                        extendTaskTime(task, extendMinutes);
                                        break;
                                    }
                                }
                            }
                            
                            @Override
                            public void onTaskAdded(Task task) {}
                            
                            @Override
                            public void onTaskUpdated(Task task) {
                                finish();
                            }
                            
                            @Override
                            public void onTaskDeleted(String taskId) {}
                            
                            @Override
                            public void onError(String message) {
                                finish();
                            }
                        });
                        
                        taskManager.loadTasks();
                    })
                    .setOnCancelListener(dialog -> finish())
                    .show();
        }
        
        private void extendTaskTime(Task task, int extendMinutes) {
            try {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date endTime = timeFormat.parse(task.getEndTime());
                
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
                        finish();
                    }
                    
                    @Override
                    public void onTaskDeleted(String taskId) {}
                    
                    @Override
                    public void onError(String message) {
                        finish();
                    }
                });
                
                taskManager.updateTaskTime(task.getTaskId(), task.getStartTime(), newEndTime);
                taskManager.updateTaskStatus(task.getTaskId(), "extended");
                
            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }
        }
    }
}