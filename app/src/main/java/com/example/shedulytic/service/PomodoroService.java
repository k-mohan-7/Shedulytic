package com.example.shedulytic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.shedulytic.R;
import com.example.shedulytic.MainActivity;
import java.util.Locale;

/**
 * Service to handle Pomodoro technique sessions for habits
 */
public class PomodoroService extends Service {
    private static final String TAG = "PomodoroService";
    private static final int NOTIFICATION_ID = 1001;
    
    // Broadcast actions
    public static final String ACTION_TIMER_TICK = "com.example.shedulytic.ACTION_TIMER_TICK";
    public static final String ACTION_TIMER_FINISHED = "com.example.shedulytic.ACTION_TIMER_FINISHED";
    public static final String ACTION_POMODORO_COMPLETED = "com.example.shedulytic.ACTION_POMODORO_COMPLETED";
    public static final String ACTION_BREAK_STARTED = "com.example.shedulytic.ACTION_BREAK_STARTED";
    public static final String ACTION_WORK_STARTED = "com.example.shedulytic.ACTION_WORK_STARTED";
    
    // Broadcast extras
    public static final String EXTRA_TIME_REMAINING = "com.example.shedulytic.EXTRA_TIME_REMAINING";
    public static final String EXTRA_HABIT_ID = "com.example.shedulytic.EXTRA_HABIT_ID";
    public static final String EXTRA_POMODORO_COUNT = "com.example.shedulytic.EXTRA_POMODORO_COUNT";
    public static final String EXTRA_COMPLETED_COUNT = "com.example.shedulytic.EXTRA_COMPLETED_COUNT";
    public static final String EXTRA_IS_BREAK = "com.example.shedulytic.EXTRA_IS_BREAK";
    
    // Notification channel
    private static final String CHANNEL_ID = "pomodoro_channel";
    
    // Pomodoro states
    public static final int STATE_IDLE = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_REST = 3;
    
    // Default values
    private static final long DEFAULT_WORK_TIME = 25 * 60 * 1000; // 25 minutes
    private static final long DEFAULT_BREAK_TIME = 5 * 60 * 1000; // 5 minutes
    
    // Binder for client communication
    private final IBinder binder = new PomodoroBinder();
    
    // Timer state
    private CountDownTimer timer;
    private long timeRemaining = 0;
    private int currentState = STATE_IDLE;
    private int totalPomodoros = 1;
    private int completedPomodoros = 0;
    private String currentHabitId;
    private long workDuration = DEFAULT_WORK_TIME;
    private long breakDuration = DEFAULT_BREAK_TIME;
    private boolean isBreak = false;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private boolean timerRunning = false;
    
    // Notification components
    private NotificationManager notificationManager;
    
    // Wakelock to keep timer running when screen is off
    private PowerManager.WakeLock wakeLock;
    
    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        
        // Initialize wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Shedulytic:PomodoroWakeLock");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as a foreground service
        startForeground(NOTIFICATION_ID, buildNotification("Pomodoro Timer", "Ready to start"));
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    public class PomodoroBinder extends Binder {
        public PomodoroService getService() {
            return PomodoroService.this;
        }
    }
    
    /**
     * Start a Pomodoro session
     * @param habitId ID of the habit
     * @param count Number of Pomodoro cycles
     * @param durationMinutes Duration of each work session in minutes
     */
    public void startPomodoro(String habitId, int count, int durationMinutes) {
        currentHabitId = habitId;
        totalPomodoros = Math.max(1, count);
        completedPomodoros = 0;
        
        // Convert minutes to milliseconds
        workDuration = durationMinutes * 60 * 1000;
        
        // Default break is 1/5 of work time or 5 minutes, whichever is shorter
        breakDuration = Math.min(workDuration / 5, DEFAULT_BREAK_TIME);
        
        // Start with work session
        isBreak = false;
        
        // Acquire wake lock to keep timer running
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(24*60*60*1000L /*24 hours*/);
        }
        
