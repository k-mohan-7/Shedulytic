package com.schedlytic.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shedulytic.R;
import com.schedlytic.app.models.Habit;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {
    private Context context;
    private List<Habit> habitsList;

    public HabitAdapter(Context context, List<Habit> habitsList) {
        this.context = context;
        this.habitsList = habitsList;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitsList.get(position);
        
        // Set habit title and description
        holder.habitTitle.setText(habit.getTitle());
        holder.habitDescription.setText("Frequency: " + habit.getFrequency());
        
        // Set streak information
        holder.habitStreak.setText("Streak: " + habit.getStreak() + " days");
        
        // Set progress
        holder.habitProgress.setProgress(habit.getStreak() * 10); // Simple progress calculation
        
        // Set checkbox status
        holder.habitCheckbox.setChecked(habit.isCompleted());
        
        // Show/hide verification sections based on trust type
        if ("location".equals(habit.getTrustType())) {
            holder.locationVerificationSection.setVisibility(View.VISIBLE);
            holder.pomodoroVerificationSection.setVisibility(View.GONE);
        } else if ("pomodoro".equals(habit.getTrustType())) {
            holder.pomodoroVerificationSection.setVisibility(View.VISIBLE);
            holder.locationVerificationSection.setVisibility(View.GONE);
            
            // Set pomodoro count
            if (habit.getPomodoroMinutes() > 0) {
                holder.pomodoroProgressBar.setProgress(habit.getPomodoroMinutes() * 10);
            }
        } else {
            // Default is checkbox
            holder.locationVerificationSection.setVisibility(View.GONE);
            holder.pomodoroVerificationSection.setVisibility(View.GONE);
        }
        
        // Handle checkbox changes
        holder.habitCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = holder.habitCheckbox.isChecked();
                habit.setCompleted(isChecked);
                // TODO: Update completion status on server
            }
        });
        
        // Handle location verification button
        holder.verifyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement location verification
            }
        });
        
        // Handle pomodoro buttons
        holder.startPomodoroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show timer and control buttons
                holder.pomodoroTimerText.setVisibility(View.VISIBLE);
                holder.startPomodoroButton.setVisibility(View.GONE);
                holder.pausePomodoroButton.setVisibility(View.VISIBLE);
                holder.stopPomodoroButton.setVisibility(View.VISIBLE);
                
                // TODO: Start timer
            }
        });
    }

    @Override
    public int getItemCount() {
        return habitsList.size();
    }

    public class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView habitTitle, habitDescription, habitStreak;
        TextView pomodoroTimerText;
        CheckBox habitCheckbox;
        ProgressBar habitProgress, pomodoroProgressBar;
        LinearLayout locationVerificationSection, pomodoroVerificationSection;
        Button verifyLocationButton, startPomodoroButton, pausePomodoroButton, resumePomodoroButton, stopPomodoroButton;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            
            habitTitle = itemView.findViewById(R.id.habit_title);
            habitDescription = itemView.findViewById(R.id.habit_description);
            habitStreak = itemView.findViewById(R.id.habit_streak);
            habitCheckbox = itemView.findViewById(R.id.habit_checkbox);
            habitProgress = itemView.findViewById(R.id.habit_progress);
            
            locationVerificationSection = itemView.findViewById(R.id.location_verification_container);
            pomodoroVerificationSection = itemView.findViewById(R.id.pomodoro_verification_container);
            
            verifyLocationButton = itemView.findViewById(R.id.verify_location_button);
            
            pomodoroTimerText = itemView.findViewById(R.id.pomodoro_timer_text);
            pomodoroProgressBar = itemView.findViewById(R.id.pomodoro_progress_bar);
            startPomodoroButton = itemView.findViewById(R.id.start_pomodoro_button);
            pausePomodoroButton = itemView.findViewById(R.id.pause_pomodoro_button);
            resumePomodoroButton = itemView.findViewById(R.id.resume_pomodoro_button);
            stopPomodoroButton = itemView.findViewById(R.id.stop_pomodoro_button);
        }
    }
} 