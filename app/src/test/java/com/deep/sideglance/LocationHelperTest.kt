package com.deep.sideglance

import com.deep.sideglance.util.LocationHelper
import org.junit.Assert.*
import org.junit.Test

class LocationHelperTest {

    @Test
    fun testHaversineDistanceCalculation() {
        // San Francisco (37.7749, -122.4194) to New York (40.7128, -74.0060)
        // Distance is roughly 4129 km
        val distance = LocationHelper.calculateDistanceKm(
            37.7749, -122.4194,
            40.7128, -74.0060
        )
        
        // Assert distance is correct within 1% variance
        assertEquals(4129.0, distance, 50.0)
    }

    @Test
    fun testFormatDistanceCases() {
        // Case 1: Permission denied
        val resultDenied = LocationHelper.formatDistance(
            37.7749, -122.4194,
            40.7128, -74.0060,
            hasPermission = false
        )
        assertEquals("infinity", resultDenied)

        // Case 2: Missing coordinate
        val resultNullCoords = LocationHelper.formatDistance(
            null, null,
            40.7128, -74.0060,
            hasPermission = true
        )
        assertEquals("infinity", resultNullCoords)

        // Case 3: Close distance (less than 1km)
        // e.g. same latitude, very small longitude diff (approx 50 meters)
        val resultClose = LocationHelper.formatDistance(
            37.7749, -122.4194,
            37.7749, -122.4199,
            hasPermission = true
        )
        assertTrue(resultClose.endsWith(" m away"))

        // Case 4: Normal distance
        val resultNormal = LocationHelper.formatDistance(
            37.7749, -122.4194,
            37.7249, -122.4194,
            hasPermission = true
        )
        // approx 5.5 km
        assertTrue(resultNormal.contains("km away"))
    }
}
