package com.vlc.final_project;

import static java.lang.Math.abs;
import static com.vlc.final_project.AoA.Location;
import static com.vlc.final_project.AoA.height;

import java.util.ArrayList;


// Room contains units, frequency and coordinates of actual lights
public class Room {
    // units of coordinates
    public String unit;
    public ArrayList<Light> tx_list;

    public Room(String unit){
        this.unit = unit;
        this.tx_list = new ArrayList<>();
    }

    // using frequency to get mapped location
    public AoA.Location getLocationFromFreq(int frequency) {
        int abs = abs(frequency - this.tx_list.get(0).freq);
        int index = 0;
        for(int i = 1; i < this.tx_list.size(); i++){
            int temp = abs(frequency - this.tx_list.get(i).freq);
            if(abs > temp){
                abs = temp;
                index = i;
            }
        }
        return this.tx_list.get(index).loc;
    }

    public int getCFreq(int frequency) {
        int abs = abs(frequency - this.tx_list.get(0).freq);
        int index = 0;
        for(int i = 1; i < this.tx_list.size(); i++){
            int temp = abs(frequency - this.tx_list.get(i).freq);
            if(abs > temp){
                abs = temp;
                index = i;
            }
        }
        return this.tx_list.get(index).freq;
    }

    public void setTx(){
        int[] freq = {3704, 5000, 6667, 10000};
//5.3
        //12.3
        tx_list.add(new Light(freq[0], new Location(-2.65, 6.15, -height)));
        tx_list.add(new Light(freq[1], new Location(2.65, 6.15, -height)));
        tx_list.add(new Light(freq[2], new Location(-2.65, -6.15, -height)));
        tx_list.add(new Light(freq[3], new Location(2.66, -6.15, -height)));
    }
}
