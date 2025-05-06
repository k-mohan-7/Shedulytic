package com.example.shedulytic;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {
    private TextView fromTimeHours;
    private TextView fromTimeMinutes;
    private TextView toTimeHours;
    private TextView toTimeMinutes;
    private EditText taskTitle;
    private EditText additionalNotes;
    private LinearLayout btnWorkflow;
    private LinearLayout btnRemainder;
    private Switch switchDaily;
    private Switch switchWeekly;
    private Switch switchMonthly;
    private Button submitButton;
    
    private String selectedDate;
    private String selectedTaskType = "remainder"; // Default
    private Calendar calendar;
    private TaskManager taskManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_task);
        
        // Initialize views
        fromTimeHours = findViewById(R.id.from_time_hours);
        fromTimeMinutes = findViewById(R.id.from_time_minutes);
        toTimeHours = findViewById(R.id.to_time_hours);
        toTimeMinutes = findViewById(R.id.to_time_minutes);
        taskTitle = findViewById(R.id.task_title);
        additionalNotes = findViewById(R.id.additional_notes);
        btnWorkflow = findViewById(R.id.btn_workflow);
        btnRemainder = findViewById(R.id.btn_remainder);
        switchDaily = findViewById(R.id.switch_daily);
        switchWeekly = findViewById(R.id.switch_weekly);
        switchMonthly = findViewById(R.id.switch_monthly);
        
        // Add submit button at the bottom
        submitButton = new Button(this);
        submitButton.setText("Add Task");
        submitButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        submitButton.setTextColor(getResources().getColor(android.R.color.white));
        
        LinearLayout mainLayout = findViewById(R.id.add_task_main_layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 32, 0, 16);
        mainLayout.addView(submitButton, params);
        
        // Initialize calendar and task manager
        calendar = Calendar.getInstance();
        taskManager = new TaskManager(this, new TaskManager.TaskListener() {
            @Override
            public void onTasksLoaded(java.util.List<Task> tasks) {
                // Not needed for this activity
            }

            @Override
            public void onTaskAdded(Task task) {
                Toast.makeText(AddTaskActivity.this, "Task added successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onTaskUpdated(Task task) {
                // Not needed for this activity
            }

            @Override
            public void onTaskDeleted(String taskId) {
                // Not needed for this activity
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddTaskActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set default date to today
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = dateFormat.format(calendar.getTime());
        
        // Set up click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Calendar container click to show date picker
        findViewById(R.id.calendar_container).setOnClickListener(v -> showDatePicker());
        
        // Time selection click listeners
        fromTimeHours.setOnClickListener(v -> showTimePicker(true, true));
        fromTimeMinutes.setOnClickListener(v -> showTimePicker(true, false));
        toTimeHours.setOnClickListener(v -> showTimePicker(false, true));
        toTimeMinutes.setOnClickListener(v -> showTimePicker(false, false));
        
        // Task type selection
        btnWorkflow.setOnClickListener(v -> {
            selectedTaskType = "workflow";
            btnWorkflow.setBackgroundColor(getResources().getColor(R.color.darkBlue));
            btnRemainder.setBackgroundColor(getResources().getColor(R.color.darkGreen));
        });
        
        btnRemainder.setOnClickListener(v -> {
            selectedTaskType = "remainder";
            btnRemainder.setBackgroundColor(getResources().getColor(R.color.darkGreen));
            btnWorkflow.setBackgroundColor(getResources().getColor(R.color.darkBlue));
        });
        
        // Submit button
        submitButton.setOnClickListener(v -> submitTask());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = dateFormat.format(calendar.getTime());
                    
                    // Update calendar display
                    // In a real implementation, you would update the calendar view
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isFromTime, boolean isHours) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    if (isFromTime) {
                        if (isHours) {
                            fromTimeHours.setText(String.format(Locale.getDefault(), "%02d", hourOfDay));
                        } else {
                            fromTimeMinutes.setText(String.format(Locale.getDefault(), "%02d", minute));
                        }
                    } else {
                        if (isHours) {
                            toTimeHours.setText(String.format(Locale.getDefault(), "%02d", hourOfDay));
                        } else {
                            toTimeMinutes.setText(String.format(Locale.getDefault(), "%02d", minute));
                        }
                    }
                },
                isHours ? Integer.parseInt(isFromTime ? fromTimeHours.getText().toString() : toTimeHours.getText().toString()) : 0,
                isHours ? 0 : Integer.parseInt(isFromTime ? fromTimeMinutes.getText().toString() : toTimeMinutes.getText().toString()),
                true);
        
        timePickerDialog.show();
    }

    private void submitTask() {
        String title = taskTitle.getText().toString().trim();
        String description = additionalNotes.getText().toString().trim();
        String startTime = fromTimeHours.getText().toString() + ":" + fromTimeMinutes.getText().toString();
        String endTime = toTimeHours.getText().toString() + ":" + toTimeMinutes.getText().toString();
        
        // Validate input
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Determine repeat frequency
        String repeatFrequency = "none";
        if (switchDaily.isChecked()) {
            repeatFrequency = "daily";
        } else if (switchWeekly.isChecked()) {
            repeatFrequency = "weekly";
        } else if (switchMonthly.isChecked()) {
            repeatFrequency = "monthly";
        }
        
        // Add task to database
        taskManager.addTask(title, description, startTime, endTime, selectedDate, selectedTaskType, repeatFrequency);
    }
}