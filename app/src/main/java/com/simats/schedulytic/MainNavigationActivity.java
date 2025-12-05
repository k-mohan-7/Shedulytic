package com.simats.schedulytic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainNavigationActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;
    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View dimBackground;
    
    private FragmentManager fragmentManager;
    private Fragment activeFragment;
    
    // Fragments
    private HomeFragment homeFragment;
    private TaskFragment taskFragment;
    private HabitFragment habitFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nav);

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        initFragments();
        setupBottomNavigation();
        setupFAB();
        setupBottomSheet();
        
        // Request notification permission for Android 13+
        checkNotificationPermission();
    }
    
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Request notification permission
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        1001);
            }
        }
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);
        bottomSheet = findViewById(R.id.bottom_sheet_container);
        dimBackground = findViewById(R.id.dim_background);
        fragmentManager = getSupportFragmentManager();
    }

    private void initFragments() {
        // Create fragment instances
        homeFragment = new HomeFragment();
        taskFragment = new TaskFragment();
        habitFragment = new HabitFragment();
        profileFragment = new ProfileFragment();

        // Add all fragments but hide all except home
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment);
        transaction.add(R.id.fragment_container, habitFragment, "habit").hide(habitFragment);
        transaction.add(R.id.fragment_container, taskFragment, "task").hide(taskFragment);
        transaction.add(R.id.fragment_container, homeFragment, "home");
        transaction.commit();

        activeFragment = homeFragment;
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Ignore placeholder item click
            if (itemId == R.id.nav_placeholder) {
                return false;
            }

            if (itemId == R.id.nav_home) {
                selectedFragment = homeFragment;
            } else if (itemId == R.id.nav_task) {
                selectedFragment = taskFragment;
            } else if (itemId == R.id.nav_habit) {
                selectedFragment = habitFragment;
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = profileFragment;
            }

            if (selectedFragment != null && selectedFragment != activeFragment) {
                switchFragment(selectedFragment);
            }
            return true;
        });
    }

    private void switchFragment(Fragment fragment) {
        fragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit();
        activeFragment = fragment;
    }

    private void setupFAB() {
        fabAdd.setOnClickListener(v -> {
            showBottomSheet();
        });
    }

    private void setupBottomSheet() {
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setSkipCollapsed(true);

            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        if (dimBackground != null) {
                            dimBackground.setVisibility(View.GONE);
                        }
                        fabAdd.show();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if (dimBackground != null) {
                        // Adjust dim background alpha based on slide offset
                        float alpha = Math.max(0, slideOffset) * 0.5f;
                        dimBackground.setAlpha(alpha);
                    }
                }
            });

            // Dim background click listener
            if (dimBackground != null) {
                dimBackground.setOnClickListener(v -> hideBottomSheet());
            }

            // Setup bottom sheet option clicks
            setupBottomSheetOptions();
        }
    }

    private void setupBottomSheetOptions() {
        // Create Reminder option
        LinearLayout optionReminder = findViewById(R.id.option_reminder);
        if (optionReminder != null) {
            optionReminder.setOnClickListener(v -> {
                hideBottomSheet();
                openAddTaskScreen("reminder");
            });
        }

        // Create Workflow option
        LinearLayout optionWorkflow = findViewById(R.id.option_workflow);
        if (optionWorkflow != null) {
            optionWorkflow.setOnClickListener(v -> {
                hideBottomSheet();
                openAddTaskScreen("workflow");
            });
        }

        // Create Habit option
        LinearLayout optionHabit = findViewById(R.id.option_habit);
        if (optionHabit != null) {
            optionHabit.setOnClickListener(v -> {
                hideBottomSheet();
                openAddHabitScreen();
            });
        }

        // Cancel button
        TextView btnCancel = findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> hideBottomSheet());
        }
    }

    private void showBottomSheet() {
        if (bottomSheetBehavior != null) {
            if (dimBackground != null) {
                dimBackground.setVisibility(View.VISIBLE);
            }
            fabAdd.hide();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void hideBottomSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void openAddTaskScreen(String taskType) {
        // Navigate to add task activity with task type
        android.content.Intent intent = new android.content.Intent(this, AddTaskActivity.class);
        intent.putExtra("task_type", taskType);
        startActivity(intent);
    }

    private void openAddHabitScreen() {
        // Navigate to add habit activity
        android.content.Intent intent = new android.content.Intent(this, AddHabitActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // If bottom sheet is open, close it first
        if (bottomSheetBehavior != null && 
            bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            hideBottomSheet();
            return;
        }

        // If not on home tab, go to home tab
        if (activeFragment != homeFragment) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            return;
        }

        // Otherwise, exit app
        super.onBackPressed();
    }

    /**
     * Call this method to refresh data in all fragments
     */
    public void refreshAllFragments() {
        if (homeFragment != null && homeFragment.isAdded()) {
            homeFragment.refreshData();
        }
        if (taskFragment != null && taskFragment.isAdded()) {
            taskFragment.refreshData();
        }
        if (habitFragment != null && habitFragment.isAdded()) {
            habitFragment.refreshData();
        }
        if (profileFragment != null && profileFragment.isAdded()) {
            profileFragment.refreshData();
        }
    }

    /**
     * Navigate to a specific tab programmatically
     */
    public void navigateToTab(int tabId) {
        bottomNav.setSelectedItemId(tabId);
    }
}
