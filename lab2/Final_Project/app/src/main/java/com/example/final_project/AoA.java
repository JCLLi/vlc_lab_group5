package com.example.final_project;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;

import java.util.ArrayList;
import java.util.function.Function;

public class AoA {

    // todo: function to give value to these 4 things
    // the order of lightCentersImg, freqImg and freqLight should be matched

    // list of coordinates of lights' center on image (x,y)
    // this get from return of image function
    // input of fn addLightImg() in Light
    // todo: change these coordinates' center to the center of the image
    public static ArrayList<int[]> lightCentersImg = new ArrayList<int[]>();

    // this get from return of image function
    // frequency of lights got from image
    // input of fn matchActualFreq() in AoA
    public static ArrayList<Integer> freqImg = new ArrayList<Integer>();

    // frequency of light got from image after rounded
    // input of fn addLightsActual() in Light
    public static ArrayList<Integer> freqRoundImg = new ArrayList<Integer>();

    // list of square distance between lights on image
    public static double[] imgSquaredDistance;

    public static double[][] transmitterPairSquaredDistance;
    public static double[][] pairwiseImageInnerProducts;

    public static ArrayList<Double> kVals;

    public static int Zf = 10;

    public static int minFreqDiff = 1000;

    public class Result {
        public ArrayList<Double> rxLocation;
        public double rxLocationError;

        public Result(ArrayList<Double> value1, double value2) {
            this.rxLocation = value1;
            this.rxLocationError = value2;
        }
    }

    public static class Location {
        double x;
        double y;
        double z;
    }

    public static int roundToNearest(int value, int nearest) {
        return Math.round((float) value / nearest) * nearest;
    }

    // match frequency got from image to actual frequency of lights
    public static void matchActualFreq () {
        for (Integer frequency : freqImg) {
            int roundedFrequency = roundToNearest(frequency, AoA.minFreqDiff);
            // add to freqLight list
            freqRoundImg.add(roundedFrequency);
        }
    }

    // calculate square distance between lights on image
    public static void computeImageSquaredDistance() {
        imgSquaredDistance = new double[Light.lightsImg.size()];
        for (int i = 0; i < Light.lightsImg.size(); i++) {
            Location location = Light.lightsImg.get(i);
            imgSquaredDistance[i] = location.x * location.x + location.y * location.y + location.z * location.z;
        }
    }

    public static double getZOffsetGuess() {
        double DEFAULT_OFFSET = 2.5;

        if (Room.getUnits().equals("m")) {
            return DEFAULT_OFFSET;
        } else if (Room.getUnits().equals("cm")) {
            return DEFAULT_OFFSET * 100;
        } else if (Room.getUnits().equals("in")) {
            return (DEFAULT_OFFSET * 100) / 2.54;
        } else {
            throw new UnsupportedOperationException("Unknown unit: " + Room.getUnits());
        }
    }

    public static void calculatePairSquaredDistance () {
        int numLights = Light.lightsImg.size();

        transmitterPairSquaredDistance = new double[numLights][numLights];

        for (int i = 0; i < numLights - 1; i++) {
            for (int j = i + 1; j < numLights; j++) {
                // get location of actual lights (transmitters)
                AoA.Location locationI = Light.transmitters.get(i);
                AoA.Location locationJ = Light.transmitters.get(j);

                // calculate distance between the actual locations of the two light sources
                double delta_x = locationI.x - locationJ.x;
                double delta_y = locationI.y - locationJ.y;
                double delta_z = locationI.z - locationJ.z;
                double sumOfSquares = delta_x * delta_x + delta_y * delta_y + delta_z * delta_z;
                transmitterPairSquaredDistance[i][j] = sumOfSquares;
            }
        }
    }

