package com.stats.kdsuneraavinash.walkingrobot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.text.InputType;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.HashMap;

import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

enum IdentifyMode {
    ROAD, JUNCTION, FOLLOW_ROAD
}

enum RobotCommand {
    FORWARD, STOP, LEFT, RIGHT, NONE, CIRCLE
}

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private ArrowRecognition arrowRecognizer;
    private RoadRecognition roadRecognizer;

    private Arduino arduino;
    private IdentifyMode mode = IdentifyMode.ROAD;
    private TextView textStatus;
    private TextView textReceived;
    private Button buttonIdentifyOverride;
    private Button buttonDirectionOverride;
    private Button buttonFollowRoad;

    // Setting - Sleep duration (milliseconds)
    @SuppressWarnings("FieldCanBeLocal")
    private int waitDuration = 10;

    EditText dialogInput;

    private HashMap<RobotCommand, String> commandWords;

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

    private ArduinoListener arduinoListener;

    {
        arduinoListener = new ArduinoListener() {
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
                        textReceived.setText(new String(bytes));
                    }
                });

            }

            @Override
            public void onArduinoOpened() {
                textStatus.setBackgroundColor(Color.YELLOW);
            }
        };
    }

    public CameraActivity() {
        commandWords = new HashMap<>();
        commandWords.put(RobotCommand.FORWARD, "1");
        commandWords.put(RobotCommand.LEFT, "3");
        commandWords.put(RobotCommand.RIGHT, "4");
        commandWords.put(RobotCommand.STOP, "5");
        commandWords.put(RobotCommand.NONE, "5");
        commandWords.put(RobotCommand.CIRCLE, "6");
    }

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
        textStatus.setBackgroundColor(Color.RED);

        textReceived = findViewById(R.id.textReceived);

        buttonIdentifyOverride = findViewById(R.id.buttonIdentifyOverride);
        buttonDirectionOverride = findViewById(R.id.buttonDirectionOverride);
        buttonFollowRoad = findViewById(R.id.buttonFollowRoad);
        clearButtonColors();

        buttonIdentifyOverride
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
                        builder.setTitle("Manually Override Identifying")
                                .setItems(new String[]{"Identify Road", "Identify Junction"}, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        clearButtonColors();
                                        findViewById(R.id.buttonIdentifyOverride).setBackgroundColor(Color.YELLOW);
                                        if (which == 0) {
                                            mode = IdentifyMode.ROAD;
                                        } else {
                                            mode = IdentifyMode.JUNCTION;
                                        }
                                    }
                                }).show();
                    }
                });


        buttonDirectionOverride
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
                        builder.setTitle("Manually Override Direction")
                                .setItems(new String[]{"Forward", "Left", "Right", "Stop"}, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        clearButtonColors();
                                        findViewById(R.id.buttonDirectionOverride).setBackgroundColor(Color.YELLOW);
                                        switch (which) {
                                            case 0:
                                                sendArduinoString(commandWords.get(RobotCommand.FORWARD));
                                                break;
                                            case 1:
                                                sendArduinoString(commandWords.get(RobotCommand.LEFT));
                                                break;
                                            case 2:
                                                sendArduinoString(commandWords.get(RobotCommand.RIGHT));
                                                break;
                                            default:
                                                sendArduinoString(commandWords.get(RobotCommand.STOP));
                                                break;
                                        }
                                    }
                                }).show();
                    }
                });


        buttonFollowRoad
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearButtonColors();
                        findViewById(R.id.buttonFollowRoad).setBackgroundColor(Color.YELLOW);
                        mode = IdentifyMode.FOLLOW_ROAD;
                    }
                });

        findViewById(R.id.buttonSettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
                        builder.setTitle("Adjust Values")
                                .setItems(new String[]{"Small Road Filter", "Max deviation in Center"}, new DialogInterface.OnClickListener() {
                                    @SuppressLint("DefaultLocale")
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0:
                                                showInputDialogBox("Small Road Filter", String.format("%.2f", roadRecognizer.getSmallRoadFilter()), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        try {
                                                            roadRecognizer.setSmallRoadFilter(Double.parseDouble(dialogInput.getText().toString()));
                                                        } catch (Exception ignored) {
                                                        }
                                                    }
                                                });
                                                break;
                                            case 1:
                                                showInputDialogBox("Max deviation in Center", String.format("%d", roadRecognizer.getCenterMaxDeviation()), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        try {
                                                            roadRecognizer.setCenterMaxDeviation(Integer.parseInt(dialogInput.getText().toString()));
                                                        } catch (Exception ignored) {
                                                        }
                                                    }
                                                });
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                }).show();
                    }
                });

        arduino = new Arduino(this);
    }

    private void clearButtonColors() {
        buttonIdentifyOverride.setBackgroundColor(Color.WHITE);
        buttonDirectionOverride.setBackgroundColor(Color.WHITE);
        buttonFollowRoad.setBackgroundColor(Color.WHITE);
    }

    private void sendArduinoString(final String message) {
        System.out.println("Sent: " + message);
        arduino.send((message).getBytes());
        this.runOnUiThread(new Runnable() {
            public void run() {
                textStatus.setText(String.format("Sent: %s", message));
            }
        });
    }

    private void showInputDialogBox(String title, String defaultText, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Set up the input
        dialogInput = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        dialogInput.setInputType(InputType.TYPE_CLASS_TEXT);
        dialogInput.setHint(defaultText);
        builder.setView(dialogInput);

        // Set up the buttons
        builder.setPositiveButton("OK", listener);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
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
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)  {
        Mat screen;
        if (mode == IdentifyMode.ROAD) {
            screen = roadRecognizer.process(inputFrame.rgba());
        } else if (mode == IdentifyMode.JUNCTION) {
            screen = arrowRecognizer.process(inputFrame.rgba());
        } else if (mode == IdentifyMode.FOLLOW_ROAD) {
            screen = roadRecognizer.process(inputFrame.rgba());
            if (roadRecognizer.isCanBeCircle()){
                mode = IdentifyMode.JUNCTION;
                sendArduinoString(commandWords.get(RobotCommand.CIRCLE));
            }else{
                RobotCommand command;
                double deviation = roadRecognizer.getMidPointDeviation();
                if (deviation > roadRecognizer.getCenterMaxDeviation()) {
                    command = RobotCommand.RIGHT;
                } else if (deviation < -roadRecognizer.getCenterMaxDeviation()) {
                    command = RobotCommand.LEFT;
                } else {
                    command = RobotCommand.FORWARD;
                }
                sendArduinoString(commandWords.get(command));
                try {
                    Thread.sleep(waitDuration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            screen = inputFrame.rgba();
        }
        return screen;
    }
}


