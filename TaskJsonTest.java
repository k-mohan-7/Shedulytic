package com.example.shedulytic;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Quick test to verify that Task.toJson() includes both verification_type and trust_type
 */
public class TaskJsonTest {
    public static void main(String[] args) {
        try {
            // Create a location verification habit
            Task locationHabit = new Task();
            locationHabit.setType("habit");
            locationHabit.setTitle("Test Location Habit");
            locationHabit.setVerificationType("location");
            locationHabit.setLatitude(40.7128);
            locationHabit.setLongitude(-74.0060);
            
            // Test toJson method
            JSONObject json = locationHabit.toJson();
            
            System.out.println("Generated JSON:");
            System.out.println(json.toString(2));
            
            // Verify both fields are present
            if (json.has("verification_type") && json.has("trust_type")) {
                String verificationType = json.getString("verification_type");
                String trustType = json.getString("trust_type");
                
                System.out.println("\n✅ SUCCESS: Both fields are present!");
                System.out.println("verification_type: " + verificationType);
                System.out.println("trust_type: " + trustType);
                
                if (verificationType.equals(trustType)) {
                    System.out.println("✅ Both fields have the same value as expected!");
                } else {
                    System.out.println("❌ Fields have different values!");
                }
            } else {
                System.out.println("❌ FAILED: Missing required fields");
                System.out.println("Has verification_type: " + json.has("verification_type"));
                System.out.println("Has trust_type: " + json.has("trust_type"));
            }
            
        } catch (JSONException e) {
            System.out.println("❌ JSON Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
}
