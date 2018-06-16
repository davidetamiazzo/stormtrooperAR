package com.google.ar.sceneform.samples.hellosceneform;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;


import org.opencv.core.Core;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.*;
import org.opencv.imgcodecs.*;
import org.opencv.android.CameraBridgeViewBase;
import java.util.*;
import java.lang.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ColorDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private CameraBridgeViewBase openCvCameraBridge;
    private Mat mRgba;

    private Mat edges;
    private Mat hierarchy;
    private int maxcntID;
    private MatOfPoint maxcnt;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {

            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {

                    openCvCameraBridge.enableView();
                }break;
                default:{
                    super.onManagerConnected(status);
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.loadLibrary("opencv_java3");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_detection);

        //requests camera
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA}, 1);

        }



        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        openCvCameraBridge = (CameraBridgeViewBase) findViewById(R.id.surfaceView);
        openCvCameraBridge.setVisibility(CameraBridgeViewBase.VISIBLE);
        openCvCameraBridge.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(openCvCameraBridge != null){
            openCvCameraBridge.disableView();
        }
    }

    @Override
    public void onResume(){
        super.onResume();


        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }else{
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        //locks screen orientation to Landscape while using openCV
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        openCvCameraBridge.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        edges = new Mat();
        hierarchy = new Mat();
        maxcnt=new MatOfPoint();

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        Mat mHSV = new Mat();
        Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV_FULL);
        Mat destination = new Mat(mRgba.rows(), mRgba.cols(), mRgba.type());

        Imgproc.cvtColor(mRgba, destination, Imgproc.COLOR_RGB2GRAY);
        int threshold = 100;

        Imgproc.Canny(destination, edges, threshold, threshold*3);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        if (!contours.isEmpty()) {

            for (int idx = 0; idx<contours.size(); idx ++) {

                MatOfPoint contour = contours.get(idx);
                Rect rect = Imgproc.boundingRect(contour);
                double contourArea = Imgproc.contourArea(contour);

                matOfPoint2f.fromList(contour.toList());
                Imgproc.approxPolyDP(matOfPoint2f, approxCurve, Imgproc.arcLength(matOfPoint2f, true) * 0.02, true);
                long total = approxCurve.total();
                if (total >= 4 && total <= 6) {
                    List<Double> cos = new ArrayList<>();
                    Point[] points = approxCurve.toArray();
                    for (int j = 2; j < total + 1; j++) {
                        cos.add(angle(points[(int) (j % total)], points[j - 2], points[j - 1]));
                    }
                    Collections.sort(cos);
                    Double minCos = cos.get(0);
                    Double maxCos = cos.get(cos.size() - 1);
                    boolean isRect = total == 4 && minCos >= -0.1 && maxCos <= 0.3;
                    if (isRect) {
                        double ratio = Math.abs(1 - (double) rect.width / rect.height);
                        //Imgproc.drawContours(mRgba, contours, idx, color, 5);

                        MatOfPoint2f areaPoints = new MatOfPoint2f(contour.toArray());
                        RotatedRect boundingRect = Imgproc.minAreaRect(areaPoints);

                        Point rotated_rect_points[] = new Point[4];
                        boundingRect.points(rotated_rect_points);

                        double centx = ((rect.tl().x+rect.br().x)/2);
                        double centy = (rect.tl().y+rect.br().y)/2;

                        double[] pixel = mHSV.get((int) centy,(int) centx);
                        System.out.print(pixel[0]);

                        if(contourArea>100){


                            maxcnt=contour;
                            maxcntID =idx;
                            Scalar color = new Scalar(0, 0, 255);



                            if(pixel[0]<50 && pixel[1] >120 && pixel[2]>20 && pixel[2]<210){
                                Imgproc.putText (mRgba,"RED",new Point(10, 50),Core.FONT_HERSHEY_SIMPLEX ,1,new Scalar(255, 255, 255),4);
                                Imgproc.drawContours(mRgba, contours, maxcntID, color, 5);
                            }


                            else if(pixel[0]>37 && pixel[0]<100 && pixel[1] > 110 && pixel[2]>120){
                                Imgproc.putText (mRgba,"GREEN",new Point(10, 50),Core.FONT_HERSHEY_SIMPLEX ,1,new Scalar(255, 255, 255),4);
                                Imgproc.drawContours(mRgba, contours, maxcntID, color, 5);
                            }

                        }


                    }

                }
            }




        }

        return mRgba;

    }
    private double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
    }






}