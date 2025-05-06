package com.example.shedulytic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Import the TaskItem class
import com.example.shedulytic.TaskItem;

public class HomeFragment extends Fragment implements TaskManager.TaskListener {
    private RecyclerView timelineRecycler;
    private TaskManager taskManager;
    private List<TaskItem> todayTasks = new ArrayList<>();
    private TextView userNameTextView;
    private String userId;
    private String username;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize views
        timelineRecycler = view.findViewById(R.id.timelineRecycler);
        timelineRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        userNameTextView = view.findViewById(R.id.textView_user_name);
        
        // Get user information from SharedPreferences
        if (getActivity() != null) {
            android.content.SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            userId = prefs.getString("user_id", null);
            username = prefs.getString("username", "User");
            
            // Set the welcome message with the user's name
            if (username != null && !username.isEmpty()) {
                userNameTextView.setText(username);
            }
        }
        
        // Initialize TaskManager
        taskManager = new TaskManager(getContext(), this);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadTasks();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks(); // Refresh tasks when returning to this fragment
    }

    private void loadTasks() {
        taskManager.loadTasks();
    }
    
    private void displayTasksInTimeline(List<Task> tasks) {
        // Get today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        // Clear previous tasks
        todayTasks.clear();
        
        // Filter tasks for today and convert Task objects to TaskItem objects
        for (Task task : tasks) {
            if (today.equals(task.getDueDate())) {
                // Convert Task to TaskItem
                String timeText = task.getStartTime();
                if (task.getEndTime() != null && !task.getEndTime().isEmpty()) {
                    timeText += " - " + task.getEndTime();
                }
                
                // Calculate duration if not available
                String duration = calculateDuration(task.getStartTime(), task.getEndTime());
                
                // Create a new TaskItem from the Task
                TaskItem taskItem = new TaskItem(
                    task.getTitle(),
                    timeText,
                    duration,
                    task.getTaskType()
                );
                todayTasks.add(taskItem);
            }
        }
        
        // Create and set adapter
        if (todayTasks.isEmpty()) {
            // Show empty state with a TaskItem instead of Task
            TaskItem emptyTaskItem = new TaskItem(
                "No tasks scheduled for today",
                "",
                "",
                "empty"
            );
            todayTasks.add(emptyTaskItem);
        }
        
        // Create and set adapter with TaskItem list
        // Use the constructor that takes TaskItem objects
        TaskAdapter adapter = new TaskAdapter(getContext(), todayTasks);
        timelineRecycler.setAdapter(adapter);
    }
    
    // This method is no longer needed as we're using RecyclerView with an adapter
    // The formatting logic can be moved to the adapter
    
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
    
    // TaskManager.TaskListener implementation
    @Override
    public void onTasksLoaded(List<Task> tasks) {
        displayTasksInTimeline(tasks);
    }

    @Override
    public void onTaskAdded(Task task) {
        loadTasks(); // Reload all tasks to refresh timeline
    }

    @Override
    public void onTaskUpdated(Task task) {
        loadTasks(); // Reload all tasks to refresh timeline
    }

    @Override
    public void onTaskDeleted(String taskId) {
        loadTasks(); // Reload all tasks to refresh timeline
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}