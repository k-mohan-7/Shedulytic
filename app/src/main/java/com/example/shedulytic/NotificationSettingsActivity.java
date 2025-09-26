package com.example.shedulytic;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity to manage notification preferences and settings
 */
public class NotificationSettingsActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "NotificationPrefs";
    
    // Preference keys
    public static final String PREF_WORKFLOW_NOTIFICATIONS = "workflow_notifications_enabled";
    public static final String PREF_REMINDER_NOTIFICATIONS = "reminder_notifications_enabled";
    public static final String PREF_EARLY_REMINDERS = "early_reminders_enabled";
    public static final String PREF_VIBRATION = "vibration_enabled";
    public static final String PREF_SOUND = "sound_enabled";
    public static final String PREF_SNOOZE_DURATION = "snooze_duration_minutes";
    public static final String PREF_EARLY_REMINDER_MINUTES = "early_reminder_minutes";
    
    // Default values
    public static final boolean DEFAULT_WORKFLOW_NOTIFICATIONS = true;
    public static final boolean DEFAULT_REMINDER_NOTIFICATIONS = true;
    public static final boolean DEFAULT_EARLY_REMINDERS = true;
    public static final boolean DEFAULT_VIBRATION = true;
    public static final boolean DEFAULT_SOUND = true;
    public static final int DEFAULT_SNOOZE_DURATION = 15;
    public static final int DEFAULT_EARLY_REMINDER_MINUTES = 5;
    
    private CheckBox workflowNotificationsCheckBox;
    private CheckBox reminderNotificationsCheckBox;
    private CheckBox earlyRemindersCheckBox;
    private CheckBox vibrationCheckBox;
    private CheckBox soundCheckBox;
    private SeekBar snoozeDurationSeekBar;
    private SeekBar earlyReminderSeekBar;
    private TextView snoozeDurationText;
    private TextView earlyReminderText;
    
    private SharedPreferences preferences;
      @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);
        
        // Initialize preferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Initialize views
        initializeViews();
        
        // Load current settings
        loadSettings();
        
        // Set up listeners
        setupListeners();
        
        // Set up back button
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }
    
    private void initializeViews() {
        workflowNotificationsCheckBox = findViewById(R.id.workflow_notifications_checkbox);
        reminderNotificationsCheckBox = findViewById(R.id.reminder_notifications_checkbox);
        earlyRemindersCheckBox = findViewById(R.id.early_reminders_checkbox);
        vibrationCheckBox = findViewById(R.id.vibration_checkbox);
        soundCheckBox = findViewById(R.id.sound_checkbox);
        snoozeDurationSeekBar = findViewById(R.id.snooze_duration_seekbar);
        earlyReminderSeekBar = findViewById(R.id.early_reminder_seekbar);
        snoozeDurationText = findViewById(R.id.snooze_duration_text);
        earlyReminderText = findViewById(R.id.early_reminder_text);
        
        // Set up seek bars
        snoozeDurationSeekBar.setMax(60); // Max 60 minutes
        snoozeDurationSeekBar.setMin(5);  // Min 5 minutes
        
        earlyReminderSeekBar.setMax(30);  // Max 30 minutes
        earlyReminderSeekBar.setMin(1);   // Min 1 minute
    }
    
    private void loadSettings() {
        // Load checkbox preferences
        workflowNotificationsCheckBox.setChecked(
            preferences.getBoolean(PREF_WORKFLOW_NOTIFICATIONS, DEFAULT_WORKFLOW_NOTIFICATIONS));
        reminderNotificationsCheckBox.setChecked(
            preferences.getBoolean(PREF_REMINDER_NOTIFICATIONS, DEFAULT_REMINDER_NOTIFICATIONS));
        earlyRemindersCheckBox.setChecked(
            preferences.getBoolean(PREF_EARLY_REMINDERS, DEFAULT_EARLY_REMINDERS));
        vibrationCheckBox.setChecked(
            preferences.getBoolean(PREF_VIBRATION, DEFAULT_VIBRATION));
        soundCheckBox.setChecked(
            preferences.getBoolean(PREF_SOUND, DEFAULT_SOUND));
        
        // Load seek bar preferences
        int snoozeDuration = preferences.getInt(PREF_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION);
        int earlyReminderMinutes = preferences.getInt(PREF_EARLY_REMINDER_MINUTES, DEFAULT_EARLY_REMINDER_MINUTES);
        
        snoozeDurationSeekBar.setProgress(snoozeDuration);
        earlyReminderSeekBar.setProgress(earlyReminderMinutes);
        
        updateSnoozeDurationText(snoozeDuration);
        updateEarlyReminderText(earlyReminderMinutes);
    }
    
    private void setupListeners() {
        // Checkbox listeners
        workflowNotificationsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(PREF_WORKFLOW_NOTIFICATIONS, isChecked);
        });
        
        reminderNotificationsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(PREF_REMINDER_NOTIFICATIONS, isChecked);
        });
        
        earlyRemindersCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(PREF_EARLY_REMINDERS, isChecked);
        });
        
        vibrationCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(PREF_VIBRATION, isChecked);
        });
        
        soundCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(PREF_SOUND, isChecked);
        });
        
        // Seek bar listeners
        snoozeDurationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSnoozeDurationText(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                savePreference(PREF_SNOOZE_DURATION, seekBar.getProgress());
                showToast("Snooze duration updated");
            }
        });
        
        earlyReminderSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateEarlyReminderText(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                savePreference(PREF_EARLY_REMINDER_MINUTES, seekBar.getProgress());
                showToast("Early reminder timing updated");
            }
        });
    }
    
    private void updateSnoozeDurationText(int minutes) {
        snoozeDurationText.setText("Snooze for " + minutes + " minutes");
    }
    
    private void updateEarlyReminderText(int minutes) {
        earlyReminderText.setText("Remind " + minutes + " minutes before");
    }
    
    private void savePreference(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
        showToast("Setting saved");
    }
    
    private void savePreference(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Helper method to get notification preferences from other parts of the app
     */
    public static SharedPreferences getNotificationPreferences(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    /**
     * Check if workflow notifications are enabled
     */
    public static boolean areWorkflowNotificationsEnabled(android.content.Context context) {
        return getNotificationPreferences(context).getBoolean(PREF_WORKFLOW_NOTIFICATIONS, DEFAULT_WORKFLOW_NOTIFICATIONS);
    }
    
    /**
     * Check if reminder notifications are enabled
     */
    public static boolean areReminderNotificationsEnabled(android.content.Context context) {
        return getNotificationPreferences(context).getBoolean(PREF_REMINDER_NOTIFICATIONS, DEFAULT_REMINDER_NOTIFICATIONS);
    }
    
    /**
     * Check if early reminders are enabled
     */
    public static boolean areEarlyRemindersEnabled(android.content.Context context) {
        return getNotificationPreferences(context).getBoolean(PREF_EARLY_REMINDERS, DEFAULT_EARLY_REMINDERS);
    }
    
    /**
     * Check if vibration is enabled
     */
    public static boolean isVibrationEnabled(android.content.Context context) {
        return getNotificationPreferences(context).getBoolean(PREF_VIBRATION, DEFAULT_VIBRATION);
    }
    
    /**
     * Check if sound is enabled
     */
    public static boolean isSoundEnabled(android.content.Context context) {
        return getNotificationPreferences(context).getBoolean(PREF_SOUND, DEFAULT_SOUND);
    }
    
    /**
     * Get snooze duration in minutes
     */
    public static int getSnoozeDuration(android.content.Context context) {
        return getNotificationPreferences(context).getInt(PREF_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION);
    }
    
    /**
     * Get early reminder minutes
     */
    public static int getEarlyReminderMinutes(android.content.Context context) {
        return getNotificationPreferences(context).getInt(PREF_EARLY_REMINDER_MINUTES, DEFAULT_EARLY_REMINDER_MINUTES);
    }
}
