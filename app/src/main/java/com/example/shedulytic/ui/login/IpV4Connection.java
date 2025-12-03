package com.example.shedulytic.ui.login; // Or your actual package

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IpV4Connection {
    private static final String TAG = "IpV4Connection";

    // Your primary server IP. This will be the first one tried.
    private static final String YOUR_PRIMARY_SERVER_IP = "10.34.179.64";

    private static final String[] FALLBACK_URLS = {
            "http://" + YOUR_PRIMARY_SERVER_IP + "/schedlytic/",      // Your primary server
            "http://" + YOUR_PRIMARY_SERVER_IP + ":80/schedlytic/",   // Your primary server with explicit port
            "http://10.0.2.2/schedlytic/",          // Android emulator special IP for host localhost
            "http://10.0.2.2:80/schedlytic/",       // Android emulator with explicit port
            // Add other potential IPs or hostnames if needed, but the above are most common for development.
            // "http://127.0.0.1/schedlytic/", // Usually not for emulator to host unless special setup
            // "http://localhost/schedlytic/"   // Usually not for emulator to host unless special setup
    };

    private static String cachedWorkingUrl = null;
    private static long lastConnectionCheckTime = 0;
    private static final long CONNECTION_CHECK_INTERVAL_MS = 2 * 60 * 1000; // Check every 2 minutes
    private static int currentUrlIndex = 0; // To cycle through FALLBACK_URLS

    public static synchronized String getBaseUrl() {
        long currentTime = System.currentTimeMillis();

        if (cachedWorkingUrl != null && (currentTime - lastConnectionCheckTime) < CONNECTION_CHECK_INTERVAL_MS) {
            // Log.d(TAG, "Using cached working URL: " + cachedWorkingUrl);
            return cachedWorkingUrl;
        }

        // Try each URL in order, starting from the current index
        for (int i = 0; i < FALLBACK_URLS.length; i++) {
            int indexToTry = (currentUrlIndex + i) % FALLBACK_URLS.length;
            String urlToTest = FALLBACK_URLS[indexToTry];

            if (isUrlReachable(urlToTest)) {
                cachedWorkingUrl = urlToTest;
                lastConnectionCheckTime = currentTime;
                currentUrlIndex = indexToTry; // Remember this working index for next full check cycle
                Log.i(TAG, "Established working base URL: " + cachedWorkingUrl);
                return cachedWorkingUrl;
            }
        }

        // If none of the FALLBACK_URLS are reachable, default to the first one (your primary)
        // and reset currentUrlIndex. Log this as a warning.
        currentUrlIndex = 0;
        cachedWorkingUrl = FALLBACK_URLS[0]; // Default to your primary URL
        lastConnectionCheckTime = currentTime; // Update time to prevent immediate re-check spam
        Log.w(TAG, "No URLs were reachable. Defaulting to primary URL: " + cachedWorkingUrl + ". Check network/server.");
        return cachedWorkingUrl;
    }

    // This method can be simplified if getBaseUrl() is robust
    public static String getFallbackUrl() {
        Log.d(TAG, "getFallbackUrl() called, deferring to getBaseUrl().");
        return getBaseUrl();
    }

    private static boolean isUrlReachable(String baseUrlString) {
        if (baseUrlString == null || baseUrlString.isEmpty()) {
            return false;
        }
        HttpURLConnection connection = null;
        try {
            // It's crucial that 'ping.php' (or a similar lightweight endpoint)
            // exists at the root of your '/schedlytic/' directory on the server.
            URL url = new URL(baseUrlString + "ping.php");
            Log.d(TAG, "Attempting to ping: " + url.toString());

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000); // 5 seconds
            connection.setReadTimeout(5000);    // 5 seconds
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false); // Avoid redirects for a simple ping
            connection.setRequestProperty("Connection", "close"); // Important
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.setRequestProperty("Pragma", "no-cache"); // For HTTP/1.0 proxies
            connection.setRequestProperty("Expires", "0"); // For proxies


            connection.connect();
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code from " + url.toString() + ": " + responseCode);

            // HTTP_OK (200) is expected for a successful ping
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.i(TAG, "Successfully connected to: " + baseUrlString);
                return true;
            } else {
                Log.w(TAG, "Failed to connect to " + baseUrlString + " (ping.php) - HTTP Status: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking URL reachability for " + baseUrlString + ": " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // --- Endpoint URL Methods ---
    // All these methods now use getBaseUrl()

    public static String getUserProfileUrl(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty for getUserProfileUrl");
            // Consider throwing an IllegalArgumentException or returning null/empty string
            // depending on how your calling code handles this.
            return ""; // Or throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        return getBaseUrl() + "get_user_profile.php?user_id=" + userId;
    }

    public static String getUserStreakUrl(String userId, String startDate, String endDate) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty for getUserStreakUrl");
            return "";
        }
        // Add null/empty checks for startDate and endDate if they are mandatory
        return getBaseUrl() + "get_user_streak.php?user_id=" + userId + "&start_date=" + startDate + "&end_date=" + endDate;
    }

    public static String getViewTasksUrl(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty for getViewTasksUrl");
            return "";
        }
        return getBaseUrl() + "view_tasks.php?user_id=" + userId;
    }

    public static String getAddTaskUrl() {
        return getBaseUrl() + "add_task.php";
    }

    public static String getUpdateTaskUrl() {
        return getBaseUrl() + "update_task.php";
    }

    public static String getDeleteTaskUrl() {
        return getBaseUrl() + "delete_task.php";
    }

    public static String getTodayTasksUrl(String userId, String date) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty for getTodayTasksUrl");
            return "";
        }
        String requestDate = date;
        if (requestDate == null || requestDate.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            requestDate = dateFormat.format(new Date());
        }
        return getBaseUrl() + "get_today_tasks.php?user_id=" + userId + "&date=" + requestDate;
    }

    public static String getUpdateTaskCompletionUrl() {
        return getBaseUrl() + "update_task_completion.php";
    }

    // General network availability check (doesn't ping your server, just checks device network state)
    public static boolean isDeviceNetworkAvailable(Context context) {
        if (context == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    // Wrapper method for isDeviceNetworkAvailable for backward compatibility
    public static boolean isNetworkAvailable(Context context) {
        return isDeviceNetworkAvailable(context);
    }
}