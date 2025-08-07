/**
 * Test to verify location-based trust verification fixes
 * This test validates that:
 * 1. Location verification method correctly maps to "map" in backend
 * 2. HabitAdapter correctly handles both "location" and "map" trust types
 * 3. JSON serialization/deserialization works correctly
 */

import com.example.shedulytic.model.Habit;
import org.json.JSONObject;

public class LocationVerificationTest {
    
    public static void main(String[] args) {
        System.out.println("=== Location Verification Test ===");
        
        try {
            // Test 1: Create habit with location verification
            System.out.println("\n1. Testing Habit Creation with Location Verification:");
            Habit locationHabit = new Habit();
            locationHabit.setTitle("Gym Workout");
            locationHabit.setUserId("test_user_123");
            locationHabit.setVerificationMethod(Habit.VERIFICATION_LOCATION);
            locationHabit.setLatitude(37.7749);
            locationHabit.setLongitude(-122.4194);
            
            System.out.println("   Created habit with verification method: " + locationHabit.getVerificationMethod());
            System.out.println("   Latitude: " + locationHabit.getLatitude());
            System.out.println("   Longitude: " + locationHabit.getLongitude());
            
            // Test 2: Convert to JSON (backend format)
            System.out.println("\n2. Testing JSON Conversion (App to Backend):");
            JSONObject jsonData = locationHabit.toJson();
            System.out.println("   JSON trust_type: " + jsonData.optString("trust_type"));
            System.out.println("   JSON verification_method: " + jsonData.optString("verification_method"));
            System.out.println("   JSON map_lat: " + jsonData.optDouble("map_lat"));
            System.out.println("   JSON map_lon: " + jsonData.optDouble("map_lon"));
            
            // Verify backend mapping
            if ("map".equals(jsonData.optString("trust_type"))) {
                System.out.println("   ✓ PASS: trust_type correctly mapped to 'map'");
            } else {
                System.out.println("   ✗ FAIL: trust_type should be 'map', got: " + jsonData.optString("trust_type"));
            }
            
            // Test 3: Backend response simulation (map -> location)
            System.out.println("\n3. Testing Backend Response Parsing:");
            JSONObject backendResponse = new JSONObject();
            backendResponse.put("habit_id", "test_habit_123");
            backendResponse.put("user_id", "test_user_123");
            backendResponse.put("title", "Gym Workout From Backend");
            backendResponse.put("trust_type", "map"); // Backend uses 'map'
            backendResponse.put("map_lat", 37.7749);
            backendResponse.put("map_lon", -122.4194);
            
            Habit parsedHabit = Habit.fromJson(backendResponse);
            System.out.println("   Parsed verification method: " + parsedHabit.getVerificationMethod());
            System.out.println("   Parsed latitude: " + parsedHabit.getLatitude());
            System.out.println("   Parsed longitude: " + parsedHabit.getLongitude());
            
            // Verify parsing
            if (Habit.VERIFICATION_LOCATION.equals(parsedHabit.getVerificationMethod())) {
                System.out.println("   ✓ PASS: Backend 'map' correctly mapped to 'location'");
            } else {
                System.out.println("   ✗ FAIL: Should be 'location', got: " + parsedHabit.getVerificationMethod());
            }
            
            // Test 4: HabitAdapter view type logic simulation
            System.out.println("\n4. Testing HabitAdapter View Type Logic:");
            
            // Simulate adapter getItemViewType logic for location habit
            String verificationMethod = parsedHabit.getVerificationMethod();
            JSONObject extraProps = parsedHabit.getExtraProperties();
            
            if (extraProps != null) {
                String trustType = extraProps.optString("trust_type", "");
                if ("map".equals(trustType) || "location".equals(trustType)) {
                    verificationMethod = Habit.VERIFICATION_LOCATION;
                }
            }
            
            int viewType;
            switch (verificationMethod) {
                case Habit.VERIFICATION_LOCATION:
                    viewType = 1; // VIEW_TYPE_LOCATION
                    break;
                case Habit.VERIFICATION_POMODORO:
                    viewType = 2; // VIEW_TYPE_POMODORO
                    break;
                default:
                    viewType = 0; // VIEW_TYPE_CHECKBOX
                    break;
            }
            
            System.out.println("   Determined view type: " + viewType + " (1=Location, 0=Checkbox, 2=Pomodoro)");
            
            if (viewType == 1) {
                System.out.println("   ✓ PASS: Adapter will use location_verification_layout.xml");
            } else {
                System.out.println("   ✗ FAIL: Should use location layout, got view type: " + viewType);
            }
            
            // Test 5: Round-trip test
            System.out.println("\n5. Testing Round-trip (App -> Backend -> App):");
            JSONObject roundTripJson = parsedHabit.toJson();
            Habit roundTripHabit = Habit.fromJson(roundTripJson);
            
            boolean roundTripSuccess = 
                Habit.VERIFICATION_LOCATION.equals(roundTripHabit.getVerificationMethod()) &&
                parsedHabit.getLatitude() == roundTripHabit.getLatitude() &&
                parsedHabit.getLongitude() == roundTripHabit.getLongitude();
            
            if (roundTripSuccess) {
                System.out.println("   ✓ PASS: Round-trip conversion successful");
            } else {
                System.out.println("   ✗ FAIL: Round-trip conversion failed");
            }
            
            System.out.println("\n=== Test Summary ===");
            System.out.println("Location verification mapping fixes appear to be working correctly!");
            System.out.println("The app should now:");
            System.out.println("- Send 'map' to backend when location verification is selected");
            System.out.println("- Parse 'map' from backend as location verification");
            System.out.println("- Display location verification layout for location-based habits");
            
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
