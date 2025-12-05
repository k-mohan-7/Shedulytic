package com.simats.schedulytic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * This BroadcastReceiver listens for the BOOT_COMPLETED action.
 * It's used to reschedule alarms, notifications, or other tasks
 * that need to persist across device restarts.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the received intent is the BOOT_COMPLETED action
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed received. Rescheduling tasks...");

            // Reschedule all active tasks' notifications
            rescheduleAllTaskNotifications(context);
        }
    }
    
    /**
     * Reschedule notifications for all active tasks
     */
    private void rescheduleAllTaskNotifications(Context context) {
        // Use AsyncTask or Thread to handle database operations off main thread
        Thread rescheduleThread = new Thread(() -> {
            try {
                TaskManager taskManager = new TaskManager(context, new TaskManager.TaskListener() {
                    @Override
                    public void onTasksLoaded(java.util.List<Task> tasks) {
                        NotificationHandler notificationHandler = new NotificationHandler(context);
                        ReminderNotificationManager reminderManager = new ReminderNotificationManager(context);
                        int rescheduledCount = 0;
                        
                        for (Task task : tasks) {
                            // Only reschedule incomplete tasks for today and future dates
                            if (!task.isCompleted() && isTaskRelevant(task)) {
                                // Use appropriate notification manager based on task type
                                if (task.isRemainder()) {
                                    reminderManager.scheduleReminderNotifications(task);
                                } else {
                                    notificationHandler.scheduleTaskNotification(task);
                                }
                                rescheduledCount++;
                            }
                        }
                        
                        Log.d(TAG, "Rescheduled " + rescheduledCount + " task notifications");
                    }
                    
                    @Override
                    public void onTaskAdded(Task task) {}
                    
                    @Override
                    public void onTaskUpdated(Task task) {}
                    
                    @Override
                    public void onTaskDeleted(String taskId) {}
                    
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error rescheduling notifications: " + message);
                    }
                    
                    @Override
                    public void onHabitStreakUpdated(String taskId, int newStreak) {}
                });
                
                // Load tasks for today and upcoming days
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = dateFormat.format(new Date());
                taskManager.loadTasks(today);
                
                // Also load tomorrow's tasks
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);
                String tomorrowDate = dateFormat.format(tomorrow.getTime());
                taskManager.loadTasks(tomorrowDate);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in rescheduleAllTaskNotifications", e);
            }
        });
        
        rescheduleThread.start();
    }
    
    /**
     * Check if a task is relevant for rescheduling (today or future)
     */
    private boolean isTaskRelevant(Task task) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date taskDate = dateFormat.parse(task.getDueDate());
            Date today = new Date();
            
            // Reset time to start of day for comparison
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTime(taskDate);
            taskCal.set(Calendar.HOUR_OF_DAY, 0);
            taskCal.set(Calendar.MINUTE, 0);
            taskCal.set(Calendar.SECOND, 0);
            taskCal.set(Calendar.MILLISECOND, 0);
            
            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(today);
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);
            
            // Task is relevant if it's today or in the future
            return taskCal.getTimeInMillis() >= todayCal.getTimeInMillis();
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking task relevance", e);
            return false;
        }
    }
}
