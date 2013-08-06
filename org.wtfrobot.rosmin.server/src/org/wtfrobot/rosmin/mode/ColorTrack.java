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
package org.wtfrobot.rosmin.mode;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.wtfrobot.rosmin.imageprocess.ColorBlobDetectionView;
import org.wtfrobot.rosmin.imageprocess.ColorBlobDetector;
import org.wtfrobot.rosmin.server.ArduinoControl;
import org.wtfrobot.rosmin.server.ControlPanel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;

 public class ColorTrack extends Activity {
  
  private static final String TAG = "ColorTrack";
  public ColorBlobDetectionView mView;
  public Handler Handler01 = new Handler(){};
  int angleH = 0;
  int angleV = 0;
  
     private BaseLoaderCallback  mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
          switch (status) {
          case LoaderCallbackInterface.SUCCESS:
          {
            Log.i(TAG, "OpenCV loaded successfully");
            // Create and set View
            mView = new ColorBlobDetectionView(mAppContext);
            mView.changeBlobColor("PingPong");
            mView.switchFrontCamera(true);
            mView.setResolution(2);
            setContentView(mView);
            // Check native OpenCV camera
            if( !mView.openCamera() ) {
              AlertDialog ad = new AlertDialog.Builder(mAppContext).create();
              ad.setCancelable(false); // This blocks the 'BACK' button
              ad.setMessage("Fatal error: can't open camera!");
              ad.setButton("OK", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
                  }
              });
              ad.show();
            }
          } break;
          default:
          {
            super.onManagerConnected(status);
          } break;
        }
        }
    };
  
  public ColorTrack()
  {
    Log.i(TAG, "Instantiated new " + this.getClass());
  }

    @Override
  protected void onPause() {
        Log.i(TAG, "onPause");
    super.onPause();
    if (null != mView)
      mView.releaseCamera();
  }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        mView.removeCallbacks(rImageControl);
  }  

  @Override
  protected void onResume() {
        Log.i(TAG, "onResume");
    super.onResume();
    ControlPanel.mArduinoControl.onResume();
    if( (null != mView) && !mView.openCamera() ) {
      AlertDialog ad = new AlertDialog.Builder(this).create();
      ad.setCancelable(false); // This blocks the 'BACK' button
      ad.setMessage("Fatal error: can't open camera!");
      ad.setButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        finish();
          }
      });
      ad.show();
    }
  }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

//----------------------------------
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack))
        {
          Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
        
        Thread tColorControl = new Thread(new Runnable(){
          public void run(){
            try{
              int deltaT = 100;
              while(true){
                deltaT = (int) (ColorBlobDetector.distance/433*110 + 30);
                Thread.sleep(deltaT);
                Handler01.post(rImageControl);
              }
            }catch(Throwable t){
              
            }
          }
         });
         tColorControl.start();
     
    }
    
    public Runnable rImageControl = new Runnable()
    {
      public void run()
      {
        if (ColorBlobDetectionView.contourFlag)
        {
          angleH = ColorBlobDetector.angleH;
//          if (angleH < 0)
//            setContentView(R.layout.i2);
          angleV = ColorBlobDetector.angleV;
//          if (angleH > 0)
//            setContentView(R.layout.i4);
          ControlPanel.mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte) 0xa, (angleH+90) ,(angleV+90));
        }
      }
    };
}