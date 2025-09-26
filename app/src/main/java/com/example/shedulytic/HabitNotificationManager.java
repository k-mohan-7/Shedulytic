package com.example.shedulytic;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class HabitNotificationManager {
    private static final String CHANNEL_ID = "habit_channel";
    private static final String CHANNEL_NAME = "Habit Reminders";
    private final Context context;
    private final NotificationManager notificationManager;

    public HabitNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for habit reminders");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleHabitReminder(Task habitTask) {
        if (!habitTask.isHabit()) return;

        Intent intent = new Intent(context, HabitReminderReceiver.class);
        intent.putExtra("taskId", habitTask.getTaskId());
        intent.putExtra("taskTitle", habitTask.getTitle());
        intent.putExtra("currentStreak", habitTask.getCurrentStreak());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            habitTask.getTaskId().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Parse time from task
        String[] timeParts = habitTask.getStartTime().split(":");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
        calendar.set(Calendar.SECOND, 0);

        // If time has passed today, schedule for tomorrow
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Schedule based on repeat frequency
        long intervalMillis;
        switch (habitTask.getRepeatFrequency()) {
            case "daily":
                intervalMillis = AlarmManager.INTERVAL_DAY;
                break;
            case "weekly":
                intervalMillis = AlarmManager.INTERVAL_DAY * 7;
                break;
            case "monthly":
                intervalMillis = AlarmManager.INTERVAL_DAY * 30;
                break;
            default:
                intervalMillis = AlarmManager.INTERVAL_DAY;
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            intervalMillis,
            pendingIntent
        );
    }

    public void cancelHabitReminder(Task habitTask) {
        Intent intent = new Intent(context, HabitReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            habitTask.getTaskId().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    public void showStreakMilestoneNotification(String taskTitle, int streak) {
        if (streak > 0 && streak % 5 == 0) { // Show for every 5-day milestone
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_streak_flame)
                .setContentTitle("Habit Streak Milestone!")
                .setContentText(String.format("%s: %d day streak! 🔥", taskTitle, streak))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

            notificationManager.notify(taskTitle.hashCode(), builder.build());
        }
    }

    public void showHabitReminder(String taskId, String taskTitle, int currentStreak) {
        String streakText = currentStreak > 0 ? 
            String.format(" (Current streak: %d days)", currentStreak) : "";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_habit)
            .setContentTitle("Habit Reminder")
            .setContentText(taskTitle + streakText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        // Add action buttons
        Intent completeIntent = new Intent(context, HabitCompletionReceiver.class);
        completeIntent.putExtra("taskId", taskId);
        completeIntent.putExtra("action", "complete");

        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.ic_check, "Complete", completePendingIntent);

        notificationManager.notify(taskId.hashCode(), builder.build());
    }
}