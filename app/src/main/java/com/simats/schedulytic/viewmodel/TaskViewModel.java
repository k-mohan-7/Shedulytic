package com.simats.schedulytic.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.simats.schedulytic.HabitManager;
import com.simats.schedulytic.model.Task;

import java.util.List;

public class TaskViewModel extends ViewModel implements HabitManager.HabitListener {

    private final MutableLiveData<List<Task>> taskList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private HabitManager habitManager;

    public void initializeHabitManager(Context context) {
        habitManager = new HabitManager(context, this);
    }

    public void toggleHabitCompletion(String taskId, boolean completed) {
        if (habitManager != null) {
            habitManager.toggleHabitCompletion(taskId, completed);
        }
    }

    @Override
    public void onHabitCompleted(String taskId, int newStreak) {
        List<Task> currentTasks = taskList.getValue();
        if (currentTasks != null) {
            for (Task task : currentTasks) {
                if (task.getTaskId().equals(taskId)) {
                    task.setCurrentStreak(newStreak);
                    break;
                }
            }
            taskList.setValue(currentTasks);
        }
    }

    @Override
    public void onHabitUncompleted(String taskId, int newStreak) {
        onHabitCompleted(taskId, newStreak);
    }

    @Override
    public void onError(String message) {
        errorMessage.setValue(message);
    }

    @Override
    public void onHabitStreakUpdated(String taskId, int newStreak) {
        onHabitCompleted(taskId, newStreak);
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<List<Task>> getTaskList() {
        return taskList;
    }
}