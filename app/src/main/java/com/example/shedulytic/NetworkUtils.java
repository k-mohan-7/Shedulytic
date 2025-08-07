package com.example.shedulytic;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling network operations with robust error handling
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    private static RequestQueue requestQueue;
    
    /**
     * Initialize the RequestQueue
     */
    public static void initialize(Context context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
    }

    /**
     * Get the RequestQueue instance
     */
    public static RequestQueue getRequestQueue(Context context) {
        if (requestQueue == null) {
            initialize(context);
        }
        return requestQueue;
    }

    /**
     * Interface for JSON response callbacks
     */
    public interface JsonResponseCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    /**
     * Check if network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Test connectivity to server with detailed diagnostics
     */
    public static boolean testServerConnectivity(Context context) {
        if (!isNetworkAvailable(context)) {
            Log.e(TAG, "Network is not available");
            return false;
        }
        
        // Try all possible URLs to ensure we can connect to at least one
        String[] urlsToTry = {
            "http://192.168.53.64/schedlytic/",
            "http://localhost/schedlytic/",
            "http://10.0.2.2/schedlytic/"
        };
        
        for (String urlToTest : urlsToTry) {
            Log.d(TAG, "Testing connectivity to: " + urlToTest);
            boolean isReachable = testUrlConnectivity(urlToTest);
            if (isReachable) {
                Log.d(TAG, "Successfully connected to: " + urlToTest);
                return true;
            }
        }
        
        Log.e(TAG, "Could not connect to any server URLs");
        return false;
    }
    
    /**
     * Test connectivity to a specific URL with detailed diagnostics
     */
    private static boolean testUrlConnectivity(String urlString) {
        HttpURLConnection connection = null;
        try {
            // First try the URL directly
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000); // Increased timeout
            connection.setReadTimeout(8000);    // Increased timeout
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.setInstanceFollowRedirects(true); // Follow redirects
            connection.setRequestProperty("Connection", "close");
            connection.setRequestProperty("Accept", "*/*"); // Accept any content type
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.setRequestProperty("Pragma", "no-cache");
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code from " + urlString + ": " + responseCode);
            
            // Accept any 2xx or 3xx response as success
            if (responseCode >= 200 && responseCode < 400) {
                Log.d(TAG, "Successfully connected to URL: " + urlString);
                return true;
            }
            
            // If that fails, try with ping.php
            if (connection != null) {
                connection.disconnect();
            }
            
            // Ensure URL ends with a slash
            String pingUrl = urlString;
            if (!pingUrl.endsWith("/")) {
                pingUrl += "/";
            }
            pingUrl += "ping.php";
            
            url = new URL(pingUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Connection", "close");
            connection.setRequestProperty("Accept", "*/*");
            connection.connect();
            
            responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code from ping URL " + pingUrl + ": " + responseCode);
            
            return (responseCode >= 200 && responseCode < 400);
        } catch (Exception e) {
            Log.e(TAG, "Error testing URL " + urlString + ": " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Check for specific connection issues with the problematic IP address
     */
    public static boolean checkSpecificIpConnectivity(Context context, String ipAddress) {
        if (!isNetworkAvailable(context)) {
            return false;
        }
        
        String testUrl = "http://" + ipAddress + "/schedlytic/ping.php";
        Log.d(TAG, "Testing specific IP connectivity to: " + testUrl);
        
        return testUrlConnectivity(testUrl);
    }

    /**
     * Make a JSON request with improved error handling and connectivity testing
     */
    public static void makeJsonRequest(Context context, String url, JSONObject jsonRequest,
                                      final JsonResponseCallback callback) {
        Log.d(TAG, "Making JSON request to: " + url);
        
        // Check network connectivity
        if (!isNetworkAvailable(context)) {
            callback.onError("Network not available");
            return;
        }
        
        // Store the original URL in a final variable to use in the inner class
        final String originalUrl = url;
        
        // Initialize RequestQueue if needed
        if (requestQueue == null) {
            initialize(context);
        }
        
        // Test server connectivity before attempting the request
        boolean isServerReachable = testServerConnectivity(context);
        if (!isServerReachable) {
            Log.e(TAG, "Server is not reachable");
            
            // Try with an alternative URL
            final String fallbackUrl = IpV4Connection.getFallbackUrl();
            if (!fallbackUrl.equals(url)) {
                Log.d(TAG, "Trying with fallback URL: " + fallbackUrl);
                makeJsonRequestWithUrl(context, fallbackUrl, jsonRequest, new JsonResponseCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        callback.onSuccess(response);
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // If even the fallback URL fails, provide a clear error message
                        callback.onError("Could not connect to server. Please check your connection and try again later.");
                    }
                });
                return;
            }
            
            callback.onError("Server is not reachable. Please check your connection and try again later.");
            return;
        }
        
        // Proceed with the JSON request
        makeJsonRequestWithUrl(context, url, jsonRequest, callback);
    }
    
    /**
     * Internal method to make a JSON request to a specific URL
     */
    private static void makeJsonRequestWithUrl(Context context, String url, JSONObject jsonRequest,
                                             final JsonResponseCallback callback) {
        // Create a JSON request with the provided data
        Log.d(TAG, "Making JSON request to URL: " + url);
        if (jsonRequest != null) {
            Log.d(TAG, "Request body: " + jsonRequest.toString());
        }
        
        // Set retry policy parameters
        final int DEFAULT_TIMEOUT_MS = 15000; // 15 seconds
        final int MAX_RETRIES = 3;
        final float BACKOFF_MULTIPLIER = 1.5f;
        
        int method = (jsonRequest == null) ? Request.Method.GET : Request.Method.POST;
        
        JsonObjectRequest request = new JsonObjectRequest(method, url, jsonRequest,
            response -> {
                try {
                    if (response == null) {
                        Log.e(TAG, "Response is null");
                        callback.onError("Received empty response from server");
                        return;
                    }
                    
                    Log.d(TAG, "Response: " + response.toString());
                    
                    // Process the response
                    callback.onSuccess(response);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response: " + e.getMessage(), e);
                    callback.onError("Error processing server response: " + e.getMessage());
                }
            },
            error -> {
                String errorMessage = getVolleyErrorMessage(error, url);
                Log.e(TAG, "Request error: " + errorMessage);
                
                // Try fallback URL if the error suggests connectivity issues
                if (error instanceof TimeoutError || error instanceof NoConnectionError || error instanceof NetworkError) {
                    String fallbackUrl = IpV4Connection.getFallbackUrl();
                    if (!fallbackUrl.equals(url)) {
                        Log.d(TAG, "Trying fallback URL after error: " + fallbackUrl);
                        makeJsonRequestWithUrl(context, fallbackUrl, jsonRequest, callback);
                        return;
                    }
                }
                
                callback.onError(errorMessage);
            }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };
        
        // Set a retry policy with exponential backoff
        request.setRetryPolicy(new DefaultRetryPolicy(
            DEFAULT_TIMEOUT_MS,
            MAX_RETRIES,
            BACKOFF_MULTIPLIER
        ));
        
        // Apply common request settings
        applyCommonRequestSettings(request);
        
        // Add the request to the queue
        getRequestQueue(context).add(request);
    }
    
    /**
     * Make a GET request with robust error handling and connectivity testing
     */
    public static void makeGetRequest(Context context, String url, final JsonResponseCallback callback) {
        Log.d(TAG, "Making GET request to: " + url);
        
        // Check network connectivity
        if (!isNetworkAvailable(context)) {
            callback.onError("Network not available");
            return;
        }
        
        // Initialize RequestQueue if needed
        if (requestQueue == null) {
            initialize(context);
        }
        
        // Store the original URL in a final variable to use in the inner class
        final String originalUrl = url;
        
        // Set retry policy parameters
        final int DEFAULT_TIMEOUT_MS = 15000; // 15 seconds
        final int MAX_RETRIES = 3;
        final float BACKOFF_MULTIPLIER = 1.5f;
        
        // Create the GET request
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    if (response == null) {
                        Log.e(TAG, "Response is null");
                        callback.onError("Received empty response from server");
                        return;
                    }
                    
                    Log.d(TAG, "GET response: " + response.toString());
                    
                    // Process the response
                    callback.onSuccess(response);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing GET response: " + e.getMessage(), e);
                    callback.onError("Error processing server response: " + e.getMessage());
                }
            },
            error -> {
                String errorMessage = getVolleyErrorMessage(error, url);
                Log.e(TAG, "GET request error: " + errorMessage);
                
                // Try fallback URL if the error suggests connectivity issues
                if (error instanceof TimeoutError || error instanceof NoConnectionError || error instanceof NetworkError) {
                    String fallbackUrl = IpV4Connection.getFallbackUrl();
                    if (!fallbackUrl.equals(url)) {
                        Log.d(TAG, "Trying fallback URL after GET error: " + fallbackUrl);
                        makeGetRequest(context, fallbackUrl, callback);
                        return;
                    }
                    
                    // Try alternative path formats if we're fetching streak data
                    if (url.contains("get_user_streak.php") || url.contains("streak")) {
                        tryAlternativeStreakPathFormats(context, url, callback);
                        return;
                    }
                }
                
                callback.onError(errorMessage);
            }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };
        
        // Set a retry policy with exponential backoff
        request.setRetryPolicy(new DefaultRetryPolicy(
            DEFAULT_TIMEOUT_MS,
            MAX_RETRIES,
            BACKOFF_MULTIPLIER
        ));
        
        // Apply common request settings
        applyCommonRequestSettings(request);
        
        // Add the request to the queue
        getRequestQueue(context).add(request);
    }
    
    /**
     * Try alternative path formats for streak data
     */
    private static void tryAlternativeStreakPathFormats(Context context, String originalUrl, JsonResponseCallback callback) {
        // Extract parameters from the original URL
        String userId = extractParameterFromUrl(originalUrl, "user_id");
        String startDate = extractParameterFromUrl(originalUrl, "start_date");
        String endDate = extractParameterFromUrl(originalUrl, "end_date");
        
        if (userId == null) {
            callback.onError("Invalid URL format: missing user_id parameter");
            return;
        }
        
        // Base URL without the endpoint path
        String baseUrl = originalUrl.substring(0, originalUrl.lastIndexOf('/') + 1);
        
        // Different path formats to try (removed phpfiles/ prefix)
        String[] pathFormats = {
            "get_user_streak.php",
            "get_user_streak_new.php",
            "streak_data.php"
        };
        
        // Try each path format one by one
        tryNextStreakPath(context, pathFormats, 0, baseUrl, userId, startDate, endDate, callback);
    }
    
    /**
     * Try the next streak path in sequence
     */
    private static void tryNextStreakPath(Context context, String[] paths, int index, String baseUrl, 
                                         String userId, String startDate, String endDate, 
                                         JsonResponseCallback callback) {
        if (index >= paths.length) {
            Log.e(TAG, "All streak URL paths failed");
            callback.onError("Could not retrieve streak data after trying multiple URL formats");
            return;
        }
        
        // Ensure baseUrl ends with a slash
        final String finalBaseUrl;
        if (!baseUrl.endsWith("/")) {
            finalBaseUrl = baseUrl + "/";
        } else {
            finalBaseUrl = baseUrl;
        }
        
        String path = paths[index];
        StringBuilder urlBuilder = new StringBuilder(finalBaseUrl).append(path).append("?user_id=").append(userId);
        
        if (startDate != null && !startDate.isEmpty()) {
            urlBuilder.append("&start_date=").append(startDate);
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            urlBuilder.append("&end_date=").append(endDate);
        }
        
        String url = urlBuilder.toString();
        Log.d(TAG, "Trying alternative streak URL: " + url);
        
        // Set retry policy parameters
        final int DEFAULT_TIMEOUT_MS = 10000; // 10 seconds
        final int MAX_RETRIES = 1;
        final float BACKOFF_MULTIPLIER = 1.0f;
        
        // Create the GET request for this alternative path
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                Log.d(TAG, "Alternative streak path succeeded: " + path);
                callback.onSuccess(response);
            },
            error -> {
                Log.e(TAG, "Alternative streak path failed: " + path + " - " + getVolleyErrorMessage(error, url));
                // Try the next path
                tryNextStreakPath(context, paths, index + 1, finalBaseUrl, userId, startDate, endDate, callback);
            }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };
        
        // Set a shorter retry policy for alternative paths
        request.setRetryPolicy(new DefaultRetryPolicy(
            DEFAULT_TIMEOUT_MS,
            MAX_RETRIES,
            BACKOFF_MULTIPLIER
        ));
        
        // Apply common request settings
        applyCommonRequestSettings(request);
        
        // Add the request to the queue
        getRequestQueue(context).add(request);
    }
    
    /**
     * Extract a parameter value from a URL
     */
    private static String extractParameterFromUrl(String url, String paramName) {
        if (url == null || !url.contains("?")) {
            return null;
        }
        
        String query = url.substring(url.indexOf("?") + 1);
        String[] pairs = query.split("&");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        
        return null;
    }

    // Helper method to add common headers to requests
    private static Map<String, String> getCommonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Connection", "close"); // Prevent connection reuse issues
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json; charset=utf-8");
        return headers;
    }

    /**
     * Get a detailed and user-friendly error message from a VolleyError
     */
    private static String getVolleyErrorMessage(VolleyError error, String url) {
        if (error == null) {
            return "Unknown network error";
        }
        
        String errorMessage;
        
        if (error instanceof TimeoutError) {
            errorMessage = "Connection timed out. The server may be down or overloaded.";
        } else if (error instanceof NoConnectionError) {
            errorMessage = "No network connection available. Please check your internet connection.";
        } else if (error instanceof NetworkError) {
            errorMessage = "Network error occurred. Please check your connection and try again.";
        } else if (error instanceof ServerError) {
            errorMessage = "Server error occurred. Please try again later.";
            
            // Try to get more details from server error
            if (error.networkResponse != null) {
                int statusCode = error.networkResponse.statusCode;
                errorMessage += " (Status code: " + statusCode + ")";
                
                // Try to extract error message from response
                if (error.networkResponse.data != null) {
                    try {
                        String responseBody = new String(error.networkResponse.data, "UTF-8");
                        Log.d(TAG, "Server error response body: " + responseBody);
                        
                        // Try to parse JSON response
                        try {
                            JSONObject jsonError = new JSONObject(responseBody);
                            if (jsonError.has("message")) {
                                errorMessage += " - " + jsonError.getString("message");
                            } else if (jsonError.has("error")) {
                                errorMessage += " - " + jsonError.getString("error");
                            }
                        } catch (JSONException jsonEx) {
                            // If not JSON, check if it's HTML and extract meaningful content
                            String cleanedMessage = extractErrorFromHtmlOrPhp(responseBody);
                            if (!cleanedMessage.isEmpty()) {
                                errorMessage += " - " + cleanedMessage;
                            } else if (responseBody.length() < 100) {
                                errorMessage += " - " + responseBody;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing server error response", e);
                    }
                }
            }
        } else {
            // For any other errors, include the error class name and message
            errorMessage = "Connection error: ";
            
            // Include actual error message if available
            if (error.getMessage() != null) {
                errorMessage += error.getMessage();
            } else {
                errorMessage += error.getClass().getSimpleName();
            }
        }
        
        // Include URL in debug log but not in user-facing message
        Log.e(TAG, "Network error for URL " + url + ": " + errorMessage);
        
        return errorMessage;
    }
    
    /**
     * Extract meaningful error messages from HTML or PHP error responses
     * This method filters out HTML tags and extracts relevant error information
     */
    private static String extractErrorFromHtmlOrPhp(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "";
        }
        
        // Convert to lowercase for case-insensitive matching
        String lowerResponse = responseBody.toLowerCase().trim();
        
        // Check if this is an HTML response
        boolean isHtml = lowerResponse.startsWith("<!doctype html") || 
                        lowerResponse.startsWith("<html") || 
                        lowerResponse.contains("<body") ||
                        lowerResponse.contains("<head");
        
        // Check if this is a PHP error
        boolean isPhpError = responseBody.contains("Fatal error") ||
                            responseBody.contains("Parse error") ||
                            responseBody.contains("Warning:") ||
                            responseBody.contains("Notice:") ||
                            lowerResponse.contains("php");
        
        if (isHtml || isPhpError) {
            // Extract meaningful content from HTML/PHP errors
            String cleanedMessage = "";
            
            // First, try to extract content from common HTML error elements
            if (isHtml) {
                // Look for title tag content
                cleanedMessage = extractBetweenTags(responseBody, "<title>", "</title>");
                if (cleanedMessage.isEmpty()) {
                    // Look for h1 tag content
                    cleanedMessage = extractBetweenTags(responseBody, "<h1>", "</h1>");
                }
                if (cleanedMessage.isEmpty()) {
                    // Look for any error message in paragraph tags
                    cleanedMessage = extractBetweenTags(responseBody, "<p>", "</p>");
                }
                
                // Remove HTML entities and extra whitespace
                cleanedMessage = cleanedMessage.replaceAll("&[a-zA-Z0-9]+;", " ")
                                               .replaceAll("\\s+", " ")
                                               .trim();
            }
            
            // Handle PHP errors specifically
            if (isPhpError) {
                // Extract PHP error messages
                String[] lines = responseBody.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("Fatal error:") || 
                        line.startsWith("Parse error:") ||
                        line.startsWith("Warning:") ||
                        line.startsWith("Notice:")) {
                        // Extract the error message part
                        cleanedMessage = line;
                        // Remove file paths and line numbers for user-friendly message
                        int inIndex = cleanedMessage.indexOf(" in ");
                        if (inIndex > 0) {
                            cleanedMessage = cleanedMessage.substring(0, inIndex);
                        }
                        break;
                    }
                }
                
                // If no specific PHP error found, look for common database errors
                if (cleanedMessage.isEmpty()) {
                    if (lowerResponse.contains("connection failed") || 
                        lowerResponse.contains("access denied")) {
                        cleanedMessage = "Database connection error";
                    } else if (lowerResponse.contains("table") && lowerResponse.contains("doesn't exist")) {
                        cleanedMessage = "Database table not found";
                    } else if (lowerResponse.contains("syntax error")) {
                        cleanedMessage = "Database query error";
                    }
                }
            }
            
            // If we still don't have a clean message, try to get any text content
            if (cleanedMessage.isEmpty()) {
                // Remove all HTML tags and get plain text
                String plainText = responseBody.replaceAll("<[^>]+>", " ")
                                              .replaceAll("\\s+", " ")
                                              .trim();
                
                // Get first meaningful sentence (up to 100 characters)
                if (plainText.length() > 0) {
                    cleanedMessage = plainText.length() > 100 ? 
                                   plainText.substring(0, 100) + "..." : plainText;
                }
            }
            
            // Final cleanup and validation
            if (!cleanedMessage.isEmpty()) {
                // Make sure it's not just generic server errors
                if (!cleanedMessage.toLowerCase().contains("internal server error") &&
                    !cleanedMessage.toLowerCase().contains("500 error") &&
                    !cleanedMessage.toLowerCase().contains("apache") &&
                    !cleanedMessage.toLowerCase().contains("nginx")) {
                    return cleanedMessage;
                }
            }
        }
        
        // If not HTML/PHP or couldn't extract meaningful content, return empty
        return "";
    }
    
    /**
     * Helper method to extract text between HTML tags
     */
    private static String extractBetweenTags(String text, String startTag, String endTag) {
        int startIndex = text.toLowerCase().indexOf(startTag.toLowerCase());
        if (startIndex >= 0) {
            startIndex += startTag.length();
            int endIndex = text.toLowerCase().indexOf(endTag.toLowerCase(), startIndex);
            if (endIndex > startIndex) {
                return text.substring(startIndex, endIndex).trim();
            }
        }
        return "";
    }

    /**
     * Apply common settings to all network requests
     */
    private static void applyCommonRequestSettings(Request<?> request) {
        // Set a tag for the request to allow cancellation by tag
        request.setTag("NetworkRequest");
        
        // Disable caching for all requests to ensure fresh data
        request.setShouldCache(false);
    }
    
    /**
     * Cancel all pending network requests
     * Call this method in onStop() or onDestroy() to prevent memory leaks
     */
    public static void cancelAllRequests(Context context) {
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
            Log.d(TAG, "Cancelled all pending network requests");
        }
    }

    // Helper method to clean JSON response string with improved error handling
    private static String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        
        // Log the original response for debugging
        Log.d(TAG, "Cleaning response: " + response.substring(0, Math.min(200, response.length())));
        
        // First try to extract JSON if it's embedded in HTML
        String extractedJson = extractJsonFromHtml(response);
        if (extractedJson != null) {
            Log.d(TAG, "Successfully extracted JSON from HTML: " + extractedJson.substring(0, Math.min(100, extractedJson.length())));
            return extractedJson;
        }
        
        // Check for the specific error pattern "error:value<br"
        if (response.contains("error:value<br")) {
            Log.d(TAG, "Detected error:value<br pattern, creating fallback JSON response");
            // Create a valid JSON response with the error message
            return "{\"status\":\"error\",\"message\":\"Server returned invalid data. Please try again.\"}";
        }
        
        // Otherwise clean HTML tags that might be in error messages while preserving JSON structure
        String cleaned = response
            .replaceAll("<br[^>]*>", "")
            .replaceAll("<[^>]*>", "")
            .replaceAll("\\s+", " ")
            .trim();
            
        // Fix common JSON syntax issues
        if (!cleaned.startsWith("{") && !cleaned.startsWith("[")) {
            // Try to find the start of JSON
            int jsonStart = cleaned.indexOf("{");
            int arrayStart = cleaned.indexOf("[");
            
            // Choose the earlier of the two starts if both exist
            if (jsonStart >= 0 && arrayStart >= 0) {
                jsonStart = Math.min(jsonStart, arrayStart);
            } else if (jsonStart < 0 && arrayStart >= 0) {
                jsonStart = arrayStart;
            }
            
            if (jsonStart >= 0) {
                cleaned = cleaned.substring(jsonStart);
            } else {
                // If no JSON structure found, create a default JSON object
                Log.d(TAG, "No JSON structure found, creating default JSON");
                return "{\"status\":\"error\",\"message\":\"Server returned invalid data. Please try again.\"}";
            }
        }
        
        // Fix trailing characters
        int lastBrace = cleaned.lastIndexOf("}");
        int lastBracket = cleaned.lastIndexOf("]");
        
        // Choose the later of the two ends if both exist
        int lastChar = Math.max(lastBrace, lastBracket);
        
        if (lastChar > 0 && lastChar < cleaned.length() - 1) {
            cleaned = cleaned.substring(0, lastChar + 1);
        }
        
        // Check for unbalanced quotes which can cause JSON parsing errors
        cleaned = balanceQuotes(cleaned);
        
        // Verify the cleaned string is valid JSON
        try {
            if (cleaned.startsWith("{")) {
                new JSONObject(cleaned);
            } else if (cleaned.startsWith("[")) {
                new JSONArray(cleaned);
            } else {
                // If not valid JSON, create a default JSON object
                Log.d(TAG, "Cleaned string is not valid JSON, creating default JSON");
                return "{\"status\":\"error\",\"message\":\"Server returned invalid data. Please try again.\"}";
            }
        } catch (JSONException e) {
            Log.e(TAG, "Cleaned string is not valid JSON: " + e.getMessage());
            return "{\"status\":\"error\",\"message\":\"Server returned invalid data. Please try again.\"}";
        }
        
        // Log the cleaned response for debugging
        Log.d(TAG, "Cleaned response: " + cleaned.substring(0, Math.min(200, cleaned.length())));
        
        return cleaned;
    }
    
    /**
     * Balance quotes in a JSON string to prevent parsing errors
     */
    private static String balanceQuotes(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        
        // Count quotes to check if they're balanced
        int quoteCount = 0;
        for (int i = 0; i < json.length(); i++) {
            // Skip escaped quotes
            if (i > 0 && json.charAt(i-1) == '\\' && json.charAt(i) == '"') {
                continue;
            }
            
            if (json.charAt(i) == '"') {
                quoteCount++;
            }
        }
        
        // If quotes are unbalanced, try to fix common issues
        if (quoteCount % 2 != 0) {
            // If the last character is not a brace or bracket, add a quote and closing brace
            char lastChar = json.charAt(json.length() - 1);                if (lastChar != '}' && lastChar != ']') {
                    json = json + "\"" + "}";
                } else {
                    // Otherwise, find the last quote and check if it needs to be escaped
                    int lastQuote = json.lastIndexOf('"');
                    if (lastQuote > 0 && json.charAt(lastQuote - 1) != '\\') {
                        // Add a closing quote before the last brace/bracket
                        int lastBrace = json.lastIndexOf('}');
                        int lastBracket = json.lastIndexOf(']');
                        int insertPos = Math.max(lastBrace, lastBracket);
                    
                    if (insertPos > 0) {
                        json = json.substring(0, insertPos) + "\"" + json.substring(insertPos);
                    }
                }
            }
        }
        
        return json;
    }
    
    /**
     * Extract JSON from HTML response
     * Sometimes the server returns JSON embedded in HTML
     */
    private static String extractJsonFromHtml(String htmlResponse) {
        if (htmlResponse == null || htmlResponse.isEmpty()) {
            return null;
        }
        
        try {
            // Look for JSON object pattern
            int jsonStart = htmlResponse.indexOf("{");
            int jsonEnd = htmlResponse.lastIndexOf("}");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String potentialJson = htmlResponse.substring(jsonStart, jsonEnd + 1);
                // Validate by trying to parse it
                try {
                    new JSONObject(potentialJson);
                    return potentialJson;
                } catch (JSONException e) {
                    // Try to clean the JSON string before giving up
                    String cleanedJson = potentialJson
                        .replaceAll("<br[^>]*>", "")
                        .replaceAll("<[^>]*>", "")
                        .replaceAll("\\s+", " ")
                        .trim();
                    
                    // Try again with cleaned JSON
                    new JSONObject(cleanedJson);
                    return cleanedJson;
                }
            }
            
            // Look for JSON array pattern
            jsonStart = htmlResponse.indexOf("[");
            jsonEnd = htmlResponse.lastIndexOf("]");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String potentialJson = htmlResponse.substring(jsonStart, jsonEnd + 1);
                // Validate by trying to parse it
                try {
                    new JSONArray(potentialJson);
                    return potentialJson;
                } catch (JSONException e) {
                    // Try to clean the JSON string before giving up
                    String cleanedJson = potentialJson
                        .replaceAll("<br[^>]*>", "")
                        .replaceAll("<[^>]*>", "")
                        .replaceAll("\\s+", " ")
                        .trim();
                    
                    // Try again with cleaned JSON
                    new JSONArray(cleanedJson);
                    return cleanedJson;
                }
            }
            
            // Try to find JSON in script tags (common in HTML responses)
            int scriptStart = htmlResponse.indexOf("<script");
            if (scriptStart >= 0) {
                int scriptEnd = htmlResponse.indexOf("</script>", scriptStart);
                if (scriptEnd > scriptStart) {
                    String scriptContent = htmlResponse.substring(scriptStart, scriptEnd);
                    // Look for JSON object in script
                    jsonStart = scriptContent.indexOf("{");
                    jsonEnd = scriptContent.lastIndexOf("}");
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        String potentialJson = scriptContent.substring(jsonStart, jsonEnd + 1);
                        try {
                            new JSONObject(potentialJson);
                            return potentialJson;
                        } catch (JSONException ignored) {
                            // Not valid JSON, continue searching
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "Failed to extract valid JSON: " + e.getMessage());
        }
        
        return null;
    }

    // Helper method to handle JSON parsing and validation with improved error handling
    private static JSONObject parseAndValidateJson(String jsonString, JsonResponseCallback callback) throws JSONException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new JSONException("Empty response");
        }

        // First try cleaning the response if it contains HTML tags or suspicious characters
        if (jsonString.contains("<") || jsonString.contains(">") || 
            !jsonString.startsWith("{") && !jsonString.startsWith("[")) {
            jsonString = cleanJsonResponse(jsonString);
            if (jsonString == null) {
                throw new JSONException("Invalid response after cleaning");
            }
        }

        try {
            JSONObject jsonResponse = new JSONObject(jsonString);
            
            // Check for different error response formats
            if (jsonResponse.has("error")) {
                String errorMsg = jsonResponse.getString("error");
                callback.onError(errorMsg);
                return null;
            }
            
            if (jsonResponse.has("status") && jsonResponse.getString("status").equals("error")) {
                String errorMsg = jsonResponse.optString("message", "Unknown error occurred");
                callback.onError(errorMsg);
                return null;
            }

            return jsonResponse;
        } catch (JSONException e) {
            // If it's not a JSON object, try parsing as a JSON array wrapped in an object
            if (jsonString.startsWith("[")) {
                try {
                    JSONArray jsonArray = new JSONArray(jsonString);
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("data", jsonArray);
                    return wrapper;
                } catch (JSONException e2) {
                    // If that fails too, rethrow the original exception
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }
}
