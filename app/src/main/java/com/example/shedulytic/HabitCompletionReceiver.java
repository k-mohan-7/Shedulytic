package com.example.shedulytic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class HabitCompletionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra("taskId");
        String action = intent.getStringExtra("action");

        if (taskId == null || action == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");

        if (userId.isEmpty()) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create HabitManager instance to handle the completion
        HabitManager habitManager = new HabitManager(context, new HabitManager.HabitListener() {
            @Override
            public void onHabitCompleted(String taskId, int newStreak) {
                Toast.makeText(context, 
                    String.format("Habit completed! Current streak: %d", newStreak), 
                    Toast.LENGTH_SHORT).show();

                // Show milestone notification if needed
                HabitNotificationManager notificationManager = new HabitNotificationManager(context);
                notificationManager.showStreakMilestoneNotification("Your habit", newStreak);
            }

            @Override
            public void onHabitUncompleted(String taskId, int newStreak) {
                Toast.makeText(context, "Habit uncompleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onHabitStreakUpdated(String taskId, int newStreak) {
                // Handle streak updates
            }

            @Override
            public void onError(String message) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle completion based on action
        habitManager.toggleHabitCompletion(taskId, action.equals("complete"));
    }
}