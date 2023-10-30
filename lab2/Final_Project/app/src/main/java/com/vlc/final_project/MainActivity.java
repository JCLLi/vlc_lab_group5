package com.vlc.final_project;

import static org.opencv.core.CvType.CV_8UC3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    Button select;
    Button video;
    Button clear;
    ImageView imageView;
    TextView textView;
    Bitmap bitmap;

    float[] freq_list;

    MediaMetadataRetriever mediaMetadataRetriever;
    final Handler handler = new Handler();
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
    private Random rng = new Random(12345);



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");

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

        mediaMetadataRetriever = new MediaMetadataRetriever();

        select = findViewById(R.id.select);
        clear = findViewById(R.id.clear);
        imageView = findViewById(R.id.image);
        textView = findViewById(R.id.freq);
        freq_list = new float[]{3704, 5000, 6667, 10000};

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

        clear.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");
                imageView.setImageResource(0);
            }
        }));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100 && data != null){
            Mat mat_copy;
            Mat result = new Mat(4000, 3000, CvType.CV_8UC1);
            Mat mat = new Mat();
            try {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                List<MatOfPoint> contours = new ArrayList<>();
                List<Double> radius_n_center = new ArrayList<>();
                Utils.bitmapToMat(rotatedBitmap, mat);
                // Find contour of the led
                mat_copy = Frame.find_contour(contours, mat);

                // Calculate radius and center of the led
                Frame.radius_and_center(contours, radius_n_center);
                int y = (int) radius_n_center.get(2).doubleValue();
                boolean black = true;
                for (int i = 0; i < mat_copy.cols(); i++){
                    double test = mat_copy.get(y, i)[0];
                    if (test != 0){
                        black = false;
                        break;
                    }
                }
                List<Float> freq_list = new ArrayList<>();
                List<int[]> center_list = new ArrayList<>();
                if (!black){
                    String frequency = "";
                    for(int i = 0; i < radius_n_center.size(); i += 3){
                        int[] range = Frame.light_range((int) radius_n_center.get(1 + i).doubleValue(),
                                (int) radius_n_center.get(2 + i).doubleValue(),
                                (float) radius_n_center.get(i).doubleValue());

                        // Process image to get the edge of pulses
                        Frame struct = Frame.frame_process(mat_copy, (float) radius_n_center.get(i).doubleValue());
                        Mat edge = new Mat();
                        Imgproc.Canny(struct.mat, edge, struct.threshold / 5, struct.threshold, 7);

                        // Convert image to quantifiable data
                        int[] columnSums = Frame.frame_quantify(edge, range, (float) radius_n_center.get(i).doubleValue());
                        List<Float> pulse = Frame.getPulses(columnSums, range[0]);

                        Frame.filterPules(pulse, struct.mat, struct.threshold, (int) radius_n_center.get(2 + i).doubleValue(),
                                (float) radius_n_center.get(i).doubleValue());

                        // Calculate corresponding led frequency
                        float freq = Frame.calc_freq(pulse, 100);
                        frequency += Float.toString(freq) + "\n";

                        // Draw the edge of pulse
                        Mat temp = Frame.draw(struct.mat, pulse, (int) radius_n_center.get(2 + i).doubleValue());
                        Core.add(temp, result, result);

                        freq_list.add(freq);

                        int[] center = {(int) radius_n_center.get(1 + i).doubleValue(), (int) radius_n_center.get(2 + i).doubleValue()};
                        center_list.add(center);

                    }

                    Utils.matToBitmap(result, rotatedBitmap);
                    imageView.setImageBitmap(rotatedBitmap);
                    textView.setText(frequency);
                }
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