    public static void calculatePairImageInnerProducts () {
        int numLights = Light.lightsImg.size();

        pairwiseImageInnerProducts = new double[numLights][numLights];
        for (int i = 0; i < numLights - 1; i++) {
            for (int j = i + 1; j < numLights; j++) {
                // get location of lights on image
                AoA.Location imgLocationI = Light.lightsImg.get(i);
                AoA.Location imgLocationJ = Light.lightsImg.get(j);

                // Computes the inner product between lights on image
                double innerProduct = imgLocationI.x * imgLocationJ.x +
                        imgLocationI.y * imgLocationJ.y + imgLocationI.z * imgLocationJ.z;
                pairwiseImageInnerProducts[i][j] = innerProduct;
            }
        }

    }

    // explicit least-squares algorithm
    public static ArrayList<Double> leastSquaresScalingFactors(ArrayList<Double> kVals) {
        ArrayList<Double> errs = new ArrayList<Double>();

        int numLights = Light.lightsImg.size();

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

    // calculate sum error
//    public double scalarScaling(ArrayList<Double> kVals) {
//        ArrayList<Double> errs = leastSquaresScalingFactors(kVals);
//        double sum = 0.0;
//        for (double err : errs) {
//            sum += err;
//        }
//        return sum;
//    }

    private static double[] calculateRoots(int i, double k) {
        double[] roots = new double[2];
        double a = imgSquaredDistance[i];
        double b = -2 * k * pairwiseImageInnerProducts[0][i];
        double c = k * k * imgSquaredDistance[0] - transmitterPairSquaredDistance[0][i];
        double discriminant = b * b - 4 * a * c;
        if (discriminant >= 0) {
            roots[0] = (-b + Math.sqrt(discriminant)) / (2 * a);
            roots[1] = (-b - Math.sqrt(discriminant)) / (2 * a);
        }
        return roots;
    }

    private static double min(ArrayList<Double> List) {
        double min = Double.MAX_VALUE;
        for (double ele : List) {
            if (ele < min) {
                min = ele;
            }
        }
        return min;
    }

//    private boolean isReal(double[] roots) {
//        return roots.length == 2 && roots[0] >= 0 && roots[1] >= 0;
//    }
    public static boolean isReal(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    // Generate a subset for guessing scaling factors (solGuess)
    // varCnt?
    private static ArrayList<Double> solGuessSubset(int index, int varCnt, ArrayList<double[]> solGuess) {
        ArrayList<Double> solGuessSub = new ArrayList<>();
        solGuessSub.add(solGuess.get(0)[0]);
        for (int i = 1; i < Light.transmitters.size(); i++) {
            if (solGuess.get(i)[1] < 0) {
                int newIndex = (int) (index % Math.pow(2, varCnt) / Math.pow(2, varCnt - 1));
                solGuessSub.add(solGuess.get(i)[newIndex]);
                varCnt--;
            } else {
                solGuessSub.add(solGuess.get(i)[0]);
            }
        }
        return solGuessSub;
    }

    public static ArrayList<Double> bruteForceK() {
        int numberOfIterations = 500;

        double[] k0Vals = new double[numberOfIterations];
        double start = -0.1;
        double stop = -0.01;
        for (int j = 0; j < numberOfIterations; j++) {
            double value = start + (stop - start) * j / (numberOfIterations - 1);
            k0Vals[j] = value;
        }

        ArrayList<Double> errHistory = new ArrayList<Double>();
        ArrayList<Integer> idxHistory = new ArrayList<Integer>();
        ArrayList<Double> kVals = new ArrayList<Double>();
        // loop for iteration
        for (int j = 0; j <= numberOfIterations; j++) {
            double k0Val;
            if (j == numberOfIterations) {
                // Last iteration, find minimum
                int minErrorHistoryIdx = errHistory.indexOf(min(errHistory));
                int minIdx = idxHistory.get(minErrorHistoryIdx);
//                System.out.println("Using index " + minIdx + " for initial guess");
                k0Val = k0Vals[minIdx];
            } else {
                k0Val = k0Vals[j];
            }

            ArrayList<double[]> solGuess = new ArrayList<>();
            solGuess.add(new double[]{k0Val, 0.0});
            boolean solFound = true;
            int multipleSol = 0;

            for (int i = 1; i < Light.transmitters.size(); i++) {
                double[] sol = calculateRoots(i, solGuess.get(0)[0]);
                if (isReal(sol[0])) {
                    if (sol[0] < 0 && sol[1] < 0) {
                        solGuess.add(sol);
                        multipleSol++;
                    } else if (sol[0] < 0) {
                        solGuess.add(new double[]{sol[0], 0.0});
                    } else if (sol[1] < 0) {
                        solGuess.add(new double[]{sol[1], 0.0});
                    } else {
                        solFound = false;
                        break;
                    }
                } else {
                    solFound = false;
                    break;
                }
            }

            if (solFound) {
                ArrayList<Double> scalingFactorsErrorCombination = new ArrayList<>();

                for (int m = 1; m <= Math.pow(2, multipleSol); m++) {
                    ArrayList<Double> solGuessCombination = solGuessSubset(m, multipleSol, solGuess);
                    ArrayList<Double> scalingFactorsErrorArr = leastSquaresScalingFactors(solGuessCombination);
                    double scalingFactorsError = 0.0;
                    for (double n : scalingFactorsErrorArr) {
                        scalingFactorsError += Math.pow(n, 2);
                    }
                    scalingFactorsErrorCombination.add(scalingFactorsError);
                }

                kVals = solGuessSubset(scalingFactorsErrorCombination.indexOf(min(scalingFactorsErrorCombination)) + 1, multipleSol, solGuess);
                if (errHistory.isEmpty()) {
                    System.out.println("First found index: " + j);
                }
                errHistory.add(min(scalingFactorsErrorCombination));
                idxHistory.add(j);
            }
        }

        return kVals;
    }

//    public ArrayList<Double> calculateKValuesActual(double[] actualLocation) {
//        ArrayList<Double> kValsActual = new ArrayList<Double>();
//
//        if (actualLocation != null) {
//            for (int i = 0; i < Light.lightsImg.size(); i++) {
//                double tSum = Math.pow(actualLocation[0] - Light.transmitters.get(i).x, 2) +
//                        Math.pow(actualLocation[1] - Light.transmitters.get(i).y, 2) +
//                        Math.pow(actualLocation[2] - Light.transmitters.get(i).z, 2);
//                double pxSum = imgSquaredDistance[i];
//                double kSquared = tSum / pxSum;
//                kValsActual.add(Math.sqrt(kSquared));
//            }
//        }
//        return kValsActual;
//    }

    private static double[] toDoubleArray(ArrayList<Double> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private static ArrayList<Double> toDoubleList(double[] array) {
        ArrayList<Double> list = new ArrayList<Double>();
        for (double value : array) {
            list.add(value);
        }
        return list;
    }

    // optimize K Values got from fn bruteForceK()
    // inputs are array of initialized values and one function
    public static ArrayList<Double> optimizeInitValues(ArrayList<Double> initValues, Function<ArrayList<Double>, ArrayList<Double>> scalingFactorsFunction) {

        int numLights = initValues.size();

        // Create the target values (errs) for the least squares optimization
        ArrayList<Double> errs = scalingFactorsFunction.apply(initValues);

        // Convert errs to an array
        double[] target = new double[errs.size()];
        for (int i = 0; i < errs.size(); i++) {
            target[i] = errs.get(i);
        }

        // Define the model for the least squares optimization
        MultivariateFunction model = new MultivariateFunction() {
            @Override
            public double value(double[] point) {
                // Calculate the squared error for each value in point
                ArrayList<Double> predicted = scalingFactorsFunction.apply(toDoubleList(point));
                double sumSquaredErrors = 0.0;
                for (int i = 0; i < numLights * (numLights - 1) / 2; i++) {
                    double error = predicted.get(i) - target[i];
                    sumSquaredErrors += error * error;
                }
                return sumSquaredErrors;
            }
        };

        // Create the optimizer
        NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
                NonLinearConjugateGradientOptimizer.Formula.FLETCHER_REEVES, new SimpleValueChecker(1e-6, 1e-6));

        // Define the optimization problem
        PointValuePair optimum = optimizer.optimize(
                new MaxEval(1000), // Maximum number of evaluations
                GoalType.MINIMIZE,
                new InitialGuess(toDoubleArray(initValues)), // Initial guess
                new ObjectiveFunction(model),
                new SearchInterval(0, 10) // Appropriate search interval for kVals
        );

        // Convert the result to ArrayList
        double[] result = optimum.getPoint();
        ArrayList<Double> optimizedKVals = toDoubleList(result);

        return optimizedKVals;
    }


    public static ArrayList<Double> initialPositionGuess() {
        ArrayList<Double> guess = new ArrayList<>();

        // Calculate the mean of x, y, and z coordinates
        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;
        double offset = getZOffsetGuess();

        for (Location transmitter : Light.transmitters) {
            sumX += transmitter.x;
            sumY += transmitter.y;
            sumZ += transmitter.z;
        }
        guess.add(sumX / Light.transmitters.size());
        guess.add(sumY / Light.transmitters.size());
        guess.add(sumZ / Light.transmitters.size() - offset);

        return guess;
    }


    public static ArrayList<Double> leastSquaresRxLocation(ArrayList<Double> rxLocation) {
        ArrayList<Double> dists = new ArrayList<Double>();

        for (int i = 0; i < Light.transmitters.size(); i++) {
            double dist = Math.pow(rxLocation.get(0) - Light.transmitters.get(i).x, 2)
                    + Math.pow(rxLocation.get(1) - Light.transmitters.get(i).y, 2)
                    + Math.pow(rxLocation.get(2) - Light.transmitters.get(i).z, 2)
                    - kVals.get(i) * kVals.get(i) * imgSquaredDistance[i];
            dists.add(dist);
        }
        return dists;
    }

    public static double computeError(ArrayList<Double> rxLocation) {
        double rxLocationError = 0;
        for (int i = 0; i < Light.transmitters.size(); i++) {
            double distance1 = Math.sqrt(Math.pow(rxLocation.get(0) - Light.transmitters.get(i).x, 2)
                    + Math.pow(rxLocation.get(1) - Light.transmitters.get(i).y, 2)
                    + Math.pow(rxLocation.get(2) - Light.transmitters.get(i).z, 2));
            double distance2 = Math.sqrt(kVals.get(i)* kVals.get(i) * imgSquaredDistance[i]);
            rxLocationError += Math.abs(distance1 - distance2);
        }
        return rxLocationError;
    }

    public Result computeAoA (String units, int[] frequencies, Location[] locations) {

        // set variables for Room
        Room.setUnits(units);
        for (int i = 0; i < frequencies.length; i++) {
            Room.addFreqLocation(frequencies[i], locations[i]);
        }

        // round frequency(from image) to multiples of 1000
        matchActualFreq();

        // calculate square distance between two lights on image
        computeImageSquaredDistance();

        // calculate public transmitterPairSquaredDistance, which will be used later
        calculatePairSquaredDistance();
        // calculate public pairwiseImageInnerProducts, which will be used later
        calculatePairImageInnerProducts();

        ArrayList<Double> kValsInit = bruteForceK();

        // calculate public kVals(after optimizing)
        kVals = optimizeInitValues(kValsInit, AoA::leastSquaresScalingFactors);

        // get initial rx position
        ArrayList<Double> rxLocationInit = initialPositionGuess();

        ArrayList<Double> rxLocation = optimizeInitValues(rxLocationInit, AoA::leastSquaresRxLocation);

        double error = computeError(rxLocation);

        return new Result(rxLocation, error);
    }
}