package com.simats.schedulytic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public class StreakFragment extends Fragment {
    private static final String TAG = "StreakFragment";
    private static final int DAYS_TO_DISPLAY = 7;

    private TextView[] weekDayTextViews;
    private TextView[] dayTextViews;
    private ImageView[] streakFireIcons;
    private TextView streakCountTextView;
    private static final String[] DAY_NAMES = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private RequestQueue requestQueue;
    private String userId;
    private final Map<String, Boolean> weekActivityData = new HashMap<>();
    private int streakCount = 0;
    private SimpleDateFormat dateFormat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weekDayTextViews = new TextView[DAYS_TO_DISPLAY];
        dayTextViews = new TextView[DAYS_TO_DISPLAY];
        streakFireIcons = new ImageView[DAYS_TO_DISPLAY];
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.streak, container, false);

        requestQueue = Volley.newRequestQueue(requireContext());

        SharedPreferences userPrefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = userPrefs.getString("user_id", "");
        streakCount = userPrefs.getInt("streak_count", 0);

        // Initialize views with proper spacing
        for (int i = 0; i < DAYS_TO_DISPLAY; i++) {
            int dayTextId = getResources().getIdentifier("dayTextView" + (i + 1), "id", requireContext().getPackageName());
            int weekDayTextId = getResources().getIdentifier("weekDayText" + (i + 1), "id", requireContext().getPackageName());
            int fireIconId = getResources().getIdentifier("streakFireIcon" + (i + 1), "id", requireContext().getPackageName());

            dayTextViews[i] = view.findViewById(dayTextId);
            weekDayTextViews[i] = view.findViewById(weekDayTextId);
            streakFireIcons[i] = view.findViewById(fireIconId);

            // Set initial visibility and style
            if (streakFireIcons[i] != null) {
                streakFireIcons[i].setImageResource(R.drawable.ic_fire);
                streakFireIcons[i].setAlpha(0.3f);
            }
        }

        // Initialize streak count TextView in HomeFragment
        TextView streakCountView = requireActivity().findViewById(R.id.textView_days_count);
        if (streakCountView != null) {
            streakCountView.setText(String.valueOf(streakCount));
        }

        loadStreakData();
        return view;
    }

    private void loadStreakData() {
        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty");
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -(DAYS_TO_DISPLAY - 1));
        String startDate = dateFormat.format(calendar.getTime());
        String endDate = dateFormat.format(Calendar.getInstance().getTime());

        String endpoint = VolleyNetworkManager.getInstance(requireContext()).getUserStreakUrl(userId, startDate, endDate);
        Log.d(TAG, "Loading streak data with endpoint: " + endpoint);

        // Use VolleyNetworkManager for networking
        VolleyNetworkManager.getInstance(requireContext()).makeGetRequest(endpoint, 
            new VolleyNetworkManager.JsonResponseListener() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        Log.d(TAG, "Streak data response received successfully");
                        processStreakData(response);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error processing streak data: " + e.getMessage(), e);
                        useDefaultStreakData();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error loading streak data: " + errorMessage);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading streak data", Toast.LENGTH_SHORT).show();
                    }
                    useDefaultStreakData();
                }
            });
    }

    private void processStreakData(JSONObject response) throws JSONException {
        // Extract streak data from the response
        JSONArray streakDataArray = null;
        
        if (response.has("streak_data")) {
            streakDataArray = response.getJSONArray("streak_data");
        } else if (response.has("data") && response.getJSONObject("data").has("streak_data")) {
            streakDataArray = response.getJSONObject("data").getJSONArray("streak_data");
        } else if (response.has("days")) {
            streakDataArray = response.getJSONArray("days");
        } else if (response.has("calendar")) {
            streakDataArray = response.getJSONArray("calendar");
        }
        
        if (streakDataArray == null || streakDataArray.length() == 0) {
            Log.e(TAG, "No streak data found in response");
            useDefaultStreakData();
            return;
        }
        
        // Process the streak data
        weekActivityData.clear();
        
        for (int i = 0; i < streakDataArray.length(); i++) {
            JSONObject dayData = streakDataArray.getJSONObject(i);
            String dateStr = dayData.getString("date");
            boolean hasActivity = dayData.getBoolean("has_activity");
            
            weekActivityData.put(dateStr, hasActivity);
        }
        
        // If we have streak count in the response, update it
        if (response.has("streak_count")) {
            streakCount = response.getInt("streak_count");
            // Update streak count display in HomeFragment
            TextView streakCountView = requireActivity().findViewById(R.id.textView_days_count);
            if (streakCountView != null) {
                streakCountView.setText(String.valueOf(streakCount));
            }
        }
        
        // Update UI with streak data
        updateUI();
    }

    private void updateCalendarWithStreakData(List<String> streakDays, List<String> missedDays) {
        // Clear existing data
        weekActivityData.clear();
        
        // Add streak days
        for (String day : streakDays) {
            weekActivityData.put(day, true);
        }
        
        // Add missed days
        for (String day : missedDays) {
            weekActivityData.put(day, false);
        }
        
        updateUI();
    }

    private void updateUI() {
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -(DAYS_TO_DISPLAY - 1));

        for (int i = 0; i < DAYS_TO_DISPLAY; i++) {
            String currentDate = dateFormat.format(calendar.getTime());
            boolean isToday = isSameDay(calendar, today);
            boolean hasActivity = weekActivityData.containsKey(currentDate) && weekActivityData.get(currentDate);

            // Update day number
            if (dayTextViews[i] != null) {
                dayTextViews[i].setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
                if (isToday) {
                    dayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                    dayTextViews[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 20); // Larger text for today
                } else {
                    dayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.grey));
                    dayTextViews[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Normal text size
                }
            }

            // Update weekday name
            if (weekDayTextViews[i] != null) {
                weekDayTextViews[i].setText(DAY_NAMES[calendar.get(Calendar.DAY_OF_WEEK) - 2]);
                if (isToday) {
                    weekDayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                    weekDayTextViews[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Slightly larger for today
                } else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    weekDayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.sunday_text));
                } else {
                    weekDayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.grey));
                }
            }

            // Update fire icon
            if (streakFireIcons[i] != null) {
                streakFireIcons[i].setVisibility(View.VISIBLE);
                if (hasActivity) {
                    streakFireIcons[i].setAlpha(1.0f);
                } else {
                    streakFireIcons[i].setAlpha(0.25f); // More transparent for inactive days
                }

                // Special handling for today
                if (isToday) {
                    if (!hasActivity) {
                        streakFireIcons[i].setAlpha(0.4f); // Slightly more visible for today if inactive
                    }
                    // Make today's icon slightly larger
                    streakFireIcons[i].getLayoutParams().width = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                    streakFireIcons[i].getLayoutParams().height = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                    streakFireIcons[i].requestLayout();
                }
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStreakData();
    }

    /**
     * Use default streak data when all network attempts fail
     */
    private void useDefaultStreakData() {
        try {
            // Create a default streak data response
            JSONObject defaultResponse = new JSONObject();
            defaultResponse.put("streak_count", streakCount);

            // Create empty streak data array
            JSONArray streakArray = new JSONArray();
            Calendar calendar = Calendar.getInstance();

            // Add today as active if we have a streak count > 0
            if (streakCount > 0) {
                JSONObject today = new JSONObject();
                today.put("date", dateFormat.format(calendar.getTime()));
                today.put("has_activity", true);
                streakArray.put(today);
            }

            defaultResponse.put("streak_data", streakArray);

            // Process this default data
            processStreakData(defaultResponse);

            // Log that we're using default data
            Log.d(TAG, "Using default streak data with streak count: " + streakCount);
        } catch (Exception e) {
            Log.e(TAG, "Error creating default streak data: " + e.getMessage(), e);
        }
    }
}