package it.sapienza.fpalini.ev3autonomousdriver.detector;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import it.sapienza.fpalini.ev3autonomousdriver.R;

public class ObjectDetector implements Detector {

    public enum OBJECT { CAR, STOP, TRAFFIC_LIGHT }
    public static final int STOP_TIMER = 3000;
    private Rect rectDetected;
    private String text;
    private CascadeClassifier[] face_cascade = new CascadeClassifier[OBJECT.values().length];
    private Scalar color = new Scalar(0, 0, 255);
    private int round = 0;
    private boolean isGreenTrafficLight = true;
    private long stopTimer;
    private boolean firstTime = true;

    @Override
    public void detect(Mat frame) {

        Mat gray = new Mat();

        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY);

        MatOfRect faces = new MatOfRect();

        face_cascade[round].detectMultiScale(gray, faces);

        rectDetected = biggestRect(faces.toArray());

        if(rectDetected != null)
        {
            text = OBJECT.values()[round].name();

            switch (OBJECT.values()[round])
            {
                case TRAFFIC_LIGHT:
                    // the implementation of get requires get(Y,X) instead of get(X,Y)
                    double[] rgb = frame.get(rectDetected.y+rectDetected.height/2, rectDetected.x+rectDetected.width/2);

                    Imgproc.circle(frame, new Point(rectDetected.x+rectDetected.width/2, rectDetected.y+rectDetected.height/2), 3, color);

                    if (rgb[0] < rgb[1]) {
                        text += " GREEN";
                        isGreenTrafficLight = true;
                    }
                    else
                    {
                        text += " RED";
                        isGreenTrafficLight = false;
                    }
                    break;

                case STOP:
                    if (firstTime)
                    {
                        stopTimer = System.currentTimeMillis();
                        firstTime = false;
                    }
                    break;

                case CAR:
                    if (!firstTime) stopTimer = System.currentTimeMillis();
                    break;
            }
        }

        if (System.currentTimeMillis() - stopTimer > 2*STOP_TIMER) firstTime = true;

        round = round == OBJECT.values().length-1 ? 0 : round+1;
    }

    public long getStopTimer() {
        return stopTimer;
    }

    public void load_cascades(Activity activity){
        String cascade_name;
        int cascade_id = -1;

        for(OBJECT object : OBJECT.values()) {
            cascade_name = "cascade_"+object.name().toLowerCase();
            try {
                cascade_id = R.raw.class.getField(cascade_name).getInt(null);
            }
            catch (IllegalAccessException | NoSuchFieldException e) { e.printStackTrace(); }

            try {
                InputStream is = activity.getResources().openRawResource(cascade_id);
                File cascadeDir = activity.getDir("cascades", Context.MODE_PRIVATE);
                File mCascadeFile = new File(cascadeDir, cascade_name+".xml");
                FileOutputStream os = new FileOutputStream(mCascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1)
                    os.write(buffer, 0, bytesRead);

                is.close();
                os.close();

                face_cascade[object.ordinal()] = new CascadeClassifier(mCascadeFile.getAbsolutePath());

                if (face_cascade[object.ordinal()].empty()) {
                    Log.v("MyActivity", "--(!)Error loading A\n");
                    return;
                } else {
                    Log.v("MyActivity", "Loaded cascade_"+object.name().toLowerCase()+" classifier from " + mCascadeFile.getAbsolutePath());
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.v("MyActivity", "Failed to load cascade_"+object.name().toLowerCase()+". Exception thrown: " + e);
            }
        }
    }

    private Rect biggestRect(Rect[] rects)
    {
        double area = -1;
        Rect maxRect = null;

        for (Rect rect : rects)
        {
            if (rect.area() > area)
            {
                area = rect.area();
                maxRect = rect;
            }
        }

        return maxRect;
    }

    public Rect getRectDetected()
    {
        return rectDetected;
    }

    @Override
    public void draw(Mat frame)
    {
        if (rectDetected != null) {
            Imgproc.rectangle(frame, rectDetected.tl(), rectDetected.br(), color, 5);
            Imgproc.putText(frame, text, new Point(rectDetected.x, rectDetected.y - 10), Core.FONT_HERSHEY_SIMPLEX, 0.5, color);
        }
    }

    public boolean isGreenTrafficLight() {
        return isGreenTrafficLight;
    }
}
