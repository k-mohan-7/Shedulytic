package com.simats.schedulytic.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Service to handle location-based habit verification
 */
public class LocationVerificationService extends Service {
    private static final String TAG = "LocationVerificationService";
    
    // Broadcast actions
    public static final String ACTION_LOCATION_UPDATED = "com.simats.schedulytic.ACTION_LOCATION_UPDATED";
    public static final String ACTION_LOCATION_ERROR = "com.simats.schedulytic.ACTION_LOCATION_ERROR";
    
    // Broadcast extras
    public static final String EXTRA_LATITUDE = "com.simats.schedulytic.EXTRA_LATITUDE";
    public static final String EXTRA_LONGITUDE = "com.simats.schedulytic.EXTRA_LONGITUDE";
    public static final String EXTRA_ACCURACY = "com.simats.schedulytic.EXTRA_ACCURACY";
    public static final String EXTRA_ERROR_MESSAGE = "com.simats.schedulytic.EXTRA_ERROR_MESSAGE";
    
    // Location settings
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_INTERVAL = 5000; // 5 seconds
    private static final float DEFAULT_RADIUS_METERS = 100f;
    
    // Location services
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean isLocationTracking = false;
    private Location currentLocation = null;
    
    // Binder
    private final IBinder binder = new LocationVerificationBinder();
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Create location request
        createLocationRequest();
        
        // Create location callback
        createLocationCallback();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as a regular service (not foreground)
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        // Remove location updates
        stopLocationTracking();
        super.onDestroy();
    }
    
    /**
     * Create location request with high accuracy
     */
    private void createLocationRequest() {
        locationRequest = LocationRequest.create()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);
    }
    
    /**
     * Create location callback
     */
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    currentLocation = locationResult.getLastLocation();
                    
                    // Broadcast location update
                    broadcastLocationUpdate(currentLocation);
                    
                    Log.d(TAG, "Location update: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                }
            }
        };
    }
    
    /**
     * Start tracking location updates
     */
    public void startLocationTracking() {
        if (isLocationTracking) {
            Log.d(TAG, "Location tracking already started");
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            broadcastLocationError("Location permission not granted");
            return;
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, 
                    locationCallback,
                    Looper.getMainLooper());
            
            isLocationTracking = true;
            Log.d(TAG, "Location tracking started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting location tracking: " + e.getMessage());
            broadcastLocationError("Error starting location tracking: " + e.getMessage());
        }
    }
    
    /**
     * Stop tracking location updates
     */
    public void stopLocationTracking() {
        if (!isLocationTracking) {
            Log.d(TAG, "Location tracking already stopped");
            return;
        }
        
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationTracking = false;
            Log.d(TAG, "Location tracking stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location tracking: " + e.getMessage());
        }
    }
    
    /**
     * Check if the current location is within a certain radius of the target location
     * 
     * @param targetLatitude target latitude
     * @param targetLongitude target longitude
     * @param radiusMeters radius in meters
     * @return true if within radius, false otherwise
     */
    public boolean isWithinRadius(double targetLatitude, double targetLongitude, double radiusMeters) {
        if (currentLocation == null) {
            Log.e(TAG, "Current location is null");
            return false;
        }
        
        // If radius not specified, use default
        if (radiusMeters <= 0) {
            radiusMeters = DEFAULT_RADIUS_METERS;
        }
        
        // Calculate distance
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(), 
                currentLocation.getLongitude(),
                targetLatitude,
                targetLongitude,
                results);
        
        float distance = results[0];
        
        Log.d(TAG, "Distance to target: " + distance + " meters (radius: " + radiusMeters + " meters)");
        
        return distance <= radiusMeters;
    }
    
    /**
     * Get the current location
     * @return current location or null if not available
     */
    public Location getCurrentLocation() {
        return currentLocation;
    }
    
    /**
     * Get the distance to a target location
     * @param targetLatitude target latitude
     * @param targetLongitude target longitude
     * @return distance in meters, or -1 if current location is not available
     */
    public float getDistanceToTarget(double targetLatitude, double targetLongitude) {
        if (currentLocation == null) {
            Log.e(TAG, "Current location is null");
            return -1;
        }
        
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(), 
                currentLocation.getLongitude(),
                targetLatitude,
                targetLongitude,
                results);
        
        return results[0];
    }
    
    /**
     * Broadcast location update
     */
    private void broadcastLocationUpdate(Location location) {
        Intent intent = new Intent(ACTION_LOCATION_UPDATED);
        intent.putExtra(EXTRA_LATITUDE, location.getLatitude());
        intent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
        intent.putExtra(EXTRA_ACCURACY, location.getAccuracy());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    /**
     * Broadcast location error
     */
    private void broadcastLocationError(String errorMessage) {
        Intent intent = new Intent(ACTION_LOCATION_ERROR);
        intent.putExtra(EXTRA_ERROR_MESSAGE, errorMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    /**
     * Binder class for clients
     */
    public class LocationVerificationBinder extends Binder {
        public LocationVerificationService getService() {
            return LocationVerificationService.this;
        }
    }
} 