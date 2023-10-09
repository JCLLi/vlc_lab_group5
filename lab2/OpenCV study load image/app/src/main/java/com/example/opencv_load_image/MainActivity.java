package com.example.opencv_load_image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    Button select;
    ImageView imageView;
    TextView textView;
    Bitmap bitmap;
    Mat mat;
    Mat mat_copy;
    Mat result;
    Mat hierarchy;
    Random rng = new Random(12345);

    float[] lastEvent = null;
    float d = 0f;
    float newRot = 0f;
    private boolean isZoomAndRotate;
    private boolean isOutSide;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private PointF start = new PointF();
    private PointF mid = new PointF();
    float oldDist = 1f;
    private float xCoOrdinate, yCoOrdinate;




    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    mat = new Mat();
                    result = new Mat();
                    hierarchy = new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);


        select = findViewById(R.id.select);
        imageView = findViewById(R.id.image);
        textView = findViewById(R.id.freq);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView view = (ImageView) v;
                view.bringToFront();
                viewTransformation(view, event);
                return true;
            }
        });

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 100);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<MatOfPoint> contours = new ArrayList<>();

        if(requestCode == 100 && data != null){
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                imageView.setImageBitmap(bitmap);

                Utils.bitmapToMat(bitmap, mat);
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
                mat_copy = mat.clone();
                Imgproc.blur(mat, mat, new Size(5, 5));
                Imgproc.threshold(mat, mat ,0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
                Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
                Point[] centers = new Point[contours.size()];
                float[][] radius = new float[contours.size()][1];
                for (int i = 0; i < contours.size(); i++) {
                    contoursPoly[i] = new MatOfPoint2f();
                    Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
                    centers[i] = new Point();
                    Imgproc.minEnclosingCircle(contoursPoly[i], centers[i], radius[i]);
                }

                float max_r = 0;
                int index = 0;
                for (int i = 0; i < radius.length; i++){
                    if(i == 0)
                        max_r = radius[i][0];
                    else {
                        if(radius[i][0] > max_r){
                            max_r = radius[i][0];
                            index = i;
                        }
                    }
                }
                int y_center = (int) centers[index].y;

                float factor = 1 / max_r;
                int blur_width = (int) (factor * max_r + 1);
                Imgproc.blur(mat_copy, mat_copy, new Size(blur_width, max_r));
                double threshold = Imgproc.threshold(mat_copy, mat_copy ,0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
                int kernel_size = (int) Math.round(max_r * 0.1) + 1;
                Imgproc.Canny(mat_copy, result, threshold / 5, threshold, 7);

                int numRows = result.rows();
                int numCols = result.cols();
                int[] columnSums = new int[numCols];
                for (int col = 0; col < numCols; col++) {
                    int sum = 0;
                    for (int row = 0; row < numRows; row++) {
                        sum += (int) result.get(row, col)[0]; // Access the pixel value at (row, col)
                    }
                    columnSums[col] = sum;
                }


                int column_index = -1;
                int weighted_sum = 0;
                int sum_weight = 0;
                List<Float> pulse = new ArrayList<>();
                int max = 0;
                for (int value: columnSums){
                    if (value > max)
                        max = value;
                }
                while (column_index < columnSums.length - 1){
                    column_index++;
                    if((float) columnSums[column_index] > 0.2 * max){
                        while ((float) columnSums[column_index] > 0.2 * max){
                            weighted_sum += column_index * columnSums[column_index];
                            sum_weight += columnSums[column_index];
                            column_index++;
                        }
                        pulse.add((float) weighted_sum/ (float) sum_weight);
                        weighted_sum = 0;
                        sum_weight = 0;
                    }
                }

                for (int i = 0; i < pulse.size(); i++){
                    for (int y = 0; y < numRows; y++){
                        int x = 0;
                        if(pulse.get(i) % ((float) ((int) pulse.get(1).floatValue())) >= 0.5)
                            x = (int) pulse.get(i).floatValue() + 1;
                        else
                            x = (int) pulse.get(i).floatValue();
                        double[] a = {200};
                        result.put(y, x, a);
                    }
                }

                List<Float> pulse_copy = new ArrayList<>();
                for (float nums: pulse){
                    pulse_copy.add(nums);
                }
                pulse.remove(0);
                pulse_copy.remove(pulse_copy.size() - 1);
                List<Float> diff = new ArrayList<>();
                float sum = 0;
                for(int i = 0; i < pulse_copy.size(); i++){
                    sum += pulse.get(i) - pulse_copy.get(i);
                }
                float interval = sum / pulse_copy.size();
                float period = interval * 0.0000219f;
                float freq = 1 / period;


                Utils.matToBitmap(result, bitmap);
                imageView.setImageBitmap(bitmap);
                textView.setText(Float.toString(freq));
                result.release();
                mat_copy.release();
                mat.release();
                hierarchy.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void viewTransformation(View view, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                xCoOrdinate = view.getX() - event.getRawX();
                yCoOrdinate = view.getY() - event.getRawY();

                start.set(event.getX(), event.getY());
                isOutSide = false;
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    midPoint(mid, event);
                    mode = ZOOM;
                }

                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
                isZoomAndRotate = false;
                if (mode == DRAG) {
                    float x = event.getX();
                    float y = event.getY();
                }
            case MotionEvent.ACTION_OUTSIDE:
                isOutSide = true;
                mode = NONE;
                lastEvent = null;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isOutSide) {
                    if (mode == DRAG) {
                        isZoomAndRotate = false;
                        view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                    }
                    if (mode == ZOOM && event.getPointerCount() == 2) {
                        float newDist1 = spacing(event);
                        if (newDist1 > 10f) {
                            float scale = newDist1 / oldDist * view.getScaleX();
                            view.setScaleX(scale);
                            view.setScaleY(scale);
                        }
                        if (lastEvent != null) {
                            newRot = rotation(event);
                            view.setRotation((float) (view.getRotation() + (newRot - d)));
                        }
                    }
                }
                break;
        }
    }

    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (int) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

}
