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
import org.opencv.core.Scalar;
import org.wtfrobot.rosmin.imageprocess.ColorBlobDetectionView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Window;
import android.widget.Button;

 public class PolygonDetect extends Activity {
  
  private static final String TAG = "PolygonDetect";
  public ColorBlobDetectionView mView;
  public TextToSpeech mTTS;
  public boolean polygonDetect = true;
  
     private BaseLoaderCallback  mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
          switch (status) {
          case LoaderCallbackInterface.SUCCESS:
          {
            Log.i(TAG, "OpenCV loaded successfully");
            // Create and set View
            mView = new ColorBlobDetectionView(mAppContext);
            mView.changeBlobColor("RedPaper");
            mView.switchFrontCamera(true);
            mView.setResolution(1);
//            mView.mDetector.setColorRadius(new Scalar(25,50,50,0));
            mView.mDetector.polygonFlag = true;
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
        polygonDetect = false;
  }  

  @Override
  protected void onResume() {
        Log.i(TAG, "onResume");
    super.onResume();
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
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack))
        {
          Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
        
        //TTS
        OnInitListener ttsInitListener = null;
        mTTS = new TextToSpeech(this,ttsInitListener);
        
        Thread tPolygonDetect = new Thread(new Runnable(){
          long lVertex = 0;
          String strTTS = "";
          public void run(){
            try{
              while(polygonDetect){    
                Thread.sleep(3000);
                lVertex = mView.mDetector.vertex;
                if (lVertex > 2 && lVertex < 10)
                {
                  if (lVertex == 3)  strTTS = "三角形";
                  else strTTS = String.valueOf(lVertex)+"边形";  
                  mTTS.speak(
                    strTTS,
                    TextToSpeech.QUEUE_FLUSH,null
                    );
                }
              }
            }catch(Throwable t){
              
            }
          }
         });
         tPolygonDetect.start();
    }
}
