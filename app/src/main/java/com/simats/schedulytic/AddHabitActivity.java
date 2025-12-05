package com.simats.schedulytic;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.simats.schedulytic.model.Habit;
import com.simats.schedulytic.service.HabitManagerService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;
import java.util.Locale;

public class AddHabitActivity extends AppCompatActivity {
    private static final String TAG = "AddHabitActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int MAP_LOCATION_REQUEST_CODE = 1002;
    
    // UI components
    private EditText titleEditText;
    private EditText descriptionEditText;
    private RadioGroup verificationMethodRadioGroup;
    private RadioButton checkboxRadioButton;
    private RadioButton locationRadioButton;
    private RadioButton pomodoroRadioButton;
    private Spinner frequencySpinner;
    
    // Verification method boxes
    private View btnTrustCheckbox;
    private View btnTrustLocation;
    private View btnTrustPomodoro;
    private View checkboxIndicator;
    private View locationIndicator;
    private View pomodoroIndicator;
    
    // Verification method specific views
    private View checkboxVerificationSection;
    private View locationVerificationSection;
    private TextView locationCoordinatesText;
    private Button selectLocationButton;
    private Button selectMapLocationButton;
    private View pomodoroVerificationSection;
    private EditText pomodoroCountEditText;
    private EditText pomodoroDurationEditText;
    
    // Buttons
    private Button saveButton;
    private Button cancelButton;
    
    // Data
    private HabitManagerService habitManager;
    private String selectedVerificationMethod = Habit.VERIFICATION_CHECKBOX;
    private double selectedLatitude = 0;
    private double selectedLongitude = 0;
    
