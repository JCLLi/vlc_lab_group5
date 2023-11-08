package com.vlc.final_project;

import static com.vlc.final_project.Light.LightList;
import static com.vlc.final_project.OptimizerUtil.gradientFunctionForK;
import static com.vlc.final_project.OptimizerUtil.gradientFunctionForLoc;
import static com.vlc.final_project.OptimizerUtil.mulVariationFunctionForK;
import static com.vlc.final_project.OptimizerUtil.mulVariationFunctionForLoc;
import static com.vlc.final_project.OptimizerUtil.optimizeInitValues;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.opencv.core.Point;

import java.util.ArrayList;


public class AoA {

    // todo: function to give value to these 4 things
    // the order of lightCentersImg, freqImg and freqLight should be matched

    // list of coordinates of lights' center on image (x,y)
    // this get from return of image function
    // input of fn addLightImg() in Light
    // todo: change these coordinates' center to the center of the image

    // this get from return of image function
    // frequency of lights got from image
    // input of fn matchActualFreq() in AoA
    public static double[] imgSquaredDistance; // list of square distance between lights on image
    public static double[][] transmitterPairSquaredDistance;
    public static double[][] pairwiseImageInnerProducts;
    private static ArrayList<Double> kVals;

    public static double height = 18; // 3, -4.2, 0(-14.2)
    public static double Zf = 400; // 4700 / 0.8 um = 5875

    public static class Result {
        public ArrayList<Double> rxLocation;
        public double rxLocationError;

        public Result(ArrayList<Double> value1, double value2) {
            this.rxLocation = value1;
            this.rxLocationError = value2;
        }
    }

    public static class Location {
        public double x;
        public double y;
        public double z;

