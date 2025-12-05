package com.simats.schedulytic;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MyDayMapAdapter extends RecyclerView.Adapter<MyDayMapAdapter.TaskViewHolder> {
    private static final String TAG = "MyDayMapAdapter";
    private final List<Task> taskList;
    private final Context context;
    private TaskClickListener clickListener;

    public interface TaskClickListener {
        void onTaskClicked(Task task);
    }

    public MyDayMapAdapter(List<Task> taskList, Context context, TaskClickListener listener) {
        this.taskList = taskList;
        this.context = context;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        try {
            Task task = taskList.get(position);
            holder.bind(task);
        } catch (Exception e) {
            Log.e(TAG, "Error binding task view: " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView timeTextView;
        private final TextView durationTextView;
        private final TextView taskTypeTextView;
        private final ImageView typeIcon;
        private final LinearLayout backgroundLayout;
        private final View statusIndicator;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.taskTitle);
            timeTextView = itemView.findViewById(R.id.taskTime);
            durationTextView = itemView.findViewById(R.id.taskDuration);
            taskTypeTextView = itemView.findViewById(R.id.taskType);
            typeIcon = itemView.findViewById(R.id.taskTypeIcon);
            backgroundLayout = itemView.findViewById(R.id.taskItemBackground);
            statusIndicator = itemView.findViewById(R.id.taskStatusIndicator);
        }

        public void bind(final Task task) {
            try {
                // Set text values safely
                titleTextView.setText(task.getTitle() != null ? task.getTitle() : "Untitled Task");
                
                // Format time
                String timeRange = "";
                if (task.getStartTime() != null && !task.getStartTime().isEmpty() &&
                    task.getEndTime() != null && !task.getEndTime().isEmpty()) {
                    timeRange = task.getStartTime() + " - " + task.getEndTime();
                }
                timeTextView.setText(timeRange);
                
                // Calculate and display remaining time between start and end times
                String duration = calculateRemainingTime(task.getStartTime(), task.getEndTime());
                durationTextView.setText(duration);
                
                // Set task type
                String taskType = task.getType();
                if (taskType != null && !taskType.isEmpty()) {
                    taskType = taskType.toUpperCase();
                } else {
                    taskType = "TASK";
                }
                taskTypeTextView.setText(taskType);
                
                // Set colors based on task type
                int backgroundColor;
                int textColor;
                
                switch (taskType.toLowerCase()) {
                    case "habit":
                        backgroundColor = ContextCompat.getColor(context, R.color.habit_color);
                        textColor = ContextCompat.getColor(context, R.color.black);
                        typeIcon.setImageResource(R.drawable.ic_habit);
                        break;
                    case "remainder":
                        backgroundColor = ContextCompat.getColor(context, R.color.remainder_color);
                        textColor = ContextCompat.getColor(context, R.color.white);
                        typeIcon.setImageResource(R.drawable.ic_remainder);
                        break;
                    case "workflow":
                        backgroundColor = ContextCompat.getColor(context, R.color.workflow_color);
                        textColor = ContextCompat.getColor(context, R.color.white);
                        typeIcon.setImageResource(R.drawable.ic_workflow);
                        break;
                    default:
                        backgroundColor = ContextCompat.getColor(context, R.color.default_task_color);
                        textColor = ContextCompat.getColor(context, R.color.white);
                        typeIcon.setImageResource(R.drawable.ic_task);
                }
                
                // Apply background color
                backgroundLayout.setBackgroundColor(backgroundColor);
                
                // Apply text colors
                titleTextView.setTextColor(textColor);
                timeTextView.setTextColor(textColor);
                durationTextView.setTextColor(textColor);
                
                // Show status indicator based on completion status
                if (statusIndicator != null) {
                    statusIndicator.setVisibility(View.VISIBLE);
                    if (task.isCompleted()) {
                        GradientDrawable completedCircle = new GradientDrawable();
                        completedCircle.setShape(GradientDrawable.OVAL);
                        completedCircle.setColor(ContextCompat.getColor(context, R.color.completed_task));
                        statusIndicator.setBackground(completedCircle);
                    } else {
                        GradientDrawable incompleteCircle = new GradientDrawable();
                        incompleteCircle.setShape(GradientDrawable.OVAL);
                        incompleteCircle.setColor(ContextCompat.getColor(context, R.color.task_indicator_circle));
                        statusIndicator.setBackground(incompleteCircle);
                    }
                }
                
                // Set click listener for the whole card
                itemView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onTaskClicked(task);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error binding task view: " + e.getMessage(), e);
            }
        }
    }
    
    private String calculateRemainingTime(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isEmpty() || endTime.isEmpty()) {
            return "";
        }
        
        try {
            // Parse the time strings (assuming format like "08:00 AM")
            String[] startParts = startTime.split(" ");
            String[] endParts = endTime.split(" ");
            
            if (startParts.length < 2 || endParts.length < 2) {
                return "";
            }
            
            String[] startHourMin = startParts[0].split(":");
            String[] endHourMin = endParts[0].split(":");
            
            if (startHourMin.length < 2 || endHourMin.length < 2) {
                return "";
            }
            
            int startHour = Integer.parseInt(startHourMin[0]);
            int startMinute = Integer.parseInt(startHourMin[1]);
            int endHour = Integer.parseInt(endHourMin[0]);
            int endMinute = Integer.parseInt(endHourMin[1]);
            
            // Convert to 24-hour format if needed
            if (startParts[1].equalsIgnoreCase("PM") && startHour < 12) {
                startHour += 12;
            } else if (startParts[1].equalsIgnoreCase("AM") && startHour == 12) {
                startHour = 0;
            }
            
            if (endParts[1].equalsIgnoreCase("PM") && endHour < 12) {
                endHour += 12;
            } else if (endParts[1].equalsIgnoreCase("AM") && endHour == 12) {
                endHour = 0;
            }
            
            // Calculate total minutes
            int startTotalMinutes = startHour * 60 + startMinute;
            int endTotalMinutes = endHour * 60 + endMinute;
            
            // Handle case where end time is on the next day
            if (endTotalMinutes < startTotalMinutes) {
                endTotalMinutes += 24 * 60; // Add a day
            }
            
            int diffMinutes = endTotalMinutes - startTotalMinutes;
            
            // Format the result
            int hours = diffMinutes / 60;
            int minutes = diffMinutes % 60;
            
            if (hours > 0) {
                return hours + "h " + (minutes > 0 ? minutes + "m" : "");
            } else {
                return minutes + "m";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating time difference: " + e.getMessage());
            return "";
        }
    }
}