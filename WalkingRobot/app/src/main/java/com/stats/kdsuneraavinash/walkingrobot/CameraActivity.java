package com.stats.kdsuneraavinash.walkingrobot;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        ArrowRecognition rec = new ArrowRecognition(frame.rows(), frame.cols());
        Mat thresh = rec.thresholdImage(frame);
        Mat morph = rec.morphology(thresh);
        MatOfPoint contour = rec.findContour(morph);
        if (contour == null) {
            return frame;
        }

        Point p = rec.getMinPoint(contour);
        Point q = rec.getMaxPoint(contour);
        Point r = rec.findFurthestPoint(contour, p, q);

        Point[] pointsInOrder = rec.findCorrectCombination(contour, p, q, r);
        p = pointsInOrder[0];
        q = pointsInOrder[1];
        r = pointsInOrder[2];

        Imgproc.circle(frame, r, 5, new Scalar(255, 0, 255), 3);
        Imgproc.contourArea(contour);
        double cx = (p.x + q.x) / 2;
        double cy = (p.y + q.y) / 2;
        Point c = new Point(cx, cy);
        Imgproc.line(frame, c, r, new Scalar(0, 0, 255));
        Imgproc.circle(frame, r, 5, new Scalar(0, 0, 255), -1);
        Imgproc.putText(frame, "" + Imgproc.contourArea(contour), r, Core.FONT_HERSHEY_COMPLEX, 1.0, new Scalar(255, 0, 0));
        return rec.drawContour(frame, contour);
    }
}


