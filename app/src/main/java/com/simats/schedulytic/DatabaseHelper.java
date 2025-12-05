package com.simats.schedulytic;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "shedulytic.db";
    private static final int DATABASE_VERSION = 6;
    private static DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tasks table
        db.execSQL("CREATE TABLE tasks (" +
                "task_id VARCHAR(36) PRIMARY KEY," +
                "user_id VARCHAR(36)," +
                "task_type VARCHAR(20)," +
                "title TEXT," +
                "description TEXT," +
                "start_time TEXT," +
                "end_time TEXT," +
                "due_date TEXT," +
                "status VARCHAR(20)," +
                "repeat_frequency VARCHAR(20)," +
                "priority VARCHAR(10)," +
                "current_streak INT DEFAULT 0," +
                "parent_task_id VARCHAR(36)," +
                "timestamp INTEGER" +
                ")");

        // Create task_completions table
        db.execSQL("CREATE TABLE task_completions (" +
                "task_id VARCHAR(36)," +
                "user_id VARCHAR(36)," +
                "completion_date DATE," +
                "PRIMARY KEY (task_id, user_id, completion_date)," +
                "FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE" +
                ")");

        // Create habits table for tracking habits
        db.execSQL("CREATE TABLE habits (" +
                "habit_id VARCHAR(36) PRIMARY KEY," +
                "user_id VARCHAR(36)," +
                "title TEXT," +
                "description TEXT," +
                "verification_method VARCHAR(20)," +
                "is_completed INTEGER DEFAULT 0," +
                "current_streak INTEGER DEFAULT 0," +
                "total_completions INTEGER DEFAULT 0," +
                "frequency VARCHAR(20) DEFAULT 'daily'," +
                "method_data TEXT," +  // JSON field for method-specific data
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
        
        // Create habit_completions table for tracking daily completions
        db.execSQL("CREATE TABLE habit_completions (" +
                "habit_id VARCHAR(36)," +
                "user_id VARCHAR(36)," +
                "completion_date DATE," +
                "PRIMARY KEY (habit_id, user_id, completion_date)," +
                "FOREIGN KEY (habit_id) REFERENCES habits(habit_id) ON DELETE CASCADE" +
                ")");

        // Create indexes
        db.execSQL("CREATE INDEX idx_task_completions_date ON task_completions(task_id, completion_date)");
        db.execSQL("CREATE INDEX idx_habit_completions_date ON habit_completions(habit_id, completion_date)");
        db.execSQL("CREATE INDEX idx_habits_user ON habits(user_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Add streak column if it doesn't exist
            db.execSQL("ALTER TABLE tasks ADD COLUMN current_streak INT DEFAULT 0");

            // Create task_completions table if it doesn't exist
            db.execSQL("CREATE TABLE IF NOT EXISTS task_completions (" +
                    "task_id VARCHAR(36)," +
                    "user_id VARCHAR(36)," +
                    "completion_date DATE," +
                    "PRIMARY KEY (task_id, user_id, completion_date)," +
                    "FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE" +
                    ")");

            // Create index if it doesn't exist
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_task_completions_date ON task_completions(task_id, completion_date)");
        }
        
        if (oldVersion < 5) {
            // Create habits table
            db.execSQL("CREATE TABLE IF NOT EXISTS habits (" +
                    "habit_id VARCHAR(36) PRIMARY KEY," +
                    "user_id VARCHAR(36)," +
                    "title TEXT," +
                    "description TEXT," +
                    "verification_method VARCHAR(20)," +
                    "is_completed INTEGER DEFAULT 0," +
                    "current_streak INTEGER DEFAULT 0," +
                    "total_completions INTEGER DEFAULT 0," +
                    "frequency VARCHAR(20) DEFAULT 'daily'," +
                    "method_data TEXT," +  // JSON field for method-specific data
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            
            // Create habit_completions table
            db.execSQL("CREATE TABLE IF NOT EXISTS habit_completions (" +
                    "habit_id VARCHAR(36)," +
                    "user_id VARCHAR(36)," +
                    "completion_date DATE," +
                    "PRIMARY KEY (habit_id, user_id, completion_date)," +
                    "FOREIGN KEY (habit_id) REFERENCES habits(habit_id) ON DELETE CASCADE" +
                    ")");
            
            // Create indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_habit_completions_date ON habit_completions(habit_id, completion_date)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_habits_user ON habits(user_id)");
        }
        
        if (oldVersion < 6) {
            // Add timestamp column to tasks table
            try {
                db.execSQL("ALTER TABLE tasks ADD COLUMN timestamp INTEGER");
                Log.d("DatabaseHelper", "Added timestamp column to tasks table");
            } catch (Exception e) {
                Log.w("DatabaseHelper", "timestamp column may already exist: " + e.getMessage());
            }
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}