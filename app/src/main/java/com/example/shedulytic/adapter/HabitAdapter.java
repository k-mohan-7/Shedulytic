package com.example.shedulytic.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.cardview.widget.CardView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shedulytic.R;
import com.example.shedulytic.model.Habit;
import com.example.shedulytic.PomodoroActivity;
import com.example.shedulytic.LocationVerificationActivity;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
// Potentially your DAO import: import com.example.shedulytic.db.UserActivityLogDao; (or similar)

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {
    private static final String TAG = "HabitAdapter";
    
    // View type constants for different verification methods
    private static final int VIEW_TYPE_CHECKBOX = 0;
    private static final int VIEW_TYPE_LOCATION = 1;
    private static final int VIEW_TYPE_POMODORO = 2;
    
    private List<Habit> habitList;
    private WeakReference<HabitInteractionListener> listenerRef;
    private WeakReference<Context> contextRef;
    private Map<String, Boolean> activePomodoros = new HashMap<>();
    private Map<String, Long> pomodoroTimes = new HashMap<>();
    private Map<String, Integer> pomodoroProgress = new HashMap<>();
    private Map<String, Integer> pomodoroTotals = new HashMap<>();
    private Map<String, Float> progressMap = new HashMap<>();
    
    private static final int POMODORO_ACTIVITY_REQUEST_CODE = 1001;
    private static final int LOCATION_VERIFICATION_ACTIVITY_REQUEST_CODE = 1002;
    
    /**
     * Interface for habit interactions
     */
    public interface HabitInteractionListener {
        void onHabitChecked(String habitId, boolean isChecked);
        void onLocationVerifyClicked(String habitId);
        void onPomodoroStartClicked(String habitId);
        void onPomodoroPauseClicked(String habitId);
        void onPomodoroResumeClicked(String habitId);
        void onPomodoroStopClicked(String habitId);
    }
    
    public HabitAdapter(List<Habit> habitList, HabitInteractionListener listener, Context context) {
        this.habitList = habitList != null ? habitList : new ArrayList<>();
        this.listenerRef = new WeakReference<>(listener);
        this.contextRef = new WeakReference<>(context);
    }
    
    // Constructor for backward compatibility
    public HabitAdapter(List<Habit> habitList, HabitInteractionListener listener) {
        this(habitList, listener, listener instanceof Context ? (Context) listener : null);
    }
    
    @Override
    public int getItemViewType(int position) {
        Habit habit = habitList.get(position);
        String verificationMethod = habit.getVerificationMethod();
        
        // Check extra properties for trust_type if verification method is not set
        JSONObject extraProps = habit.getExtraProperties();
        if (extraProps != null) {
            try {
                String trustType = extraProps.optString("trust_type", "");
                if (!trustType.isEmpty()) {
                    if (trustType.equals("checkbox")) {
                        verificationMethod = Habit.VERIFICATION_CHECKBOX;
                    } else if (trustType.equals("location") || trustType.equals("map")) {
                        verificationMethod = Habit.VERIFICATION_LOCATION;
                    } else if (trustType.equals("pomodoro")) {
                        verificationMethod = Habit.VERIFICATION_POMODORO;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading trust_type: " + e.getMessage());
            }
        }
        
        // Return appropriate view type
        switch (verificationMethod) {
            case Habit.VERIFICATION_LOCATION:
                return VIEW_TYPE_LOCATION;
            case Habit.VERIFICATION_POMODORO:
                return VIEW_TYPE_POMODORO;
            case Habit.VERIFICATION_CHECKBOX:
            default:
                return VIEW_TYPE_CHECKBOX;
        }
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use separate layouts for each verification type with circular progress
        int layoutRes;
        switch (viewType) {
            case VIEW_TYPE_LOCATION:
                layoutRes = R.layout.item_habit_location;
                break;
            case VIEW_TYPE_POMODORO:
                layoutRes = R.layout.item_habit_pomodoro;
                break;
            case VIEW_TYPE_CHECKBOX:
            default:
                layoutRes = R.layout.item_habit_check;
                break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new HabitViewHolder(view, viewType);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        Context context = contextRef.get();
        HabitInteractionListener listener = listenerRef.get();
        
        if (context == null) return;
        
        // Set habit title
        if (holder.titleTextView != null) {
            holder.titleTextView.setText(habit.getTitle());
        }
        
        // Calculate and set progress percentage based on daily completion rate
        int progressPercent = calculateHabitProgress(habit);
        
        if (holder.habitProgress != null) {
            holder.habitProgress.setProgress(progressPercent);
        }
        
        if (holder.percentageTextView != null) {
            holder.percentageTextView.setText(progressPercent + "%");
        }
        
        // Set streak info
        if (holder.streakTextView != null) {
            int streak = habit.getCurrentStreak();
            holder.streakTextView.setText(streak + " day streak");
        }
        
        // Bind type-specific views
        int viewType = holder.viewType;
        switch (viewType) {
            case VIEW_TYPE_CHECKBOX:
                bindCheckboxHabit(holder, habit, listener);
                break;
            case VIEW_TYPE_POMODORO:
                bindPomodoroHabit(holder, habit, listener, context);
                break;
            case VIEW_TYPE_LOCATION:
                bindLocationHabit(holder, habit, listener, context);
                break;
        }
        
        // Apply completed state styling
        if (holder.habitCard != null) {
            holder.habitCard.setAlpha(habit.isCompleted() ? 0.7f : 1.0f);
        }
        
        // Update progress from stored progress map if available
        if (progressMap.containsKey(habit.getHabitId())) {
            float storedProgress = progressMap.get(habit.getHabitId());
            int storedPercent = Math.round(storedProgress * 100);
            if (holder.habitProgress != null) {
                holder.habitProgress.setProgress(storedPercent);
            }
            if (holder.percentageTextView != null) {
                holder.percentageTextView.setText(storedPercent + "%");
            }
        }
    }
    
    private void bindCheckboxHabit(HabitViewHolder holder, Habit habit, HabitInteractionListener listener) {
        if (holder.btnMarkComplete != null) {
            if (habit.isCompleted()) {
                holder.btnMarkComplete.setText("Done ✓");
                holder.btnMarkComplete.setEnabled(false);
                holder.btnMarkComplete.setAlpha(0.6f);
            } else {
                holder.btnMarkComplete.setText("Done");
                holder.btnMarkComplete.setEnabled(true);
                holder.btnMarkComplete.setAlpha(1.0f);
            }
            
            holder.btnMarkComplete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHabitChecked(habit.getHabitId(), true);
                }
            });
        }
    }
    
    private void bindPomodoroHabit(HabitViewHolder holder, Habit habit, HabitInteractionListener listener, Context context) {
        String habitId = habit.getHabitId();
        boolean isActive = activePomodoros.containsKey(habitId) && activePomodoros.get(habitId);
        
        if (holder.pomodoroTime != null) {
            if (pomodoroTimes.containsKey(habitId)) {
                long timeRemaining = pomodoroTimes.get(habitId);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) % 60;
                holder.pomodoroTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            } else {
                holder.pomodoroTime.setText("25:00");
            }
        }
        
        if (holder.pomodoroStatus != null) {
            if (habit.isCompleted()) {
                holder.pomodoroStatus.setText("Session completed today! ✓");
            } else if (isActive) {
                int completed = pomodoroProgress.getOrDefault(habitId, 0);
                int total = pomodoroTotals.getOrDefault(habitId, 1);
                holder.pomodoroStatus.setText("Focus session " + completed + "/" + total);
            } else {
                holder.pomodoroStatus.setText("Tap Start to begin focus session");
            }
        }
        
        if (holder.btnStartPomodoro != null) {
            if (habit.isCompleted()) {
                holder.btnStartPomodoro.setText("Done");
                holder.btnStartPomodoro.setEnabled(false);
                holder.btnStartPomodoro.setAlpha(0.6f);
            } else if (isActive) {
                holder.btnStartPomodoro.setText("Pause");
                holder.btnStartPomodoro.setEnabled(true);
                holder.btnStartPomodoro.setAlpha(1.0f);
            } else {
                holder.btnStartPomodoro.setText("Start");
                holder.btnStartPomodoro.setEnabled(true);
                holder.btnStartPomodoro.setAlpha(1.0f);
            }
            
            holder.btnStartPomodoro.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPomodoroStartClicked(habitId);
                }
            });
        }
    }
    
    private void bindLocationHabit(HabitViewHolder holder, Habit habit, HabitInteractionListener listener, Context context) {
        // Get location name from habit extra properties
        String locName = "Location";
        JSONObject extraProps = habit.getExtraProperties();
        if (extraProps != null) {
            locName = extraProps.optString("location_name", "Location");
            if (locName.isEmpty()) {
                locName = extraProps.optString("address", "Location");
            }
        }
        
        if (holder.locationName != null) {
            holder.locationName.setText(locName);
        }
        
        if (holder.locationDistance != null) {
            if (habit.isCompleted()) {
                holder.locationDistance.setText("Location verified today! ✓");
            } else {
                holder.locationDistance.setText("Tap Verify when you arrive");
            }
        }
        
        if (holder.btnVerifyLocation != null) {
            if (habit.isCompleted()) {
                holder.btnVerifyLocation.setText("Verified");
                holder.btnVerifyLocation.setEnabled(false);
                holder.btnVerifyLocation.setAlpha(0.6f);
            } else {
                holder.btnVerifyLocation.setText("Verify");
                holder.btnVerifyLocation.setEnabled(true);
                holder.btnVerifyLocation.setAlpha(1.0f);
            }
            
            holder.btnVerifyLocation.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLocationVerifyClicked(habit.getHabitId());
                }
            });
        }
    }
    
    /**
     * Calculate habit progress percentage based on completion rate
     * Uses total completions vs expected completions based on frequency
     */
    private int calculateHabitProgress(Habit habit) {
        // If completed today, return 100%
        if (habit.isCompleted()) {
            return 100;
        }
        
        // Calculate based on streak and total completions
        int totalCompletions = habit.getTotalCompletions();
        int currentStreak = habit.getCurrentStreak();
        
        // Simple progress calculation:
        // - If never completed, 0%
        // - If has completions but not today, calculate based on recent streak
        if (totalCompletions == 0) {
            return 0;
        }
        
        // Use streak as a rough indicator (max 7 days for 100%)
        // This gives a visual representation of consistency
        int progress = Math.min(100, (currentStreak * 100) / 7);
        
        // Minimum 10% if habit exists but not completed today
        return Math.max(10, progress);
    }
    
    /**
     * Handle habit click based on verification method
     */
    private void handleHabitClick(Habit habit, HabitInteractionListener listener, Context context) {
        String verificationMethod = habit.getVerificationMethod();
        
        switch (verificationMethod) {
            case Habit.VERIFICATION_LOCATION:
                listener.onLocationVerifyClicked(habit.getHabitId());
                break;
            case Habit.VERIFICATION_POMODORO:
                listener.onPomodoroStartClicked(habit.getHabitId());
                break;
            case Habit.VERIFICATION_CHECKBOX:
            default:
                // Toggle checkbox state
                listener.onHabitChecked(habit.getHabitId(), !habit.isCompleted());
                break;
        }
    }
    
    @Override
    public int getItemCount() {
        return habitList != null ? habitList.size() : 0;
    }
    
    /**
     * Update the habit list
     */
    public void updateHabits(List<Habit> newHabits) {
        this.habitList = newHabits != null ? newHabits : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void updatePomodoroStatus(String habitId, long timeRemaining, int completedCount, int totalCount) {
        pomodoroTimes.put(habitId, timeRemaining);
        pomodoroProgress.put(habitId, completedCount);
        pomodoroTotals.put(habitId, totalCount);
        
        // Find the position of the habit in the list
        for (int i = 0; i < habitList.size(); i++) {
            if (habitList.get(i).getHabitId().equals(habitId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    public void setPomodoroActive(String habitId, boolean active) {
        activePomodoros.put(habitId, active);
        
        if (!active) {
            pomodoroTimes.remove(habitId);
            pomodoroProgress.remove(habitId);
            pomodoroTotals.remove(habitId);
        }
        
        // Find the position of the habit in the list
        for (int i = 0; i < habitList.size(); i++) {
            if (habitList.get(i).getHabitId().equals(habitId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    public void updateProgress(Map<String, Float> habitProgress) {
        // Update progress for each habit
        if (habitProgress != null) {
            progressMap.clear();
            progressMap.putAll(habitProgress);
            
            for (int i = 0; i < habitList.size(); i++) {
                String habitId = habitList.get(i).getHabitId();
                if (habitProgress.containsKey(habitId)) {
                    notifyItemChanged(i);
                }
            }
        }
    }
    
    public class HabitViewHolder extends RecyclerView.ViewHolder {
        public CardView habitCard;
        public TextView titleTextView;
        public ProgressBar habitProgress;
        public TextView percentageTextView;
        public TextView streakTextView;
        public ImageView habitIcon;
        public CheckBox habitCheckbox;
        
        // Check type specific views
        public Button btnMarkComplete;
        
        // Pomodoro type specific views
        public TextView pomodoroTime;
        public TextView pomodoroStatus;
        public Button btnStartPomodoro;
        
        // Location type specific views
        public TextView locationName;
        public TextView locationDistance;
        public Button btnVerifyLocation;
        
        // Store the view type for this holder
        private int viewType;
        
        public HabitViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            
            // Common views
            habitCard = itemView.findViewById(R.id.habit_card);
            if (habitCard == null && itemView instanceof CardView) {
                habitCard = (CardView) itemView;
            }
            
            titleTextView = itemView.findViewById(R.id.habit_title);
            habitProgress = itemView.findViewById(R.id.habit_progress);
            percentageTextView = itemView.findViewById(R.id.habit_percentage);
            streakTextView = itemView.findViewById(R.id.habit_streak);
            habitIcon = itemView.findViewById(R.id.habit_icon);
            habitCheckbox = itemView.findViewById(R.id.habit_checkbox);
            
            // Type-specific views
            switch (viewType) {
                case VIEW_TYPE_CHECKBOX:
                    btnMarkComplete = itemView.findViewById(R.id.btn_mark_complete);
                    break;
                case VIEW_TYPE_POMODORO:
                    pomodoroTime = itemView.findViewById(R.id.pomodoro_time);
                    pomodoroStatus = itemView.findViewById(R.id.pomodoro_status);
                    btnStartPomodoro = itemView.findViewById(R.id.btn_start_pomodoro);
                    break;
                case VIEW_TYPE_LOCATION:
                    locationName = itemView.findViewById(R.id.location_name);
                    locationDistance = itemView.findViewById(R.id.location_distance);
                    btnVerifyLocation = itemView.findViewById(R.id.btn_verify_location);
                    break;
            }
        }
    }
}