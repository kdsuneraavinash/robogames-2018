package com.stats.kdsuneraavinash.walkingrobot;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

enum IdentifyMode{
    ROAD, JUNCTION
}

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private ArrowRecognition arrowRecognizer;
    private RoadRecognition roadRecognizer;

    private IdentifyMode mode = IdentifyMode.ROAD;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    arrowRecognizer = new ArrowRecognition();
                    roadRecognizer = new RoadRecognition();
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

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Button buttonRoadMode = findViewById(R.id.buttonRoadMode);
        Button buttonJunctionMode = findViewById(R.id.buttonJunctionMode);
        buttonJunctionMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = IdentifyMode.JUNCTION;
            }
        });
        buttonRoadMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = IdentifyMode.ROAD;
            }
        });
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
        Mat screen;
        if (mode == IdentifyMode.ROAD){
            screen = roadRecognizer.process(inputFrame.rgba());
        }else if (mode == IdentifyMode.JUNCTION){
            screen = arrowRecognizer.process(inputFrame.rgba());
        }else{
            screen = inputFrame.rgba();
        }
        return screen;

//        return arrowRecognizer.process(inputFrame.rgba());
    }
}


