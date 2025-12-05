package com.example.shedulytic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerNow = findViewById(R.id.registerNow);

        loginButton.setOnClickListener(view -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password are required.", Toast.LENGTH_SHORT).show();
                return;
            }
            loginUser(email, password);
        });

        registerNow.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(String email, String password) {
        // Show loading message
        runOnUiThread(() -> Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show());

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // Try multiple server URLs in sequence - primary IP should match IpV4Connection
                String[] serverUrls = {
                    IpV4Connection.getBaseUrl(),  // Use centralized IP configuration
                    "http://10.0.2.2/shedulytic/",
                    "http://localhost/shedulytic/"
                };
                
                boolean connected = false;
                String responseData = "";
                int responseCode = 0;
                
                // Try each URL until one works
                for (String baseUrl : serverUrls) {
                    try {
                        Log.d(TAG, "Trying to connect to: " + baseUrl);
                        URL url = new URL(baseUrl + "login.php");
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        conn.setConnectTimeout(10000); // 10 seconds timeout
                        conn.setReadTimeout(10000);

                        JSONObject requestBody = new JSONObject();
                        requestBody.put("email", email);
                        requestBody.put("password", password);

                        OutputStream os = conn.getOutputStream();
                        os.write(requestBody.toString().getBytes("UTF-8"));
                        os.close();

                        responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
                            StringBuilder response = new StringBuilder();
                            while (scanner.hasNextLine()) {
                                response.append(scanner.nextLine());
                            }
                            scanner.close();
                            
                            responseData = response.toString();
                            Log.d(TAG, "Response: " + responseData);
                            connected = true;
                            break; // Success, exit the loop
                        } else {
                            Log.d(TAG, "Server returned error code: " + responseCode);
                            if (conn != null) conn.disconnect();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error connecting to " + baseUrl + ": " + e.getMessage());
                        if (conn != null) conn.disconnect();
                    }
                }
                
                if (!connected) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, 
                        "Could not connect to server. Please check your network connection.", 
                        Toast.LENGTH_LONG).show());
                    return;
                }
                
                // Process the response
                final String finalResponseData = responseData;
                try {
                    JSONObject jsonResponse = new JSONObject(finalResponseData);
                    String status = jsonResponse.optString("status", "error");

                    runOnUiThread(() -> {
                        try {
                            if ("success".equals(status)) {
                                JSONObject user = jsonResponse.getJSONObject("user");
                                String userId = user.getString("user_id");

                                // Store user_id in SharedPreferences with commit for immediate write
                                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("user_id", userId);
                                
                                // Mark user as logged in for persistent login
                                editor.putBoolean("is_logged_in", true);
                                editor.putBoolean("onboarding_completed", true);

                                // Get username from response with better fallback handling
                                String username = "";
                                if (user.has("username") && !user.isNull("username")) {
                                    username = user.getString("username");
                                } else if (user.has("name") && !user.isNull("name")) {
                                    username = user.getString("name");
                                } else if (user.has("email") && !user.isNull("email")) {
                                    // Use email as last resort for display name
                                    username = user.getString("email").split("@")[0];
                                }

                                // Store username in SharedPreferences
                                if (!username.isEmpty()) {
                                    editor.putString("username", username);
                                    Log.d(TAG, "Stored username: " + username);
                                }

                                // Store XP points with default value if not available
                                int xpPoints = 0;
                                if (user.has("xp_points") && !user.isNull("xp_points")) {
                                    xpPoints = user.getInt("xp_points");
                                }
                                editor.putInt("xp_points", xpPoints);
                                Log.d(TAG, "Stored XP points: " + xpPoints);

                                // Commit all changes at once
                                editor.commit();
                                
                                // Log user activity for streak tracking
                                logUserActivityForStreak(userId);

                                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainNavigationActivity.class);
                                intent.putExtra("user_id", userId);
                                intent.putExtra("username", username);
                                intent.putExtra("xp_points", xpPoints);
                                startActivity(intent);
                                finish();
                            } else {
                                String message = jsonResponse.optString("message", "Invalid credentials!");
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user data: " + e.getMessage(), e);
                            Toast.makeText(this, "Error parsing user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, 
                        "Server response error: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, 
                    "Network error: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
    
    /**
     * Log user activity for streak tracking when user logs in
     */
    private void logUserActivityForStreak(String userId) {
        new Thread(() -> {
            try {
                String baseUrl = IpV4Connection.getBaseUrl();
                // Ensure proper URL construction with / separator
                String endpoint = baseUrl.endsWith("/") ? baseUrl + "update_user_activity.php" : baseUrl + "/update_user_activity.php";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);
                
                // Send POST data
                String postData = "user_id=" + userId + "&activity_type=login";
                conn.getOutputStream().write(postData.getBytes("UTF-8"));
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine());
                    }
                    scanner.close();
                    Log.d(TAG, "Activity logged for streak: " + response.toString());
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error logging activity for streak: " + e.getMessage());
            }
        }).start();
    }
}
