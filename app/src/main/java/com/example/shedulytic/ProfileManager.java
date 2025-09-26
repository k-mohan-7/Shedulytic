package com.example.shedulytic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.example.shedulytic.ui.login.IpV4Connection;

public class ProfileManager {
    private static final String TAG = "ProfileManager";
    private final Context context;
    private final ProfileLoadListener listener;

    public interface ProfileLoadListener {
        void onProfileLoaded(String name, String email, String avatarUrl);
        void onError(String message);
    }

    public ProfileManager(Context context, ProfileLoadListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void loadUserProfile() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                String userId = prefs.getString("user_id", "");
                
                if (userId.isEmpty()) {
                    listener.onError("User ID not found");
                    return;
                }

                String urlString = IpV4Connection.getUserProfileUrl(userId);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.getString("status").equals("success")) {
                        JSONObject userData = jsonResponse.getJSONObject("user");
                        String name = userData.getString("name");
                        String email = userData.getString("email");
                        String avatarUrl = userData.optString("avatar_url", "");
                        
                        listener.onProfileLoaded(name, email, avatarUrl);
                    } else {
                        listener.onError(jsonResponse.optString("message", "Failed to load profile"));
                    }
                } else {
                    listener.onError("Server returned code: " + responseCode);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile: " + e.getMessage(), e);
                listener.onError("Error: " + e.getMessage());
            }
        }).start();
    }
}