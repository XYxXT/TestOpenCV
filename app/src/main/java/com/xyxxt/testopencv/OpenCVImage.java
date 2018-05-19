package com.xyxxt.testopencv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import org.opencv.core.MatOfPoint;

import java.io.FileNotFoundException;
import java.io.IOException;
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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
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
                    showImageFromCamera(sudokuSolver.rotateImage(sudokuSolver.passToPerspective(sudokuSolver.getImgOriginal(), sudokuSolver.bigContour), angle));
                }
                return true;
            case R.id.detectNumber:
                if(isImageSelect){
                    prepareLayoutForDetectNumber();
                    detectNumber();
                }
                return true;
            case R.id.detectFeature:
                if(isImageSelect){
                    detectFeature();
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
        sudokuSolver.imageProcessing();
    }

    private void prepareLayoutForDetectNumber(){
        txtNumber.setVisibility(View.VISIBLE);
        liLayoutButton.setVisibility(View.VISIBLE);
    }

    private void detectNumber(){
        sudokuSplits = sudokuSolver.splitSudoku(sudokuSolver.rotateImage(sudokuSolver.passToPerspective(sudokuSolver.getImgOriginal(), sudokuSolver.bigContour), angle));
        iteradorI = 0; iteradorJ = 0;
        showImageFromCamera(sudokuSplits[iteradorI][iteradorJ]);
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
        showImageFromCamera(sudokuSplits[iteradorI][iteradorJ]);
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
        showImageFromCamera(sudokuSplits[iteradorI][iteradorJ]);
        detectNumberOfImage();
    }


    private void detectNumberOfImage(){
        try{
            txtNumber.setText(String.valueOf(sudokuSolver.getTextWithTesseract(sudokuSolver.rotateImage((sudokuSplits[iteradorI][iteradorJ]), angle))));
            //txtNumber.setText(String.valueOf(sudokuSolver.getTextWithTesseract(sudokuSolver.getImageThreshold())));
        }catch (Exception e){
            e.printStackTrace();
            txtNumber.setText("?");
        }
    }

    private void detectFeature() {
        InputStream ims;
        Bitmap bitmap;
        try {
            ims = getAssets().open("feature.png");
            bitmap = BitmapFactory.decodeStream(ims);
            Mat im = new Mat();
            Utils.bitmapToMat(bitmap.copy(Bitmap.Config.ARGB_8888, true), im);

            //showImageFromCamera(sudokuSolver.getImageThreshold(sudokuSolver.getImgOriginal()));

          //  showImageFromCamera(sudokuSolver.drawContours(sudokuSolver.getImgOriginal(), sudokuSolver.findContour(sudokuSolver.getImgOriginal())));
           // showImageFromCamera(sudokuSolver.rotateImage(sudokuSolver.passToPerspective(sudokuSolver.getImgOriginal(), sudokuSolver.findContour(sudokuSolver.getImgOriginal()).get(0)), angle));

            //FindMatching findMatching = new FindMatching(im);


            List<MatOfPoint> contours = sudokuSolver.findContour(sudokuSolver.getImgOriginal());

            List<MatOfPoint> squads = sudokuSolver.filterContoursSquad(contours);
            List<MatOfPoint> circles = sudokuSolver.filterContoursCircle(contours);

             showImageFromCamera(sudokuSolver.drawContours(sudokuSolver.getImgOriginal(), circles));

            //showImageFromCamera(sudokuSolver.drawCircle(sudokuSolver.rotateImage(sudokuSolver.passToPerspective(sudokuSolver.getImgOriginal(), squads.get(0)), angle)));

        } catch (IOException e) {
            // handle exception
        }
    }

}
