package com.vlc.final_project;

import static com.vlc.final_project.AoA.Zf;
import static com.vlc.final_project.AoA.Location;

import java.util.ArrayList;
import java.util.List;

// Light includes coordinates of lights on image,
// and actual coordinates of lights, both are (x,y,z)

// todo: if need, add a function to delete location whose frequency is too small
public class Light {
    public int freq;
    public Location loc;

    public Light(int freq, Location loc){
        this.freq = freq;
        this.loc = loc;
    }

    public static class LightList{
        public ArrayList<Location> lightsOnImg = new ArrayList<>();
        // actual coordinates of lights(x,y,z)
        public ArrayList<Location> transmitters = new ArrayList<>();

        public ArrayList<Integer> txFreq = new ArrayList<>();

        public LightList(Room room, ArrayList<Integer> freq_list, ArrayList<Location> center_list){
            for (int i = 0; i < freq_list.size(); i++) {
                Location tx_Location = room.getLocationFromFreq(freq_list.get(i));
                this.txFreq.add(room.getCFreq(freq_list.get(i)));
                this.transmitters.add(tx_Location);


                Location lightOnImage = new Location(center_list.get(i).x, center_list.get(i).y, center_list.get(i).z);
                this.lightsOnImg.add(lightOnImage);
            }
        }
    }
    // coordinates of lights on image(x,y,z)

}
