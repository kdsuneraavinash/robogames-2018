package com.stats.kdsuneraavinash.walkingrobot;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


enum IdentifyMode {
    ROAD, JUNCTION
}

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private ArrowRecognition arrowRecognizer;
    private RoadRecognition roadRecognizer;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbDeviceConnection connection;
    private UsbDevice device;
    private UsbSerialDevice serialPort;

    private Button buttonUsbBegin;
    private EditText text;

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

    public CameraActivity() {
    }

    private void onClickUsbBegin(View v) {
        HashMap<String, UsbDevice> usbDevices;
        try {
            usbDevices = usbManager.getDeviceList();
        } catch (NullPointerException e) {
            text.append("No Usb Devices Detected");
            System.out.println("No Usb Devices Detected");
            return;
        }
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2342) {
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pendingIntent);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep) {
                    break;
                }
            }
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), ACTION_USB_PERMISSION)) {
                boolean granted;
                try {
                    granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                } catch (NullPointerException e) {
                    granted = false;
                }
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) {
                            // TODO: Set UI Buttons enabled
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(new UsbSerialInterface.UsbReadCallback() {
                                @Override
                                public void onReceivedData(byte[] bytes) {
                                    String data;
                                    try {
                                        data = new String(bytes, "UTF-8");
                                        data = data.concat("\n");
                                        text.append(data);
                                        System.out.println(data);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            text.append("Serial Connection Opened");
                            System.out.println("Serial Connection Opened");
                        } else {
                            text.append("Serial port not opened");
                            System.out.println("Serial port not opened");
                        }
                    } else {
                        text.append("Port is null");
                        System.out.println("Port is null");
                    }
                } else {
                    text.append("Serial Permission not granted");
                    System.out.println("Serial Permission not granted");
                }
            } else if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_ACCESSORY_ATTACHED)) {
                onClickUsbBegin(buttonUsbBegin);
            } else if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                // TODO: Press stop button
                System.out.println("Add code to stop");
            } else{
                text.append("Error");
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
        buttonUsbBegin = findViewById(R.id.buttonUsbBegin);
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
        buttonUsbBegin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onClickUsbBegin(v);
            }
        });
        text = findViewById(R.id.text);
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
        if (mode == IdentifyMode.ROAD) {
            screen = roadRecognizer.process(inputFrame.rgba());
        } else if (mode == IdentifyMode.JUNCTION) {
            screen = arrowRecognizer.process(inputFrame.rgba());
        } else {
            screen = inputFrame.rgba();
        }
        return screen;

//        return arrowRecognizer.process(inputFrame.rgba());
    }
}


