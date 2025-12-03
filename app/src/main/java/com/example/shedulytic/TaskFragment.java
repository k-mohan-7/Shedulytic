package com.example.shedulytic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.widget.ScrollView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TaskFragment extends Fragment implements TaskManager.TaskListener, TaskAdapter.TaskCheckListener {
    private static final String TAG = "TaskFragment";
    private TextView totalWorkflowCount;
    private TextView totalRemainderCount;
    private TextView completedWorkflowCount;
    private TextView completedRemainderCount;
    private Button addTaskButton;
    private RecyclerView taskRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList = new ArrayList<>();
    private TextView emptyStateTextView;
    private String todayDate;
    private SwipeRefreshLayout swipeRefresh; // Added for optimized scrolling

    // Make sure we have the correct taskCheckListener implementation 
    private final TaskAdapter.TaskCheckListener taskCheckListener = this;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        // Get today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        todayDate = dateFormat.format(new Date());
        Log.d(TAG, "Today's date: " + todayDate);

        totalWorkflowCount = view.findViewById(R.id.total_workflow_count);
        totalRemainderCount = view.findViewById(R.id.total_remainder_count);
        completedWorkflowCount = view.findViewById(R.id.completed_workflow_count);
        completedRemainderCount = view.findViewById(R.id.completed_remainder_count);
        addTaskButton = view.findViewById(R.id.add_task_button);
        taskRecyclerView = view.findViewById(R.id.task_recycler_view);
        emptyStateTextView = view.findViewById(R.id.empty_state_text);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        // Initialize SwipeRefreshLayout with optimizations
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.darkGreen);
            swipeRefresh.setProgressBackgroundColorSchemeResource(android.R.color.white);
            swipeRefresh.setSize(SwipeRefreshLayout.DEFAULT);
            swipeRefresh.setDistanceToTriggerSync(120);
            swipeRefresh.setSlingshotDistance(150);
            
            // Set refresh listener
            swipeRefresh.setOnRefreshListener(() -> {
                Log.i(TAG, "Swipe to refresh triggered - refreshing tasks");
                refreshAllDataFast();
            });
            
            // Optimize refresh behavior to prevent scroll conflicts
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
                if (taskRecyclerView != null && taskRecyclerView.canScrollVertically(-1)) {
                    return true;
                }
                if (child instanceof ScrollView) {
                    return child.getScrollY() > 0;
                }
                return false;
            });
        }

        // Set up optimized RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        taskRecyclerView.setLayoutManager(layoutManager);
        
        // Apply comprehensive scroll optimizations
        optimizeRecyclerViewInScrollView(taskRecyclerView, 20);
        
        // Enable layout manager optimizations
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(4);
        
        taskAdapter = new TaskAdapter(taskList, taskCheckListener, requireContext());
        taskRecyclerView.setAdapter(taskAdapter);

        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTaskActivity.class);
            startActivity(intent);
        });

        // Apply scrolling performance optimizations
        optimizeScrollingPerformance();

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
        
        // Re-apply scrolling optimizations after resume
        optimizeScrollingPerformance();
    }

    private void loadTasks() {
        // Ensure we have today's date
        if (todayDate == null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            todayDate = dateFormat.format(new Date());
        }

        // Show loading state only if we don't have tasks already
        if (taskList.isEmpty() && emptyStateTextView != null) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            emptyStateTextView.setText("Loading tasks...");
            
            // Add loading animation for better UX
            if (getActivity() != null) {
                ProgressBar progressBar = new ProgressBar(getActivity());
                progressBar.setId(View.generateViewId());
                progressBar.setIndeterminate(true);
                
                ViewGroup layout = (ViewGroup) emptyStateTextView.getParent();
                if (layout != null) {
                    layout.addView(progressBar);
                    
                    // Set a 5-second timeout to remove progress indicator if loading takes too long
                    new android.os.Handler().postDelayed(() -> {
                        if (isAdded() && layout != null) {
                            layout.removeView(progressBar);
                        }
                    }, 5000);
                }
            }
        }

        // Get user ID for API requests
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        
        // Initialize task manager with proper listener
        TaskManager taskManager = new TaskManager(requireContext(), this);
        
        // Log task loading attempt
        Log.d(TAG, "Attempting to load tasks for date: " + todayDate + " with user ID: " + userId);
        
        // Always use parallel loading for best performance
        taskManager.loadTasks(todayDate);
        
        // As a fallback, also try to load from TodayActivitiesManager which has additional data sources
        if (getActivity() != null) {
            TodayActivitiesManager activitiesManager = new TodayActivitiesManager(
                getActivity(),
                userId,
                new TodayActivitiesManager.TodayActivitiesListener() {
    @Override
                    public void onActivitiesLoaded(List<TaskItem> activities) {
                        if (activities != null && !activities.isEmpty() && isAdded()) {
                            Log.d(TAG, "Loaded " + activities.size() + " tasks from TodayActivitiesManager");
                            
                            // Convert TaskItems to Tasks
                            List<Task> tasks = new ArrayList<>();
                            for (TaskItem item : activities) {
                                Task task = new Task(
                                    item.getId(),  // Using the String taskId constructor
                                    userId,
                                    item.getType(),
                                    item.getTitle(),
                                    item.getDescription(),
                                    item.getStartTime(),
                                    item.getEndTime(),
                                    todayDate,
                                    item.getStatus(),
                                    item.getRepeatFrequency(),
                                    item.getPriority()
                                );
                                tasks.add(task);
                            }
                            
                            // Update UI
                            if (!tasks.isEmpty()) {
                                requireActivity().runOnUiThread(() -> {
                                    onTasksLoaded(tasks);
                                });
                            }
                        }
                    }

                    @Override
                    public void onUserProfileLoaded(String name, int streakCount, String avatarUrl) {
                        // Not needed for this fragment
                    }

                    @Override
                    public void onStreakDataLoaded(Map<String, Boolean> streakData) {
                        // Not needed for this fragment
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "TodayActivitiesManager error: " + message);
                    }
                }
            );
            
            // Load activities quickly
            activitiesManager.loadTodayActivitiesQuickly();
        }
    }
    
    /**
     * Check if we have a stable connection for optimal loading
     */
    private boolean hasStableConnection() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            if (activeNetwork.getType() == android.net.ConnectivityManager.TYPE_WIFI) {
                // WiFi connection is usually stable
                return true;
            } else if (activeNetwork.getType() == android.net.ConnectivityManager.TYPE_MOBILE) {
                // Check mobile signal strength
                android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager)
                        requireContext().getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    // Only use parallel loading if we have decent signal
                    return true;
            }
        }
        }
        return false;
    }

    @Override
    public void onTasksLoaded(List<Task> tasks) {
        // Only process if we're still attached to the activity
        if (!isAdded() || getContext() == null) return;
        
        Log.d(TAG, "Tasks loaded: " + tasks.size() + " tasks");
        
        // Hide any loading indicators
        if (emptyStateTextView != null && emptyStateTextView.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) emptyStateTextView.getParent();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof ProgressBar) {
                    parent.removeView(child);
                    break;
                }
            }
        }
        
        // Filter tasks to only include today's tasks
        List<Task> todayTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (todayDate.equals(task.getDueDate())) {
                // Make sure the task type is correct
                String type = task.getType() != null ? task.getType().toLowerCase() : "";
                if (!type.equals("workflow") && !type.equals("remainder")) {
                    task.setTaskType("remainder"); // Default to remainder if type is invalid
                    Log.d(TAG, "Fixed task type for: " + task.getTitle());
                }
                todayTasks.add(task);
                Log.d(TAG, "Adding task: " + task.getTitle() + ", Type: " + task.getType());
            }
        }
        
        // Compare current tasks with new tasks to avoid UI flicker
        if (!todayTasks.isEmpty()) {
            boolean hasChanges = taskList.size() != todayTasks.size();
            if (!hasChanges) {
                // Check if any tasks are different
                Set<String> existingIds = new HashSet<>();
                for (Task task : taskList) {
                    existingIds.add(task.getTaskId());
                }
                
                for (Task task : todayTasks) {
                    if (!existingIds.contains(task.getTaskId())) {
                        hasChanges = true;
                        break;
                    }
                }
            }
            
            // Only update if there are actually changes
            if (hasChanges) {
        taskList.clear();
        taskList.addAll(todayTasks);
        taskAdapter.notifyDataSetChanged();
        updateTaskCounts();
            }
        }

        // Show empty state message when no tasks are available
        if (emptyStateTextView != null) {
        if (taskList.isEmpty()) {
                emptyStateTextView.setVisibility(View.VISIBLE);
                emptyStateTextView.setText("No tasks available for today");
        } else {
                emptyStateTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onTaskAdded(Task task) {
        // Only process if we're still attached to the activity
        if (!isAdded() || getContext() == null) return;
        
        Log.d(TAG, "Task added: " + task.getTitle() + ", Due date: " + task.getDueDate() + ", Today: " + todayDate);
        
        // Only add task if it's for today
        if (todayDate.equals(task.getDueDate())) {
            // Make sure task type is valid
            String type = task.getType() != null ? task.getType().toLowerCase() : "";
            if (!type.equals("workflow") && !type.equals("remainder")) {
                task.setTaskType("remainder");
                Log.d(TAG, "Fixed task type for new task: " + task.getTitle());
            }
            
            // Check if task already exists (by ID)
            for (int i = 0; i < taskList.size(); i++) {
                if (taskList.get(i).getTaskId().equals(task.getTaskId())) {
                    // Update existing task
                    taskList.set(i, task);
                    taskAdapter.notifyItemChanged(i);
                    updateTaskCounts();
                    Log.d(TAG, "Updated existing task: " + task.getTitle());
                    return;
                }
            }
            
            // Add new task
            taskList.add(task);
            taskAdapter.notifyItemInserted(taskList.size() - 1);
            updateTaskCounts();
            
            // Hide empty state
            if (emptyStateTextView != null) {
                emptyStateTextView.setVisibility(View.GONE);
            }
            if (taskRecyclerView != null) {
                taskRecyclerView.setVisibility(View.VISIBLE);
            }
            
            // Show a toast for user feedback
            Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show();
            
            Log.d(TAG, "Added new task: " + task.getTitle());
        }
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        String newStatus = isChecked ? "completed" : "pending";
        
        // Update local task status first for immediate UI response
        task.setStatus(newStatus);
        
        // Create TaskManager instance for network update
        TaskManager taskManager = new TaskManager(requireContext(), new TaskManager.TaskListener() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                // Not needed for this callback
            }
            
            @Override
            public void onTaskAdded(Task addedTask) {
                // Not needed for this callback
            }
            
            @Override
            public void onTaskUpdated(Task updatedTask) {
                // Find the task in our list and update only its status
                // This preserves all other properties and ensures UI consistency
                for (int i = 0; i < taskList.size(); i++) {
                    if (taskList.get(i).getTaskId().equals(updatedTask.getTaskId())) {
                        // Update only the status of the existing task object
                        taskList.get(i).setStatus(updatedTask.getStatus());
                        
                        // Notify adapter about the specific item change
                        taskAdapter.notifyItemChanged(i);
                        Log.d(TAG, "Updated task " + updatedTask.getTaskId() + " status to: " + updatedTask.getStatus());
                        break;
                    }
                }
                
                // Update task counts
                updateTaskCounts();
            }
            
            @Override
            public void onTaskDeleted(String taskId) {
                // Not needed for this callback
            }
            
            @Override
            public void onHabitStreakUpdated(String taskId, int newStreak) {
                // Not needed for this callback
            }
            
            @Override
            public void onError(String message) {
                Log.e(TAG, "Error updating task status: " + message);
                // Revert UI change on error
                task.setStatus(isChecked ? "pending" : "completed");
                taskAdapter.notifyDataSetChanged();
            }
        });
        
        // Send update to server using completion system
        if (isChecked) {
            taskManager.updateTaskCompletion(task.getTaskId(), true);
        } else {
            taskManager.updateTaskCompletion(task.getTaskId(), false);
        }
        
        // Update the task counts on UI without waiting for server response
        updateTaskCounts();
    }

    @Override
    public void onTaskUpdated(Task task) {
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getTaskId().equals(task.getTaskId())) {
                taskList.set(i, task);
                taskAdapter.notifyItemChanged(i);
                break;
            }
        }
        updateTaskCounts();
    }

    @Override
    public void onTaskDeleted(String taskId) {
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getTaskId().equals(taskId)) {
                taskList.remove(i);
                taskAdapter.notifyItemRemoved(i);
                break;
            }
        }
        updateTaskCounts();
        
        // Show empty state if no tasks left
        if (taskList.isEmpty() && emptyStateTextView != null && taskRecyclerView != null) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            taskRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onHabitStreakUpdated(String taskId, int newStreak) {
        // Only process if we're still attached to the activity
        if (!isAdded() || getContext() == null) return;
        
        // Update the streak in the task object
        for (Task task : taskList) {
            if (task.getTaskId().equals(taskId)) {
                task.setCurrentStreak(newStreak);
                Log.d(TAG, "Updated streak for task " + task.getTitle() + " to " + newStreak);
                break;
            }
        }
        
        // Update the UI
        taskAdapter.notifyDataSetChanged();
    }

    @Override
    public void onError(String message) {
        // Only handle error if we're still attached to activity
        if (!isAdded() || getContext() == null) return;
        
        // Hide any loading indicators
        if (emptyStateTextView != null && emptyStateTextView.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) emptyStateTextView.getParent();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof ProgressBar) {
                    parent.removeView(child);
                    break;
                }
            }
        }
        
        // Only show error if we don't have any tasks to display
        if (taskList.isEmpty()) {
            if (emptyStateTextView != null) {
                emptyStateTextView.setVisibility(View.VISIBLE);
                
                // Show a more user-friendly message than the technical error
                if (message.contains("Network") || message.contains("connection") || 
                        message.contains("internet") || message.contains("timeout")) {
                    emptyStateTextView.setText("Network issue. Using offline tasks.");
                } else {
                    emptyStateTextView.setText("Couldn't load tasks. Using cached data.");
                }
            }
            
            // Try to load from local cache as fallback
            TaskManager taskManager = new TaskManager(requireContext(), this);
            List<Task> localTasks = taskManager.loadLocalTasks(todayDate);
            if (!localTasks.isEmpty()) {
                taskList.clear();
                taskList.addAll(localTasks);
                taskAdapter.notifyDataSetChanged();
                updateTaskCounts();
                
                // Adjust message to indicate we're using offline data
                if (emptyStateTextView != null) {
                    emptyStateTextView.setVisibility(View.GONE);
        }
            }
        }
        
        // Log the actual error for debugging
        Log.e(TAG, "Error loading tasks: " + message);
    }

    private void updateTaskCounts() {
        int totalWorkflow = 0;
        int totalRemainder = 0;
        int completedWorkflow = 0;
        int completedRemainder = 0;

        for (Task task : taskList) {
            String type = task.getType() != null ? task.getType().toLowerCase() : "";
            
            switch (type) {
                case "workflow":
                    totalWorkflow++;
                    if (task.isCompleted()) completedWorkflow++;
                    break;
                case "remainder":
                    totalRemainder++;
                    if (task.isCompleted()) completedRemainder++;
                    break;
                default:
                    // Default to remainder
                    totalRemainder++;
                    if (task.isCompleted()) completedRemainder++;
                    break;
            }
        }

        if (totalWorkflowCount != null) totalWorkflowCount.setText(String.valueOf(totalWorkflow));
        if (totalRemainderCount != null) totalRemainderCount.setText(String.valueOf(totalRemainder));
        if (completedWorkflowCount != null) completedWorkflowCount.setText(String.valueOf(completedWorkflow));
        if (completedRemainderCount != null) completedRemainderCount.setText(String.valueOf(completedRemainder));
    }

    /**
     * Fast refresh method with minimal UI blocking for better performance
     * Used when user triggers swipe-to-refresh
     */
    private void refreshAllDataFast() {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Cannot refresh data fast: Fragment not attached");
            return;
        }
        
        Log.d(TAG, "Fast refresh triggered - optimized for performance");
        
        try {
            // Quick task refresh without heavy operations
            loadTasks();
            
            // Stop refresh animation quickly
            if (swipeRefresh != null) {
                swipeRefresh.postDelayed(() -> {
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }
                }, 500); // Short delay for better UX
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in fast refresh: " + e.getMessage(), e);
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
        }
    }

    /**
     * Optimizes scrolling performance for nested RecyclerViews and ScrollView
     * Fixes scroll conflicts and improves user experience
     */
    private void optimizeScrollingPerformance() {
        if (getView() == null) return;
        
        // Find the SwipeRefreshLayout and its ScrollView child
        SwipeRefreshLayout swipeRefreshLayout = getView().findViewById(R.id.swipeRefresh);
        if (swipeRefreshLayout != null) {
            // Get the ScrollView child of SwipeRefreshLayout
            if (swipeRefreshLayout.getChildCount() > 0) {
                View scrollChild = swipeRefreshLayout.getChildAt(0);
                if (scrollChild instanceof ScrollView) {
                    ScrollView scrollView = (ScrollView) scrollChild;
                    
                    // Optimize ScrollView performance
                    scrollView.setVerticalScrollBarEnabled(false);
                    scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                    scrollView.setFillViewport(true);
                    
                    // Enable smooth scrolling
                    scrollView.setSmoothScrollingEnabled(true);
                    
                    // Optimize nested scrolling behavior to prevent conflicts
                    scrollView.setNestedScrollingEnabled(true);
                    
                    // Set scroll sensitivity for better touch handling
                    scrollView.setScrollbarFadingEnabled(false);
                }
            }
        }
        
        // Optimize the main RecyclerView
        optimizeRecyclerViewInScrollView(taskRecyclerView, 20);
        
        Log.d(TAG, "Scrolling performance optimizations applied successfully");
    }
    
    /**
     * Helper method to optimize individual RecyclerViews within ScrollView
     */
    private void optimizeRecyclerViewInScrollView(RecyclerView recyclerView, int cacheSize) {
        if (recyclerView == null) return;
        
        // Disable nested scrolling to prevent conflicts with parent ScrollView
        recyclerView.setNestedScrollingEnabled(false);
        
        // Performance optimizations
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(cacheSize);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setVerticalScrollBarEnabled(false);
        
        // Enable drawing cache for smoother scrolling
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);
        
        // Get the layout manager and optimize it
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            linearLayoutManager.setItemPrefetchEnabled(true);
            linearLayoutManager.setInitialPrefetchItemCount(Math.min(4, cacheSize / 2));
        }
        
        // Prevent focus stealing from parent ScrollView
        recyclerView.setFocusable(false);
        recyclerView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    /**
     * Public method to refresh fragment data
     * Called from MainNavigationActivity when data needs to be updated
     */
    public void refreshData() {
        refreshAllDataFast();
    }
}