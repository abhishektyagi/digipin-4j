package com.digipin;

/**
 * @author abhishek.tyagi
 *
 * DIGIPIN Encoder and Decoder Library
 * Developed by India Post, Department of Posts
 * Released under an open-source license for public use
 *
 * This class contains two main methods:
 *  - getDigiPin(lat, lon): Encodes latitude &amp;  longitude into a 10-digit alphanumeric DIGIPIN
 *  - getLatLngFromDigiPin(digiPin): Decodes a DIGIPIN back into its central latitude &amp;  longitude
 */
public class DigipinConverter {
    
    private static final char[][] DIGIPIN_GRID = {
        {'F', 'C', '9', '8'},
        {'J', '3', '2', '7'},
        {'K', '4', '5', '6'},
        {'L', 'M', 'P', 'T'}
    };
    
    private static final class Bounds {
        static final double MIN_LAT = 2.5;
        static final double MAX_LAT = 38.5;
        static final double MIN_LON = 63.5;
        static final double MAX_LON = 99.5;
    }
    
    /**
     * Encodes latitude and longitude into a DIGIPIN
     * @param lat Latitude value
     * @param lon Longitude value
     * @return 10-digit alphanumeric DIGIPIN string
     * @throws IllegalArgumentException if coordinates are out of range
     */
    public static String getDigiPin(double lat, double lon) {
        if (lat < Bounds.MIN_LAT || lat > Bounds.MAX_LAT) {
            throw new IllegalArgumentException("Latitude out of range");
        }
        if (lon < Bounds.MIN_LON || lon > Bounds.MAX_LON) {
            throw new IllegalArgumentException("Longitude out of range");
        }
        
        double minLat = Bounds.MIN_LAT;
        double maxLat = Bounds.MAX_LAT;
        double minLon = Bounds.MIN_LON;
        double maxLon = Bounds.MAX_LON;
        
        StringBuilder digiPin = new StringBuilder();
        
        for (int level = 1; level <= 10; level++) {
            double latDiv = (maxLat - minLat) / 4;
            double lonDiv = (maxLon - minLon) / 4;
            
            // REVERSED row logic (to match original)
            int row = 3 - (int) Math.floor((lat - minLat) / latDiv);
            int col = (int) Math.floor((lon - minLon) / lonDiv);
            
            row = Math.max(0, Math.min(row, 3));
            col = Math.max(0, Math.min(col, 3));
            
            digiPin.append(DIGIPIN_GRID[row][col]);
            
            if (level == 3 || level == 6) {
                digiPin.append('-');
            }
            
            // Update bounds (reverse logic for row)
            maxLat = minLat + latDiv * (4 - row);
            minLat = minLat + latDiv * (3 - row);
            
            minLon = minLon + lonDiv * col;
            maxLon = minLon + lonDiv;
        }
        
        return digiPin.toString();
    }
    
    /**
     * Decodes a DIGIPIN back to latitude and longitude coordinates
     * @param digiPin DIGIPIN string to decode
     * @return LatLng object containing latitude and longitude
     * @throws IllegalArgumentException if DIGIPIN is invalid
     */
    public static LatLng getLatLngFromDigiPin(String digiPin) {
        String pin = digiPin.replace("-", "");
        if (pin.length() != 10) {
            throw new IllegalArgumentException("Invalid DIGIPIN");
        }
        
        double minLat = Bounds.MIN_LAT;
        double maxLat = Bounds.MAX_LAT;
        double minLon = Bounds.MIN_LON;
        double maxLon = Bounds.MAX_LON;
        
        for (int i = 0; i < 10; i++) {
            char ch = pin.charAt(i);
            boolean found = false;
            int ri = -1, ci = -1;
            
            // Locate character in DIGIPIN grid
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (DIGIPIN_GRID[r][c] == ch) {
                        ri = r;
                        ci = c;
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            
            if (!found) {
                throw new IllegalArgumentException("Invalid character in DIGIPIN");
            }
            
            double latDiv = (maxLat - minLat) / 4;
            double lonDiv = (maxLon - minLon) / 4;
            
            double lat1 = maxLat - latDiv * (ri + 1);
            double lat2 = maxLat - latDiv * ri;
            double lon1 = minLon + lonDiv * ci;
            double lon2 = minLon + lonDiv * (ci + 1);
            
            // Update bounding box for next level
            minLat = lat1;
            maxLat = lat2;
            minLon = lon1;
            maxLon = lon2;
        }
        
        double centerLat = (minLat + maxLat) / 2;
        double centerLon = (minLon + maxLon) / 2;
        
        return new LatLng(
            Math.round(centerLat * 1000000.0) / 1000000.0,
            Math.round(centerLon * 1000000.0) / 1000000.0
        );
    }
    
    /**
     * Data class to hold latitude and longitude coordinates
     */
    public static class LatLng {
        private final double latitude;
        private final double longitude;
        
        public LatLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        public double getLatitude() {
            return latitude;
        }
        
        public double getLongitude() {
            return longitude;
        }
        
        @Override
        public String toString() {
            return String.format("LatLng{latitude=%.6f, longitude=%.6f}", latitude, longitude);
        }
    }
    
    // Example usage
    public static void main(String[] args) {
        try {
            // Encode coordinates to DIGIPIN
            String digiPin = getDigiPin(28.6139, 77.2090); // New Delhi coordinates
            System.out.println("DIGIPIN: " + digiPin);
            
            // Decode DIGIPIN back to coordinates
            LatLng coordinates = getLatLngFromDigiPin(digiPin);
            System.out.println("Coordinates: " + coordinates);
            
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}