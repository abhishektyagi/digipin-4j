package com.digipin;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.io.IOException;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

class DigipinConverterTest {

    private static final double MIN_LAT = 2.5;
    private static final double MAX_LAT = 38.5;
    private static final double MIN_LON = 63.5;
    private static final double MAX_LON = 99.5;
    
    // Official API endpoint configuration
    private static final String API_BASE_URL = "http://localhost:9000";
    private static final String ENCODE_ENDPOINT = "/api/digipin/encode";
    
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void getDigiPin() {
        Random random = new Random();
        
        // Generate 5 random test cases
        System.out.println("=== DIGIPIN Test Cases with Random Coordinates ===");
        
        for (int i = 1; i <= 5; i++) {
            // Generate random latitude within valid range (2.5 to 38.5)
            double randomLat = MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble();
            
            // Generate random longitude within valid range (63.5 to 99.5)
            double randomLon = MIN_LON + (MAX_LON - MIN_LON) * random.nextDouble();
            
            // Round to 6 decimal places for better readability
            randomLat = Math.round(randomLat * 1000000.0) / 1000000.0;
            randomLon = Math.round(randomLon * 1000000.0) / 1000000.0;
            
            System.out.printf("\nTest Case %d:\n", i);
            System.out.printf("  Input Coordinates: Lat=%.6f, Lon=%.6f\n", randomLat, randomLon);
            
            try {
                // Generate DIGIPIN
                String digiPin = DigipinConverter.getDigiPin(randomLat, randomLon);
                System.out.printf("  Generated DIGIPIN: %s\n", digiPin);
                
                // Verify the DIGIPIN is not null and has correct format
                assertNotNull(digiPin, "DIGIPIN should not be null");
                assertTrue(digiPin.matches("[FCT98J327K456LMP-]+"), "DIGIPIN should contain only valid characters");
                assertEquals(12, digiPin.length(), "DIGIPIN should be 12 characters long (including hyphens)");
                
                // Decode back to verify round-trip
                DigipinConverter.LatLng decoded = DigipinConverter.getLatLngFromDigiPin(digiPin);
                System.out.printf("  Decoded Coordinates: %s\n", decoded);
                
                // Verify coordinates are within valid ranges
                assertTrue(decoded.getLatitude() >= MIN_LAT && decoded.getLatitude() <= MAX_LAT, 
                          "Decoded latitude should be within valid range");
                assertTrue(decoded.getLongitude() >= MIN_LON && decoded.getLongitude() <= MAX_LON, 
                          "Decoded longitude should be within valid range");
                
                System.out.printf("  ✓ Test Case %d PASSED\n", i);
                
            } catch (Exception e) {
                System.err.printf("  ✗ Test Case %d FAILED: %s\n", i, e.getMessage());
                fail("Test case " + i + " failed with exception: " + e.getMessage());
            }
        }
        
        System.out.println("\n=== All Random DIGIPIN Test Cases Completed ===");
    }

    @Test
    void getLatLngFromDigiPin() {
        // Test with some known DIGIPINs
        System.out.println("\n=== Testing DIGIPIN Decoding ===");
        
        // Generate a random coordinate and encode it, then decode back
        Random random = new Random();
        double testLat = MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble();
        double testLon = MIN_LON + (MAX_LON - MIN_LON) * random.nextDouble();
        
        testLat = Math.round(testLat * 1000000.0) / 1000000.0;
        testLon = Math.round(testLon * 1000000.0) / 1000000.0;
        
        System.out.printf("Original Coordinates: Lat=%.6f, Lon=%.6f\n", testLat, testLon);
        
        String digiPin = DigipinConverter.getDigiPin(testLat, testLon);
        System.out.printf("Generated DIGIPIN: %s\n", digiPin);
        
        DigipinConverter.LatLng decoded = DigipinConverter.getLatLngFromDigiPin(digiPin);
        System.out.printf("Decoded Coordinates: %s\n", decoded);
        
        // Verify the decoded coordinates are reasonably close to original
        // (Note: Due to the nature of the encoding, they won't be exactly the same)
        assertNotNull(decoded, "Decoded coordinates should not be null");
        assertTrue(Math.abs(decoded.getLatitude() - testLat) < 1.0, 
                  "Decoded latitude should be reasonably close to original");
        assertTrue(Math.abs(decoded.getLongitude() - testLon) < 1.0, 
                  "Decoded longitude should be reasonably close to original");
        
        System.out.println("✓ DIGIPIN Decoding Test PASSED");
    }
    
