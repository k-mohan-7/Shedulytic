package com.example.shedulytic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.EditText;
import android.util.Log;
import android.widget.Toast;

import com.example.shedulytic.ui.login.IpV4Connection;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SignupActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView loginLink;
    private Button registerButton;
    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        // Initialize view references
        backButton = findViewById(R.id.backButton);
        loginLink = findViewById(R.id.loginLink);
        registerButton = findViewById(R.id.registerButton);
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        // Set click listener for the back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Set click listener for the login link
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Set click listener for the register button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        final String name = usernameInput.getText().toString().trim();
        final String username = usernameInput.getText().toString().trim();
        final String email = emailInput.getText().toString().trim();
        final String password = passwordInput.getText().toString().trim();
        final String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(SignupActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(SignupActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // URL of your PHP registration script
                URL url = new URL(IpV4Connection.getBaseUrl() + "signup.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Create JSON request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("name", name);
                requestBody.put("username", username);
                requestBody.put("email", email);
                requestBody.put("password", password);
                requestBody.put("confirm_password", confirmPassword);

                // Send the request
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8)); // Use StandardCharsets
                os.close();

                final int responseCode = conn.getResponseCode(); //  Get response code
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Server error: " + responseCode, Toast.LENGTH_SHORT).show());
                    return;
                }

                // Read the response
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name()); // Specify encoding
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                final String finalResponse = response.toString(); // Store response for UI thread

                // Parse the JSON response on the main thread
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(finalResponse);
                        String status = jsonResponse.optString("status", "error");
                        String message = jsonResponse.optString("message", "An error occurred");

                        if ("success".equals(status)) {
                            // Store user ID if available in response
                            try {
                                if (jsonResponse.has("user")) {
                                    JSONObject user = jsonResponse.getJSONObject("user");
                                    if (user.has("user_id")) {
                                        String userId = user.getString("user_id");
                                        
                                        // Store user_id in SharedPreferences
                                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString("user_id", userId);
                                        editor.commit(); // Using commit instead of apply for immediate effect
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("User Data", "Error storing user data: " + e.getMessage());
                            }
                            
                            Toast.makeText(SignupActivity.this, message, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignupActivity.this, LoginActivity.class); // Redirect to login after signup
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON Error", "Error parsing JSON: " + e.getMessage());
                        Toast.makeText(SignupActivity.this, "Error parsing server response", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e("Exception", "Error during registration: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
