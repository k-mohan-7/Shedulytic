package com.example.shedulytic;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Simple test runner to validate notification functionality
 */
@RunWith(AndroidJUnit4.class)
public class NotificationTestRunner {

    @Test
    public void testNotificationSystemIntegration() {
        // Context of the app under test
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.shedulytic", appContext.getPackageName());

        // Test that NotificationTestHelper can be instantiated and basic functionality works
        NotificationTestHelper helper = new NotificationTestHelper(appContext);
        assertTrue("Basic notification functionality should work", helper.testBasicFunctionality());
        assertTrue("Workflow notifications should work", helper.testWorkflowNotifications());
        assertTrue("Reminder notifications should work", helper.testReminderNotifications());
    }    @Test
    public void testNotificationIntegrationFromIntegrationTest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Run the corrected NotificationIntegrationTest static methods
        NotificationIntegrationTest.runBasicTest(appContext);
        NotificationIntegrationTest.runCurrentTimeTest(appContext);
        NotificationIntegrationTest.runButtonFunctionalityTest(appContext);
        
        // The test passes if no exceptions are thrown
        assertTrue("Integration tests should complete without exceptions", true);
    }
}