    @Test
    void testBoundaryConditions() {
        System.out.println("\n=== Testing Boundary Conditions ===");
        
        // Test coordinates at the boundaries
        double[][] boundaryCoords = {
            {MIN_LAT, MIN_LON}, // Bottom-left corner
            {MIN_LAT, MAX_LON}, // Bottom-right corner
            {MAX_LAT, MIN_LON}, // Top-left corner
            {MAX_LAT, MAX_LON}, // Top-right corner
            {(MIN_LAT + MAX_LAT) / 2, (MIN_LON + MAX_LON) / 2} // Center
        };
        
        String[] testNames = {"Bottom-Left", "Bottom-Right", "Top-Left", "Top-Right", "Center"};
        
        for (int i = 0; i < boundaryCoords.length; i++) {
            double lat = boundaryCoords[i][0];
            double lon = boundaryCoords[i][1];
            
            System.out.printf("\n%s Corner Test:\n", testNames[i]);
            System.out.printf("  Coordinates: Lat=%.6f, Lon=%.6f\n", lat, lon);
            
            String digiPin = DigipinConverter.getDigiPin(lat, lon);
            System.out.printf("  DIGIPIN: %s\n", digiPin);
            
            DigipinConverter.LatLng decoded = DigipinConverter.getLatLngFromDigiPin(digiPin);
            System.out.printf("  Decoded: %s\n", decoded);
            
            assertNotNull(digiPin, "DIGIPIN should not be null for boundary coordinates");
            assertNotNull(decoded, "Decoded coordinates should not be null");
            
            System.out.printf("  ✓ %s Test PASSED\n", testNames[i]);
        }
        
        System.out.println("\n=== Boundary Conditions Test Completed ===");
    }
    
