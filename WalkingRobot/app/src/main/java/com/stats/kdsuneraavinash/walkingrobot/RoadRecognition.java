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

class RoadRecognition {
    private static final Size GAUSSIAN_BLUR_KERNEL = new Size(5, 5);
    private double slope;
    private double intercept;

    Mat process(Mat colorImage) {
        Mat grayImage = new Mat();
        Mat blurredImage = new Mat();
        Mat threshedImage = new Mat();

        // gray-scale image
        Imgproc.cvtColor(colorImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Otsu's threshing after Gaussian filtering
        Imgproc.GaussianBlur(grayImage, blurredImage, GAUSSIAN_BLUR_KERNEL, 0);
        Imgproc.threshold(blurredImage, threshedImage, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);


        List<Point> whitePoints = new ArrayList<>();
        for (int x = 0; x < threshedImage.size().width; x+=10) {
            for (int y = 0; y < threshedImage.size().height; y+=10) {
                if (threshedImage.get(y, x)[0] > 127){
                    Imgproc.circle(colorImage, new Point(x, y), 5, new Scalar(255, 255, 255), -1);
                    whitePoints.add(new Point(x, y));
                }
            }
        }

        // Linear Regression and prediction calculation
        // https://enlight.nyc/projects/linear-regression/
        double sumXPoints = 0;
        double sumYPoints = 0;
        double sumXYPoints = 0;
        double sumXXPoints = 0;
        for (int i = 0; i < whitePoints.size(); i++) {
            sumXPoints += whitePoints.get(i).x;
            sumYPoints += whitePoints.get(i).y;
            sumXYPoints += whitePoints.get(i).x * whitePoints.get(i).y;
            sumXXPoints += whitePoints.get(i).x * whitePoints.get(i).x;
        }
        double meanXPoints = sumXPoints/whitePoints.size();
        double meanYPoints = sumYPoints/whitePoints.size();
        double meanXYPoints = sumXYPoints/whitePoints.size();
        double meanXXPoints = sumXXPoints/whitePoints.size();

        slope = ((meanXPoints*meanYPoints) - meanXYPoints) /
                ((meanXPoints*meanXPoints) - meanXXPoints);

        intercept = meanYPoints - slope*meanXPoints;

        double squaredErrorRegression = 0;
        double squaredErrorYMean = 0;
        for (int i = 0; i < whitePoints.size(); i++) {
            // y values of regression line
            double regressionY = slope*whitePoints.get(i).x + intercept;
            // squared error of regression line
            squaredErrorRegression += Math.pow(regressionY - whitePoints.get(i).y, 2);
            // squared error of the y mean line - horizontal line (mean of y values)
            squaredErrorYMean +=  Math.pow(meanYPoints - whitePoints.get(i).y, 2);
        }
        double rSquared = 1 -( squaredErrorRegression/squaredErrorYMean);

        System.out.println(rSquared);

        Imgproc.line(colorImage, new Point(0, intercept), new Point(1000,1000*slope + intercept), new Scalar(255, 0, 0), 5);
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
