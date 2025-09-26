package com.example.shedulytic;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Activity to handle "Can't Complete" task scenarios
 * Provides options to reschedule or cancel the task
 */
public class CantCompleteTaskActivity extends Activity {
    private static final String TAG = "CantCompleteTaskActivity";
    private String taskId;
    private NotificationManager notificationManager;
    private TextView taskInfoText;
    private Button rescheduleButton;
    private Button cancelTaskButton;
    private Button skipForTodayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cant_complete_task);
        
        // Get task ID from intent
        taskId = getIntent().getStringExtra("task_id");
        if (taskId == null) {
            Log.e(TAG, "No task ID provided");
            finish();
            return;
        }

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        initializeViews();
        setupClickListeners();
        
        // Dismiss the notification since user is handling it
        notificationManager.cancel(Integer.parseInt(taskId));
    }

    private void initializeViews() {
        taskInfoText = findViewById(R.id.task_info_text);
        rescheduleButton = findViewById(R.id.reschedule_button);
        cancelTaskButton = findViewById(R.id.cancel_task_button);
        skipForTodayButton = findViewById(R.id.skip_today_button);
        
        // Set task info text
        taskInfoText.setText("What would you like to do with this task?");
    }

    private void setupClickListeners() {
        rescheduleButton.setOnClickListener(v -> showRescheduleOptions());
        
        skipForTodayButton.setOnClickListener(v -> {
            handleSkipForToday();
            finish();
        });
        
        cancelTaskButton.setOnClickListener(v -> {
            handleCancelTask();
            finish();
        });
    }

    private void showRescheduleOptions() {
        // Show options: Tomorrow, This Weekend, Next Week, Pick Date & Time
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Reschedule for when?");
        
        String[] options = {
            "Tomorrow (same time)",
            "This Weekend",
            "Next Week",
            "Pick Date & Time"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    rescheduleTomorrow();
                    break;
                case 1:
                    rescheduleThisWeekend();
                    break;
                case 2:
                    rescheduleNextWeek();
                    break;
                case 3:
                    showDateTimePicker();
                    break;
            }
            finish();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void rescheduleTomorrow() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        
        rescheduleTask(tomorrow.getTime(), "tomorrow");
    }

    private void rescheduleThisWeekend() {
        Calendar weekend = Calendar.getInstance();
        
        // Find next Saturday
        while (weekend.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            weekend.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Set to 10 AM
        weekend.set(Calendar.HOUR_OF_DAY, 10);
        weekend.set(Calendar.MINUTE, 0);
        weekend.set(Calendar.SECOND, 0);
        
        rescheduleTask(weekend.getTime(), "this weekend");
    }

    private void rescheduleNextWeek() {
        Calendar nextWeek = Calendar.getInstance();
        nextWeek.add(Calendar.WEEK_OF_YEAR, 1);
        
        // Set to Monday
        while (nextWeek.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            nextWeek.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        rescheduleTask(nextWeek.getTime(), "next week");
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                showTimePickerForDate(calendar);
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePickerForDate(Calendar selectedDate) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDate.set(Calendar.MINUTE, minute);
                selectedDate.set(Calendar.SECOND, 0);
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
                String dateTimeStr = dateFormat.format(selectedDate.getTime());
                
                rescheduleTask(selectedDate.getTime(), dateTimeStr);            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            false
        );
        
        timePickerDialog.show();
    }

    private void rescheduleTask(Date newDateTime, String timeDescription) {
        try {
            // Update task in database with new date/time
            updateTaskDateTime(newDateTime);
            
            // Schedule new notification
            NotificationHandler notificationHandler = new NotificationHandler(this);
            Task rescheduledTask = createRescheduledTask(newDateTime);
              // Schedule based on task type
            if (isWorkflowTask()) {
                notificationHandler.scheduleWorkflowNotifications(
                    rescheduledTask.getTaskId(),
                    rescheduledTask.getTitle(),
                    rescheduledTask.getDescription(),
                    rescheduledTask.getStartTime(),
                    rescheduledTask.getEndTime(),
                    rescheduledTask.getDueDate()
                );
            } else {
                notificationHandler.scheduleReminderNotification(
                    rescheduledTask.getTaskId(),
                    rescheduledTask.getTitle(),
                    rescheduledTask.getDescription(),
                    rescheduledTask.getStartTime(),
                    rescheduledTask.getDueDate()
                );
            }
            
            Toast.makeText(this, 
                "Task rescheduled for " + timeDescription, 
                Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Task " + taskId + " rescheduled for " + timeDescription);
            
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling task: " + e.getMessage());
            Toast.makeText(this, "Error rescheduling task", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSkipForToday() {
        try {
            // Mark task as skipped for today
            TaskManager taskManager = createTaskManager("Task skipped for today");
            taskManager.updateTaskStatus(taskId, "skipped_today");
            
            Toast.makeText(this, "Task skipped for today", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Task " + taskId + " skipped for today");
            
        } catch (Exception e) {
            Log.e(TAG, "Error skipping task: " + e.getMessage());
            Toast.makeText(this, "Error skipping task", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCancelTask() {
        try {
            // Mark task as cancelled
            TaskManager taskManager = createTaskManager("Task cancelled");
            taskManager.updateTaskStatus(taskId, "cancelled");
            
            Toast.makeText(this, "Task cancelled", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Task " + taskId + " cancelled");
            
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling task: " + e.getMessage());
            Toast.makeText(this, "Error cancelling task", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTaskDateTime(Date newDateTime) {
        try {
            TaskManager taskManager = createTaskManager("Task rescheduled successfully");
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
              SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String newStartTime = timeFormat.format(newDateTime);
            // For end time, let's add 1 hour to start time
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(newDateTime);
            endCalendar.add(Calendar.HOUR_OF_DAY, 1);
            String newEndTime = timeFormat.format(endCalendar.getTime());
            
            taskManager.updateTaskTime(taskId, newStartTime, newEndTime);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating task date/time: " + e.getMessage());
        }
    }

    private Task createRescheduledTask(Date newDateTime) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setTitle("Rescheduled Task"); // This would normally come from database
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        task.setDueDate(dateFormat.format(newDateTime));
        task.setStartTime(datetimeFormat.format(newDateTime));
        
        return task;
    }

    private boolean isWorkflowTask() {
        // Logic to determine if this is a workflow task or reminder
        // This would check task type from database
        return true; // Default to workflow for now
    }    private TaskManager createTaskManager(String successMessage) {
        return new TaskManager(this, new TaskManager.TaskListener() {
            @Override
            public void onTasksLoaded(java.util.List<Task> tasks) {
                // Not needed for this use case
            }
            
            @Override
            public void onTaskAdded(Task task) {
                // Not needed for this use case
            }
            
            @Override
            public void onTaskUpdated(Task task) {
                Log.d(TAG, successMessage);
            }
            
            @Override
            public void onTaskDeleted(String taskId) {
                // Not needed for this use case
            }
            
            @Override
            public void onHabitStreakUpdated(String taskId, int newStreak) {
                // Not needed for this use case
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "TaskManager error: " + error);
            }
        });
    }
}
