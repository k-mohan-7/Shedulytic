package com.example.shedulytic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HabitReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra("taskId");
        String taskTitle = intent.getStringExtra("taskTitle");
        int currentStreak = intent.getIntExtra("currentStreak", 0);

        HabitNotificationManager notificationManager = new HabitNotificationManager(context);
        notificationManager.showHabitReminder(taskId, taskTitle, currentStreak);
    }
}