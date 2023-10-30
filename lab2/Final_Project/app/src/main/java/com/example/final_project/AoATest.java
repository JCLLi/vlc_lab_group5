package com.example.final_project;

import static org.junit.Assert.*;
import org.testng.annotations.Test;
import com.example.final_project.AoA;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

public class AoATest {

    @Test
    public void testOptimizeKVals() {
        ArrayList<Double> kValsInit = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0)); // Sample initial kVals

        // Call the function to optimize kVals
        ArrayList<Double> optimizedKVals = AoA.optimizeInitValues(kValsInit, AoA::leastSquaresScalingFactors);

        // Define your expected result based on the input and actual implementation
        ArrayList<Double> expectedKVals = new ArrayList<>(); // Define your expected kVals

        // Assert that the actual result matches the expected result
        assertEquals(expectedKVals, optimizedKVals);
    }

    @Test
    public void testOptimizeRxLocation() {
        ArrayList<Double> rxLocationInit = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0)); // Sample initial rxLocation

        // Call the function to optimize rxLocation
        ArrayList<Double> optimizedRxLocation = AoA.optimizeInitValues(rxLocationInit, AoA::leastSquaresRxLocation);

        // Define your expected result based on the input and actual implementation
        ArrayList<Double> expectedRxLocation = new ArrayList<>(); // Define your expected rxLocation

        // Assert that the actual result matches the expected result
        assertEquals(expectedRxLocation, optimizedRxLocation);
    }

    @Test
    public void testComputeAoA() {
        Room.freqToLocations = new ArrayList<>();

        String units = "m";
        int[] frequencies = {1000, 2000};
        AoA.Location[] locations = new AoA.Location[2];
        locations[0] = new AoA.Location();
        locations[0].x = 10.0;
        locations[0].y = 10.0;
        locations[0].z = -10.0;
        locations[1] = new AoA.Location();
        locations[1].x = -10.0;
        locations[1].y = 10.0;
        locations[1].z = -10.0;

        ArrayList<Integer> freq_list = new ArrayList<>();
        freq_list.add(1001);
        freq_list.add(2002);
        ArrayList<int[]> center_list = new ArrayList<>();
        center_list.add(new int[]{-10, -10});
        center_list.add(new int[]{10,-10});

        AoA.freqImg = freq_list;
        AoA.lightCentersImg = center_list;

        AoA aoa = new AoA();
        AoA.Result result = aoa.computeAoA(units, frequencies, locations);

        System.out.println( result.rxLocation.get(0));

        assertEquals(0.0, result.rxLocation.get(0), 0.01);
        assertEquals(0.0, result.rxLocation.get(1), 0.01);
        assertEquals(0.0, result.rxLocation.get(2), 0.01);
        assertEquals(0.0, result.rxLocationError, 0.01);
    }
}
