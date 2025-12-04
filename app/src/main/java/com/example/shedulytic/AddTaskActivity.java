package com.example.shedulytic;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {
    private TextView fromTimeHours;
    private TextView fromTimeMinutes;
    private TextView fromTimePeriod;
    private TextView toTimeHours;
    private TextView toTimeMinutes;
    private TextView toTimePeriod;
    private LinearLayout fromTimeContainer;
    private LinearLayout toTimeContainer;
    private EditText taskTitle;
    private EditText additionalNotes;
    private LinearLayout btnWorkflow;
    private LinearLayout btnRemainder;
    private LinearLayout btnHabit;  // Add habit button
    private Switch switchDaily;
    private Switch switchWeekly;
    private Switch switchMonthly;
    private Button submitButton;

    private String selectedDate;
    private String selectedTaskType = "remainder"; // Default
    private Calendar calendar;
    private TaskManager taskManager;
    
    // Store time in 24-hour format for internal use
    private int fromHour = 9;
    private int fromMinute = 0;
    private int toHour = 10;
    private int toMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_task);

        // Initialize views
        fromTimeHours = findViewById(R.id.from_time_hours);
        fromTimeMinutes = findViewById(R.id.from_time_minutes);
        fromTimePeriod = findViewById(R.id.from_time_period);
        toTimeHours = findViewById(R.id.to_time_hours);
        toTimeMinutes = findViewById(R.id.to_time_minutes);
        toTimePeriod = findViewById(R.id.to_time_period);
        fromTimeContainer = findViewById(R.id.from_time_container);
        toTimeContainer = findViewById(R.id.to_time_container);
        taskTitle = findViewById(R.id.task_title);
        additionalNotes = findViewById(R.id.additional_notes);
        btnWorkflow = findViewById(R.id.btn_workflow);
        btnRemainder = findViewById(R.id.btn_remainder);
        btnHabit = findViewById(R.id.btn_habit);
        switchDaily = findViewById(R.id.switch_daily);
        switchWeekly = findViewById(R.id.switch_weekly);
        switchMonthly = findViewById(R.id.switch_monthly);
        
        // Initialize time display with default values
        updateTimeDisplay(true);
        updateTimeDisplay(false);

        // Set initial colors
        btnWorkflow.setBackgroundColor(getResources().getColor(R.color.grey));
        btnRemainder.setBackgroundColor(getResources().getColor(R.color.darkGreen));
        if (btnHabit != null) {
        btnHabit.setBackgroundColor(getResources().getColor(R.color.yellow));
        }
        
        // Handle task_type intent parameter from overlay
        String intentTaskType = getIntent().getStringExtra("task_type");
        if (intentTaskType != null) {
            // Pre-select the task type based on intent parameter
            switch (intentTaskType.toLowerCase()) {
                case "workflow":
                    selectedTaskType = "workflow";
                    updateTaskTypeSelection(btnWorkflow, btnRemainder);
                    break;
                case "remainder":
                    selectedTaskType = "remainder";
                    updateTaskTypeSelection(btnRemainder, btnWorkflow);
                    break;
                case "habit":
                    selectedTaskType = "habit";
                    // Note: habit button is removed but keep the type for backend compatibility
                    updateTaskTypeSelection(btnRemainder, btnWorkflow); // Default to remainder UI
                    break;
                default:
                    selectedTaskType = "remainder";
                    updateTaskTypeSelection(btnRemainder, btnWorkflow);
                    break;
            }
        } else {
            // Set remainder as default selected when no intent parameter
            selectedTaskType = "remainder";
            updateTaskTypeSelection(btnRemainder, btnWorkflow);
        }

        // Add submit button at the bottom
        submitButton = findViewById(R.id.submit_task_button);

        // Initialize calendar and task manager
        calendar = Calendar.getInstance();
        taskManager = new TaskManager(this, new TaskManager.TaskListener() {
            @Override
            public void onTasksLoaded(java.util.List<Task> tasks) {
                // Not needed for this activity
            }

            @Override
            public void onTaskAdded(Task task) {
                // Show success message
                TextView successMessage = findViewById(R.id.success_message);
                if (successMessage != null) {
                    successMessage.setVisibility(View.VISIBLE);
                }
                
                // Display toast notification
                Toast.makeText(AddTaskActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                
                // Delay closing the activity to allow the user to see the success message
                new Handler().postDelayed(() -> finish(), 1500);
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
            public void onHabitStreakUpdated(String taskId, int newStreak) {
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
        
        // Initialize calendar display
        initializeCalendarDisplay();
        
        // Set up click listeners
        setupClickListeners();
    }

    /**
     * Initialize the calendar display with real-time data
     */
    private void initializeCalendarDisplay() {
        // Set the current month and year display
        TextView monthYearText = findViewById(R.id.calendar_month_year);
        if (monthYearText != null) {
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            monthYearText.setText(monthYearFormat.format(calendar.getTime()));
        }
        
        // Get the GridLayout that holds the date items
        GridLayout dateContainer = findViewById(R.id.calendar_dates_container);
        if (dateContainer == null) {
            Log.e("AddTaskActivity", "Date container not found in layout");
            return;
        }
        
        // Clear existing date views if any
        dateContainer.removeAllViews();
        
        // Get current date for highlighting today
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH);
        int currentYear = today.get(Calendar.YEAR);
        
        // Clone calendar to avoid modifying the original
        Calendar calendarCopy = (Calendar) calendar.clone();
        
        // Get the number of days in the current month
        int daysInMonth = calendarCopy.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Get the day of week for the first day of the month
        calendarCopy.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendarCopy.get(Calendar.DAY_OF_WEEK);
        
        // Adjust for weeks starting on Monday (if needed)
        int startOffset = firstDayOfWeek - 1;
        
        // Add days from previous month to align with the day of week
        for (int i = 0; i < startOffset; i++) {
            View emptyView = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = getResources().getDimensionPixelSize(R.dimen.date_item_width);
            params.height = getResources().getDimensionPixelSize(R.dimen.date_item_height);
            emptyView.setLayoutParams(params);
            dateContainer.addView(emptyView);
        }
        
        // Add days of the current month
        for (int day = 1; day <= daysInMonth; day++) {
            // Create date item view
            View dateItemView = getLayoutInflater().inflate(R.layout.calendar_date_item, dateContainer, false);
            TextView dateText = dateItemView.findViewById(R.id.date_text);
            
            if (dateText != null) {
                // Set the day number
                dateText.setText(String.valueOf(day));
                
                // Check if this is today
                boolean isToday = (day == currentDay && 
                                  calendar.get(Calendar.MONTH) == currentMonth && 
                                  calendar.get(Calendar.YEAR) == currentYear);
                
                // Highlight today's date with bold text and white color
                if (isToday) {
                    dateText.setTypeface(dateText.getTypeface(), android.graphics.Typeface.BOLD);
                    dateText.setBackgroundResource(R.drawable.today_date_background);
                    dateText.setTextColor(getResources().getColor(android.R.color.white));
                }
                
                // Set click listener to select this date
                final int dayFinal = day;
                dateItemView.setOnClickListener(v -> {
                    // Update selected date
                    calendar.set(Calendar.DAY_OF_MONTH, dayFinal);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = dateFormat.format(calendar.getTime());
                    
                    // Update selected date display
                    updateSelectedDateDisplay();
                    
                    // Highlight selected date by updating UI
                    updateSelectedDateUI(dayFinal);
                    
                    Log.d("AddTaskActivity", "Selected date: " + selectedDate);
                });
            }
            
            // Add the date item to the grid
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = GridLayout.LayoutParams.WRAP_CONTENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            dateContainer.addView(dateItemView, params);
        }
        
        // Update selected date display after initializing the calendar
        updateSelectedDateDisplay();
    }

    /**
     * Updates the selected date display that shows what date will be used
     */
    private void updateSelectedDateDisplay() {
        // Check if we already created the selected date display text view
        TextView selectedDateDisplay = null;
        
        // Try to find by tag instead of by ID (since we create it dynamically)
        ViewGroup calendarParent = (ViewGroup) findViewById(R.id.calendar_container).getParent();
        for (int i = 0; i < calendarParent.getChildCount(); i++) {
            View child = calendarParent.getChildAt(i);
            if (child instanceof TextView && "selected_date_display".equals(child.getTag())) {
                selectedDateDisplay = (TextView) child;
                break;
            }
        }
        
        // If not found, we'll add it dynamically after the calendar
        if (selectedDateDisplay == null) {
            int calendarIndex = calendarParent.indexOfChild(findViewById(R.id.calendar_container));
            
            selectedDateDisplay = new TextView(this);
            selectedDateDisplay.setId(View.generateViewId());
            selectedDateDisplay.setTag("selected_date_display");  // Set a tag to find it later
            selectedDateDisplay.setBackgroundResource(R.drawable.selected_date_display_background);
            selectedDateDisplay.setPadding(16, 8, 16, 8);
            selectedDateDisplay.setTextColor(getResources().getColor(R.color.darkGreen));
            selectedDateDisplay.setTextSize(16);
            selectedDateDisplay.setGravity(android.view.Gravity.CENTER);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            
            calendarParent.addView(selectedDateDisplay, calendarIndex + 1, params);
        }
        
        // Format the date for display (e.g., "Selected Date: Friday, May 15, 2025")
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        selectedDateDisplay.setText("Selected Date: " + displayFormat.format(calendar.getTime()));
    }

    private void setupClickListeners() {
        // Back button click listener with improved error handling
        View backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Log.d("AddTaskActivity", "Back button clicked");
                finish();
            });
        } else {
            Log.e("AddTaskActivity", "Back button not found in layout");
        }

        // Calendar container click to show date picker
        findViewById(R.id.calendar_container).setOnClickListener(v -> showDatePicker());

        // Time selection click listeners - now using Material Time Picker
        fromTimeContainer.setOnClickListener(v -> showMaterialTimePicker(true));
        toTimeContainer.setOnClickListener(v -> showMaterialTimePicker(false));

        // Task type selection - workflow and remainder only
        btnWorkflow.setOnClickListener(v -> {
            selectedTaskType = "workflow";
            updateTaskTypeSelection(btnWorkflow, btnRemainder);
        });

        btnRemainder.setOnClickListener(v -> {
            selectedTaskType = "remainder";
            updateTaskTypeSelection(btnRemainder, btnWorkflow);
        });

        // Completely remove habit button
        if (btnHabit != null) {
            btnHabit.setVisibility(View.GONE);
            ViewGroup parent = (ViewGroup) btnHabit.getParent();
            if (parent != null) {
                parent.removeView(btnHabit);
            }
            btnHabit = null; // Set to null to prevent any further references
        }

        // Submit button
        submitButton.setOnClickListener(v -> submitTask());
    }

    private void updateTaskTypeSelection(LinearLayout selected, LinearLayout... others) {
        // Set the selected button's state
        selected.setBackgroundColor(getTaskTypeColor(selectedTaskType));
        View selectedIndicator = selected.findViewById(getTaskTypeIndicatorId(selectedTaskType));
        if (selectedIndicator != null) {
            selectedIndicator.setVisibility(View.VISIBLE);
        }

        // Reset other buttons
        for (LinearLayout other : others) {
            other.setBackgroundColor(getResources().getColor(R.color.grey));
            View indicator = other.findViewById(getTaskTypeIndicatorId(other.getId()));
            if (indicator != null) {
                indicator.setVisibility(View.INVISIBLE);
            }
        }
    }

    private int getTaskTypeColor(String taskType) {
        switch (taskType.toLowerCase()) {
            case "workflow":
                return getResources().getColor(R.color.darkBlue);
            case "remainder":
                return getResources().getColor(R.color.darkGreen);
            // Habit case removed as requested
            default:
                return getResources().getColor(R.color.grey);
        }
    }

    private int getTaskTypeIndicatorId(String taskType) {
        switch (taskType.toLowerCase()) {
            case "workflow":
                return R.id.workflow_indicator;
            case "remainder":
                return R.id.remainder_indicator;
            // Habit case removed as requested
            default:
                return 0;
        }
    }

    private int getTaskTypeIndicatorId(int buttonId) {
        if (buttonId == R.id.btn_workflow) {
            return R.id.workflow_indicator;
        } else if (buttonId == R.id.btn_remainder) {
            return R.id.remainder_indicator;
        }
        // Habit case removed as requested
        return 0;
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
                    
                    // Just log the selected date for now
                    Log.d("AddTaskActivity", "Selected date: " + selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    /**
     * Shows the Material Time Picker with scrolling wheels like alarm apps
     */
    private void showMaterialTimePicker(boolean isFromTime) {
        int currentHour = isFromTime ? fromHour : toHour;
        int currentMinute = isFromTime ? fromMinute : toMinute;
        
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(currentHour)
                .setMinute(currentMinute)
                .setTitleText(isFromTime ? "Select Start Time" : "Select End Time")
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();
        
        picker.addOnPositiveButtonClickListener(v -> {
            int selectedHour = picker.getHour();
            int selectedMinute = picker.getMinute();
            
            if (isFromTime) {
                fromHour = selectedHour;
                fromMinute = selectedMinute;
            } else {
                toHour = selectedHour;
                toMinute = selectedMinute;
            }
            
            updateTimeDisplay(isFromTime);
        });
        
        picker.show(getSupportFragmentManager(), isFromTime ? "from_time_picker" : "to_time_picker");
    }
    
    /**
     * Updates the time display on the cards
     */
    private void updateTimeDisplay(boolean isFromTime) {
        int hour = isFromTime ? fromHour : toHour;
        int minute = isFromTime ? fromMinute : toMinute;
        
        // Convert to 12-hour format
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        
        if (isFromTime) {
            fromTimeHours.setText(String.format(Locale.getDefault(), "%02d", displayHour));
            fromTimeMinutes.setText(String.format(Locale.getDefault(), "%02d", minute));
            fromTimePeriod.setText(period);
        } else {
            toTimeHours.setText(String.format(Locale.getDefault(), "%02d", displayHour));
            toTimeMinutes.setText(String.format(Locale.getDefault(), "%02d", minute));
            toTimePeriod.setText(period);
        }
    }

    private void submitTask() {
        String title = taskTitle.getText().toString().trim();
        String description = additionalNotes.getText().toString().trim();
        // Use the stored 24-hour format values for task manager
        String startTime = formatTimeForTaskManager(String.valueOf(fromHour), String.format(Locale.getDefault(), "%02d", fromMinute));
        String endTime = formatTimeForTaskManager(String.valueOf(toHour), String.format(Locale.getDefault(), "%02d", toMinute));

        // Validate input
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate time - prevent creating tasks for past dates/times
        if (!validateTimeNotInPast(selectedDate, startTime)) {
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
        
        // Disable submit button to prevent multiple submissions
        submitButton.setEnabled(false);
        submitButton.setText("Adding task...");
        
        // Get user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");

        // Log what we're sending to the server
        Log.d("AddTaskActivity", "Adding task with date: " + selectedDate + 
                ", startTime: " + startTime + ", endTime: " + endTime);
        
        // Set flag for HomeFragment to refresh immediately
        SharedPreferences refreshPrefs = getSharedPreferences("RefreshState", Context.MODE_PRIVATE);
        refreshPrefs.edit()
            .putBoolean("refresh_on_resume", true)
            .putLong("last_task_added", System.currentTimeMillis())
            .apply();
        
        // Show progress indicator
        View progressIndicator = findViewById(R.id.progress_indicator);
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        
        // Create final copies of the variables for the lambda
        final String finalTitle = title;
        final String finalDescription = description;
        final String finalStartTime = startTime;
        final String finalEndTime = endTime;
        final String finalRepeatFrequency = repeatFrequency;
        final String finalSelectedDate = selectedDate;
        
        // Use a Handler to manage UI updates from the main thread
        final Handler mainHandler = new Handler(getMainLooper());
        
        // Create a separate thread for network operation
        Thread networkThread = new Thread(() -> {
            // Create a special TaskManager just for adding this task
            TaskManager networkTaskManager = new TaskManager(getApplicationContext(), new TaskManager.TaskListener() {
                @Override
                public void onTasksLoaded(List<Task> tasks) {}
                
                @Override
                public void onTaskAdded(Task task) {
                    // Update UI on the main thread with success
                    mainHandler.post(() -> {
                        // Note: Reminder notifications are now scheduled by TaskManager
                        // No need to schedule here to avoid duplicate notifications
                        Log.d("AddTaskActivity", "Task added, notifications scheduled by TaskManager");
                        
                        // Show success message
                        TextView successMessage = findViewById(R.id.success_message);
                        if (successMessage != null) {
                            successMessage.setVisibility(View.VISIBLE);
                        }
                        
                        // Hide progress indicator
                        View progressBar = findViewById(R.id.progress_indicator);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        
                        // Display toast notification
                        Toast.makeText(AddTaskActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Log success
                    Log.d("AddTaskActivity", "Task successfully added to server with ID: " + task.getTaskId());
                        
                        // Close activity after a short delay
                        new Handler().postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 1000);
                    });
                }
                
                @Override
                public void onTaskUpdated(Task task) {}
                
                @Override
                public void onTaskDeleted(String taskId) {}
                
                @Override
                public void onHabitStreakUpdated(String taskId, int newStreak) {}
                
                @Override
                public void onError(String message) {
                    // Update UI on the main thread with error
                    mainHandler.post(() -> {
                        // Re-enable submit button
                        submitButton.setEnabled(true);
                        submitButton.setText("Add Task");
                        
                        // Hide progress indicator
                        View progressBar = findViewById(R.id.progress_indicator);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        
                        // Show error message
                    Log.e("AddTaskActivity", "Error adding task to server: " + message);
                        Toast.makeText(AddTaskActivity.this, 
                            "Failed to add task to server. Please try again.", Toast.LENGTH_LONG).show();
                    });
                }
            });
            
            try {
                // Add task to database in background with the selected date
                networkTaskManager.addTask(finalTitle, finalDescription, finalStartTime, finalEndTime, 
                        finalSelectedDate, selectedTaskType, finalRepeatFrequency);
            } catch (Exception e) {
                // Handle any unexpected exceptions
                Log.e("AddTaskActivity", "Exception during task submission: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    // Re-enable submit button
                    submitButton.setEnabled(true);
                    submitButton.setText("Add Task");
                    
                    // Hide progress indicator if exists
                    View progressBar = findViewById(R.id.progress_indicator);
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    // Show error message
                    Toast.makeText(AddTaskActivity.this, 
                        "Error adding task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        networkThread.start();
    }

    /**
     * Updates the UI to reflect the newly selected date
     */
    private void updateSelectedDateUI(int selectedDay) {
        // Get the date container
        GridLayout dateContainer = findViewById(R.id.calendar_dates_container);
        if (dateContainer == null) return;
        
        // Get current date for highlighting today
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH);
        int currentYear = today.get(Calendar.YEAR);
        
        // Reset all date items and highlight the selected one
        for (int i = 0; i < dateContainer.getChildCount(); i++) {
            View child = dateContainer.getChildAt(i);
            TextView dateText = child.findViewById(R.id.date_text);
            
            if (dateText != null) {
                String dayText = dateText.getText().toString();
                
                if (!dayText.isEmpty()) {
                    int day = Integer.parseInt(dayText);
                    
                    // Check if this is today's date
                    boolean isToday = (day == currentDay && 
                                     calendar.get(Calendar.MONTH) == currentMonth && 
                                     calendar.get(Calendar.YEAR) == currentYear);
                    
                    // Check if this is the selected date
                    boolean isSelected = (day == selectedDay);
                    
                    // Reset text style
                    dateText.setTypeface(null, isToday ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                    
                    // Set background and text color based on whether it's selected or today
                    if (isSelected) {
                        dateText.setBackgroundResource(R.drawable.selected_date_background);
                        dateText.setTextColor(getResources().getColor(R.color.black));
                    } else if (isToday) {
                        dateText.setBackgroundResource(R.drawable.today_date_background);
                        dateText.setTextColor(getResources().getColor(android.R.color.white));
                    } else {
                        dateText.setBackgroundResource(android.R.color.transparent);
                        dateText.setTextColor(getResources().getColor(R.color.text_primary));
                    }
                }
            }
        }
    }

    /**
     * Converts 24-hour time format to 12-hour format with AM/PM for TaskManager
     */
    private String formatTimeForTaskManager(String hours, String minutes) {
        try {
            int hour = Integer.parseInt(hours);
            int min = Integer.parseInt(minutes);
            
            String amPm = "AM";
            if (hour >= 12) {
                amPm = "PM";
                if (hour > 12) {
                    hour -= 12;
                }
            } else if (hour == 0) {
                hour = 12;
            }
            
            return String.format("%02d:%02d %s", hour, min, amPm);
        } catch (NumberFormatException e) {
            // Fallback to default format if parsing fails
            return hours + ":" + minutes + " AM";
        }
    }

    /**
     * Validates that the selected date and time are not in the past
     * @param date The selected date string in format "MMM dd, yyyy"
     * @param startTime The start time string in format "HH:mm AM/PM"
     * @return true if the time is valid (not in past), false otherwise
     */
    private boolean validateTimeNotInPast(String date, String startTime) {
        try {
            // Parse the selected date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date selectedDate = dateFormat.parse(date);
            
            if (selectedDate == null) {
                Log.e("AddTaskActivity", "Failed to parse selected date: " + date);
                return true; // Allow if we can't parse to avoid blocking valid tasks
            }
            
            // Parse the start time
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date selectedTime = timeFormat.parse(startTime);
            
            if (selectedTime == null) {
                Log.e("AddTaskActivity", "Failed to parse start time: " + startTime);
                return true; // Allow if we can't parse to avoid blocking valid tasks
            }
            
            // Combine date and time
            Calendar selectedDateTime = Calendar.getInstance();
            selectedDateTime.setTime(selectedDate);
            
            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(selectedTime);
            
            selectedDateTime.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            selectedDateTime.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            selectedDateTime.set(Calendar.SECOND, 0);
            selectedDateTime.set(Calendar.MILLISECOND, 0);
            
            // Get current time
            Calendar now = Calendar.getInstance();
            
            // Check if the selected date/time is in the past
            if (selectedDateTime.before(now)) {
                // Show warning dialog
                new AlertDialog.Builder(this)
                    .setTitle("Invalid Time")
                    .setMessage("You cannot create a task for a past date and time. Please select a future date and time.")
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e("AddTaskActivity", "Error validating time: " + e.getMessage());
            // If there's an error parsing, allow the task to be created to avoid blocking valid tasks
            return true;
        }
    }
}