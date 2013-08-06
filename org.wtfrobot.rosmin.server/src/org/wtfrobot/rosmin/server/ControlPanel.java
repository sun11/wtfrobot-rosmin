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
package org.wtfrobot.rosmin.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.wtfrobot.rosmin.imageprocess.ColorBlobDetectionView;
import org.wtfrobot.rosmin.imageprocess.ColorBlobDetector;
import org.wtfrobot.rosmin.mode.ColorTrack;
import org.wtfrobot.rosmin.mode.ObjFind;
import org.wtfrobot.rosmin.mode.QR_MODE;
import org.wtfrobot.rosmin.mode.PolygonDetect;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;


public class ControlPanel extends Activity implements SensorEventListener
{
  public static boolean bIfDebug = true;
  public static String TAG = "HIPPO_DEBUG";
  public static String strDebugPreFix = "EX08_27:";
  public boolean colorTrackFlag = false;
 
  private MenuItem   mItemColorTrack;
  private MenuItem   mItemQRReader;
  private MenuItem   mItemObjFind;
  private MenuItem   mItemPolygonDetection;
  private MenuItem   mItemMonitor;
  
  public static final int     COLOR_TRACK_MODE     = 0;
  public static final int     QR_MODE              = 1;
  public static final int     MONITOR_MODE         = 2;
  public static final int     OBJ_FIND_MODE        = 3;
  public static final int     POLYGON_DETECT_MODE  = 4;
  private int mMode;
  

  
  //Socket Server
  private ServerSocket mServerSocket01;
  private Socket mSocket01;
  private int intSocketServerPort = 8080;
  
  //Socket Client
  private Socket mSocket02;
  public Handler Handler01 = new Handler(){};
  
  //UI
  private TextView mTextView01;
  private TextView mTextView02;
  private Button mButton01,mButton02,mButton03;  
  private String strTemp01="";
  public String msg="";
  public String contents="";
  
  //TTS
  public static TextToSpeech mTTS;
  
  
  //Arduino Control
  public static ArduinoControl mArduinoControl; //需要在ArduinoControl中定义发送和接受数据的格式
  public static int angleH = 0;
  public static int angleV = 0;
  public static boolean rotateFlag = false;
  String strMove = "";
  public static String strCommand = "";
  public static String strQR ="0";
  public static byte[] byteReceive = {10,10,10,10};

  public static boolean leftFlag = false;
//常亮  
  PowerManager powerManager = null; 
  WakeLock wakeLock = null;
  
  //Sensor 
  public SensorManager sensorManager;
  public double accelerometer ;
  Sensor accSensor = null;
  Sensor oriSensor = null;
  Sensor gyrSensor = null;
  public int u1=0,u2=0;
  public float corrected_a=0;
  public static float corrected_gx=0;
  public static float corrected_gy=0;
  public static float corrected_gz=0;
  public static float gyrAngle=0,rotateAngle = 90;
  public static float tempGyr = 0;
  public static float init_gx,init_gy,init_gz;
  public static float init_gyr;
  public float mCorrectGX=0,mCorrectGY=0,mCorrectGZ=0;
  public static float[] correctGX,correctGY,correctGZ;
  public float[] correctAzimuth;
  public float gyrX,gyrY,gyrZ;
  public float accX,accY,accZ;
  public float oriA,oriP,oriR; //azimuth方位角；pitch倾斜角，上下翻的角度；roll滚动角，左右翻的角度
  public boolean correctFlag = true;  //得到init数值后转false 不再计算得init数值
  public float td;
  public static String strMoveFlag = "S";  
  public static String tempStr = "";

 
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {    
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    //屏幕常亮
    this.powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE); 
    this.wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock"); 
    
    mArduinoControl=new ArduinoControl(this); 
    
    mTextView01 = (TextView)findViewById(R.id.myTextView1);
    mTextView02 = (TextView)findViewById(R.id.myTextView2);
    mButton01 = (Button)findViewById(R.id.myButton1);
    mButton02 = (Button)findViewById(R.id.myButton2);
    mButton02.setEnabled(false);
    
