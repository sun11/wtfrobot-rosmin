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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.graphics.Bitmap;
import android.util.Log;

public class ColorBlobDetector
{
  public boolean polygonFlag = false;
	public long vertex = 0;
	public static int angleH  = 0;
	public static int angleV  = 0;
	public static double maxArea = 0;
	public static double distance = 0;
	
	public void setColorRadius(Scalar radius)
	{
		mColorRadius = radius;
	}
	
	public void setHsvColor(Scalar hsvColor)
	{
	    double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0; 
    	    double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

  		mLowerBound.val[0] = minH;
   		mUpperBound.val[0] = maxH;

  		mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
   		mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

  		mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
   		mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

   		mLowerBound.val[3] = 0;
   		mUpperBound.val[3] = 255;

   		Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

 		for (int j = 0; j < maxH-minH; j++)
   		{
   			byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
   			spectrumHsv.put(0, j, tmp);
   		}

   		Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);

	}
	
	public Mat getSpectrum()
	{
		return mSpectrum;
	}
	
	public void setMinContourArea(double area)
	{
		mMinContourArea = area;
	}
	
	public Bitmap process(Mat rgbaImage)
	{
    	Mat pyrDownMat = new Mat();

    	Imgproc.pyrDown(rgbaImage, pyrDownMat);
    	Imgproc.pyrDown(pyrDownMat, pyrDownMat);
      	Mat hsvMat = new Mat();
    	Imgproc.cvtColor(pyrDownMat, hsvMat, Imgproc.COLOR_RGB2HSV_FULL);

    	Mat Mask = new Mat();
    	Core.inRange(hsvMat, mLowerBound, mUpperBound, Mask);
    	Mat dilatedMask = new Mat();
    	Imgproc.dilate(Mask, dilatedMask, new Mat());
    //    find contours
      List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
      Mat hierarchy = new Mat();

      Imgproc.findContours(dilatedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
      
      if (contours.isEmpty()) Log.i("contours","contours empty");

      // Find max contour area
      maxArea = 0;
      Iterator<MatOfPoint> each = contours.iterator();
      while (each.hasNext())
      {
      	MatOfPoint wrapper = each.next();
      	double area = Imgproc.contourArea(wrapper);
      	if (area > maxArea)
      		maxArea = area;
      }

      // Filter contours by area and resize to fit the original image size
      mContours.clear();
      each = contours.iterator();
      while (each.hasNext())
      {
      	MatOfPoint contour = each.next();
      	
      	if (Imgproc.contourArea(contour) > mMinContourArea*maxArea)
      	{
      		Core.multiply(contour, new Scalar(4,4), contour);
      		mContours.add(contour);
      	}
      }
        
      try {
            int indexMaxArea = 0;
            for (int i = 0; i < contours.size(); i++) {
                if(Imgproc.contourArea(contours.get(i)) > maxArea){
                    indexMaxArea = i;
                    maxArea = Imgproc.contourArea(contours.get(i));
                }
            } 
            
     Log.i("MaxArea","S="+ String.valueOf(maxArea));
			
		// draw center
         MatOfPoint iContour = contours.get(indexMaxArea);
         Moments  mMoments =   Imgproc.moments(iContour);
         double x = mMoments.get_m10()/mMoments.get_m00();
         double y = mMoments.get_m01()/mMoments.get_m00();
         Point center = new Point(x,y);
         distance = Math.sqrt( (x-360)*(x-360) + (y-240)*(y-240) );
         Core.circle(rgbaImage,center,3,new Scalar(0,255,0),3,8,0);
         

    divideArea(center);
//    Point center1 = center;
//    if (center1.equals(center)){
//      angleV = 0;
//      angleH = 0;
//    }

      if(polygonFlag)
      {
     		MatOfPoint2f fContour = new MatOfPoint2f();
     		iContour.convertTo(fContour, CvType.CV_32FC2);
     		Log.d("SAMPLE", "MatOfPoint2f = " + fContour);
     		Log.d("SAMPLE", "MatOfPoint2f = " + fContour.dump());	
     		MatOfPoint2f approxCurve = new MatOfPoint2f();
     		Imgproc.approxPolyDP(fContour, approxCurve, 25, true);
     
     		vertex = approxCurve.total();
     		
     		Log.i("vertex","found"+vertex);
      }
 		
     } catch (Exception e) {
              e.printStackTrace();
     }
        
        
		Bitmap bmp = Bitmap.createBitmap(rgbaImage.cols(), rgbaImage.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(rgbaImage, bmp);
        return bmp;
		
	}
	
	public void divideArea(Point center)
	{
	  double PixelX = center.x;
	  double PixelY = center.y;
//	  int LatticeM  = (int) PixelX/64;
//	  int LatticeN  = (int) PixelY/48;
//	  
//	  int AngleNumH = Lattice2AngleNum(LatticeM);
//	  int AngleNumV = Lattice2AngleNum(LatticeN);
////	  angleH = (-1)*5*AngleNumH;
////	  angleV = (-1)*5*AngleNumV;
//	  if (AngleNumH > 1)
//	    angleH = -1;
//	  else if (AngleNumH < -1) angleH = 1;
//	  if (AngleNumV > 1)
//	    angleV = -1;
//	  else if (AngleNumV < -1) angleV = 1;
	  
//	  if (PixelX > 280 && PixelX < 360 )
//	    angleH = 0;
//	  else if (PixelX <= 280) angleH = 2;
//	  else if (PixelX >= 360) angleH = -2;
//	  if (PixelY > 220 && PixelY < 260 )
//	    angleV = 0;
//	  else if (PixelY <= 220) angleV = 1;
//	  else if (PixelY >= 260) angleV = -1;
	  
	  if (PixelX > 288 && PixelX < 432 )
      angleH = 0;
//	  else if (PixelX <= 288 && Pixel)
    else if (PixelX <= 288) angleH = 1;
    else if (PixelX >= 432) angleH = -1;
    if (PixelY > 192 && PixelY < 288 )
      angleV = 0;
    else if (PixelY <= 192) angleV = 1;
    else if (PixelY >= 288) angleV = -1;
	}
	
	public int Lattice2AngleNum(int lattice)
	{
	  int angleNum = 0;
	   // four area in the middle is center 
	  if (lattice == 4||lattice == 5)
	    angleNum = 0;
	  if (lattice < 4)
	    angleNum = lattice - 4;
	  if (lattice > 5)
	    angleNum = lattice - 5;
	  return angleNum;	    
	}

	public List<MatOfPoint> getContours()
	{
		return mContours;
	}

	// Lower and Upper bounds for range checking in HSV color space
	private Scalar mLowerBound = new Scalar(0);
	private Scalar mUpperBound = new Scalar(0);
	// Minimum contour area in percent for contours filtering
	private static double mMinContourArea = 0.1;
	// Color radius for range checking in HSV color space
	private Scalar mColorRadius = new Scalar(25,50,50,0);
	private Mat mSpectrum = new Mat();
	private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();;
}
