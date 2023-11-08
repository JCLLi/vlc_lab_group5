package com.vlc.final_project;
import static com.vlc.final_project.AoA.Location;
import static com.vlc.final_project.AoA.height;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.pow;

import java.util.ArrayList;

public class BruteForce {
    public double max_x;
    public double max_y;

    public BruteForce(double max_x, double max_y){
        this.max_x = max_x;
        this.max_y = max_y;
    }

    public Location getLoc(ArrayList<Location> center_list, ArrayList<Integer> freq_list){
        Room room = new Room("m");
        room.setTx();
        ArrayList<Location> order = new ArrayList<>();
        ArrayList<Integer> ordered_freq = new ArrayList<>();
        ArrayList<Integer> classified_freq = new ArrayList<>();

        int[] preset_freq = {3704, 5000, 6667, 10000};
        for(int freq: freq_list){
            classified_freq.add(room.getCFreq(freq));
        }

        for(int j = 0; j < 4; j++){
            for(int i = 0; i < center_list.size(); i++){
                if(classified_freq.get(i) == preset_freq[j]){
                    ordered_freq.add(classified_freq.get(i));
                    order.add(center_list.get(i));
                    break;
                }
            }
        }


//        double min = 0;
//        int index = 0;
        int first_point = 0;
        int second_point = 0;
//        for(int j = 0; j < 4; j++){
//            for(int i = 0; i < center_list.size(); i++){
//                if (!order.contains(center_list.get(i))){
//                    min = pow(center_list.get(i).x + 1500, 2) + pow(-center_list.get(i).y + 2000, 2);
//                    index = i;
//                    break;
//                }
//            }
//
//            for(int i = 0; i < center_list.size(); i++){
//                if(!order.contains(center_list.get(i))){
//                    double temp = pow(center_list.get(i).x + 1500, 2) + pow(-center_list.get(i).y + 2000, 2);
//                    if(temp < min){
//                        index = i;
//                        min = temp;
//                    }
//                }
//            }
//            order.add(center_list.get(index));
//            ordered_freq.add(room.getCFreq(freq_list.get(index)));
//        }

        double x1 = (order.get(1).x + order.get(0).x) / 2;
        double x2 = (order.get(3).x + order.get(2).x) / 2;
        double y1 = (order.get(0).y + order.get(2).y) / 2;
        double y2 = (order.get(1).y + order.get(3).y) / 2;
        double average_x = (x1 + x2) / 2;
        double average_y = (y1 + y2) / 2;

        double[] A = {-average_x + (order.get(0).x + order.get(1).x) / 2, -average_y + (order.get(0).y + order.get(1).y) / 2};
        double[] B = {0, abs(-average_y + (order.get(0).y + order.get(1).y)) / 2};
        double angleRadians = Math.atan2(B[0], B[1]) - Math.atan2(A[0], A[1]);
        double angleDegrees = Math.toDegrees(angleRadians);


        double average_width = ((order.get(1).x - order.get(0).x) + (order.get(3).x - order.get(2).x)) / 2;
        double average_length = ((order.get(0).y - order.get(2).y) + (order.get(1).y - order.get(3).y)) / 2;
        return new Location(-max_x * average_x / (1500.0 - average_width / 2), -max_y * average_y / (2000 - average_length / 2), height);
    }

    public static double angle(double[] vectorA, double[] vectorB) {
        double dotProduct = dotProduct(vectorA, vectorB);

        // Calculate the magnitudes of the vectors
        double magnitudeA = magnitude(vectorA);
        double magnitudeB = magnitude(vectorB);

        // Calculate the angle in radians
        double angleRadians = Math.acos(dotProduct / (magnitudeA * magnitudeB));

        // Convert the angle to degrees if needed
        return Math.toDegrees(angleRadians);
    }

    public static double dotProduct(double[] vectorA, double[] vectorB) {
        double result = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            result += vectorA[i] * vectorB[i];
        }
        return result;
    }

    public static double magnitude(double[] vector) {
        double result = 0.0;
        for (double component : vector) {
            result += component * component;
        }
        return Math.sqrt(result);
    }
}


