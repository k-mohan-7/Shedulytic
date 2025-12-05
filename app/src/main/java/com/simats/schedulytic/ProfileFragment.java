package com.simats.schedulytic;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    
    // Header views
    private TextView usernameTextView;
    private TextView pointsTextView;
    private TextView currentLevelBadge;
    
    // XP Breakdown cards
    private TextView workflowPointsText;
    private TextView workflowTaskCountText;
    private TextView remainderPointsText;
    private TextView remainderTaskCountText;
    private TextView habitPointsText;
    private TextView habitCountText;
    
    // Level progress views
    private ProgressBar levelOneProgress;
    private TextView levelOneProgressText;
    private TextView levelOneStatus;
    private ImageView levelOneIcon;
    
    private ProgressBar levelTwoProgress;
    private TextView levelTwoProgressText;
    private TextView levelTwoStatus;
    private ImageView levelTwoIcon;
    
    private ProgressBar levelThreeProgress;
    private TextView levelThreeProgressText;
    private TextView levelThreeStatus;
    private ImageView levelThreeIcon;
    
    // Level thresholds
    private static final int LEVEL_1_XP = 100;
    private static final int LEVEL_2_XP = 250;
    private static final int LEVEL_3_XP = 500;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Initialize header views
        usernameTextView = view.findViewById(R.id.username_text);
        pointsTextView = view.findViewById(R.id.points_text);
        currentLevelBadge = view.findViewById(R.id.current_level_badge);
        
        // Initialize XP breakdown views
        workflowPointsText = view.findViewById(R.id.workflow_points);
        workflowTaskCountText = view.findViewById(R.id.workflow_task_count);
        remainderPointsText = view.findViewById(R.id.remainder_points);
        remainderTaskCountText = view.findViewById(R.id.remainder_task_count);
        habitPointsText = view.findViewById(R.id.habit_points);
        habitCountText = view.findViewById(R.id.habit_count);
        
        // Initialize level progress views
        levelOneProgress = view.findViewById(R.id.level_one_progress);
        levelOneProgressText = view.findViewById(R.id.level_one_progress_text);
        levelOneStatus = view.findViewById(R.id.level_one_status);
        levelOneIcon = view.findViewById(R.id.level_one_icon);
        
        levelTwoProgress = view.findViewById(R.id.level_two_progress);
        levelTwoProgressText = view.findViewById(R.id.level_two_progress_text);
        levelTwoStatus = view.findViewById(R.id.level_two_status);
        levelTwoIcon = view.findViewById(R.id.level_two_icon);
        
        levelThreeProgress = view.findViewById(R.id.level_three_progress);
        levelThreeProgressText = view.findViewById(R.id.level_three_progress_text);
        levelThreeStatus = view.findViewById(R.id.level_three_status);
        levelThreeIcon = view.findViewById(R.id.level_three_icon);
        
        // Set up click listeners for cards
        setupCardClickListeners(view);
        
        // Load user data
        loadUserData();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // First load from cache
        loadUserData();
        // Then fetch latest from server
        fetchXPFromServer();
    }
    
    /**
     * Fetch latest XP from server to ensure ProfileFragment shows current data
     */
    private void fetchXPFromServer() {
        if (!isAdded()) return;
        
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "No user ID found, skipping server fetch");
            return;
        }
        
        String url = VolleyNetworkManager.getInstance(requireContext()).getBaseUrl() + "get_user_profile.php?user_id=" + userId;
        
        VolleyNetworkManager.getInstance(requireContext()).makeGetRequest(
                url,
                new VolleyNetworkManager.JsonResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            if (response.has("status") && "success".equals(response.getString("status"))) {
                                int xpPoints = 0;
                                if (response.has("xp_points")) {
                                    xpPoints = response.getInt("xp_points");
                                } else if (response.has("user") && response.getJSONObject("user").has("xp_points")) {
                                    xpPoints = response.getJSONObject("user").getInt("xp_points");
                                }
                                
                                // Update SharedPreferences with latest XP
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("xp_points", xpPoints);
                                editor.putInt("xp_coins", xpPoints);
                                editor.putFloat("xp_points_float", (float) xpPoints);
                                editor.apply();
                                
                                Log.d(TAG, "Fetched XP from server: " + xpPoints);
                                
                                // Refresh UI
                                if (isAdded()) {
                                    loadUserData();
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing XP from server: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error fetching XP from server: " + message);
                    }
                }
        );
    }
    
    private void setupCardClickListeners(View view) {
        // Workflow card click
        CardView workflowCard = view.findViewById(R.id.workflow_card);
        if (workflowCard != null) {
            workflowCard.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Workflow tasks give +2.5 XP each", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Remainder card click
        CardView remainderCard = view.findViewById(R.id.remainder_card);
        if (remainderCard != null) {
            remainderCard.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Reminder tasks give +2.0 XP each", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Habit card click
        CardView habitCard = view.findViewById(R.id.habit_card);
        if (habitCard != null) {
            habitCard.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Habits give +1.0 XP each", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Level cards click for info
        CardView levelOneCard = view.findViewById(R.id.level_one_card);
        if (levelOneCard != null) {
            levelOneCard.setOnClickListener(v -> showLevelInfo(1));
        }
        
        CardView levelTwoCard = view.findViewById(R.id.level_two_card);
        if (levelTwoCard != null) {
            levelTwoCard.setOnClickListener(v -> showLevelInfo(2));
        }
        
        CardView levelThreeCard = view.findViewById(R.id.level_three_card);
        if (levelThreeCard != null) {
            levelThreeCard.setOnClickListener(v -> showLevelInfo(3));
        }
    }
    
    private void showLevelInfo(int level) {
        String message;
        switch (level) {
            case 1:
                message = "Level I - Beginner\nReach " + LEVEL_1_XP + " XP to complete\nReward: Custom themes unlocked!";
                break;
            case 2:
                message = "Level II - Intermediate\nReach " + LEVEL_2_XP + " XP to complete\nReward: Achievement badges unlocked!";
                break;
            case 3:
                message = "Level III - Expert\nReach " + LEVEL_3_XP + " XP to complete\nReward: Premium features unlocked!";
                break;
            default:
                message = "Keep earning XP to unlock levels!";
        }
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
    
    private void loadUserData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        
        // Get username
        String username = prefs.getString("username", "User");
        if (usernameTextView != null) {
            usernameTextView.setText(username);
        }
        
        // Get XP points (stored as float for decimals like 2.5)
        float totalXP = prefs.getFloat("xp_points_float", 0f);
        // Also check int version for backward compatibility
        if (totalXP == 0) {
            totalXP = prefs.getInt("xp_points", 0);
        }
        
        // Get breakdown XP
        float workflowXP = prefs.getFloat("workflow_xp", 0f);
        float remainderXP = prefs.getFloat("remainder_xp", 0f);
        float habitXP = prefs.getFloat("habit_xp", 0f);
        
        // Get task counts
        int workflowCount = prefs.getInt("workflow_completed_count", 0);
        int remainderCount = prefs.getInt("remainder_completed_count", 0);
        int habitCount = prefs.getInt("habit_completed_count", 0);
        
        // Update total XP display
        if (pointsTextView != null) {
            pointsTextView.setText(String.format("%.1f XP", totalXP));
        }
        
        // Update current level badge
        updateCurrentLevelBadge(totalXP);
        
        // Update XP breakdown
        if (workflowPointsText != null) {
            workflowPointsText.setText(String.format("+%.1f XP", workflowXP));
        }
        if (workflowTaskCountText != null) {
            workflowTaskCountText.setText(workflowCount + " tasks completed");
        }
        
        if (remainderPointsText != null) {
            remainderPointsText.setText(String.format("+%.1f XP", remainderXP));
        }
        if (remainderTaskCountText != null) {
            remainderTaskCountText.setText(remainderCount + " tasks completed");
        }
        
        if (habitPointsText != null) {
            habitPointsText.setText(String.format("+%.1f XP", habitXP));
        }
        if (habitCountText != null) {
            habitCountText.setText(habitCount + " habits completed");
        }
        
        // Update level progress
        updateLevelProgress(totalXP);
    }
    
    private void updateCurrentLevelBadge(float xp) {
        if (currentLevelBadge == null) return;
        
        String levelText;
        int badgeColor;
        
        if (xp >= LEVEL_3_XP) {
            levelText = "EXPERT";
            badgeColor = R.drawable.level_badge_expert;
        } else if (xp >= LEVEL_2_XP) {
            levelText = "INTERMEDIATE";
            badgeColor = R.drawable.level_badge_intermediate;
        } else if (xp >= LEVEL_1_XP) {
            levelText = "BEGINNER+";
            badgeColor = R.drawable.level_badge_beginner_plus;
        } else {
            levelText = "BEGINNER";
            badgeColor = R.drawable.level_badge_background;
        }
        
        currentLevelBadge.setText(levelText);
        try {
            currentLevelBadge.setBackgroundResource(badgeColor);
        } catch (Exception e) {
            // Use default if specific badge doesn't exist
            currentLevelBadge.setBackgroundResource(R.drawable.level_badge_background);
        }
    }
    
    private void updateLevelProgress(float totalXP) {
        // Level 1 Progress (0 to LEVEL_1_XP)
        updateLevelCard(1, totalXP, 0, LEVEL_1_XP, 
                levelOneProgress, levelOneProgressText, levelOneStatus, levelOneIcon);
        
        // Level 2 Progress (LEVEL_1_XP to LEVEL_2_XP)
        updateLevelCard(2, totalXP, LEVEL_1_XP, LEVEL_2_XP,
                levelTwoProgress, levelTwoProgressText, levelTwoStatus, levelTwoIcon);
        
        // Level 3 Progress (LEVEL_2_XP to LEVEL_3_XP)
        updateLevelCard(3, totalXP, LEVEL_2_XP, LEVEL_3_XP,
                levelThreeProgress, levelThreeProgressText, levelThreeStatus, levelThreeIcon);
    }
    
    private void updateLevelCard(int level, float totalXP, int minXP, int maxXP,
                                  ProgressBar progressBar, TextView progressText,
                                  TextView statusText, ImageView icon) {
        if (progressBar == null || progressText == null || statusText == null) return;
        
        int xpRange = maxXP - minXP;
        float xpInLevel = Math.max(0, Math.min(totalXP - minXP, xpRange));
        int progressPercent = (int) ((xpInLevel / xpRange) * 100);
        
        // Animate progress bar
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressPercent);
        animation.setDuration(1000);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
        
        if (totalXP >= maxXP) {
            // Level completed
            progressText.setText("Completed! ✓");
            statusText.setText("DONE");
            statusText.setBackgroundResource(R.drawable.status_badge_completed);
            if (icon != null) {
                icon.setAlpha(1.0f);
            }
            progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.level_progress_drawable, null));
        } else if (totalXP >= minXP) {
            // Level active (in progress)
            progressText.setText(String.format("%.0f / %d XP", xpInLevel + minXP, maxXP));
            statusText.setText("ACTIVE");
            statusText.setBackgroundResource(R.drawable.status_badge_active);
            if (icon != null) {
                icon.setAlpha(1.0f);
            }
            progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.level_progress_drawable, null));
        } else {
            // Level locked
            progressText.setText("Requires " + minXP + " XP to unlock");
            statusText.setText("LOCKED");
            statusText.setBackgroundResource(R.drawable.status_badge_locked);
            if (icon != null) {
                icon.setAlpha(0.5f);
            }
            progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.level_progress_drawable_locked, null));
        }
    }
    
    /**
     * Public method to refresh fragment data
     */
    public void refreshData() {
        loadUserData();
        fetchXPFromServer();
    }
}
