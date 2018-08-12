package it.sapienza.fpalini.ev3autonomousdriver.detector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class LaneDetector implements Detector {

    private final Scalar BLUE  = new Scalar(0, 0, 255);
    private final Scalar GREEN = new Scalar(0, 255, 0);
    private final Scalar RED   = new Scalar(255, 0, 0);

    private Scalar lowHSV = new Scalar(50, 100, 100);
    private Scalar highHSV = new Scalar(70, 255, 255);

    private PID pid = new PID(1.2, 0, 0);

    private boolean showOriginal = true;

    private Rect street;
    private Point streetPoint;

    private List<MatOfPoint> contours;

    public LaneDetector()
    {
        contours = new ArrayList<>();
    }

    @Override
    public void detect(Mat frame) {

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2HSV);

        Core.inRange(frame, lowHSV, highHSV, frame);

        // Imgproc.morphologyEx(frame, frame, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_OPEN, new Size(2*kernelSize+1, 2*kernelSize+1)));

        contours.clear();

        Mat hierarchy = new Mat();

        Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2RGB);

        MatOfPoint biggestContour = null;
        double biggestArea = -1;

        for(MatOfPoint contour : contours)
        {
            double area = Imgproc.contourArea(contour);

            if(area > biggestArea && frame.height()*4/5 > Imgproc.boundingRect(contour).y)
            {
                biggestArea = area;
                biggestContour = contour;
            }
        }

        if(biggestContour != null)
        {
            street = Imgproc.boundingRect(biggestContour);

            streetPoint = getMiddlePoint(street.tl(), street.br());

            if (streetPoint.y > frame.height()*4/5)
                if (street.y + street.height > frame.height()*4/5) streetPoint.y = frame.height()*4/5;

            pid.compute(new Point(frame.width()/2, frame.height()-1), streetPoint);
        }
        else street = null; // streetPoint must be preserved
    }

    @Override
    public void draw(Mat frame) {
        if (street != null) Imgproc.rectangle(frame, street.tl(), street.br(), RED, 5);

        if (streetPoint != null)
        {
            Imgproc.drawMarker(frame, streetPoint, BLUE);
            Imgproc.line(frame, new Point(frame.width() / 2, frame.height() - 1), streetPoint, RED, 10);
            Imgproc.putText(frame, "" + pid.getAngle(), new Point(frame.width() / 2 - 50, frame.height() - 50), Core.FONT_HERSHEY_SIMPLEX, 2, GREEN);
            Imgproc.putText(frame, "PID value: " + pid.getValue(), new Point(frame.width() / 2 - 50, frame.height() - 20), Core.FONT_HERSHEY_SIMPLEX, 0.5, GREEN);
        }
    }

    public int getCommand()
    {
        return pid.getValue();
    }

    public void setHighHSV(Scalar highHSV)
    {
        this.highHSV = highHSV;
    }

    public void setLowHSV(Scalar lowHSV)
    {
        this.lowHSV = lowHSV;
    }

    public void setShowOriginal(boolean showOriginal)
    {
        this.showOriginal = showOriginal;
    }

    public boolean showOriginal()
    {
        return showOriginal;
    }

    private Point getMiddlePoint(Point lowPoint, Point highPoint)
    {
        return new Point(lowPoint.x/2+highPoint.x/2, lowPoint.y/2+highPoint.y/2);
    }

    private class PID
    {
        private double Kp, Kd, Ki;
        private double Ei, prev_Ep;

        private final int WINDUP_BOUND = 300;

        private long timer;

        private int value;

        private PID(double Kp, double Kd, double Ki)
        {
            this.Kp = Kp;
            this.Kd = Kd;
            this.Ki = Ki;

            timer = System.currentTimeMillis();
        }

        private void compute(Point from, Point to)
        {
            double angle = computeAngle(from, to);

            long now = System.currentTimeMillis();

            double Ep = angle;
            double Ed = (Ep - prev_Ep)/(now-timer) * 1000;
            Ei += Ep *(now-timer);

            // Reset Windup
            if(Ei > WINDUP_BOUND) Ei = WINDUP_BOUND;
            else if(Ei < -WINDUP_BOUND) Ei = -WINDUP_BOUND;

            value = (int)(Kp*Ep+Kd*Ed+Ki*Ei);

            timer = now;
            prev_Ep = angle;
        }

        private double computeAngle(Point lowPoint, Point highPoint)
        {
            Point x = new Point(lowPoint.x, highPoint.y);

            double a = distance(lowPoint, x);
            double ipot = distance(lowPoint, highPoint);
            double angle = Math.acos(a / ipot) * 180 / Math.PI;

            if (lowPoint.x > highPoint.x) angle = -angle;

            return angle;
        }

        private double distance(Point p1, Point p2)
        {
            return Math.sqrt(square_distance(p1,p2));
        }

        private double square_distance(Point p1, Point p2)
        {
            return Math.pow(p1.x-p2.x,2)+Math.pow(p1.y-p2.y,2);
        }

        private int getAngle(){ return (int)prev_Ep; }

        public int getValue() { return value; }
    }
}
