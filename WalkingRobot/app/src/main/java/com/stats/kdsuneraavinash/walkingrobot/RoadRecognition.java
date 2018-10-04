package com.stats.kdsuneraavinash.walkingrobot;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class RoadRecognition {
    private static final Size GAUSSIAN_BLUR_KERNEL = new Size(5, 5);
    private double slope;
    private double intercept;

    Mat process(Mat colorImage) {
        Mat grayImage = new Mat();
        Mat blurredImage = new Mat();
        Mat threshedImage = new Mat();

        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint roadContour = new MatOfPoint();

        // gray-scale image
        Imgproc.cvtColor(colorImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Otsu's threshing after Gaussian filtering
        Imgproc.GaussianBlur(grayImage, blurredImage, GAUSSIAN_BLUR_KERNEL, 0);
        Imgproc.threshold(blurredImage, threshedImage, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        // Find contours in image
        Imgproc.findContours(threshedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxArea = Double.NEGATIVE_INFINITY;
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            // Filter only the needed contour
            if (area > maxArea) {
                maxArea = area;
                roadContour = contour;
            }
        }

        if (roadContour.empty()) {
            return colorImage;
        }

        List<Point> roadContourPoints = roadContour.toList();

        // Linear Regression and prediction calculation
        // https://enlight.nyc/projects/linear-regression/
        double sumXPoints = 0;
        double sumYPoints = 0;
        double sumXYPoints = 0;
        double sumXXPoints = 0;
        for (int i = 0; i < roadContourPoints.size(); i++) {
            sumXPoints += roadContourPoints.get(i).x;
            sumYPoints += roadContourPoints.get(i).y;
            sumXYPoints += roadContourPoints.get(i).x * roadContourPoints.get(i).y;
            sumXXPoints += roadContourPoints.get(i).x * roadContourPoints.get(i).x;
        }
        double meanXPoints = sumXPoints/roadContourPoints.size();
        double meanYPoints = sumYPoints/roadContourPoints.size();
        double meanXYPoints = sumXYPoints/roadContourPoints.size();
        double meanXXPoints = sumXXPoints/roadContourPoints.size();

        slope = ((meanXPoints*meanYPoints) - meanXYPoints) /
                ((meanXPoints*meanXPoints) - meanXXPoints);

        intercept = meanYPoints - slope*meanXPoints;

        double squaredErrorRegression = 0;
        double squaredErrorYMean = 0;
        for (int i = 0; i < roadContourPoints.size(); i++) {
            // y values of regression line
            double regressionY = slope*roadContourPoints.get(i).x + intercept;
            // squared error of regression line
            squaredErrorRegression += Math.pow(regressionY - roadContourPoints.get(i).y, 2);
            // squared error of the y mean line - horizontal line (mean of y values)
            squaredErrorYMean +=  Math.pow(meanYPoints - roadContourPoints.get(i).y, 2);
        }
        double rSquared = 1 -( squaredErrorRegression/squaredErrorYMean);

        System.out.println(rSquared);

        Imgproc.line(colorImage, new Point(0, intercept), new Point(1000,1000*slope + intercept), new Scalar(255, 0, 0));
        Imgproc.putText(colorImage, "" + rSquared, new Point(100,100*slope + intercept), Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(0, 255, 0));

        return colorImage;
    }

    public double getSlope() {
        return slope;
    }

    public double getIntercept() {
        return intercept;
    }
}
