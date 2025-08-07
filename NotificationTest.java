import com.example.shedulytic.NotificationHandler;
import com.example.shedulytic.Task;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Simple test to verify notification layouts work correctly
 */
public class NotificationTest {
    
    public static void testReminder(android.content.Context context) {
        try {
            NotificationHandler handler = new NotificationHandler(context);
            
            // Create a test reminder for 5 seconds from now
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 5);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            
            String testDate = dateFormat.format(calendar.getTime());
            String testTime = timeFormat.format(calendar.getTime());
            
            // Schedule reminder notification
            handler.scheduleReminderNotification(
                "999", 
                "Test Reminder", 
                "This is a test reminder to check notification layouts", 
                testTime, 
                testDate
            );
            
            System.out.println("Test reminder scheduled for: " + calendar.getTime());
            
        } catch (Exception e) {
            System.err.println("Error testing reminder: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void testWorkflow(android.content.Context context) {
        try {
            NotificationHandler handler = new NotificationHandler(context);
            
            // Create a test workflow for 5 seconds from now
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.add(Calendar.SECOND, 5);
            
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.add(Calendar.MINUTE, 15); // 15 minute workflow
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            
            String testDate = dateFormat.format(startCalendar.getTime());
            String startTime = timeFormat.format(startCalendar.getTime());
            String endTime = timeFormat.format(endCalendar.getTime());
            
            // Schedule workflow notifications
            handler.scheduleWorkflowNotifications(
                "998", 
                "Test Workflow", 
                "This is a test workflow to check notification layouts", 
                startTime, 
                endTime, 
                testDate
            );
            
            System.out.println("Test workflow scheduled from: " + startCalendar.getTime() + " to: " + endCalendar.getTime());
            
        } catch (Exception e) {
            System.err.println("Error testing workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
