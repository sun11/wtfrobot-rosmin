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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Map extends MapActivity implements RecognitionListener {
  private MapView mapView;
  public double lat1,lng1;
  public long moveTimeGap=0;
  //Route
  private String[] m={"Home","BallTrack","ObjectFind","PolygonDetect","Monitor","QR","Section one","Section two"};
  private Spinner spinner;
  private ArrayAdapter<String> adapter;
  private Drawable originMaker = null,currMaker = null,ballMaker = null;
  private MapOverlay myOverlay = null;
  public int gyroAngle=0,oriAngle=0;
  public Button voice,hide;
  public String QRmsg;
  private boolean isHide=false,getQR=false,findBALL=false;
  ImageButton dF2,dB2,dFR2,dFL2,p1;
  RosminState appState;
  public boolean move = false,ori = true;
  List<GeoPoint> points = new ArrayList<GeoPoint>();
  MapController mapcontroller;
  public Handler Handler01 = new Handler(){};

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
  TextView performance_text;
  /**
   * Editable text view.
   */

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       requestWindowFeature(Window.FEATURE_NO_TITLE);
       setContentView(R.layout.drawmap);
       //spinner
       spinner = (Spinner)findViewById(R.id.spinner1);
       adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,m);
       adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
       spinner.setAdapter(adapter);
       spinner.setOnItemSelectedListener(new OnItemSelectedListener()
       {
         @Override
         public void onItemSelected
          (AdapterView<?> arg0, View arg1, int arg2,long arg3)
         {
           /* 将Spinner显示 */
           arg0.setVisibility(View.VISIBLE);
           switch(arg2){
             case 0:{
//               Control.action("switch home");
               break;}
             case 1:{
               Control.action("ball track");
               break;}
             case 2:{
               Control.action("object find");
               break;}
             case 3:{
               Control.action("polygon detect");
               break;}
             case 4:{
               Control.action("monitor mode");
               break;}
             case 5:{
               Control.action("qr mode");
               break;}
             case 6:{
               Control.action("go to section one");
               break;
             }
             case 7:{
               Control.action("go to section two");
             }
           }
         }
         @Override
         public void onNothingSelected(AdapterView<?> arg0)
         {
           // TODO Auto-generated method stub
         }
       });
       spinner.setVisibility(View.VISIBLE);

       dF2 = (ImageButton)findViewById(R.id.dF2);
       dFL2 = (ImageButton)findViewById(R.id.dFL2);
       dFR2= (ImageButton)findViewById(R.id.dFR2);
       dB2 = (ImageButton)findViewById(R.id.dB2);
       p1 = (ImageButton)findViewById(R.id.p1);
         //Voice Control
       this.rec = new RecognizerTask();
       this.rec_thread = new Thread(this.rec);
       this.listening = false;
       //语音识别
       voice = (Button)findViewById(R.id.voice);
       voice.setOnTouchListener(new OnTouchListener(){
         @Override
         public boolean onTouch(View v, MotionEvent event) {
           if (event.getAction() == MotionEvent.ACTION_DOWN)
           {
             start_date = new Date();
             listening = true;
             rec.start();
           }
           else if(event.getAction() == MotionEvent.ACTION_UP)
           {
             Date end_date = new Date();
             long nmsec = end_date.getTime() - start_date.getTime();
             speech_dur = (float)nmsec / 1000;
             if (listening) {
               Log.d(getClass().getName(), "Showing Dialog");
               rec_dialog = ProgressDialog.show(Map.this, "", "Recognizing speech...", true);
               rec_dialog.setCancelable(false);
               listening = false;
             }
             rec.stop();
           }
          return false;
          }
           });       
       performance_text = (TextView) findViewById(R.id.VoiceText);
       rec.setRecognitionListener(this);
       rec_thread.start();

       hide = (Button)findViewById(R.id.hide);
       appState = ((RosminState)getApplicationContext());
       mapView =(MapView)findViewById(R.id.map);
       mapcontroller = mapView.getController();
       originMaker = getResources().getDrawable(R.drawable.origin);//起点图标
       currMaker = getResources().getDrawable(R.drawable.sign);      //当前位置图标
       ballMaker = getResources().getDrawable(R.drawable.goal);       //发现小球图标

       lat1 = 0;
       lng1 = 0;
       //存储连接的点的信息