        public Location(double x, double y, double z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    // calculate square distance between lights on image
    public static void computeImageSquaredDistance(LightList lightList) {
        imgSquaredDistance = new double[lightList.lightsOnImg.size()];
        for (int i = 0; i < lightList.lightsOnImg.size(); i++) {
            Location location = lightList.lightsOnImg.get(i);
            imgSquaredDistance[i] = location.x * location.x + location.y * location.y + location.z * location.z;
        }
    }

    public static void calculatePairSquaredDistance (LightList lightList) {
        int numLights = lightList.lightsOnImg.size();

        transmitterPairSquaredDistance = new double[numLights][numLights];

        for (int i = 0; i < numLights - 1; i++) {
            for (int j = i + 1; j < numLights; j++) {
                // get location of actual lights (transmitters)
                AoA.Location locationI = lightList.transmitters.get(i);
                AoA.Location locationJ = lightList.transmitters.get(j);

                // calculate distance between the actual locations of the two light sources
                double delta_x = locationI.x - locationJ.x;
                double delta_y = locationI.y - locationJ.y;
                double delta_z = locationI.z - locationJ.z;
                double sumOfSquares = delta_x * delta_x + delta_y * delta_y + delta_z * delta_z;
                transmitterPairSquaredDistance[i][j] = sumOfSquares;
            }
        }
    }

    public static void calculatePairImageInnerProducts (LightList lightList) {
        int numLights = lightList.lightsOnImg.size();

        pairwiseImageInnerProducts = new double[numLights][numLights];
        for (int i = 0; i < numLights - 1; i++) {
            for (int j = i + 1; j < numLights; j++) {
                // get location of lights on image
                AoA.Location imgLocationI = lightList.lightsOnImg.get(i);
                AoA.Location imgLocationJ = lightList.lightsOnImg.get(j);

                // Computes the inner product between lights on image
                double innerProduct = imgLocationI.x * imgLocationJ.x +
                        imgLocationI.y * imgLocationJ.y + imgLocationI.z * imgLocationJ.z;
                pairwiseImageInnerProducts[i][j] = innerProduct;
            }
        }
    }

    // explicit least-squares algorithm
    public static ArrayList<Double> leastSquaresScalingFactors(ArrayList<Double> kVals, LightList lightList) {
        ArrayList<Double> errs = new ArrayList<>();

        int numLights = lightList.lightsOnImg.size();

        for (int i = 0; i < numLights - 1; i++) {
            for (int j = i + 1; j < numLights; j++) {
                double err = kVals.get(i) * kVals.get(i) * imgSquaredDistance[i]
                        + kVals.get(j) * kVals.get(j) * imgSquaredDistance[j]
                        - 2 * kVals.get(i) * kVals.get(j) * pairwiseImageInnerProducts[i][j]
                        - transmitterPairSquaredDistance[i][j];
                errs.add(err);
            }
        }
        return errs;
    }

    public static ArrayList<Double> bruteforceLoc(LightList lightList) { //w7 * h9
        ArrayList<Double> res = new ArrayList<>();
        double min = 0;
        Point point = new Point(0, 0);
        for(double x = -4.75; x < 4.76; x += 0.1){
            for(double y = -4.0; y < 4.1; y += 0.1){
                double dist = 0;
                for (int i = 0; i < lightList.transmitters.size(); i++) {
                    dist += pow((pow(x - lightList.transmitters.get(i).x, 2)
                            + pow(y - lightList.transmitters.get(i).y, 2)
                            + pow(0 - lightList.transmitters.get(i).z, 2)
                            - kVals.get(i) * kVals.get(i) * imgSquaredDistance[i]), 2);
                }
                if(x == -4.75 && y == -4.0){
                    min = dist;
                    point.x = x;
                    point.y = y;
                }else {
                    if(dist < min){
                        min = dist;
                        point.x = x;
                        point.y = y;
                    }
                }
            }
        }
        res.add(point.x);
        res.add(point.y);
        res.add(0.0);
        return res;
    }

    public static ArrayList<Double> leastSquaresRxLocation(ArrayList<Double> rxLocation, LightList lightList) { //w7 * h9
        ArrayList<Double> res = new ArrayList<>();
        for (int i = 0; i < lightList.transmitters.size(); i++) {
            double dist = pow(rxLocation.get(0) - lightList.transmitters.get(i).x, 2)
                    + pow(rxLocation.get(1) - lightList.transmitters.get(i).y, 2)
                    + pow(rxLocation.get(2) - lightList.transmitters.get(i).z, 2)
                    - kVals.get(i) * kVals.get(i) * imgSquaredDistance[i];
            res.add(dist);
        }
        return res;
    }

    public static ArrayList<Double> k_init(ArrayList<Integer> freq_list){
        double k = height / Zf;
        ArrayList<Double> ks = new ArrayList<>();
        for(int freq: freq_list) {
            ks.add(k);
        }
        return ks;
    }
    public static ArrayList<Double> initialPositionGuess(LightList lightList) {
        ArrayList<Double> guess = new ArrayList<>();

        // Calculate the mean of x, y, and z coordinates
        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;

        for (Location transmitter : lightList.transmitters) {
            sumX += transmitter.x;
            sumY += transmitter.y;
        }
        guess.add(sumX / lightList.transmitters.size());
        guess.add(0.0);
        guess.add(sumZ);

        return guess;
    }

    public static double computeError(ArrayList<Double> rxLocation, LightList lightList) {
        double rxLocationError = 0;
        for (int i = 0; i < lightList.transmitters.size(); i++) {
            double distance1 = Math.sqrt(pow(rxLocation.get(0) - lightList.transmitters.get(i).x, 2)
                    + pow(rxLocation.get(1) - lightList.transmitters.get(i).y, 2)
                    + pow(rxLocation.get(2) - lightList.transmitters.get(i).z, 2));
            double distance2 = Math.sqrt(kVals.get(i)* kVals.get(i) * imgSquaredDistance[i]);
            rxLocationError += abs(distance1 - distance2);
        }
        return rxLocationError;
    }

    public Result computeAoA(ArrayList<Integer> freq_list, ArrayList<Location> center_list) {

        // set variables for Room
        Room room = new Room("m");
        room.setTx();

        LightList lightList = new LightList(room, freq_list, center_list);

        computeImageSquaredDistance(lightList); // Calculate square distance between two lights on image

        calculatePairSquaredDistance(lightList); // Calculate public transmitterPairSquaredDistance, which will be used later

        calculatePairImageInnerProducts(lightList);  // Calculate public pairwiseImageInnerProducts, which will be used later

        ArrayList<Double> kValsInit = k_init(freq_list); // Initialize kValues
        MultivariateVectorFunction mvvfK = gradientFunctionForK(kValsInit.size()); // Create gradient function for kVals
        MultivariateFunction mvfK = mulVariationFunctionForK(lightList); // Create multiple variate function for KVals
        kVals = optimizeInitValues(kValsInit, mvvfK, mvfK); // Optimize kVals
//        kVals = new ArrayList<>();
//        for(int i = 0; i < kValsInit.size(); i++){
//            kVals.add(kValsInit.get(i));
//        }


        ArrayList<Double> rxLocationInit = initialPositionGuess(lightList); // Initialize rx location
        MultivariateVectorFunction mvvfloc = gradientFunctionForLoc(freq_list.size(), kVals, lightList); // Create gradient function for rx location
        MultivariateFunction mvfloc = mulVariationFunctionForLoc(lightList); // Create multiple variate function for rx location
        ArrayList<Double> rxLocation = optimizeInitValues(rxLocationInit, mvvfloc, mvfloc); // Optimize rx location

//        ArrayList<Double> rxLocation = bruteforceLoc(lightList);
        double error = computeError(rxLocation, lightList);

        return new Result(rxLocation, error);
    }
}
