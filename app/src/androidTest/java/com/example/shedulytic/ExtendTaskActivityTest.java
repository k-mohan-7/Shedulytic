package com.example.shedulytic;

import android.content.Context;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Test;
import org.junit.Rule;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Test for ExtendTaskActivity dialog UI improvements
 */
@RunWith(AndroidJUnit4.class)
public class ExtendTaskActivityTest {

    @Rule
    public ActivityTestRule<NotificationHandler.ExtendTaskActivity> mActivityRule = 
        new ActivityTestRule<>(NotificationHandler.ExtendTaskActivity.class, false, false);

    @Test
    public void testExtendDialogLaunch() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Create intent with task ID
        Intent intent = new Intent(targetContext, NotificationHandler.ExtendTaskActivity.class);
        intent.putExtra("task_id", "123");
        
        // Launch activity
        mActivityRule.launchActivity(intent);
        
        // Verify activity is not null (basic test that dialog can be created)
        assertNotNull(mActivityRule.getActivity());
    }
    
    @Test
    public void testTaskIdExtra() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        String testTaskId = "test_task_456";
        Intent intent = new Intent(targetContext, NotificationHandler.ExtendTaskActivity.class);
        intent.putExtra("task_id", testTaskId);
        
        mActivityRule.launchActivity(intent);
        
        // Verify the activity was created successfully
        assertNotNull(mActivityRule.getActivity());
        
        // Verify the task ID was passed correctly
        assertEquals(testTaskId, mActivityRule.getActivity().getIntent().getStringExtra("task_id"));
    }
}
