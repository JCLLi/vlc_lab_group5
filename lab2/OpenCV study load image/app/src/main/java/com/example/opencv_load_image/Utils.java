package com.example.opencv_load_image;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class Utils {
//    public int image_recognize(float radius){
//        float factor = 1 / radius;
//        int blur_width = (int) (factor * radius + 1);
//
//    }


//                v_idx = -1
//                tot = 0
//                div = 0
//                impulses = []
//                while v_idx < len(vals)-1:
//                v_idx += 1
//                if vals[v_idx] < (.3 * max(vals)):
//                continue
//                while vals[v_idx] > (.3 * max(vals)):
//                tot += v_idx * vals[v_idx]
//                div += vals[v_idx]
//                v_idx += 1
//                try:
//                impulses.append(float(tot) / div)
//                except ZeroDivisionError:
//                pass
//                        tot = 0
//                div = 0


//                Mat drawing = Mat.zeros(mat.size(), CvType.CV_8UC3);
//                List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
//
//                for (MatOfPoint2f poly : contoursPoly) {
//                    contoursPolyList.add(new MatOfPoint(poly.toArray()));
//                }
//
//                Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
//                for (int i = 0; i < contours.size(); i++) {
//                    Imgproc.drawContours(drawing, contoursPolyList, i, color);
//                }
//

//                Imgproc.circle(drawing, centers[index], (int) radius[index][0], color, 2);
//
//                Utils.matToBitmap(drawing, bitmap);
    public Mat image_process(List<MatOfPoint> contours, Mat mat){
        Mat hierarchy = new Mat();
        Mat mat_copy = new Mat();
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        mat_copy = mat.clone();
        Imgproc.blur(mat, mat, new Size(5, 5));
        Imgproc.threshold(mat, mat ,0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        return mat_copy;
    }
}
