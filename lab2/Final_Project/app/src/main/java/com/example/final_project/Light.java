package com.example.final_project;

import static com.example.final_project.AoA.Zf;

import java.util.ArrayList;
import java.util.List;

// Light includes coordinates of lights on image,
// and actual coordinates of lights, both are (x,y,z)

// todo: if need, add a function to delete location whose frequency is too small
public class Light {
    // coordinates of lights on image(x,y,z)
    public static ArrayList<AoA.Location> lightsImg = new ArrayList<AoA.Location>();


    // actual coordinates of lights(x,y,z)
    public static ArrayList<AoA.Location> transmitters = new ArrayList<AoA.Location>();

    //
    public static void addLight () {

        AoA.Location location = new AoA.Location();

        for (int i = 0; i < AoA.lightCentersImg.size() - 1; i++) {

            AoA.Location actualLocation = Room.getLocationForFreq(AoA.freqRoundImg.get(i));

            if (actualLocation != null){
                // add actual light's location to transmitters list
                transmitters.add(location);

                // add light's location on image to lightsImg list
                location.x = AoA.lightCentersImg.get(i)[0];
                location.y = AoA.lightCentersImg.get(i)[1];
                location.z = Zf;
                lightsImg.add(location);
            }
        }
    }
}