        // Start timer
        startWorkSession();
    }
    
    /**
     * Start a work session
     */
    private void startWorkSession() {
        isBreak = false;
        timeRemaining = workDuration;
        
        // Update notification
        String title = "Pomodoro - Work Session";
        String content = String.format("%d of %d - %02d:%02d remaining", 
                                     completedPomodoros + 1, totalPomodoros,
                                     timeRemaining / 60000, (timeRemaining % 60000) / 1000);
        updateNotification(title, content);
        
        // Broadcast work session started
        Intent intent = new Intent(ACTION_WORK_STARTED);
        intent.putExtra(EXTRA_HABIT_ID, currentHabitId);
        intent.putExtra(EXTRA_TIME_REMAINING, timeRemaining);
        intent.putExtra(EXTRA_POMODORO_COUNT, totalPomodoros);
        intent.putExtra(EXTRA_COMPLETED_COUNT, completedPomodoros);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        
        // Start timer
        startTimer();
    }
    
    /**
     * Start a break session
     */
    private void startBreakSession() {
        isBreak = true;
        timeRemaining = breakDuration;
        
        // Update notification
        String title = "Pomodoro - Break Time";
        String content = String.format("Break %d of %d - %02d:%02d remaining", 
                                     completedPomodoros, totalPomodoros,
                                     timeRemaining / 60000, (timeRemaining % 60000) / 1000);
        updateNotification(title, content);
        
        // Broadcast break session started
        Intent intent = new Intent(ACTION_BREAK_STARTED);
        intent.putExtra(EXTRA_HABIT_ID, currentHabitId);
        intent.putExtra(EXTRA_TIME_REMAINING, timeRemaining);
        intent.putExtra(EXTRA_POMODORO_COUNT, totalPomodoros);
        intent.putExtra(EXTRA_COMPLETED_COUNT, completedPomodoros);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        
        // Start timer
        startTimer();
    }
    
    /**
     * Start the countdown timer
     */
    private void startTimer() {
        // Cancel existing timer if any
        if (timer != null) {
            timer.cancel();
        }
        
        // Create new timer
        timer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                
                // Update notification every 15 seconds
                if (millisUntilFinished % 15000 <= 1000) {
                    String phase = isBreak ? "Break" : "Work";
                    String title = "Pomodoro - " + phase + " Session";
                    String content = String.format("%s %d of %d - %02d:%02d remaining", 
                                               phase, completedPomodoros + (isBreak ? 0 : 1), totalPomodoros,
                                               millisUntilFinished / 60000, (millisUntilFinished % 60000) / 1000);
                    updateNotification(title, content);
                }
                
                // Broadcast tick event
                Intent intent = new Intent(ACTION_TIMER_TICK);
                intent.putExtra(EXTRA_HABIT_ID, currentHabitId);
                intent.putExtra(EXTRA_TIME_REMAINING, millisUntilFinished);
                intent.putExtra(EXTRA_IS_BREAK, isBreak);
                intent.putExtra(EXTRA_POMODORO_COUNT, totalPomodoros);
                intent.putExtra(EXTRA_COMPLETED_COUNT, completedPomodoros);
                LocalBroadcastManager.getInstance(PomodoroService.this).sendBroadcast(intent);
            }
            
            @Override
            public void onFinish() {
                // Broadcast finished event
                Intent intent = new Intent(ACTION_TIMER_FINISHED);
                intent.putExtra(EXTRA_HABIT_ID, currentHabitId);
                intent.putExtra(EXTRA_IS_BREAK, isBreak);
                LocalBroadcastManager.getInstance(PomodoroService.this).sendBroadcast(intent);
                
                // Handle session completion
                if (isBreak) {
                    // Break finished, start next work session if available
                    if (completedPomodoros < totalPomodoros) {
                        startWorkSession();
                    } else {
                        // All pomodoros completed
                        pomodoroCompleted();
                    }
                } else {
                    // Work session finished
                    completedPomodoros++;
                    
                    // Play alert and vibrate
                    playAlert();
                    
                    // If all pomodoros completed, don't start break
                    if (completedPomodoros >= totalPomodoros) {
                        pomodoroCompleted();
                    } else {
                        startBreakSession();
                    }
                }
            }
        };
        
        // Start the timer
        timer.start();
        isRunning = true;
        isPaused = false;
        timerRunning = true;
    }
    
    /**
     * Pause the timer
     */
    public void pauseTimer() {
        if (timer != null && isRunning && !isPaused) {
            timer.cancel();
            isPaused = true;
            isRunning = false;
            
            // Update notification
            String phase = isBreak ? "Break" : "Work";
            String title = "Pomodoro - Paused";
            String content = String.format("%s %d of %d - %02d:%02d remaining", 
                                       phase, completedPomodoros + (isBreak ? 0 : 1), totalPomodoros,
                                       timeRemaining / 60000, (timeRemaining % 60000) / 1000);
            updateNotification(title, content);
        }
    }
    
    /**
     * Resume the timer
     */
    public void resumeTimer() {
        if (isPaused) {
            // Create new timer with remaining time
            timer = new CountDownTimer(timeRemaining, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeRemaining = millisUntilFinished;
                    
                    // Update notification every 15 seconds
                    if (millisUntilFinished % 15000 <= 1000) {
                        String phase = isBreak ? "Break" : "Work";
                        String title = "Pomodoro - " + phase + " Session";
                        String content = String.format("%s %d of %d - %02d:%02d remaining", 
                                                   phase, completedPomodoros + (isBreak ? 0 : 1), totalPomodoros,
                                                   millisUntilFinished / 60000, (millisUntilFinished % 60000) / 1000);
                        updateNotification(title, content);
                    }
                    
                    // Broadcast tick event
                    Intent intent = new Intent(ACTION_TIMER_TICK);
                    intent.putExtra(EXTRA_HABIT_ID, currentHabitId);
                    intent.putExtra(EXTRA_TIME_REMAINING, millisUntilFinished);
                    intent.putExtra(EXTRA_IS_BREAK, isBreak);
                    intent.putExtra(EXTRA_POMODORO_COUNT, totalPomodoros);
                    intent.putExtra(EXTRA_COMPLETED_COUNT, completedPomodoros);
                    LocalBroadcastManager.getInstance(PomodoroService.this).sendBroadcast(intent);
                }
                
                @Override
                public void onFinish() {
                    // Same as above onFinish
                    Intent intent = new Intent(ACTION_TIMER_FINISHED);
                    intent.putExtra(EXTRA_HABIT_ID, currentHabitId);
                    intent.putExtra(EXTRA_IS_BREAK, isBreak);
                    LocalBroadcastManager.getInstance(PomodoroService.this).sendBroadcast(intent);
                    
                    if (isBreak) {
                        if (completedPomodoros < totalPomodoros) {
                            startWorkSession();
                        } else {
                            pomodoroCompleted();
                        }
                    } else {
                        completedPomodoros++;
                        playAlert();
                        
                        if (completedPomodoros >= totalPomodoros) {
                            pomodoroCompleted();
                        } else {
                            startBreakSession();
                        }
                    }
                }
            };
            
            // Start the timer
            timer.start();
            isRunning = true;
            isPaused = false;
            
            // Update notification
            String phase = isBreak ? "Break" : "Work";
            String title = "Pomodoro - " + phase + " Session";
            String content = String.format("%s %d of %d - %02d:%02d remaining", 
                                       phase, completedPomodoros + (isBreak ? 0 : 1), totalPomodoros,
                                       timeRemaining / 60000, (timeRemaining % 60000) / 1000);
            updateNotification(title, content);
        }
    }
    
    /**
     * Stop the timer
     */
    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        
        isRunning = false;
        isPaused = false;
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // Update notification
        updateNotification("Pomodoro Timer Stopped", "Session ended");
    }
    
    /**
     * Handle when all pomodoros are completed
     */
    private void pomodoroCompleted() {
        // Play final alert and vibrate
        playAlert();
        
        // Send pomodoro completed broadcast
        Intent intent = new Intent(ACTION_POMODORO_COMPLETED);
        intent.putExtra(EXTRA_HABIT_ID, currentHabitId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        
        // Update notification
        updateNotification("Pomodoro Completed!", 
                       String.format("Completed %d pomodoros", completedPomodoros));
        
        // Stop timer
        stopTimer();
    }
    
    /**
     * Play alert sound and vibrate when a session ends
     */
    private void playAlert() {
        try {
            // Play notification sound
            Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            RingtoneManager.getRingtone(getApplicationContext(), notificationSound).play();
            
            // Vibrate
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(500);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing alert: " + e.getMessage());
        }
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pomodoro Timer",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            
            channel.setDescription("Notifications for Pomodoro timer sessions");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Build notification
     */
    private NotificationCompat.Builder buildNotificationBuilder(String title, String content) {
        // Create intent for clicking the notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        // Build notification
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_task)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true);
    }
    
    /**
     * Build and return a notification
     */
    private android.app.Notification buildNotification(String title, String content) {
        return buildNotificationBuilder(title, content).build();
    }
    
    /**
     * Update the service notification
     */
    private void updateNotification(String title, String content) {
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(title, content));
        }
    }
    
    /**
     * Get current Pomodoro state
     */
    public int getCurrentState() {
        return currentState;
    }
    
    /**
     * Get time remaining in current Pomodoro or break
     */
    public long getTimeRemaining() {
        return timeRemaining;
    }
    
    /**
     * Get completed Pomodoros count
     */
    public int getCompletedPomodoros() {
        return completedPomodoros;
    }
    
    /**
     * Get total Pomodoros required
     */
    public int getTotalPomodoros() {
        return totalPomodoros;
    }
    
    /**
     * Get the current state of the timer
     * @return True if the timer is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Get the current state of the timer
     * @return True if the timer is paused, false otherwise
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * Get the current phase of the timer
     * @return True if in break phase, false if in work phase
     */
    public boolean isBreak() {
        return isBreak;
    }
    
    /**
     * Check if timer is currently running
     * @return true if timer is running, false otherwise
     */
    public boolean isTimerRunning() {
        return timerRunning;
    }
    
    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }
} 