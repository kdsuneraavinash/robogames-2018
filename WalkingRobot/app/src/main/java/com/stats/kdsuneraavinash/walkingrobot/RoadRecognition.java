package com.stats.kdsuneraavinash.walkingrobot;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

class RoadRecognition {
    private int numberOfDetectedRoads = 0;
    private double maxWidthOfRoads = Double.NEGATIVE_INFINITY;
    private boolean isCompletelyDeviated = false;

    private static final Scalar MAT_RED = new Scalar(198, 40, 40);
    private static final Scalar MAT_BLUE = new Scalar(48, 63, 159);
    private static final Scalar MAT_YELLOW = new Scalar(255, 234, 0);
    private static final Scalar MAT_L_GREEN = new Scalar(156, 204, 101);
    private static final Scalar MAT_GREY = new Scalar(38, 50, 56);

    private Point previouslyIdentifiedPoint = new Point(0, 0);

    // Setting - Roads which are thinner than this will be neglected
    private double smallRoadFilter = 600.0;
    // Setting - Forward will flip to Right or Left after this much deviation
    private int centerMaxDeviation = 100;

    private double deviation = 0;
    private boolean canBeCircle = false;

    boolean isCanBeCircle() {
        return canBeCircle;
    }

    double getMidPointDeviation() {
        return deviation;
    }

    double getSmallRoadFilter() {
        return smallRoadFilter;
    }

    int getCenterMaxDeviation() {
        return centerMaxDeviation;
    }

    void setCenterMaxDeviation(int centerMaxDeviation) {
        this.centerMaxDeviation = centerMaxDeviation;
    }

    void setSmallRoadFilter(double smallRoadFilter) {
        this.smallRoadFilter = smallRoadFilter;
    }

    private double getMidPoint(Mat camImage) {
        // Define ROI parameters
        Rect rectROI = new Rect(camImage.cols() / 3, 10, camImage.cols() / 6, camImage.rows() - 20);

        // Get only the region of interest
        Mat regionOfInterest = new Mat(camImage, rectROI);

        // Grey-scale image
        Mat monoColorROI = new Mat();
        Imgproc.cvtColor(regionOfInterest, monoColorROI, Imgproc.COLOR_RGBA2GRAY);

        // Put gaussian blur to image
        Mat blurredROI = new Mat();
        Size blurSize = new Size(9, 9);
        Imgproc.GaussianBlur(monoColorROI, blurredROI, blurSize, 2, 2);

        // Threshold image
        Mat threshedROI = new Mat();
        Imgproc.threshold(blurredROI, threshedROI, 0, 255, Imgproc.THRESH_OTSU);

        // Apply morphological transformations
        Mat transformedROI = new Mat();
        Mat morphKernel = new Mat(new Size(3, 3), CvType.CV_8UC1, new Scalar(255));
        Imgproc.morphologyEx(threshedROI, transformedROI, Imgproc.MORPH_OPEN, morphKernel);
        Imgproc.morphologyEx(transformedROI, transformedROI, Imgproc.MORPH_CLOSE, morphKernel);

        // Get contours
        Mat notUsed = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(transformedROI, contours, notUsed, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Change ROI - draw all contours and morphed
        Imgproc.cvtColor(transformedROI, regionOfInterest, Imgproc.COLOR_GRAY2RGBA);
        Imgproc.drawContours(regionOfInterest, contours, -1, MAT_BLUE, 2);

        // Define min max using bias
        // double minMaxCy = (bias > 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        Point bestMidPoint = new Point(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

        numberOfDetectedRoads = 0;
        maxWidthOfRoads = Double.NEGATIVE_INFINITY;
        double smallestError = Double.POSITIVE_INFINITY;
        for (MatOfPoint cont : contours) {
            Moments mu = Imgproc.moments(cont, false);
            if (mu.get_m00() > smallRoadFilter) {
                Rect boundingRect = Imgproc.boundingRect(cont);
                numberOfDetectedRoads++;

                // Draw bounding rectangle
                Imgproc.rectangle(regionOfInterest,
                        new Point(boundingRect.x, boundingRect.y),
                        new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                        MAT_YELLOW);

                Point point = new Point(mu.get_m10() / mu.get_m00(), mu.get_m01() / mu.get_m00());

                double error = Math.abs(previouslyIdentifiedPoint.y - point.y);
                // Draw identified contour
                Imgproc.putText(regionOfInterest, "E: " + Math.round(error), point,
                        Core.FONT_HERSHEY_COMPLEX, 0.5, MAT_GREY);
                if (error < smallestError) {
                    bestMidPoint = point;
                    smallestError = error;
                }

                if (boundingRect.height > maxWidthOfRoads) {
                    //noinspection SuspiciousNameCombination
                    maxWidthOfRoads = boundingRect.height;
                    isCompletelyDeviated = Math.abs((2*boundingRect.y + boundingRect.height) / 2 - regionOfInterest.rows()/2) > 100;
                }
            }
        }

        if (Double.isInfinite(bestMidPoint.y)) {
            bestMidPoint = new Point(rectROI.width / 2, rectROI.height / 2);
        }

        Imgproc.circle(regionOfInterest, bestMidPoint, 10, MAT_RED, -1);

        previouslyIdentifiedPoint = bestMidPoint;
        return regionOfInterest.rows() / 2 - bestMidPoint.y;
    }

    Mat process(Mat colorImage) {
        canBeCircle = false;
        deviation = getMidPoint(colorImage);
        String command;
        if (deviation < -centerMaxDeviation) {
            command = "LEFT: " + Math.round(-deviation * 200 / colorImage.width()) + " %";
        } else if (deviation > centerMaxDeviation) {
            command = "RIGHT: " + Math.round(deviation * 200 / colorImage.width()) + " %";
        } else {
            command = "FORWARD";
        }
        Imgproc.putText(colorImage, command, new Point(100, 100), Core.FONT_HERSHEY_COMPLEX, 2, MAT_YELLOW, 4);
        Imgproc.putText(colorImage, "DETECTED ROADS=: " + numberOfDetectedRoads, new Point(100, 150),
                Core.FONT_HERSHEY_COMPLEX, 1, MAT_L_GREEN);
        Imgproc.putText(colorImage, "MAX W ROAD: " + maxWidthOfRoads, new Point(100, 200),
                Core.FONT_HERSHEY_COMPLEX, 1, MAT_L_GREEN);
        if (maxWidthOfRoads > 900 && !isCompletelyDeviated) {
            canBeCircle = true;
            Imgproc.putText(colorImage, "CIRCLE", new Point(100, 250), Core.FONT_HERSHEY_COMPLEX, 1.5, MAT_YELLOW, 4);
        }
        return colorImage;
    }
}
