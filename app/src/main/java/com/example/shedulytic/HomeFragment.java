package com.example.shedulytic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar; // Added import for ProgressBar
import android.widget.ScrollView; // Added import for ScrollView
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // For loading colors
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import com.example.shedulytic.VolleyNetworkManager;
import com.example.shedulytic.service.HabitManagerService;
import com.example.shedulytic.model.Habit;
import com.example.shedulytic.adapter.HabitAdapter;
// Remove Volley imports if NetworkUtils handles it internally, or keep if HomeFragment makes direct calls.
// Assuming NetworkUtils is your primary way to make calls.
// import com.android.volley.Request;
// import com.android.volley.RequestQueue;
// import com.android.volley.toolbox.JsonObjectRequest;
// import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

// Assuming these manager and model classes exist in your project
// import com.example.shedulytic.managers.TodayActivitiesManager; (Example package)
// import com.example.shedulytic.managers.ProfileManager;
// import com.example.shedulytic.managers.HabitManager;
// import com.example.shedulytic.managers.TaskManager;
// import com.example.shedulytic.models.TaskItem;
// import com.example.shedulytic.models.Task;
// import com.example.shedulytic.utils.IpV4Connection; (Example package)
// import com.example.shedulytic.utils.NetworkUtils;

public class HomeFragment extends Fragment implements ProfileManager.ProfileLoadListener, TodayActivitiesManager.TodayActivitiesListener, TaskManager.TaskListener, HabitManagerService.HabitListener, HabitAdapter.HabitInteractionListener {
    private static final String TAG = "HomeFragment";
    private RecyclerView myDayTimelineRecycler; // Timeline in MyDayMap
    private RecyclerView myDayRecyclerView; // Added for MyDayMap
    private View myDayEmptyState; // Added for MyDayMap empty state
    private TextView myDayTimelineEmptyText; // Added for timeline empty state
    private TodayActivitiesManager activitiesManager;
    private List<TaskItem> todayActivities = new ArrayList<>();
    private List<Task> myDayTasks = new ArrayList<>(); // Added for MyDayMap
    
    // Habit-related fields
    private RecyclerView habitRecyclerView;
    private LinearLayout habitEmptyState;
    private TextView emptyHabitsText;
    private HabitManagerService habitManager;
    private HabitAdapter habitAdapter;
    private List<Habit> habitList = new ArrayList<>();
    
    private TextView userNameTextView;
    private TextView streakCountTextView; // This is textView_days_count from XML
    private ImageView fireIconStreakImageView; // Added for the fire icon
    private TextView topStreakCountTextView; // For top bar streak chip
    private TextView xpCountTextView; // For XP/coins display
    private TextView currentDayTextView; // For header day display
    private TextView currentDateTextView; // For header date display
    private ImageView profileImageView;
    private SwipeRefreshLayout swipeRefresh; // Added for explicit null check
    
    // Stats TextViews for real-time updates
    private TextView statTasksCompletedTextView;
    private TextView statWorkflowsCompletedTextView;
    private TextView statHabitsCompletedTextView;
    private int totalTasks = 0;
    private int completedTasks = 0;
    private int totalWorkflows = 0;
    private int completedWorkflows = 0;
    private int totalHabits = 0;
    private int completedHabits = 0;
    
    private String userId;
    private String username; // From SharedPreferences
    private Map<String, Boolean> weekStreakData = new HashMap<>();
    // private RequestQueue requestQueue; // Only if HomeFragment makes direct Volley calls

    private ProfileManager profileManager;

    // Calendar views
    private TextView[] weekDayTexts = new TextView[7];
    private TextView[] dayTextViews = new TextView[7];
    private ImageView[] streakImageViews = new ImageView[7];


