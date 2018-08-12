#include <jni.h>

#include <string>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>

extern "C" {

using namespace std;
using namespace cv;

bool isInRoi(Rect roi, vector<Point> contour);
double distance(Point x1, Point x2);

JNIEXPORT jobject JNICALL
Java_it_sapienza_fpalini_ev3autonomousdriver_activity_MainActivity_detectLane(JNIEnv *env,
                                                                              jobject instance,
                                                                              jobject frame) {

    /*Mat src = *((Mat*) frame);

    int width = 1280;
    int height = 768;

    Mat dst,  cdst, src_lines;
    Rect roi(0, height*2/3, width, height*1/3);
    int id = 1;

    vector<Vec4i> lines;
    Vec4i l;

    src_lines = src.clone();
    blur(src, src, Size(3,3));
    Canny(src, dst, 0, 200);
    cvtColor(dst, cdst, COLOR_GRAY2BGR);

    vector< vector<Point> >contours;
    vector<Vec4i>hierarchy;

    findContours( dst, contours, hierarchy, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );

    int counter = 0;
    double angle[2];

    for( int i = 0; i< contours.size(); i++ ) {
        Point highest = contours[i][0];
        Point lowest = contours[i][0];

        if (isInRoi(roi, contours[i]) && arcLength(contours[i], false) > 400) {
            drawContours(src, contours, i, Scalar(0, 0, 255), 2, 8, hierarchy, 0, Point());
            drawContours(cdst, contours, i, Scalar(0, 0, 255), 2, 8, hierarchy, 0, Point());

            counter++;

            if(counter > 2) {
                return frame;
            }

            for (int j = 1; j <contours[i].size(); ++j) {
                if(highest.y > contours[i][j].y) highest = contours[i][j];
                if(lowest.y < contours[i][j].y) lowest = contours[i][j];
            }

            if(highest.x < lowest.x)
                for (int j = 1; j <contours[i].size(); ++j) {
                    if(highest.x > contours[i][j].x) highest = contours[i][j];
                }
            else
                for (int j = 1; j <contours[i].size(); ++j) {
                    if(highest.x < contours[i][j].x) highest = contours[i][j];
                }

            circle(src, highest, 3, Scalar(255, 0 ,0), 5);
            circle(src, lowest, 3, Scalar(0, 255 ,0), 5);

            double a, ipot;

            Point x(highest.x, lowest.y);

            a = distance(lowest, x);
            ipot = distance(lowest, highest);

            int side = lowest.x < width/2 ? 0 : 1;

            if(lowest.x < highest.x)
                if(!side) angle[side] = acos(a/ipot)*180/CV_PI;
                else angle[side] = 180 - acos(a/ipot)*180/CV_PI;
            else
            if(side) angle[side] = acos(a/ipot)*180/CV_PI;
            else angle[side] = 180 - acos(a/ipot)*180/CV_PI;

            putText(src, to_string(angle[id]), Point(lowest.x-40, lowest.y-20), FONT_HERSHEY_COMPLEX, 0.5, Scalar(0,0,0));

            if(counter == 2)
            {
                string direction = angle[0] > 90 || angle[1] < 50? "LEFT" :
                                   angle[0] < 50 || angle[1] > 90? "RIGHT" : "FRONT";
                putText(src, direction, Point(width/2-100, height/2), FONT_HERSHEY_COMPLEX, 2, Scalar(255, 0, 0));
            }
        }
    }

    if(counter == 1)
        if(angle[0] != 0)
        {
            string direction = angle[0] > 90? "LEFT" :
                               angle[0] < 50? "RIGHT" : "FRONT";
            putText(src, direction, Point(width/2-100, height/2), FONT_HERSHEY_COMPLEX, 2, Scalar(255, 0, 0));
        }
        else
        {
            string direction = angle[1] < 50? "LEFT" :
                               angle[1] > 90? "RIGHT" : "FRONT";
            putText(src, direction, Point(width/2-100, height/2), FONT_HERSHEY_COMPLEX, 2, Scalar(255, 0, 0));
        }

    jobject result = (jobject) &src;

    return result;*/

    return frame;
}

bool isInRoi(Rect roi, vector<Point> contour)
{
    for(int i = 0; i < contour.size(); i++)
        if(roi.contains(contour[i]))
            return true;

    return false;
}

double distance(Point x1, Point x2)
{
    return sqrt(pow(x1.x-x2.x,2)+pow(x1.y-x2.y,2));
}

}
