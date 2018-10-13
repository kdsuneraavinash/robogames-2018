package com.stats.kdsuneraavinash.walkingrobot;

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
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

class ArrowRecognition {
    // Setting - Min Radius
    private static final int minRadius = 100;
    // Setting - Max Radius
    private static final int maxRadius = 500;
    // Setting - Max Error
    private static final int maxError = 80;

    private static final Scalar MAT_YELLOW = new Scalar(255, 234, 0);
    private static final Scalar MAT_RED = new Scalar(198, 40, 40);
    private static final Scalar MAT_L_BLUE = new Scalar(3, 169, 244);

    private Mat preprocessor(Mat colorImage) {
        // Grey-scale image
        Mat monoImage = new Mat();
        Imgproc.cvtColor(colorImage, monoImage, Imgproc.COLOR_RGBA2GRAY);

        // Blur image
        Mat blurredImage = new Mat();
        Size blurSize = new Size(5, 5);
        Imgproc.GaussianBlur(monoImage, blurredImage, blurSize, 2, 2);

        // Threshold image
        Mat threshedImage = new Mat();
        Imgproc.threshold(blurredImage, threshedImage, 0, 255, Imgproc.THRESH_OTSU);

        // Apply morphological transformations
        Mat transformedImage = new Mat();
        Mat morphKernel = new Mat(new Size(3, 3), CvType.CV_8UC1, new Scalar(255));
        Imgproc.morphologyEx(threshedImage, transformedImage, Imgproc.MORPH_OPEN, morphKernel);
        Imgproc.morphologyEx(transformedImage, transformedImage, Imgproc.MORPH_CLOSE, morphKernel);

        return transformedImage;
    }

    private DetectedCircle detectCircle(Mat preprocessedImage, Mat colorImage) {
        // Find contours in image
        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(preprocessedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        List<DetectedCircle> detectedCircles = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);
            double area = Imgproc.contourArea(contour);

            // Too small
            if (area < 50) continue;

            // Approximate radius using area and perimeter
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double arcLength = Imgproc.arcLength(contour2f, true);
            double radiusByArea = Math.sqrt(area / (Math.PI));
            double radiusByLength = arcLength / (2 * Math.PI);
            double approxRadius = (radiusByArea + radiusByLength) / 2;
            double error = Math.abs(radiusByLength - radiusByArea);

            // Filter out
            if (approxRadius < minRadius || approxRadius > maxRadius || error > maxError) {
                continue;
            }

            Imgproc.drawContours(colorImage, contours, i, MAT_RED, 3);

            // Get center point
            Moments mu = Imgproc.moments(contour);
            Point center = new Point(mu.get_m10() / mu.get_m00(), mu.get_m01() / mu.get_m00());

            // Add circle
            detectedCircles.add(new DetectedCircle(center, approxRadius, error, contour));
        }

