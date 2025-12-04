package com.example.shedulytic;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.shedulytic.service.HabitManagerService;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PomodoroActivity extends AppCompatActivity {

    private static final String TAG = "PomodoroActivity";    private TextView timerTextViewFullscreen;
    private Button pauseResumeButtonFullscreen;
    private Button stopButtonFullscreen;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private long initialDurationMillis;
    private boolean timerRunning;
    private boolean timerPaused;

    private String habitId;
    private HabitManagerService habitManagerService;

    public static final String EXTRA_HABIT_ID = "com.example.shedulytic.EXTRA_HABIT_ID";
    public static final String EXTRA_POMODORO_LENGTH_MIN = "com.example.shedulytic.EXTRA_POMODORO_LENGTH_MIN";    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pomodoro_timer_layout);

        // Initialize HabitManagerService
        habitManagerService = HabitManagerService.getInstance(this);

        timerTextViewFullscreen = findViewById(R.id.timer_text);
        pauseResumeButtonFullscreen = findViewById(R.id.pause_button);
        stopButtonFullscreen = findViewById(R.id.stop_button);
        
        // Handle habit title and description from the new layout
        TextView habitTitle = findViewById(R.id.habit_title);
        TextView habitDescription = findViewById(R.id.habit_description);
        TextView progressText = findViewById(R.id.progress_text);
        TextView pomodoroCountText = findViewById(R.id.pomodoro_count_text);        Intent intent = getIntent();
        habitId = intent.getStringExtra(EXTRA_HABIT_ID);
        String habitTitleText = intent.getStringExtra("habit_title");
        int pomodoroLengthMin = intent.getIntExtra(EXTRA_POMODORO_LENGTH_MIN, 25); // Default to 25 mins

        if (habitId == null) {
            // Handle error: habitId is essential
            finish(); // Or show an error message
            return;
        }

        // Populate the new layout views
        if (habitTitle != null && habitTitleText != null) {
            habitTitle.setText(habitTitleText);
        }
        if (habitDescription != null) {
            habitDescription.setText("Pomodoro Focus Session");
        }
        if (progressText != null) {
            progressText.setText(String.format("Focus for %d minutes. You can do it!", pomodoroLengthMin));
        }
        if (pomodoroCountText != null) {
            pomodoroCountText.setText("1 of 1"); // For now, single pomodoro session
        }

        initialDurationMillis = TimeUnit.MINUTES.toMillis(pomodoroLengthMin);
        timeLeftInMillis = initialDurationMillis;

        updateTimerText();

        pauseResumeButtonFullscreen.setOnClickListener(v -> {
            if (timerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        stopButtonFullscreen.setOnClickListener(v -> stopTimer(false)); // false = not completed

        startTimer(); // Auto-start timer when activity opens
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }            @Override
            public void onFinish() {
                timerRunning = false;
                timerPaused = false;
                
                // Mark habit as completed using HabitManagerService
                habitManagerService.verifyHabitWithPomodoro(habitId);
                
                // Update UI to show completion
                TextView progressText = findViewById(R.id.progress_text);
                if (progressText != null) {
                    progressText.setText("🎉 Pomodoro Complete! Well done!");
                    progressText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
                
                // Show success toast
                Toast.makeText(PomodoroActivity.this, 
                    "🎉 Pomodoro completed! Habit marked as done!", Toast.LENGTH_SHORT).show();
                
                pauseResumeButtonFullscreen.setVisibility(View.GONE);
                stopButtonFullscreen.setText("Close ✓");
                stopButtonFullscreen.setOnClickListener(v -> finish());
                
                // Set result for calling activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("habitId", habitId);
                resultIntent.putExtra("completed", true);
                setResult(RESULT_OK, resultIntent);
            }
        }.start();        timerRunning = true;
        timerPaused = false;
        pauseResumeButtonFullscreen.setText("Pause");
        
        // Reset progress text when starting
        TextView progressText = findViewById(R.id.progress_text);
        if (progressText != null) {
            int pomodoroLengthMin = (int) (initialDurationMillis / (1000 * 60));
            progressText.setText(String.format("Focus for %d minutes. You can do it!", pomodoroLengthMin));
            progressText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        
        stopButtonFullscreen.setVisibility(View.VISIBLE);
        stopButtonFullscreen.setText("Stop");
        stopButtonFullscreen.setOnClickListener(v -> stopTimer(false));
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        timerPaused = true;
        pauseResumeButtonFullscreen.setText("Resume");
    }

    private void stopTimer(boolean completed) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        timerPaused = false;
        // TODO: Communicate stop/completion status (and habitId) back
        // For now, just finish the activity
        // If completed is true, it means natural finish. If false, user stopped.
        Intent resultIntent = new Intent();
        resultIntent.putExtra("habitId", habitId);
        resultIntent.putExtra("completed", completed);
        setResult(RESULT_OK, resultIntent); 
        finish();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerTextViewFullscreen.setText(timeFormatted);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
