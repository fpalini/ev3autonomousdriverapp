package it.sapienza.fpalini.ev3autonomousdriver.detector;

import org.opencv.core.Mat;

interface Detector
{
    void detect(Mat frame);
    void draw(Mat frame);
}
