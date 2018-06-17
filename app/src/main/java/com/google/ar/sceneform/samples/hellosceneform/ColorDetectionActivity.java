package com.google.ar.sceneform.samples.hellosceneform;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

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

    public String TAG = "hellosceneform";
    private CameraBridgeViewBase openCvCameraBridge;
    private Mat mRgba;

    private Mat edges;
    private Mat hierarchy;
    private int maxcntID;
    private MatOfPoint maxcnt;
    public String finalColor;
    private int redFrame;
    private int greenFrame;
    private int foundColor;
    private int missedFrames = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {

            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    // caricare un modulo native

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
        finalColor="";
        greenFrame=0;
        redFrame=0;


    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //barra di caricamento per progresso riconoscimento colore
        ProgressBar pb = findViewById(R.id.loadingBar);

        foundColor=0;
        //Trasformazione della camera frame in ogetto Mat
        mRgba = inputFrame.rgba();
        Mat mHSV = new Mat();
        //Conversione della frame in HSV
        Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV_FULL);
        Mat destination = new Mat(mRgba.rows(), mRgba.cols(), mRgba.type());
        //ALtra conversione in grayscale (meglio per il riconoscimento di contorni)
        Imgproc.cvtColor(mRgba, destination, Imgproc.COLOR_RGB2GRAY);
        int threshold = 100;
        //Effetuare un Canny sulla frame in grayscale
        //(Canny è una trasformazione che permette di detettare i contorni)
        Imgproc.Canny(destination, edges, threshold, threshold*3);
        List<MatOfPoint> contours = new ArrayList<>();
        //Creazione di un array di contorni
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        //assicurarsi che almeno un contorno sia trovato
        if (!contours.isEmpty()) {
            //percorrere tutti i contorni
            for (int idx = 0; idx<contours.size(); idx ++) {

                MatOfPoint contour = contours.get(idx);
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
                        //Si entra in questa zona solo se il contorno è un rettangolo
                        MatOfPoint2f areaPoints = new MatOfPoint2f(contour.toArray());
                        //Creazione di un bounding rectangle (Rettangolo che segue le rotazioni del contorno)
                        RotatedRect boundingRect = Imgproc.minAreaRect(areaPoints);

                        Point rotated_rect_points[] = new Point[4];
                        boundingRect.points(rotated_rect_points);
                        //recuperazione del centro del rettangolo (dunque del contorno)
                        double centx = ((rect.tl().x+rect.br().x)/2);
                        double centy = (rect.tl().y+rect.br().y)/2;

                        //recupero delle informazioni HSV del pixel centrale
                        double[] pixel = mHSV.get((int) centy,(int) centx);

                        if(contourArea>100){
                            //ci interessano solo i rettangoli di una certa grandezza (evitare parassiti)
                            maxcnt=contour;
                            maxcntID =idx;
                            Scalar color = new Scalar(0, 0, 255);


                            //in funzione dei valori H S e V del pixel centrale, possiamo definire
                            //di che colore è il rettangolo

                            //il parametro recuperato sarà "finalColor" dove
                            //0: nessun colore dettettato (default)
                            //1: Rosso
                            //2: Verde
                            if(pixel[0]<50 && pixel[1] >120 && pixel[2]>20 && pixel[2]<210){


                                //Imgproc.putText (mRgba,"RED",new Point(10, 50),Core.FONT_HERSHEY_SIMPLEX ,1,new Scalar(255, 255, 255),4);
                                //Imgproc.drawContours(mRgba, contours, maxcntID, color, 5);
                                foundColor = 1;
                            }
                            else if(pixel[0]>37 && pixel[0]<100 && pixel[1] > 110 && pixel[2]>120){


                                //Imgproc.putText (mRgba,"GREEN",new Point(10, 50),Core.FONT_HERSHEY_SIMPLEX ,1,new Scalar(255, 255, 255),4);
                                //Imgproc.drawContours(mRgba, contours, maxcntID, color, 5);
                                foundColor = 2;

                            }

                        }


                    }

                }
            }
            if(foundColor==1){
                if(redFrame<32){
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
                if(greenFrame<32){
                    greenFrame++;
                    //sets progress bar percentage
                    pb.setProgress(redFrame*3);
                }
                else{
                    Imgproc.putText (mRgba,"GREEN",new Point(10, 50),Core.FONT_HERSHEY_SIMPLEX ,1,new Scalar(0, 255, 0),4);
                    foundColor=2;
                    finalColor = "GREEN";
                }
            }

        }

        if(foundColor == 0)
        {
            missedFrames++;
            if (missedFrames > 4)
            {
                Log.e(TAG, "missedFrames>4");
                greenFrame = 0; redFrame = 0; missedFrames = 0;
                pb.setProgress(0);
            }
        }
        foundColor = 0;
        Log.e(TAG, "foundColor " + foundColor + " missedFrames " + missedFrames);


        //passa colore ad activity di Sceneform, dove verrà applicato conseguentemente il filtro
        if (finalColor != "") {
                    Intent i = new Intent( ColorDetectionActivity.this, HelloSceneformActivity.class);
                    i.putExtra("color", finalColor);
                    startActivity(i);
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