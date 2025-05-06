package com.example.shedulytic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shedulytic.ui.login.IpV4Connection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TimelineActivity extends AppCompatActivity {
    private static final String TAG = "TimelineActivity";
    private LinearLayout timelineContainer;
    private List<Task> taskList = new ArrayList<>();
    private String userId;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private boolean isRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);
        timelineContainer = findViewById(R.id.timeline_container);

        handler = new Handler(Looper.getMainLooper());

        userId = getIntent().getStringExtra("user_id");
        if (userId == null) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("user_id", null);
        }

        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (userId != null) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("user_id", userId);
            editor.apply();
        }

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    fetchTasks();
                    handler.postDelayed(this, 60000); // Refresh every minute
                }
            }
        };

        fetchTasks();
    }

    private void fetchTasks() {
        if (isRefreshing) return;
        isRefreshing = true;

        new Thread(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                String viewTasksUrl = IpV4Connection.getBaseUrl() + "view_tasks.php?user_id=" + userId;
                URL url = new URL(viewTasksUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setUseCaches(false);
                conn.setDefaultUseCaches(false);

                if (!isNetworkAvailable()) {
                    runOnUiThread(() -> {
                        Toast.makeText(TimelineActivity.this, "No network connection available", Toast.LENGTH_SHORT).show();
                        isRefreshing = false;
                    });
                    return;
                }

                conn.connect();
                int responseCode = conn.getResponseCode();

                Log.d(TAG, "Response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    String responseData = response.toString();
                    Log.d(TAG, "Response: " + responseData);

                    if (responseData.isEmpty()) {
                        runOnUiThread(() -> {
                            Toast.makeText(TimelineActivity.this, "Empty response from server", Toast.LENGTH_SHORT).show();
                            isRefreshing = false;
                        });
                        return;
                    }

                    JSONObject jsonResponse;
                    try {
                        jsonResponse = new JSONObject(responseData);
                        String status = jsonResponse.optString("status", "error");

                        if ("success".equals(status) && jsonResponse.has("tasks")) {
                            List<Task> newTaskList = new ArrayList<>();
                            JSONArray tasks = jsonResponse.getJSONArray("tasks");
                            for (int i = 0; i < tasks.length(); i++) {
                                JSONObject task = tasks.getJSONObject(i);
                                String taskId = task.optString("id", "");
                                if (taskId.isEmpty()) {
                                    taskId = String.valueOf(task.optInt("id", 0));
                                }
                                Task newTask = new Task(
                                        taskId,
                                        userId,
                                        task.optString("task_type", "workflow"),
                                        task.optString("title", ""),
                                        task.optString("description", ""),
                                        task.optString("start_time", null),
                                        task.optString("end_time", null),
                                        task.optString("due_date", null),
                                        task.optString("status", "pending"),
                                        task.optString("repeat_frequency", "none"),
                                        task.optString("priority", "medium")
                                );
                                newTaskList.add(newTask);
                            }
                            taskList.clear();
                            taskList.addAll(newTaskList);
                            processRepeatingTasks();
                            runOnUiThread(() -> {
                                updateTimeline();
                                isRefreshing = false;
                            });
                        } else {
                            String message = jsonResponse.optString("message", "Unknown error");
                            Log.e(TAG, "Error fetching tasks: " + message);
                            runOnUiThread(() -> {
                                Toast.makeText(TimelineActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                isRefreshing = false;
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(TimelineActivity.this, "Server parser failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            isRefreshing = false;
                        });
                    }
                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "Authentication error: " + responseCode);
                    runOnUiThread(() -> {
                        Toast.makeText(TimelineActivity.this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(TimelineActivity.this, LoginActivity.class));
                        finish();
                    });
                } else {
                    String errorMessage = "";
                    try {
                        InputStream errorStream = conn.getErrorStream();
                        if (errorStream != null) {
                            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                            StringBuilder errorResponse = new StringBuilder();
                            String errorLine;
                            while ((errorLine = errorReader.readLine()) != null) {
                                errorResponse.append(errorLine);
                            }
                            errorReader.close();
                            errorMessage = errorResponse.toString();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error stream", e);
                    }
                    final String finalErrorMessage = errorMessage.isEmpty() ?
                            "Server error: " + responseCode : errorMessage;
                    Log.e(TAG, "Server returned error: " + finalErrorMessage);
                    runOnUiThread(() -> {
                        Toast.makeText(TimelineActivity.this, finalErrorMessage, Toast.LENGTH_SHORT).show();
                        isRefreshing = false;
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(TimelineActivity.this, "Network error: Check your connection", Toast.LENGTH_SHORT).show();
                    isRefreshing = false;
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(TimelineActivity.this, "Unexpected error occurred", Toast.LENGTH_SHORT).show();
                    isRefreshing = false;
                });
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing reader", e);
                }
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void processRepeatingTasks() {
        List<Task> expandedTasks = new ArrayList<>(taskList);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        final long THIRTY_DAYS_MS = TimeUnit.DAYS.toMillis(30);
        final Date endLimit = new Date(System.currentTimeMillis() + THIRTY_DAYS_MS);

        for (Task task : taskList) {
            String repeatFrequency = task.getRepeatFrequency();
            if ("none".equals(repeatFrequency) || task.getStartTime() == null) continue;

            try {
                Date startDate = sdf.parse(task.getStartTime());
                if (startDate == null) continue;
                if (startDate.before(now.getTime()) && "none".equals(repeatFrequency)) continue;

                int field = getCalendarField(repeatFrequency);
                if (field == 0) continue;

                long duration = 0;
                if (task.getEndTime() != null) {
                    Date originalEnd = sdf.parse(task.getEndTime());
                    if (originalEnd != null) {
                        duration = originalEnd.getTime() - startDate.getTime();
                    }
                }

                calendar.setTime(startDate);
                int maxOccurrences = 10;
                int count = 0;
                while (calendar.getTime().before(endLimit) && count < maxOccurrences) {
                    calendar.add(field, 1);
                    Date newStart = calendar.getTime();
                    if (newStart.before(now.getTime())) continue;

                    String formattedEndTime = null;
                    if (duration > 0) {
                        Date newEnd = new Date(newStart.getTime() + duration);
                        formattedEndTime = sdf.format(newEnd);
                    }

                    Task repeatedTask = new Task(
                            task.getTaskId(),
                            task.getUserId(),
                            task.getTaskType(),
                            task.getTitle(),
                            task.getDescription(),
                            sdf.format(newStart),
                            formattedEndTime,
                            task.getDueDate(),
                            task.getStatus(),
                            "none",
                            task.getPriority()
                    );
                    expandedTasks.add(repeatedTask);
                    count++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing repeating task: " + task.getTitle(), e);
            }
        }

        taskList = expandedTasks;
    }

    private int getCalendarField(String frequency) {
        switch (frequency) {
            case "daily": return Calendar.DAY_OF_YEAR;
            case "weekly": return Calendar.WEEK_OF_YEAR;
            case "monthly": return Calendar.MONTH;
            default: return 0;
        }
    }

    private void updateTimeline() {
        timelineContainer.removeAllViews();

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        Calendar now = Calendar.getInstance();
        int currentDayOfYear = now.get(Calendar.DAY_OF_YEAR);
        int currentYear = now.get(Calendar.YEAR);

        taskList.sort((t1, t2) -> {
            if (t1.getStartTime() == null || t2.getStartTime() == null) return 0;
            return t1.getStartTime().compareTo(t2.getStartTime());
        });

        Map<Integer, List<Task>> tasksByHour = new HashMap<>();
        Calendar taskCal = Calendar.getInstance();

        for (Task task : taskList) {
            if (task.getStartTime() == null) continue;
            try {
                Date start = fullDateFormat.parse(task.getStartTime());
                if (start == null) continue;
                taskCal.setTime(start);
                if (taskCal.get(Calendar.YEAR) == currentYear &&
                        taskCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear) {
                    int hour = taskCal.get(Calendar.HOUR_OF_DAY);
                    if (!tasksByHour.containsKey(hour)) {
                        tasksByHour.put(hour, new ArrayList<>());
                    }
                    tasksByHour.get(hour).add(task);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing task start time: " + task.getTitle(), e);
            }
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int hour = 0; hour < 24; hour++) {
            LinearLayout timeSlot = new LinearLayout(this);
            timeSlot.setOrientation(LinearLayout.HORIZONTAL);
            timeSlot.setPadding(8, 8, 8, 8);

            TextView timeText = new TextView(this);
            timeText.setText(String.format(Locale.getDefault(), "%02d:00 %s",
                    hour % 12 == 0 ? 12 : hour % 12, hour < 12 ? "am" : "pm"));
            timeText.setTextSize(14);
            timeText.setTextColor(0xFF333333);
            timeText.setTextIsSelectable(false);
            timeText.setCursorVisible(false);
            timeSlot.addView(timeText);

            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            line.setBackgroundColor(0xFFCCCCCC);
            timeSlot.addView(line);

            timelineContainer.addView(timeSlot);

            List<Task> tasksForHour = tasksByHour.get(hour);
            if (tasksForHour != null && !tasksForHour.isEmpty()) {
                for (Task task : tasksForHour) {
                    try {
                        View taskView = inflater.inflate(R.layout.task_card, timelineContainer, false);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(8, 8, 8, 8);
                        taskView.setLayoutParams(params);

                        taskView.setClickable(true);
                        taskView.setFocusable(true);

                        TypedValue outValue = new TypedValue();
                        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                        taskView.setBackgroundResource(outValue.resourceId);

                        final String taskId = task.getTaskId();
                        final String taskTitle = task.getTitle();
                        taskView.setOnClickListener(v -> showTaskOptions(taskId, taskTitle, task));

                        TextView taskTypeLabel = taskView.findViewById(R.id.taskTypeLabel);
                        TextView titleView = taskView.findViewById(R.id.taskTitle);
                        TextView timeView = taskView.findViewById(R.id.taskTime);
                        TextView durationView = taskView.findViewById(R.id.taskDuration);
                        ImageView iconView = taskView.findViewById(R.id.taskIcon);

                        titleView.setTextIsSelectable(false);
                        timeView.setTextIsSelectable(false);
                        durationView.setTextIsSelectable(false);
                        taskTypeLabel.setTextIsSelectable(false);

                        taskTypeLabel.setText(task.getTaskType().toUpperCase());
                        titleView.setText(task.getTitle());

                        int backgroundColor;
                        switch (task.getTaskType().toLowerCase()) {
                            case "workflow":
                                backgroundColor = getResources().getColor(android.R.color.holo_green_light);
                                break;
                            case "habit":
                                backgroundColor = getResources().getColor(android.R.color.holo_orange_light);
                                break;
                            case "reminder":
                                backgroundColor = getResources().getColor(android.R.color.holo_blue_light);
                                break;
                            default:
                                backgroundColor = getResources().getColor(android.R.color.darker_gray);
                                break;
                        }

                        GradientDrawable shape = new GradientDrawable();
                        shape.setColor(backgroundColor);
                        shape.setCornerRadius(8);

                        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
                        TypedArray typedArray = getTheme().obtainStyledAttributes(attrs);
                        Drawable foreground = typedArray.getDrawable(0);
                        typedArray.recycle();

                        Drawable[] layers = new Drawable[]{shape, foreground};
                        LayerDrawable layerDrawable = new LayerDrawable(layers);
                        taskView.setBackground(layerDrawable);

                        Date start = fullDateFormat.parse(task.getStartTime());
                        Date end = task.getEndTime() != null ? fullDateFormat.parse(task.getEndTime()) : null;

                        String endTimeStr = end != null ? timeFormat.format(end) : "";
                        timeView.setText(timeFormat.format(start) + (endTimeStr.isEmpty() ? "" : " - " + endTimeStr));

                        if (end != null) {
                            long durationMs = end.getTime() - start.getTime();
                            int durationMins = (int) (durationMs / (1000 * 60));
                            durationView.setText(durationMins + " mins");
                        } else {
                            durationView.setVisibility(View.GONE);
                        }

                        switch (task.getTaskType()) {
                            case "habit":
                                taskView.setBackgroundColor(0xFFFFEB3B);
                                taskTypeLabel.setTextColor(0xFF000000);
                                titleView.setTextColor(0xFF000000);
                                timeView.setTextColor(0xFF333333);
                                durationView.setTextColor(0xFF333333);
                                iconView.setImageResource(R.drawable.ic_dumbbell);
                                break;
                            case "reminder":
                                taskView.setBackgroundColor(0xFF195C31);
                                taskTypeLabel.setTextColor(0xFFFFFFFF);
                                titleView.setTextColor(0xFFFFFFFF);
                                timeView.setTextColor(0xFFFFFFFF);
                                durationView.setTextColor(0xFFFFFFFF);
                                iconView.setImageResource(R.drawable.ic_group);
                                iconView.setColorFilter(0xFFFFFFFF);
                                break;
                            case "workflow":
                            default:
                                taskView.setBackgroundColor(0xFF0D3B66);
                                taskTypeLabel.setTextColor(0xFFFFFFFF);
                                titleView.setTextColor(0xFFFFFFFF);
                                timeView.setTextColor(0xFFFFFFFF);
                                durationView.setTextColor(0xFFFFFFFF);
                                iconView.setImageResource(R.drawable.ic_fork_knife);
                                iconView.setColorFilter(0xFFFFFFFF);
                                break;
                        }

                        timelineContainer.addView(taskView);
                    } catch (Exception e) {
                        Log.e(TAG, "Error rendering task: " + task.getTitle(), e);
                    }
                }
            }
        }

        if (timelineContainer.getChildCount() == 24 * 2) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No tasks scheduled for today.");
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(16, 32, 16, 32);
            emptyView.setTextSize(16);
            timelineContainer.addView(emptyView);
        }
    }

    private void showTaskOptions(String taskId, String taskTitle, Task task) {
        Toast.makeText(this, "Clicked task: " + taskTitle, Toast.LENGTH_SHORT).show();
    }

    private void deleteTask(String taskId) {
        Toast.makeText(this, "Deleting task: " + taskId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRefreshing = false;
        if (isNetworkAvailable()) {
            handler.post(refreshRunnable);
        } else {
            Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager =
                (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        // NetworkInfo is deprecated, but we need to use it for backward compatibility
        @SuppressWarnings("deprecation")
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}