    private TaskAdapter.TaskCheckListener taskCheckListener = new TaskAdapter.TaskCheckListener() {
        @Override
        public void onTaskChecked(Task task, boolean isChecked) {
            try {
                if (getContext() == null || !isAdded()) return; // Prevent action if fragment not attached

                Log.d(TAG, "Task " + task.getTaskId() + " checked: " + isChecked);
                task.setCompleted(isChecked);
                String newStatus = isChecked ? "completed" : "pending";
                
                // Make sure we're preserving the original task type
                String originalType = task.getType();
                
                // Assuming TaskManager is properly initialized and context is valid
                TaskManager taskManager = new TaskManager(requireContext(), new TaskManager.TaskListener() {
                    @Override
                    public void onTasksLoaded(List<Task> tasks) {}
                    @Override
                    public void onTaskAdded(Task task) {
                        Log.d(TAG, "TaskListener: onTaskAdded - refreshing activities");
                        if (activitiesManager != null && userId != null) activitiesManager.loadTodayActivities();
                    }
                    @Override
                    public void onTaskUpdated(Task updatedTask) {
                        Log.d(TAG, "TaskListener: onTaskUpdated - refreshing activities");
                        if (activitiesManager != null && userId != null) activitiesManager.loadTodayActivities();
                        
                        // Sort completed tasks to bottom of list
                        if (myDayTimelineRecycler != null && myDayTimelineRecycler.getAdapter() instanceof TaskAdapter) {
                            TaskAdapter adapter = (TaskAdapter) myDayTimelineRecycler.getAdapter();
                            adapter.sortTasksByCompletionStatus();
                        }
                    }
                    @Override
                    public void onTaskDeleted(String taskId) {
                        Log.d(TAG, "TaskListener: onTaskDeleted - refreshing activities");
                        // Local removal is good for immediate UI feedback, then reload for sync.
                        for (int i = 0; i < todayActivities.size(); i++) {
                            if (todayActivities.get(i).getId().equals(taskId)) {
                                todayActivities.remove(i);
                                if (myDayTimelineRecycler != null && myDayTimelineRecycler.getAdapter() != null) {
                                    myDayTimelineRecycler.getAdapter().notifyItemRemoved(i);
                                    // Or notifyDataSetChanged() if positions are complex
                                }
                                break;
                            }
                        }
                        if (activitiesManager != null && userId != null) activitiesManager.loadTodayActivities();
                    }
                    @Override
                    public void onHabitStreakUpdated(String taskId, int newStreak) {
                        Log.d(TAG, "TaskListener: onHabitStreakUpdated for task " + taskId + " New Streak: " + newStreak);
                        updateTaskStreakInList(taskId, newStreak);
                    }
                    @Override
                    public void onError(String message) {
                        if (getContext() != null && isAdded()) {
                            Toast.makeText(requireContext(), "Task Update Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                        Log.e(TAG, "TaskListener Error: " + message);
                    }
                });
                
                // Preserve the original task type when updating completion
                task.setTaskType(originalType);
                taskManager.updateTaskCompletion(task.getTaskId(), newStatus.equals("completed"));
                
                // Update UI immediately for better user experience
                for (TaskItem item : todayActivities) {
                    if (item.getId() != null && item.getId().equals(task.getTaskId())) {
                        item.setStatus(newStatus);
                        break;
                    }
                }
                
                // Update stats immediately for real-time feedback
                calculateAndUpdateStats();
                
                // Sort the tasks if the adapter is available
                if (myDayTimelineRecycler != null && myDayTimelineRecycler.getAdapter() instanceof TaskAdapter) {
                    TaskAdapter adapter = (TaskAdapter) myDayTimelineRecycler.getAdapter();
                    adapter.sortTasksByCompletionStatus();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing task check: " + e.getMessage());
                Toast.makeText(requireContext(), "Error updating task status", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void updateTaskStreakInList(String taskId, int newStreak) {
        for (TaskItem task : todayActivities) {
            if (task.getId() != null && task.getId().equals(taskId)) { // task.getId() was task.getTaskId()
                task.setStreak(newStreak);
                if (myDayTimelineRecycler != null && myDayTimelineRecycler.getAdapter() != null) {
                    // Be careful with notifyDataSetChanged(), find specific item if possible
                    myDayTimelineRecycler.getAdapter().notifyDataSetChanged();
                }
                break;
            }
        }
    }

    // Add missing updateHabitProgress method - Remove this as we no longer track habits
    // private void updateHabitProgress() method removed

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_new, container, false);

        // requestQueue = Volley.newRequestQueue(requireContext()); // If making direct Volley calls

        // Initialize Timeline RecyclerView (replaces MyDayMap in new layout)
        RecyclerView timelineRecycler = view.findViewById(R.id.recycler_timeline);
        View emptyTimeline = view.findViewById(R.id.empty_timeline);
        
        if (timelineRecycler != null) {
            LinearLayoutManager timelineLayoutManager = new LinearLayoutManager(getContext());
            timelineRecycler.setLayoutManager(timelineLayoutManager);
            timelineRecycler.setNestedScrollingEnabled(false);
            timelineRecycler.setHasFixedSize(true);
            myDayTimelineRecycler = timelineRecycler; // Reuse existing field
            
            // Apply performance optimizations for timeline RecyclerView
            myDayTimelineRecycler.setItemViewCacheSize(20);
            myDayTimelineRecycler.setVerticalScrollBarEnabled(false);
            myDayTimelineRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        userNameTextView = view.findViewById(R.id.textView_user_name);
        streakCountTextView = view.findViewById(R.id.textView_days_count); // Main streak display
        fireIconStreakImageView = view.findViewById(R.id.fire_icon_streak); // Initialize here
        topStreakCountTextView = view.findViewById(R.id.text_streak_count); // Top bar streak chip
        xpCountTextView = view.findViewById(R.id.text_xp_count); // XP/coins display
        currentDayTextView = view.findViewById(R.id.text_current_day); // Header day
        currentDateTextView = view.findViewById(R.id.text_current_date); // Header date
        profileImageView = view.findViewById(R.id.profileImage);
        swipeRefresh = view.findViewById(R.id.swipeRefresh); // Initialize swipeRefresh
        
        // Initialize stats TextViews for real-time updates
        statTasksCompletedTextView = view.findViewById(R.id.stat_tasks_completed);
        statWorkflowsCompletedTextView = view.findViewById(R.id.stat_workflows_completed);
        statHabitsCompletedTextView = view.findViewById(R.id.stat_habits_completed);
        
        // Update header with current date
        updateHeaderDate();
        
        // Initialize menu components
        setupMenuComponents(view);
        
        // Initialize add task overlay components
        setupAddTaskOverlay(view);

        // Initialize habit views
        habitRecyclerView = view.findViewById(R.id.habit_recycler_view);
        habitEmptyState = view.findViewById(R.id.habit_empty_state);
        emptyHabitsText = view.findViewById(R.id.empty_habits_text);

        // Setup habit RecyclerView with enhanced performance optimizations
        if (habitRecyclerView != null) {
            LinearLayoutManager habitLayoutManager = new LinearLayoutManager(getContext());
            habitRecyclerView.setLayoutManager(habitLayoutManager);
            
            // Critical optimizations for habits RecyclerView in ScrollView
            habitRecyclerView.setNestedScrollingEnabled(false); // Better performance in ScrollView
            habitRecyclerView.setHasFixedSize(true);
            habitRecyclerView.setItemViewCacheSize(10);
            habitRecyclerView.setVerticalScrollBarEnabled(false);
            habitRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            
            // Enable prefetching for smoother scrolling
            habitLayoutManager.setItemPrefetchEnabled(true);
            habitLayoutManager.setInitialPrefetchItemCount(2);
            
            habitAdapter = new HabitAdapter(habitList, this, getContext());
            habitRecyclerView.setAdapter(habitAdapter);
        }

        // Initialize HabitManagerService
        habitManager = HabitManagerService.getInstance(requireContext());
        habitManager.setListener(this);


        initializeCalendarViews(view);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        username = prefs.getString("username", "User (Default)"); // Provide a default
        int currentStreak = prefs.getInt("streak_count", 0);
        int xpCoins = prefs.getInt("xp_coins", 0);

        Log.i(TAG, "User ID from Prefs: " + userId + ", Username: " + username + ", Initial Streak: " + currentStreak);

        if (userNameTextView != null) userNameTextView.setText(username + " 👋");
        if (streakCountTextView != null) streakCountTextView.setText(String.valueOf(currentStreak));
        if (topStreakCountTextView != null) topStreakCountTextView.setText(String.valueOf(currentStreak));
        if (xpCountTextView != null) xpCountTextView.setText(String.valueOf(xpCoins));


        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "USER ID IS NULL OR EMPTY. Cannot initialize managers or load user-specific data.");
            if (isAdded()) { // Check if fragment is added to an activity
                Toast.makeText(getContext(), "User session error. Please log in again.", Toast.LENGTH_LONG).show();
            }
            // Optionally, navigate to login screen or disable refresh/data loading
        } else {
            Log.d(TAG, "User ID found: " + userId + ". Initializing managers.");
            activitiesManager = new TodayActivitiesManager(requireContext(), userId, this);
            profileManager = new ProfileManager(requireContext(), this); // Assuming ProfileManager needs context and listener
        }

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.darkGreen); // Ensure this color exists
            // Optimize refresh animation for faster experience and better performance
            swipeRefresh.setProgressBackgroundColorSchemeResource(android.R.color.white);
            swipeRefresh.setSize(SwipeRefreshLayout.DEFAULT);
            swipeRefresh.setDistanceToTriggerSync(120); // Reduced distance for faster trigger
            swipeRefresh.setSlingshotDistance(150); // Faster animation
            
            // Optimize refresh behavior to prevent scroll conflicts
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
                // Check if any nested RecyclerView can scroll up
                if (myDayTimelineRecycler != null && myDayTimelineRecycler.canScrollVertically(-1)) {
                    return true;
                }
                if (habitRecyclerView != null && habitRecyclerView.canScrollVertically(-1)) {
                    return true;
                }
                // Check if the main ScrollView can scroll up
                if (child instanceof ScrollView) {
                    return child.getScrollY() > 0;
                }
                return false;
            });
            
            swipeRefresh.setOnRefreshListener(() -> {
                Log.i(TAG, "Swipe to refresh triggered - fast refresh mode.");
                // Use faster refresh with minimal UI blocking
                refreshAllDataFast();
            });
        } else {
            Log.e(TAG, "swipeRefresh is null!");
        }
        
        // Apply scrolling performance optimizations
        optimizeScrollingPerformance();
        
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // Setup swipe refresh
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> refreshAllData(true));
        }
        
        // Refresh data
        refreshAllData(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Refresh data
        refreshAllData(false);
        
        // Re-apply scrolling optimizations after resume
        optimizeScrollingPerformance();
    }

    /**
     * Update user login streak when app is opened - removed as habits are no longer tracked
     */
    // private void updateUserLoginStreak() method removed
    
    /**
     * Ensure calendar is properly initialized and updated
     */
    private void ensureStreakCalendarInitialized() {
        if (isAdded() && getView() != null) {
            updateCalendarWithCurrentWeek();
        }
    }

    /**
     * Refresh all data displayed in the fragment
     */
    private void refreshAllData(boolean forceRefresh) {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Cannot refresh data: Fragment not attached");
            return;
        }
        
        Log.d(TAG, "Refreshing data for HomeFragment with forceRefresh=" + forceRefresh);
        
        // Show loading state only for manual refreshes
        if (swipeRefresh != null && forceRefresh) {
            swipeRefresh.setRefreshing(true);
        }
        
        // Load data in sequence with optimizations
        try {
            // Clear task cache in a background thread to avoid UI blocking
            new Thread(() -> {
                try {
                    if (getContext() != null) {
                        TaskManager tempManager = new TaskManager(getContext(), new TaskManager.TaskListener() {
                            @Override public void onTasksLoaded(List<Task> tasks) {}
                            @Override public void onTaskAdded(Task task) {}
                            @Override public void onTaskUpdated(Task updatedTask) {}
                            @Override public void onTaskDeleted(String taskId) {}
                            @Override public void onHabitStreakUpdated(String taskId, int newStreak) {}
                            @Override public void onError(String message) {}
                        });
                        tempManager.clearDeletedTasks();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing deleted tasks in background: " + e.getMessage());
                }
            }).start();
            
            // Refresh today's activities with optimized loading
            if (activitiesManager != null) {
                // Use a faster direct approach for better performance
                activitiesManager.loadTodayActivitiesQuickly();
            } else {
                Log.e(TAG, "Activities manager is null, cannot load today's activities");
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }
            
            // Load habits for today
            if (habitManager != null) {
                habitManager.loadHabits();
            } else {
                Log.e(TAG, "Habit manager is null, cannot load habits");
            }
            
            // Always load streak data (not just on force refresh) to ensure UI is updated
            // This ensures the streak chip shows correct value when fragment loads
            if (activitiesManager != null) {
                activitiesManager.loadStreakData();
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::loadUserStreakData);
                }
            }
            
            // Refresh profile data only on manual refresh
            if (forceRefresh) {
                if (profileManager != null) {
                    profileManager.loadUserProfile();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing data: " + e.getMessage(), e);
            
            // Hide loading state
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            
            // Show error toast if fragment is attached
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserStreakData() {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot load user streak data: User ID is missing.");
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            useDefaultStreakData(); // Show default or cached calendar
            return;
        }

        Log.d(TAG, "Loading user streak data with ID: " + userId);

        // Calculate streak data range (last 7 days)
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String endDate = dateFormat.format(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, -6); // Get last 7 days including today
        String startDate = dateFormat.format(cal.getTime());

        // Get endpoint from VolleyNetworkManager instead of IpV4Connection
        VolleyNetworkManager networkManager = VolleyNetworkManager.getInstance(requireContext());
        String endpoint = networkManager.getUserStreakUrl(userId, startDate, endDate);
        Log.i(TAG, "Loading user streak data with endpoint: " + endpoint);

        if (endpoint.isEmpty()){
            Log.e(TAG, "Generated streak endpoint is empty. Aborting.");
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            useDefaultStreakData();
            return;
        }

        // Use VolleyNetworkManager directly instead of NetworkUtils
        networkManager.makeGetRequest(endpoint, new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    Log.d(TAG, "Successfully received streak data: " + response.toString().substring(0, Math.min(200, response.toString().length())));
                    
                    // Extract streak count
                    int overallStreakCount = 0;
                    if (response.has("streak_count")) {
                        overallStreakCount = response.getInt("streak_count");
                        updateStreakCountUI(overallStreakCount);
                    } else if (response.has("data") && response.getJSONObject("data").has("streak_count")) {
                        overallStreakCount = response.getJSONObject("data").getInt("streak_count");
                        updateStreakCountUI(overallStreakCount);
                    } else {
                        Log.w(TAG, "No streak_count in response");
                        // Try to get from SharedPreferences
                        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        overallStreakCount = prefs.getInt("streak_count", 0);
                        updateStreakCountUI(overallStreakCount);
                    }

                    // Process streak data for calendar
                    processStreakDataResponse(response, startDate, endDate, overallStreakCount);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing streak data: " + e.getMessage(), e);
                    useDefaultStreakData();
                } finally {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading streak data: " + errorMessage);
                // Try to get streak count from SharedPreferences
                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                int streakCount = prefs.getInt("streak_count", 0);
                updateStreakCountUI(streakCount);
                
                // Use default streak data for calendar
                useDefaultStreakData();
                
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });
    }

    /**
     * Process streak data from server response
     */
    private void processStreakDataResponse(JSONObject response, String startDate, String endDate, int streakCount) {
        try {
            // Look for streak data in various formats
            JSONArray streakDataArray = null;
            
            if (response.has("streak_data")) {
                streakDataArray = response.getJSONArray("streak_data");
                Log.d(TAG, "Found streak_data array with " + streakDataArray.length() + " items");
            } else if (response.has("data") && response.getJSONObject("data").has("streak_data")) {
                streakDataArray = response.getJSONObject("data").getJSONArray("streak_data");
                Log.d(TAG, "Found streak_data in data object with " + streakDataArray.length() + " items");
            } else if (response.has("active_days")) {
                streakDataArray = response.getJSONArray("active_days");
                Log.d(TAG, "Found active_days array with " + streakDataArray.length() + " items");
            }
            
            Map<String, Boolean> newWeekStreakData = new HashMap<>();
            
            // Process streak data array if found
            if (streakDataArray != null && streakDataArray.length() > 0) {
                for (int i = 0; i < streakDataArray.length(); i++) {
                    Object item = streakDataArray.get(i);
                    
                    if (item instanceof JSONObject) {
                        JSONObject dayData = (JSONObject) item;
                    
                    // Extract date
                    String date = null;
                    if (dayData.has("date")) {
                        date = dayData.getString("date");
                    } else if (dayData.has("completion_date")) {
                        date = dayData.getString("completion_date");
                    }
                    
                    if (date != null) {
                        boolean hasActivity = false;
                        
                        // Check various fields for activity status
                        if (dayData.has("has_activity")) {
                            hasActivity = dayData.getBoolean("has_activity");
                        } else if (dayData.has("activity_count")) {
                            hasActivity = dayData.getInt("activity_count") > 0;
                        } else if (dayData.has("count")) {
                            hasActivity = dayData.getInt("count") > 0;
                        } else if (dayData.has("completed")) {
                            hasActivity = dayData.getBoolean("completed");
                        } else {
                            // Default to true if date is in the data
                            hasActivity = true;
                        }
                        
                        Log.d(TAG, "Streak data: " + date + " = " + hasActivity);
                        newWeekStreakData.put(date, hasActivity);
                        }
                    } else if (item instanceof String) {
                        // Simple date string format
                        String date = (String) item;
                        newWeekStreakData.put(date, true);
                        Log.d(TAG, "Streak data (string format): " + date + " = true");
                    }
                }
            } else {
                // Look for dates as direct keys
                Iterator<String> keys = response.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        boolean hasActivity = response.optBoolean(key, false);
                        newWeekStreakData.put(key, hasActivity);
                    }
                }
            }

            // Always ensure today shows as active if there's a login streak
            if (streakCount > 0) {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                newWeekStreakData.put(today, true);
            }
            
            // Save streak data for offline access
            saveStreakDataLocally(newWeekStreakData, streakCount);
            
            // Update the UI with the streak data
            weekStreakData.clear();
            weekStreakData.putAll(newWeekStreakData);
            updateCalendarWithCurrentWeek();
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing streak data JSON: " + e.getMessage(), e);
            useDefaultStreakData();
        }
    }

    /**
     * Save streak data to SharedPreferences for offline access
     */
    private void saveStreakDataLocally(Map<String, Boolean> streakData, int streakCount) {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("StreakPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Save streak count
            editor.putInt("streak_count", streakCount);
            
            // Save active days
            JSONArray activeDays = new JSONArray();
            for (Map.Entry<String, Boolean> entry : streakData.entrySet()) {
                if (entry.getValue()) {
                    activeDays.put(entry.getKey());
                }
            }
            
            editor.putString("active_days", activeDays.toString());
            editor.apply();
            
            Log.d(TAG, "Saved streak data locally: " + streakCount + " days, " + activeDays.length() + " active days");
        } catch (Exception e) {
            Log.e(TAG, "Error saving streak data locally: " + e.getMessage());
        }
    }

    private void useDefaultStreakData() {
        Log.w(TAG, "Using default streak data for calendar.");
        
        // Get streak count from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        int savedStreakCount = prefs.getInt("streak_count", 0);
        
        // Create simple streak data
        weekStreakData.clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Get dates for the last 7 days
        Calendar cal = Calendar.getInstance();
        String today = dateFormat.format(cal.getTime());
        
        // Set today as active if there's a streak count
        if (savedStreakCount > 0) {
            weekStreakData.put(today, true);
        }
        
        // Get more dates for the past week
        for (int i = 1; i <= 6; i++) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String date = dateFormat.format(cal.getTime());
            // For past dates, alternate pattern
            weekStreakData.put(date, savedStreakCount > 0 && i % 2 == 0);
        }
        
        // Update the calendar UI
        updateCalendarWithCurrentWeek();
        
        // Show a toast message about using cached data
        if (getContext() != null && isAdded()) {
            Toast.makeText(getContext(), "Using cached streak data. Check connection.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStreakCountUI(int count) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (streakCountTextView != null) {
                    streakCountTextView.setText(String.valueOf(count));
                    Log.d(TAG, "UI Updated: Streak days count set to " + count);
                } else {
                    Log.e(TAG, "streakCountTextView is null, cannot update streak count UI");
                }
                
                // Also update the top streak count chip if available (new layout uses text_streak_count)
                if (topStreakCountTextView != null) {
                    topStreakCountTextView.setText(String.valueOf(count));
                    Log.d(TAG, "Updated top streak count UI to: " + count);
                }
                
                // Save to SharedPreferences
                SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                prefs.edit().putInt("streak_count", count).apply();

                // Update fire icon visibility based on streak count
                if (fireIconStreakImageView != null) {
                    fireIconStreakImageView.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    /**
     * Update the "Today so far" stats in real-time
     * Call this method whenever tasks, workflows, or habits change
     */
    private void updateTodayStats() {
        if (getActivity() == null || !isAdded()) return;
        
        getActivity().runOnUiThread(() -> {
            // Update Tasks stat
            if (statTasksCompletedTextView != null) {
                statTasksCompletedTextView.setText(completedTasks + "/" + totalTasks);
            }
            
            // Update Workflows stat
            if (statWorkflowsCompletedTextView != null) {
                statWorkflowsCompletedTextView.setText(completedWorkflows + "/" + totalWorkflows);
            }
            
            // Update Habits stat
            if (statHabitsCompletedTextView != null) {
                statHabitsCompletedTextView.setText(completedHabits + "/" + totalHabits);
            }
            
            Log.d(TAG, "Stats updated - Tasks: " + completedTasks + "/" + totalTasks + 
                       ", Workflows: " + completedWorkflows + "/" + totalWorkflows + 
                       ", Habits: " + completedHabits + "/" + totalHabits);
        });
    }

    /**
     * Calculate and update stats based on current data
     */
    private void calculateAndUpdateStats() {
        // Reset counters
        totalTasks = 0;
        completedTasks = 0;
        totalWorkflows = 0;
        completedWorkflows = 0;
        
        // Count tasks and workflows from today's activities
        if (todayActivities != null) {
            for (TaskItem item : todayActivities) {
                if (item != null) {
                    String type = item.getType();
                    boolean isCompleted = item.isCompleted();
                    
                    if ("workflow".equalsIgnoreCase(type)) {
                        totalWorkflows++;
                        if (isCompleted) completedWorkflows++;
                    } else {
                        // Count as task (reminder, task, etc.)
                        totalTasks++;
                        if (isCompleted) completedTasks++;
                    }
                }
            }
        }
        
        // Count habits
        totalHabits = habitList != null ? habitList.size() : 0;
        completedHabits = 0;
        if (habitList != null) {
            for (Habit habit : habitList) {
                if (habit != null && habit.isCompleted()) {
                    completedHabits++;
                }
            }
        }
        
        // Update the UI
        updateTodayStats();
    }

    private void initializeCalendarViews(View view) {
        // Find the week calendar container (may be an include or direct)
        View weekCalendar = view.findViewById(R.id.week_calendar);
        View calendarRoot = weekCalendar != null ? weekCalendar : view;
        
        // Day of week TextViews
        weekDayTexts[0] = calendarRoot.findViewById(R.id.weekDayText1);
        weekDayTexts[1] = calendarRoot.findViewById(R.id.weekDayText2);
        weekDayTexts[2] = calendarRoot.findViewById(R.id.weekDayText3);
        weekDayTexts[3] = calendarRoot.findViewById(R.id.weekDayText4);
        weekDayTexts[4] = calendarRoot.findViewById(R.id.weekDayText5);
        weekDayTexts[5] = calendarRoot.findViewById(R.id.weekDayText6);
        weekDayTexts[6] = calendarRoot.findViewById(R.id.weekDayText7);

        // Day number TextViews
        dayTextViews[0] = calendarRoot.findViewById(R.id.dayTextView1);
        dayTextViews[1] = calendarRoot.findViewById(R.id.dayTextView2);
        dayTextViews[2] = calendarRoot.findViewById(R.id.dayTextView3);
        dayTextViews[3] = calendarRoot.findViewById(R.id.dayTextView4);
        dayTextViews[4] = calendarRoot.findViewById(R.id.dayTextView5);
        dayTextViews[5] = calendarRoot.findViewById(R.id.dayTextView6);
        dayTextViews[6] = calendarRoot.findViewById(R.id.dayTextView7);

        // Streak fire ImageView
        streakImageViews[0] = calendarRoot.findViewById(R.id.streakFireIcon1);
        streakImageViews[1] = calendarRoot.findViewById(R.id.streakFireIcon2);
        streakImageViews[2] = calendarRoot.findViewById(R.id.streakFireIcon3);
        streakImageViews[3] = calendarRoot.findViewById(R.id.streakFireIcon4);
        streakImageViews[4] = calendarRoot.findViewById(R.id.streakFireIcon5);
        streakImageViews[5] = calendarRoot.findViewById(R.id.streakFireIcon6);
        streakImageViews[6] = calendarRoot.findViewById(R.id.streakFireIcon7);

        // Check if all views were found (basic sanity check)
        for(int i=0; i<7; i++) {
            if (weekDayTexts[i] == null || dayTextViews[i] == null || streakImageViews[i] == null) {
                Log.e(TAG, "Calendar view at index " + i + " is missing! Check R.id names in streak.xml and fragment_home.xml include tag.");
                // You might want to disable calendar updates if views are missing.
                return;
            }
        }
        Log.d(TAG, "Calendar views initialized.");
    }

    /**
     * Update calendar with current week data and display fire icons
     */
    private void updateCalendarWithCurrentWeek() {
        try {
            // Initialize the calendar to show the current week
            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(Calendar.SUNDAY);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            
            // Format for date display and comparisons
            SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
            SimpleDateFormat weekDayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            String todayDate = fullDateFormat.format(new Date());
            
            // Get current streak from SharedPreferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            int currentStreak = prefs.getInt("streak_count", 0);
            
            // Update each day in the week
            for (int i = 0; i < 7; i++) {
                String dayOfMonth = dayFormat.format(calendar.getTime());
                String weekDay = weekDayFormat.format(calendar.getTime());
                String fullDate = fullDateFormat.format(calendar.getTime());
                
                // Set day number
                if (dayTextViews[i] != null) {
                    dayTextViews[i].setText(dayOfMonth);
                    
                    // Highlight today
                    if (fullDate.equals(todayDate)) {
                        GradientDrawable shape = new GradientDrawable();
                        shape.setShape(GradientDrawable.OVAL);
                        shape.setColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
                        dayTextViews[i].setBackground(shape);
                        dayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                    } else {
                        dayTextViews[i].setBackground(null);
                        dayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                }
                
                    // Store the date in the tag for the CalendarViewWithFireIcons
                    dayTextViews[i].setTag(fullDate);
                }
                
                // Set weekday name
                if (weekDayTexts[i] != null) {
                    weekDayTexts[i].setText(weekDay);
                }
                
                // Set streak fire icon visibility
                if (streakImageViews[i] != null) {
                    // For today, show streak if we have a streak count > 0
                    if (fullDate.equals(todayDate) && currentStreak > 0) {
                        streakImageViews[i].setVisibility(View.VISIBLE);
                        streakImageViews[i].setImageResource(R.drawable.ic_fire);
                        streakImageViews[i].setAlpha(1.0f);
                    } else {
                        // For other days, check in the week streak data
                        boolean hasStreak = false;
                        
                        if (weekStreakData.containsKey(fullDate)) {
                            hasStreak = weekStreakData.get(fullDate);
                        }
                        
                        if (hasStreak) {
                            streakImageViews[i].setVisibility(View.VISIBLE);
                            streakImageViews[i].setImageResource(R.drawable.ic_fire);
                            streakImageViews[i].setAlpha(1.0f);
                        } else {
                            streakImageViews[i].setVisibility(View.INVISIBLE);
                        }
                    }
                }
                
                // Move to next day
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            Log.d(TAG, "Calendar updated with current week and streak fire icons");
        } catch (Exception e) {
            Log.e(TAG, "Error updating calendar: " + e.getMessage(), e);
        }
    }

    private void displayActivitiesInTimeline(List<TaskItem> activities) {
        if (getContext() == null || !isAdded() || myDayTimelineRecycler == null) {
            Log.w(TAG, "Cannot display activities: context/view not available.");
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false); // Stop refresh if it was running
            return;
        }

        // Log the received activities for debugging
        if (activities != null) {
            Log.d(TAG, "Received " + activities.size() + " activities to display");
            for (int i = 0; i < Math.min(activities.size(), 5); i++) { // Log first 5 activities max
                TaskItem task = activities.get(i);
                Log.d(TAG, "Activity " + i + ": " + task.getTitle() + ", Type: " + task.getType() + ", ID: " + task.getId());
            }
        } else {
            Log.w(TAG, "Received null activities list");
            // Create an empty list to avoid null pointer exceptions
            activities = new ArrayList<>();
        }

        todayActivities.clear();
        if (activities != null && !activities.isEmpty()) {
            todayActivities.addAll(activities);
            Log.i(TAG, "Displaying " + todayActivities.size() + " activities in timeline.");
            
            // Convert TaskItems to Tasks for the adapter
            List<Task> tasks = convertTaskItemsToTasks(activities);
            
            // Create and set the adapter for the main timeline
            if (myDayTimelineRecycler != null) {
                TaskAdapter adapter = new TaskAdapter(tasks, taskCheckListener, requireContext());
                myDayTimelineRecycler.setAdapter(adapter);
                myDayTimelineRecycler.setVisibility(View.VISIBLE);
                
                // Hide empty state container
                View emptyStateContainer = findTimelineEmptyStateContainer();
                if (emptyStateContainer != null) {
                    emptyStateContainer.setVisibility(View.GONE);
                }
            }
            
            // Hide the empty timeline state in new layout
            View emptyTimeline = getView() != null ? getView().findViewById(R.id.empty_timeline) : null;
            if (emptyTimeline != null) {
                emptyTimeline.setVisibility(View.GONE);
            }
            
            // Force a layout pass to ensure the RecyclerView updates
            if (myDayTimelineRecycler != null) {
                myDayTimelineRecycler.post(() -> {
                    if (myDayTimelineRecycler.getAdapter() != null) {
                        myDayTimelineRecycler.getAdapter().notifyDataSetChanged();
                    }
                });
            }
        } else {
            Log.i(TAG, "No activities to display for today. Showing empty state.");
            displayEmptyTaskState();
        }
        
        // Update the "Today so far" stats with real-time counts
        calculateAndUpdateStats();
        
        // Stop the refresh indicator
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false); // Activities loaded (or empty state shown)
    }

    private List<Task> convertTaskItemsToTasks(List<TaskItem> activities) {
        List<Task> tasks = new ArrayList<>();
        if (activities == null) return tasks;

        String currentUserId = "";
        if(getActivity() != null) { // Ensure activity context is available for SharedPreferences
            SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            currentUserId = prefs.getString("user_id", ""); // Get current user ID
        }

        // Get today's date for due date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        for (TaskItem item : activities) {
            // Ensure task type is either "workflow" or "remainder" 
            String taskType = item.getType() != null ? item.getType().toLowerCase() : "";
            if (!taskType.equals("workflow") && !taskType.equals("remainder")) {
                taskType = "remainder"; // Default to remainder
            }
            
            Task task = new Task(
                    item.getId(),
                    currentUserId, // Use fetched userId
                    taskType,
                    item.getTitle(),
                    item.getDescription(),
                    item.getStartTime(),
                    item.getEndTime(),
                    todayDate, // Always set due date to today for home screen tasks
                    item.isCompleted() ? "completed" : "pending",
                    item.getRepeatFrequency(),
                    "medium" // Default priority - ensure your Task model handles this
            );
            task.setCurrentStreak(item.getStreak()); // Assuming Task class has set streak method
            tasks.add(task);
            
            Log.d(TAG, "Converted task: " + task.getTitle() + ", Type: " + task.getType() + ", ID: " + task.getTaskId());
        }
        return tasks;
    }

    private void displayEmptyTaskState() {
        // You might want a more user-friendly empty state, e.g., a specific layout in the RecyclerView
        todayActivities.clear();
        
        // Update MyDayMap timeline empty state
        if (myDayTimelineRecycler != null && getContext() != null) {
            myDayTimelineRecycler.setAdapter(new TaskAdapter(new ArrayList<>(), taskCheckListener, requireContext()));
            myDayTimelineRecycler.setVisibility(View.GONE);
            
            // Show the timeline empty state container
            View emptyStateContainer = findTimelineEmptyStateContainer();
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(View.VISIBLE);
            }
        }
        
        // Show empty timeline state in new layout
        View emptyTimeline = getView() != null ? getView().findViewById(R.id.empty_timeline) : null;
        if (emptyTimeline != null && todayActivities.isEmpty()) {
            emptyTimeline.setVisibility(View.VISIBLE);
        }
    }

    // Helper method to find the timeline empty state container
    private View findTimelineEmptyStateContainer() {
        // For new layout, use empty_timeline directly
        View emptyTimeline = getView() != null ? getView().findViewById(R.id.empty_timeline) : null;
        if (emptyTimeline != null) {
            return emptyTimeline;
        }
        
        // Fallback for old layout with myDayMapLayout
        View myDayMapLayout = getView() != null ? getView().findViewById(R.id.card_timeline) : null;
        if (myDayMapLayout != null) {
            View timelineContainer = myDayMapLayout.findViewById(R.id.empty_timeline);
            if (timelineContainer != null) {
                return timelineContainer;
            }
        }
        return null;
    }

    // --- Listener Implementations ---

    // TodayActivitiesManager.TodayActivitiesListener
    @Override
    public void onActivitiesLoaded(List<TaskItem> activities) {
        try {
            // Ensure this runs on the UI thread
            if (getActivity() == null || !isAdded()) return;
            
            getActivity().runOnUiThread(() -> {
                Log.d(TAG, "Received " + activities.size() + " activities to display");
                
                // Debug the activities to understand what's coming back from the server
                for (int i = 0; i < activities.size(); i++) {
                    TaskItem activity = activities.get(i);
                    // Ensure every activity has the correct type set
                    if (activity.getType() == null || activity.getType().isEmpty()) {
                        activity.setType("remainder"); // Default to remainder if type is missing
                    }
                    Log.d(TAG, "Activity " + i + ": " + activity.getTitle() + ", Type: " + activity.getType() + ", ID: " + activity.getId());
                }
                
                // Store the activities for use in other parts of the fragment
                todayActivities.clear();
                todayActivities.addAll(activities);

                // Display the activities in the timeline
                if (activities.isEmpty()) {
                    Log.i(TAG, "No activities to display in timeline.");
                    displayEmptyTaskState();
                } else {
                    Log.i(TAG, "Displaying " + activities.size() + " activities in timeline.");
                    // Convert activities to task model for display
                    List<Task> tasks = convertTaskItemsToTasks(activities);
                    
                    // Display activities in timeline
                    if (myDayTimelineRecycler != null) {
                        // Create and set the adapter
                        TaskAdapter adapter = new TaskAdapter(tasks, taskCheckListener, requireContext());
                        
                        // Sort to move completed tasks to the bottom
                        adapter.sortTasksByCompletionStatus();
                        
                        myDayTimelineRecycler.setAdapter(adapter);
                        
                        // Hide empty state
                        View emptyStateView = findTimelineEmptyStateContainer();
                        if (emptyStateView != null) {
                            emptyStateView.setVisibility(View.GONE);
                        }
                        if (myDayTimelineEmptyText != null) {
                            myDayTimelineEmptyText.setVisibility(View.GONE);
                        }
                    }
                    
                }
                
                Log.d(TAG, "Activities loaded: " + activities.size());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error displaying activities: " + e.getMessage());
        }
    }

    // ProfileManager.ProfileLoadListener
    @Override
    public void onProfileLoaded(String name, String email, String avatarUrl) {
        // This is from ProfileManager (potentially for a more detailed profile)
        Log.i(TAG, "onProfileLoaded (from ProfileManager) Name: " + name + ", Email: " + email + ", Avatar: " + avatarUrl);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (userNameTextView != null) userNameTextView.setText(name);
                // SharedPreferences update for name/email if this is the primary source
                SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("username", name);
                if (email != null) editor.putString("email", email);
                editor.apply();

                if (profileImageView != null && getContext() != null) {
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)     // Ensure this exists
                                .circleCrop()
                                .into(profileImageView);
                    } else {
                        Glide.with(requireContext())
                                .load(R.drawable.default_avatar) // Default if no URL
                                .circleCrop()
                                .into(profileImageView);
                    }
                }
                // After profile update, you might want to refresh other dependent data
                // e.g., if user ID could change or settings affecting tasks.
                // For now, assume main data refresh is handled by onResume/SwipeRefresh.
            });
        }
    }

    // TaskManager.TaskListener (parts already handled by inline listener, this is for tasks loaded by TaskManager directly)
    @Override
    public void onTasksLoaded(List<Task> tasks) {
        // This is called if TaskManager itself loads a list of all tasks
        // Since we no longer track habits, we just log the task count
        Log.d(TAG, "TaskManager: onTasksLoaded with " + (tasks != null ? tasks.size() : "null") + " tasks.");
    }

    // Removed updateTaskCountsAndProgress method as habit tracking is no longer needed

    // Other TaskManager.TaskListener methods if HomeFragment needs to respond to them directly
    // Note: The taskCheckListener already has its own TaskManager instance for updates.
    // These methods below would be for a TaskManager instance owned by HomeFragment itself, if any.

    @Override
    public void onTaskAdded(Task task) { // From a HomeFragment-owned TaskManager
        Log.d(TAG, "HomeFragment's TaskManager: onTaskAdded - " + task.getTitle());
        if (activitiesManager != null && userId != null) activitiesManager.loadTodayActivities(); // Refresh timeline
    }

    @Override
    public void onTaskUpdated(Task task) { // From a HomeFragment-owned TaskManager
        Log.d(TAG, "HomeFragment's TaskManager: onTaskUpdated - " + task.getTitle());
        if (activitiesManager != null && userId != null) activitiesManager.loadTodayActivities(); // Refresh timeline
    }

    @Override
    public void onTaskDeleted(String taskId) { // From a HomeFragment-owned TaskManager
        Log.d(TAG, "HomeFragment's TaskManager: onTaskDeleted - " + taskId);
        // Local removal and refresh
        for (int i = 0; i < todayActivities.size(); i++) {
            if (todayActivities.get(i).getId().equals(taskId)) {
                todayActivities.remove(i);
                if (myDayTimelineRecycler != null && myDayTimelineRecycler.getAdapter() != null) {
                    myDayTimelineRecycler.getAdapter().notifyItemRemoved(i);
                }
                break;
            }
        }
        if (activitiesManager != null && userId != null) activitiesManager.loadTodayActivities();
    }

    @Override
    public void onHabitStreakUpdated(String taskId, int newStreak) { // From a HomeFragment-owned TaskManager and HabitManagerService
        Log.d(TAG, "HomeFragment's TaskManager/HabitManager: onHabitStreakUpdated for task/habit " + taskId + " New Streak: " + newStreak);
        updateTaskStreakInList(taskId, newStreak);
        
        // Also update habit streaks if applicable
        for (Habit habit : habitList) {
            if (habit.getHabitId().equals(taskId)) {
                // Update habit streak if needed
                Log.d(TAG, "Updated habit streak for: " + habit.getTitle());
                break;
            }
        }
    }

    @Override
    public void onStreakDataLoaded(Map<String, Boolean> streakData) {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Cannot process streak data: Fragment not attached");
            return;
        }
        
        try {
            // Store the streak data
            weekStreakData.clear();
            weekStreakData.putAll(streakData);
            
            // Update calendar UI with streak data
            requireActivity().runOnUiThread(this::updateCalendarWithCurrentWeek);
            
            Log.d(TAG, "Streak data loaded and calendar updated");
        } catch (Exception e) {
            Log.e(TAG, "Error processing streak data: " + e.getMessage(), e);
        }
    }

    @Override
    public void onUserProfileLoaded(String name, int streakCount, String avatarUrl) {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Cannot update profile: Fragment not attached");
            return;
        }
        
        try {
            Log.d(TAG, "User profile loaded: " + name + ", streak: " + streakCount);
            
            // Update streak count if provided
            if (streakCount >= 0) {
                requireActivity().runOnUiThread(() -> {
                    // Update main streak count display
                    if (streakCountTextView != null) {
                        streakCountTextView.setText(String.valueOf(streakCount));
                    }
                    
                    // Update top bar streak chip (text_streak_count)
                    if (topStreakCountTextView != null) {
                        topStreakCountTextView.setText(String.valueOf(streakCount));
                        Log.d(TAG, "Updated top streak chip to: " + streakCount);
                    }
                    
                    // Update fire icon visibility
                    if (fireIconStreakImageView != null) {
                        fireIconStreakImageView.setVisibility(streakCount > 0 ? View.VISIBLE : View.GONE);
                    }
                    
                    // Save streak count to preferences
                    SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putInt("streak_count", streakCount).apply();
                    
                    // Update calendar to reflect streak in today's date
                    updateCalendarWithCurrentWeek();
                });
            }
            
            // Update username if provided and different from current
            if (name != null && !name.isEmpty() && userNameTextView != null && !name.equals(username)) {
                requireActivity().runOnUiThread(() -> {
                    userNameTextView.setText(name);
                    
                    // Save username to preferences
                    SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putString("username", name).apply();
                    username = name;
                });
            }
            
            // Update XP/coins display from SharedPreferences (saved by TodayActivitiesManager)
            requireActivity().runOnUiThread(() -> {
                if (xpCountTextView != null) {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    int xpCoins = prefs.getInt("xp_coins", 0);
                    xpCountTextView.setText(String.valueOf(xpCoins));
                    Log.d(TAG, "Updated XP/coins display to: " + xpCoins);
                }
            });
            
            // Update avatar if provided and view exists
            if (avatarUrl != null && !avatarUrl.isEmpty() && profileImageView != null) {
                requireActivity().runOnUiThread(() -> {
                    Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(profileImageView);
                    
                    // Save avatar URL to preferences
                    SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putString("avatar_url", avatarUrl).apply();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating profile: " + e.getMessage(), e);
        }
    }

    @Override
    public void onError(String message) {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Cannot display error: Fragment not attached");
            return;
        }
        
        // Hide loading indicator
        if (swipeRefresh != null) {
            requireActivity().runOnUiThread(() -> swipeRefresh.setRefreshing(false));
        }
        
        // Log the error
        Log.e(TAG, "Error in HomeFragment: " + message);
        
        // Show a toast with the error message
        requireActivity().runOnUiThread(() -> {
            // Only show user-friendly part of the message
            String userMessage = message;
            if (message.contains("Network error")) {
                userMessage = "Network error. Using locally stored data.";
            } else if (message.contains("Error loading")) {
                userMessage = "Error loading data. Please try again later.";
            }
            
            Toast.makeText(getContext(), userMessage, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Updates the MyDayMap component with the given tasks
     */
    private void updateMyDayMap(List<Task> tasks) {
        if (myDayRecyclerView == null) {
            Log.e(TAG, "MyDayMap views not initialized");
            return;
        }
        
        // Filter tasks to only include workflow and remainder types (exclude habits)
        List<Task> filteredTasks = new ArrayList<>();
        
        // Add regular tasks (workflow and remainder types only)
        if (tasks != null && !tasks.isEmpty()) {
            for (Task task : tasks) {
                // Only include workflow and remainder type tasks, exclude habits
                if (!task.isHabit() && 
                    (task.getTaskType() == null || 
                     task.getTaskType().equalsIgnoreCase("workflow") || 
                     task.getTaskType().equalsIgnoreCase("remainder"))) {
                    filteredTasks.add(task);
                }
            }
        }
        
        // Also filter from todayActivities - exclude habits
        if (todayActivities != null && !todayActivities.isEmpty()) {
            for (TaskItem item : todayActivities) {
                // Skip habit-type items
                if (!"habit".equalsIgnoreCase(item.getType())) {
                    // Only include workflow and remainder types
                    if (item.getType() == null || 
                        item.getType().equalsIgnoreCase("workflow") || 
                        item.getType().equalsIgnoreCase("remainder")) {
                        Task task = createTaskFromTaskItem(item);
                        filteredTasks.add(task);
                    }
                }
            }
        }
        
        // Sort tasks by completion status and time
        Collections.sort(filteredTasks, (t1, t2) -> {
            // First sort by completion status (incomplete first)
            if (t1.isCompleted() && !t2.isCompleted()) return 1;
            if (!t1.isCompleted() && t2.isCompleted()) return -1;
            
            // Then sort by start time if available
            String time1 = t1.getStartTime();
            String time2 = t2.getStartTime();
            
            if (time1 != null && time2 != null && !time1.isEmpty() && !time2.isEmpty()) {
                return time1.compareTo(time2);
            }
            
            return 0;
        });
        
        // Store tasks for reference
        myDayTasks.clear();
        myDayTasks.addAll(filteredTasks);
        
        // Update the main MyDay section
        if (filteredTasks.isEmpty() && myDayEmptyState != null) {
            myDayRecyclerView.setVisibility(View.GONE);
            myDayEmptyState.setVisibility(View.VISIBLE);
        } else {
            myDayRecyclerView.setVisibility(View.VISIBLE);
            if (myDayEmptyState != null) {
                myDayEmptyState.setVisibility(View.GONE);
            }
            
            // Create and set adapter for tasks
            TaskAdapter adapter = new TaskAdapter(filteredTasks, taskCheckListener, requireContext());
            myDayRecyclerView.setAdapter(adapter);
        }
        
        // Update the timeline section
        if (myDayTimelineRecycler != null) {
            if (filteredTasks.isEmpty()) {
                myDayTimelineRecycler.setVisibility(View.GONE);
                
                // Show timeline empty state
                View timelineEmptyState = findTimelineEmptyStateContainer();
                if (timelineEmptyState != null) {
                    timelineEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                myDayTimelineRecycler.setVisibility(View.VISIBLE);
                
                // Hide timeline empty state
                View timelineEmptyState = findTimelineEmptyStateContainer();
                if (timelineEmptyState != null) {
                    timelineEmptyState.setVisibility(View.GONE);
                }
                
                TaskAdapter timelineAdapter = new TaskAdapter(filteredTasks, taskCheckListener, requireContext());
                myDayTimelineRecycler.setAdapter(timelineAdapter);
            }
        }
        
        Log.d(TAG, "Updated MyDay Map - Tasks (workflow/remainder only): " + filteredTasks.size());
    }
    
    /**
     * Convert a single TaskItem to a Task
     */
    private Task createTaskFromTaskItem(TaskItem item) {
        String userId = "";
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("user_id", "");
        }
        
        // Create new task with required parameters
        Task task = new Task(
            item.getId(), // task id
            userId,      // user id
            item.getType(), // type
            item.getTitle(), // title
            item.getDescription() != null ? item.getDescription() : "", // description
            item.getStartTime() != null ? item.getStartTime() : "", // start time
            item.getEndTime() != null ? item.getEndTime() : "", // end time
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()), // due date (today)
            item.isCompleted() ? "completed" : "pending", // status
            "", // repeat frequency
            item.getPriority() != null ? item.getPriority() : "medium" // priority
        );
        
        // Set completion status
        task.setCompleted(item.isCompleted());
        
        // Handle extra properties if available
        try {
            if (item.getExtraProperties() != null) {
                JSONObject extraProps = item.getExtraProperties();
                
                // Check for verification type
                if (extraProps.has("trust_type")) {
                    task.setTaskType(extraProps.getString("trust_type"));
                }
                
                // Check for location data
                if (extraProps.has("map_lat") && extraProps.has("map_lon")) {
                    task.setLatitude(extraProps.getDouble("map_lat"));
                    task.setLongitude(extraProps.getDouble("map_lon"));
                }
                
                // Check for pomodoro data
                if (extraProps.has("pomodoro_count") && extraProps.has("pomodoro_duration")) {
                    task.setPomodoroCount(extraProps.getInt("pomodoro_count"));
                    task.setPomodoroLength(extraProps.getInt("pomodoro_duration"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing extra properties: " + e.getMessage());
        }
        
        return task;
    }

    // Keep the old method signature for backward compatibility
    private void refreshAllData() {
        refreshAllData(true);
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
            // Quick activity refresh without heavy operations
            if (activitiesManager != null) {
                // Use cached data first, then refresh in background
                activitiesManager.loadTodayActivitiesQuickly();
            }
            
            // Quick habit refresh
            if (habitManager != null) {
                habitManager.loadHabits();
            }
            
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

    // HabitManagerService.HabitListener implementation
    @Override
    public void onHabitsLoaded(List<Habit> habits) {
        if (!isAdded() || getContext() == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                habitList.clear();
                if (habits != null && !habits.isEmpty()) {
                    habitList.addAll(habits);
                    Log.d(TAG, "Loaded " + habits.size() + " habits in home");
                } else {
                    Log.d(TAG, "No habits loaded for today");
                }
                
                if (habitAdapter != null) {
                    habitAdapter.notifyDataSetChanged();
                }
                
                updateHabitEmptyState();
                
                // Update the "Today so far" stats with real-time counts
                calculateAndUpdateStats();
                
                // Hide refresh loading
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating habits UI: " + e.getMessage());
            }
        });
    }

    @Override
    public void onHabitAdded(Habit habit) {
        if (!isAdded() || getContext() == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                habitList.add(habit);
                if (habitAdapter != null) {
                    habitAdapter.notifyItemInserted(habitList.size() - 1);
                }
                updateHabitEmptyState();
                
                // Update stats after adding habit
                calculateAndUpdateStats();
                
                Log.d(TAG, "Habit added to home: " + habit.getTitle());
            } catch (Exception e) {
                Log.e(TAG, "Error adding habit to UI: " + e.getMessage());
            }
        });
    }

    @Override
    public void onHabitUpdated(Habit habit) {
        if (!isAdded() || getContext() == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                for (int i = 0; i < habitList.size(); i++) {
                    if (habitList.get(i).getHabitId().equals(habit.getHabitId())) {
                        habitList.set(i, habit);
                        if (habitAdapter != null) {
                            habitAdapter.notifyItemChanged(i);
                        }
                        Log.d(TAG, "Habit updated in home: " + habit.getTitle());
                        break;
                    }
                }
                // Update stats after habit completion status change
                calculateAndUpdateStats();
            } catch (Exception e) {
                Log.e(TAG, "Error updating habit in UI: " + e.getMessage());
            }
        });
    }

    @Override
    public void onHabitVerified(Habit habit, boolean isCompleted) {
        if (!isAdded() || getContext() == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                onHabitUpdated(habit);
                // Stats already updated by onHabitUpdated
                Toast.makeText(getContext(), 
                    habit.getTitle() + " " + (isCompleted ? "completed" : "verification failed"), 
                    Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error handling habit verification: " + e.getMessage());
            }
        });
    }

    @Override
    public void onProgressUpdated(float overallProgress, Map<String, Float> habitProgress) {
        Log.d(TAG, "Habit progress updated: " + overallProgress);
    }

    private void updateHabitEmptyState() {
        if (habitEmptyState != null && habitRecyclerView != null) {
            if (habitList.isEmpty()) {
                habitEmptyState.setVisibility(View.VISIBLE);
                habitRecyclerView.setVisibility(View.GONE);
            } else {
                habitEmptyState.setVisibility(View.GONE);
                habitRecyclerView.setVisibility(View.VISIBLE);
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
        
        // Optimize all RecyclerViews to work better within ScrollView
        // These RecyclerViews may not exist in new layout - check for null
        RecyclerView timelineRecycler = getView().findViewById(R.id.recycler_timeline);
        optimizeRecyclerViewInScrollView(timelineRecycler, 10);
        optimizeRecyclerViewInScrollView(habitRecyclerView, 10);
        
        // Optimize timeline RecyclerView
        optimizeRecyclerViewInScrollView(myDayTimelineRecycler, 20);
        
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

    // Implement HabitAdapter.HabitInteractionListener methods
    @Override
    public void onHabitChecked(String habitId, boolean isChecked) {
        Log.d(TAG, "Habit checked: " + habitId + ", checked: " + isChecked);
        
        // Find the habit in the list and update its completion status
        for (Habit habit : habitList) {
            if (habit.getHabitId().equals(habitId)) {
                habit.setCompleted(isChecked);
                break;
            }
        }
        
        // Update the habit through HabitManagerService
        if (habitManager != null) {
            habitManager.verifyHabitWithCheckbox(habitId, isChecked);
        }
        
        // Refresh the adapter
        if (habitAdapter != null) {
            habitAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLocationVerifyClicked(String habitId) {
        Log.d(TAG, "Location verify clicked for habit: " + habitId);
        
        // Find the habit and launch location verification activity
        Habit habit = null;
        for (Habit h : habitList) {
            if (h.getHabitId().equals(habitId)) {
                habit = h;
                break;
            }
        }
        
        if (habit != null && habit.getVerificationMethod().equals(Habit.VERIFICATION_LOCATION)) {
            // Launch the location verification activity with all required data
            Intent intent = new Intent(getContext(), LocationVerificationActivity.class);
            intent.putExtra(LocationVerificationActivity.EXTRA_HABIT_ID, habitId);
            intent.putExtra(LocationVerificationActivity.EXTRA_HABIT_TITLE, habit.getTitle());
            intent.putExtra(LocationVerificationActivity.EXTRA_HABIT_DESCRIPTION, habit.getDescription());
            intent.putExtra(LocationVerificationActivity.EXTRA_TARGET_LATITUDE, habit.getLatitude());
            intent.putExtra(LocationVerificationActivity.EXTRA_TARGET_LONGITUDE, habit.getLongitude());
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Habit not found or not location-based", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPomodoroStartClicked(String habitId) {
        Log.d(TAG, "Pomodoro start clicked for habit: " + habitId);
        
        // Launch the PomodoroActivity instead of directly marking complete
        if (habitManager != null && getContext() != null) {
            Habit habit = habitManager.getHabit(habitId);
            if (habit != null) {
                Intent intent = new Intent(getContext(), PomodoroActivity.class);
                intent.putExtra(PomodoroActivity.EXTRA_HABIT_ID, habitId);
                intent.putExtra("habit_title", habit.getTitle());
                // Get duration from habit - use getPomodoroLength() which defaults to 25
                int duration = habit.getPomodoroLength();
                if (duration <= 0) duration = 25; // Fallback to default
                intent.putExtra(PomodoroActivity.EXTRA_POMODORO_LENGTH_MIN, duration);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Habit not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPomodoroPauseClicked(String habitId) {
        Log.d(TAG, "Pomodoro pause clicked for habit: " + habitId);
        
        // For now, just log as HabitManagerService doesn't have pause functionality
        // This could be implemented in the PomodoroActivity or future pomodoro management
        Toast.makeText(getContext(), "Pomodoro paused for habit", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPomodoroResumeClicked(String habitId) {
        Log.d(TAG, "Pomodoro resume clicked for habit: " + habitId);
        
        // For now, just log as HabitManagerService doesn't have resume functionality
        // This could be implemented in the PomodoroActivity or future pomodoro management
        Toast.makeText(getContext(), "Pomodoro resumed for habit", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPomodoroStopClicked(String habitId) {
        Log.d(TAG, "Pomodoro stop clicked for habit: " + habitId);
        
        // For now, just log as HabitManagerService doesn't have stop functionality
        // This could be implemented in the PomodoroActivity or future pomodoro management
        Toast.makeText(getContext(), "Pomodoro stopped for habit", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Apply scroll optimizations to fix nested RecyclerView conflicts
        optimizeScrollingPerformance();
    }

    /**
     * Updates the header with current day and date
     */
    private void updateHeaderDate() {
        Calendar calendar = Calendar.getInstance();
        
        // Format: "Tuesday"
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String dayOfWeek = dayFormat.format(calendar.getTime());
        
        // Format: "December 2, 2025"
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String fullDate = dateFormat.format(calendar.getTime());
        
        if (currentDayTextView != null) {
            currentDayTextView.setText(dayOfWeek);
        }
        if (currentDateTextView != null) {
            currentDateTextView.setText(fullDate);
        }
    }

    /**
     * Sets up menu components including menu button and overlay
     */
    private void setupMenuComponents(View view) {
        ImageButton menuButton = view.findViewById(R.id.menuButton);
        View menuOverlay = view.findViewById(R.id.menu_overlay);
        
        if (menuButton != null && menuOverlay != null) {
            // Set menu button click listener
            menuButton.setOnClickListener(v -> {
                if (menuOverlay.getVisibility() == View.GONE) {
                    menuOverlay.setVisibility(View.VISIBLE);
                } else {
                    menuOverlay.setVisibility(View.GONE);
                }
            });
            
            // Setup menu item click listeners
            setupMenuItemListeners(menuOverlay);
            
            // Hide menu when clicking outside
            view.setOnClickListener(v -> {
                if (menuOverlay.getVisibility() == View.VISIBLE) {
                    menuOverlay.setVisibility(View.GONE);
                }
            });
        }
    }
    
    /**
     * Sets up click listeners for menu items
     */
    private void setupMenuItemListeners(View menuOverlay) {
        // Settings main button - toggles submenu
        View settingsButton = menuOverlay.findViewById(R.id.menu_settings);
        View settingsSubmenu = menuOverlay.findViewById(R.id.settings_submenu);
        ImageView settingsArrow = menuOverlay.findViewById(R.id.settings_arrow);
        
        if (settingsButton != null && settingsSubmenu != null) {
            settingsButton.setOnClickListener(v -> {
                if (settingsSubmenu.getVisibility() == View.GONE) {
                    // Expand submenu
                    settingsSubmenu.setVisibility(View.VISIBLE);
                    if (settingsArrow != null) {
                        settingsArrow.setRotation(180f); // Rotate arrow up
                    }
                } else {
                    // Collapse submenu
                    settingsSubmenu.setVisibility(View.GONE);
                    if (settingsArrow != null) {
                        settingsArrow.setRotation(0f); // Rotate arrow down
                    }
                }
            });
        }
        
        // Notification Settings
        View notificationSettings = menuOverlay.findViewById(R.id.menu_notification_settings);
        if (notificationSettings != null) {
            notificationSettings.setOnClickListener(v -> {
                hideMenu(menuOverlay);
                Intent intent = new Intent(getActivity(), NotificationSettingsActivity.class);
                startActivity(intent);
            });
        }
        
        // Reminder Settings
        View reminderSettings = menuOverlay.findViewById(R.id.menu_reminder_settings);
        if (reminderSettings != null) {
            reminderSettings.setOnClickListener(v -> {
                hideMenu(menuOverlay);
                Toast.makeText(getContext(), "Reminder Settings - Coming Soon!", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Test Notifications (Debug)
        View testNotifications = menuOverlay.findViewById(R.id.menu_test_notifications);
        if (testNotifications != null) {
            testNotifications.setOnClickListener(v -> {
                hideMenu(menuOverlay);
                runNotificationTests();
            });
        }
        
        // Logout
        View logout = menuOverlay.findViewById(R.id.menu_logout);
        if (logout != null) {
            logout.setOnClickListener(v -> {
                hideMenu(menuOverlay);
                performLogout();
            });
        }
    }
    
    /**
     * Hides the menu overlay and resets submenu states
     */
    private void hideMenu(View menuOverlay) {
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
            
            // Reset settings submenu state
            View settingsSubmenu = menuOverlay.findViewById(R.id.settings_submenu);
            ImageView settingsArrow = menuOverlay.findViewById(R.id.settings_arrow);
            
            if (settingsSubmenu != null) {
                settingsSubmenu.setVisibility(View.GONE);
            }
            if (settingsArrow != null) {
                settingsArrow.setRotation(0f);
            }
        }
    }
    
    /**
     * Performs user logout
     */
    private void performLogout() {
        // Clear user preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
    
    /**
     * Run notification integration tests (debug feature)
     */
    private void runNotificationTests() {
        if (getContext() == null) {
            Log.e(TAG, "Cannot run tests: Context is null");
            return;
        }
        
        Log.d(TAG, "Starting notification integration tests from HomeFragment menu");
        Toast.makeText(getContext(), "Running notification integration tests...", Toast.LENGTH_SHORT).show();
        
        // Run tests in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Run basic integration tests using the helper
                NotificationTestHelper helper = new NotificationTestHelper(getContext());
                
                // Test basic functionality
                Log.d(TAG, "Running basic notification tests...");
                boolean basicTestResult = helper.testBasicFunctionality();
                
                // Test workflow notifications
                Log.d(TAG, "Running workflow notification tests...");
                boolean workflowTestResult = helper.testWorkflowNotifications();
                
                // Test reminder notifications
                Log.d(TAG, "Running reminder notification tests...");
                boolean reminderTestResult = helper.testReminderNotifications();
                
                // Show success message on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String message = String.format("Tests completed!\nBasic: %s\nWorkflow: %s\nReminder: %s",
                            basicTestResult ? "✓" : "✗",
                            workflowTestResult ? "✓" : "✗", 
                            reminderTestResult ? "✓" : "✗");
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error running notification tests: " + e.getMessage(), e);
                
                // Show error message on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Notification tests failed: " + e.getMessage(), 
                                     Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }
    
    /**
     * Sets up add task overlay components - Now handled by MainNavigationActivity FAB
     * This method is kept for backward compatibility but does nothing
     */
    private void setupAddTaskOverlay(View view) {
        // Add task button and overlay removed - using unified FAB in MainNavigationActivity
        // The FAB bottom sheet in MainNavigationActivity now handles all add task/habit functionality
    }
    
    /**
     * Sets up click listeners for add task overlay items - No longer used
     * Functionality moved to MainNavigationActivity bottom sheet
     */
    private void setupAddTaskOverlayListeners(View addTaskOverlay) {
        // This functionality is now in MainNavigationActivity bottom sheet
    }
    
    /**
     * Hides the add task overlay - No longer used
     */
    private void hideAddTaskOverlay(View addTaskOverlay) {
        // No longer used - handled by MainNavigationActivity
    }

    /**
     * Public method to refresh fragment data
     * Called from MainNavigationActivity when data needs to be updated
     */
    public void refreshData() {
        refreshAllData(true);
    }
}