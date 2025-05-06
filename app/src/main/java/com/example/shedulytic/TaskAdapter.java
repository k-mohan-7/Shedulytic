package com.example.shedulytic;

import android.content.Context;
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

public class TaskAdapter<T> extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<T> itemList;
    private Context context;
    private boolean isTaskItemList;

    // Generic constructor that works for both TaskItem and Task objects
    public TaskAdapter(Context context, List<T> itemList) {
        this.context = context;
        this.itemList = itemList;
        // Determine the type of list based on the first non-null item
        if (itemList != null && !itemList.isEmpty() && itemList.get(0) != null) {
            this.isTaskItemList = itemList.get(0) instanceof TaskItem;
        } else {
            // Default to TaskItem if list is empty (can be adjusted based on usage context)
            this.isTaskItemList = true;
        }
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the updated task_item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // Handle different types of objects based on the list type
        if (isTaskItemList) {
            // Handle TaskItem objects
            TaskItem taskItem = (TaskItem) itemList.get(position);
            
            // Handle empty state
            if ("empty".equals(taskItem.getType())) {
                holder.taskTitle.setText(taskItem.getTitle());
                holder.taskTime.setVisibility(View.GONE);
                holder.taskDuration.setVisibility(View.GONE);
                holder.taskStatus.setVisibility(View.GONE);
                holder.taskIcon.setVisibility(View.GONE);
                holder.taskTypeLabel.setVisibility(View.GONE);
                return;
            }
            
            // Set task details
            holder.taskTitle.setText(taskItem.getTitle());
            holder.taskTime.setText(taskItem.getTime());
            holder.taskDuration.setText(taskItem.getDuration());
            holder.taskStatus.setText(""); // TaskItem doesn't have status
            holder.taskTypeLabel.setText(taskItem.getType().toUpperCase());
            
            // Set icon, background color, and text colors based on task type
            String taskType = taskItem.getType().toUpperCase();
            
            // Apply styling based on task type
            applyTaskStyling(holder, taskType);
        } else {
            // Handle Task objects
            Task task = (Task) itemList.get(position);
            
            // Handle empty state
            if ("empty".equals(task.getTaskType())) {
                holder.taskTitle.setText(task.getTitle());
                holder.taskTime.setVisibility(View.GONE);
                holder.taskDuration.setVisibility(View.GONE);
                holder.taskStatus.setVisibility(View.GONE);
                holder.taskIcon.setVisibility(View.GONE);
                holder.taskTypeLabel.setVisibility(View.GONE);
                return;
            }
            
            // Set task details
            holder.taskTitle.setText(task.getTitle());
            
            // Set time
            String timeText = task.getStartTime();
            if (task.getEndTime() != null && !task.getEndTime().isEmpty()) {
                timeText += " - " + task.getEndTime();
            }
            holder.taskTime.setText(timeText);
            
            // Set status
            holder.taskStatus.setText(formatStatus(task.getStatus()));
            
            // Set duration
            String duration = task.getDuration();
            if (duration == null || duration.isEmpty()) {
                // Calculate duration if not provided
                duration = calculateDuration(task.getStartTime(), task.getEndTime());
            }
            holder.taskDuration.setText(duration);
            
            // Set task type label
            holder.taskTypeLabel.setText(task.getTaskType().toUpperCase());
            
            // Set icon, background color, and text colors based on task type
            String taskType = task.getTaskType().toUpperCase();
            
            // Apply styling based on task type
            applyTaskStyling(holder, taskType);
        }
    }
    
    // Helper method to apply styling based on task type
    private void applyTaskStyling(TaskViewHolder holder, String taskType) {
        switch (taskType) {
            case "HABIT":
                holder.taskIcon.setImageResource(R.drawable.ic_gym); // Example icon for Habit
                holder.taskItemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
                holder.taskTitle.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                holder.taskTime.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                holder.taskDuration.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                holder.taskStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                holder.taskTypeLabel.setBackgroundResource(R.drawable.label_background);
                holder.taskTypeLabel.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                break;
            case "REMAINDER":
                holder.taskIcon.setImageResource(R.drawable.ic_group); // Example icon for Reminder
                holder.taskItemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.darkGreen));
                holder.taskTitle.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.taskTime.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.taskDuration.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
                holder.taskStatus.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
                holder.taskTypeLabel.setBackgroundResource(R.drawable.label_background);
                holder.taskTypeLabel.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                break;
            case "WORKFLOW":
                holder.taskIcon.setImageResource(R.drawable.workflow_logo); // Example icon for Workflow
                holder.taskItemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.darkBlue));
                holder.taskTitle.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.taskTime.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.taskDuration.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
                holder.taskStatus.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
                holder.taskTypeLabel.setBackgroundResource(R.drawable.label_background);
                holder.taskTypeLabel.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                break;
            default:
                // Default appearance if type is unknown
                holder.taskIcon.setImageResource(R.drawable.ic_task); // Default task icon
                holder.taskItemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
                holder.taskTitle.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                holder.taskTime.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                holder.taskDuration.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                holder.taskStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                holder.taskTypeLabel.setText("TASK");
                holder.taskTypeLabel.setBackgroundResource(R.drawable.label_background);
                holder.taskTypeLabel.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    // ViewHolder class holds references to the views in task_item.xml
    // Helper method to format status
    private String formatStatus(String status) {
        // Convert status like "in_progress" to "In Progress"
        String[] words = status.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return formatted.toString().trim();
    }

    // Helper method to calculate duration
    private String calculateDuration(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isEmpty() || endTime.isEmpty()) {
            return "";
        }
        
        try {
            // Parse times in format like "08:10 am"
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
            java.util.Date start = format.parse(startTime);
            java.util.Date end = format.parse(endTime);
            
            if (start != null && end != null) {
                // Calculate difference in minutes
                long diffMs = end.getTime() - start.getTime();
                long diffMinutes = diffMs / (60 * 1000);
                
                // Format the duration
                if (diffMinutes < 60) {
                    return diffMinutes + " mins";
                } else {
                    long hours = diffMinutes / 60;
                    long mins = diffMinutes % 60;
                    if (mins > 0) {
                        return hours + " hr " + mins + " mins";
                    } else {
                        return hours + " hr";
                    }
                }
            }
        } catch (Exception e) {
            // In case of parsing error
            return "";
        }
        return "";
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        ImageView taskIcon;
        TextView taskTitle, taskTime, taskDuration, taskTypeLabel, taskStatus;
        LinearLayout taskItemLayout; // Reference to the main layout for background color changes

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find views by their IDs from the updated task_item.xml
            taskIcon = itemView.findViewById(R.id.task_icon);
            taskTitle = itemView.findViewById(R.id.task_title);
            taskTime = itemView.findViewById(R.id.task_time);
            taskDuration = itemView.findViewById(R.id.task_duration);
            taskStatus = itemView.findViewById(R.id.task_status);
            taskTypeLabel = itemView.findViewById(R.id.task_type_label);
            taskItemLayout = itemView.findViewById(R.id.task_item_layout);
        }
    }
}