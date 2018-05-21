package com.xyxxt.testopencv;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

public class OpenCVCamera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    int option = R.id.actionDefault;
    JavaCameraView cameraView;
    ImageView imageView;
    Mat rgbA, rgbT, rgbF;
    TextView textView;
    double angle = 0;
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status){
                case SUCCESS:
                    cameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cvcamera);

        textView = findViewById(R.id.txt);
        cameraView = findViewById(R.id.View1Camera);
        imageView = findViewById(R.id.imageView);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, baseLoaderCallback);


        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraView != null)cameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraView != null)cameraView.disableView();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.e("Option", String.valueOf(item.getItemId()));
        switch (item.getItemId()){
            case R.id.actionDilate:
                option = R.id.actionDilate;
                return true;
            case R.id.actionDetectSudoku:
                option = R.id.actionDetectSudoku;
                return true;
            default:
                option = R.id.actionDefault;
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        rgbA = new Mat(height, width, CvType.CV_8UC1);
        rgbF = new Mat(height, width, CvType.CV_8UC1);
        rgbT = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        rgbA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        rgbA = inputFrame.rgba();
        Core.transpose(rgbA, rgbT);
        Imgproc.resize(rgbT, rgbF, rgbF.size(), 0,0, 0);
        Core.flip(rgbF, rgbA, 1 );
        Mat dest = new Mat(rgbA.rows(),rgbA.cols(),CvType.CV_8UC1);
        switch (option){
            case R.id.actionDilate:

                Imgproc.dilate(rgbA, dest, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(11, 11)));
                showImageFromCamera(dest);
                break;
            case R.id.actionDetectSudoku:
                SudokuSolver sudokuSolver = new SudokuSolver(rgbA, getApplicationContext());
                Mat img = sudokuSolver.getImageThreshold(sudokuSolver.getImgOriginal());
                FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
                MatOfKeyPoint keyPoints = new MatOfKeyPoint();
                featureDetector.detect(img, keyPoints);

                Mat drawImage = sudokuSolver.getImgOriginal().clone();
                for (int i = 0; i < keyPoints.toArray().length; ++i)
                    Imgproc.circle(drawImage, keyPoints.toArray()[i].pt, 10, new Scalar(255, 0, 255), -1);

                showImageFromCamera(drawImage);
                break;
            default:
                runOnUiThread(new Runnable() {
                    public void run(){
                        imageView.setImageResource(android.R.color.transparent);
                    }
                });
                break;
        }




        //Log.e("Option", String.valueOf(option));

        return rgbA;
    }


    public void showImageFromCamera(Mat result) {
        final Bitmap bm = Bitmap.createBitmap(result.cols(), result.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bm);

        runOnUiThread(new Runnable() {
            public void run(){
                imageView.setImageBitmap(bm);
            }
        });
    }



}
