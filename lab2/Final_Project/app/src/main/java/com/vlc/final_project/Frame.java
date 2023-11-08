package com.vlc.final_project;

import static java.lang.Math.abs;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Frame {
    public Mat mat;
    public double threshold;

    public Frame(Mat mat, double threshold){
        this.mat = mat;
        this.threshold = threshold;
    }


    public static Mat find_contour(List<MatOfPoint> contours, Mat mat){
        Mat hierarchy = new Mat();
        Mat mat_copy;
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        mat_copy = mat.clone();
        Imgproc.blur(mat, mat, new Size(5, 5));
        Imgproc.threshold(mat, mat ,0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();
        return mat_copy;
    }

    public static void radius_and_center(List<MatOfPoint> contours, List<Double> r_n_c){
        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        Point[] centers = new Point[contours.size()];
        float[][] radius = new float[contours.size()][1];
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            centers[i] = new Point();
            Imgproc.minEnclosingCircle(contoursPoly[i], centers[i], radius[i]);
        }

        List<ArrayList<Integer>> pointGroup = new ArrayList<>();
        for (int i = 0; i < centers.length; i++){
            boolean has = false;
            for (ArrayList<Integer> pg: pointGroup){
                if (pg.contains(i)){
                    has = true;
                    break;
                }
            }
            if(!has){
                boolean sim = false;
                for (ArrayList<Integer> pg: pointGroup){
                    double width = abs(centers[pg.get(0)].x - centers[i].x);
                    double height = abs(centers[pg.get(0)].y - centers[i].y);
                    if ( width < 300 &&  height < 300){
                        pg.add(i);
                        sim = true;
                        break;
                    }
                }
                if (!sim){
                    ArrayList<Integer> group = new ArrayList<>();
                    group.add(i);
                    pointGroup.add(group);
                }
            }
        }


        for(int i = 0; i < pointGroup.size(); i++){
            int index = pointGroup.get(i).get(0);
            float max_radius = radius[index][0];
            for(int j = 1; j < pointGroup.get(i).size(); j++){
                if(radius[pointGroup.get(i).get(j)][0] > max_radius){
                    max_radius = radius[pointGroup.get(i).get(j)][0];
                    index = pointGroup.get(i).get(j);
                }
            }
            r_n_c.add((double) max_radius);
            r_n_c.add((double) centers[index].x);
            r_n_c.add((double) centers[index].y);
        }
        int a = 0;
    }

    public static int[] frame_quantify(Mat edge, int[] range, float radius){

        int[] columnSums = new int[2 * (int) radius + 40];
        for (int col = range[0]; col < range[1]; col++) {
            int sum = 0;
            for (int row = range[2]; row < range[3]; row++) {
                sum += (int) edge.get(row, col)[0]; // Access the pixel value at (row, col)
            }
            columnSums[col- range[0]] = sum;
        }

        return columnSums;
    }

    public static List<Float> getPulses(int[] columnSums, int start_x) {
        int column_index = -1;
        int weighted_sum = 0;
        int sum_weight = 0;
        List<Float> pulse = new ArrayList<>();
        int max = 0;
        for (int value : columnSums) {
            if (value > max)
                max = value;
        }
        boolean start = false;
        while (column_index < columnSums.length - 1) {
            column_index++;
            boolean is_black = true;
            if(columnSums[column_index] != 0)
                is_black = false;
            if (!start && !is_black){
                pulse.add((float) column_index + start_x);
                start = true;
            }else {
                if ((float) columnSums[column_index] > 0.2 * max) {
                    while ((float) columnSums[column_index] > 0.2 * max) {
                        weighted_sum += (column_index + start_x) * columnSums[column_index];
                        sum_weight += columnSums[column_index];
                        column_index++;
                        if(column_index == columnSums.length)
                            break;
                    }
                    pulse.add((float) weighted_sum / (float) sum_weight);
                    weighted_sum = 0;
                    sum_weight = 0;
                }
            }
        }
        return pulse;
    }

    public static void filterPules(List<Float> pulse, Mat mat, double threshold, int center_y, float radius){
//        float dis = 1000;
        int one = 0;
        boolean remove = false;
        boolean last = false;
        boolean cur = false;
        List<Integer> remove_index = new ArrayList<>();
        Log.d("df", Double.toString(radius * 0.1));
        for(int i = 1; i < pulse.size(); i++){
            Log.d("a", Double.toString(pulse.get(i) - pulse.get(i - 1)));
            if (pulse.get(i) - pulse.get(i - 1) < radius * 0.1){
                cur = true;
                if(last || i == 1 || i == pulse.size() - 1)
                    remove_index.add(i - 1);
            }else {
                cur = false;
            }
            last = cur;
        }
        for (int i = 0; i < remove_index.size(); i++){
            pulse.remove(remove_index.get(i) - i);
        }

        last = false;
        cur = false;
        List<Integer> index = new ArrayList<>();
        int count = 0;
        for(int i = 1; i < pulse.size(); i++){
            int check = (int) (pulse.get(i - 1) + (pulse.get(i) - pulse.get(i - 1)) / 2);
            for (int j = -5; j < 5; j++){
                if ((double) mat.get(center_y + j * (int) (radius / 10), check)[0] > threshold)
                    count++;
            }
//                if ((double) mat.get(center_y, (int) check)[0] > threshold)
//                    count++;
//                if ((double) mat.get(center_y - (int) (radius / 10), (int) check)[0] > threshold)
//                    count++;
//                if ((double) mat.get(center_y + (int) (radius / 10), (int) check)[0] > threshold)
//                    count++;

            if (count >= 2)
                cur = true;
            if ((last && cur) || (!last && !cur))
                index.add(i - 1);
            last = cur;
            cur = false;
            count = 0;
        }
        for (int i = 0; i < index.size(); i++){
            pulse.remove(index.get(i) - i);
        }
        for(int i = 1; i < pulse.size(); i++){
            Log.d("d", Float.toString(pulse.get(i) - pulse.get(i - 1)));
        }
    }
    public static Frame frame_process(Mat mat_copy, float radius){
        float factor = 1 / radius;
        int blur_width = (int) (factor * radius + 1);
        Mat inter = new Mat();
        Imgproc.blur(mat_copy, inter, new Size(blur_width - 1, radius));
        double threshold = Imgproc.threshold(inter, inter ,0, 255,
                Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        return new Frame(inter, threshold);
    }

    public static Mat draw(Mat mat, List<Float> pulse, int center_y){
        int numRows = mat.rows();
        double[] a = {200};
        for (int i = 0; i < pulse.size(); i++){
            for (int y = 0; y < numRows; y++){
                int x = 0;
                if(pulse.get(i) % ((float) ((int) pulse.get(1).floatValue())) >= 0.5)
                    x = (int) pulse.get(i).floatValue() + 1;
                else
                    x = (int) pulse.get(i).floatValue();

                mat.put(y, x, a);
            }
        }
        for (int i = 0; i < mat.cols(); i++){
            mat.put(center_y, i, a);
        }
        return mat;
    }

    public static float calc_freq(List<Float> pulse, int code){
        List<Float> pulse_copy_1  = new ArrayList<>();
        List<Float> pulse_copy_2  = new ArrayList<>();
        for (float nums: pulse){
            pulse_copy_1.add(nums);
            pulse_copy_2.add(nums);
        }
        pulse_copy_1.remove(0);
        pulse_copy_2.remove(pulse_copy_2.size() - 1);
        List<Float> diff = new ArrayList<>();
        float sum = 0;
        for(int i = 0; i < pulse_copy_2.size(); i++){
            sum += pulse_copy_1.get(i) - pulse_copy_2.get(i);
        }
        float interval = sum / pulse_copy_2.size();
        float period = 0;
        if (code == 100){
            period = interval * 0.00000941f;
        }else {
            period = interval * 0.0000186f;
        }
        float freq = 1 / period;
        return freq;
    }

    public static int[] light_range(int center_x, int center_y, float radius){
        int start_x = center_x - (int) radius - 20;
        if(start_x < 0) start_x = 0;
        int end_x = center_x + (int) radius + 20;
        if(end_x > 2999 ) end_x = 2999;
        int start_y = center_y - (int) radius - 50;
        if(start_y < 0) start_y = 0;
        int end_y = center_y + (int) radius + 50;
        if(end_y > 3999 ) end_y = 3999;
        return new int[]{start_x, end_x, start_y, end_y};
    }
}

//    MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
//    Point[] centers = new Point[contours.size()];
//    float[][] radius = new float[contours.size()][1];
//                for (int i = 0; i < contours.size(); i++) {
//        contoursPoly[i] = new MatOfPoint2f();
//        Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
//        centers[i] = new Point();
//        Imgproc.minEnclosingCircle(contoursPoly[i], centers[i], radius[i]);
//        }
//
//
//        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
//        for (MatOfPoint2f poly : contoursPoly) {
//        contoursPolyList.add(new MatOfPoint(poly.toArray()));
//        }
//        for (int i = 0; i < contours.size(); i++) {
//        Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
//        Imgproc.drawContours(mat, contoursPolyList, i, color);
//        Imgproc.circle(mat, centers[i], (int) radius[i][0], color, 2);
//        }
