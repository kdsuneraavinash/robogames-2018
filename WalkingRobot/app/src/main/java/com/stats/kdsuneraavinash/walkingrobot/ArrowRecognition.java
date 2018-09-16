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
    private int n_rows;
    private int n_cols;

    ArrowRecognition(int n_rows, int n_cols) {
        this.n_rows = n_rows;
        this.n_cols = n_cols;
    }

    Mat thresholdImage(Mat image) {
        Mat greyed = createEmptyMat();
        Imgproc.cvtColor(image, greyed, Imgproc.COLOR_BGR2GRAY);

        Mat thresh = createEmptyMat();
        Imgproc.threshold(greyed, thresh, 100, 255, 0);
        return thresh;
    }

    Mat morphology(Mat thresh) {
        // Open and close so that all small gaps are removed
        Mat kernel = new Mat(new Size(3, 3), CvType.CV_8UC1, new Scalar(255));
        Mat opening = createEmptyMat();
        Mat closing = createEmptyMat();
        Imgproc.morphologyEx(thresh, opening, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(opening, closing, Imgproc.MORPH_CLOSE, kernel);
        return closing;
    }

    MatOfPoint findContour(Mat image) {
        // Find contours in image
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(image, contours, image, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxArea = -1;
        MatOfPoint maxAreaContour = null;
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > 33000 || area < 10000){
                continue;
            }
            if (area > maxArea) {
                maxArea = area;
                maxAreaContour = contour;
            }
        }
        return maxAreaContour;
    }

    Mat drawContour(Mat image, MatOfPoint contour) {
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        Imgproc.drawContours(image, contours, 0, new Scalar(0, 255, 255));
        return image;
    }

    private Mat createEmptyMat() {
        return Mat.zeros(this.n_rows, this.n_cols, CvType.CV_8UC1);
    }

    Point getMinPoint(MatOfPoint contour) {
        // Find bottom most point
        List<Point> contourPoints = contour.toList();
        Point minPoint = contourPoints.get(0);
        for (Point point : contourPoints) {
            if (point.y < minPoint.y) {
                minPoint = point;
            }
        }
        return minPoint;
    }


    Point getMaxPoint(MatOfPoint contour) {
        // Find top most point
        List<Point> contourPoints = contour.toList();
        Point maxPoint = contourPoints.get(0);
        for (Point point : contourPoints) {
            if (point.y > maxPoint.y) {
                maxPoint = point;
            }
        }
        return maxPoint;
    }

    Point findFurthestPoint(MatOfPoint contour, Point p, Point q) {
        List<Point> contourPoints = contour.toList();

        // Find point farthest away from line between max nad min
        double maxDistance = 0;
        Point furthestPoint = p;
        for (Point point : contourPoints) {
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

    Point[] findCorrectCombination(MatOfPoint contour, Point p, Point q, Point r) {
        Point[][] combinations = {
                {p, q, r},
                {p, r, q},
                {q, r, p},
        };
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
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
