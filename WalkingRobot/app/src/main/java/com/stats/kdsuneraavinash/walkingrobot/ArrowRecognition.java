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

    private double angle = 0.0;

    private static final Scalar MAT_YELLOW = new Scalar(255, 234, 0);
    private static final Scalar MAT_RED = new Scalar(198, 40, 40);
    private static final Scalar MAT_L_BLUE = new Scalar(3,169,244);
    // private static final Mat MORPH_KERNEL = new Mat(new Size(3, 3), CvType.CV_8UC1, new Scalar(255));

    private DetectedCircle detectCircle(Mat preprocessedImage, Mat coloredImage) {
        // Get canny edges
        Mat edgesImage = new Mat();
        Imgproc.Canny(preprocessedImage, edgesImage, 5, 50, 3);

        // Put gaussian blur to image
        Mat blurredROI = new Mat();
        Size blurSize = new Size(3, 3);
        Imgproc.GaussianBlur(edgesImage, blurredROI, blurSize, 2, 2);


        // Change ROI - draw all contours and morphed
        Imgproc.cvtColor(edgesImage, coloredImage, Imgproc.COLOR_GRAY2RGBA);

        Mat circles = new Mat();
        Imgproc.HoughCircles(blurredROI, circles, Imgproc.CV_HOUGH_GRADIENT, 1.0, 5.0, 30.0, 100.0);

        /// Draw the circles detected
        int circleIndex = 0;
        while (circles.cols() > circleIndex) {
            double x;
            double y;
            int r;
            double[] data = circles.get(circleIndex, 0);
            System.out.println(data[0] + "  " + data[1]);
            x = data[0];
            y = data[1];
            r = (int) data[2];
            Point center = new Point(x, y);
            if (r < 100) {
                return new DetectedCircle(center, r);
            }
            circleIndex++;
        }

        return new DetectedCircle();

    }

    private DetectedArrow detectArrow(Mat preprocessedImage, DetectedCircle detectedCircle) {
      DetectedArrow   detectedArrow = new DetectedArrow();

        // Find contours in image
        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(preprocessedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);

            // Get center of arrow
            Moments mu = Imgproc.moments(contour);
            Point arrowCenter = new Point(mu.m10 / mu.m00, mu.m01 / mu.m00);

            // Get distance of contour to circle center, must be positive
            double distanceFromCenterCircle = Imgproc.pointPolygonTest(new MatOfPoint2f(contour.toArray()),
                    detectedCircle.getCenter(), true);

            // If meets constraints
            if (0 < distanceFromCenterCircle && distanceFromCenterCircle < 15) {
                Rect arrowBounds = Imgproc.boundingRect(contour);

                // Skip if not min
                if (detectedArrow.getCenter() != null) {
                    if (detectedArrow.getBoundingBox().width > arrowBounds.width) {
                        // Get only minimum width one
                        continue;
                    }
                }

                // Detect
                detectedArrow = new DetectedArrow(arrowCenter, arrowBounds, distanceFromCenterCircle, contour, i);
            }
        }
        return  detectedArrow;
    }

    Mat process(Mat colorImage) {
        Imgproc.pyrDown(colorImage, colorImage);
        Imgproc.pyrDown(colorImage, colorImage);

        // Grey-scale image
        Mat monoImage = new Mat();
        Imgproc.cvtColor(colorImage, monoImage, Imgproc.COLOR_RGBA2GRAY);


        Mat blurredImage = new Mat();
        Size blurSize = new Size(5, 5);
        Imgproc.GaussianBlur(monoImage, blurredImage, blurSize, 2, 2);

        // Threshold image
        Mat threshedImage = new Mat();
        Imgproc.threshold(blurredImage, threshedImage, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        // Apply morphological transformations
        Mat transformedImage = new Mat();
        Mat morphKernel = new Mat(new Size(3, 3), CvType.CV_8UC1, new Scalar(255));
        Imgproc.morphologyEx(threshedImage, transformedImage, Imgproc.MORPH_OPEN, morphKernel);
        Imgproc.morphologyEx(transformedImage, transformedImage, Imgproc.MORPH_CLOSE, morphKernel);

        DetectedCircle detectedCircle = detectCircle(transformedImage, colorImage);

        if (detectedCircle.isDefined()) {
            DetectedArrow detectedArrow = detectArrow(transformedImage, detectedCircle);

            // Draw details
            Imgproc.circle(colorImage, detectedCircle.getCenter(), 2, MAT_L_BLUE, -1);
            Imgproc.circle(colorImage, detectedCircle.getCenter(), (int) detectedCircle.getRadius(), MAT_L_BLUE, 1);
            if (detectedArrow.isDefined()){
                Imgproc.putText(colorImage, "" +Math.round(detectedArrow.getDistFromCircleCenter()) , detectedArrow.getCenter(),
                        Core.FONT_HERSHEY_COMPLEX, 0.3, MAT_RED);
                Imgproc.rectangle(colorImage,
                        new Point(detectedArrow.getBoundingBox().x, detectedArrow.getBoundingBox().y),
                        new Point(detectedArrow.getBoundingBox().x + detectedArrow.getBoundingBox().width,
                                detectedArrow.getBoundingBox().y + detectedArrow.getBoundingBox().height),
                        MAT_RED);
            }else{
                // detection failed
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

            if (c.x == r.x) {
                angle = 90.0;
            } else {
                angle = Math.rint(Math.toDegrees(Math.atan((r.y - c.y) / (r.x - c.x))));
            }

            if (c.x < r.x) {
                if (c.y < r.y) {
                    angle = -180 + angle;
                } else {
                    angle = 180 + angle;
                }
            }

            Imgproc.circle(colorImage, p, 2, MAT_YELLOW, 1);
            Imgproc.circle(colorImage, q, 2, MAT_YELLOW, 1);
            Imgproc.circle(colorImage, r, 2, MAT_YELLOW, 1);
            Imgproc.line(colorImage, c, r, MAT_RED);
            Imgproc.circle(colorImage, r, 2, MAT_RED, -1);
            Imgproc.putText(colorImage, "" + angle, new Point(5, 15), Core.FONT_HERSHEY_COMPLEX, 0.3, MAT_YELLOW);
        }

        Imgproc.pyrUp(colorImage, colorImage);
        Imgproc.pyrUp(colorImage, colorImage);
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

class DetectedArrow{
    private Point center;
    private Rect boundingBox;
    private double distFromCircleCenter;
    private MatOfPoint contour;
    private int contourIndex;

    Point getCenter() {
        return center;
    }

    Rect getBoundingBox() {
        return boundingBox;
    }

    double getDistFromCircleCenter() {
        return distFromCircleCenter;
    }

    DetectedArrow(Point center, Rect boundingBox, double distFromCircleCenter, MatOfPoint contour, int contourIndex) {
        this.center = center;
        this.boundingBox = boundingBox;
        this.distFromCircleCenter = distFromCircleCenter;
        this.contour = contour;
        this.contourIndex = contourIndex;
    }

    DetectedArrow(){
        this.center = null;
        this.boundingBox = null;
        this.distFromCircleCenter = 0.0;
        this.contour = null;
    }

    MatOfPoint getContour() {
        return contour;
    }

    boolean isDefined(){
        return this.center != null;
    }

    int getContourIndex() {
        return contourIndex;
    }
}

class DetectedCircle{
    private Point center;
    private double radius;

    Point getCenter() {
        return center;
    }

    double getRadius() {
        return radius;
    }

    DetectedCircle(Point center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    DetectedCircle() {
        this.center = null;
        this.radius = 0.0;
    }

    boolean isDefined(){
        return center != null && (center.x != 0.0 && center.y != 0.0);
    }
}