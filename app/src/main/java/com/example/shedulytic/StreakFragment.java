package com.example.shedulytic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StreakFragment extends Fragment {
    // Day TextViews and ImageViews
    private TextView[] weekDayTextViews = new TextView[7];
    private TextView[] dayTextViews = new TextView[7];
    private ImageView[] streakImageViews = new ImageView[7];
    private ImageView[] streakFireIcons = new ImageView[7];

    // Day names
    private static final String[] DAY_NAMES = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    // SharedPreferences for tracking streak
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "StreakPrefs";
    private static final String LAST_STREAK_DATE = "LastStreakDate";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.streak, container, false);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize views
        initializeViews(view);

        // Update the streak information
        updateStreakInfo();

        return view;
    }

    private void initializeViews(View view) {
        // Initialize all views dynamically
        for (int i = 0; i < 7; i++) {
            // Find day name TextViews
            int dayNameResId = getResources().getIdentifier("weekDayText" + (i + 1), "id", requireContext().getPackageName());
            weekDayTextViews[i] = view.findViewById(dayNameResId);

            // Find day number TextViews
            int dayNumberResId = getResources().getIdentifier("dayTextView" + (i + 1), "id", requireContext().getPackageName());
            dayTextViews[i] = view.findViewById(dayNumberResId);

            // Find streak ImageViews
            int streakImageResId = getResources().getIdentifier("streakImageView" + (i + 1), "id", requireContext().getPackageName());
            streakImageViews[i] = view.findViewById(streakImageResId);

            // Find streak fire icons
            int streakFireResId = getResources().getIdentifier("streakFireIcon" + (i + 1), "id", requireContext().getPackageName());
            streakFireIcons[i] = view.findViewById(streakFireResId);
        }
    }

    private void updateStreakInfo() {
        // Get current date
        Calendar calendar = Calendar.getInstance();

        // Find the start of the current week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        // Populate each day of the week
        for (int i = 0; i < 7; i++) {
            // Set day name
            weekDayTextViews[i].setText(DAY_NAMES[i]);

            // Set day number
            dayTextViews[i].setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));

            // Check if it's today
            boolean isToday = isSameDay(calendar, Calendar.getInstance());

            // Reset text and background colors
            resetDayStyle(i);

            // Special handling for today
            if (isToday) {
                // Check if streak should be activated
                boolean hasStreak = checkAndUpdateStreak(calendar);

                // Set today's circle to red background
                streakImageViews[i].setImageResource(R.drawable.circle_red_background);

                // Show/hide streak fire based on streak logic
                streakFireIcons[i].setVisibility(hasStreak ? View.VISIBLE : View.GONE);
            }

            // Special handling for Sunday
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                weekDayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.sunday_text));
                dayTextViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.sunday_text));
            }

            // Move to next day
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private boolean checkAndUpdateStreak(Calendar today) {
        // Get the last streak date
        long lastStreakDate = sharedPreferences.getLong(LAST_STREAK_DATE, 0);
        Calendar lastStreak = Calendar.getInstance();
        lastStreak.setTimeInMillis(lastStreakDate);

        // Check if today's streak is valid
        boolean isStreakValid = isConsecutiveDay(lastStreak, today);

        // Update last streak date if valid
        if (isStreakValid) {
            sharedPreferences.edit().putLong(LAST_STREAK_DATE, today.getTimeInMillis()).apply();
        }

        return isStreakValid;
    }

    private boolean isConsecutiveDay(Calendar lastStreak, Calendar today) {
        // Calculate the difference between days
        long diffInMillis = today.getTimeInMillis() - lastStreak.getTimeInMillis();
        long daysDiff = diffInMillis / (24 * 60 * 60 * 1000);

        // Streak is valid if it's the next consecutive day
        return daysDiff == 1;
    }

    private void resetDayStyle(int dayIndex) {
        // Reset text color
        weekDayTextViews[dayIndex].setTextColor(ContextCompat.getColor(requireContext(), R.color.grey));
        dayTextViews[dayIndex].setTextColor(ContextCompat.getColor(requireContext(), R.color.grey));

        // Reset background
        streakImageViews[dayIndex].setImageResource(R.drawable.circle_filled);

        // Hide fire icon
        streakFireIcons[dayIndex].setVisibility(View.GONE);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
}