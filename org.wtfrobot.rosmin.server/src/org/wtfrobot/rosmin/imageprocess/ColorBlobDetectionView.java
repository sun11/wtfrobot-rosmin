/*
Copyright (c) 2013 WTFRobot

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

  * The name of the copyright holders may not be used to endorse or promote products
    derived from this software without specific prior written permission.

This software is provided by the copyright holders and contributors "as is" and
any express or implied warranties, including, but not limited to, the implied
warranties of merchantability and fitness for a particular purpose are disclaimed.
In no event shall the Intel Corporation or contributors be liable for any direct,
indirect, incidental, special, exemplary, or consequential damages
(including, but not limited to, procurement of substitute goods or services;
loss of use, data, or profits; or business interruption) however caused
and on any theory of liability, whether in contract, strict liability,
or tort (including negligence or otherwise) arising in any way out of
the use of this software, even if advised of the possibility of such damage.
*/
package org.wtfrobot.rosmin.imageprocess;

import java.io.IOException;
import java.util.List;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

public class ColorBlobDetectionView extends SampleViewBase {

  public ColorBlobDetector mDetector = new ColorBlobDetector();
  private Mat mYuv;
  private Mat mRgba;
  public static boolean contourFlag = true;
  
//ping pong ball:
  public Scalar colorPingPong = new Scalar(25.34375,215.578125,218.46875,0);
//red paper:
  public Scalar colorRedPaper = new Scalar(249.75,126.5,224.609375,0);
//tennis ball:
  public Scalar colorTennisBall = new Scalar(58.65625,131.65625,134.875,0);
  
	private Scalar mBlobColorRgba = new Scalar(255);
	public Scalar mBlobColorHsv = colorPingPong;

	
	private Mat mSpectrum = new Mat();
	private static Size SPECTRUM_SIZE = new Size(200, 32);
	private MenuItem  mItemYellow;
	private MenuItem mItemRed;
	private MenuItem mItemPolygon;
	private MenuItem mItemTrack;

	// Logcat tag
	private static final String TAG = "Example/ColorBlobDetection";
	
	private static final Scalar CONTOUR_COLOR = new Scalar(255,0,0,255);
	
	
	public ColorBlobDetectionView(Context context)
	{
        super(context);
        
	}
	
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (this) {
            // initialize Mat before usage
            mRgba = new Mat();
        }
        
        super.surfaceCreated(holder);
    }
	
		
	@Override
	protected void onPreviewStarted(int previewWidtd, int previewHeight) {
		
	    mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2, getFrameWidth(), CvType.CV_8UC1);
        mRgba = new Mat();
	}

	@Override
	protected void onPreviewStopped() {
		
        // Explicitly deallocate Mats
        if (mYuv != null)
            mYuv.release();
        if (mRgba != null)
            mRgba.release();

        mYuv = null;
        mRgba = null;
		
	}

	@Override
	protected Bitmap processFrame(byte[] data) {
		mYuv.put(0,0,data);
		Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB , 4);
		
        Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        
    	
   		mDetector.setHsvColor(mBlobColorHsv);
   		Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
        
   		   //draw contours
        	mDetector.process(mRgba);
        	List<MatOfPoint> contours = mDetector.getContours();
        	if (contours.isEmpty()) contourFlag = false;
        	else contourFlag = true;
            Log.e(TAG, "Contours count: " + contours.size());
        	Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
            
            Mat colorLabel = mRgba.submat(2, 34, 2, 34);
            colorLabel.setTo(mBlobColorRgba);
            
            Mat spectrumLabel = mRgba.submat(2, 2 + mSpectrum.rows(), 38, 38 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);

        try {
        	Utils.matToBitmap(mRgba, bmp);
        } catch(Exception e) {
        	Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
            bmp.recycle();
            bmp = null;
        }
        
        return bmp;
	}
	
	private Scalar converScalarHsv2Rgba(Scalar hsvColor)
	{	
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
        
        return new Scalar(pointMatRgba.get(0, 0));
	}
	
	private Scalar converScalarRgba2Hsv(Scalar rgbaColor)
	{	
        Mat pointMatRgba = new Mat(1, 1, CvType.CV_8UC3, rgbaColor);
        Mat pointMatHsv = new Mat();
        Imgproc.cvtColor(pointMatRgba, pointMatHsv, Imgproc.COLOR_RGB2HSV_FULL, 4);
        
        return new Scalar(pointMatRgba.get(0, 0));
	}
	
	public void changeBlobColor(String a)
	{
	  if (a.equals("PingPong"))  mBlobColorHsv = colorPingPong;
	  else if (a.equals("RedPaper")) mBlobColorHsv = colorRedPaper;
	  else if (a.equals("TennisBall")) mBlobColorHsv = colorTennisBall;
	}

	
    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
            if (mRgba != null)
                mRgba.release();

            mRgba = null;
        }
    }
}
