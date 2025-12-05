package com.shedulytic.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.shedulytic.R;
import com.shedulytic.app.activities.AddHabitActivity;
import com.shedulytic.app.adapters.HabitAdapter;
import com.shedulytic.app.models.Habit;
import com.shedulytic.app.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HabitFragment extends Fragment {
    private static final String TAG = "HabitFragment";
    
    private RecyclerView habitsRecyclerView;
    private TextView emptyHabitsText;
    private LinearLayout habitsPlannedContainer, habitsCompletedContainer;
    private TextView daysCountTextView;
    private Button addHabitButton;
    
    private List<Habit> habitsList = new ArrayList<>();
    private HabitAdapter habitAdapter;
    private RequestQueue requestQueue;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit, container, false);
        
        // Initialize components
        habitsRecyclerView = view.findViewById(R.id.habits_recycler_view);
        emptyHabitsText = view.findViewById(R.id.empty_habits_text);
        habitsPlannedContainer = view.findViewById(R.id.habits_planned_container);
        habitsCompletedContainer = view.findViewById(R.id.habits_completed_container);
        daysCountTextView = view.findViewById(R.id.textView_days_count);
        addHabitButton = view.findViewById(R.id.add_habit_button);
        
        // Set up RecyclerView
        habitsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        habitAdapter = new HabitAdapter(getContext(), habitsList);
        habitsRecyclerView.setAdapter(habitAdapter);
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(getContext());
        
        // Get user ID from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", -1);
        
        if (userId == -1) {
            // Handle case where user is not logged in
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }
        
        // Set up add habit button
        addHabitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddHabitActivity.class);
                startActivity(intent);
            }
        });
        
        // Load habits data
        loadHabits();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload data when returning to fragment
        loadHabits();
    }
    
    private void loadHabits() {
        String url = Constants.BASE_URL + "get_habits.php?user_id=" + userId;
        
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        habitsList.clear();
                        habitsPlannedContainer.removeAllViews();
                        habitsCompletedContainer.removeAllViews();
                        int streakCount = 0;
                        
                        try {
                            // Process response
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject habitObj = response.getJSONObject(i);
                                Habit habit = new Habit();
                                
                                habit.setId(habitObj.getInt("id"));
                                habit.setTitle(habitObj.getString("title"));
                                habit.setFrequency(habitObj.getString("frequency"));
                                habit.setTrustType(habitObj.getString("trust_type"));
                                habit.setReminderDate(habitObj.getString("reminder_date"));
                                habit.setReminderTime(habitObj.getString("reminder_time"));
                                
                                // Check for location data
                                if (!habitObj.isNull("map_lat") && !habitObj.isNull("map_lon")) {
                                    habit.setMapLat(habitObj.getDouble("map_lat"));
                                    habit.setMapLon(habitObj.getDouble("map_lon"));
                                }
                                
                                // Check for pomodoro duration
                                if (!habitObj.isNull("pomodoro_duration")) {
                                    habit.setPomodoroMinutes(habitObj.getInt("pomodoro_duration"));
                                }
                                
                                // Add to list
                                habitsList.add(habit);
                                
                                // Add to planned container
                                addHabitToPlannedList(habit);
                                
                                // Update streak count
                                streakCount++;
                            }
                            
                            // Update UI
                            habitAdapter.notifyDataSetChanged();
                            daysCountTextView.setText(String.valueOf(streakCount));
                            
                            // Show/hide empty state
                            if (habitsList.isEmpty()) {
                                habitsRecyclerView.setVisibility(View.GONE);
                                emptyHabitsText.setVisibility(View.VISIBLE);
                            } else {
                                habitsRecyclerView.setVisibility(View.VISIBLE);
                                emptyHabitsText.setVisibility(View.GONE);
                            }
                            
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error: " + error.toString());
                        Toast.makeText(getContext(), "Failed to load habits", Toast.LENGTH_SHORT).show();
                    }
                });
        
        requestQueue.add(request);
    }
    
    private void addHabitToPlannedList(Habit habit) {
        // Create view for habit in planned list
        TextView habitView = new TextView(getContext());
        habitView.setText(habit.getTitle());
        habitView.setTextColor(getResources().getColor(R.color.yellow));
        habitView.setTextSize(16);
        
        // Set drawable on start (triangle marker)
        habitView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_triangle, 0, 0, 0);
        habitView.setCompoundDrawablePadding(8);
        
        // Add margin bottom
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 6;
        habitView.setLayoutParams(params);
        
        // Add to container
        habitsPlannedContainer.addView(habitView);
    }
} 
