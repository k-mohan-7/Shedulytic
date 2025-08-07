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
        int layoutResource;
        
        // Select layout based on view type (verification method)
        switch (viewType) {
            case VIEW_TYPE_CHECKBOX:
                layoutResource = R.layout.checkbox_verification_layout;
                break;
            case VIEW_TYPE_LOCATION:
                layoutResource = R.layout.location_verification_layout;
                break;
            case VIEW_TYPE_POMODORO:
                layoutResource = R.layout.pomodoro_verification_layout;
                break;
            default:
                layoutResource = R.layout.checkbox_verification_layout; // Default fallback
                break;
        }
        
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResource, parent, false);
        return new HabitViewHolder(view, viewType);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        Context context = contextRef.get();
        HabitInteractionListener listener = listenerRef.get();
        
        if (context == null || listener == null) return;
        
        // Set basic habit info
        holder.titleTextView.setText(habit.getTitle());
        
        String description = habit.getDescription();
        holder.descriptionTextView.setVisibility(description != null && !description.isEmpty() ? View.VISIBLE : View.GONE);
        holder.descriptionTextView.setText(description);
        
        // Note: Streak and progress views are not available in the new simplified layouts
        // These features will be handled differently in the new design
        
        // Note: Container views don't exist in the new simplified layouts
        
        // Show the appropriate verification view based on type
        String verificationMethod = habit.getVerificationMethod();
        Log.d(TAG, "Habit " + habit.getTitle() + " has verification method: " + verificationMethod);
        
        // Check extra properties for trust_type but DO NOT override verification method
        // The verification method should already be correctly set by Habit.fromJson()
        JSONObject extraProps = habit.getExtraProperties();
        if (extraProps != null) {
            try {
                String trustType = ((org.json.JSONObject) extraProps).optString("trust_type", "");
                Log.d(TAG, "Trust type from extra properties: " + trustType);
                // Don't override the verification method here - it should already be correct
            } catch (Exception e) {
                Log.e(TAG, "Error reading trust_type: " + e.getMessage());
            }
        }
        
        switch (verificationMethod) {
            case Habit.VERIFICATION_CHECKBOX:
                setupCheckboxVerification(holder, habit);
                break;
            case Habit.VERIFICATION_LOCATION:
                setupLocationVerification(holder, habit);
                break;
            case Habit.VERIFICATION_POMODORO:
                setupPomodoroVerification(holder, habit);
                break;
            default:
                // Default to checkbox if verification method is unknown
                setupCheckboxVerification(holder, habit);
                break;
        }
        
        // Set card appearance
        int cardColor = context.getResources().getColor(
                habit.isCompleted() ? R.color.habit_dark_color : R.color.habit_color);
        if (holder.habitCard != null) {
            holder.habitCard.setCardBackgroundColor(cardColor);
        }
    }
    
    /**
     * Set up checkbox verification UI
     */
    private void setupCheckboxVerification(HabitViewHolder holder, Habit habit) {
        Log.d(TAG, "Setting up checkbox verification for habit: " + habit.getTitle());
        
        // Note: Container views don't exist in new simplified layouts
        
        // Set up the verify button for checkbox verification
        if (holder.verifyButton != null) {
            if (habit.isCompleted()) {
                holder.verifyButton.setText("Completed");
                holder.verifyButton.setEnabled(false);
            } else {
                holder.verifyButton.setText("Mark Complete");
                holder.verifyButton.setEnabled(true);
                
                holder.verifyButton.setOnClickListener(v -> {
                    HabitInteractionListener listener = listenerRef.get();
                    if (listener != null) {
                        listener.onHabitChecked(habit.getHabitId(), true);
                    }
                });
            }
        } else {
            Log.e(TAG, "Verify button is null");
        }
        
        // Set icon for checkbox verification
        if (holder.habitIcon != null) {
            holder.habitIcon.setImageResource(R.drawable.ic_check);
        }
        
        // Set card color for checkbox habits
        if (holder.habitCard != null) {
            Context context = holder.habitCard.getContext();
            int cardColor = context.getResources().getColor(habit.isCompleted() ? 
                    R.color.habit_dark_color : R.color.habit_color);
            holder.habitCard.setCardBackgroundColor(cardColor);
        }
        
        // Ensure the verification method is saved correctly
        try {
            JSONObject extraProps = habit.getExtraProperties();
            if (extraProps == null) {
                extraProps = new JSONObject();
                habit.setExtraProperties(extraProps);
            }
            
            if (!extraProps.has("trust_type") || !extraProps.getString("trust_type").equals("checkbox")) {
                extraProps.put("trust_type", "checkbox");
                habit.setExtraProperties(extraProps);
                Log.d(TAG, "Updated trust_type to checkbox in extra properties");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating trust_type: " + e.getMessage());
        }
    }
    
    private void setupLocationVerification(HabitViewHolder holder, Habit habit) {
        Log.d(TAG, "Setting up location verification for habit: " + habit.getTitle());
        
        // Note: Container views don't exist in new simplified layouts
        
        // Set up the verify button for location verification
        if (holder.verifyButton != null) {
            if (habit.isCompleted()) {
                holder.verifyButton.setText("Verified");
                holder.verifyButton.setEnabled(false);
            } else {
                holder.verifyButton.setText("Verify Location");
                holder.verifyButton.setEnabled(true);
                
                holder.verifyButton.setOnClickListener(v -> {
                    Context context = contextRef.get();
                    HabitInteractionListener listener = listenerRef.get();
                    
                    if (context != null && listener != null) {
                        Intent intent = new Intent(context, LocationVerificationActivity.class);
                        intent.putExtra(LocationVerificationActivity.EXTRA_HABIT_ID, habit.getHabitId());
                        intent.putExtra(LocationVerificationActivity.EXTRA_HABIT_TITLE, habit.getTitle());
                        intent.putExtra(LocationVerificationActivity.EXTRA_HABIT_DESCRIPTION, habit.getDescription());
                        intent.putExtra(LocationVerificationActivity.EXTRA_TARGET_LATITUDE, habit.getLatitude());
                        intent.putExtra(LocationVerificationActivity.EXTRA_TARGET_LONGITUDE, habit.getLongitude());

                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).startActivityForResult(intent, LOCATION_VERIFICATION_ACTIVITY_REQUEST_CODE);
                        } else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    }
                });
            }
        }
        
        if (holder.habitIcon != null) {
            holder.habitIcon.setImageResource(R.drawable.ic_location);
            holder.habitIcon.setScaleX(1.0f); 
            holder.habitIcon.setScaleY(1.0f);
        }
        
        // Ensure the verification method is saved correctly
        try {
            JSONObject extraProps = habit.getExtraProperties();
            if (extraProps == null) {
                extraProps = new JSONObject();
                habit.setExtraProperties(extraProps);
            }
            
            if (!extraProps.has("trust_type") || 
                (!extraProps.getString("trust_type").equals("location") && 
                 !extraProps.getString("trust_type").equals("map"))) {
                extraProps.put("trust_type", "map"); // Use backend enum value
                habit.setExtraProperties(extraProps);
                Log.d(TAG, "Updated trust_type to map in extra properties");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating trust_type: " + e.getMessage());
        }
        
        if (holder.habitCard != null) {
                holder.habitCard.setCardBackgroundColor(Color.parseColor("#F4DE00"));
            }
    }
    
    private void setupPomodoroVerification(HabitViewHolder holder, Habit habit) {
        Log.d(TAG, "Setting up pomodoro verification for habit: " + habit.getTitle());
        
        // Note: Container views don't exist in new simplified layouts
        
        String habitId = habit.getHabitId();
        
        // Note: Pomodoro-specific views like progress bars and timers don't exist in the new layout
        // The new layout only has basic title, description, icon, and verify button
        
        // Set up the verify button for pomodoro verification
        if (holder.verifyButton != null) {
            boolean isActive = activePomodoros.containsKey(habitId) && activePomodoros.get(habitId);
            
            if (habit.isCompleted()) {
                holder.verifyButton.setText("Completed");
                holder.verifyButton.setEnabled(false);
            } else if (isActive) {
                holder.verifyButton.setText("In Progress...");
                holder.verifyButton.setEnabled(false);
            } else {
                holder.verifyButton.setText("Start Pomodoro");
                holder.verifyButton.setEnabled(true);
                
                holder.verifyButton.setOnClickListener(v -> {
                    Context context = contextRef.get();
                    HabitInteractionListener listener = listenerRef.get();
                    
                    if (context != null && listener != null) {
                        listener.onPomodoroStartClicked(habit.getHabitId());
                        
                        Intent intent = new Intent(context, PomodoroActivity.class);
                        intent.putExtra(PomodoroActivity.EXTRA_HABIT_ID, habit.getHabitId());
                        intent.putExtra(PomodoroActivity.EXTRA_POMODORO_LENGTH_MIN, habit.getPomodoroLength());
                        
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).startActivityForResult(
                                    intent, POMODORO_ACTIVITY_REQUEST_CODE);
                        } else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    }
                });
            }
        }
        
        if (holder.habitIcon != null) {
            holder.habitIcon.setImageResource(R.drawable.ic_timer);
        }
        
        // Ensure the verification method is saved correctly
        try {
            JSONObject extraProps = habit.getExtraProperties();
            if (extraProps == null) {
                extraProps = new JSONObject();
                habit.setExtraProperties(extraProps);
            }
            
            if (!extraProps.has("trust_type") || !extraProps.getString("trust_type").equals("pomodoro")) {
                extraProps.put("trust_type", "pomodoro");
                habit.setExtraProperties(extraProps);
                Log.d(TAG, "Updated trust_type to pomodoro in extra properties");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating trust_type: " + e.getMessage());
        }
    }
    
    // Note: These methods were removed as the new simplified layouts 
    // don't have complex pomodoro UI elements like timers and progress bars
    
    @Override
    public int getItemCount() {
        return habitList.size();
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
        public TextView descriptionTextView;
        public ImageView habitIcon;
        public Button verifyButton;
        
        // Store the view type for this holder
        private int viewType;
        
        // Legacy views that don't exist in new layouts (keeping for compatibility)
        public View checkboxContainer;
        public View locationContainer;
        public View pomodoroContainer;
        public TextView locationVerificationTitle;
        public TextView locationVerificationDescription;
        public ImageView locationVerificationIcon;
        public Button verifyLocationButton;
        public TextView pomodoroInfoText;
        public TextView pomodoroTimerText;
        public ProgressBar pomodoroProgressBar;
        public Button startPomodoroButton;
        public Button pausePomodoroButton;
        public Button resumePomodoroButton;
        public Button stopPomodoroButton;
        
        public HabitViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            
            // Find common views that exist in all layouts
            habitCard = itemView.findViewById(R.id.habit_card);
            if (habitCard == null) {
                // CardView is the root in the new layouts
                habitCard = (CardView) itemView;
            }
            
            // Find views specific to each verification type
            switch (viewType) {
                case VIEW_TYPE_CHECKBOX:
                    setupCheckboxViews(itemView);
                    break;
                case VIEW_TYPE_LOCATION:
                    setupLocationViews(itemView);
                    break;
                case VIEW_TYPE_POMODORO:
                    setupPomodoroViews(itemView);
                    break;
            }
        }
        
        private void setupCheckboxViews(View itemView) {
            habitIcon = itemView.findViewById(R.id.checkbox_icon);
            titleTextView = itemView.findViewById(R.id.checkbox_verification_title);
            descriptionTextView = itemView.findViewById(R.id.checkbox_verification_description);
            verifyButton = itemView.findViewById(R.id.verify_checkbox_button);
        }
        
        private void setupLocationViews(View itemView) {
            habitIcon = itemView.findViewById(R.id.location_icon);
            titleTextView = itemView.findViewById(R.id.location_verification_title);
            descriptionTextView = itemView.findViewById(R.id.location_verification_description);
            verifyButton = itemView.findViewById(R.id.verify_location_button);
        }
        
        private void setupPomodoroViews(View itemView) {
            habitIcon = itemView.findViewById(R.id.pomodoro_icon);
            titleTextView = itemView.findViewById(R.id.pomodoro_verification_title);
            descriptionTextView = itemView.findViewById(R.id.pomodoro_verification_description);
            verifyButton = itemView.findViewById(R.id.verify_pomodoro_button);
        }
        
        public void bind(Habit habit) {
            // Set basic habit info
            titleTextView.setText(habit.getTitle());
            
            if (descriptionTextView != null) {
                descriptionTextView.setText(habit.getDescription());
                descriptionTextView.setVisibility(habit.getDescription() != null && !habit.getDescription().isEmpty() 
                        ? View.VISIBLE : View.GONE);
            }

            // Note: Streak and progress views are not available in the new simplified layouts
            // These features will be handled differently in the new design

            // Set icon based on verification method
            if (habitIcon != null) {
                int iconRes;
                switch (habit.getVerificationMethod()) {
                    case Habit.VERIFICATION_LOCATION:
                        iconRes = R.drawable.ic_location;
                        break;
                    case Habit.VERIFICATION_POMODORO:
                        iconRes = R.drawable.ic_timer;
                        break;
                    case Habit.VERIFICATION_CHECKBOX:
                    default:
                        iconRes = R.drawable.ic_check;
                        break;
                }
                habitIcon.setImageResource(iconRes);
            }

            // Set card color to habit yellow
            if (habitCard != null) {
                Context context = habitCard.getContext();
                int cardColor = context.getResources().getColor(habit.isCompleted() ? 
                        R.color.habit_dark_color : R.color.habit_color);
                habitCard.setCardBackgroundColor(cardColor);
            }

            // Verification method-specific UI is handled in onBindViewHolder
        }
    }
}