        DetectedCircle realCircle = new DetectedCircle();
        for (DetectedCircle detectedCircle : detectedCircles) {
            // Get only smallest circle
            if (detectedCircle.getError() < realCircle.getError()) {
                realCircle = detectedCircle;
            }
        }
        return realCircle;
    }

    private DetectedArrow detectArrow(Mat regionOfInterest) {
        DetectedArrow detectedArrow = new DetectedArrow();

        // Process region of interest
        Mat processedROI = preprocessor(regionOfInterest);
        Imgproc.cvtColor(processedROI, regionOfInterest, Imgproc.COLOR_GRAY2RGBA);

        // Get all contours
        Mat notUsed = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(processedROI, contours, notUsed, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);
            // Get center of arrow
            Moments mu = Imgproc.moments(contour);
            Point arrowCenter = new Point(mu.m10 / mu.m00, mu.m01 / mu.m00);

            // Get distance of contour to circle center, must be positive
            double distanceFromCenterCircle = Imgproc.pointPolygonTest(new MatOfPoint2f(contour.toArray()),
                    new Point(regionOfInterest.cols() / 2, regionOfInterest.rows() / 2), true);

            // If meets constraints
            if (0 <= distanceFromCenterCircle && distanceFromCenterCircle < 100) {
                Rect arrowBounds = Imgproc.boundingRect(contour);
                detectedArrow = new DetectedArrow(arrowCenter, arrowBounds, distanceFromCenterCircle, contour);
            }
        }

        if (detectedArrow.isDefined()) {
            Imgproc.putText(processedROI, "" + Math.round(detectedArrow.getDistFromCircleCenter()), detectedArrow.getCenter(),
                    Core.FONT_HERSHEY_COMPLEX, 0.3, MAT_RED);
            Imgproc.rectangle(processedROI,
                    new Point(detectedArrow.getBoundingBox().x, detectedArrow.getBoundingBox().y),
                    new Point(detectedArrow.getBoundingBox().x + detectedArrow.getBoundingBox().width,
                            detectedArrow.getBoundingBox().y + detectedArrow.getBoundingBox().height),
                    MAT_RED);
        }

        return detectedArrow;
    }

    Mat process(Mat colorImage) {
        Mat preprocessedImage = preprocessor(colorImage);
        DetectedCircle detectedCircle = detectCircle(preprocessedImage, colorImage);

        if (!detectedCircle.isDefined()) {
            Imgproc.putText(colorImage, "NO CIRCLE", new Point(50, 100), Core.FONT_HERSHEY_COMPLEX, 1.0, MAT_YELLOW);
            return colorImage;
        }
        MatOfPoint circleContour = detectedCircle.getContour();
        Rect circleBoundingBox = Imgproc.boundingRect(circleContour);
        Mat regionOfInterest = new Mat(colorImage, circleBoundingBox);

        DetectedArrow detectedArrow = detectArrow(regionOfInterest);
        // Draw details
        Imgproc.circle(colorImage, detectedCircle.getCenter(), 5, MAT_L_BLUE, -1);
        Imgproc.circle(colorImage, detectedCircle.getCenter(), (int) detectedCircle.getRadius(), MAT_L_BLUE, 3);

        if (!detectedArrow.isDefined()) {
            // detection failed
            Imgproc.putText(colorImage, "WRONG CIRCLE? - NO ARROW", new Point(50, 100), Core.FONT_HERSHEY_COMPLEX, 1.0, MAT_YELLOW);
            Imgproc.circle(colorImage, detectedCircle.getCenter(), 10, MAT_RED, -1);
            return colorImage;
        }

        List<Point> arrowContourPoints = detectedArrow.getContour().toList();

        // Find bottom most point and top most point
        Point minPoint = arrowContourPoints.get(0);
        Point maxPoint = arrowContourPoints.get(0);
        for (Point point : arrowContourPoints) {
            if (point.y < minPoint.y) {
                minPoint = point;
            }
            if (point.y > maxPoint.y) {
                maxPoint = point;
            }
        }

        Point p = minPoint;
        Point q = maxPoint;

        // Find the furthest point from p and q by finding the point r which maximizes
        // the area of the triangle pqr
        double maxAreaOfTriangle = 0;
        Point r = p;
        for (Point point : arrowContourPoints) {
            double areaOfTriangle = 0.5 * Math.abs((p.x - point.x) * (q.y - p.y) - (p.x - q.x) * (point.y - p.y));
            if (maxAreaOfTriangle < areaOfTriangle) {
                maxAreaOfTriangle = areaOfTriangle;
                r = point;
            }
        }

        Point[] pointsInOrder = findCorrectCombination(p, q, r, detectedArrow.getContour());
        p = pointsInOrder[0];
        q = pointsInOrder[1];
        r = pointsInOrder[2];

        double cx = (p.x + q.x) / 2;
        double cy = (p.y + q.y) / 2;
        Point c = new Point(cx, cy);

        double angle;
        if (c.y == r.y) {
            angle = -90.0;
        } else {
            angle = Math.rint(Math.toDegrees(Math.atan((r.x - c.x) / (r.y - c.y))));
        }

        if (c.y < r.y) {
            if (c.x < r.x) {
                // 4 circle
                angle = -180 + angle;
            } else {

                // 3 circle
                angle = 180 + angle;
            }
        }

        if (c.y > r.y){
            if (c.x > r.x){
                angle=90-angle;
            }else{
                angle=90-angle;
            }
        }else{
            if (c.x > r.x){
                angle=90-angle;
            }else{
                angle=270+ angle;
            }
        }


        Imgproc.circle(regionOfInterest, p, 2, MAT_YELLOW, 3);
        Imgproc.circle(regionOfInterest, q, 2, MAT_YELLOW, 3);
        Imgproc.circle(regionOfInterest, r, 2, MAT_YELLOW, 3);
        Imgproc.line(regionOfInterest, c, r, MAT_RED);
        Imgproc.circle(regionOfInterest, r, 5, MAT_RED, -1);
        String angleText;
        if (angle>20){
            angleText = "ANGLE: COUNTER CLOCKWISE " + angle;
        }else if(angle<20){
            angleText = "ANGLE: CLOCKWISE " + angle;
        }else{
            angleText = "ANGLE: FORWARD";
        }
        Imgproc.putText(colorImage, angleText, new Point(50, 100), Core.FONT_HERSHEY_COMPLEX, 1.0, MAT_YELLOW);

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
}

class DetectedArrow {
    private Point center;
    private Rect boundingBox;
    private double distFromCircleCenter;
    private MatOfPoint contour;

    Point getCenter() {
        return center;
    }

    Rect getBoundingBox() {
        return boundingBox;
    }

    double getDistFromCircleCenter() {
        return distFromCircleCenter;
    }

    DetectedArrow(Point center, Rect boundingBox, double distFromCircleCenter, MatOfPoint contour) {
        this.center = center;
        this.boundingBox = boundingBox;
        this.distFromCircleCenter = distFromCircleCenter;
        this.contour = contour;
    }

    DetectedArrow() {
        this.center = null;
        this.boundingBox = null;
        this.distFromCircleCenter = 0.0;
        this.contour = null;
    }

    MatOfPoint getContour() {
        return contour;
    }

    boolean isDefined() {
        return this.center != null;
    }
}

class DetectedCircle {
    private Point center;
    private double radius;
    private double error;
    private MatOfPoint contour;

    Point getCenter() {
        return center;
    }

    double getRadius() {
        return radius;
    }

    DetectedCircle(Point center, double radius, double error, MatOfPoint contour) {
        this.center = center;
        this.radius = radius;
        this.error = error;
        this.contour = contour;
    }

    DetectedCircle() {
        this.center = null;
        this.radius = 0.0;
        this.error = Double.POSITIVE_INFINITY;
        this.contour = null;
    }

    boolean isDefined() {
        return center != null && (center.x != 0.0 && center.y != 0.0);
    }

    double getError() {
        return error;
    }

    MatOfPoint getContour() {
        return contour;
    }
}