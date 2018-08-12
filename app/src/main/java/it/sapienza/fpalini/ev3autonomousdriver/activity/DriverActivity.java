package it.sapienza.fpalini.ev3autonomousdriver.activity;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import it.sapienza.fpalini.ev3autonomousdriver.R;
import it.sapienza.fpalini.ev3autonomousdriver.detector.LaneDetector;
import it.sapienza.fpalini.ev3autonomousdriver.detector.ObjectDetector;

public class DriverActivity extends Activity implements CvCameraViewListener2 {

    private enum ACTION { TURN, MOVE, STOP, END }

    private static final String  TAG = "EV3AD::DriverActivity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean started, ended;
    private boolean terminateProgram, stopProgram = true;
    private Server server;

    private final int STARTING_SPEED = 100;

    private LaneDetector laneDetector = new LaneDetector();
    private ObjectDetector objectDetector = new ObjectDetector();

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.item_startStopProgram:
                stopProgram = !stopProgram;
                String title = item.getTitle().toString().equals("Start EV3") ? "Stop EV3" : "Start EV3";
                item.setTitle(title);
                break;

            case R.id.item_endProgram:
                terminateProgram = true;
                break;
        }
        return false;
    }

    public DriverActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_driver);

        ActivityCompat.requestPermissions(DriverActivity.this, new String[]{Manifest.permission.CAMERA}, 1);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                laneDetector.setShowOriginal(!laneDetector.showOriginal());
            }
        });

        server = new Server(4444);
        server.start();

        objectDetector.load_cascades(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        server.stopListening();
    }

    public void onCameraViewStarted(int width, int height) {
        Bundle extras = getIntent().getExtras();

        if(extras !=null)
        {
            int lowH = extras.getInt("lowH");
            int highH = extras.getInt("highH");
            int lowS = extras.getInt("lowS");
            int highS = extras.getInt("highS");
            int lowV = extras.getInt("lowV");
            int highV = extras.getInt("highV");

            if(lowH <= highH && lowS <= highS && lowV <= highV)
            {
                laneDetector.setLowHSV(new Scalar(lowH, lowS, lowV));
                laneDetector.setHighHSV(new Scalar(highH, highS, highV));
            }
        }
    }

    public void onCameraViewStopped() {}

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();

        int resFactor = 4;
        int realWidth = frame.width();
        int realHeight = frame.height();

        Imgproc.resize(frame, frame, new Size(realWidth/resFactor, realHeight/resFactor));

        if(laneDetector.showOriginal())
        {
            Mat frameClone = frame.clone();

            objectDetector.detect(frameClone);

            frameClone = frame.clone();

            if (objectDetector.getRectDetected() != null)
                // to exclude the object from the color (lane) detection
                Imgproc.rectangle(frameClone, objectDetector.getRectDetected().tl(), objectDetector.getRectDetected().br(), new Scalar(123, 123, 123), -1);

            laneDetector.detect(frameClone);

            laneDetector.draw(frame);
            objectDetector.draw(frame);
        }
        else
        {
            laneDetector.detect(frame);
            laneDetector.draw(frame);
        }

        if (terminateProgram)
        {
            server.setCommand(ACTION.END.toString());

            if(isEnded()) terminateProgram = false; // checks that the termination signal has been received by EV3
        }
        else if(stopProgram) server.setCommand(ACTION.STOP.toString());
        else // !stopProgram
        {
            if (!objectDetector.isGreenTrafficLight()) server.setCommand(ACTION.STOP.toString());
            else if (objectDetector.getStopTimer() != 0 && System.currentTimeMillis() - objectDetector.getStopTimer() < ObjectDetector.STOP_TIMER) server.setCommand(ACTION.STOP.toString());
            else if(!isStarted()) server.setCommand(ACTION.MOVE + " " + STARTING_SPEED);
            else server.setCommand(ACTION.TURN + " " + laneDetector.getCommand());
        }

        Imgproc.resize(frame, frame, new Size(realWidth, realHeight));

        return frame;
    }

    private boolean isStarted()
    {
        return started;
    }

    private boolean isEnded()
    {
        return ended;
    }

    private class Server extends Thread {

        private final int PORT;

        private String command;
        private boolean stopListening;

        private Server(int port)
        {
            PORT = port;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        @Override
        public void run() {
            Log.i(TAG, "Starting server...");

            ServerSocket listener;
            Socket socket;

            DataInputStream in;
            DataOutputStream out;

            try {
                listener = new ServerSocket(PORT);

                while (!stopListening) {
                    Log.i(TAG, "Server listening for clients...");

                    socket = listener.accept();

                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());

                    out.writeUTF(command);

                    String input = in.readUTF();

                    switch(input)
                    {
                        case "STARTED": started = true; ended = false; break;
                        case "STOPPED": started = false; ended = false; break;
                        case "ENDED": started = false; ended = true; break;
                    }

                    Log.i(TAG, "Sent: " + command + "\tReceived: " + input);
                }
            }
            catch (IOException e) { e.printStackTrace(); }
        }

        private void stopListening() {
            stopListening = true;
        }
    }

    static { System.loadLibrary("opencv_java3"); }

}
