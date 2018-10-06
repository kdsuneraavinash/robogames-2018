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
    private double maxDetectedWidth = Double.NEGATIVE_INFINITY;
    private boolean canBeCircle = false;

    private static final Scalar MAT_RED = new Scalar(198, 40, 40);
    private static final Scalar MAT_BLUE = new Scalar(48, 63, 159);
    private static final Scalar MAT_YELLOW = new Scalar(255, 234, 0);
    private static final Scalar MAT_L_GREEN = new Scalar(156,204,101);
    private static final Scalar MAT_GREY = new Scalar(38, 50, 56);

    private double getMidPoint(Mat camImage, @SuppressWarnings("SameParameterValue") int bias) {
        // Define ROI parameters
        Rect rectROI = new Rect(10, 2 * camImage.rows() / 3, camImage.cols() - 20, camImage.rows() / 8);

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
        Imgproc.threshold(blurredROI, threshedROI, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

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
        double minMaxCx = (bias > 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);

        numberOfDetectedRoads = 0;
        maxDetectedWidth = Double.NEGATIVE_INFINITY;
        for (MatOfPoint cont : contours) {
            Moments mu = Imgproc.moments(cont, false);
            if (mu.get_m00() > 300.0) {
                Rect boundingRect = Imgproc.boundingRect(cont);
                numberOfDetectedRoads++;

                // Draw bounding rectangle
                Imgproc.rectangle(regionOfInterest,
                        new Point(boundingRect.x, boundingRect.y),
                        new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                        MAT_YELLOW);

                double cx;
                if (bias > 0) {
                    cx = boundingRect.x + boundingRect.width - 12;
                    if (cx > minMaxCx) {
                        minMaxCx = cx;
                    }
                } else {
                    cx = boundingRect.x + 12;
                    if (minMaxCx > cx) {
                        minMaxCx = cx;
                    }
                }

                if (boundingRect.width > maxDetectedWidth) {
                    maxDetectedWidth = boundingRect.width;
                }

                // Draw identified contour
                Imgproc.putText(regionOfInterest, "W: " + boundingRect.width, new Point(mu.get_m10() / mu.get_m00(), mu.get_m01() / mu.get_m00()),
                        Core.FONT_HERSHEY_COMPLEX, 0.5, MAT_GREY);
            }
        }
        if (Double.isInfinite(minMaxCx))
            minMaxCx = regionOfInterest.cols() / 2;

        Imgproc.circle(regionOfInterest, new Point(minMaxCx, rectROI.height / 2), 10, MAT_RED, -1);
        return minMaxCx - regionOfInterest.cols() / 2;
    }

    Mat process(Mat colorImage) {
        double midPoint = getMidPoint(colorImage, -1);
        String command;
        if (midPoint < -10) {
            command = "LEFT WITH POWER: " + Math.round(-midPoint * 200 / colorImage.width()) + " %";
        } else if (midPoint > 10) {
            command = "RIGHT WITH POWER: " + Math.round(midPoint * 200 / colorImage.width()) + " %";
        } else {
            command = "GO FORWARD";
        }
        Imgproc.putText(colorImage, command, new Point(100, 100), Core.FONT_HERSHEY_COMPLEX, 1, MAT_YELLOW);
        Imgproc.putText(colorImage, "DETECTED ROADS: " + numberOfDetectedRoads, new Point(100, 150),
                Core.FONT_HERSHEY_COMPLEX, 1, MAT_L_GREEN);
        Imgproc.putText(colorImage, "MAX W ROAD" + (maxDetectedWidth>300 ? "(CIRCLE?): ": ": ") + maxDetectedWidth, new Point(100, 200),
                Core.FONT_HERSHEY_COMPLEX, 1, MAT_L_GREEN);
        canBeCircle = maxDetectedWidth>300;
        return colorImage;
    }

    boolean isCanBeCircle() {
        return canBeCircle;
    }
}
