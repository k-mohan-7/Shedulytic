package com.example.shedulytic;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private static final String TAG = "TaskAdapter";
    private final List<Task> taskList;
    private final TaskCheckListener checkListener;
    private final Context context;
    private final HabitManager habitManager;

    // Habit-specific display flags
    private boolean showVerificationControls = false;
    private boolean showStreakInfo = false;

    public interface TaskCheckListener {
        void onTaskChecked(Task task, boolean isChecked);
    }

    public TaskAdapter(List<Task> taskList, TaskCheckListener listener, Context context) {
        this.taskList = taskList;
        this.checkListener = listener;
        this.context = context;
        this.habitManager = new HabitManager(context, new HabitManager.HabitListener() {
            @Override
            public void onHabitCompleted(String taskId, int newStreak) {
                updateTaskStreak(taskId, newStreak);
            }

            @Override
            public void onHabitUncompleted(String taskId, int newStreak) {
                updateTaskStreak(taskId, newStreak);
            }

            @Override
            public void onHabitStreakUpdated(String taskId, int newStreak) {
                updateTaskStreak(taskId, newStreak);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "HabitManager error: " + message);
            }
        });
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            // Use item_habit.xml layout for habits in the habits section
            if (showVerificationControls) {
                View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_habit, parent, false);
                return new TaskViewHolder(view);
            }
            
            // Otherwise use regular task layout
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "Error creating view holder: " + e.getMessage());
            // Fallback to task_item.xml if item_task.xml fails to inflate
            View fallbackView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
            return new TaskViewHolder(fallbackView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        try {
            Task task = taskList.get(position);
            Log.d(TAG, "Binding task: " + task.getTitle() + ", Type: " + task.getType());
            holder.bind(task);
        } catch (Exception e) {
            Log.e(TAG, "Error binding task view: " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    private void updateTaskStreak(String taskId, int newStreak) {
        try {
            for (int i = 0; i < taskList.size(); i++) {
                Task task = taskList.get(i);
                if (task.getTaskId() != null && task.getTaskId().equals(taskId)) {
                    task.setCurrentStreak(newStreak);
                    notifyItemChanged(i);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating task streak: " + e.getMessage());
        }
    }

    /**
     * Handle checkbox state changes with optimized performance
     * This prevents multiple rapid clicks and improves animation
     */
    private void handleCheckbox(Task task, CheckBox checkBox) {
        if (checkBox == null) return;
        
        try {
            // Store the original task properties before modifying
            final String originalTitle = task.getTitle();
            final String originalType = task.getType();
            final String originalStartTime = task.getStartTime();
            final String originalEndTime = task.getEndTime();
            final String originalDescription = task.getDescription();
            
            // Update without triggering listener to avoid infinite loops
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(task.getStatus() != null && task.getStatus().equals("completed"));
            
            // Set fade-in animation for better visual effect when changing
            if (checkBox.isChecked()) {
                checkBox.setAlpha(0.7f);
                checkBox.animate().alpha(1.0f).setDuration(200).start();
            }
            
            // Setup listener after setting initial state
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Prevent multiple rapid checkbox clicks
                buttonView.setEnabled(false);
                
                // Update task status based on checkbox
                String newStatus = isChecked ? "completed" : "pending";
                task.setStatus(newStatus);
                
                // Ensure task properties are preserved when unchecking
                if (!isChecked) {
                    task.setTitle(originalTitle);
                    task.setTaskType(originalType);
                    task.setStartTime(originalStartTime);
                    task.setEndTime(originalEndTime);
                    task.setDescription(originalDescription);
                }
                
                // Notify listener about the change
                if (checkListener != null) {
                    checkListener.onTaskChecked(task, isChecked);
                }
                
                // Re-enable after a short delay to prevent double-clicks
                new Handler().postDelayed(() -> {
                    if (buttonView != null) {
                        buttonView.setEnabled(true);
                    }
                }, 300);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error handling checkbox: " + e.getMessage());
        }
    }

    private void updateTaskIcon(TaskViewHolder holder, Task task) {
        try {
            int iconResId;
            int gradientResId;
            String type = task.getType() != null ? task.getType().toLowerCase() : "";
            
            switch (type) {
                case "workflow":
                    iconResId = R.drawable.ic_workflow;
                    gradientResId = R.drawable.gradient_workflow_card;
                    break;
                case "remainder":
                    iconResId = R.drawable.ic_remainder;
                    gradientResId = R.drawable.gradient_reminder_card;
                    break;
                case "habit":
                    iconResId = R.drawable.ic_remainder; // Use appropriate habit icon if available
                    gradientResId = R.drawable.gradient_habit_card;
                    break;
                default:
                    // Default to reminder icon and gradient if type is unrecognized
                    iconResId = R.drawable.ic_remainder;
                    gradientResId = R.drawable.gradient_default_card;
                    break;
            }
            holder.typeIcon.setImageResource(iconResId);
            
            // Set gradient background based on task type
            if (holder.backgroundLayout != null) {
                holder.backgroundLayout.setBackgroundResource(gradientResId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating task icon: " + e.getMessage());
            holder.typeIcon.setImageResource(R.drawable.ic_remainder);
        }
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView timeTextView;
        private final TextView durationTextView;
        private final TextView taskTypeTextView;
        private final ImageView typeIcon;
        private final View backgroundLayout;
        private final CheckBox checkboxView;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Find all views, but handle case where they might not exist in the layout
            TextView title = itemView.findViewById(R.id.taskTitle);
            if (title == null) {
                // If taskTitle doesn't exist, try habit_title (from item_habit.xml)
                title = itemView.findViewById(R.id.habit_title);
            }
            titleTextView = title;
            
            timeTextView = itemView.findViewById(R.id.taskTime);
            durationTextView = itemView.findViewById(R.id.taskDuration);
            taskTypeTextView = itemView.findViewById(R.id.taskType);
            
            ImageView icon = itemView.findViewById(R.id.taskTypeIcon);
            if (icon == null) {
                // If taskTypeIcon doesn't exist, try habit_icon (from item_habit.xml)
                icon = itemView.findViewById(R.id.habit_icon);
            }
            typeIcon = icon;
            
            backgroundLayout = itemView.findViewById(R.id.taskItemBackground);
            
            // Handle the checkbox which might be MaterialCheckBox or regular CheckBox
            CheckBox tempCheckbox = null;
            try {
                // Try to find the checkbox in item_task.xml
                tempCheckbox = itemView.findViewById(R.id.taskCheckbox);
                
                // If not found, try to find it in item_habit.xml
                if (tempCheckbox == null) {
                    tempCheckbox = itemView.findViewById(R.id.habit_checkbox);
                }
                
                if (tempCheckbox != null) {
                    tempCheckbox.setButtonDrawable(R.drawable.custom_checkbox);
                    // Use higher padding to make the checkbox more visible
                    tempCheckbox.setPadding(4, 4, 4, 4);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error finding checkbox view: " + e.getMessage());
            }
            checkboxView = tempCheckbox;
        }

        public void bind(Task task) {
            try {
                if (task == null) return;
                
                Log.d(TAG, "Binding task: " + task.getTitle() + " with status: " + task.getStatus());
                
                // Set title with strikethrough for completed tasks
                if (titleTextView != null) {
                    titleTextView.setText(task.getTitle());
                    
                    // Apply strikethrough effect for completed tasks
                    if (task.getStatus() != null && task.getStatus().equals("completed")) {
                        titleTextView.setPaintFlags(titleTextView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        Log.d(TAG, "Applied strikethrough to task: " + task.getTitle());
                    } else {
                        titleTextView.setPaintFlags(titleTextView.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        Log.d(TAG, "Removed strikethrough from task: " + task.getTitle());
                    }
                }
                
                // Show streak information for habits if enabled
                if (showStreakInfo && task.isHabit()) {
                    TextView streakTextView = itemView.findViewById(R.id.habit_streak);
                    if (streakTextView != null) {
                        int streak = task.getCurrentStreak();
                        String streakText = streak > 0 ? 
                                String.format(Locale.getDefault(), "%d day%s streak", streak, streak > 1 ? "s" : "") : 
                                "Start your streak today!";
                        streakTextView.setText(streakText);
                        streakTextView.setVisibility(View.VISIBLE);
                    }
                }
                
                // Set time
                if (timeTextView != null) {
                    String startTime = task.getStartTime();
                    String endTime = task.getEndTime();
                    if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                        timeTextView.setText(formatTimeRange(startTime, endTime));
                    } else {
                        timeTextView.setText("Time not specified");
                    }
                }
                
                // Set duration (remaining time)
                if (durationTextView != null) {
                    String startTime = task.getStartTime();
                    String endTime = task.getEndTime();
                    if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                        durationTextView.setText("Remaining: " + calculateRemainingTime(startTime, endTime));
                    } else {
                        durationTextView.setText("");
                    }
                }
                
                // Set task type - preserve original type regardless of case
                String type = task.getType();
                if (type == null || type.isEmpty()) {
                    // If type is null or empty, use "remainder" as fallback
                    type = "remainder";
                }
                
                // Capitalize the task type for display
                if (taskTypeTextView != null) {
                    String displayType = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
                    taskTypeTextView.setText(displayType);
                    
                    // Set text color based on type
                    int textColorResId;
                    switch (type.toLowerCase()) {
                        case "workflow":
                            textColorResId = R.color.workflow_color;
                            break;
                        case "remainder":
                            textColorResId = R.color.remainder_color;
                            break;
                        case "habit":
                            textColorResId = R.color.habit_color;
                            break;
                        default:
                            textColorResId = R.color.default_task_color;
                            break;
                    }
                    taskTypeTextView.setTextColor(ContextCompat.getColor(context, textColorResId));
                }
                
                // Update the checkbox state
                handleCheckbox(task, checkboxView);
                
                // Apply visual styling for completed tasks
                if (task.getStatus() != null && task.getStatus().equals("completed")) {
                    // Make completed tasks more faded
                    if (backgroundLayout != null) {
                        backgroundLayout.setAlpha(0.7f);
                    }
                    if (titleTextView != null) titleTextView.setAlpha(0.7f);
                    if (timeTextView != null) timeTextView.setAlpha(0.7f);
                    if (durationTextView != null) durationTextView.setAlpha(0.7f);
                    if (taskTypeTextView != null) taskTypeTextView.setAlpha(0.7f);
                    if (typeIcon != null) typeIcon.setAlpha(0.7f);
                } else {
                    // Normal opacity for pending tasks
                    if (backgroundLayout != null) {
                        backgroundLayout.setAlpha(1.0f);
                    }
                    if (titleTextView != null) titleTextView.setAlpha(1.0f);
                    if (timeTextView != null) timeTextView.setAlpha(1.0f);
                    if (durationTextView != null) durationTextView.setAlpha(1.0f);
                    if (taskTypeTextView != null) taskTypeTextView.setAlpha(1.0f);
                    if (typeIcon != null) typeIcon.setAlpha(1.0f);
                }
                
                // Update task icon based on type
                updateTaskIcon(this, task);
                
                // Show verification controls for habits if enabled
                if (showVerificationControls && task.isHabit()) {
                    String verificationType = task.getVerificationType();
                    
                    // Find verification containers
                    View checkboxContainer = itemView.findViewById(R.id.checkbox_verification_container);
                    View locationContainer = itemView.findViewById(R.id.location_verification_container);
                    View pomodoroContainer = itemView.findViewById(R.id.pomodoro_verification_container);
                    
                    // Hide all containers initially
                    if (checkboxContainer != null) checkboxContainer.setVisibility(View.GONE);
                    if (locationContainer != null) locationContainer.setVisibility(View.GONE);
                    if (pomodoroContainer != null) pomodoroContainer.setVisibility(View.GONE);
                    
                    // Show the appropriate container based on verification type
                    if (verificationType != null) {
                        if (verificationType.equals("location") && locationContainer != null) {
                            locationContainer.setVisibility(View.VISIBLE);
                            Button verifyLocationBtn = itemView.findViewById(R.id.verify_location_button);
                            if (verifyLocationBtn != null) {
                                verifyLocationBtn.setOnClickListener(v -> {
                                    Intent intent = new Intent(context, LocationVerificationActivity.class);
                                    intent.putExtra("habit_id", task.getTaskId());
                                    context.startActivity(intent);
                                });
                            }
                        } else if (verificationType.equals("pomodoro") && pomodoroContainer != null) {
                            pomodoroContainer.setVisibility(View.VISIBLE);
                            Button startPomodoroBtn = itemView.findViewById(R.id.start_pomodoro_button);
                            if (startPomodoroBtn != null) {
                                startPomodoroBtn.setOnClickListener(v -> {
                                    Intent intent = new Intent(context, PomodoroActivity.class);
                                    intent.putExtra("habit_id", task.getTaskId());
                                    intent.putExtra("habit_title", task.getTitle());
                                    context.startActivity(intent);
                                });
                            }
                        } else if (checkboxContainer != null) {
                            checkboxContainer.setVisibility(View.VISIBLE);
                        }
                    } else if (checkboxContainer != null) {
                        // Default to checkbox verification
                        checkboxContainer.setVisibility(View.VISIBLE);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error binding task: " + e.getMessage(), e);
                if (titleTextView != null) titleTextView.setText(task != null ? task.getTitle() : "Error");
            }
        }
    }
    
    /**
     * Format time range for display (e.g., "9:00 AM - 10:30 AM")
     */
    private String formatTimeRange(String startTime, String endTime) {
        try {
            // Extract only the time part if we have a full datetime string (YYYY-MM-DD HH:MM:SS)
            if (startTime != null && startTime.contains(" ")) {
                startTime = startTime.split(" ")[1];
            }
            if (endTime != null && endTime.contains(" ")) {
                endTime = endTime.split(" ")[1];
            }
            
            // Handle common time formats
            if (startTime != null && startTime.contains(":")) {
                SimpleDateFormat inputFormat;
                if (startTime.split(":").length > 2) {
                    // Format with seconds
                    inputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                } else {
                    // Format without seconds
                    inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                }
                
                SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                
                Date startDate = inputFormat.parse(startTime);
                Date endDate = inputFormat.parse(endTime);
                
                if (startDate != null && endDate != null) {
                    return outputFormat.format(startDate) + " - " + outputFormat.format(endDate);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time range: " + e.getMessage());
        }
        
        // Fallback to simple format if parsing fails
        return (startTime != null ? startTime : "") + " - " + (endTime != null ? endTime : "");
    }
    
    /**
     * Calculate remaining time between current time and end time
     */
    private String calculateRemainingTime(String startTime, String endTime) {
        try {
            // Extract only the time part if we have a full datetime string
            if (endTime.contains(" ")) {
                endTime = endTime.split(" ")[1];
            }
            
            // Get today's date for calculation
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String fullEndTime = today + " " + endTime;
            
            // Parse the end time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date endDate = dateFormat.parse(fullEndTime);
            Date currentDate = new Date();
            
            if (endDate != null) {
                // Calculate time difference in minutes
                long diffInMillis = endDate.getTime() - currentDate.getTime();
                
                // If end time has already passed, return "Overdue"
                if (diffInMillis < 0) {
                    return "Overdue";
                }
                
                // Convert to hours and minutes
                long diffMinutes = diffInMillis / (60 * 1000);
                long hours = diffMinutes / 60;
                long minutes = diffMinutes % 60;
                
                // Format the time difference
                if (hours > 0) {
                    return hours + "h " + minutes + "m left";
                } else {
                    return minutes + "m left";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating remaining time: " + e.getMessage());
        }
        
        return "";
    }

    /**
     * Sorts the task list to move completed tasks to the bottom
     */
    public void sortTasksByCompletionStatus() {
        if (taskList != null) {
            Collections.sort(taskList, (task1, task2) -> {
                // First, check completion status - completed tasks go to the bottom
                boolean isCompleted1 = "completed".equals(task1.getStatus());
                boolean isCompleted2 = "completed".equals(task2.getStatus());
                
                if (isCompleted1 && !isCompleted2) {
                    return 1; // t1 is completed, t2 is not, so t1 goes after t2
                } else if (!isCompleted1 && isCompleted2) {
                    return -1; // t1 is not completed, t2 is, so t1 goes before t2
                }
                
                // If both are completed or both are not completed, sort by time
                return 0; // Keep original order
            });
            
            notifyDataSetChanged();
        }
    }

    /**
     * Set whether to show verification controls for habits
     * @param show True to show verification controls
     */
    public void setShowVerificationControls(boolean show) {
        this.showVerificationControls = show;
    }
    
    /**
     * Set whether to show streak information for habits
     * @param show True to show streak information
     */
    public void setShowStreakInfo(boolean show) {
        this.showStreakInfo = show;
    }
}