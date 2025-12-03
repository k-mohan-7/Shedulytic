package com.example.shedulytic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.widget.ScrollView;

import com.example.shedulytic.adapter.HabitAdapter;
import com.example.shedulytic.model.Habit;
import com.example.shedulytic.service.HabitManagerService;
import com.example.shedulytic.VolleyNetworkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.example.shedulytic.service.LocationVerificationService;
import com.example.shedulytic.service.PomodoroService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HabitFragment extends Fragment implements HabitManagerService.HabitListener, HabitAdapter.HabitInteractionListener {
    private static final String TAG = "HabitFragment";
    
    // UI components
    private RecyclerView recyclerView;
    private HabitAdapter habitAdapter;
    private TextView daysCountTextView;
    private TextView emptyStateText;
    private Button addHabitButton;
    private SwipeRefreshLayout swipeRefresh; // Added for optimized scrolling
    
    // Data
    private List<Habit> habitList = new ArrayList<>();
    
    // Services
    private HabitManagerService habitManager;
    private LocationVerificationService locationService;
    private PomodoroService pomodoroService;
    private boolean locationServiceBound = false;
    private boolean pomodoroServiceBound = false;
    
    // Broadcast receivers
    private BroadcastReceiver locationReceiver;
    private BroadcastReceiver pomodoroReceiver;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit, container, false);
        
        // Initialize UI components
        recyclerView = view.findViewById(R.id.habits_recycler_view);
        daysCountTextView = view.findViewById(R.id.textView_days_count);
        emptyStateText = view.findViewById(R.id.empty_habits_text);
        addHabitButton = view.findViewById(R.id.add_habit_button);
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
                Log.i(TAG, "Swipe to refresh triggered - refreshing habits");
                refreshAllDataFast();
            });
            
            // Optimize refresh behavior to prevent scroll conflicts
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
                // CRITICAL FIX: Better scroll detection to prevent lock-up
                if (recyclerView != null) {
                    // Check if RecyclerView can scroll up and has content
                    boolean canScrollUp = recyclerView.canScrollVertically(-1);
                    if (canScrollUp) {
                        return true;
                    }
                }
                
                // Check ScrollView scroll position
                if (child instanceof ScrollView) {
                    ScrollView scrollView = (ScrollView) child;
                    return scrollView.getScrollY() > 0;
                }
                
                // Default: allow refresh when no scroll conflicts
                return false;
            });
        }
        
        // Set up RecyclerView with optimizations
        setupRecyclerView();
        
        // Set up add habit button
        addHabitButton.setOnClickListener(v -> {
            // Launch add habit activity
            Intent intent = new Intent(getContext(), AddHabitActivity.class);
            startActivity(intent);
        });
        
        // Initialize habit manager
        habitManager = HabitManagerService.getInstance(requireContext());
        habitManager.setListener(this);
        
        // Set up broadcast receivers
        setupBroadcastReceivers();
        
        // Apply scrolling performance optimizations
        optimizeScrollingPerformance();
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Load habits
        loadHabits();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        // Bind to services
        bindServices();
        
        // Register broadcast receivers
        registerReceivers();
    }
    
    @Override
    public void onStop() {
        // Unregister broadcast receivers
        unregisterReceivers();
        
        // Unbind from services
        unbindServices();
        
        super.onStop();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Refresh habits
        loadHabits();
        
        // Re-apply scrolling optimizations after resume
        optimizeScrollingPerformance();
    }
    
    /**
     * Set up broadcast receivers for location and pomodoro services
     */
    private void setupBroadcastReceivers() {
        // Location receiver
        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationVerificationService.ACTION_LOCATION_UPDATED.equals(intent.getAction())) {
                    double latitude = intent.getDoubleExtra(LocationVerificationService.EXTRA_LATITUDE, 0);
                    double longitude = intent.getDoubleExtra(LocationVerificationService.EXTRA_LONGITUDE, 0);
                    
                    // Check if any location-based habits can be verified
                    checkLocationHabits(latitude, longitude);
                }
            }
        };
        
        // Pomodoro receiver
        pomodoroReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (PomodoroService.ACTION_POMODORO_COMPLETED.equals(intent.getAction())) {
                    String habitId = intent.getStringExtra(PomodoroService.EXTRA_HABIT_ID);
                    
                    // Verify the habit
                    if (habitId != null) {
                        habitManager.verifyHabitWithPomodoro(habitId);
                    }
                } else if (PomodoroService.ACTION_TIMER_TICK.equals(intent.getAction())) {
                    // Update UI for any active pomodoro timers
                    long timeRemaining = intent.getLongExtra(PomodoroService.EXTRA_TIME_REMAINING, 0);
                    int totalPomodoros = intent.getIntExtra(PomodoroService.EXTRA_POMODORO_COUNT, 1);
                    int completedPomodoros = intent.getIntExtra(PomodoroService.EXTRA_COMPLETED_COUNT, 0);
                    String habitId = intent.getStringExtra(PomodoroService.EXTRA_HABIT_ID);
                    
                    // Update the habit item in the adapter
                    if (habitId != null) {
                        habitAdapter.updatePomodoroStatus(habitId, timeRemaining, completedPomodoros, totalPomodoros);
                    }
                }
            }
        };
    }
    
    /**
     * Register broadcast receivers
     */
    private void registerReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        
        // Register location receiver
        IntentFilter locationFilter = new IntentFilter(LocationVerificationService.ACTION_LOCATION_UPDATED);
        lbm.registerReceiver(locationReceiver, locationFilter);
        
        // Register pomodoro receiver
        IntentFilter pomodoroFilter = new IntentFilter();
        pomodoroFilter.addAction(PomodoroService.ACTION_TIMER_TICK);
        pomodoroFilter.addAction(PomodoroService.ACTION_TIMER_FINISHED);
        pomodoroFilter.addAction(PomodoroService.ACTION_POMODORO_COMPLETED);
        lbm.registerReceiver(pomodoroReceiver, pomodoroFilter);
    }
    
    /**
     * Unregister broadcast receivers
     */
    private void unregisterReceivers() {
        try {
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
            lbm.unregisterReceiver(locationReceiver);
            lbm.unregisterReceiver(pomodoroReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receivers: " + e.getMessage());
        }
    }
    
    /**
     * Bind to services
     */
    private void bindServices() {
        // Bind to location service
        Intent locationIntent = new Intent(getActivity(), LocationVerificationService.class);
        requireActivity().bindService(locationIntent, locationConnection, Context.BIND_AUTO_CREATE);
        
        // Bind to pomodoro service
        Intent pomodoroIntent = new Intent(getActivity(), PomodoroService.class);
        requireActivity().bindService(pomodoroIntent, pomodoroConnection, Context.BIND_AUTO_CREATE);
    }
    
    /**
     * Unbind from services
     */
    private void unbindServices() {
        // Unbind from location service
        if (locationServiceBound) {
            requireActivity().unbindService(locationConnection);
            locationServiceBound = false;
        }
        
        // Unbind from pomodoro service
        if (pomodoroServiceBound) {
            requireActivity().unbindService(pomodoroConnection);
            pomodoroServiceBound = false;
        }
    }
    
    /**
     * Service connection for location service
     */
    private final ServiceConnection locationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocationVerificationService.LocationVerificationBinder binder = 
                (LocationVerificationService.LocationVerificationBinder) service;
            locationService = binder.getService();
            locationServiceBound = true;
            
            // Start location tracking
            locationService.startLocationTracking();
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locationServiceBound = false;
        }
    };
    
    /**
     * Service connection for pomodoro service
     */
    private final ServiceConnection pomodoroConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            PomodoroService.PomodoroBinder binder = 
                (PomodoroService.PomodoroBinder) service;
            pomodoroService = binder.getService();
            pomodoroServiceBound = true;
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            pomodoroServiceBound = false;
        }
    };
    
    /**
     * Load habits from storage and server
     */
    private void loadHabits() {
        // Show loading state
        if (daysCountTextView != null) {
            daysCountTextView.setVisibility(View.VISIBLE);
        }
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        
        if (userId.isEmpty()) {
            // Handle case where user ID is not available
            Toast.makeText(requireContext(), "User ID not available", Toast.LENGTH_SHORT).show();
            if (daysCountTextView != null) {
                daysCountTextView.setVisibility(View.GONE);
            }
            return;
        }
        
        // Get today's date for filtering
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(calendar.getTime());
        
        // Load habits for today from server
        String endpoint = "get_habits.php?user_id=" + userId + "&date=" + today;
        
        VolleyNetworkManager networkManager = VolleyNetworkManager.getInstance(requireContext());
        networkManager.makeArrayGetRequest(endpoint, new VolleyNetworkManager.ArrayResponseListener() {
            @Override
            public void onSuccess(JSONArray response) {
                try {
                    List<Habit> habits = new ArrayList<>();
                    
                    // Parse the JSON array of habits
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject habitObj = response.getJSONObject(i);
                        Habit habit = parseHabit(habitObj);
                        
                        // Only add habits for today based on frequency and schedule
                        if (shouldShowHabitToday(habit, today)) {
                            habits.add(habit);
                        }
                    }
                    
                    // Update UI on main thread
                    requireActivity().runOnUiThread(() -> {
                        habitList.clear();
                        habitList.addAll(habits);
                        habitAdapter.notifyDataSetChanged();
                        updateEmptyState();
                        
                        // Update streak count
                        updateStreakCount();
                        
                        // Hide loading state
                        if (daysCountTextView != null) {
                            daysCountTextView.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing habits: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        if (daysCountTextView != null) {
                            daysCountTextView.setVisibility(View.GONE);
                        }
                        Toast.makeText(requireContext(), "Error loading habits: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String message) {
                Log.e(TAG, "Error loading habits: " + message);
                requireActivity().runOnUiThread(() -> {
                    if (daysCountTextView != null) {
                        daysCountTextView.setVisibility(View.GONE);
                    }
                    Toast.makeText(requireContext(), "Error loading habits: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Determine if a habit should be shown today based on its frequency
     * Updated to be less restrictive and match HomeFragment approach
     */
    private boolean shouldShowHabitToday(Habit habit, String today) {
        String frequency = habit.getFrequency();
        if (frequency == null) return true; // Default to showing if frequency not specified
        
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 2 = Monday, etc.
        
        switch (frequency.toLowerCase()) {
            case "daily":
                return true;
                
            case "weekly":
                // Show weekly habits every day - let user decide when to complete them
                return true;
                
            case "weekdays":
                // Show weekday habits Monday through Friday
                return dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY;
                
            case "weekends":
                // Show weekend habits Saturday and Sunday
                return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
                
            default:
                // For custom frequencies or unknown types, show them
                // This matches HomeFragment's permissive approach
                return true;
        }
    }
    
    /**
     * Parse a habit from JSON object
     */
    private Habit parseHabit(JSONObject habitObj) throws JSONException {
        Habit habit = new Habit();
        
        // Try different field names that might be used
        habit.setHabitId(habitObj.optString("habit_id", habitObj.optString("id", UUID.randomUUID().toString())));
        habit.setUserId(habitObj.optString("user_id", ""));
        habit.setTitle(habitObj.optString("title", ""));
        habit.setDescription(habitObj.optString("description", ""));
        
        // Default to checkbox verification if not specified
        String trustType = habitObj.optString("trust_type", "checkbox");
        if (trustType.equalsIgnoreCase("pomodoro")) {
            habit.setVerificationMethod(Habit.VERIFICATION_POMODORO);
            habit.setPomodoroLength(habitObj.optInt("pomodoro_duration", 25));
            habit.setPomodoroCount(habitObj.optInt("pomodoro_count", 1));
        } else if (trustType.equalsIgnoreCase("location") || trustType.equalsIgnoreCase("map")) {
            habit.setVerificationMethod(Habit.VERIFICATION_LOCATION);
            habit.setLatitude(habitObj.optDouble("map_lat", 0));
            habit.setLongitude(habitObj.optDouble("map_lon", 0));
            habit.setRadiusMeters(habitObj.optInt("radius_meters", 100));
        } else {
            habit.setVerificationMethod(Habit.VERIFICATION_CHECKBOX);
        }
        
        // Parse other fields
        habit.setCompleted(habitObj.optInt("completed", 0) == 1);
        habit.setCurrentStreak(habitObj.optInt("current_streak", 0));
        habit.setTotalCompletions(habitObj.optInt("total_completions", 0));
        habit.setFrequency(habitObj.optString("frequency", "daily"));
        
        // Parse reminder time
        if (habitObj.has("reminder_time")) {
            habit.setReminderTime(habitObj.getString("reminder_time"));
        }
        
        return habit;
    }
    
    /**
     * Load user streak count from server (similar to HomeFragment implementation)
     */
    private void updateStreakCount() {
        if (daysCountTextView == null) return;
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot load user streak data: User ID is missing.");
            // Fallback to cached streak count
            int cachedStreakCount = prefs.getInt("streak_count", 0);
            daysCountTextView.setText(String.format("%d", cachedStreakCount));
            daysCountTextView.setVisibility(View.VISIBLE);
            return;
        }

        Log.d(TAG, "Loading user streak data from server with ID: " + userId);

        // Calculate streak data range (today only for streak count)
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(cal.getTime());
        
        // Get endpoint from VolleyNetworkManager
        VolleyNetworkManager networkManager = VolleyNetworkManager.getInstance(requireContext());
        String endpoint = networkManager.getUserStreakUrl(userId, today, today);
        
        Log.i(TAG, "Loading user streak data with endpoint: " + endpoint);

        if (endpoint.isEmpty()) {
            Log.e(TAG, "Generated streak endpoint is empty. Using cached data.");
            // Fallback to cached streak count
            int cachedStreakCount = prefs.getInt("streak_count", 0);
            daysCountTextView.setText(String.format("%d", cachedStreakCount));
            daysCountTextView.setVisibility(View.VISIBLE);
            return;
        }

        // Use VolleyNetworkManager to make server request
        networkManager.makeGetRequest(endpoint, new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Successfully received streak data: " + response.toString().substring(0, Math.min(200, response.toString().length())));
                            
                            // Extract streak count from server response
                            int serverStreakCount = 0;
                            if (response.has("streak_count")) {
                                serverStreakCount = response.getInt("streak_count");
                            } else if (response.has("data") && response.getJSONObject("data").has("streak_count")) {
                                serverStreakCount = response.getJSONObject("data").getInt("streak_count");
                            } else {
                                Log.w(TAG, "No streak_count in response, using cached data");
                                // Fallback to cached data
                                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                                serverStreakCount = prefs.getInt("streak_count", 0);
                            }
                            
                            // Update UI with server streak count
                            if (daysCountTextView != null) {
                                daysCountTextView.setText(String.format("%d", serverStreakCount));
                                daysCountTextView.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Updated streak count UI to: " + serverStreakCount);
                            }
                            
                            // Save server streak count to cache
                            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            prefs.edit().putInt("streak_count", serverStreakCount).apply();
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing streak data: " + e.getMessage(), e);
                            // Fallback to cached data on error
                            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            int cachedStreakCount = prefs.getInt("streak_count", 0);
                            if (daysCountTextView != null) {
                                daysCountTextView.setText(String.format("%d", cachedStreakCount));
                                daysCountTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading streak data from server: " + errorMessage);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Fallback to cached streak count on network error
                        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        int cachedStreakCount = prefs.getInt("streak_count", 0);
                        if (daysCountTextView != null) {
                            daysCountTextView.setText(String.format("%d", cachedStreakCount));
                            daysCountTextView.setVisibility(View.VISIBLE);
                        }
                        Log.d(TAG, "Using cached streak count: " + cachedStreakCount);
                    });
                }
            }
        });
    }
    
    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (emptyStateText != null) {
            emptyStateText.setVisibility(habitList.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(habitList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
    
    /**
     * Check if any location-based habits can be verified with the given coordinates
     */
    private void checkLocationHabits(double latitude, double longitude) {
        // If no habits, return early
        if (habitList == null || habitList.isEmpty()) {
            return;
        }
        
        // For each habit, check if it has location verification
        for (Habit habit : habitList) {
            if (habit.getVerificationMethod().equals(Habit.VERIFICATION_LOCATION) && !habit.isCompleted()) {
                double habitLat = habit.getLatitude();
                double habitLng = habit.getLongitude();
                double radiusMeters = habit.getRadiusMeters();
                
                // If radius not set, use default of 100 meters
                if (radiusMeters <= 0) {
                    radiusMeters = 100;
                }
                
                // Calculate distance between current location and habit location
                float[] results = new float[1];
                android.location.Location.distanceBetween(latitude, longitude, habitLat, habitLng, results);
                float distance = results[0]; // distance in meters
                
                // If within radius, verify the habit
                if (distance <= radiusMeters) {
                    // Show success dialog
                    showLocationVerificationSuccessDialog(habit, distance);
                    
                    // Mark as verified
                    habitManager.verifyHabitWithLocation(habit.getHabitId());
                } else {
                    // Show failure dialog
                    showLocationVerificationFailureDialog(habit, distance);
                }
            }
        }
    }
    
    /**
     * Show dialog when location verification succeeds
     */
    private void showLocationVerificationSuccessDialog(Habit habit, float distance) {
        if (getContext() == null) return;
        
        // Inflate the custom location verification layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.location_verification_layout, null);
        
        // Find and populate the views
        TextView titleView = dialogView.findViewById(R.id.location_verification_title);
        TextView descriptionView = dialogView.findViewById(R.id.location_verification_description);
        Button verifyButton = dialogView.findViewById(R.id.verify_location_button);
        
        // Set the content
        if (titleView != null) {
            titleView.setText(habit.getTitle());
        }
        if (descriptionView != null) {
            descriptionView.setText(String.format(
                "Great! You are within %.1f meters of your habit location. Keep up the good work!", 
                distance));
        }
        if (verifyButton != null) {
            verifyButton.setText("Awesome!");
        }
        
        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
            
        // Set up button click
        if (verifyButton != null) {
            verifyButton.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }
    
    /**
     * Show dialog when location verification fails
     */
    private void showLocationVerificationFailureDialog(Habit habit, float distance) {
        if (getContext() == null) return;
        
        // Inflate the custom location verification layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.location_verification_layout, null);
        
        // Find and populate the views
        TextView titleView = dialogView.findViewById(R.id.location_verification_title);
        TextView descriptionView = dialogView.findViewById(R.id.location_verification_description);
        Button verifyButton = dialogView.findViewById(R.id.verify_location_button);
        
        // Set the content for failure case
        if (titleView != null) {
            titleView.setText(habit.getTitle());
        }
        if (descriptionView != null) {
            descriptionView.setText(String.format(
                "You are %.1f meters away from your habit location. You need to be within %d meters to verify this habit.", 
                distance, (int)habit.getRadiusMeters()));
        }
        if (verifyButton != null) {
            verifyButton.setText("Got it");
        }
        
        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
            
        // Set up button click
        if (verifyButton != null) {
            verifyButton.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }
    
    /**
     * Show dialog for pomodoro verification
     */
    private void showPomodoroVerificationDialog(Habit habit) {
        if (getContext() == null) return;
        
        // Inflate the custom pomodoro verification layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.pomodoro_verification_layout, null);
        
        // Find and populate the views
        TextView titleView = dialogView.findViewById(R.id.pomodoro_verification_title);
        TextView descriptionView = dialogView.findViewById(R.id.pomodoro_verification_description);
        Button verifyButton = dialogView.findViewById(R.id.verify_pomodoro_button);
        
        // Set the content
        if (titleView != null) {
            titleView.setText(habit.getTitle());
        }
        if (descriptionView != null) {
            int duration = habit.getPomodoroLength() > 0 ? habit.getPomodoroLength() : 25;
            descriptionView.setText(String.format(
                "Complete a %d-minute focused work session to verify this habit.", 
                duration));
        }
        if (verifyButton != null) {
            verifyButton.setText("Start Pomodoro");
        }
        
        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
            
        // Set up button click to start pomodoro
        if (verifyButton != null) {
            verifyButton.setOnClickListener(v -> {
                dialog.dismiss();
                // Launch the pomodoro activity
                if (pomodoroService != null) {
                    Intent intent = new Intent(getContext(), PomodoroActivity.class);
                    intent.putExtra("habit_id", habit.getHabitId());
                    startActivity(intent);
                }
            });
        }
        
        dialog.show();
    }
    
    /**
     * Show dialog for checkbox verification
     */
    private void showCheckboxVerificationDialog(Habit habit) {
        if (getContext() == null) return;
        
        // Inflate the custom checkbox verification layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.checkbox_verification_layout, null);
        
        // Find and populate the views
        TextView titleView = dialogView.findViewById(R.id.checkbox_verification_title);
        TextView descriptionView = dialogView.findViewById(R.id.checkbox_verification_description);
        Button verifyButton = dialogView.findViewById(R.id.verify_checkbox_button);
        
        // Set the content
        if (titleView != null) {
            titleView.setText(habit.getTitle());
        }
        if (descriptionView != null) {
            descriptionView.setText("Mark this habit as completed for today. Honor system - be honest with yourself!");
        }
        if (verifyButton != null) {
            verifyButton.setText("Mark Complete");
        }
        
        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();
            
        // Set up button click to mark as complete
        if (verifyButton != null) {
            verifyButton.setOnClickListener(v -> {
                dialog.dismiss();
                habitManager.verifyHabitWithCheckbox(habit.getHabitId(), true);
            });
        }
        
        dialog.show();
    }

    // HabitManagerService.HabitListener implementation
    
    @Override
    public void onHabitsLoaded(List<Habit> habits) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Log.d(TAG, "onHabitsLoaded called with " + habits.size() + " habits");
                
                habitList.clear();
                
                // Filter to show only today's habits based on frequency
                String today = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(Calendar.getInstance().getTime());
                List<Habit> todaysHabits = new ArrayList<>();
                for (Habit habit : habits) {
                    Log.d(TAG, "Processing habit: " + habit.getTitle() + " with verification method: " + habit.getVerificationMethod());
                    if (shouldShowHabitToday(habit, today)) {
                        todaysHabits.add(habit);
                    }
                }
                
                habitList.addAll(todaysHabits);
                habitAdapter.notifyDataSetChanged();
                updateEmptyState();
                updateStreakCount();
                
                // Show completion toast for refresh operation
                Toast.makeText(getContext(), "Habits refreshed successfully (" + todaysHabits.size() + " today)", Toast.LENGTH_SHORT).show();
                
                // Hide loading state
                if (daysCountTextView != null) {
                    daysCountTextView.setVisibility(View.VISIBLE);
                }
            });
        }
    }
    
    @Override
    public void onHabitAdded(Habit habit) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Add the new habit to the list
                habitList.add(habit);
                habitAdapter.notifyItemInserted(habitList.size() - 1);
                updateEmptyState();
            });
        }
    }
    
    @Override
    public void onHabitUpdated(Habit habit) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Find and update the habit in the list
                for (int i = 0; i < habitList.size(); i++) {
                    if (habitList.get(i).getHabitId().equals(habit.getHabitId())) {
                        habitList.set(i, habit);
                        habitAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            });
        }
    }
    
    @Override
    public void onHabitVerified(Habit habit, boolean isCompleted) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Find and update the habit in the list
                for (int i = 0; i < habitList.size(); i++) {
                    if (habitList.get(i).getHabitId().equals(habit.getHabitId())) {
                        habitList.set(i, habit);
                        habitAdapter.notifyItemChanged(i);
                        
                        // Show a toast
                        String message = isCompleted ? 
                            "Habit \"" + habit.getTitle() + "\" completed!" : 
                            "Habit \"" + habit.getTitle() + "\" marked as incomplete";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            });
        }
    }
    
    @Override
    public void onHabitStreakUpdated(String habitId, int newStreak) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Find and update the habit in the list
                for (int i = 0; i < habitList.size(); i++) {
                    if (habitList.get(i).getHabitId().equals(habitId)) {
                        Habit habit = habitList.get(i);
                        habit.setCurrentStreak(newStreak);
                        habitList.set(i, habit);
                        habitAdapter.notifyItemChanged(i);
                        break;
                    }
                }
                
                // Get today's date for comparison
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = dateFormat.format(calendar.getTime());
                
                // Get SharedPreferences for user streak data
                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                String lastLoginDate = prefs.getString("last_login_date", "");
                int currentStreakCount = prefs.getInt("streak_count", 0);

                // Check if this is a new day (different from the last login)
                if (!today.equals(lastLoginDate)) {
                    // New day login detected, increment streak count
                    currentStreakCount++;
                    
                    // Update preferences with new streak and login date
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("streak_count", currentStreakCount);
                    editor.putString("last_login_date", today);
                    
                    // Also mark that user logged in today
                    editor.putBoolean("logged_in_today", true);
                    editor.apply();
                    
                    Log.d(TAG, "New day login detected! Incrementing streak count to: " + currentStreakCount);
                    
                    // Update UI with new streak count
                    if (daysCountTextView != null) {
                        daysCountTextView.setText(String.format("%d", currentStreakCount));
                    }
                    
                    // Notify the user about the streak increase
                    if (getContext() != null) {
                        Toast.makeText(getContext(), 
                            "You're on a " + currentStreakCount + " day streak! Keep it up!", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    @Override
    public void onProgressUpdated(float overallProgress, Map<String, Float> habitProgress) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Update progress bar
                if (daysCountTextView != null) {
                    daysCountTextView.setText(String.format("%.0f%%", overallProgress));
                }
                
                // Update individual habit progress in adapter
                habitAdapter.updateProgress(habitProgress);
            });
        }
    }
    
    @Override
    public void onError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Hide loading state
                if (daysCountTextView != null) {
                    daysCountTextView.setVisibility(View.GONE);
                }
                
                // Show error message
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    // HabitAdapter.HabitInteractionListener implementation
    
    @Override
    public void onHabitChecked(String habitId, boolean isChecked) {
        // Verify the habit using checkbox method
        habitManager.verifyHabitWithCheckbox(habitId, isChecked);
    }
    
    @Override
    public void onLocationVerifyClicked(String habitId) {
        Habit habit = habitManager.getHabit(habitId);
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
        if (pomodoroService != null) {
            Habit habit = habitManager.getHabit(habitId);
            if (habit != null) {
                // Launch the pomodoro activity
                Intent intent = new Intent(getContext(), PomodoroActivity.class);
                intent.putExtra("habit_id", habitId);
                startActivity(intent);
            }
        }
    }
    
    @Override
    public void onPomodoroPauseClicked(String habitId) {
        if (pomodoroService != null) {
            pomodoroService.pauseTimer();
        }
    }
    
    @Override
    public void onPomodoroResumeClicked(String habitId) {
        if (pomodoroService != null) {
            pomodoroService.resumeTimer();
        }
    }
    
    @Override
    public void onPomodoroStopClicked(String habitId) {
        if (pomodoroService != null) {
            pomodoroService.stopTimer();
            habitAdapter.setPomodoroActive(habitId, false);
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        
        // Apply scroll optimizations (same pattern as TaskFragment and HomeFragment)
        optimizeRecyclerViewInScrollView(recyclerView, 15);
        
        // Enable layout manager optimizations
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(3);
        
        habitList = new ArrayList<>();
        habitAdapter = new HabitAdapter(habitList, this, getContext());
        recyclerView.setAdapter(habitAdapter);
        
        Log.d(TAG, "RecyclerView setup completed with optimized scrolling");
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
            // Quick habit refresh without heavy operations
            loadHabits();
            
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
                    
                    // CRITICAL FIX: Disable nested scrolling to prevent scroll lock-up
                    // This allows the ScrollView to handle all scroll events consistently
                    scrollView.setNestedScrollingEnabled(false);
                    
                    // Disable scroll bars to prevent visual conflicts
                    scrollView.setScrollbarFadingEnabled(true);
                }
            }
        }
        
        // Optimize the main RecyclerView
        optimizeRecyclerViewInScrollView(recyclerView, 10);
        
        Log.d(TAG, "Scrolling performance optimizations applied successfully");
    }
    
    /**
     * Helper method to optimize individual RecyclerViews within ScrollView
     */
    private void optimizeRecyclerViewInScrollView(RecyclerView recyclerView, int cacheSize) {
        if (recyclerView == null) return;
        
        // CRITICAL FIX: Disable nested scrolling to prevent conflicts with parent ScrollView
        recyclerView.setNestedScrollingEnabled(false);
        
        // Performance optimizations - use same pattern as TaskFragment
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
        
        // CRITICAL FIX: Prevent focus conflicts that can cause scroll lock-up
        recyclerView.setFocusable(false);
        recyclerView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        
        // Additional fix: Prevent touch event conflicts at list boundaries
        recyclerView.setOnTouchListener((v, event) -> {
            // Allow parent ScrollView to handle touch events when RecyclerView can't scroll
            v.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });
    }

    /**
     * Public method to refresh fragment data
     * Called from MainNavigationActivity when data needs to be updated
     */
    public void refreshData() {
        refreshAllDataFast();
    }
}