package com.xyxxt.testopencv;

import android.content.Context;
import android.graphics.Bitmap;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by XYxXT on 09/05/2018.
 */

public class SudokuSolver {
    private static final String DATA_PATH = "tessdata";
    ////Dataset mnist

    private Mat imgOriginal;
    private final float SUDOKU_HEIGHT = 500;
    private final float SUDOKU_WIDTH = 500;
    public MatOfPoint bigContour;
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
        List<MatOfPoint> contours = findContour(imgOriginal);

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

    public List<MatOfPoint> findContour(Mat image){
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(
                image,
                contours,
                new Mat(),
                Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE
        );


        return contours;
    }

    private MatOfPoint aproxPolygon(MatOfPoint poly) {

        MatOfPoint2f dst = new MatOfPoint2f();
        MatOfPoint2f src = new MatOfPoint2f();
        MatOfPoint result = new MatOfPoint();
        poly.convertTo(src, CvType.CV_32FC2);

        double arcLength = Imgproc.arcLength(src, true);
        Imgproc.approxPolyDP(src, dst, 0.01 * arcLength, true);
        dst.convertTo(result, CvType.CV_32S);
        return result;
    }

    public List<MatOfPoint> filterContoursSquad(List<MatOfPoint> contours){
        List<MatOfPoint> newList = new ArrayList<>();
        contours.sort(new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint t0, MatOfPoint t1) {
                if(Imgproc.contourArea(t0) > Imgproc.contourArea(t1))
                    return 1;
                else return 0;
            }
        });

        for(MatOfPoint contour : contours){
            MatOfPoint tmp = aproxPolygon(contour);
            if(tmp .toArray().length == 4 && Imgproc.contourArea(tmp) > 500)
                newList.add(tmp);
        }

        return newList;
    }

    public List<MatOfPoint> filterContoursCircle(List<MatOfPoint> contours){
        List<MatOfPoint> newList = new ArrayList<>();
        for(MatOfPoint contour : contours){
            MatOfPoint tmp = aproxPolygon(contour);
            if(tmp .toArray().length > 8 && tmp .toArray().length < 24 && Imgproc.contourArea(tmp) > 1000)
                newList.add(tmp);
        }
        return newList;
    }

    public Mat getImageThreshold(Mat img){
        Mat imgTransform = new Mat(img.rows(),img.cols(), CvType.CV_8UC1);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        //convert to grayscale
        Imgproc.cvtColor(img, imgTransform, Imgproc.COLOR_BGR2GRAY);

        //Imgproc.morphologyEx(imgTransform, imgTransform, Imgproc.MORPH_CLOSE, element);
        //Imgproc.morphologyEx(imgTransform, imgTransform, Imgproc.MORPH_OPEN, element);

        Imgproc.GaussianBlur(imgTransform, imgTransform, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(imgTransform, imgTransform, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
        return imgTransform;
    }

    public Mat getImageThreshold_2(Mat img){
        Mat imgTransform = new Mat(img.rows(),img.cols(), CvType.CV_8UC1);
        Imgproc.resize(img, imgTransform, new Size(200,200));
        //convert to grayscale
        Imgproc.cvtColor(img, imgTransform, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(imgTransform, imgTransform, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);

        Imgproc.morphologyEx(imgTransform, imgTransform, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
        Imgproc.morphologyEx(imgTransform, imgTransform, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));

        Imgproc.GaussianBlur(imgTransform, imgTransform, new Size(5, 5), 0);

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

    public Mat drawContours(Mat image, List<MatOfPoint> coutours){
        Mat imgResult = image.clone();
        for(MatOfPoint contour : coutours){
            Imgproc.drawContours(imgResult, Collections.singletonList(contour), -1, new Scalar(255, 0, 0), 4);
        }
        return imgResult;
    }

    // Pass to perspective
    public Mat passToPerspective(Mat image, MatOfPoint contour, int width, int height){
        Mat imgResult = image.clone(); //imgOriginal.clone();
        if(contour != null){
            List<Point> pointList = new ArrayList<>();
            Collections.addAll(pointList, contour.toArray());
            Mat src_mat  = Converters.vector_Point2f_to_Mat(pointList);

            Mat dst_mat = new Mat(4,1,CvType.CV_32FC2);
            dst_mat.put(0,0,0,0, width, 0, width, height, 0,height);

            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
            Imgproc.warpPerspective(imgResult, imgResult, perspectiveTransform, new Size(width, height));
        }

        //Imgproc.morphologyEx(imgResult, imgResult, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));

        Core.flip(imgResult, imgResult, 1);
        return imgResult;
    }

    public Mat[][] splitSudoku(Mat origin){
        Mat squads[][] = new Mat[9][9];
        int squadHeight = (int) (SUDOKU_HEIGHT / 9);
        int squadWidth = (int) (SUDOKU_WIDTH / 9);
        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j ++){
                squads[i][j] = new  Mat(origin,  new Rect(i * squadWidth + 8,  j *squadHeight + 8 ,squadWidth - 14, squadHeight - 14));
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
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
        tessBaseAPI.setImage(bm);
        String retStr = tessBaseAPI.getUTF8Text();
        tessBaseAPI.end();
        return retStr;
    }

    public Mat drawCircle(Mat im){
        Mat circles = new Mat();
        Imgproc.HoughCircles(im, circles, Imgproc.HOUGH_GRADIENT, 2,
                (double)im.rows()/4, // change this value to detect circles with different distances to each other
                200, 100, 30, 100); // change the last two parameters
        // (min_radius & max_radius) to detect larger circles
        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));
            // circle center
            Imgproc.circle(im, center, 1, new Scalar(0,100,100), 3, 8, 0 );
            // circle outline
            int radius = (int) Math.round(c[2]);
            Imgproc.circle(im, center, radius, new Scalar(255,0,255), 3, 8, 0 );
        }

        return im;
    }


    public void checkParamFile(){
        try{

            File dir = new File(context.getFilesDir(), DATA_PATH);
            if(!dir.exists()){
                dir.mkdirs();
            }
            //if(!(new File(context.getFilesDir() + "/" + DATA_PATH, "blod.xml")).exists()){
                InputStream is = context.getAssets().open("blod.xml");
                OutputStream os = new FileOutputStream(context.getFilesDir() + "/" + DATA_PATH + "/" + "blod.xml");
                byte [] buff = new byte[1024];
                int len;
                while((len = is.read(buff))>0){
                    os.write(buff,0,len);
                }
                is.close();
                os.close();
            //d}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
