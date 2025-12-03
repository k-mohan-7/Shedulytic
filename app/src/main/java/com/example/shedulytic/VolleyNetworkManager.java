package com.example.shedulytic;

import android.content.Context;
import android.util.Log;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.Cache;
import com.android.volley.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A singleton class to manage all Volley network requests
 */
public class VolleyNetworkManager {
    private static final String TAG = "VolleyNetworkManager";
    private static final String BASE_URL = "http://10.34.179.64/schedlytic/";
    private static final String FALLBACK_URL = "http://localhost/schedlytic/";
    private static final String LOCAL_URL = "http://10.0.2.2/schedlytic/";
    
    // Reduced timeouts for better performance 
    private static final int DEFAULT_TIMEOUT_MS = 3000; // 3 seconds instead of 5
    private static final int FAST_TIMEOUT_MS = 1000; // 1 second for quick requests
    private static final int MAX_RETRIES = 1; // Reduce retries to speed up fallback
    private static final float BACKOFF_MULTIPLIER = 1.0f; // No backoff to speed up retries
    
    private static VolleyNetworkManager instance;
    private RequestQueue requestQueue;
    private Context context;
    private String activeBaseUrl = BASE_URL;
    private ConcurrentHashMap<String, Long> lastUrlSuccessTime = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> endpointCache = new ConcurrentHashMap<>();

    /**
     * Interface for JSON response listeners
     */
    public interface JsonResponseListener {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    /**
     * Interface for JSON Array response listeners
     */
    public interface ArrayResponseListener {
        void onSuccess(org.json.JSONArray response);
        void onError(String message);
    }

    /**
     * Private constructor for singleton
     */
    private VolleyNetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = getRequestQueue();
        
        // Initialize with active URL check
        findBestWorkingUrl();
    }

    /**
     * Get singleton instance
     */
    public static synchronized VolleyNetworkManager getInstance(Context context) {
        if (instance == null) {
            instance = new VolleyNetworkManager(context);
        }
        return instance;
    }

