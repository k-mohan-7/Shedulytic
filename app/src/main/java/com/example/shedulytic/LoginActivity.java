package com.example.shedulytic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.shedulytic.ui.login.IpV4Connection;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class LoginActivity extends AppCompatActivity {
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
        new Thread(() -> {
            try {
                URL url = new URL(IpV4Connection.getBaseUrl() + "login.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject requestBody = new JSONObject();
                requestBody.put("email", email);
                requestBody.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(this, "Server Error: " + responseCode, Toast.LENGTH_SHORT).show());
                    return;
                }

                Scanner scanner = new Scanner(conn.getInputStream());
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                // Improved error handling for JSON parsing
                try {
                    JSONObject jsonResponse = new JSONObject(response.toString());
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
                                editor.commit(); // Using commit instead of apply for immediate effect

                                // Get username from response if available
                                String username = "";
                                if (user.has("username")) {
                                    username = user.getString("username");
                                    // Store username in SharedPreferences
                                    editor.putString("username", username);
                                }
                                
                                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, HomepageActivity.class);
                                intent.putExtra("user_id", userId); // Pass user_id to HomepageActivity
                                intent.putExtra("username", username); // Pass username to HomepageActivity
                                startActivity(intent);
                                finish();
                            } else {
                                String message = jsonResponse.optString("message", "Invalid credentials!");
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Error parsing user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                });
                } catch (JSONException e) {
                    Log.e("JSON Error", "Error parsing JSON: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Server parser failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        }).start();
    }
}