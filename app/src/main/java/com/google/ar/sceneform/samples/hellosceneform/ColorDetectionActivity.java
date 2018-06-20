package com.google.ar.sceneform.samples.hellosceneform;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.*;

import java.util.*;
import java.lang.*;


public class ColorDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private final String TAG = "ColorDetection"; //for Log purposes
    private CameraBridgeViewBase openCvCameraBridge;
    private Mat mRgba;

    private int missedFrames = 0;
    private Mat hierarchy;
    private int maxcntID;
    public String finalColor;
    private int redFrame;
    private int greenFrame;

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
        ImageButton info = findViewById(R.id.info_button);

        //set an info button to explain the functioning of the color detection in an AlertDialog
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ColorDetectionActivity.this);
                alertDialog.setTitle("Color Detection");
                alertDialog.setMessage(R.string.info_color_detection);
                alertDialog.setNeutralButton("GOT IT!",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.create().show();
                }
        });

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
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }

        //hide the status bar to make it look like we are in portrait mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        openCvCameraBridge.disableView();
    }

    //necessary override in openCV
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        hierarchy = new Mat();
        finalColor="";
        greenFrame=0;
        redFrame=0;
        missedFrames=0;


    }

    //necessary override in openCV
    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        ProgressBar pb = findViewById(R.id.loadingBar);
        int foundColor = 0;
        //Trasformazione della camera frame in ogetto Mat
        mRgba = inputFrame.rgba();
        Mat mHSV = new Mat();
        Mat HSVcopy = new Mat();
        //Mat Blurred = new Mat();
        Mat BlueTresh= new Mat();
        Mat GreenTresh = new Mat();
        //Conversione della frame in HSV
        //Imgproc.GaussianBlur(mRgba,Blurred,new Size(45,45),0);
        Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV_FULL);
        mHSV.copyTo(HSVcopy);
        Core.inRange(HSVcopy,new Scalar(0,154,164),new Scalar(31,255,255),BlueTresh);
        Core.inRange(HSVcopy,new Scalar(37,110,120),new Scalar(100,255,255),GreenTresh);

        List<MatOfPoint> Gcontours = new ArrayList<>();
        List<MatOfPoint> Bcontours = new ArrayList<>();
        //Creazione di un array di contorni
        Imgproc.findContours(GreenTresh, Gcontours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.findContours(BlueTresh, Bcontours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        MatOfPoint2f approxCurve = new MatOfPoint2f();


        //assicurarsi che almeno un contorno sia trovato
        if (!Bcontours.isEmpty()) {
            //percorrere tutti i contorni
            for (int idx = 0; idx<Bcontours.size(); idx ++) {

                MatOfPoint contour = Bcontours.get(idx);
                //trovare il rettangolo piu piccolo che circonda il contorno
                Rect rect = Imgproc.boundingRect(contour);
                double contourArea = Imgproc.contourArea(contour);



                matOfPoint2f.fromList(contour.toList());
                //Calcolo del numero di angoli e del loro valore
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
                    //considereare solo le forme con quattro angoli e con certi valori di coseni
                    boolean isRect = total == 4 && minCos >= -0.1 && maxCos <= 0.3;
                    if (isRect) {
                        if(contourArea>100){
                            Imgproc.drawContours(mRgba, Bcontours, idx, new Scalar(255, 0, 0), 5);
                            //ci interessano solo i rettangoli di una certa grandezza (evitare parassiti)
                            maxcntID =idx;
                            Scalar color = new Scalar(0, 0, 255);

                            foundColor = 1;

                        }


                    }

                }
            }
        }
        if (!Gcontours.isEmpty()) {
            //percorrere tutti i contorni
            for (int idx = 0; idx<Gcontours.size(); idx ++) {

                MatOfPoint contour = Gcontours.get(idx);

                double contourArea = Imgproc.contourArea(contour);



                matOfPoint2f.fromList(contour.toList());
                //Calcolo del numero di angoli e del loro valore
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
                    //considereare solo le forme con quattro angoli e con certi valori di coseni
                    boolean isRect = total == 4 && minCos >= -0.1 && maxCos <= 0.3;
                    if (isRect) {
                        if(contourArea>100){
                            maxcntID =idx;
                            foundColor = 2;
                        }


                    }

                }
            }
        }
        if(!Gcontours.isEmpty() || !Bcontours.isEmpty()) {
            if(foundColor==1){
                Imgproc.drawContours(mRgba, Bcontours, maxcntID, new Scalar(255, 0, 0), 5);
                if(redFrame<33){
                    redFrame++;
                    //sets progress bar percentage
                    pb.setProgress(redFrame*3);
                }
                else{
                    Imgproc.putText (mRgba,"RED",new Point(10, 50),Core.FONT_HERSHEY_SIMPLEX ,1,new Scalar(255, 0,0),4);
                    foundColor=1;
                    finalColor = "RED";
                }
            }
            else if(foundColor==2){
                Imgproc.drawContours(mRgba, Gcontours, maxcntID, new Scalar(255, 0, 0), 5);
                if(greenFrame<33){
                    greenFrame++;
                    Imgproc.drawContours(mRgba, Gcontours, maxcntID, new Scalar(255, 0, 0), 5);

                    //sets progress bar percentage
                    pb.setProgress(greenFrame*3);
                }
                else{
                    Imgproc.drawContours(mRgba, Gcontours, maxcntID, new Scalar(255, 0, 0), 5);
                    Imgproc.putText (mRgba,"GREEN",new Point(10, 50),Core.FONT_HERSHEY_SIMPLEX ,1,new Scalar(0, 255, 0),4);
                    foundColor=2;
                    finalColor = "GREEN";
                }
            }

        }

        if(foundColor == 0)
        {
            //resetta la status bar se per alcuni frame consecutivi non rileviamo colore e forma
            missedFrames++;
            if (missedFrames > 10)
            {
                //Log.e(TAG, "missedFrames>4");
                greenFrame = 0; redFrame = 0; missedFrames = 0;
                pb.setProgress(0);
            }
        }

        //pass the result back to the calling Activity
        if (finalColor.equals("GREEN") || finalColor.equals("RED"))
        {
            Log.e(TAG, "final Color " + finalColor);
            Intent returnIntent = new Intent();
            returnIntent.putExtra("COLOR", finalColor);
            setResult(SceneformActivity.RESULT_OK, returnIntent);
            finish();
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