    /**
     * Get RequestQueue with custom cache settings
     */
    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // Create a request queue with a larger cache size
            com.android.volley.toolbox.DiskBasedCache cache = new com.android.volley.toolbox.DiskBasedCache(context.getCacheDir(), 10 * 1024 * 1024); // 10MB cache
            com.android.volley.Network network = new com.android.volley.toolbox.BasicNetwork(new com.android.volley.toolbox.HurlStack());
            requestQueue = new RequestQueue(cache, network);
            requestQueue.start();
        }
        return requestQueue;
    }

    /**
     * Add request to queue with tag
     */
    private <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    /**
     * Cancel all requests with tag
     */
    public void cancelAllRequests() {
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    /**
     * Build URL with the most reliable base URL
     */
    private String buildUrl(String endpoint) {
        // Check if we have a cached successful endpoint
        String cachedUrl = endpointCache.get(endpoint);
        if (cachedUrl != null) {
            // If cached URL was used successfully in the last 15 minutes, use it directly
            Long lastSuccess = lastUrlSuccessTime.get(cachedUrl);
            if (lastSuccess != null && System.currentTimeMillis() - lastSuccess < TimeUnit.MINUTES.toMillis(15)) {
                Log.d(TAG, "Using cached URL: " + cachedUrl + " for endpoint: " + endpoint);
                return cachedUrl;
            }
        }
    
        // Otherwise build from active base URL
        if (!endpoint.startsWith("http")) {
            // Only prepend base URL if not already a full URL
            return activeBaseUrl + endpoint;
        }
        return endpoint;
    }

    /**
     * Build fallback URL
     */
    private String buildFallbackUrl(String endpoint) {
        if (!endpoint.startsWith("http")) {
            // Try all possible URLs in order of likely success
            if (activeBaseUrl.equals(BASE_URL)) {
                return LOCAL_URL + endpoint;
            } else if (activeBaseUrl.equals(LOCAL_URL)) {
                return FALLBACK_URL + endpoint;
            } else {
                return BASE_URL + endpoint;
            }
        }
        return endpoint;
    }

    /**
     * Setup request configuration to improve performance
     */
    private void setupRequestConfig(Request<?> request) {
        // Use more aggressive timeout settings
        request.setRetryPolicy(new DefaultRetryPolicy(
                DEFAULT_TIMEOUT_MS,
                MAX_RETRIES,
                BACKOFF_MULTIPLIER
        ));
        
        // Enable caching for GET requests (helps with offline and performance)
        if (request.getMethod() == Request.Method.GET) {
            request.setShouldCache(true);
        } else {
            request.setShouldCache(false);
        }
    }

    /**
     * Check network availability
     */
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Get common headers for all requests
     */
    private Map<String, String> getCommonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json; charset=utf-8");
        // Adding a Connection: close header to prevent connection pooling issues
        headers.put("Connection", "close");
        return headers;
    }

    /**
     * Get detailed error message from VolleyError
     */
    private String getVolleyErrorMessage(VolleyError error) {
        String errorMessage;
        
        if (error instanceof TimeoutError) {
            errorMessage = "Connection timed out. Please check your internet connection and try again.";
        } else if (error instanceof NoConnectionError) {
            errorMessage = "No connection available. Please check your internet connection and try again.";
        } else if (error instanceof AuthFailureError) {
            errorMessage = "Authentication error. Please check your credentials and try again.";
        } else if (error instanceof ServerError) {
            errorMessage = "Server error. Please try again later.";
        } else if (error instanceof NetworkError) {
            errorMessage = "Network error. Please check your internet connection and try again.";
        } else {
            // Generic error handler
            if (error.networkResponse != null) {
                int statusCode = error.networkResponse.statusCode;
                errorMessage = "Error: HTTP " + statusCode;
                
                // Try to extract any error message from response
                try {
                    String responseBody = new String(error.networkResponse.data, "utf-8");
                    if (responseBody.length() > 0) {
                        try {
                            // Check if the response starts with a square bracket, which indicates a JSON array
                            if (responseBody.trim().startsWith("[")) {
                                // This is a JSON array, but we expected an object
                                errorMessage = "Server returned a JSON array instead of an object. Try using makeArrayGetRequest instead.";
                                Log.d(TAG, "Response is a JSON array: " + responseBody);
                            } else {
                                // Try to parse as a JSON object
                                JSONObject jsonError = new JSONObject(responseBody);
                                if (jsonError.has("message")) {
                                    errorMessage += " - " + jsonError.getString("message");
                                } else if (jsonError.has("error")) {
                                    errorMessage += " - " + jsonError.getString("error");
                                }
                            }
                        } catch (Exception e) {
                            // If not JSON, check if it's HTML and extract meaningful content
                            String cleanedMessage = extractErrorFromHtmlOrPhp(responseBody);
                            if (!cleanedMessage.isEmpty()) {
                                errorMessage += " - " + cleanedMessage;
                            } else if (responseBody.length() < 100) {
                                errorMessage += " - " + responseBody;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing error response", e);
                }
            } else {
                // Check for JSON array exceptions
                if (error.getCause() != null && error.getCause() instanceof org.json.JSONException) {
                    String exceptionMessage = error.getCause().getMessage();
                    if (exceptionMessage != null && exceptionMessage.contains("JSONArray")) {
                        errorMessage = "Server returned a JSON array instead of an object. Try using makeArrayGetRequest instead.";
                    } else {
                        errorMessage = "JSON parsing error: " + exceptionMessage;
                    }
                } else {
                    errorMessage = "Unknown error: " + (error.getMessage() != null ? error.getMessage() : "Please try again");
                }
            }
        }
        
        // Log the error for debugging
        Log.e(TAG, "Network error: " + errorMessage, error);
        
        return errorMessage;
    }

    /**
     * Extract meaningful error messages from HTML or PHP error responses
     * This method filters out HTML tags and extracts relevant error information
     */
    private String extractErrorFromHtmlOrPhp(String responseBody) {
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
    private String extractBetweenTags(String text, String startTag, String endTag) {
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
     * Validates that the response is valid JSON and parses it
     * @param response The server response string
     * @param url The URL that was requested (for logging purposes)
     * @return JSONObject if valid JSON, null if invalid
     */
    private JSONObject validateAndParseJsonResponse(String response, String url) {
        if (response == null || response.trim().isEmpty()) {
            Log.e(TAG, "Empty response from " + url);
            return null;
        }
        
        // Trim whitespace
        String trimmedResponse = response.trim();
        
        // Check if response starts with typical JSON characters
        if (!trimmedResponse.startsWith("{") && !trimmedResponse.startsWith("[")) {
            Log.d(TAG, "Response from " + url + " does not start with JSON - first 100 chars: " + 
                  trimmedResponse.substring(0, Math.min(100, trimmedResponse.length())));
            return null;
        }
        
        // Check for HTML content (common when server returns error pages)
        String lowerResponse = trimmedResponse.toLowerCase();
        if (lowerResponse.contains("<!doctype html") || 
            lowerResponse.contains("<html") || 
            lowerResponse.contains("<body") ||
            lowerResponse.contains("<head") ||
            lowerResponse.contains("<br") ||
            lowerResponse.contains("fatal error") ||
            lowerResponse.contains("parse error") ||
            lowerResponse.contains("warning:") ||
            lowerResponse.contains("notice:")) {
            Log.d(TAG, "Response from " + url + " contains HTML/PHP error content");
            return null;
        }
        
        // Try to parse as JSON
        try {
            JSONObject jsonResponse = new JSONObject(trimmedResponse);
            Log.d(TAG, "Successfully parsed JSON response from " + url);
            return jsonResponse;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON from " + url + ": " + e.getMessage());
            Log.d(TAG, "Invalid JSON content (first 200 chars): " + 
                  trimmedResponse.substring(0, Math.min(200, trimmedResponse.length())));
            return null;
        }
    }

    /**
     * Find the best working server URL
     */
    public void findBestWorkingUrl() {
        // Try to reach each URL with a minimal request
        String[] urls = {BASE_URL, LOCAL_URL, FALLBACK_URL};
        
        // Use an atomic boolean to track if we found a working URL
        AtomicBoolean foundWorkingUrl = new AtomicBoolean(false);
        
        for (String url : urls) {
            // Make a very quick HEAD request to check connectivity
            makeConnectionTest(url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if (!foundWorkingUrl.get()) {
                        // We found a working URL, use it as active
                        activeBaseUrl = url;
                        foundWorkingUrl.set(true);
                        Log.d(TAG, "Found working URL: " + url);
                    }
                }
            });
        }
    }

    /**
     * Make a lightweight connection test
     */
    private void makeConnectionTest(String url, Response.Listener<String> listener) {
        // Use a custom StringRequest to just test connectivity
        String testUrl = url + "ping.php";
        StringRequest request = new StringRequest(Request.Method.GET, testUrl,
                listener,
                error -> {
                    Log.d(TAG, "Connection test failed for " + url + ": " + getVolleyErrorMessage(error));
                });
        
        // Set very short timeout for the test
        request.setRetryPolicy(new DefaultRetryPolicy(
                500, // 500ms timeout for the test
                0,   // No retries for the test
                BACKOFF_MULTIPLIER
        ));
        
        addToRequestQueue(request);
    }

    /**
     * Get specific URLs for commonly used endpoints
     */
    public String getTodayTasksUrl(String userId, String date) {
        return activeBaseUrl + "get_today_tasks.php?date=" + date + "&user_id=" + userId;
    }
    
    /**
     * Get all tasks URL for a user
     */
    public String getAllTasksUrl(String userId) {
        // Changed from get_all_tasks.php to get_today_tasks.php
        // We'll use a date range to get all tasks
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Get current date
        String today = dateFormat.format(new Date());
        
        return activeBaseUrl + "get_today_tasks.php?date=" + today + "&user_id=" + userId;
    }
    
    /**
     * Get URL for updating tasks
     */
    public String getUpdateTaskUrl() {
        return activeBaseUrl + "update_task.php";
    }

    /**
     * Get URL for adding tasks
     */
    public String getAddTaskUrl() {
        return activeBaseUrl + "add_task.php";
    }
    
    /**
     * Get URL for task completion operations
     */
    public String getTaskCompletionUrl() {
        return activeBaseUrl + "task_completion.php";
    }

    /**
     * Make a GET request with a turbo timeout
     * Use this for data that needs to load very quickly, even if it means increased failure chance
     */
    public void makeTurboGetRequest(String endpoint, JsonResponseListener listener) {
        // Skip network check for speed
        String url = buildUrl(endpoint);
        Log.d(TAG, "Making turbo GET request to: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Turbo GET success from: " + url);
                    // Cache successful URL for future requests
                    endpointCache.put(endpoint, url);
                    lastUrlSuccessTime.put(url, System.currentTimeMillis());
                    
                    listener.onSuccess(response);
                },
                error -> {
                    String errorMessage = getVolleyErrorMessage(error);
                    Log.e(TAG, "Turbo GET error from " + url + ": " + errorMessage);

                    // Try with fallback URL if this is the first attempt
                        String fallbackUrl = buildFallbackUrl(endpoint);
                        if (!fallbackUrl.equals(url)) {
                        Log.d(TAG, "Trying fallback for turbo GET: " + fallbackUrl);
                            makeGetRequestWithUrl(fallbackUrl, listener);
                    } else {
                        listener.onError(errorMessage);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };

        // Ultra-fast timeout with no retries
        request.setRetryPolicy(new DefaultRetryPolicy(
                FAST_TIMEOUT_MS, // 1 second timeout
                0,               // No retries
                BACKOFF_MULTIPLIER
        ));
        
        request.setShouldCache(true);
        addToRequestQueue(request);
    }

    /**
     * Execute multiple parallel requests to different endpoints and merge results
     * This is used to improve reliability by trying multiple server options at once
     */
    public void makeBroadcastGetRequest(String[] endpoints, JsonResponseListener listener) {
        final int requestCount = endpoints.length;
        final AtomicBoolean hasResponded = new AtomicBoolean(false);
        
        for (String endpoint : endpoints) {
            String url = buildUrl(endpoint);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                        // Only process the first successful response
                        if (hasResponded.compareAndSet(false, true)) {
                            Log.d(TAG, "Broadcast GET success from: " + url);
                            
                            // Remember this was a good URL
                            lastUrlSuccessTime.put(url, System.currentTimeMillis());
                            endpointCache.put(endpoint, url);
                            
                    listener.onSuccess(response);
                        }
                },
                error -> {
                        Log.e(TAG, "Broadcast GET error from " + url + ": " + getVolleyErrorMessage(error));
                        // We do nothing on individual errors, only if all fail
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };

            // Extra short timeout since we're making multiple parallel requests
            request.setRetryPolicy(new DefaultRetryPolicy(
                    FAST_TIMEOUT_MS, // 1 second timeout
                    0,               // No retries 
                    BACKOFF_MULTIPLIER
            ));
            
            request.setShouldCache(true);
        addToRequestQueue(request);
        }
        
        // Add a final request as a fallback in case all endpoints fail
        // This leverages the regular request mechanism with fallbacks
        // We delay it slightly to give the parallel requests a chance
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(() -> {
            if (!hasResponded.get()) {
                // All parallel requests failed, try the regular way
                Log.d(TAG, "All broadcast requests failed, trying regular request");
                makeGetRequestWithTimeout(endpoints[0], listener, DEFAULT_TIMEOUT_MS);
            }
        }, FAST_TIMEOUT_MS + 300); // Wait just slightly longer than the timeout
    }

    // Improved HurlStack with connection pooling
    private static class CustomHurlStack extends com.android.volley.toolbox.HurlStack {
        @Override
        protected java.net.HttpURLConnection createConnection(java.net.URL url) throws java.io.IOException {
            java.net.HttpURLConnection connection = super.createConnection(url);
            // Disable connection pooling to prevent timeouts
            connection.setRequestProperty("Connection", "close");
            // Set shorter timeouts
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(3000);
            return connection;
        }
    }

    /**
     * Make a GET request
     */
    public void makeGetRequest(String endpoint, JsonResponseListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("No network connection available");
            return;
        }
        
        String url = buildUrl(endpoint);
        makeGetRequestWithUrl(url, listener);
    }
    
    /**
     * Make a GET request with specific URL
     */
    private void makeGetRequestWithUrl(String url, JsonResponseListener listener) {
        Log.d(TAG, "Making GET request to: " + url);
        
        // Use StringRequest first to validate JSON before parsing
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d(TAG, "GET success from: " + url);
                    
                    // Validate that response is actual JSON before parsing
                    JSONObject jsonResponse = validateAndParseJsonResponse(response, url);
                    if (jsonResponse != null) {
                        // Cache successful URL for future requests
                        lastUrlSuccessTime.put(url, System.currentTimeMillis());
                        listener.onSuccess(jsonResponse);
                    } else {
                        // Response was not valid JSON, treat as error
                        String errorMsg = "Server returned non-JSON response: " + extractErrorFromHtmlOrPhp(response);
                        if (errorMsg.isEmpty() || errorMsg.contains("Server returned non-JSON response: ")) {
                            errorMsg = "Server returned invalid response format";
                        }
                        Log.e(TAG, "Invalid JSON response from " + url + ": " + response.substring(0, Math.min(200, response.length())));
                        
                        // Try with fallback URL if this is not already a fallback
                        String fallbackUrl = buildFallbackUrl(url);
                        if (!fallbackUrl.equals(url)) {
                            Log.d(TAG, "Trying fallback for GET due to invalid JSON: " + fallbackUrl);
                            makeGetRequestWithUrl(fallbackUrl, listener);
                        } else {
                            listener.onError(errorMsg);
                        }
                    }
                },
                error -> {
                    String errorMessage = getVolleyErrorMessage(error);
                    Log.e(TAG, "GET error from " + url + ": " + errorMessage);
                    
                    // Try with fallback URL if this is not already a fallback
                    String fallbackUrl = buildFallbackUrl(url);
                    if (!fallbackUrl.equals(url)) {
                        Log.d(TAG, "Trying fallback for GET: " + fallbackUrl);
                        makeGetRequestWithUrl(fallbackUrl, listener);
                    } else {
                        listener.onError(errorMessage);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };

        setupRequestConfig(request);
        addToRequestQueue(request);
    }

    /**
     * Make a GET request with custom timeout
     */
    public void makeGetRequestWithTimeout(String endpoint, JsonResponseListener listener, int timeoutMs) {
        if (!isNetworkAvailable()) {
            listener.onError("No network connection available");
            return;
        }
        
        String url = buildUrl(endpoint);
        Log.d(TAG, "Making GET request with timeout to: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "GET success from: " + url);
                    lastUrlSuccessTime.put(url, System.currentTimeMillis());
                    
                    listener.onSuccess(response);
                },
                error -> {
                    String errorMessage = getVolleyErrorMessage(error);
                    Log.e(TAG, "GET error from " + url + ": " + errorMessage);

                    // Try with fallback URL
                    String fallbackUrl = buildFallbackUrl(url);
                        if (!fallbackUrl.equals(url)) {
                        Log.d(TAG, "Trying fallback for GET: " + fallbackUrl);
                        makeGetRequestWithUrl(fallbackUrl, listener);
                    } else {
                        listener.onError(errorMessage);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };

        // Custom timeout
        request.setRetryPolicy(new DefaultRetryPolicy(
                timeoutMs,
                MAX_RETRIES,
                BACKOFF_MULTIPLIER
        ));
        
        request.setShouldCache(true);
        addToRequestQueue(request);
    }

    /**
     * Make a POST request
     */
    public void makePostRequest(String endpoint, JSONObject requestData, JsonResponseListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("No network connection available");
            return;
        }
        
        String url = buildUrl(endpoint);
        Log.d(TAG, "Making POST request to: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestData,
                response -> {
                    Log.d(TAG, "POST success to: " + url);
                    lastUrlSuccessTime.put(url, System.currentTimeMillis());
                    
                    listener.onSuccess(response);
                },
                error -> {
                    String errorMessage = getVolleyErrorMessage(error);
                    Log.e(TAG, "POST error to " + url + ": " + errorMessage);
                    
                    // Try with fallback URL
                    String fallbackUrl = buildFallbackUrl(url);
                    if (!fallbackUrl.equals(url)) {
                        Log.d(TAG, "Trying fallback for POST: " + fallbackUrl);
                        makePostRequestWithUrl(fallbackUrl, requestData, listener);
                    } else {
                    listener.onError(errorMessage);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };

        setupRequestConfig(request);
        addToRequestQueue(request);
    }

    /**
     * Make a POST request with specific URL
     */
    private void makePostRequestWithUrl(String url, JSONObject requestData, JsonResponseListener listener) {
        Log.d(TAG, "Making POST request to: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestData,
                response -> {
                    Log.d(TAG, "POST success to: " + url);
                    lastUrlSuccessTime.put(url, System.currentTimeMillis());
                    
                        listener.onSuccess(response);
                    },
                    error -> {
                        String errorMessage = getVolleyErrorMessage(error);
                    Log.e(TAG, "POST error to " + url + ": " + errorMessage);
                        listener.onError(errorMessage);
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return getCommonHeaders();
                }
            };
            
            setupRequestConfig(request);
            addToRequestQueue(request);
    }
    
    /**
     * Make a POST request with timeout
     */
    public void makePostRequestWithTimeout(String endpoint, JSONObject requestData, JsonResponseListener listener, int timeoutMs) {
        if (!isNetworkAvailable()) {
            listener.onError("No network connection available");
            return;
        }
        
        String url = buildUrl(endpoint);
        Log.d(TAG, "Making POST request with timeout to: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestData,
                response -> {
                    Log.d(TAG, "POST success to: " + url);
                    lastUrlSuccessTime.put(url, System.currentTimeMillis());
                    
                    listener.onSuccess(response);
                },
                error -> {
                    String errorMessage = getVolleyErrorMessage(error);
                    Log.e(TAG, "POST error to " + url + ": " + errorMessage);
                    
                    // Try with fallback URL
                    String fallbackUrl = buildFallbackUrl(url);
                    if (!fallbackUrl.equals(url)) {
                        Log.d(TAG, "Trying fallback for POST: " + fallbackUrl);
                        makePostRequestWithUrl(fallbackUrl, requestData, listener);
                    } else {
                        listener.onError(errorMessage);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };

        // Custom timeout
        request.setRetryPolicy(new DefaultRetryPolicy(
                timeoutMs,
                MAX_RETRIES,
                BACKOFF_MULTIPLIER
        ));
        
        setupRequestConfig(request);
        addToRequestQueue(request);
    }
    
    /**
     * Get user streak URL
     */
    public String getUserStreakUrl(String userId, String startDate, String endDate) {
        return activeBaseUrl + "get_user_streak.php?user_id=" + userId + 
               "&start_date=" + startDate + "&end_date=" + endDate;
    }
    
    /**
     * Get user profile URL
     */
    public String getUserProfileUrl(String userId) {
        return activeBaseUrl + "get_user_profile.php?user_id=" + userId;
    }

    /**
     * Get URL for updating user activity
     */
    public String getUpdateUserActivityUrl() {
        return activeBaseUrl + "update_user_activity.php";
    }

    /**
     * Find the best working server URL with a callback
     */
    public void findWorkingServerUrl(Runnable callback) {
        // Find best working URL
        findBestWorkingUrl();
        
        // Wait briefly and execute callback
        new android.os.Handler().postDelayed(callback, 500);
    }

    /**
     * Make a custom Volley request
     * This method allows direct access to adding a request to the queue from outside classes
     */
    public <T> void makeCustomRequest(Request<T> request) {
        Log.d(TAG, "Making custom request to: " + request.getUrl());
        
        // Configure the request with our standard settings
        setupRequestConfig(request);
        
        // Add to the queue
        addToRequestQueue(request);
    }

    /**
     * Make a GET request that expects a JSON array response (for handling habit data)
     */
    public void makeArrayGetRequest(String endpoint, ArrayResponseListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("No network connection available");
            return;
        }
        
        String url = buildUrl(endpoint);
        Log.d(TAG, "Making JSON Array GET request to: " + url);
        
        com.android.volley.toolbox.JsonArrayRequest request = new com.android.volley.toolbox.JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "JSON Array GET success from: " + url);
                    // Cache successful URL for future requests
                    lastUrlSuccessTime.put(url, System.currentTimeMillis());
                    
                    listener.onSuccess(response);
                },
                error -> {
                    String errorMessage = getVolleyErrorMessage(error);
                    Log.e(TAG, "JSON Array GET error from " + url + ": " + errorMessage);
                    
                    // Try with fallback URL if this is not already a fallback
                    String fallbackUrl = buildFallbackUrl(url);
                    if (!fallbackUrl.equals(url)) {
                        Log.d(TAG, "Trying fallback for JSON Array GET: " + fallbackUrl);
                        makeArrayGetRequestWithUrl(fallbackUrl, listener);
                    } else {
                        listener.onError(errorMessage);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };

        setupRequestConfig(request);
        addToRequestQueue(request);
    }
    
    /**
     * Make a JSON Array GET request with specific URL
     */
    private void makeArrayGetRequestWithUrl(String url, ArrayResponseListener listener) {
        Log.d(TAG, "Making JSON Array GET request to: " + url);
        
        com.android.volley.toolbox.JsonArrayRequest request = new com.android.volley.toolbox.JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "JSON Array GET success from: " + url);
                    // Cache successful URL for future requests
                    lastUrlSuccessTime.put(url, System.currentTimeMillis());
                    
                    listener.onSuccess(response);
                },
                error -> {
                    String errorMessage = getVolleyErrorMessage(error);
                    Log.e(TAG, "JSON Array GET error from " + url + ": " + errorMessage);
                    listener.onError(errorMessage);
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getCommonHeaders();
            }
        };

        setupRequestConfig(request);
        addToRequestQueue(request);
    }
    
    /**
     * Get the current base URL being used
     */
    public String getBaseUrl() {
        return activeBaseUrl;
    }
    
    /**
     * Get the URL for deleting a task
     */
    public String getDeleteTaskUrl() {
        return activeBaseUrl + "delete_task.php";
    }
}