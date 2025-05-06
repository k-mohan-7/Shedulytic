package com.example.shedulytic;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Import the new TaskItem
import com.example.shedulytic.TaskItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Remove TaskManager.TaskListener implementation
public class TaskFragment extends Fragment {
    private TextView totalWorkflowCount;
    private TextView totalRemainderCount;
    private TextView completedWorkflowCount;
    private TextView completedRemainderCount;
    private Button addTaskButton;
    // Remove timelineContainer as it's part of the main layout now
    // private LinearLayout timelineContainer;
    private RecyclerView taskRecyclerView;
    private TaskAdapter taskAdapter; // Use the updated TaskAdapter
    // Remove old TaskManager and taskList
    // private TaskManager taskManager;
    // private List<Task> taskList = new ArrayList<>();

    // Use the new TaskItem list
    private List<TaskItem> taskItemList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);
        
        // Initialize views (IDs might need checking against the updated fragment_task.xml)
        totalWorkflowCount = view.findViewById(R.id.total_workflow_count);
        totalRemainderCount = view.findViewById(R.id.total_remainder_count);
        completedWorkflowCount = view.findViewById(R.id.completed_workflow_count);
        completedRemainderCount = view.findViewById(R.id.completed_remainder_count);
        addTaskButton = view.findViewById(R.id.add_task_button);
        // timelineContainer = view.findViewById(R.id.timeline_container); // Removed
        taskRecyclerView = view.findViewById(R.id.task_recycler_view);
        
        // Set up RecyclerView with the new Adapter and TaskItem list
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new TaskAdapter(getContext(), taskItemList); // Use new adapter constructor
        taskRecyclerView.setAdapter(taskAdapter);
        
        // Remove old TaskManager initialization
        // taskManager = new TaskManager(getContext(), this);
        
        // Connect Add Task button to AddTaskActivity
        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTaskActivity.class);
            startActivity(intent);
        });
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadTasks(); // Load initial data
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks(); // Refresh tasks when returning to this fragment
    }

    // Updated loadTasks to populate with dummy TaskItem data
    private void loadTasks() {
        // ** Placeholder Data Loading **
        // Replace this with your actual database fetching logic
        // to retrieve data and create TaskItem objects.
        taskItemList.clear(); // Clear previous data

        // Add dummy data matching the image
        taskItemList.add(new TaskItem("Gym", "08:10 am - 08:50 am", "40 mins", "HABIT"));
        taskItemList.add(new TaskItem("Sales Discussion", "09:30 am - 10:30 am", "60 mins", "REMAINDER"));
        taskItemList.add(new TaskItem("Workflow Lunch", "12:00 pm - 01:30 pm", "90 mins", "WORKFLOW"));
        taskItemList.add(new TaskItem("Remainder Sales D...", "12:00 pm - 12:40 pm", "40 mins", "REMAINDER"));
        // Add more dummy tasks as needed

        taskAdapter.notifyDataSetChanged(); // Notify adapter about data change
        updateTaskCounts(); // Update summary counts
    }

    // Update task counts based on the new TaskItem list
    private void updateTaskCounts() {
        int totalWorkflow = 0;
        int totalRemainder = 0;
        int totalHabit = 0; // Added habit count
        // Completed counts might need different logic depending on data source
        // For now, let's just count totals based on type

        for (TaskItem task : taskItemList) {
            switch (task.getType().toUpperCase()) {
                case "WORKFLOW":
                    totalWorkflow++;
                    break;
                case "REMAINDER":
                    totalRemainder++;
                    break;
                case "HABIT":
                    totalHabit++;
                    break;
            }
        }

        // Update the TextViews (assuming IDs are correct)
        // Display total counts for now. Completed counts need data source logic.
        totalWorkflowCount.setText(String.valueOf(totalWorkflow));
        totalRemainderCount.setText(String.valueOf(totalRemainder));
        // Assuming you might want to display habit count somewhere, or adjust UI
        // completedWorkflowCount.setText("0"); // Placeholder
        // completedRemainderCount.setText("0"); // Placeholder

        // Example: Update the 'Planned' card counts (adjust IDs if needed)
        // You might need separate TextViews if you want to show Habit counts
        // For simplicity, let's assume total_workflow_count and total_remainder_count are in the 'Planned' card

        // Placeholder for 'Completed' counts - requires status info from data source
        completedWorkflowCount.setText("0");
        completedRemainderCount.setText("0");
    }

    // Remove methods related to old TaskManager and Task actions
    /*
    @Override
    public void onTasksLoaded(List<Task> loadedTasks) {
        taskList.clear();
        taskList.addAll(loadedTasks);
        taskAdapter.notifyDataSetChanged();
        updateTaskCounts();
    }

    @Override
    public void onTaskUpdated() {
        loadTasks(); // Reload tasks after an update
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
    }
    */

    /*
    private void onTaskStatusChanged(Task task, String newStatus) {
        if (newStatus.equals("extend")) {
            showExtendDialog(task);
        } else {
            taskManager.updateTaskStatus(task.getTaskId(), newStatus);
        }
    }

    private void showExtendDialog(Task task) {
        String[] options = {"10 minutes", "20 minutes", "30 minutes", "Custom"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Extend Task Time")
                .setItems(options, (dialog, which) -> {
                    int extendMinutes;
                    switch (which) {
                        case 0: extendMinutes = 10; break;
                        case 1: extendMinutes = 20; break;
                        case 2: extendMinutes = 30; break;
                        case 3:
                            showCustomExtendDialog(task);
                            return;
                        default: extendMinutes = 10; break;
                    }
                    extendTaskTime(task, extendMinutes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCustomExtendDialog(Task task) {
        // Implement custom time input dialog
        // For simplicity, we'll just extend by 15 minutes
        extendTaskTime(task, 15);
    }

    private void extendTaskTime(Task task, int extendMinutes) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date endTime = timeFormat.parse(task.getEndTime());
            
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endTime);
            calendar.add(Calendar.MINUTE, extendMinutes);
            
            String newEndTime = timeFormat.format(calendar.getTime());
            taskManager.updateTaskTime(task.getTaskId(), task.getStartTime(), newEndTime);
            taskManager.updateTaskStatus(task.getTaskId(), "extended");
            
            // Check for conflicts and adjust other tasks if needed
            checkAndResolveTimeConflicts(task, newEndTime);
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error extending task time", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndResolveTimeConflicts(Task extendedTask, String newEndTime) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        try {
            Date extendedEndTime = timeFormat.parse(newEndTime);
            String extendedTaskDate = extendedTask.getDueDate();
            
            for (Task task : taskList) {
                // Skip the task being extended and tasks on different dates
                if (task.getTaskId().equals(extendedTask.getTaskId()) || 
                    !task.getDueDate().equals(extendedTaskDate) ||
                    task.isCompleted()) {
                    continue;
                }
                
                Date taskStartTime = timeFormat.parse(task.getStartTime());
                
                // If this task starts after our extended task ends, no conflict
                if (taskStartTime.after(extendedEndTime)) {
                    continue;
                }
                
                // If this task starts before our extended task ends, we have a conflict
                Date taskEndTime = timeFormat.parse(task.getEndTime());
                if (taskStartTime.before(extendedEndTime) || taskStartTime.equals(extendedEndTime)) {
                    // Move this task to start after the extended task ends
                    Calendar calendar = Calendar.getInstance();
                    
                    // Set new start time to extended task end time
                    calendar.setTime(extendedEndTime);
                    String newStartTime = timeFormat.format(calendar.getTime());
                    
                    // Calculate task duration
                    long taskDurationMs = taskEndTime.getTime() - taskStartTime.getTime();
                    
                    // Calculate new end time
                    calendar.add(Calendar.MILLISECOND, (int) taskDurationMs);
                    String newConflictingEndTime = timeFormat.format(calendar.getTime());
                    
                    // Update the conflicting task's time
                    taskManager.updateTaskTime(task.getTaskId(), newStartTime, newConflictingEndTime);
                    
                    // Recursively check for further conflicts caused by this shift
                    checkAndResolveTimeConflicts(task, newConflictingEndTime);
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error resolving time conflicts", Toast.LENGTH_SHORT).show();
        }
    }
    */
}