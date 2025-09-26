package com.example.shedulytic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.shedulytic.ui.login.IpV4Connection;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        if (userId.isEmpty()) {
            return;
        }

        // Get today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(Calendar.getInstance().getTime());
        
        // Use today's date when fetching tasks
        String url = IpV4Connection.getTodayTasksUrl(userId, today);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getString("status").equals("success")) {
                        JSONArray tasksArray = response.getJSONArray("tasks");
                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskJson = tasksArray.getJSONObject(i);
                            Task task = new Task(
                                taskJson.getString("task_id"),  // Changed from getInt to getString
                                userId,
                                taskJson.getString("type"),
                                taskJson.getString("title"),
                                taskJson.getString("description"),
                                taskJson.getString("start_time"),
                                taskJson.getString("end_time"),
                                today, // Using today as the due date
                                taskJson.getBoolean("completed") ? "completed" : "pending",
                                "none", // Default repeat frequency
                                "medium" // Default priority
                            );
                            // Schedule notification for this task
                            scheduleNotification(context, task);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing tasks: " + e.getMessage());
                }
            },
            error -> Log.e(TAG, "Error fetching tasks: " + error.getMessage())
        );

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(request);
    }

    private void scheduleNotification(Context context, Task task) {
        // Implementation for scheduling notification
    }
}