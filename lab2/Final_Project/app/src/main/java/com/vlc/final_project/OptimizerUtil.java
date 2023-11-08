package com.vlc.final_project;

import static com.vlc.final_project.AoA.imgSquaredDistance;
import static com.vlc.final_project.AoA.leastSquaresRxLocation;
import static com.vlc.final_project.AoA.leastSquaresScalingFactors;
import static com.vlc.final_project.AoA.pairwiseImageInnerProducts;
import static com.vlc.final_project.Light.LightList;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;

import java.util.ArrayList;

public class OptimizerUtil {
    public static MultivariateFunction mulVariationFunctionForK(LightList lightList){
        MultivariateFunction model = new MultivariateFunction() {
            @Override
            public double value(double[] point) {
                ArrayList<Double> errs = leastSquaresScalingFactors(toDoubleList(point), lightList);
                return errs.stream().mapToDouble(err -> err * err).sum();
            }
        };
        return model;
    }

    public static MultivariateVectorFunction gradientFunctionForK(int num){
        MultivariateVectorFunction mvf = new MultivariateVectorFunction() {
            @Override
            public double[] value(double[] point) throws IllegalArgumentException {
                int index = 0;
                int numVariables = point.length;
                double[] gradient = new double[numVariables];

                for (int i = 0; i < num - 1; i++) {
                    for (int j = i + 1; j < num; j++) {
                        // Compute the gradient component with respect to kVals[i]
                        gradient[i] += 2 * (point[i] * imgSquaredDistance[i] - point[j] * pairwiseImageInnerProducts[i][j]);

                        // Compute the gradient component with respect to kVals[j]
                        gradient[j] += 2 * (point[j] * imgSquaredDistance[j] - point[i] * pairwiseImageInnerProducts[i][j]);
                    }
                }
                return gradient;
            }
        };
        return mvf;
    }

    public static MultivariateFunction mulVariationFunctionForLoc(LightList lightList){
        MultivariateFunction model = new MultivariateFunction() {
            @Override
            public double value(double[] point) {
                ArrayList<Double> errs = leastSquaresRxLocation(toDoubleList(point), lightList);
                return errs.stream().mapToDouble(err -> err * err).sum();
            }
        };
        return model;
    }
    public static MultivariateVectorFunction gradientFunctionForLoc(int num, ArrayList<Double> kVals, LightList lightList){
        MultivariateVectorFunction mvf = new MultivariateVectorFunction() {
            @Override
            public double[] value(double[] rxLocation) throws IllegalArgumentException {
                double[] gradient = new double[3]; // 3 components for rxLocation[0], rxLocation[1], rxLocation[2]

                for (int i = 0; i < num; i++) {
                    double deltaX = rxLocation[0] - lightList.transmitters.get(i).x;
                    double deltaY = rxLocation[1] - lightList.transmitters.get(i).y;
                    double deltaZ = rxLocation[2] - lightList.transmitters.get(i).z;
                    double imgDist = kVals.get(i) * kVals.get(i) * imgSquaredDistance[i];

                    // Compute the gradient components
                    gradient[0] += 2 * deltaX * (deltaX + deltaY + deltaZ - imgDist);
                    gradient[1] += 2 * deltaY * (deltaX + deltaY + deltaZ - imgDist);
                    gradient[2] += 2 * deltaZ * (deltaX + deltaY + deltaZ - imgDist);
                }

                return gradient;
            }
        };
        return mvf;
    }

    public static ArrayList<Double> optimizeInitValues(ArrayList<Double> initialGuess,
                                                       MultivariateVectorFunction multivariateVectorFunction,
                                                       MultivariateFunction model) {
        // Create the optimizer
        NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
                NonLinearConjugateGradientOptimizer.Formula.FLETCHER_REEVES, new SimpleValueChecker(1e-6, 1e-6));
        // Define the optimization problem
        PointValuePair optimum = optimizer.optimize(
                new MaxEval(3000), // Maximum number of evaluations
                GoalType.MINIMIZE,
                new InitialGuess(toDoubleArray(initialGuess)), // Initial guess
                new ObjectiveFunction(model),
                new SearchInterval(0, 20), // Appropriate search interval for kVals
                new ObjectiveFunctionGradient(multivariateVectorFunction));

        // Convert the result to ArrayList
        double[] result = optimum.getPoint();

        return toDoubleList(result);
    }

    private static double[] toDoubleArray(ArrayList<Double> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private static ArrayList<Double> toDoubleList(double[] array) {
        ArrayList<Double> list = new ArrayList<>();
        for (double value : array) {
            list.add(value);
        }
        return list;
    }



}
