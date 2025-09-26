package com.example.shedulytic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

/**
 * Test activity to verify VolleyNetworkManager connections
 */
public class NetworkTestActivity extends AppCompatActivity {
    private static final String TAG = "NetworkTestActivity";
    private VolleyNetworkManager networkManager;
    private TextView statusTextView;
    private ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_test);
        
        // Initialize UI components
        statusTextView = findViewById(R.id.textView_status);
        progressBar = findViewById(R.id.progressBar);
        Button testConnectionButton = findViewById(R.id.button_test_connection);
        Button testStreakButton = findViewById(R.id.button_test_streak);
        Button testTasksButton = findViewById(R.id.button_test_tasks);
        
        // Initialize network manager
        networkManager = VolleyNetworkManager.getInstance(this);
        
        // Set button click listeners
        testConnectionButton.setOnClickListener(v -> testConnection());
        testStreakButton.setOnClickListener(v -> testStreakData());
        testTasksButton.setOnClickListener(v -> testTasksData());
    }
    
    /**
     * Test basic server connection
     */
    private void testConnection() {
        showProgress("Testing server connection...");
        
        // Find working server URL
        networkManager.findWorkingServerUrl(() -> {
            // Test ping endpoint
            testPingEndpoint();
        });
    }
    
    /**
     * Test ping endpoint
     */
    private void testPingEndpoint() {
        networkManager.makeGetRequest("ping.php", new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgress();
                updateStatus("Connection successful: " + response.toString());
                Toast.makeText(NetworkTestActivity.this, "Connection successful!", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String errorMessage) {
                hideProgress();
                updateStatus("Connection failed: " + errorMessage);
                Toast.makeText(NetworkTestActivity.this, "Connection failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Test streak data endpoint
     */
    private void testStreakData() {
        showProgress("Testing streak data...");
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        
        if (userId.isEmpty()) {
            hideProgress();
            updateStatus("Error: User ID is not available. Please log in first.");
            return;
        }
        
        // Get current date and 30 days ago
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Date today = new java.util.Date();
        java.util.Date startDate = new java.util.Date(today.getTime() - 29 * 24 * 60 * 60 * 1000L);
        
        String startDateStr = dateFormat.format(startDate);
        String endDateStr = dateFormat.format(today);
        
        // Get streak data endpoint
        String endpoint = networkManager.getUserStreakUrl(userId, startDateStr, endDateStr);
        
        // Make the request
        networkManager.makeGetRequest(endpoint, new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgress();
                updateStatus("Streak data loaded successfully: " + response.toString());
                Toast.makeText(NetworkTestActivity.this, "Streak data loaded!", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String errorMessage) {
                hideProgress();
                updateStatus("Error loading streak data: " + errorMessage);
                Toast.makeText(NetworkTestActivity.this, "Streak data error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Test tasks data endpoint
     */
    private void testTasksData() {
        showProgress("Testing tasks data...");
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        
        if (userId.isEmpty()) {
            hideProgress();
            updateStatus("Error: User ID is not available. Please log in first.");
            return;
        }
        
        // Get current date
        String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        
        // Get tasks endpoint
        String endpoint = networkManager.getTodayTasksUrl(userId, currentDate);
        
        // Make the request
        networkManager.makeGetRequest(endpoint, new VolleyNetworkManager.JsonResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgress();
                updateStatus("Tasks data loaded successfully: " + response.toString());
                Toast.makeText(NetworkTestActivity.this, "Tasks data loaded!", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String errorMessage) {
                hideProgress();
                updateStatus("Error loading tasks data: " + errorMessage);
                Toast.makeText(NetworkTestActivity.this, "Tasks data error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Show progress indicator
     */
    private void showProgress(String message) {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus(message);
    }
    
    /**
     * Hide progress indicator
     */
    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }
    
    /**
     * Update status text view
     */
    private void updateStatus(String message) {
        Log.d(TAG, message);
        statusTextView.setText(message);
    }
} 