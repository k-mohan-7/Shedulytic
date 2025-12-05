package com.simats.schedulytic;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity to handle task completion from notification
 * This is necessary for Android 12+ as BroadcastReceivers cannot start activities reliably
 */
public class TaskCompletionActivity extends AppCompatActivity {
    private static final String TAG = "TaskCompletionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get task details from intent
        String taskId = getIntent().getStringExtra("task_id");
        String taskTitle = getIntent().getStringExtra("task_title");
        int notificationId = getIntent().getIntExtra("notification_id", 0);
        
        Log.d(TAG, "Completing task: " + taskId + " - " + taskTitle);
        
        if (taskId == null) {
            Log.e(TAG, "No task ID provided");
            finish();
            return;
        }
        
        // Dismiss notification
        NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notifManager != null) {
            if (notificationId != 0) {
                notifManager.cancel(notificationId);
            }
            notifManager.cancel((taskId + "_exact").hashCode());
            notifManager.cancel((taskId + "_pre").hashCode());
        }
        
        // Complete the task
        ReminderNotificationManager manager = new ReminderNotificationManager(this);
        manager.handleTaskCompletion(taskId, "completed", new ReminderNotificationManager.TaskCompletionCallback() {
            @Override
            public void onSuccess(double xpChange) {
                Log.d(TAG, "Task completed successfully, XP: " + xpChange);
                
                // Show reward popup
                Intent rewardIntent = new Intent(TaskCompletionActivity.this, RewardPopupActivity.class);
                rewardIntent.putExtra("xp_change", xpChange);
                rewardIntent.putExtra("task_title", taskTitle != null ? taskTitle : "Task");
                rewardIntent.putExtra("is_reward", true);
                startActivity(rewardIntent);
                
                finish();
            }
            
            @Override
            public void onError(String message) {
                Log.e(TAG, "Error completing task: " + message);
                
                // Still show success locally even if server fails
                Toast.makeText(TaskCompletionActivity.this, "Task marked as completed!", Toast.LENGTH_SHORT).show();
                
                // Show reward popup anyway with default XP
                Intent rewardIntent = new Intent(TaskCompletionActivity.this, RewardPopupActivity.class);
                rewardIntent.putExtra("xp_change", 2.0); // Default reward
                rewardIntent.putExtra("task_title", taskTitle != null ? taskTitle : "Task");
                rewardIntent.putExtra("is_reward", true);
                startActivity(rewardIntent);
                
                finish();
            }
        });
    }
}
