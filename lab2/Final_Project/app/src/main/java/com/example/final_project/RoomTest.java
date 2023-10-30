package com.example.final_project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import com.example.final_project.AoA;
import com.example.final_project.Room;
import java.util.ArrayList;

public class RoomTest {

    @Test
    public void testAddFreqLocation() {
        Room.freqToLocations = new ArrayList<>();

        int frequency = 1000;
        AoA.Location location = new AoA.Location();
        location.x = 1.0;
        location.y = 2.0;
        location.z = 3.0;

        Room.addFreqLocation(frequency, location);

        assertEquals(1, Room.freqToLocations.size());
        assertEquals(frequency, Room.freqToLocations.get(0).keySet().iterator().next().intValue());
        assertEquals(location, Room.freqToLocations.get(0).values().iterator().next());
    }

    @Test
    public void testGetLocationForFreq() {
        Room.freqToLocations = new ArrayList<>();

        int frequency = 2000;
        AoA.Location location = new AoA.Location();
        location.x = 4.0;
        location.y = 5.0;
        location.z = 6.0;

        Room.addFreqLocation(frequency, location);

        AoA.Location retrievedLocation = Room.getLocationForFreq(frequency);
        assertEquals(location, retrievedLocation);

        int nonExistentFrequency = 3000;
        AoA.Location nonExistentLocation = Room.getLocationForFreq(nonExistentFrequency);
        assertNull(nonExistentLocation);
    }

    @Test
    public void testGetUnits() {
        Room.units = "m";
        assertEquals("m", Room.getUnits());

        Room.units = "cm";
        assertEquals("cm", Room.getUnits());
    }

    @Test
    public void testSetUnits() {
        Room.setUnits("m");
        assertEquals("m", Room.units);

        Room.setUnits("cm");
        assertEquals("cm", Room.units);
    }
}
