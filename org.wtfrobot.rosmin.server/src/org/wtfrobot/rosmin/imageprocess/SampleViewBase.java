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
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class SampleViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    protected static final String TAG = "Sample::SurfaceView";

    private Camera              mCamera;
    private SurfaceHolder       mHolder;
    private int                 mFrameWidth;
    private int                 mFrameHeight;
    private byte[]              mFrame;
    private boolean             mThreadRun;
    private byte[]              mBuffer;
    public boolean              frontCameraFlag = false;
    public  int                 cameraResolution = 0;


    public SampleViewBase(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public void setPreview() throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mCamera.setPreviewTexture( new SurfaceTexture(10) );
        else
        	mCamera.setPreviewDisplay(null);
	}
    
    public boolean openCamera() {
        Log.i(TAG, "openCamera");
        releaseCamera();
        
        if (!frontCameraFlag)
        mCamera = Camera.open();
        
        // open the front camera
        else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
             CameraInfo info = new CameraInfo();
             Camera.getCameraInfo(i, info);
             if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
              mCamera = Camera.open(i);
             } 
            }
           }
          if(mCamera == null) 
            mCamera = Camera.open();
        }
        
        if(mCamera == null) {
        	Log.e(TAG, "Can't open camera!");
        	return false;
        }

        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (SampleViewBase.this) {
                    System.arraycopy(data, 0, mFrame, 0, data.length);
                    SampleViewBase.this.notify(); 
                }
                camera.addCallbackBuffer(mBuffer);
            }
        });
        return true;
    }
    
    public void releaseCamera() {
        Log.i(TAG, "releaseCamera");
        mThreadRun = false;
        synchronized (this) {
	        if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
        onPreviewStopped();
    }
    
    public void setupCamera(int width, int height) {
        Log.i(TAG, "setupCamera");
        synchronized (this) {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();
//                mFrameWidth = 720;
//                mFrameHeight = 480;

                mFrameWidth = sizes.get(cameraResolution).width;
                mFrameHeight = sizes.get(cameraResolution).height;
                
                // selecting optimal camera preview size
//                {
//                    int  minDiff = Integer.MAX_VALUE;
//                    for (Camera.Size size : sizes) {
//                        if (Math.abs(size.height - height) < minDiff) {
//                            mFrameWidth = size.width;
//                            mFrameHeight = size.height;
//                            minDiff = Math.abs(size.height - height);
//                        }
//                    }
//                }

                params.setPreviewSize(getFrameWidth(), getFrameHeight());
                
                List<String> FocusModes = params.getSupportedFocusModes();
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                {
                	params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }            
                
                mCamera.setParameters(params);
                
                /* Now allocate the buffer */
                params = mCamera.getParameters();
                int size = params.getPreviewSize().width * params.getPreviewSize().height;
                size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                mBuffer = new byte[size];
                /* The buffer where the current frame will be copied */
                mFrame = new byte [size];
                mCamera.addCallbackBuffer(mBuffer);

    			try {
    				setPreview();
    			} catch (IOException e) {
    				Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
    			}

                /* Notify that the preview is about to be started and deliver preview size */
                onPreviewStarted(params.getPreviewSize().width, params.getPreviewSize().height);

                /* Now we can start a preview */
                mCamera.startPreview();
            }
        }
    }
    
    public void switchFrontCamera(boolean Flag){
      if (Flag) frontCameraFlag = true;
      else frontCameraFlag      = false;
    }
    
    public void setResolution(int a){
      cameraResolution = a;
    }
    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        setupCamera(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        releaseCamera();
    }



    /* The bitmap returned by this method shall be owned by the child and released in onPreviewStopped() */
    protected abstract Bitmap processFrame(byte[] data);

    /**
     * This method is called when the preview process is being started. It is called before the first frame delivered and processFrame is called
     * It is called with the width and height parameters of the preview process. It can be used to prepare the data needed during the frame processing.
     * @param previewWidth - the width of the preview frames that will be delivered via processFrame
     * @param previewHeight - the height of the preview frames that will be delivered via processFrame
     */
    protected abstract void onPreviewStarted(int previewWidtd, int previewHeight);

    /**
     * This method is called when preview is stopped. When this method is called the preview stopped and all the processing of frames already completed.
     * If the Bitmap object returned via processFrame is cached - it is a good time to recycle it.
     * Any other resources used during the preview can be released.
     */
    protected abstract void onPreviewStopped();

    public void run() {
        mThreadRun = true;
        Log.i(TAG, "Starting processing thread");
        while (mThreadRun) {
            Bitmap bmp = null;

            synchronized (this) {
                try {
                    this.wait();
                    bmp = processFrame(mFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
//                	//Change to support portrait view
//                    Matrix matrix = new Matrix();
//                    matrix.preTranslate( (canvas.getWidth() - bmp.getWidth())/2,
//                                   (canvas.getHeight() - bmp.getHeight()) / 2);
//                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
//                    matrix.postRotate(90f,(canvas.getWidth()) / 2,(canvas.getHeight()) / 2);
                    canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}