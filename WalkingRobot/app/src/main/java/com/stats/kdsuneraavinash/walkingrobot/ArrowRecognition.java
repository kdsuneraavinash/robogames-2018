package com.stats.kdsuneraavinash.walkingrobot;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

class ArrowRecognition {
    private Mat editImage;
    private List<MatOfPoint> contours = new ArrayList<>();
    private MatOfPoint arrowContour = new MatOfPoint();
    private List<Point> arrowContourPoints = new ArrayList<>();

    ArrowRecognition(Mat original) {
        this.editImage = original.clone();
    }

    void thresholdImage() {
        Imgproc.cvtColor(this.editImage, this.editImage, Imgproc.COLOR_BGR2GRAY);
        // Otsu's threshing after Gaussian filtering
        Imgproc.GaussianBlur(this.editImage, this.editImage, new Size(5, 5), 0);
        Imgproc.threshold(this.editImage, this.editImage, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
    }

    void morphology() {
        // Open and close so that all small gaps are removed
        Mat kernel = new Mat(new Size(3, 3), CvType.CV_8UC1, new Scalar(255));
        Imgproc.morphologyEx(this.editImage, this.editImage, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(this.editImage, this.editImage, Imgproc.MORPH_CLOSE, kernel);
    }

    MatOfPoint findContour() {
        // Find contours in image
        Imgproc.findContours(this.editImage, this.contours, this.editImage, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxArea = -1;
        for (MatOfPoint contour : this.contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                this.arrowContour = contour;
            }
        }
        this.arrowContourPoints = this.arrowContour.toList();
        return this.arrowContour;
    }

    Mat drawContour(Mat image) {
        ArrayList<MatOfPoint> tampContour = new ArrayList<>();
        tampContour.add(this.arrowContour);
        Imgproc.drawContours(image, tampContour, 0, new Scalar(0, 255, 255));
        return image;
    }

    Point getMinPoint() {
        // Find bottom most point
        Point minPoint = arrowContourPoints.get(0);
        for (Point point : arrowContourPoints) {
            if (point.y < minPoint.y) {
                minPoint = point;
            }
        }
        return minPoint;
    }


    Point getMaxPoint() {
        // Find top most point
        Point maxPoint = arrowContourPoints.get(0);
        for (Point point : arrowContourPoints) {
            if (point.y > maxPoint.y) {
                maxPoint = point;
            }
        }
        return maxPoint;
    }

    Point findFurthestPoint(Point p, Point q) {
        // Find point farthest away from line between max nad min
        double maxDistance = 0;
        Point furthestPoint = p;
        for (Point point : arrowContourPoints) {
            double distance = 0.5 * Math.abs((p.x - point.x) * (q.y - p.y) - (p.x - q.x) * (point.y - p.y));
            if (maxDistance < distance) {
                maxDistance = distance;
                furthestPoint = point;
            }
        }
        return furthestPoint;
    }

    private int countBlackPoints(MatOfPoint2f contour, Point a) {
        int blackPoints = 0;
        for (int i = (int) a.x - 10; i < a.x + 10; i++) {
            for (int j = (int) a.y - 10; j < a.y + 10; j++) {
                try {
                    if (Imgproc.pointPolygonTest(contour, new Point(i, j), false) == 1) {
                        blackPoints++;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return blackPoints;
    }

    Point[] findCorrectCombination(Point p, Point q, Point r) {
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