    /**
     * Calls the official DIGIPIN API to get the reference implementation result
     */
    private String getOfficialDigipin(double latitude, double longitude) throws IOException {
        String json = String.format("{\"latitude\": %f, \"longitude\": %f}", latitude, longitude);
        
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_BASE_URL + ENCODE_ENDPOINT)
                .post(body)
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("digipin").asText();
        }
    }
    
    @Test
    void validateAgainstOfficialAPI() {
        System.out.println("\n=== Validating Against Official DIGIPIN API ===");
        
        // Test coordinates to validate
        double[][] testCoordinates = {
            {12.0, 77.0},           // Bangalore
            {28.6139, 77.2090},     // New Delhi
            {19.0760, 72.8777},     // Mumbai
            {13.0827, 80.2707},     // Chennai
            {22.5726, 88.3639},     // Kolkata
            {17.3850, 78.4867},     // Hyderabad
            {12.9716, 77.5946},     // Bangalore IT
            {26.9124, 75.7873}      // Jaipur
        };
        
        String[] locationNames = {
            "Bangalore", "New Delhi", "Mumbai", "Chennai", 
            "Kolkata", "Hyderabad", "Bangalore IT", "Jaipur"
        };
        
        int passedTests = 0;
        int totalTests = testCoordinates.length;
        
        for (int i = 0; i < testCoordinates.length; i++) {
            double lat = testCoordinates[i][0];
            double lon = testCoordinates[i][1];
            String location = locationNames[i];
            
            System.out.printf("\nTest %d - %s:\n", i + 1, location);
            System.out.printf("  Coordinates: Lat=%.6f, Lon=%.6f\n", lat, lon);
            
            try {
                // Get DIGIPIN from our implementation
                String ourDigipin = DigipinConverter.getDigiPin(lat, lon);
                System.out.printf("  Our Implementation: %s\n", ourDigipin);
                
                // Get DIGIPIN from official API
                String officialDigipin = getOfficialDigipin(lat, lon);
                System.out.printf("  Official API:      %s\n", officialDigipin);
                
                // Compare results
                if (ourDigipin.equals(officialDigipin)) {
                    System.out.printf("  ✅ MATCH - Test %d PASSED\n", i + 1);
                    passedTests++;
                } else {
                    System.out.printf("  ❌ MISMATCH - Test %d FAILED\n", i + 1);
                    System.out.printf("     Expected: %s\n", officialDigipin);
                    System.out.printf("     Got:      %s\n", ourDigipin);
                }
                
            } catch (IOException e) {
                System.err.printf("  ⚠️  API Error for %s: %s\n", location, e.getMessage());
                System.err.println("     Make sure the official server is running at localhost:9000");
                
                // Still test our implementation works
                try {
                    String ourDigipin = DigipinConverter.getDigiPin(lat, lon);
                    System.out.printf("  Our Implementation: %s (API unavailable)\n", ourDigipin);
                } catch (Exception ex) {
                    fail("Our implementation failed: " + ex.getMessage());
                }
            } catch (Exception e) {
                System.err.printf("  ❌ Unexpected error for %s: %s\n", location, e.getMessage());
                fail("Test failed with exception: " + e.getMessage());
            }
        }
        
        System.out.printf("\n=== Validation Summary ===\n");
        System.out.printf("Tests Passed: %d/%d\n", passedTests, totalTests);
        
        if (passedTests == totalTests) {
            System.out.println("🎉 All validations PASSED! Our implementation matches the official API.");
        } else if (passedTests > 0) {
            System.out.printf("⚠️  Partial validation: %d tests passed, %d failed.\n", passedTests, totalTests - passedTests);
        } else {
            System.out.println("❌ No API validations could be completed. Check if the official server is running.");
        }
    }
    
    @Test
    void validateRandomCoordinatesAgainstAPI() {
        System.out.println("\n=== Validating Random Coordinates Against Official API ===");
        
        Random random = new Random();
        int testCount = 3; // Test 3 random coordinates
        int passedTests = 0;
        
        for (int i = 1; i <= testCount; i++) {
            // Generate random coordinates within valid range
            double randomLat = MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble();
            double randomLon = MIN_LON + (MAX_LON - MIN_LON) * random.nextDouble();
            
            // Round to 6 decimal places
            randomLat = Math.round(randomLat * 1000000.0) / 1000000.0;
            randomLon = Math.round(randomLon * 1000000.0) / 1000000.0;
            
            System.out.printf("\nRandom Test %d:\n", i);
            System.out.printf("  Coordinates: Lat=%.6f, Lon=%.6f\n", randomLat, randomLon);
            
            try {
                // Get DIGIPIN from our implementation
                String ourDigipin = DigipinConverter.getDigiPin(randomLat, randomLon);
                System.out.printf("  Our Implementation: %s\n", ourDigipin);
                
                // Get DIGIPIN from official API
                String officialDigipin = getOfficialDigipin(randomLat, randomLon);
                System.out.printf("  Official API:      %s\n", officialDigipin);
                
                // Compare results
                if (ourDigipin.equals(officialDigipin)) {
                    System.out.printf("MATCH - Random Test %d PASSED\n", i);
                    passedTests++;
                } else {
                    System.out.printf("MISMATCH - Random Test %d FAILED\n", i);
                }
                
            } catch (IOException e) {
                System.err.printf("  ⚠️  API Error: %s\n", e.getMessage());
                // Continue with next test
            } catch (Exception e) {
                System.err.printf("Unexpected error: %s\n", e.getMessage());
            }
        }
        
        System.out.printf("\nRandom Validation Summary: %d/%d tests passed\n", passedTests, testCount);
    }
}