package com.stats.kdsuneraavinash.walkingrobot;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

enum IdentifyMode {
    ROAD, JUNCTION
}

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private ArrowRecognition arrowRecognizer;
    private RoadRecognition roadRecognizer;

    Arduino arduino;
    private IdentifyMode mode = IdentifyMode.ROAD;
    private TextView textStatus;

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

    private View.OnClickListener buttonRoadModeOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mode = IdentifyMode.ROAD;
        }
    };

    private View.OnClickListener buttonJunctionModeOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mode = IdentifyMode.JUNCTION;
        }
    };

    private ArduinoListener arduinoListener = new ArduinoListener() {
        @Override
        public void onArduinoAttached(UsbDevice device) {
            arduino.open(device);
            textStatus.setBackgroundColor(Color.BLUE);
        }

        @Override
        public void onArduinoDetached() {
            textStatus.setBackgroundColor(Color.RED);

        }

        @Override
        public void onArduinoMessage(final byte[] bytes) {
            textStatus.setBackgroundColor(Color.GREEN);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textStatus.append(new String(bytes));
                }
            });

        }

        @Override
        public void onArduinoOpened() {
            textStatus.setBackgroundColor(Color.YELLOW);
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

        textStatus = findViewById(R.id.textIndicator);

        Button buttonRoadMode = findViewById(R.id.buttonRoadMode);
        Button buttonJunctionMode = findViewById(R.id.buttonJunctionMode);
        buttonJunctionMode.setOnClickListener(buttonJunctionModeOnClick);
        buttonRoadMode.setOnClickListener(buttonRoadModeOnClick);

        findViewById(R.id.buttonForward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arduino.send("1".getBytes());
            }
        });
        findViewById(R.id.buttonStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arduino.send("5".getBytes());
            }
        });
        findViewById(R.id.buttonLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arduino.send("3".getBytes());
            }
        });
        findViewById(R.id.buttonRight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arduino.send("4".getBytes());
            }
        });

        arduino = new Arduino(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        arduino.setArduinoListener(arduinoListener);
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
        arduino.unsetArduinoListener();
        arduino.close();
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
        if (mode == IdentifyMode.ROAD) {
            screen = roadRecognizer.process(inputFrame.rgba());
        } else if (mode == IdentifyMode.JUNCTION) {
            screen = arrowRecognizer.process(inputFrame.rgba());
        } else {
            screen = inputFrame.rgba();
        }
        return screen;
    }
}


