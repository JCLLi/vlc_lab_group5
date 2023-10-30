package com.example.final_project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Room contains units, frequency and coordinates of actual lights
public class Room {
    // units of coordinates
    public static String units;

    // this includes frequency of actual lights and coordinates of mapped lights
    public static ArrayList<Map<Integer, AoA.Location>> freqToLocations;

    // add new map of frequency and location to list freqToLocations
    public static void addFreqLocation(int frequency, AoA.Location location) {
        Map<Integer, AoA.Location> frequencyLocationMap = new HashMap<>();
        frequencyLocationMap.put(frequency, location);
        freqToLocations.add(frequencyLocationMap);
    }

    // using frequency to get mapped location
    public static AoA.Location getLocationForFreq(int frequency) {
        for (Map<Integer, AoA.Location> freqToLocationMap : freqToLocations) {
            AoA.Location location = freqToLocationMap.get(frequency);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    public static String getUnits() {
        return units;
    }

    public static void setUnits(String unit) {
        units = unit;
    }

}