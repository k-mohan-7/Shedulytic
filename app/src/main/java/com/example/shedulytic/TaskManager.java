package com.example.shedulytic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.shedulytic.ui.login.IpV4Connection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private static final String TAG = "TaskManager";
    private Context context;
    private String userId;
    private TaskListener taskListener;

    public interface TaskListener {
        void onTasksLoaded(List<Task> tasks);
        void onTaskAdded(Task task);
        void onTaskUpdated(Task task);
        void onTaskDeleted(String taskId);
        void onError(String message);
    }

    public TaskManager(Context context, TaskListener listener) {
        this.context = context;
        this.taskListener = listener;
        
        // Get user ID from SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("user_id", "");
    }

    public void loadTasks() {
        new FetchTasksTask().execute();
    }

    public void addTask(String title, String description, String startTime, String endTime, 
                       String dueDate, String taskType, String repeatFrequency) {
        new AddTaskTask().execute(title, description, startTime, endTime, dueDate, taskType, repeatFrequency);
    }

    public void updateTaskStatus(String taskId, String status) {
        new UpdateTaskTask().execute(taskId, status);
    }

    public void updateTaskTime(String taskId, String startTime, String endTime) {
        new UpdateTaskTimeTask().execute(taskId, startTime, endTime);
    }

    public void deleteTask(String taskId) {
        new DeleteTaskTask().execute(taskId);
    }

    private class FetchTasksTask extends AsyncTask<Void, Void, List<Task>> {
        private String errorMessage;

        @Override
        protected List<Task> doInBackground(Void... voids) {
            List<Task> tasks = new ArrayList<>();
            HttpURLConnection connection = null;
            try {
                URL url = new URL(IpV4Connection.getBaseUrl() + "view_tasks.php?user_id=" + userId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(connection.getInputStream());
                    JSONObject jsonResponse = new JSONObject(response);

                    if (jsonResponse.getString("status").equals("success")) {
                        if (jsonResponse.has("tasks")) {
                            JSONArray tasksArray = jsonResponse.getJSONArray("tasks");
                            for (int i = 0; i < tasksArray.length(); i++) {
                                JSONObject taskObj = tasksArray.getJSONObject(i);
                                Task task = new Task(
                                        taskObj.getString("task_id"),
                                        taskObj.getString("user_id"),
                                        taskObj.getString("task_type"),
                                        taskObj.getString("title"),
                                        taskObj.optString("description", ""),
                                        taskObj.optString("start_time", ""),
                                        taskObj.optString("end_time", ""),
                                        taskObj.optString("due_date", ""),
                                        taskObj.getString("status"),
                                        taskObj.optString("repeat_frequency", "none"),
                                        taskObj.optString("priority", "medium")
                                );
                                tasks.add(task);
                            }
                        }
                    } else {
                        errorMessage = jsonResponse.optString("message", "Unknown error");
                    }
                } else {
                    errorMessage = "Server returned code: " + responseCode;
                }
            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return tasks;
        }

        @Override
        protected void onPostExecute(List<Task> tasks) {
            if (errorMessage != null) {
                taskListener.onError(errorMessage);
            } else {
                taskListener.onTasksLoaded(tasks);
            }
        }
    }

    private class AddTaskTask extends AsyncTask<String, Void, Task> {
        private String errorMessage;

        @Override
        protected Task doInBackground(String... params) {
            String title = params[0];
            String description = params[1];
            String startTime = params[2];
            String endTime = params[3];
            String dueDate = params[4];
            String taskType = params[5];
            String repeatFrequency = params[6];

            HttpURLConnection connection = null;
            try {
                URL url = new URL(IpV4Connection.getBaseUrl() + "add_task.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject postData = new JSONObject();
                postData.put("user_id", userId);
                postData.put("title", title);
                postData.put("description", description);
                postData.put("start_time", startTime);
                postData.put("end_time", endTime);
                postData.put("due_date", dueDate);
                postData.put("task_type", taskType);
                postData.put("repeat_frequency", repeatFrequency);
                postData.put("status", "pending");
                postData.put("priority", "medium");

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(connection.getInputStream());
                    JSONObject jsonResponse = new JSONObject(response);

                    if (jsonResponse.getString("status").equals("success")) {
                        JSONObject taskData = jsonResponse.getJSONObject("task");
                        return new Task(
                                taskData.getString("task_id"),
                                userId,
                                taskType,
                                title,
                                description,
                                startTime,
                                endTime,
                                dueDate,
                                "pending",
                                repeatFrequency,
                                "medium"
                        );
                    } else {
                        errorMessage = jsonResponse.optString("message", "Unknown error");
                    }
                } else {
                    errorMessage = "Server returned code: " + responseCode;
                }
            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Task task) {
            if (task != null) {
                taskListener.onTaskAdded(task);
            } else {
                taskListener.onError(errorMessage);
            }
        }
    }

    private class UpdateTaskTask extends AsyncTask<String, Void, Task> {
        private String errorMessage;

        @Override
        protected Task doInBackground(String... params) {
            String taskId = params[0];
            String status = params[1];

            HttpURLConnection connection = null;
            try {
                URL url = new URL(IpV4Connection.getBaseUrl() + "update_task.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject postData = new JSONObject();
                postData.put("task_id", taskId);
                postData.put("status", status);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(connection.getInputStream());
                    JSONObject jsonResponse = new JSONObject(response);

                    if (jsonResponse.getString("status").equals("success")) {
                        JSONObject taskData = jsonResponse.getJSONObject("task");
                        return new Task(
                                taskData.getString("task_id"),
                                taskData.getString("user_id"),
                                taskData.getString("task_type"),
                                taskData.getString("title"),
                                taskData.optString("description", ""),
                                taskData.optString("start_time", ""),
                                taskData.optString("end_time", ""),
                                taskData.optString("due_date", ""),
                                status,
                                taskData.optString("repeat_frequency", "none"),
                                taskData.optString("priority", "medium")
                        );
                    } else {
                        errorMessage = jsonResponse.optString("message", "Unknown error");
                    }
                } else {
                    errorMessage = "Server returned code: " + responseCode;
                }
            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Task task) {
            if (task != null) {
                taskListener.onTaskUpdated(task);
            } else {
                taskListener.onError(errorMessage);
            }
        }
    }

    private class UpdateTaskTimeTask extends AsyncTask<String, Void, Task> {
        private String errorMessage;

        @Override
        protected Task doInBackground(String... params) {
            String taskId = params[0];
            String startTime = params[1];
            String endTime = params[2];

            HttpURLConnection connection = null;
            try {
                URL url = new URL(IpV4Connection.getBaseUrl() + "update_task.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject postData = new JSONObject();
                postData.put("task_id", taskId);
                postData.put("start_time", startTime);
                postData.put("end_time", endTime);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(connection.getInputStream());
                    JSONObject jsonResponse = new JSONObject(response);

                    if (jsonResponse.getString("status").equals("success")) {
                        JSONObject taskData = jsonResponse.getJSONObject("task");
                        return new Task(
                                taskData.getString("task_id"),
                                taskData.getString("user_id"),
                                taskData.getString("task_type"),
                                taskData.getString("title"),
                                taskData.optString("description", ""),
                                startTime,
                                endTime,
                                taskData.optString("due_date", ""),
                                taskData.getString("status"),
                                taskData.optString("repeat_frequency", "none"),
                                taskData.optString("priority", "medium")
                        );
                    } else {
                        errorMessage = jsonResponse.optString("message", "Unknown error");
                    }
                } else {
                    errorMessage = "Server returned code: " + responseCode;
                }
            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Task task) {
            if (task != null) {
                taskListener.onTaskUpdated(task);
            } else {
                taskListener.onError(errorMessage);
            }
        }
    }

    private class DeleteTaskTask extends AsyncTask<String, Void, String> {
        private String errorMessage;

        @Override
        protected String doInBackground(String... params) {
            String taskId = params[0];

            HttpURLConnection connection = null;
            try {
                URL url = new URL(IpV4Connection.getBaseUrl() + "delete_task.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject postData = new JSONObject();
                postData.put("task_id", taskId);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(connection.getInputStream());
                    JSONObject jsonResponse = new JSONObject(response);

                    if (jsonResponse.getString("status").equals("success")) {
                        return taskId;
                    } else {
                        errorMessage = jsonResponse.optString("message", "Unknown error");
                    }
                } else {
                    errorMessage = "Server returned code: " + responseCode;
                }
            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String taskId) {
            if (taskId != null) {
                taskListener.onTaskDeleted(taskId);
            } else {
                taskListener.onError(errorMessage);
            }
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }
}