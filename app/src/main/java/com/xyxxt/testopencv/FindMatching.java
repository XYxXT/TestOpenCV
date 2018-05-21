package com.xyxxt.testopencv;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

public class FindMatching {
    private static final int MIN_MATCH_COUNT = 10;
    private Mat queryImage, descriptorsQueryImage;
    private FeatureDetector  featureDetector;
    private DescriptorExtractor descriptorExtractor;
    private MatOfKeyPoint keypointsQueryImage;

    public FindMatching(Mat queryImage){
        Imgproc.cvtColor(queryImage, queryImage, Imgproc.COLOR_BGR2GRAY);
        this.queryImage = queryImage;
        this.descriptorsQueryImage = new Mat();
        featureDetector = FeatureDetector.create(FeatureDetector.ORB);
        descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        keypointsQueryImage = new MatOfKeyPoint();

        featureDetector.detect(queryImage, keypointsQueryImage);
        descriptorExtractor.compute(queryImage, keypointsQueryImage, descriptorsQueryImage);
    }

    public Mat detectFeature(Mat imageTrain){
        Imgproc.cvtColor(imageTrain, imageTrain, Imgproc.COLOR_BGR2GRAY);
        if(keypointsQueryImage.toArray().length > 4){
            Mat descriptor = new Mat();
            MatOfKeyPoint keyPoints = new MatOfKeyPoint();
            featureDetector.detect(imageTrain, keyPoints);
            descriptorExtractor.compute(imageTrain, keyPoints, descriptor);
            if(keyPoints.toArray().length > 4){


                MatOfDMatch matches = new MatOfDMatch();
                DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                matcher.match(descriptorsQueryImage, descriptor, matches);

                List<DMatch> matchesList = matches.toList();
                Double max_dist = 0.0;
                Double min_dist = 100.0;
                for (int i = 0; i < matchesList.size(); i++) {
                    Double dist = (double) matchesList.get(i).distance;
                    if (dist < min_dist)
                        min_dist = dist;
                    if (dist > max_dist)
                        max_dist = dist;
                }

                LinkedList<DMatch> good_matches = new LinkedList<>();
                for (int i = 0; i < matchesList.size(); i++) {
                    if (matchesList.get(i).distance <= (1.5 * min_dist))
                        good_matches.addLast(matchesList.get(i));
                }

                MatOfDMatch gm = new MatOfDMatch();
                gm.fromList(good_matches);
                Mat outputImg = new Mat();
                MatOfByte drawnMatches = new MatOfByte();
                Features2d.drawMatches(queryImage, keypointsQueryImage, imageTrain, keyPoints, gm, outputImg, new Scalar(255, 0, 0)   , new Scalar(0, 255, 0), drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
                Imgproc.resize(outputImg, outputImg, imageTrain.size());

                return outputImg;

            }
        }
        return null;
    }





}