    // Location services
    private FusedLocationProviderClient fusedLocationClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_habit);
        
        // Initialize UI components
        titleEditText = findViewById(R.id.habit_title_edit_text);
        descriptionEditText = findViewById(R.id.habit_description_edit_text);
        verificationMethodRadioGroup = findViewById(R.id.verification_method_radio_group);
        checkboxRadioButton = findViewById(R.id.checkbox_verification_radio);
        locationRadioButton = findViewById(R.id.location_verification_radio);
        pomodoroRadioButton = findViewById(R.id.pomodoro_verification_radio);
        frequencySpinner = findViewById(R.id.habit_frequency_spinner);
        
        // Initialize verification method boxes
        btnTrustCheckbox = findViewById(R.id.btn_trust_checkbox);
        btnTrustLocation = findViewById(R.id.btn_trust_location);
        btnTrustPomodoro = findViewById(R.id.btn_trust_pomodoro);
        checkboxIndicator = findViewById(R.id.checkbox_indicator);
        locationIndicator = findViewById(R.id.location_indicator);
        pomodoroIndicator = findViewById(R.id.pomodoro_indicator);
        
        // Verification method specific views
        checkboxVerificationSection = findViewById(R.id.checkbox_verification_container);
        locationVerificationSection = findViewById(R.id.location_verification_container);
        locationCoordinatesText = findViewById(R.id.location_coordinates_text);
        selectLocationButton = findViewById(R.id.select_location_button);
        pomodoroVerificationSection = findViewById(R.id.pomodoro_verification_container);
        pomodoroCountEditText = findViewById(R.id.pomodoro_count_edit_text);
        pomodoroDurationEditText = findViewById(R.id.pomodoro_duration_edit_text);
        
        // Buttons
        saveButton = findViewById(R.id.save_habit_button);
        cancelButton = findViewById(R.id.cancel_button);
        
        // Initialize habit manager
        habitManager = HabitManagerService.getInstance(this);
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Set up UI interactions
        setupVerificationMethodRadioButtons();
        setupFrequencySpinner();
        setupLocationButton();
        setupSaveButton();
        setupCancelButton();
        
        // Add map location button
        addMapLocationButton();
    }
    
    /**
     * Add a button to select location from map
     */
    private void addMapLocationButton() {
        // Create button for map location selection
        selectMapLocationButton = new Button(this);
        selectMapLocationButton.setText("Select Location on Map");
        selectMapLocationButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.remainder_color));
        selectMapLocationButton.setTextColor(ContextCompat.getColor(this, R.color.white));
        selectMapLocationButton.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // Add click listener to open map selection
        selectMapLocationButton.setOnClickListener(v -> openEmbeddedMapForSelection());
        
        // Add to location verification section
        if (locationVerificationSection instanceof android.view.ViewGroup) {
            android.view.ViewGroup container = (android.view.ViewGroup) locationVerificationSection;
            container.addView(selectMapLocationButton);
        }
    }
    
    /**
     * Open embedded map for location selection using OSMDroid
     */
    private void openEmbeddedMapForSelection() {
        // Initialize OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        // Create dialog to show embedded map
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Location");
        
        // Inflate custom map view layout
        View mapView = getLayoutInflater().inflate(R.layout.embedded_map_view, null);
        MapView osmMapView = mapView.findViewById(R.id.osm_map_view);
        Button btnCurrentLocation = mapView.findViewById(R.id.btn_current_location);
        Button btnClearSelection = mapView.findViewById(R.id.btn_clear_selection);
        TextView selectedLocationText = mapView.findViewById(R.id.selected_location_text);
        
        // Configure OSMDroid MapView
        osmMapView.setTileSource(TileSourceFactory.MAPNIK);
        osmMapView.setMultiTouchControls(true);
        osmMapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        
        // Set up map controller
        IMapController mapController = osmMapView.getController();
        mapController.setZoom(13.0);
        
        // Set initial location (India center or user's last known location)
        GeoPoint startPoint = new GeoPoint(20.5937, 78.9629); // India center
        mapController.setCenter(startPoint);
        
        // Create location overlay for "My Location" functionality
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), osmMapView);
        locationOverlay.enableMyLocation();
        osmMapView.getOverlays().add(locationOverlay);
        
        // Variable to hold the selected marker
        final Marker[] selectedMarker = {null};
        
        // Set up map click listener for location selection
        osmMapView.getOverlays().add(new org.osmdroid.views.overlay.Overlay() {
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent e, MapView mapView) {
                // Convert screen coordinates to geographic coordinates
                org.osmdroid.api.IGeoPoint geoPoint = mapView.getProjection().fromPixels((int)e.getX(), (int)e.getY());
                
                // Remove previous marker if exists
                if (selectedMarker[0] != null) {
                    osmMapView.getOverlays().remove(selectedMarker[0]);
                }
                
                // Create new marker at selected location
                selectedMarker[0] = new Marker(osmMapView);
                selectedMarker[0].setPosition(new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude()));
                selectedMarker[0].setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                selectedMarker[0].setTitle("Selected Location");
                selectedMarker[0].setSnippet(String.format("Lat: %.6f, Lng: %.6f", 
                    geoPoint.getLatitude(), geoPoint.getLongitude()));
                
                // Add marker to map
                osmMapView.getOverlays().add(selectedMarker[0]);
                osmMapView.invalidate();
                
                // Update selected coordinates
                selectedLatitude = geoPoint.getLatitude();
                selectedLongitude = geoPoint.getLongitude();
                
                // Update location text
                selectedLocationText.setText(String.format("Selected: %.6f, %.6f", 
                    selectedLatitude, selectedLongitude));
                selectedLocationText.setVisibility(View.VISIBLE);
                
                Log.d(TAG, String.format("Location selected: %.6f, %.6f", selectedLatitude, selectedLongitude));
                
                // Try to get address in background (optional)
                new Thread(() -> {
                    try {
                        Geocoder geocoder = new Geocoder(AddHabitActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(selectedLatitude, selectedLongitude, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String addressText = address.getAddressLine(0);
                            
                            runOnUiThread(() -> {
                                selectedLocationText.setText(String.format("Selected: %s\n(%.6f, %.6f)", 
                                    addressText, selectedLatitude, selectedLongitude));
                                selectedMarker[0].setSnippet(addressText);
                            });
                        }
                    } catch (Exception ex) {
                        Log.w(TAG, "Geocoding failed (non-critical): " + ex.getMessage());
                    }
                }).start();
                
                return true;
            }
        });
        
        // Current Location Button click listener
        btnCurrentLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
                
                btnCurrentLocation.setEnabled(false);
                btnCurrentLocation.setText("Getting location...");
                
                // Get current location
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapController.setCenter(userLocation);
                        mapController.setZoom(16.0);
                        
                        // Remove previous marker if exists
                        if (selectedMarker[0] != null) {
                            osmMapView.getOverlays().remove(selectedMarker[0]);
                        }
                        
                        // Add marker at current location
                        selectedMarker[0] = new Marker(osmMapView);
                        selectedMarker[0].setPosition(userLocation);
                        selectedMarker[0].setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        selectedMarker[0].setTitle("My Location");
                        selectedMarker[0].setSnippet(String.format("Lat: %.6f, Lng: %.6f", 
                            location.getLatitude(), location.getLongitude()));
                        
                        osmMapView.getOverlays().add(selectedMarker[0]);
                        osmMapView.invalidate();
                        
                        // Update selected coordinates
                        selectedLatitude = location.getLatitude();
                        selectedLongitude = location.getLongitude();
                        
                        selectedLocationText.setText(String.format("My Location: %.6f, %.6f", 
                            selectedLatitude, selectedLongitude));
                        selectedLocationText.setVisibility(View.VISIBLE);
                        
                        btnCurrentLocation.setEnabled(true);
                        btnCurrentLocation.setText("📍 Use My Location");
                        
                        Log.d(TAG, String.format("Current location selected: %.6f, %.6f", selectedLatitude, selectedLongitude));
                    } else {
                        Toast.makeText(this, "Could not get current location", Toast.LENGTH_SHORT).show();
                        btnCurrentLocation.setEnabled(true);
                        btnCurrentLocation.setText("📍 Use My Location");
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnCurrentLocation.setEnabled(true);
                    btnCurrentLocation.setText("📍 Use My Location");
                });
            } else {
                // Request location permission
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
            }
        });
        
        // Clear Selection Button click listener
        btnClearSelection.setOnClickListener(v -> {
            if (selectedMarker[0] != null) {
                osmMapView.getOverlays().remove(selectedMarker[0]);
                selectedMarker[0] = null;
                osmMapView.invalidate();
            }
            
            selectedLatitude = 0.0;
            selectedLongitude = 0.0;
            selectedLocationText.setText("Tap on the map to select a location");
            selectedLocationText.setVisibility(View.VISIBLE);
            
            Log.d(TAG, "Location selection cleared");
        });
        
        // Add the MapView to the dialog
        builder.setView(mapView);
        
        // Add buttons
        builder.setPositiveButton("Select", (dialog, which) -> {
            if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
                // Update UI with selected location
                locationCoordinatesText.setText(String.format(
                    "Location: %.6f, %.6f", selectedLatitude, selectedLongitude));
                locationCoordinatesText.setVisibility(View.VISIBLE);
                
                Log.d(TAG, String.format("Location confirmed: %.6f, %.6f", selectedLatitude, selectedLongitude));
            } else {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        // Show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Cleanup when dialog is dismissed
        dialog.setOnDismissListener(dialogInterface -> {
            if (osmMapView != null) {
                osmMapView.onDetach();
            }
        });
    }
    
    /**
     * Set up verification method radio group
     */
    private void setupVerificationMethodRadioButtons() {
        // Set up box buttons for verification methods
        btnTrustCheckbox.setOnClickListener(v -> {
            // Update selection indicators
            checkboxIndicator.setVisibility(View.VISIBLE);
            locationIndicator.setVisibility(View.GONE);
            pomodoroIndicator.setVisibility(View.GONE);
            
            // Update radio buttons to maintain compatibility
            verificationMethodRadioGroup.check(R.id.checkbox_verification_radio);
            selectedVerificationMethod = Habit.VERIFICATION_CHECKBOX;
            
            // Show checkbox section, hide others
            checkboxVerificationSection.setVisibility(View.VISIBLE);
            locationVerificationSection.setVisibility(View.GONE);
            pomodoroVerificationSection.setVisibility(View.GONE);
        });
        
        btnTrustLocation.setOnClickListener(v -> {
            // Update selection indicators
            checkboxIndicator.setVisibility(View.GONE);
            locationIndicator.setVisibility(View.VISIBLE);
            pomodoroIndicator.setVisibility(View.GONE);
            
            // Update radio buttons to maintain compatibility
            verificationMethodRadioGroup.check(R.id.location_verification_radio);
            selectedVerificationMethod = Habit.VERIFICATION_LOCATION;
            
            // Show location section, hide others
            checkboxVerificationSection.setVisibility(View.GONE);
            locationVerificationSection.setVisibility(View.VISIBLE);
            pomodoroVerificationSection.setVisibility(View.GONE);
        });
        
        btnTrustPomodoro.setOnClickListener(v -> {
            // Update selection indicators
            checkboxIndicator.setVisibility(View.GONE);
            locationIndicator.setVisibility(View.GONE);
            pomodoroIndicator.setVisibility(View.VISIBLE);
            
            // Update radio buttons to maintain compatibility
            verificationMethodRadioGroup.check(R.id.pomodoro_verification_radio);
            selectedVerificationMethod = Habit.VERIFICATION_POMODORO;
            
            // Show pomodoro section, hide others
            checkboxVerificationSection.setVisibility(View.GONE);
            locationVerificationSection.setVisibility(View.GONE);
            pomodoroVerificationSection.setVisibility(View.VISIBLE);
        });
        
        // Keep the old implementation for backward compatibility
        checkboxRadioButton.setOnClickListener(v -> {
            updateVerificationBoxSelections(Habit.VERIFICATION_CHECKBOX);
        });
        
        locationRadioButton.setOnClickListener(v -> {
            updateVerificationBoxSelections(Habit.VERIFICATION_LOCATION);
        });
        
        pomodoroRadioButton.setOnClickListener(v -> {
            updateVerificationBoxSelections(Habit.VERIFICATION_POMODORO);
        });
        
        // Default to checkbox
        updateVerificationBoxSelections(Habit.VERIFICATION_CHECKBOX);
    }
    
    /**
     * Update verification box selections based on verification method
     */
    private void updateVerificationBoxSelections(String verificationMethod) {
        // Update selection indicators based on verification method
        checkboxIndicator.setVisibility(
            verificationMethod.equals(Habit.VERIFICATION_CHECKBOX) ? View.VISIBLE : View.GONE);
        locationIndicator.setVisibility(
            verificationMethod.equals(Habit.VERIFICATION_LOCATION) ? View.VISIBLE : View.GONE);
        pomodoroIndicator.setVisibility(
            verificationMethod.equals(Habit.VERIFICATION_POMODORO) ? View.VISIBLE : View.GONE);
        
        // Set verification method
        selectedVerificationMethod = verificationMethod;
        
        // Update visibility of verification sections
        checkboxVerificationSection.setVisibility(
            verificationMethod.equals(Habit.VERIFICATION_CHECKBOX) ? View.VISIBLE : View.GONE);
        locationVerificationSection.setVisibility(
            verificationMethod.equals(Habit.VERIFICATION_LOCATION) ? View.VISIBLE : View.GONE);
        pomodoroVerificationSection.setVisibility(
            verificationMethod.equals(Habit.VERIFICATION_POMODORO) ? View.VISIBLE : View.GONE);
    }
    
    /**
     * Set up frequency spinner
     */
    private void setupFrequencySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.habit_frequency_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(adapter);
    }
    
    /**
     * Set up location button
     */
    private void setupLocationButton() {
        selectLocationButton.setOnClickListener(v -> {
            // Check for location permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request location permissions
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Get accurate current location
                getCurrentLocation();
            }
        });
    }
    
    /**
     * Get accurate current location
     */
    private void getCurrentLocation() {
        try {
            // Show loading indicator
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Getting your accurate location...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Request location with high accuracy
            com.google.android.gms.location.LocationRequest locationRequest = 
                com.google.android.gms.location.LocationRequest.create();
            locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(2000);
            
            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    progressDialog.dismiss();
                    
                    if (locationResult == null) {
                        Toast.makeText(AddHabitActivity.this, "Unable to get accurate location. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        selectedLatitude = location.getLatitude();
                        selectedLongitude = location.getLongitude();
                        
                        // Update UI
                        locationCoordinatesText.setText(String.format(
                            "Location: %.6f, %.6f", selectedLatitude, selectedLongitude));
                        locationCoordinatesText.setVisibility(View.VISIBLE);
                        
                        // Get address from coordinates for better user experience
                        getAddressFromLocation(location);
                        
                        // Remove location updates after getting location
                        fusedLocationClient.removeLocationUpdates(this);
                    }
                }
            };
            
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            
            // Set timeout to stop location updates after 15 seconds
            new Handler().postDelayed(() -> {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(AddHabitActivity.this, "Location request timed out. Using best available location.", Toast.LENGTH_SHORT).show();
                }
            }, 15000);
            
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Get address from location coordinates
     */
    private void getAddressFromLocation(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                
                // Get address components
                if (address.getAddressLine(0) != null) {
                    sb.append(address.getAddressLine(0));
                } else {
                    // Build from components
                    if (address.getLocality() != null) {
                        sb.append(address.getLocality()).append(", ");
                    }
                    if (address.getAdminArea() != null) {
                        sb.append(address.getAdminArea());
                    }
                }
                
                // Update UI with friendly address
                if (sb.length() > 0) {
                    String addressText = sb.toString();
                    locationCoordinatesText.setText(String.format(
                        "Location: %s\n(%.6f, %.6f)", addressText, selectedLatitude, selectedLongitude));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting address: " + e.getMessage());
            // If getting address fails, coordinates are still displayed
        }
    }
    
    /**
     * Set up save button
     */
    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            // Validate input
            if (validateInput()) {
                // Create habit
                Habit habit = createHabit();
                
                // Save habit
                habitManager.addHabit(habit);
                
                // Show success message
                Toast.makeText(this, "Habit added successfully", Toast.LENGTH_SHORT).show();
                
                // Finish activity
                finish();
            }
        });
    }
    
    /**
     * Set up cancel button
     */
    private void setupCancelButton() {
        cancelButton.setOnClickListener(v -> {
            // Confirm cancellation
            new AlertDialog.Builder(this)
                .setTitle("Cancel")
                .setMessage("Are you sure you want to discard this habit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
        });
    }
    
    /**
     * Validate user input
     */
    private boolean validateInput() {
        String title = titleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            return false;
        }
        
        if (selectedVerificationMethod.equals(Habit.VERIFICATION_LOCATION) && 
                selectedLatitude == 0 && selectedLongitude == 0) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (selectedVerificationMethod.equals(Habit.VERIFICATION_POMODORO)) {
            String countStr = pomodoroCountEditText.getText().toString().trim();
            String durationStr = pomodoroDurationEditText.getText().toString().trim();
            
            if (countStr.isEmpty()) {
                pomodoroCountEditText.setError("Pomodoro count is required");
                return false;
            }
            
            if (durationStr.isEmpty()) {
                pomodoroDurationEditText.setError("Pomodoro duration is required");
                return false;
            }
            
            try {
                int count = Integer.parseInt(countStr);
                int duration = Integer.parseInt(durationStr);
                
                if (count <= 0 || count > 10) {
                    pomodoroCountEditText.setError("Count must be between 1 and 10");
                    return false;
                }
                
                if (duration < 5 || duration > 60) {
                    pomodoroDurationEditText.setError("Duration must be between 5 and 60 minutes");
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create a habit from user input and ensure location data is properly set
     */
    private Habit createHabit() {
        Habit habit = new Habit();
        
        // Set basic properties
        habit.setTitle(titleEditText.getText().toString().trim());
        habit.setDescription(descriptionEditText.getText().toString().trim());
        habit.setFrequency(frequencySpinner.getSelectedItem().toString().toLowerCase());
        
        // Set user ID from preferences
        habit.setUserId(getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("user_id", ""));
        
        // Set verification method specific properties
        habit.setVerificationMethod(selectedVerificationMethod);
        
        // Ensure location data is properly set for location-based habits
        if (selectedVerificationMethod.equals(Habit.VERIFICATION_LOCATION)) {
            habit.setLatitude(selectedLatitude);
            habit.setLongitude(selectedLongitude);
            habit.setRadiusMeters(100); // Default radius
            
            // Add extra properties to ensure location data is properly saved in backend
            try {
                JSONObject extraProperties = new JSONObject();
                extraProperties.put("map_lat", selectedLatitude);
                extraProperties.put("map_lon", selectedLongitude);
                extraProperties.put("radius_meters", 100);
                extraProperties.put("trust_type", "map"); // Use backend enum value
                extraProperties.put("verification_method", "location"); // Keep app value for compatibility
                habit.setExtraProperties(extraProperties);
            } catch (Exception e) {
                Log.e(TAG, "Error setting extra properties: " + e.getMessage());
            }
        } else if (selectedVerificationMethod.equals(Habit.VERIFICATION_POMODORO)) {
            int pomodoroCount = Integer.parseInt(pomodoroCountEditText.getText().toString().trim());
            int pomodoroDuration = Integer.parseInt(pomodoroDurationEditText.getText().toString().trim());
            
            habit.setPomodoroCount(pomodoroCount);
            habit.setPomodoroLength(pomodoroDuration);
            
            // Add extra properties for pomodoro
            try {
                JSONObject extraProperties = new JSONObject();
                extraProperties.put("pomodoro_count", pomodoroCount);
                extraProperties.put("pomodoro_duration", pomodoroDuration);
                extraProperties.put("trust_type", "pomodoro");
                habit.setExtraProperties(extraProperties);
            } catch (Exception e) {
                Log.e(TAG, "Error setting extra properties: " + e.getMessage());
            }
        } else {
            // For checkbox verification
            try {
                JSONObject extraProperties = new JSONObject();
                extraProperties.put("trust_type", "checkbox");
                habit.setExtraProperties(extraProperties);
            } catch (Exception e) {
                Log.e(TAG, "Error setting extra properties: " + e.getMessage());
            }
        }
        
        return habit;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getCurrentLocation();
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission is required for location verification", Toast.LENGTH_SHORT).show();
                
                // Reset radio button to checkbox if location permission denied
                checkboxRadioButton.setChecked(true);
                selectedVerificationMethod = Habit.VERIFICATION_CHECKBOX;
                locationVerificationSection.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == MAP_LOCATION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Handle map location result
            double latitude = data.getDoubleExtra("latitude", 0);
            double longitude = data.getDoubleExtra("longitude", 0);
            
            if (latitude != 0 && longitude != 0) {
                selectedLatitude = latitude;
                selectedLongitude = longitude;
                
                // Update UI
                locationCoordinatesText.setText(String.format(
                    "Location: %.6f, %.6f", selectedLatitude, selectedLongitude));
                locationCoordinatesText.setVisibility(View.VISIBLE);
            }
        }
    }
} 