//       final List<GeoPoint> points = new ArrayList<GeoPoint>();
//       points.add(new GeoPoint(0,0));
       //画出起点，小车路线原理就是画出两点并连线，周而复始
       points.add(new GeoPoint((int) (lat1 * 1000000),(int) (lng1 * 1000000)));
       
       //添加自定义的图层
       PathOverLay polyline = new PathOverLay(points);  //添加polyline图层，负责画出路线
       mapView.getOverlays().add(polyline);
       mapView.invalidate();
       mapView.setBuiltInZoomControls(true);
       //设置中心和放大倍数
       GeoPoint  point=new GeoPoint((int) (lat1 * 1000000),(int) (lng1 * 1000000));
       mapcontroller.setCenter(point);
       mapcontroller.setZoom(15);
       //这个图层负责添加图标，这里添加“起点”图标
       myOverlay = new MapOverlay(originMaker);
       myOverlay.setItem(point);
       myOverlay.setQR("起点");
       mapView.getOverlays().add(myOverlay);
       mapcontroller.animateTo(point);
       
       //用于按键的隐藏和显示
       hide.setOnClickListener(new Button.OnClickListener()
       {
         @Override
         public void onClick(View v)
         {
           if(!isHide){
           dF2.setVisibility(View.INVISIBLE);
           dB2.setVisibility(View.INVISIBLE);
           dFR2.setVisibility(View.INVISIBLE);
           dFL2.setVisibility(View.INVISIBLE);
           p1.setVisibility(View.INVISIBLE);
           voice.setVisibility(View.INVISIBLE);
           hide.setText("显示");
           isHide = true;
           }
           else{
           dF2.setVisibility(View.VISIBLE);
           dB2.setVisibility(View.VISIBLE);
           dFR2.setVisibility(View.VISIBLE);
           dFL2.setVisibility(View.VISIBLE);
           p1.setVisibility(View.VISIBLE);
           voice.setVisibility(View.VISIBLE);
           hide.setText("隐藏");
           isHide = false;
           }
         }
       });
       dF2.setOnTouchListener(new OnTouchListener(){
         @Override
         public boolean onTouch(View v, MotionEvent event) {
           // TODO Auto-generated method stub
           if (event.getAction() == MotionEvent.ACTION_DOWN)
           {
             Control.action("forward");
           }
           else if(event.getAction() == MotionEvent.ACTION_UP)
           {
             Control.action("stop");
           }
          return false;
          }
           });
       dFR2.setOnTouchListener(new OnTouchListener(){
         @Override
         public boolean onTouch(View v, MotionEvent event) {
           // TODO Auto-generated method stub
           if (event.getAction() == MotionEvent.ACTION_DOWN)
           {
             Control.action("turnRightOrigin");
           }
           else if(event.getAction() == MotionEvent.ACTION_UP)
           {
            Control.action("stop");
           }
          return false;
          }
           });
       dFL2.setOnTouchListener(new OnTouchListener(){
         @Override
         public boolean onTouch(View v, MotionEvent event) {
           // TODO Auto-generated method stub
           if (event.getAction() == MotionEvent.ACTION_DOWN)
           {
             Control.action("turnLeftOrigin");
           }
           else if(event.getAction() == MotionEvent.ACTION_UP)
           {
            Control.action("stop");
           }
          return false;
          }
           });
       dB2.setOnTouchListener(new OnTouchListener(){
         @Override
         public boolean onTouch(View v, MotionEvent event) {
           // TODO Auto-generated method stub
           if (event.getAction() == MotionEvent.ACTION_DOWN)
           {
             Control.action("back");
           }
           else if(event.getAction() == MotionEvent.ACTION_UP)
           {
            Control.action("stop");
           }
          return false;
          }
           });

       p1.setOnTouchListener(new OnTouchListener(){
         @Override
         public boolean onTouch(View v, MotionEvent event) {
           // TODO Auto-generated method stub
           if (event.getAction() == MotionEvent.ACTION_DOWN)
           {
             Control.action("ok");
           }
           else if(event.getAction() == MotionEvent.ACTION_UP)
           {
            Control.action("stop");
           }
          return false;
          }
           });

       //在地图上画出路线以及做出标记
     Thread time = new Thread(new Runnable()
       {
       public void run() {
         long   t1 = 0,t2 = 0 ;
                while(true){
                  while(!Control.a[0].equals("F") && !Control.a[0].equals("B"))//未获得行动命令就等待
                  {
                    if(!Control.a[3].equals("0") && !Control.a[3].equals("ball"))
                    {
                       break;
                    }                    
                    if(Control.a[3].equals("ball"))
                    {
                      break;              
                    }
                  }
                  
                  t1 = System.currentTimeMillis();
                    if(!Control.a[3].equals("0") && !Control.a[3].equals("ball"))
                      //发现QR码并保存，在地图上标记
                    {
                        QRmsg = Control.a[3];
                        getQR =true;
                        addMarker();        
                    }
                    if(Control.a[3].equals("ball"))  
                      //发现小球，在地图上标记
                    {
                      findBALL=true;
                      addMarker();
                    }
                  try
                 {
                   Thread.sleep(50);
                 }
                 catch (InterruptedException e)
                 {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
                 }
                  t2 = System.currentTimeMillis();
                  moveTimeGap = t2 -t1 ;
                  Handler01.post(rDraw);
                  }
             }//run
       });
    time.start();
   }  //oncreate

   /** Called when partial results are generated. */
   //下面是语音识别对识别结果进行处理的部分
   public void onPartialResults(Bundle b) {
     final String hyp = b.getString("hyp");
     Handler01.post(new Runnable() {
       public void run() {
         performance_text.setText(hyp);
       }
     });
   }

   /** Called with full results are generated. */
   public void onResults(Bundle b) {
     final String hyp = b.getString("hyp");
     if (hyp.equals("FORWARD"))
       Control.action("forward");
     else if (hyp.equals("TURN LEFT"))
       Control.action("turnLeftOrigin");
     else if (hyp.equals("TURN RIGHT"))
       Control.action("turnRightOrigin");
     else if (hyp.equals("BACKWARD"))
       Control.action("back");
     else if (hyp.equals("STOP"))
       Control.action("stop");
     else if (hyp.equals(""))
       ;
     else Control.action(hyp.toLowerCase());
     final Map that = this;
     Handler01.post(new Runnable() {
       public void run() {
         Date end_date = new Date();
         long nmsec = end_date.getTime() - that.start_date.getTime();
         float rec_dur = (float)nmsec / 1000;
         performance_text.setText(String.format("%.2f seconds %.2f xRT",
                               that.speech_dur,
                               rec_dur / that.speech_dur));
         Log.d(getClass().getName(), "Hiding Dialog");
         that.rec_dialog.dismiss();
       }
     });
   }

   @Override
   public void onError(int err)
   {

   }

