package com.example.mgide.myapplication;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static com.example.mgide.myapplication.R.*;


public class MainActivity extends AppCompatActivity implements OnTouchListener,CvCameraViewListener2{
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba;

    private Scalar mBlobColorHsv;
    private Scalar mBlobColorRgba;


    private Scalar mYellowRgba;
    private Scalar mOrangeRgba;
    private Scalar mBlueRgba;
    private Scalar mPinkRgba;
    private Scalar mGreenRgba;

    private ArrayList<Scalar> colorRGBList = new ArrayList<Scalar>();
    private ArrayList<String> colorNamesList = new ArrayList<String>();

    private ArrayList colorDistances = new ArrayList();

    private double minColorDistance  ;
    private double colorDist ;
    private int closestColorIndex;
    private String closestColor;

    double x = -1;
    double y = -1;
    TextView touch_coordinates;
    TextView touch_color;

    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        touch_coordinates = (TextView) findViewById(R.id.touch_coordinates);
        touch_color = (TextView) findViewById(R.id.touch_color);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_tutorial_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // TODO
        // Change these values based on your observations !!
        mYellowRgba = new Scalar(255,255,0);
        mOrangeRgba = new Scalar(255,0,0);
        mBlueRgba = new Scalar(0,255,0);
        mPinkRgba = new Scalar(255,10,0);
        mGreenRgba = new Scalar(0,255,0);
        //

        colorRGBList.add(mYellowRgba);
        colorNamesList.add("Yellow");

        //TODO
        //Add other RGB values to the colorRGBList and corresponding names to the colorNamesList in the same way as shown above

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug())
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0,this,mLoaderCallBack);
        }
        else
        {
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mOpenCvCameraView !=null)
            mOpenCvCameraView.disableView();

    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        mRgba = inputFrame.rgba();
        return mRgba;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        double yLow = (double)mOpenCvCameraView.getHeight() * 0.2401961;
        double yHigh = (double)mOpenCvCameraView.getHeight() * 0.7696078;
        double xScale = (double)cols / (double)mOpenCvCameraView.getWidth();
        double yScale = (double)rows / (yHigh - yLow);
        x = event.getX();
        y = event.getY();
        y = y - yLow;
        x = x * xScale;
        y = y * yScale;
        if((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
        touch_coordinates.setText("X: " + String.format("%3.2f",x) + ", Y: " + String.format("%3.2f",y));
        Rect touchedRect = new Rect();

        touchedRect.x = (int)x;
        touchedRect.y = (int)y;

        touchedRect.width = 8;
        touchedRect.height = 8;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);


         //TODO
        minColorDistance =  0; //Change this value to maximum distance possible or at least a very large value
        closestColorIndex = 0;
        for(int i = 0;  i < colorRGBList.size(); i++)
        {
            colorDist  = getColorDifference(colorRGBList.get(i),mBlobColorRgba);


            if (colorDist < minColorDistance) {
                closestColorIndex = i;
                minColorDistance = colorDist;
            }
        }

        closestColor = colorNamesList.get(closestColorIndex);




        //Display RGB values for average color in 8 x 8 area around touched point
        touch_color.setText("Color: R = " + String.format("%d", (int)mBlobColorRgba.val[0])
                + ", G = " + String.format("%d",(int)mBlobColorRgba.val[1])
                + ", B = " + String.format("%d",(int)mBlobColorRgba.val[2])
                + ", Closest Color = " + closestColor);


        // Uncomment next 4 lines to see Hex code instead of RGB
        // touch_color.setText("Color: #" + String.format("%02X", (int)mBlobColorRgba.val[0])
        //        + String.format("%02X", (int)mBlobColorRgba.val[1])
        //        + String.format("%02X", (int)mBlobColorRgba.val[2])
        //        + ", Closest Color = " + closestColor );

        touch_color.setTextColor(Color.rgb((int) mBlobColorRgba.val[0],
                (int) mBlobColorRgba.val[1],
                (int) mBlobColorRgba.val[2]));
        touch_coordinates.setTextColor(Color.rgb((int)mBlobColorRgba.val[0],
                (int)mBlobColorRgba.val[1],
                (int)mBlobColorRgba.val[2]));

        return false;
    }

    private double getColorDifference(Scalar color1, Scalar color2)
    {
        double colorDiffVal = 0 ;

        for(int i = 0; i < 3; i++)
        {
           //TODO
            // Write expression to compute sum of squared difference of RGB values of color1 and color2
            // HINT:  RGB values in scalar x are stored in  x.val[0], x.val[1] and x.val[2] respectively
        }
        return colorDiffVal;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }


}