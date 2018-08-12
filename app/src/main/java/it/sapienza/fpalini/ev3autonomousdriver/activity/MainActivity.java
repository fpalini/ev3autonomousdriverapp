package it.sapienza.fpalini.ev3autonomousdriver.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;

import it.sapienza.fpalini.ev3autonomousdriver.R;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private static final String  TAG = "EV3AD::MainActivity";

    private CameraBridgeViewBase mOpenCvCameraView;

    private int lowH;
    private int highH;
    private int lowS;
    private int highS;
    private int lowV;
    private int highV;

    private SharedPreferences sharedPref;

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

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPref = getPreferences(Context.MODE_PRIVATE);

        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();

        Button startButton = (Button) findViewById(R.id.button_start);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences.Editor editor = sharedPref.edit();

                editor.putInt("lowH", lowH);
                editor.putInt("highH", highH);
                editor.putInt("lowS", lowS);
                editor.putInt("highS", highS);
                editor.putInt("lowV", lowV);
                editor.putInt("highV", highV);

                editor.apply();

                Intent intent = new Intent(getBaseContext(), DriverActivity.class);

                intent.putExtra("lowH", lowH);
                intent.putExtra("highH", highH);
                intent.putExtra("lowS", lowS);
                intent.putExtra("highS", highS);
                intent.putExtra("lowV", lowV);
                intent.putExtra("highV", highV);

                startActivity(intent);
            }
        });

        SeekBar seekLowH = (SeekBar) findViewById(R.id.seek_lowH);
        SeekBar seekHighH = (SeekBar) findViewById(R.id.seek_highH);
        SeekBar seekLowS = (SeekBar) findViewById(R.id.seek_lowS);
        SeekBar seekHighS = (SeekBar) findViewById(R.id.seek_highS);
        SeekBar seekLowV = (SeekBar) findViewById(R.id.seek_lowV);
        SeekBar seekHighV = (SeekBar) findViewById(R.id.seek_highV);

        seekLowH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                lowH = progress;
            }
        });

        seekHighH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                highH = progress;
            }
        });

        seekLowS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                lowS = progress;
            }
        });

        seekHighS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                highS = progress;
            }
        });

        seekLowV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                lowV = progress;
            }
        });

        seekHighV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                highV = progress;
            }
        });

        lowH = sharedPref.getInt("lowH", seekLowH.getProgress());
        highH = sharedPref.getInt("highH", seekHighH.getProgress());
        lowS = sharedPref.getInt("lowS", seekLowS.getProgress());
        highS = sharedPref.getInt("highS", seekHighS.getProgress());
        lowV = sharedPref.getInt("lowV", seekLowV.getProgress());
        highV = sharedPref.getInt("highV", seekHighV.getProgress());

        seekLowH.setProgress(lowH);
        seekHighH.setProgress(highH);
        seekLowS.setProgress(lowS);
        seekHighS.setProgress(highS);
        seekLowV.setProgress(lowV);
        seekHighV.setProgress(highV);
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
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2HSV);

        Core.inRange(frame, new Scalar(lowH, lowS, lowV), new Scalar(highH, highS, highV), frame);

        return frame;
    }

    static { System.loadLibrary("opencv_java3"); }

}
