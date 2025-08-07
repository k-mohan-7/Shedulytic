package com.schedlytic.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.shedulytic.R;
import com.schedlytic.app.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddHabitActivity extends AppCompatActivity {
    private static final String TAG = "AddHabitActivity";
    
    private EditText habitTitleInput;
    private EditText habitDescriptionInput;
    private Spinner habitFrequencySpinner;    private LinearLayout btnTrustCheckbox, btnTrustPomodoro, btnTrustLocation;
    private View checkboxIndicator, pomodoroIndicator, locationIndicator;
    private LinearLayout checkboxVerificationContainer, locationVerificationContainer, pomodoroVerificationContainer;
    private EditText pomodoroCountEditText;
    private EditText pomodoroDurationEditText;
    private TextView locationCoordinatesText;
    private Button selectLocationButton;
    private Button cancelButton;
    private Button saveHabitButton;

    private RequestQueue requestQueue;
    private String selectedTrustType = "checkbox"; // Default
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private int userId;

    @Override    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_habit);
        
        // Initialize components
        initializeViews();
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);
        
        // Get user ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", -1);
        
        if (userId == -1) {
            // Handle case where user is not logged in
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set up listeners
        setupListeners();
        
        // Set up verification method selection
        setupVerificationMethodSelection();    }
    
    private void initializeViews() {
        habitTitleInput = findViewById(R.id.habit_title_edit_text);
        habitDescriptionInput = findViewById(R.id.habit_description_edit_text);
        habitFrequencySpinner = findViewById(R.id.habit_frequency_spinner);
        
        btnTrustCheckbox = findViewById(R.id.btn_trust_checkbox);
        btnTrustPomodoro = findViewById(R.id.btn_trust_pomodoro);
        btnTrustLocation = findViewById(R.id.btn_trust_location);
          checkboxIndicator = findViewById(R.id.checkbox_indicator);
        pomodoroIndicator = findViewById(R.id.pomodoro_indicator);
        locationIndicator = findViewById(R.id.location_indicator);
        
        checkboxVerificationContainer = findViewById(R.id.checkbox_verification_container);
        locationVerificationContainer = findViewById(R.id.location_verification_container);
        pomodoroVerificationContainer = findViewById(R.id.pomodoro_verification_container);
        
        pomodoroCountEditText = findViewById(R.id.pomodoro_count_edit_text);
        pomodoroDurationEditText = findViewById(R.id.pomodoro_duration_edit_text);
          locationCoordinatesText = findViewById(R.id.location_coordinates_text);
        selectLocationButton = findViewById(R.id.select_location_button);
        
        cancelButton = findViewById(R.id.cancel_button);
        saveHabitButton = findViewById(R.id.save_habit_button);
    }
    
    private void setupVerificationMethodSelection() {
        // Set default selection
        selectedTrustType = "checkbox";
        updateTrustTypeSelection();    }
    
    private void setupListeners() {
        // Trust type selection
        btnTrustCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTrustType = "checkbox";
                updateTrustTypeSelection();
            }
        });
        
        btnTrustPomodoro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTrustType = "pomodoro";
                updateTrustTypeSelection();
            }
        });
        
        btnTrustLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTrustType = "location";
                updateTrustTypeSelection();
            }
        });
        
        // Location selection
        selectLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For demo purposes, set a fixed location
                selectedLatitude = 37.7749;
                selectedLongitude = -122.4194;
                locationCoordinatesText.setText("Location selected: 37.7749, -122.4194");
                // In a real app, use LocationManager or FusedLocationProviderClient
            }
        });
        
        // Cancel button
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }        });
        
        // Save button
        saveHabitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitHabit();
            }
        });
    }
      private void updateTrustTypeSelection() {        // Update indicators
        checkboxIndicator.setVisibility("checkbox".equals(selectedTrustType) ? View.VISIBLE : View.INVISIBLE);
        pomodoroIndicator.setVisibility("pomodoro".equals(selectedTrustType) ? View.VISIBLE : View.INVISIBLE);
        locationIndicator.setVisibility("location".equals(selectedTrustType) ? View.VISIBLE : View.INVISIBLE);
        
        // Show/hide verification containers
        checkboxVerificationContainer.setVisibility("checkbox".equals(selectedTrustType) ? View.VISIBLE : View.GONE);
        pomodoroVerificationContainer.setVisibility("pomodoro".equals(selectedTrustType) ? View.VISIBLE : View.GONE);
        locationVerificationContainer.setVisibility("location".equals(selectedTrustType) ? View.VISIBLE : View.GONE);
    }
    
    private void submitHabit() {
        // Validate input
        String title = habitTitleInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a habit title", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String description = habitDescriptionInput.getText().toString().trim();
        
        // Get frequency from spinner
        String frequency = habitFrequencySpinner.getSelectedItem().toString().toLowerCase();
        
        // Format current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String reminderDate = dateFormat.format(Calendar.getInstance().getTime());
        
        // Prepare data for API request
        try {
            JSONObject habitData = new JSONObject();
            habitData.put("user_id", userId);
            habitData.put("title", title);
            habitData.put("description", description);
            habitData.put("frequency", frequency);
            habitData.put("trust_type", selectedTrustType);
            habitData.put("reminder_date", reminderDate);
            
            // Add type-specific data
            if ("location".equals(selectedTrustType)) {
                habitData.put("map_lat", selectedLatitude);
                habitData.put("map_lon", selectedLongitude);
            } else if ("pomodoro".equals(selectedTrustType)) {
                String pomodoroCountStr = pomodoroCountEditText.getText().toString().trim();
                String pomodoroDurationStr = pomodoroDurationEditText.getText().toString().trim();
                
                int pomodoroCount = pomodoroCountStr.isEmpty() ? 1 : Integer.parseInt(pomodoroCountStr);
                int duration = pomodoroDurationStr.isEmpty() ? 25 : Integer.parseInt(pomodoroDurationStr);
                
                habitData.put("pomodoro_count", pomodoroCount);
                habitData.put("pomodoro_duration", duration);
            }
            
            // Make API request
            sendAddHabitRequest(habitData);
            
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing data", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sendAddHabitRequest(JSONObject habitData) {
        String url = Constants.BASE_URL + "add_habit.php";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, habitData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String message = response.getString("message");
                            Toast.makeText(AddHabitActivity.this, message, Toast.LENGTH_SHORT).show();
                            
                            // Close activity and return to habits list
                            finish();
                            
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(AddHabitActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error: " + error.toString());
                        Toast.makeText(AddHabitActivity.this, "Failed to add habit", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(request);
    }
} 