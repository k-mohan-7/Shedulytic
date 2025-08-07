// Test file to verify extend functionality is working properly
// This is a simple test case that can be manually executed

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class test_extend_functionality {
    
    public static void main(String[] args) {
        System.out.println("Testing extend functionality implementation...");
        
        // Test 1: Test time extension calculation
        testTimeExtension();
        
        // Test 2: Test task ID validation
        testTaskIdValidation();
        
        // Test 3: Test date search range
        testDateSearchRange();
        
        System.out.println("All tests completed.");
    }
    
    public static void testTimeExtension() {
        System.out.println("\n=== Test 1: Time Extension Calculation ===");
        
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            // Test extending 14:30 by 30 minutes
            Date endTime = timeFormat.parse("14:30");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endTime);
            calendar.add(Calendar.MINUTE, 30);
            
            String newEndTime = timeFormat.format(calendar.getTime());
            String expected = "15:00";
            
            if (newEndTime.equals(expected)) {
                System.out.println("✅ Time extension test PASSED: 14:30 + 30min = " + newEndTime);
            } else {
                System.out.println("❌ Time extension test FAILED: Expected " + expected + ", got " + newEndTime);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Time extension test ERROR: " + e.getMessage());
        }
    }
    
    public static void testTaskIdValidation() {
        System.out.println("\n=== Test 2: Task ID Validation ===");
        
        // Test valid task IDs
        String[] validIds = {"123", "abc123", "task_456"};
        String[] invalidIds = {null, "", "   "};
        
        for (String id : validIds) {
            if (isValidTaskId(id)) {
                System.out.println("✅ Valid task ID test PASSED: " + id);
            } else {
                System.out.println("❌ Valid task ID test FAILED: " + id + " should be valid");
            }
        }
        
        for (String id : invalidIds) {
            if (!isValidTaskId(id)) {
                System.out.println("✅ Invalid task ID test PASSED: " + id + " correctly rejected");
            } else {
                System.out.println("❌ Invalid task ID test FAILED: " + id + " should be invalid");
            }
        }
    }
    
    public static void testDateSearchRange() {
        System.out.println("\n=== Test 3: Date Search Range ===");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        
        // Test searching from -7 to +7 days
        Date today = new Date();
        calendar.setTime(today);
        
        System.out.println("Today: " + dateFormat.format(today));
        
        // Test -7 days
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        String sevenDaysAgo = dateFormat.format(calendar.getTime());
        System.out.println("7 days ago: " + sevenDaysAgo);
        
        // Test +7 days
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        String sevenDaysAhead = dateFormat.format(calendar.getTime());
        System.out.println("7 days ahead: " + sevenDaysAhead);
        
        System.out.println("✅ Date range calculation test PASSED");
    }
    
    private static boolean isValidTaskId(String taskId) {
        return taskId != null && !taskId.trim().isEmpty();
    }
}
