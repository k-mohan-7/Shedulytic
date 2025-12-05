package com.simats.schedulytic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Custom calendar view that supports showing fire icons for streak days
 */
public class CalendarViewWithFireIcons extends GridLayout implements HabitManager.CalendarViewWithFireIcons {
    private static final String TAG = "CalendarViewWithFire";
    private final Set<String> markedDates = new HashSet<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private Drawable fireIcon;

    public CalendarViewWithFireIcons(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CalendarViewWithFireIcons(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CalendarViewWithFireIcons(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Always use the ic_fire.xml as specified by user
        fireIcon = ContextCompat.getDrawable(context, R.drawable.ic_fire);
        if (fireIcon == null) {
            // Only fallback if ic_fire is null
            fireIcon = ContextCompat.getDrawable(context, R.drawable.ic_streak_fire);
            Log.w(TAG, "Using fallback ic_streak_fire because ic_fire was not found");
        } else {
            Log.d(TAG, "Successfully loaded ic_fire drawable");
        }
        
        // Set fire icon size
        if (fireIcon != null) {
            // Make the fire icon slightly larger for better visibility
            fireIcon.setBounds(0, 0, 32, 32);
        } else {
            Log.e(TAG, "Failed to load any fire icon drawable");
        }
    }

    @Override
    public void markDateWithFireIcon(String dateStr) {
        try {
            // Validate the date format
            dateFormat.parse(dateStr);
            
            // Add to marked dates
            markedDates.add(dateStr);
            
            // Request redraw
            post(() -> {
                updateCalendarDisplay();
                invalidate();
            });
            
            Log.d(TAG, "Marked date with fire icon: " + dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Invalid date format: " + dateStr, e);
        }
    }
    
    /**
     * Clear all marked dates
     */
    public void clearMarkedDates() {
        markedDates.clear();
        updateCalendarDisplay();
        invalidate();
    }
    
    /**
     * Update the calendar display to show fire icons
     */
    private void updateCalendarDisplay() {
        // Find all day cells in the calendar
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof TextView) {
                TextView dayView = (TextView) child;
                
                // Get the date from the tag if available
                Object tag = dayView.getTag();
                if (tag instanceof String) {
                    String dateStr = (String) tag;
                    
                    // Check if this date is marked
                    if (markedDates.contains(dateStr)) {
                        // Set compound drawable to show fire icon
                        dayView.setCompoundDrawables(null, null, fireIcon, null);
                    } else {
                        // Clear compound drawable
                        dayView.setCompoundDrawables(null, null, null, null);
                    }
                }
            }
        }
    }
    
    /**
     * Convert day number to date string based on current month/year
     * @param dayOfMonth The day to convert
     * @return Date string in yyyy-MM-dd format
     */
    public String dayToDateString(int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return dateFormat.format(calendar.getTime());
    }
    
    /**
     * Check if a specific date is marked with a fire icon
     * @param dateStr Date string in yyyy-MM-dd format
     * @return true if the date is marked
     */
    public boolean isDateMarked(String dateStr) {
        return markedDates.contains(dateStr);
    }
} 