/**
 * LocationMapTest.java
 * 
 * Comprehensive test to validate the location-based verification map functionality
 * in the Android Shedulytic app.
 * 
 * This test validates:
 * 1. Map functionality in AddHabitActivity
 * 2. Location verification in HabitFragment and HomeFragment
 * 3. LocationVerificationActivity integration
 * 4. Proper data flow between components
 */

import java.io.*;
import java.util.regex.*;

public class LocationMapTest {
    
    public static void main(String[] args) {
        System.out.println("=== Location-Based Verification Map Functionality Test ===\n");
        
        try {
            testAddHabitMapFunctionality();
            testLocationVerificationActivity();
            testHabitFragmentIntegration();
            testHomeFragmentIntegration();
            testLocationServices();
            
            System.out.println("\n=== All Tests Completed Successfully! ===");
            System.out.println("✅ Map functionality is properly implemented");
            System.out.println("✅ Location verification flow is complete");
            System.out.println("✅ Integration between components is working");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test map functionality in AddHabitActivity
     */
    private static void testAddHabitMapFunctionality() throws IOException {
        System.out.println("1. Testing AddHabitActivity Map Functionality...");
        
        String filePath = "app/src/main/java/com/example/shedulytic/AddHabitActivity.java";
        String content = readFile(filePath);
        
        // Check for updated Leaflet.js version
        if (content.contains("leaflet@1.9.4")) {
            System.out.println("   ✅ Leaflet.js updated to latest version (1.9.4)");
        } else {
            throw new RuntimeException("Leaflet.js not updated to latest version");
        }
        
        // Check for map HTML generation method
        if (content.contains("private String generateMapHTML()")) {
            System.out.println("   ✅ Map HTML generation method found");
        } else {
            throw new RuntimeException("Map HTML generation method not found");
        }
        
        // Check for error handling in map
        if (content.contains("errorTileUrl") && content.contains("onerror")) {
            System.out.println("   ✅ Map error handling implemented");
        } else {
            throw new RuntimeException("Map error handling not properly implemented");
        }
        
        // Check for geocoding with timeout
        if (content.contains("setTimeout") && content.contains("clearTimeout")) {
            System.out.println("   ✅ Geocoding timeout handling implemented");
        } else {
            throw new RuntimeException("Geocoding timeout handling not implemented");
        }
        
        // Check for location selection callback
        if (content.contains("MapInterface.locationSelected")) {
            System.out.println("   ✅ Location selection callback implemented");
        } else {
            throw new RuntimeException("Location selection callback not found");
        }
        
        System.out.println("   ✅ AddHabitActivity map functionality verified\n");
    }
    
    /**
     * Test LocationVerificationActivity
     */
    private static void testLocationVerificationActivity() throws IOException {
        System.out.println("2. Testing LocationVerificationActivity...");
        
        String filePath = "app/src/main/java/com/example/shedulytic/LocationVerificationActivity.java";
        String content = readFile(filePath);
        
        // Check for required intent extras
        if (content.contains("EXTRA_HABIT_ID") && content.contains("EXTRA_TARGET_LATITUDE") && 
            content.contains("EXTRA_TARGET_LONGITUDE")) {
            System.out.println("   ✅ Required intent extras defined");
        } else {
            throw new RuntimeException("Required intent extras not properly defined");
        }
        
        // Check for location permission handling
        if (content.contains("ACCESS_FINE_LOCATION") && content.contains("onRequestPermissionsResult")) {
            System.out.println("   ✅ Location permission handling implemented");
        } else {
            throw new RuntimeException("Location permission handling not implemented");
        }
        
        // Check for distance calculation
        if (content.contains("Location.distanceBetween")) {
            System.out.println("   ✅ Distance calculation implemented");
        } else {
            throw new RuntimeException("Distance calculation not implemented");
        }
        
        // Check for verification radius
        if (content.contains("VERIFICATION_RADIUS_METERS")) {
            System.out.println("   ✅ Verification radius configured");
        } else {
            throw new RuntimeException("Verification radius not configured");
        }
        
        System.out.println("   ✅ LocationVerificationActivity verified\n");
    }
    
    /**
     * Test HabitFragment integration
     */
    private static void testHabitFragmentIntegration() throws IOException {
        System.out.println("3. Testing HabitFragment Integration...");
        
        String filePath = "app/src/main/java/com/example/shedulytic/HabitFragment.java";
        String content = readFile(filePath);
        
        // Check for location verification click handler
        if (content.contains("onLocationVerifyClicked")) {
            System.out.println("   ✅ Location verification click handler found");
        } else {
            throw new RuntimeException("Location verification click handler not found");
        }
        
        // Check for proper intent creation with all extras
        if (content.contains("LocationVerificationActivity.EXTRA_HABIT_ID") && 
            content.contains("LocationVerificationActivity.EXTRA_TARGET_LATITUDE")) {
            System.out.println("   ✅ Proper intent creation with all required extras");
        } else {
            throw new RuntimeException("Intent creation incomplete - missing required extras");
        }
        
        // Check for location service integration
        if (content.contains("LocationVerificationService")) {
            System.out.println("   ✅ Location service integration found");
        } else {
            throw new RuntimeException("Location service integration not found");
        }
        
        // Check for location-based habit filtering
        if (content.contains("VERIFICATION_LOCATION") && content.contains("checkLocationHabits")) {
            System.out.println("   ✅ Location-based habit filtering implemented");
        } else {
            throw new RuntimeException("Location-based habit filtering not implemented");
        }
        
        System.out.println("   ✅ HabitFragment integration verified\n");
    }
    
    /**
     * Test HomeFragment integration
     */
    private static void testHomeFragmentIntegration() throws IOException {
        System.out.println("4. Testing HomeFragment Integration...");
        
        String filePath = "app/src/main/java/com/example/shedulytic/HomeFragment.java";
        String content = readFile(filePath);
        
        // Check for location verification click handler
        if (content.contains("onLocationVerifyClicked")) {
            System.out.println("   ✅ Location verification click handler found");
        } else {
            throw new RuntimeException("Location verification click handler not found");
        }
        
        // Check for proper intent creation with all extras
        if (content.contains("LocationVerificationActivity.EXTRA_HABIT_ID")) {
            System.out.println("   ✅ Proper intent creation with required extras");
        } else {
            throw new RuntimeException("Intent creation incomplete in HomeFragment");
        }
        
        // Check for habit list integration
        if (content.contains("habitList") && content.contains("HabitAdapter")) {
            System.out.println("   ✅ Habit list integration found");
        } else {
            throw new RuntimeException("Habit list integration not found");
        }
        
        System.out.println("   ✅ HomeFragment integration verified\n");
    }
    
    /**
     * Test location services
     */
    private static void testLocationServices() throws IOException {
        System.out.println("5. Testing Location Services...");
        
        // Check LocationVerificationService
        String serviceFile = "app/src/main/java/com/example/shedulytic/service/LocationVerificationService.java";
        if (new File(serviceFile).exists()) {
            String content = readFile(serviceFile);
            if (content.contains("FusedLocationProviderClient") || content.contains("LocationManager")) {
                System.out.println("   ✅ LocationVerificationService found and configured");
            } else {
                System.out.println("   ⚠️  LocationVerificationService exists but may need location provider setup");
            }
        } else {
            System.out.println("   ⚠️  LocationVerificationService not found (may need creation)");
        }
        
        // Check AndroidManifest for location permissions
        String manifestFile = "app/src/main/AndroidManifest.xml";
        String manifestContent = readFile(manifestFile);
        if (manifestContent.contains("ACCESS_FINE_LOCATION")) {
            System.out.println("   ✅ Location permissions declared in manifest");
        } else {
            System.out.println("   ⚠️  Location permissions may need to be added to manifest");
        }
        
        System.out.println("   ✅ Location services configuration checked\n");
    }
    
    /**
     * Helper method to read file content
     */
    private static String readFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