/**画图部分，这里使用经纬度来表示小车朝向与行进路线
 *   最初用磁力计得到方向角，然后与陀螺仪得到的角度进行处理得到当前小车朝向
 * */
   private Runnable rDraw = new Runnable()
   {
     public void run()
     {
       if(ori)
       {
       oriAngle = Integer.parseInt(Control.a[2]);
       ori = false;
       }
       gyroAngle = Integer.parseInt( Control.a[1] );
       if(Control.a[0].equals("F"))
       {
       lat1 += (moveTimeGap*0.000001)*Math.cos((oriAngle-gyroAngle)*0.017452);
       lng1 += (moveTimeGap*0.000001)*Math.sin((oriAngle-gyroAngle)*0.017452);
       points.add(new GeoPoint((int) (lat1 * 1000000),(int) (lng1 * 1000000)));
       }
       if(Control.a[0].equals("B"))
       {
         lat1 -= (moveTimeGap*0.000001)*Math.cos((oriAngle-gyroAngle)*0.017452);
         lng1 -= (moveTimeGap*0.000001)*Math.sin((oriAngle-gyroAngle)*0.017452);
         points.add(new GeoPoint((int) (lat1 * 1000000),(int) (lng1 * 1000000)));
       }
     }
   };

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
     /*
      * add()方法的四个参数，依次是：
      * 1、组别，如果不分组的话就写Menu.NONE,
      * 2、Id，这个很重要，Android根据这个Id来确定不同的菜单
      * 3、顺序，那个菜单现在在前面由这个参数的大小决定
      * 4、文本，菜单的显示文本
      */
     menu.add(Menu.NONE, Menu.FIRST + 1, 5,"绘制地图").setIcon(R.drawable.drawmap);
     // setIcon()方法为菜单设置图标，这里使用的是系统自带的图标，同学们留意一下,以
     // android.R开头的资源是系统提供的，我们自己提供的资源是以R开头的
     menu.add(Menu.NONE, Menu.FIRST + 2, 2, "手动控制").setIcon(R.drawable.control);
     // return true才会起作用
     return true;
   }


   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
     switch (item.getItemId()) {
     case Menu.FIRST + 1:{
       Toast.makeText(this, "查看地图", Toast.LENGTH_SHORT).show();
       break;}
     case Menu.FIRST + 2:{
       Toast.makeText(this, "返回控制", Toast.LENGTH_SHORT).show();
       Intent intent = new Intent();
       intent.setClass(Map.this, Control.class);
       startActivity(intent);
       finish();
       break;}
     }
     return false;
   }

   /**
    * 是否显示路线显示
    */
   @Override
   protected boolean isRouteDisplayed() {
       return true;
   }

   /*
    * Add marker to my current location
    */
   private void addMarker() {
       if(getQR)  //发现QR码后，存储坐标，显示图标，记录QR信息
     {
               GeoPoint myGeoPoint = new GeoPoint((int) (lat1 * 1000000),(int) (lng1 * 1000000));
               myOverlay = new MapOverlay(currMaker);
               myOverlay.setItem(myGeoPoint);
               myOverlay.setQR(QRmsg);
               mapView.getOverlays().add(myOverlay);
               getQR = false;
    }
      if(findBALL)  //发现球后，存储坐标，显示图标，记录球信息
     {
               GeoPoint myGeoPoint = new GeoPoint((int) (lat1 * 1000000),(int) (lng1 * 1000000));
               myOverlay = new MapOverlay(ballMaker);
               myOverlay.setItem(myGeoPoint);
               myOverlay.setQR("发现小球");
               mapView.getOverlays().add(myOverlay);
               findBALL = false;
    }
   }
   public class MapOverlay extends ItemizedOverlay<OverlayItem> {
     private List<GeoPoint> mItems = new ArrayList<GeoPoint>();
     public List<String> QRmessage = new ArrayList<String>();

     public MapOverlay(Drawable marker) {
         super(boundCenterBottom(marker));
     }

     public void setItems(ArrayList<GeoPoint> items) {
         mItems = items;
         populate();
     }

     public void setItem(GeoPoint item) {
         mItems.add(item);
         populate();
     }

     public void setQR(String s) {
       QRmessage.add(s);
       populate();
   }


     @Override
     protected OverlayItem createItem(int i) {
         return new OverlayItem(mItems.get(i), null, null);
     }

     @Override
     public int size() {
         return mItems.size();
     }

     @Override
     protected boolean onTap(int i) {
        Toast.makeText(Map.this, "获得信息\n "+ QRmessage, Toast.LENGTH_SHORT).show();
         return true;
     }

 }
}