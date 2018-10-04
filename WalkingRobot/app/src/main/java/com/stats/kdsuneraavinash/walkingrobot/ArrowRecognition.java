package com.stats.kdsuneraavinash.walkingrobot;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CV_HOUGH_GRADIENT;

class ArrowRecognition {

    private double angle = 0.0;
    private static final Size GAUSSIAN_BLUR_KERNEL = new Size(5, 5);
    // private static final Mat MORPH_KERNEL = new Mat(new Size(3, 3), CvType.CV_8UC1, new Scalar(255));

    Mat process(Mat colorImage) {
        Mat grayImage = new Mat();
        Mat blurredImage = new Mat();
        Mat threshedImage = new Mat();
        // Mat morphedImage = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint arrowContour = new MatOfPoint();

        // gray-scale image
        Imgproc.cvtColor(colorImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Otsu's threshing after Gaussian filtering
        Imgproc.GaussianBlur(grayImage, blurredImage, GAUSSIAN_BLUR_KERNEL, 0);
        Imgproc.threshold(blurredImage, threshedImage, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        Mat edges = new Mat();
        int lowThreshold = 40;
        int ratio = 3;
        Imgproc.Canny(threshedImage, edges, lowThreshold, lowThreshold * ratio);

        Mat circles = new Mat();
        Imgproc.HoughCircles(edges, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 1, 200, 20, 300, 350 );


        /// Draw the circles detected
        for (int i = 0; i < circles.cols() && i<10; i++) {
            double x;
            double y;
            int r;
            double[] data = circles.get(0, i);
            System.out.println(data[0] + "  " + data[1]);
            x = data[0];
            y = data[1];
            r = (int) data[2];
            Point center = new Point(x, y);
            // circle center
            Imgproc.circle(colorImage, center, 3, new Scalar(0, 255, 0), -1);
            // circle outline
            Imgproc.circle(colorImage, center, r, new Scalar(0, 0, 255), 1);
        }

        /*
        // MORPHING IS SLOW
        // Open and close so that all small gaps are removed
        Imgproc.morphologyEx(threshedImage, morphedImage, Imgproc.MORPH_OPEN, MORPH_KERNEL);
        Imgproc.morphologyEx(morphedImage, morphedImage, Imgproc.MORPH_CLOSE, MORPH_KERNEL);
        */

//        // Find contours in image
//        Imgproc.findContours(threshedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//        double maxArea = Double.NEGATIVE_INFINITY;
//        for (MatOfPoint contour : contours) {
//            double area = Imgproc.contourArea(contour);
//            // Filter only the needed contour
//            if (area > maxArea) {
//                maxArea = area;
//                arrowContour = contour;
//            }
//        }
//        List<Point> arrowContourPoints = arrowContour.toList();
//
//        if (arrowContour.empty()) {
//            return colorImage;
//        }
//
//        // Find bottom most point and top most point
//        Point minPoint = arrowContourPoints.get(0);
//        Point maxPoint = arrowContourPoints.get(0);
//        for (Point point : arrowContourPoints) {
//            if (point.y < minPoint.y) {
//                minPoint = point;
//            }
//            if (point.y > maxPoint.y) {
//                maxPoint = point;
//            }
//        }
//
//        Point p = minPoint;
//        Point q = maxPoint;
//
//        // Find the furthest point from p and q by finding the point r which maximizes
//        // the area of the triangle pqr
//        double maxAreaOfTriangle = 0;
//        Point r = p;
//        for (Point point : arrowContourPoints) {
//            double areaOfTriangle = 0.5 * Math.abs((p.x - point.x) * (q.y - p.y) - (p.x - q.x) * (point.y - p.y));
//            if (maxAreaOfTriangle < areaOfTriangle) {
//                maxAreaOfTriangle = areaOfTriangle;
//                r = point;
//            }
//        }
//
//        Point[] pointsInOrder = findCorrectCombination(p, q, r, arrowContour);
//        p = pointsInOrder[0];
//        q = pointsInOrder[1];
//        r = pointsInOrder[2];
//
//        double cx = (p.x + q.x) / 2;
//        double cy = (p.y + q.y) / 2;
//        Point c = new Point(cx, cy);
//
//        if (c.x == r.x) {
//            angle = 90.0;
//        } else {
//            angle = Math.rint(Math.toDegrees(Math.atan((r.y - c.y) / (r.x - c.x))));
//        }
//
//        if (c.x < r.x){
//            if (c.y < r.y){
//                angle = -180 + angle;
//            }else{
//                angle = 180 + angle;
//            }
//        }
//
//        Imgproc.circle(colorImage, p, 5, new Scalar(255, 255, 255), 1);
//        Imgproc.circle(colorImage, q, 5, new Scalar(255, 255, 255), 1);
//        Imgproc.circle(colorImage, r, 5, new Scalar(255, 255, 255), 1);
//        Imgproc.line(colorImage, c, r, new Scalar(255, 0, 0));
//        Imgproc.circle(colorImage, r, 5, new Scalar(255, 0, 0), -1);
//        Imgproc.putText(colorImage, "" + angle, r, Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(0, 255, 0));

        return colorImage;
    }

    private int countBlackPoints(MatOfPoint2f contour, Point a) {
        int pointsInsideContour = 0;
        int w = 5;
        for (int i = (int) a.x - w; i < a.x + w; i++) {
            for (int j = (int) a.y - w; j < a.y + w; j++) {
                try {
                    if (Imgproc.pointPolygonTest(contour, new Point(i, j), false) == 1) {
                        pointsInsideContour++;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return pointsInsideContour;
    }

    private Point[] findCorrectCombination(Point p, Point q, Point r, MatOfPoint arrowContour) {
        Point[][] combinations = {
                {p, q, r},
                {p, r, q},
                {q, r, p},
        };
        MatOfPoint2f contour2f = new MatOfPoint2f(arrowContour.toArray());
        Point[] correctCombination = null;
        int leastBlackPoints = Integer.MAX_VALUE;
        for (Point[] combination : combinations) {
            double cx = (combination[0].x + combination[1].x) / 2;
            double cy = (combination[0].y + combination[1].y) / 2;
            Point c = new Point(cx, cy);
            int blackPoints = countBlackPoints(contour2f, c);
            if (blackPoints < leastBlackPoints) {
                leastBlackPoints = blackPoints;
                correctCombination = combination;
            }
        }
        return correctCombination;
    }

    double getAngle() {
        return angle;
    }
}