    // Run Socket Server
    mButton01.setOnClickListener(new Button.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        // TODO Auto-generated method stub
        mButton01.setEnabled(false);
        mButton02.setEnabled(true);
        setContentView(R.layout.i3);
        Thread thread = new Thread(new mSocketConnectRunnable1());
        thread.start();
      }
    });
    
    //Stop Socket Server
    mButton02.setOnClickListener(new Button.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        // TODO Auto-generated method stub
        try
        {
          mButton01.setEnabled(true);
          mButton02.setEnabled(false);
          
          closeSocket();
          strTemp01="Socket Server Closed.";
          Handler01.post(rManualControl);
        }
        catch (Exception e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
    
    //TTS
    OnInitListener ttsInitListener = null;
    mTTS = new TextToSpeech(this,ttsInitListener);
    
    //Sensor
    sensorManager = (SensorManager)this.getSystemService(SENSOR_SERVICE);       
    oriSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    gyrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    
    correctGX = new float[5];
    correctGY = new float[5];
    correctGZ = new float[5];
    correctAzimuth = new float[5];
    
    //对得到的陀螺仪数据进行处理
    Thread accDispose = new Thread(new Runnable()
    {       
      public void run() { 
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        long   t1 = 0,t2 = 0 ;
        float temp;
         while(true)
        {
            t1 = System.currentTimeMillis(); 
            try {
            for(int i=0;i<5;i++)
            {
              correctGX[i] = gyrX;
              correctGY[i] = gyrY;
              correctGZ[i] = gyrZ;
              correctAzimuth[i] =  oriA;
              if(i<4)
			  {
              Thread.sleep(5);
			  }
            }
            //冒泡排序
            for(int y=0;y<4;y++)
            {
              for(int x=0;x<4-y;x++)
              {
                if(correctGY[x]>correctGY[x+1])//陀螺仪Z方向数据交换
                {       
                  temp=correctGY[x];
                  correctGY[x]=correctGY[x+1];
                  correctGY[x+1]=temp;
                }
              }
            }
            if(correctFlag)//初始化一次
            { 
              //陀螺仪的各轴初始值
              gyrInit();
              correctFlag = false;
            }
            //去掉数组中最大最小的数，其余3个数据相加
            corrected_gx = (correctGX[1]+correctGX[2]+correctGX[3])/3 - init_gx;
            corrected_gy = (correctGY[1]+correctGY[2]+correctGY[3])/3 - init_gy;
            corrected_gz = (correctGZ[1] +correctGZ[2]+correctGZ[3])/3 - init_gz;
            corrected_a = (correctAzimuth[1]+correctAzimuth[2]+correctAzimuth[3])/3;
			//去掉小值干扰
            if( Math.abs(corrected_gx) < 0.04 )
            { corrected_gx=0; }
            if( Math.abs(corrected_gy) < 0.04 )
            { corrected_gy=0; }
            if( Math.abs(corrected_gz) < 0.04 )
            { corrected_gz=0; }
                      
            t2 = System.currentTimeMillis(); 
            td = (float)(t2 - t1)/1000;
            if(accX>3)
            {
            gyrAngle = (float) (gyrAngle + corrected_gx*td*57.296);
            }
            if(accX<-3)
            {
            gyrAngle = (float) (gyrAngle - corrected_gx*td*57.296);
            }
          } catch (InterruptedException e1) {
            e1.printStackTrace();
          }
        
        }//while          
      }//run
    });
     accDispose.start();
     
     //用于接受从Arduino发来的数据，10HZ
     Thread receiveArduino = new Thread(new Runnable()
     {
       public void run(){
         while(true){
           try
           {
             Thread.sleep(100);
           }
           catch (InterruptedException e)
           {
             // TODO Auto-generated catch block
             e.printStackTrace();
           }
           byteReceive = ControlPanel.mArduinoControl.getCommand();
         }
       }
     });
     receiveArduino.start();
     
  }
  public static void gyrInit()
  {
    corrected_gx=0;
    corrected_gy=0;
    corrected_gz=0;
			
    init_gx = (correctGX[1]+correctGX[2]+correctGX[3])/3 ;
    init_gy = (correctGY[1]+correctGY[2]+correctGY[3])/3 ;          
    init_gz = (correctGZ[1] +correctGZ[2]+correctGZ[3])/3;
  }

  @Override
  protected void onStart()
  {
        super.onStart();     
  }
  
  @Override
  protected void onPause()
  {
    super.onPause();
    wakeLock.release();
  }
  
  @Override
  public void onDestroy(){
      super.onDestroy();
      exitActivity(1);
      mArduinoControl.onDestroy();
      sensorManager.unregisterListener(this);
}  
  
  @Override
  public void onResume(){
      super.onResume();
      mTextView02.setText(msg);
      wakeLock.acquire();
      sensorManager.registerListener(this,oriSensor,SensorManager.SENSOR_DELAY_UI);
      sensorManager.registerListener(this,gyrSensor,SensorManager.SENSOR_DELAY_UI);
      sensorManager.registerListener(this,accSensor,SensorManager.SENSOR_DELAY_UI);
      mArduinoControl.onResume();
  }
  
  
  //Socket Server
  public class mSocketConnectRunnable1 implements Runnable
  {
    @Override
    public void run()
    {
      // TODO Auto-generated method stub
      try
      {
        mServerSocket01 = new ServerSocket(intSocketServerPort);
        mServerSocket01.setReuseAddress(true);
        Log.i(TAG, strDebugPreFix+"Socket Server is Running: "+intSocketServerPort);

        while (!mServerSocket01.isClosed())
        {  
          mSocket01 = mServerSocket01.accept();
          Thread read = new Thread(new Runnable()
          {
            BufferedReader br = new BufferedReader(new InputStreamReader(mSocket01.getInputStream())); 
            @Override
            public void run()
            {
              try
              {                             
                while (mSocket01.isConnected())
                {                  
                  msg = br.readLine();
                  Handler01.post(rManualControl);
                }
              }
              catch (Exception e)
              {
                if(bIfDebug)
                {
                  Log.e(TAG, e.toString());
                  e.printStackTrace();
                }
              }            
            }
          });
          read.start();       
        
          //在接受数据的同时发送数据，实现双向通信
        Thread write = new Thread (new Runnable()
        {
          @Override
          public void run()
          {
            while (mSocket01.isConnected())
            {
              try
              {
                Thread.sleep(100);
              }
              catch (InterruptedException e)
              {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
              Handler01.post(rSendStr);
            }
          }
        });
         write.start();
        }
      }   
      catch(Exception e)
      {
        if(bIfDebug)
        {
          Log.e(TAG, e.toString());
          e.printStackTrace();
        }
      }
    }
  }  
 
  private void closeSocket()
  {
    try
    {
      if(!mServerSocket01.isClosed())
      {
        mServerSocket01.close();
      }
      if(!mSocket01.isClosed())
      {
        mSocket01.close();
      }
      if(!mSocket02.isClosed())
      {
        mSocket02.close();
      }
    }
    catch(Exception e)
    {
      if(bIfDebug)
      {
        Log.e(TAG, e.toString());
        e.printStackTrace();
      }
    }
  }
   
  public void exitActivity(int exitMethod)
  {
    //throw new RuntimeException("Exit!");
    try
    {
      switch(exitMethod)
      {
        case 0:
          System.exit(0);
          break;
        case 1:
          android.os.Process.killProcess(android.os.Process.myPid());
          break;
        case 2:
          finish();
          break;
      }
    }
    catch(Exception e)
    {
      if(bIfDebug)
      {
        Log.e(TAG, strDebugPreFix+e.toString());
        e.printStackTrace();
      }
      finish();
    }
  }
  
  public Runnable rManualControl = new Runnable()
  {
    public void run()
    {  
     manualControl(msg);
     mTextView02.setText(msg);
    }
  };
  
  public static Runnable rImageControl = new Runnable()
  {
    public void run()
    {
      if (ColorBlobDetectionView.contourFlag)
      {
        angleH = ColorBlobDetector.angleH;
//        if (angleH < 0)
//          setContentView(R.layout.i2);
        angleV = ColorBlobDetector.angleV;
//        if (angleH > 0)
//          setContentView(R.layout.i4);
        mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte) 0xa, (angleH+90) ,(angleV+90));
      }
    }
  };
  
  public Runnable rSensorControl = new Runnable()
  {
    public void run()
      {
        if( Math.abs(gyrAngle - init_gyr) < (rotateAngle - 2))
        {            
          mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte)0x7, 200,0);
        }
        if ( Math.abs(gyrAngle - init_gyr)> (rotateAngle + 2))
        {
          mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte)0x8, 200,0);
        }
        if(Math.abs(gyrAngle - init_gyr) < (rotateAngle + 2)  && Math.abs(gyrAngle - init_gyr) > (rotateAngle - 2))
        {
          mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte)0x0, 1,0);
          rotateFlag = true;
        }
      }
  };
  
  public Runnable rSendStr = new Runnable(){
    public void run()
    {
      try
      {
        BufferedWriter bw = new BufferedWriter( new OutputStreamWriter(mSocket01.getOutputStream()) );
        strMove = strMoveFlag
        + " "  + String.valueOf((int) gyrAngle) 
        + " " + String.valueOf( ( ((int)corrected_a) +((accX>3)?90:180) )%360)    //由于小车姿态问题，需要进行修正
        + " " + strQR
        +" \n";
        bw.write(strMove);
        bw.flush();
        strQR = "0"; // accX>3 
      }
      catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }  
    }
  };
  



  
  public static boolean isNumeric(String checkStr) {
    try {
         Integer.parseInt(checkStr);
         return true; 
     } catch (NumberFormatException err) {
         return false; 
      }
}
  
  public void manualControl(String a)
  {
    basicControl(a);
   // switch mode
    {
      if ( a.contains("ball track") ) setMode(COLOR_TRACK_MODE);
      if ( a.contains("qr mode") ) setMode(QR_MODE);
      if ( a.contains("go to"))  
      {
        if (a.contains("section one"))
          strCommand = "一区";
        else if (a.contains("section two"))
          strCommand = "二区";
        setMode(QR_MODE);
      }
      if ( a.contains("object find") ) setMode(OBJ_FIND_MODE);
      if ( a.contains("monitor mode") ) setMode(MONITOR_MODE);
      if ( a.contains("polygon detect") ) setMode(POLYGON_DETECT_MODE);
    }
  }
  
  //To make map draw available,add it.
  public static void basicControl(String a)
  { 
    if(isNumeric(a))
    {
        int angle = Integer.parseInt(a);
        if(angle >= 90 && angle < 180)//右转
        {
          mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0xa, (180-angle) ,0);
        }
        if(angle < 90 && angle >0 )////左转
        {
          mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x9, (180-angle),0); 
        }
        if(angle>200 && angle<290)//下转
        {
          mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0xc, (angle-200),0);
        }
        if(angle<380 && angle>=290)//上转
        {
          mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0xb, (angle-200),0);
        }
        strMoveFlag = "S";
    }    
    else
    {               
      if( a.equals("forward"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x1, 200,0);
        strMoveFlag = "F";
      }
      if( a.equals("back"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x2, 200,0);
        strMoveFlag = "B";
      }
      if( a.equals("turnLeft"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x3, 200,0);
        strMoveFlag = "F";
      }
      if( a.equals("turnRight"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x4, 200,0);
        strMoveFlag = "F";
      }
      if( a.equals("turnLeft2"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x5, 200,0);
        strMoveFlag = "S";
      }
      if( a.equals("turnRight2"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x6, 200,0);
        strMoveFlag = "S";
      }
      if( a.equals("turnLeftOrigin"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x7, 200,0);
        strMoveFlag = "S";
      }
      if( a.equals("turnRightOrigin"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x8, 200,0);
        strMoveFlag = "S";
      }
      if( a.equals("stop"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
        strMoveFlag = "S";
      } 
      if( a.equals("ok"))
      {
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0xd, 1,0);
        strMoveFlag = "S";
      } 
    } 
  }
  
  
  //Sensor Control
  //传感器获取数据部分
  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // TODO Auto-generated method stub    
  }
  
  @Override
  public void onSensorChanged(SensorEvent e) {
    // TODO Auto-generated method stub  
    switch(e.sensor.getType()){
    case Sensor.TYPE_ORIENTATION:{
      oriA = e.values[SensorManager.DATA_X];
      oriP = e.values[SensorManager.DATA_Y];
      oriR = e.values[SensorManager.DATA_Z];
      break;
    }
    case Sensor.TYPE_GYROSCOPE:{          
      gyrX = e.values[SensorManager.DATA_X];      
      gyrY = e.values[SensorManager.DATA_Y];    
      gyrZ = e.values[SensorManager.DATA_Z];
      break;
    }
    case Sensor.TYPE_ACCELEROMETER:{
      accX = e.values[SensorManager.DATA_X]; 
      accY = e.values[SensorManager.DATA_Y]; 
      accZ = e.values[SensorManager.DATA_Z]; 
    }
    }
  }  
  
 public static void setRotateAngle(int angle)
 {
   rotateFlag = false;
   rotateAngle = angle;
 }
 
 //原地旋转某度数
 public static  Runnable rRotate = new Runnable(){
   public void run()
   {
     if( gyrAngle - init_gyr < (rotateAngle - 2))
     {            
       mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte)0x8, 200,0);
     }
     if (gyrAngle - init_gyr> (rotateAngle + 2))
     {
       mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte)0x7, 200,0);
     }
     if(gyrAngle - init_gyr < (rotateAngle + 2)  && gyrAngle - init_gyr > (rotateAngle - 2))
     {
       rotateFlag = true;
       strMoveFlag = "F";
       mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte) 0xd, 7, 0);
