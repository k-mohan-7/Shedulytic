package com.example.shedulytic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private TextView usernameTextView;
    private TextView pointsTextView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
          // Initialize views
        usernameTextView = view.findViewById(R.id.username_text);
        pointsTextView = view.findViewById(R.id.points_text);
        
        // Load user data from SharedPreferences
        loadUserData();
        
        // Apply performance optimizations
        optimizeViewPerformance();
        
        return view;
    }
      @Override
    public void onResume() {
        super.onResume();
        // Refresh user data when fragment becomes visible
        loadUserData();
        
        // Re-apply performance optimizations
        optimizeViewPerformance();
    }
    
    private void loadUserData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        
        // Get username
        String username = prefs.getString("username", "User");
        if (usernameTextView != null) {
            usernameTextView.setText(username);
        }
        
        // Get XP points
        int xpPoints = prefs.getInt("xp_points", 0);
        if (pointsTextView != null) {
            pointsTextView.setText(String.valueOf(xpPoints));
        }    }
    
    /**
     * Optimizes view performance and prevents potential scroll issues
     */
    private void optimizeViewPerformance() {
        if (getView() == null) return;
        
        try {
            // Apply general performance optimizations
            View rootView = getView();
            if (rootView != null) {
                // Disable hardware acceleration for complex views if needed
                // rootView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                
                // Optimize drawing cache for better performance
                rootView.setDrawingCacheEnabled(true);
                rootView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);
                
                Log.d(TAG, "ProfileFragment view performance optimizations applied");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error optimizing view performance: " + e.getMessage(), e);
        }
    }

    /**
     * Public method to refresh fragment data
     * Called from MainNavigationActivity when data needs to be updated
     */
    public void refreshData() {
        loadUserData();
    }
}
