package com.simats.schedulytic;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class SnoozePickerActivity extends AppCompatActivity {

    private static final String TAG = "SnoozePickerActivity";

    private CardView snoozeCard;
    private TextView tvTaskTitle;
    private MaterialButton btnSnooze5min, btnSnooze15min, btnSnooze30min, btnSnooze1hr;
    private MaterialButton btnSnoozeCustom, btnConfirmCustom, btnCancel;
    private LinearLayout customTimeContainer;
    private EditText etHours, etMinutes;

    private int taskId;
    private String taskTitle;
    private int notificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze_picker);

        // Get data from intent
        taskId = getIntent().getIntExtra("task_id", -1);
        taskTitle = getIntent().getStringExtra("task_title");
        notificationId = getIntent().getIntExtra("notification_id", 0);

        if (taskTitle == null) taskTitle = "Your Task";

        initViews();
        setupClickListeners();
        animateCardEntry();

        // Dismiss the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            if (notificationId != 0) {
                notificationManager.cancel(notificationId);
            }
            // Also cancel by task-based ID as fallback
            notificationManager.cancel((String.valueOf(taskId) + "_exact").hashCode());
            notificationManager.cancel((String.valueOf(taskId) + "_pre").hashCode());
        }

        // Provide haptic feedback
        provideHapticFeedback();
    }

    private void initViews() {
        snoozeCard = findViewById(R.id.snooze_card);
        tvTaskTitle = findViewById(R.id.tv_task_title);
        btnSnooze5min = findViewById(R.id.btn_snooze_5min);
        btnSnooze15min = findViewById(R.id.btn_snooze_15min);
        btnSnooze30min = findViewById(R.id.btn_snooze_30min);
        btnSnooze1hr = findViewById(R.id.btn_snooze_1hr);
        btnSnoozeCustom = findViewById(R.id.btn_snooze_custom);
        btnConfirmCustom = findViewById(R.id.btn_confirm_custom);
        btnCancel = findViewById(R.id.btn_cancel);
        customTimeContainer = findViewById(R.id.custom_time_container);
        etHours = findViewById(R.id.et_hours);
        etMinutes = findViewById(R.id.et_minutes);

        tvTaskTitle.setText(taskTitle);
    }

    private void setupClickListeners() {
        // Preset snooze times
        btnSnooze5min.setOnClickListener(v -> snoozeFor(5));
        btnSnooze15min.setOnClickListener(v -> snoozeFor(15));
        btnSnooze30min.setOnClickListener(v -> snoozeFor(30));
        btnSnooze1hr.setOnClickListener(v -> snoozeFor(60));

        // Custom time toggle
        btnSnoozeCustom.setOnClickListener(v -> toggleCustomTimeInput());

        // Confirm custom time
        btnConfirmCustom.setOnClickListener(v -> confirmCustomTime());

        // Cancel
        btnCancel.setOnClickListener(v -> animateAndFinish());

        // Click outside to dismiss
        findViewById(R.id.snooze_root).setOnClickListener(v -> animateAndFinish());
        snoozeCard.setOnClickListener(v -> {
            // Prevent click through to root
        });
    }

    private void toggleCustomTimeInput() {
        if (customTimeContainer.getVisibility() == View.GONE) {
            customTimeContainer.setVisibility(View.VISIBLE);
            customTimeContainer.setAlpha(0f);
            customTimeContainer.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
            btnSnoozeCustom.setText("Hide Custom Time");
        } else {
            customTimeContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> customTimeContainer.setVisibility(View.GONE))
                    .start();
            btnSnoozeCustom.setText("⏰ Custom Time");
        }
    }

    private void confirmCustomTime() {
        String hoursStr = etHours.getText().toString().trim();
        String minutesStr = etMinutes.getText().toString().trim();

        int hours = 0;
        int minutes = 0;

        try {
            if (!hoursStr.isEmpty()) hours = Integer.parseInt(hoursStr);
            if (!minutesStr.isEmpty()) minutes = Integer.parseInt(minutesStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalMinutes = (hours * 60) + minutes;

        if (totalMinutes <= 0) {
            Toast.makeText(this, "Please enter a time greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalMinutes > 1440) { // Max 24 hours
            Toast.makeText(this, "Maximum snooze time is 24 hours", Toast.LENGTH_SHORT).show();
            return;
        }

        snoozeFor(totalMinutes);
    }

    private void snoozeFor(int minutes) {
        // Update task time in database
        updateTaskTimeInDatabase(minutes);
        
        // Schedule the new reminder
        scheduleSnoozeReminder(minutes);

        // Show confirmation
        String timeText = formatSnoozeTime(minutes);
        Toast.makeText(this, "⏰ Reminder snoozed for " + timeText, Toast.LENGTH_SHORT).show();

        // Provide haptic feedback
        provideHapticFeedback();

        // Close the activity
        animateAndFinish();
    }
    
    private void updateTaskTimeInDatabase(int minutes) {
        // Calculate new start time
        long newStartTimeMillis = System.currentTimeMillis() + (minutes * 60 * 1000L);
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US);
        java.util.Date newTime = new java.util.Date(newStartTimeMillis);
        
        // Format as "10:30 AM" - TaskManager expects this format
        String newStartTime = timeFormat.format(newTime);
        
        // Update in database via TaskManager
        TaskManager taskManager = new TaskManager(this, new TaskManager.TaskListener() {
            @Override
            public void onTasksLoaded(java.util.List<Task> tasks) {}
            
            @Override
            public void onTaskAdded(Task task) {}
            
            @Override
            public void onTaskUpdated(Task task) {
                android.util.Log.d("SnoozePickerActivity", "Task time updated to: " + newStartTime);
            }
            
            @Override
            public void onTaskDeleted(String taskId) {}
            
            @Override
            public void onHabitStreakUpdated(String taskId, int newStreak) {}
            
            @Override
            public void onError(String message) {
                android.util.Log.e("SnoozePickerActivity", "Error updating task time: " + message);
            }
        });
        
        // Update task start time - TaskManager will combine with current date
        taskManager.updateTaskTime(String.valueOf(taskId), newStartTime, null);
    }

    private void scheduleSnoozeReminder(int minutes) {
        long snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Create intent for the reminder
        Intent intent = new Intent(this, ReminderNotificationManager.ReminderReceiver.class);
        intent.setAction("com.simats.schedulytic.SNOOZE_REMINDER");
        intent.putExtra("task_id", taskId);
        intent.putExtra("task_title", taskTitle);
        intent.putExtra("is_pre_reminder", false);
        intent.putExtra("is_snooze", true);

        // Use a unique request code for snooze
        int requestCode = taskId + 20000; // Offset to avoid conflicts

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the alarm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTime,
                            pendingIntent
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTime,
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                );
            }
        } catch (SecurityException e) {
            // Fallback if exact alarm permission not granted
            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
        }
    }

    private String formatSnoozeTime(int minutes) {
        if (minutes < 60) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + " hour" + (hours > 1 ? "s" : "");
            } else {
                return hours + " hour" + (hours > 1 ? "s" : "") + " " + 
                       remainingMinutes + " minute" + (remainingMinutes > 1 ? "s" : "");
            }
        }
    }

    private void animateCardEntry() {
        snoozeCard.setScaleX(0.8f);
        snoozeCard.setScaleY(0.8f);
        snoozeCard.setAlpha(0f);

        snoozeCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    private void animateAndFinish() {
        snoozeCard.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    finish();
                    overridePendingTransition(0, 0);
                })
                .start();
    }

    private void provideHapticFeedback() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    @Override
    public void onBackPressed() {
        animateAndFinish();
    }
}
