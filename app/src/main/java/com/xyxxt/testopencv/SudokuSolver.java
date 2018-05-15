package com.xyxxt.testopencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.SparseArray;

import com.googlecode.tesseract.android.TessBaseAPI;

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
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by XYxXT on 09/05/2018.
 */

public class SudokuSolver {
    private static final String DATA_PATH = "tessdata";

    private Mat imgOriginal;
    private final float SUDOKU_HEIGHT = 500;
    private final float SUDOKU_WIDTH = 500;
    private MatOfPoint bigContour;
    private Context context;
    private TessBaseAPI tessBaseAPI;


    public SudokuSolver(Mat imgOriginal, Context context){
        this.imgOriginal = imgOriginal;
        this.context = context;

        prepareTessData();
    }

    public Mat getImgOriginal() {
        return imgOriginal;
    }

    /*
    public MatOfPoint detectSquad(MatOfPoint contour){
        MatOfPoint approxContour = new MatOfPoint();
        MatOfPoint2f approxContour2f = new MatOfPoint2f();
        double peri = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
        Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approxContour2f, 0.1 * peri, true);
        approxContour2f.convertTo(approxContour, CvType.CV_32S);

        Point[] points = approxContour.toArray();

        if(points.length == 4 &&
                Math.abs(Math.abs(points[0].x - points[1].x) - Math.abs(points[2].x - points[3].x)) < 2 &&
                Math.abs(Math.abs(points[0].y - points[2].y) - Math.abs(points[1].y - points[3].y)) < 2){
            return approxContour;
        }
        return null;
    }
    */


    // Detect contour
    public void imageProcessing(){
        // Find contour with max area and squad form
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(
                getImageThreshold(),
                contours,
                new Mat(),
                Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE
        );

        if (contours.size() > 0) {
            MatOfPoint maxContour = contours.get(0);
            MatOfPoint approxContour = new MatOfPoint();
            MatOfPoint2f approxContour2f = new MatOfPoint2f();
            double maxVal = Imgproc.contourArea(maxContour);
            for (MatOfPoint contour : contours) {
                double contourArea = Imgproc.contourArea(contour);
                if (maxVal < contourArea) {
                    double peri = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                    Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approxContour2f, 0.1 * peri, true);
                    approxContour2f.convertTo(approxContour, CvType.CV_32S);
                    if (approxContour.size().height == 4) {
                        maxVal = contourArea;
                    }
                }
            }

            if(approxContour.size().height == 4){
                bigContour = approxContour;
            }
        }
    }

    public Mat getImageThreshold(){
        Mat imgTransform = new Mat(imgOriginal.rows(),imgOriginal.cols(), CvType.CV_8UC1);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        //convert to grayscale
        Imgproc.cvtColor(imgOriginal, imgTransform, Imgproc.COLOR_BGR2GRAY);

        //I have done just noise removal and thresholding. And it is working. So I haven't done anything extra.
        Imgproc.dilate(imgTransform, imgTransform, element);
        Imgproc.erode(imgTransform, imgTransform, element);

        Imgproc.GaussianBlur(imgTransform, imgTransform, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(imgTransform, imgTransform, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        return imgTransform;
    }

    public Mat rotateImage(Mat img, Double angle){
        Point src_center = new Point(img.cols()/2.0F, img.rows()/2.0F);
        Mat rot_mat = Imgproc.getRotationMatrix2D(src_center, - angle, 1);
        Mat dst = new Mat(img.rows(), img.cols(), CvType.CV_8UC1);
        Imgproc.warpAffine(img, dst, rot_mat, img.size());
        return dst;
    }

    // Draw contour and corner
    public Mat drawContour(){
        Mat imgResult = imgOriginal.clone();
        if(bigContour != null){
            Imgproc.drawContours(imgResult, Collections.singletonList(bigContour), -1, new Scalar(255, 0, 0), 4);
            for(Point point : bigContour.toArray()){
                Imgproc.circle(imgResult, point, 30, new Scalar(0,255,0));
            }
        }
        return imgResult;
    }

    // Pass to perspective
    public Mat passToPerspective(){
        Mat imgResult = getImageThreshold(); //imgOriginal.clone();
        if(bigContour != null){
            List<Point> pointList = new ArrayList<>();
            Collections.addAll(pointList, bigContour.toArray());
            Mat src_mat  = Converters.vector_Point2f_to_Mat(pointList);

            Mat dst_mat = new Mat(4,1,CvType.CV_32FC2);
            dst_mat.put(0,0,0,0, SUDOKU_WIDTH, 0, SUDOKU_WIDTH, SUDOKU_HEIGHT, 0,SUDOKU_HEIGHT);

            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
            Imgproc.warpPerspective(imgResult, imgResult, perspectiveTransform, new Size(SUDOKU_WIDTH, SUDOKU_HEIGHT));
        }

        //Core.flip(imgResult, imgResult, 1);
        return imgResult;
    }

    public Mat[][] splitSudoku(){
        Mat squads[][] = new Mat[9][9];
        int squadHeight = (int) (SUDOKU_HEIGHT / 9);
        int squadWidth = (int) (SUDOKU_WIDTH / 9);
        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j ++){
                squads[i][j] = new  Mat(passToPerspective(),  new Rect(i * squadWidth,  j *squadHeight ,squadWidth, squadHeight));
            }
        }
        return squads;
    }

    //  Check tessract data file in assert
    private void prepareTessData(){
        try{
            File dir = new File(context.getFilesDir(), DATA_PATH);
            if(!dir.exists()){
                dir.mkdirs();
            }
            String fileList[] = context.getAssets().list("");
            for(String fileName : fileList){
                if(!(new File(context.getFilesDir() + "/" + DATA_PATH, fileName)).exists()){
                    InputStream is = context.getAssets().open(fileName);
                    OutputStream os = new FileOutputStream(context.getFilesDir() + "/" + DATA_PATH + "/" +fileName);
                    byte [] buff = new byte[1024];
                    int len;
                    while((len = is.read(buff))>0){
                        os.write(buff,0,len);
                    }
                    is.close();
                    os.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTextWithTesseract(Mat result){
        tessBaseAPI = new TessBaseAPI();
        Bitmap bm = Bitmap.createBitmap(result.cols(), result.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bm);
        tessBaseAPI.init(context.getFilesDir() + "/","eng");
        tessBaseAPI.setImage(bm);
        String retStr = tessBaseAPI.getUTF8Text();
        tessBaseAPI.end();
        return retStr;
    }
}