//       try
//       {
//         Thread.sleep(7000);
//         mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x1, 200,0);
//       }
//       catch (InterruptedException e)
//       {
//         // TODO Auto-generated catch block
//         e.printStackTrace();
//       }
//       mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
       
     }
     
     if(!String.valueOf(((int) byteReceive[3]) ).equals(tempStr))
     mTTS.speak(
         String.valueOf(((int) byteReceive[3]) ),
         TextToSpeech.QUEUE_FLUSH,null
         );
     tempStr = String.valueOf(((int) byteReceive[3]) );
   }
 };
 
  //选择控制模式
  public void setMode(int Mode) {
    mMode = Mode;
    switch (mMode) {
      case COLOR_TRACK_MODE:
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
        sendBroadcast(new Intent("com.pas.webcam.CONTROL").putExtra("action", "stop"));
        Intent colortrack_intent = new Intent();
        colortrack_intent.setClass(this, ColorTrack.class);
        colortrack_intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//        ActivityManager mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//        mActivityManager.killBackgroundProcesses("org.wtfrobot.rosmin.mode");
        this.startActivity(colortrack_intent); 
        break;
      case QR_MODE:
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
        sendBroadcast(new Intent("com.pas.webcam.CONTROL").putExtra("action", "stop"));
        Intent qrmode_intent = new Intent();
//        qrmode_intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        qrmode_intent.setClass(this, QR_MODE.class);
        this.startActivity(qrmode_intent);
//        mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte) 0xb, 1 , 1);        
        break;
      case MONITOR_MODE:
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
        Intent ipwebcam = 
          new Intent()
          .setClassName("com.pas.webcam", "com.pas.webcam.Rolling")
          .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
          .putExtra("hidebtn1", true)                // Hide help button
          .putExtra("caption2", "Run in background"); // Change caption on "Actions..."
//          .putExtra("intent2", launcher);            // And give button another purpose       
//        ipwebcam.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        this.startActivityForResult(ipwebcam, 1);
        Log.i(TAG, "Video is started");
        break;
      case OBJ_FIND_MODE:
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
        sendBroadcast(new Intent("com.pas.webcam.CONTROL").putExtra("action", "stop"));
        Intent objfind_intent = new Intent();
        objfind_intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        objfind_intent.setClass(this, ObjFind.class);
        this.startActivity(objfind_intent);
        
        break;
      case POLYGON_DETECT_MODE:
        mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
        sendBroadcast(new Intent("com.pas.webcam.CONTROL").putExtra("action", "stop"));
        Intent polygondetect_intent = new Intent();
        polygondetect_intent.setClass(this, PolygonDetect.class);
        polygondetect_intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        this.startActivity(polygondetect_intent);
        break;
    }
 }
  
  public boolean onCreateOptionsMenu(Menu menu) {
    Log.i(TAG, "onCreateOptionsMenu");
    mItemColorTrack = menu.add("ColorTrack");
    mItemQRReader = menu.add("QR Read");
    mItemMonitor  = menu.add("Monitor");
    mItemObjFind  = menu.add("Object Find");
    mItemPolygonDetection = menu.add("Polygon detection");
    return true;
}
  

  public boolean onOptionsItemSelected(MenuItem item) {
      Log.i("Menu", "Menu Item selected " + item);
     
     if (item == mItemColorTrack) {
        setMode(COLOR_TRACK_MODE);
     }  else if (item == mItemQRReader) {
        setMode(QR_MODE);
     }  else if (item == mItemMonitor) {
        setMode(MONITOR_MODE);
     }  else if (item == mItemObjFind){
        setMode(OBJ_FIND_MODE);
     }  else if (item == mItemPolygonDetection){
        setMode(POLYGON_DETECT_MODE);
     }
      return true;
  } 
  
}