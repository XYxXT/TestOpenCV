package com.xyxxt.testopencv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraDevice;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class OpenCVImage extends AppCompatActivity {

    private final static int RESULT_LOAD_IMG = 3;

    private LinearLayout liLayoutButton;
    private ImageView imageSrc, imageDst;
    private TextView txtNumber;
    private Button btnPrevious, btnNext;
    private double angle = 0;

    private boolean isImageSelect = false;
    private SudokuSolver sudokuSolver;
    private Mat rgbA;
    private Mat sudokuSplits[][];
    private int iteradorI, iteradorJ;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status){
                case SUCCESS:
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
        setContentView(R.layout.activity_open_cvimage);

        imageSrc = findViewById(R.id.imageSrc);
        imageDst = findViewById(R.id.imageDst);
        liLayoutButton = findViewById(R.id.liLayoutButton);
        txtNumber = findViewById(R.id.txtNumber);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_opencv_image, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        resetLayout();
        switch (item.getItemId()){
            case R.id.selectImage:
                selectImage();
                return true;
            case R.id.rotateImage:
                if(isImageSelect){
                    angle = angle + 90;
                    if(angle >= 360) angle = 0;
                    showImageFromCamera(sudokuSolver.rotateImage(sudokuSolver.getImgOriginal(), angle));
                }
                return true;
            case R.id.drawContour:
                if(isImageSelect){
                    showImageFromCamera(sudokuSolver.drawContour());
                }
                return true;
            case R.id.passPerspective:
                if(isImageSelect){
                    showImageFromCamera(sudokuSolver.rotateImage(sudokuSolver.passToPerspective(sudokuSolver.getImgOriginal(), sudokuSolver.bigContour, 1650, 750), angle));
                }
                return true;
            case R.id.detectNumber:
                if(isImageSelect){
                    prepareLayoutForDetectNumber();
                    detectNumber();
                }
                return true;
            case R.id.detectBorder:
                if(isImageSelect){
                    detectBorder();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }



    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                isImageSelect = true;
                showImageSelect(data.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                isImageSelect = false;
            }

        }else {
            isImageSelect = false;
            Toast.makeText(this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    private void resetLayout(){
        txtNumber.setVisibility(View.GONE);
        liLayoutButton.setVisibility(View.GONE);
    }

    public void showImageFromCamera(Mat result) {
        final Bitmap bm = Bitmap.createBitmap(result.cols(), result.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bm);
        runOnUiThread(new Runnable() {
            public void run(){
                imageDst.setImageBitmap(bm);
            }
        });
    }

    private void selectImage(){
        imageSrc.setImageResource(android.R.color.transparent);
        imageDst.setImageResource(android.R.color.transparent);
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
    }

    private void showImageSelect(Uri imageUri) throws FileNotFoundException {
        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
        imageSrc.setImageBitmap(selectedImage);
        rgbA = new Mat();
        Utils.bitmapToMat(selectedImage.copy(Bitmap.Config.ARGB_8888, true), rgbA);
        sudokuSolver = new SudokuSolver(rgbA, getApplicationContext());
        //sudokuSolver.imageProcessing();
    }

    private void prepareLayoutForDetectNumber(){
        txtNumber.setVisibility(View.VISIBLE);
        liLayoutButton.setVisibility(View.VISIBLE);
    }

    private void detectNumber(){
       // sudokuSplits = sudokuSolver.splitSudoku(sudokuSolver.rotateImage(sudokuSolver.passToPerspective(sudokuSolver.getImgOriginal(), sudokuSolver.bigContour, 1650, 750), angle));

        sudokuSplits = sudokuSolver.splitSudoku(sudokuSolver.getImgOriginal());
        iteradorI = 0; iteradorJ = 0;

        detectNumberOfImage();
    }

    public void onclick_btnPrevious(View view){
        iteradorJ --;
        if(iteradorJ < 0){
            if(iteradorI == 0){
                iteradorI = 8; iteradorJ = 8;
            }else{
                iteradorI --; iteradorJ = 8;
            }
        }
        detectNumberOfImage();
    }

    public void onclick_btnNext(View view){
        iteradorJ ++;
        if(iteradorJ > 8){
            if(iteradorI == 8){
                iteradorI = 0; iteradorJ = 0;
            }else{
                iteradorI ++; iteradorJ = 0;
            }
        }
        detectNumberOfImage();
    }


    private void detectNumberOfImage(){
        try{

            Mat image = sudokuSplits[iteradorI][iteradorJ].clone();
            image = sudokuSolver.getImageThreshold_2(image);
            showImageFromCamera(image);
            txtNumber.setText(String.valueOf(sudokuSolver.getTextWithTesseract(sudokuSolver.rotateImage((image), angle))));
            //txtNumber.setText(String.valueOf(sudokuSolver.getTextWithTesseract(sudokuSolver.getImageThreshold())));
        }catch (Exception e){
            e.printStackTrace();
            txtNumber.setText("?");
        }
    }


    private void detectBorder() {

        Mat img = sudokuSolver.getImageThreshold(sudokuSolver.getImgOriginal());
        FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
        sudokuSolver.checkParamFile();
        featureDetector.read(new File(getFilesDir() + "/tessdata/blod.xml").getPath());


        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        featureDetector.detect(img, keyPoints);
        Mat drawImage = sudokuSolver.getImgOriginal().clone();
        for (int i = 0; i < keyPoints.toArray().length; ++i)
            Imgproc.circle(drawImage, keyPoints.toArray()[i].pt, 10, new Scalar(255, 0, 255), -1);

        showImageFromCamera(drawImage);

        /*
        Mat img = sudokuSolver.getImageThreshold(sudokuSolver.getImgOriginal());

        List<MatOfPoint> generalContours = sudokuSolver.findContour(img);
        List<MatOfPoint> squads = sudokuSolver.filterContoursSquad(generalContours);
        MatOfPoint borderContour= null;
        List<MatOfPoint> circles;
        Mat borderImage;
        //showImageFromCamera(tmpImage);
        for(MatOfPoint contour : squads){
            borderImage = sudokuSolver.passToPerspective(img, contour, 500, 500);
            circles = sudokuSolver.filterContoursCircle(sudokuSolver.findContour(borderImage));
            if(circles.size() >= 3) borderContour = contour;
        }

        borderImage = sudokuSolver.passToPerspective(sudokuSolver.getImgOriginal(), borderContour, 500, 500);


        Mat image = sudokuSolver.getImageThreshold(borderImage);
        List<MatOfPoint> tt = sudokuSolver.filterContoursCircle(sudokuSolver.findContour(image));

        //borderImage = getCorrectImageRotate(borderImage);

        showImageFromCamera(sudokuSolver.drawContours(borderImage, tt));
        */

/*
        InputStream ims;
        Bitmap bitmap;
        try {
            ims = getAssets().open("fea.png");
            bitmap = BitmapFactory.decodeStream(ims);
            Mat im = new Mat();
            Utils.bitmapToMat(bitmap.copy(Bitmap.Config.ARGB_8888, true), im);
            FindMatching findMatching = new FindMatching(im);

            showImageFromCamera(findMatching.detectFeature(sudokuSolver.getImgOriginal()));
        } catch (IOException e) {
            // handle exception
        }
*/
    }

    public Mat getCorrectImageRotate(Mat original){
        Mat image = sudokuSolver.getImageThreshold(original);
        int angle;
        for(angle = 0; angle <= 360; angle = angle + 90){
            image = sudokuSolver.rotateImage(image, (double) angle);
            List<MatOfPoint> circles = sudokuSolver.filterContoursCircle(sudokuSolver.findContour(image));
            boolean left = false, top = false;
            for(MatOfPoint circle : circles){
                if(circles.get(0).toArray()[0].x - circle.toArray()[0].x < 500)
                    left = true;
                if(circles.get(0).toArray()[0].y - circle.toArray()[0].y < 500)
                    top = true;
            }
            if(left && top){
                return sudokuSolver.rotateImage(original, (double) angle);
            }
        }
        CameraDevice s;

        return sudokuSolver.rotateImage(original, (double) angle);
    }

}
