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
package org.wtfrobot.rosmin.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Control extends Activity implements OnTouchListener, RecognitionListener
{
  public static boolean bIfDebug = true;
  public static String TAG = "HIPPO_DEBUG";
  public static String strDebugPreFix = "Client:";
  public Handler handler = new Handler(){};
  public static String[] a ={"S","0","0","0"};
  /**  
   *   a[0]中S表示停止，F表示前进，B表示后退
   *   a[1]存的是陀螺仪角度，和小车初始方向角处理得到当前小车朝向
   *   a[2]存的是磁力角角度，确定小车初始方向角
   *   a[3]记录发现的物体，比如 "ball" 和QR码
   *   */
  
  //Socket Server
  private int intSocketServerPort = 8080;
  public static BufferedWriter bw;
  
  //Socket Client
  private static Socket mSocket02;
  
  //UI
 // private TextView servoRight,servoLeft,servoUp,servoDown;
  public TextView Value01,Value02;//用于舵机数值显示
  public static SeekBar servo1;
  public static SeekBar servo2;
  private Button connectButton;
  ImageButton dF,dB,dR,dL,dFR,dFL,dBR,dBL,p0;
  private EditText mEditText02;
  private String strTemp01="";
  public String ip;
  
  //draw route
//  private boolean first = true;

  RosminState appState;
 
  
  //PocketSphinx
  static {
    System.loadLibrary("pocketsphinx_jni");
  }
  /**
   * Recognizer task, which runs in a worker thread.
   */
  RecognizerTask rec;
  /**
   * Thread in which the recognizer task runs.
   */
  Thread rec_thread;
  /**
   * Time at which current recognition started.
   */
  Date start_date;
  /**
   * Number of seconds of speech.
   */
  float speech_dur;
  /**
   * Are we listening?
   */
  boolean listening;
  /**
   * Progress dialog for final recognition.
   */
  ProgressDialog rec_dialog;
  /**
   * Performance counter view.
   */
  static TextView performance_text;
  /**
   * Editable text view.
   */
  
  /**
   * Respond to touch events on the Speak button.
   * 
   * This allows the Speak button to function as a "push and hold" button, by
   * triggering the start of recognition when it is first pushed, and the end
   * of recognition when it is released.
   * 
   * @param v
   *            View on which this event is called
   * @param event
   *            Event that was triggered.
   */
  public boolean onTouch(View v, MotionEvent event) {
    switch (event.getAction()) {
    case MotionEvent.ACTION_DOWN:
      start_date = new Date();
      this.listening = true;
      this.rec.start();
      break;
    case MotionEvent.ACTION_UP:
      Date end_date = new Date();
      long nmsec = end_date.getTime() - start_date.getTime();
      this.speech_dur = (float)nmsec / 1000;
      if (this.listening) {
        Log.d(getClass().getName(), "Showing Dialog");
        this.rec_dialog = ProgressDialog.show(this, "", "Recognizing speech...", true);
        this.rec_dialog.setCancelable(false);
        this.listening = false;
      }
      this.rec.stop();
      break;
    default:
      ;
    }
    /* Let the button handle its own state */
    return false;
  }
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.main);
    
    connectButton = (Button)findViewById(R.id.myButton3);
    mEditText02 = (EditText)findViewById(R.id.myEditText2);
    mEditText02.setText("192.168.137.15");
    connectButton.setEnabled(true); 
    
    appState = ((RosminState)getApplicationContext());
         
    // Socket Client connect to Server
    connectButton.setOnClickListener(new Button.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        // TODO Auto-generated method stub
        try
        {
          ip = mEditText02.getText().toString();
          Thread thread = new Thread(new mSocketConnectRunnable2());
          thread.start();
//          thread.sleep(100);
          goToControl();           //进行控制命令的处理
          connectButton.setVisibility(View.INVISIBLE);
          mEditText02.setVisibility(View.INVISIBLE);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });  
  } 
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    /*
     * add()方法的四个参数，依次是：
     * 1、组别，如果不分组的话就写Menu.NONE,
     * 2、Id，这个很重要，Android根据这个Id来确定不同的菜单
     * 3、顺序，那个菜单现在在前面由这个参数的大小决定
     * 4、文本，菜单的显示文本
     */
    menu.add(Menu.NONE, Menu.FIRST + 1, 5,"绘制地图").setIcon(
        R.drawable.drawmap);
    // setIcon()方法为菜单设置图标，这里使用的是系统自带的图标，同学们留意一下,以
    // android.R开头的资源是系统提供的，我们自己提供的资源是以R开头的
    menu.add(Menu.NONE, Menu.FIRST + 2, 2, "手动控制").setIcon(
        R.drawable.control);
    // return true才会起作用
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case Menu.FIRST + 1:
      Toast.makeText(this, "查看地图", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent();
    intent.setClass(Control.this, Map.class);
    startActivity(intent);
      break;
    case Menu.FIRST + 2:
      Toast.makeText(this, "返回控制", Toast.LENGTH_SHORT).show();
      break;
    }
    return false;
  }
  
  //Socket Client
  public class mSocketConnectRunnable2 implements Runnable
  {         
    @Override
    public void run()
    {
      try
      {
     mSocket02 = new Socket(ip, intSocketServerPort);
     if(mSocket02.isConnected())
        {
          Log.i(TAG, "Socket Client is connected to Server.");
          strTemp01="Socket Client is connected to Server.";       
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(mSocket02.getInputStream()));
        while (true)
        {
         strTemp01 = br.readLine();
         if(!strTemp01.isEmpty())
           handler.post(rReceiveInfo);           
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
  
  private Runnable rReceiveInfo = new Runnable()
  {
    public void run()
    {
      a = splitString(strTemp01);
      Control.performance_text.setText(strTemp01);
    }
  }; 
  
  public void exitActivity(int exitMethod)
  {
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
 

  public void goToControl() {   
    dF = (ImageButton)findViewById(R.id.dF); 
    dFL = (ImageButton)findViewById(R.id.dFL);
    dFR = (ImageButton)findViewById(R.id.dFR);
    dR = (ImageButton)findViewById(R.id.dR);
    dL = (ImageButton)findViewById(R.id.dL);
    dBL = (ImageButton)findViewById(R.id.dBL);
    dBR = (ImageButton)findViewById(R.id.dBR);
    dB = (ImageButton)findViewById(R.id.dB);
    p0 = (ImageButton)findViewById(R.id.p0);
    
    Value01 = (TextView)findViewById(R.id.servoValue01);
    Value02 = (TextView)findViewById(R.id.servoValue02);
    final SeekBar servo1 = (SeekBar)findViewById(R.id.servo1); //左右
    final SeekBar servo2 = (SeekBar)findViewById(R.id.servo2); //上下
    
    //Voice Control
    this.rec = new RecognizerTask();
    this.rec_thread = new Thread(this.rec);
    this.listening = false;
    Button VoiceButton = (Button) findViewById(R.id.VoiceButton);
    VoiceButton.setOnTouchListener(this);
    performance_text = (TextView) findViewById(R.id.PerformanceText);
    this.rec.setRecognitionListener(this);
    this.rec_thread.start();
    
    servo1.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
      {
        Value01.setText("当前角度:" + (progress+10));
        action( String.valueOf(progress+10));
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar)
      {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar)
      { 
      }
      
    });
    
    servo2.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser)
      {
        Value02.setText("当前角度:" + (progress+10));  
        action(String.valueOf(progress+210));
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar)
      {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar)
      {
      }
      });
    
          dF.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("forward");
              //按下前进，松开停止
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              }); 
          dFR.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("turnRightOrigin");
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              }); 
          dFL.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("turnLeftOrigin");
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              });  
          dB.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("back");
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              }); 
          
          
          dBR.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("turnRight2");
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              });
          dBL.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("turnLeft2");
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              });
          
          dL.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("turnLeft");
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              });
          dR.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("turnRight");
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              });
          p0.setOnTouchListener(new OnTouchListener(){     
            @Override    
            public boolean onTouch(View v, MotionEvent event) {  
              // TODO Auto-generated method stub       
              if (event.getAction() == MotionEvent.ACTION_DOWN)
              {
              action("ok");       //舵机复位
              servo1.setProgress(80);
              servo2.setProgress(80);
              }
              else if(event.getAction() == MotionEvent.ACTION_UP)
              {
                action("stop");       
              }        
             return false;   
             }    
              });
          ///////servo
 
  }
  
  /** Called when partial results are generated. */
  public void onPartialResults(Bundle b) {
    final String hyp = b.getString("hyp");
    handler.post(new Runnable() {
      public void run() {
        Control.performance_text.setText(hyp);
      }
    });
  }

  /** Called with full results are generated. */
  public void onResults(Bundle b) {
    final String hyp = b.getString("hyp");
    //获得语音识别的结果后发送对应运动命令
    if (hyp.equals("FORWARD"))
      action("forward");
    else if (hyp.equals("TURN LEFT"))
      action("turnLeftOrigin");
    else if (hyp.equals("TURN RIGHT"))
      action("turnRightOrigin");
    else if (hyp.equals("BACKWARD"))
      action("back");
    else if (hyp.equals("STOP"))
      action("stop");
    else if (hyp.equals(" "))
      ;
    else action(hyp.toLowerCase());
    final Control that = this;
    handler.post(new Runnable() {
      public void run() {
        Date end_date = new Date();
        long nmsec = end_date.getTime() - that.start_date.getTime();
        float rec_dur = (float)nmsec / 1000;
        Control.performance_text.setText(String.format("%.2f seconds %.2f xRT",
                              that.speech_dur,
                              rec_dur / that.speech_dur));
        Log.d(getClass().getName(), "Hiding Dialog");
        that.rec_dialog.dismiss();
      }
    });
  }

  public void onError(int err) {
    
  }

  public static void action(String a){
    //当Socket连接正常且不为空时，流输出给server
    if(mSocket02!=null && mSocket02.isConnected() && !a.equals(""))
    { 
      try
      {             
        bw = new BufferedWriter(new OutputStreamWriter(mSocket02.getOutputStream()));          
        bw.write(a+"\n");
        bw.flush();         
      }
      catch (Exception e)
      {
          Log.e(TAG, e.toString());
          e.printStackTrace();
        }
    }
  }
  //以空格作为间隔分割字符串，并存成数组
  private String[] splitString(String s1)
  {
    String [] a1 = s1.split(" ");;
    return a1;
  }
    
  @Override
  protected void onPause()
  {
    super.onPause();
  }
  
}
