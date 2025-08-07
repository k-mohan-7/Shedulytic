package com.example.shedulytic.adapter;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shedulytic.R;

public class HabitViewHolder extends RecyclerView.ViewHolder {
    public CardView habitCard;
    public TextView titleTextView;
    public TextView descriptionTextView;
    public TextView streakTextView;
    public ImageView habitIcon;
    public ProgressBar habitProgress;
    public CheckBox habitCheckbox;
    
    // Checkbox verification
    public View checkboxContainer;
    
    // Location verification
    public View locationContainer;
    public TextView locationInfoText;
    public TextView locationVerificationTitle;
    public TextView locationVerificationDescription;
    public Button verifyLocationButton;
    
    // Pomodoro verification
    public View pomodoroContainer;
    public TextView pomodoroInfoText;
    public TextView pomodoroTimerText;
    public ProgressBar pomodoroProgressBar;
    public Button startPomodoroButton;
    public Button pausePomodoroButton;
    public Button resumePomodoroButton;
    public Button stopPomodoroButton;
    
    public HabitViewHolder(@NonNull View itemView) {
        super(itemView);
        
        // Find views
        habitCard = itemView.findViewById(R.id.habit_card);
        titleTextView = itemView.findViewById(R.id.habit_title);
        descriptionTextView = itemView.findViewById(R.id.habit_description);
        streakTextView = itemView.findViewById(R.id.habit_streak);
        habitIcon = itemView.findViewById(R.id.habit_icon);
        habitProgress = itemView.findViewById(R.id.habit_progress);
        habitCheckbox = itemView.findViewById(R.id.habit_checkbox);
        
        // Checkbox verification
        checkboxContainer = itemView.findViewById(R.id.checkbox_verification_container);
        
        // Location verification
        locationContainer = itemView.findViewById(R.id.location_verification_container);
        locationInfoText = itemView.findViewById(R.id.location_info_text);
        locationVerificationTitle = itemView.findViewById(R.id.location_verification_title);
        locationVerificationDescription = itemView.findViewById(R.id.location_verification_description);
        verifyLocationButton = itemView.findViewById(R.id.verify_location_button);
        
        // Pomodoro verification
        pomodoroContainer = itemView.findViewById(R.id.pomodoro_verification_container);
        pomodoroInfoText = itemView.findViewById(R.id.pomodoro_info_text);
        pomodoroTimerText = itemView.findViewById(R.id.pomodoro_timer_text);
        pomodoroProgressBar = itemView.findViewById(R.id.pomodoro_progress_bar);
        startPomodoroButton = itemView.findViewById(R.id.start_pomodoro_button);
        pausePomodoroButton = itemView.findViewById(R.id.pause_pomodoro_button);
        resumePomodoroButton = itemView.findViewById(R.id.resume_pomodoro_button);
        stopPomodoroButton = itemView.findViewById(R.id.stop_pomodoro_button);
    }
} 