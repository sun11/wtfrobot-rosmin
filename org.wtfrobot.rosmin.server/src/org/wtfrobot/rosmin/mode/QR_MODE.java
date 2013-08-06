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

import org.wtfrobot.rosmin.server.ArduinoControl;
import org.wtfrobot.rosmin.server.ControlPanel;

import android.app.Activity;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class QR_MODE extends Activity
{
  public Handler Handler01 = new Handler(){};
  boolean cornerFlag = false;
  boolean leftFlag = false;
  boolean firstForLeftFlag = true;
  boolean pauseFlag = false;
  boolean servoReadyOnceFlag = true;
  boolean receiveFlag = false;
  boolean finishFlag = false;
  public TextToSpeech mTTS;
  String contents = "";
  
  @Override
  public void onCreate(Bundle savedInstanceState)
  { 
    super.onCreate(savedInstanceState);
    
    OnInitListener ttsInitListener = null;
    mTTS = new TextToSpeech(this,ttsInitListener);
    
    ControlPanel.rotateFlag = false;
    cornerFlag = false;
    leftFlag = false;
    firstForLeftFlag = true;
    pauseFlag = false;
    servoReadyOnceFlag = true;
    receiveFlag = false;
    finishFlag = false;
    contents = "";
    ControlPanel.byteReceive[0] = 10;
    ControlPanel.byteReceive[1] = 10;
    ControlPanel.byteReceive[2] = 10;
    ControlPanel.byteReceive[3] = 10;
    
    Intent qr_intent = new Intent("com.google.zxing.client.android.SCAN");
    qr_intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    qr_intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
    this.startActivityForResult(qr_intent,0);
    
    
//    ControlPanel.tempGyr = ControlPanel.gyrAngle;
//    ControlPanel.mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte) 0xb, 1 , 1);        
//    try 
//    {
//      Thread.sleep(1000);
//    }
//    catch (InterruptedException e)
//    {
//      e.printStackTrace();
//    }
//    ControlPanel.gyrInit();
//    ControlPanel.gyrAngle = ControlPanel.tempGyr;    
    
   Thread tNavigation = new Thread(new Runnable(){
     public void run(){
       if (!finishFlag){
         try{
           ControlPanel.tempGyr = ControlPanel.gyrAngle;
           ControlPanel.mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte) 0xb, 1 , 1);        
           Thread.sleep(3000);
           ControlPanel.gyrInit();
           ControlPanel.gyrAngle = ControlPanel.tempGyr;  
           ControlPanel.strMoveFlag = "F";
           
           ControlPanel.mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte)0xc, 1,1);
           while(!finishFlag){
             Thread.sleep(300);
             // detect corner
             if (!cornerFlag)
             {
                Handler01.post(rDetectCorner);
             }
             
             if(cornerFlag && !ControlPanel.rotateFlag){
               if (!leftFlag)
                 ControlPanel.setRotateAngle(90);
               else {
                 ControlPanel.setRotateAngle(-90);
               }
               ControlPanel.strMoveFlag = "S";
               Handler01.post(rRotate);
             }
             
             if (cornerFlag && ControlPanel.rotateFlag ){
               Thread.sleep(7000);
               ControlPanel.strMoveFlag = "S";
               mTTS.speak(
                   "到达"+ControlPanel.strCommand,
                   TextToSpeech.QUEUE_FLUSH,null
                   );
               ControlPanel.strCommand = "";
               finishFlag = true;
               finish();
             }
               
           }
           
         }catch(Throwable t){
         }
       }
     }
    });
    tNavigation.start();
    
  }
  
  public Runnable rDetectCorner = new Runnable(){
    public void run()
    { 
        if (ControlPanel.byteReceive[3] < 80){
//          if (leftFlag && firstForLeftFlag){
//            ControlPanel.mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte)0x1, 1,1);
//            firstForLeftFlag = false;
//          }
          //servo is ready
          if (servoReadyOnceFlag && ControlPanel.byteReceive[0] == 3 && ControlPanel.byteReceive[1] == 3 ){
            ControlPanel.gyrAngle = ControlPanel.tempGyr;
            ControlPanel.strMoveFlag = "F";
            servoReadyOnceFlag = false;
          }
          
          if ( qrProcess(contents,ControlPanel.strCommand).equals("左转") )
          {
            ControlPanel.mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x1, 1,1);
            ControlPanel.strMoveFlag = "S";
            ControlPanel.strQR = "左转";  
            ControlPanel.tempGyr = ControlPanel.gyrAngle;
           pauseFlag = false;
           contents = "";
           receiveFlag = true;
          }
          else if ( qrProcess(contents,ControlPanel.strCommand).equals("右转") )
          {
            ControlPanel.mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x2, 1,1);
            ControlPanel.strQR = "右转";
            contents = "";
            pauseFlag = false;
            receiveFlag = true;
          }
          
          if(!String.valueOf(((int) ControlPanel.byteReceive[3]) ).equals(ControlPanel.tempStr))
            mTTS.speak(
            String.valueOf(((int) ControlPanel.byteReceive[3]) ),
            TextToSpeech.QUEUE_FLUSH,null
            );
        ControlPanel.tempStr = String.valueOf(((int) ControlPanel.byteReceive[3]) );
          
          
//          else 
//            ControlPanel.mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x1, 200,0);
        }
        if (ControlPanel.byteReceive[3] >= 80){
          ControlPanel.strMoveFlag = "F";
//          ControlPanel.gyrInit();
//          ControlPanel.mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
//          ControlPanel.mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x1, 200,0);
          ControlPanel.mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte) 0xd, 2,0);
          cornerFlag = true;
//          try
//          {
//            Thread.sleep(1000);
//          }
//          catch (InterruptedException e)
//          {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//          };
        
      if(!String.valueOf(((int) ControlPanel.byteReceive[3]) ).equals(ControlPanel.tempStr))
          mTTS.speak(
          String.valueOf(((int) ControlPanel.byteReceive[3]) ),
          TextToSpeech.QUEUE_FLUSH,null
          );
      ControlPanel.tempStr = String.valueOf(((int) ControlPanel.byteReceive[3]) );
        }
//        if (!cornerFlag && receiveFlag)
//        ControlPanel.byteReceive = ControlPanel.mArduinoControl.getCommand();
//      else 
//        ControlPanel.mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte) 0x0, 1,0);
      
    }
  };
  
  public  Runnable rRotate = new Runnable(){
    public void run()
    {
      if( ControlPanel.init_gyr - ControlPanel.gyrAngle < (ControlPanel.rotateAngle - 2))
      {            
        ControlPanel.mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte)0x8, 200,0);
      }
      if ( ControlPanel.init_gyr - ControlPanel.gyrAngle> (ControlPanel.rotateAngle + 2))
      {
        ControlPanel.mArduinoControl.sendCommand(ArduinoControl.MOTOR_COMMAND, (byte)0x7, 200,0);
      }
      if(ControlPanel.init_gyr -ControlPanel.gyrAngle <= (ControlPanel.rotateAngle + 2)  &&  ControlPanel.init_gyr - ControlPanel.gyrAngle>= (ControlPanel.rotateAngle - 2))
      {
        ControlPanel.rotateFlag = true;
        ControlPanel.strMoveFlag = "F";
        ControlPanel.mArduinoControl.sendCommand(ArduinoControl.IMAGE_COMMAND, (byte)0xd, 7, 0);
        
      }
      
//      if(!String.valueOf(((int) ControlPanel.byteReceive[3]) ).equals(ControlPanel.tempStr))
//        ControlPanel.mTTS.speak(
//          String.valueOf(((int) ControlPanel.byteReceive[3]) ),
//          TextToSpeech.QUEUE_FLUSH,null
//          );
//      ControlPanel.tempStr = String.valueOf(((int) ControlPanel.byteReceive[3]) );
    }
  };
  
  public String qrProcess(String strContent,String strCommand){
    String strStep1 [];
    strStep1 = strContent.split(" ");
    String strResult = "";
    if (!strContent.equals("") && strCommand.equals("一区")){
      if (strStep1[0].contains("一区")){
        if (strStep1[0].contains("左"))
          strResult = "左转";
        else if (strStep1[0].contains("右"))
          strResult = "右转";
      }
      if (strStep1[1].contains("一区")){
        if (strStep1[1].contains("左"))
          strResult = "左转";
        else if (strStep1[1].contains("右"))
          strResult = "右转";
      }
    }
    
    else if (!strContent.equals("") && strCommand.equals("二区")){
      if (strStep1[0].contains("二区")){
        if (strStep1[0].contains("左"))
          strResult = "左转";
        else if (strStep1[0].contains("右"))
          strResult = "右转";
      }
      if (strStep1[1].contains("二区")){
        if (strStep1[1].contains("左"))
          strResult = "左转";
        else if (strStep1[1].contains("右"))
          strResult = "右转";
      }
    }
    return strResult;
  }
  

  @Override
  public void onDestroy(){
      super.onDestroy();
}  
  
  @Override
  public void onResume(){
      super.onResume();
      ControlPanel.mArduinoControl.onResume();
  }
  
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == 0) {
        if (resultCode == RESULT_OK) {
           contents = intent.getStringExtra("SCAN_RESULT");
           if (qrProcess(contents,ControlPanel.strCommand).equals("左转") ){
             leftFlag = true;
           }
           pauseFlag = true;
           ControlPanel.init_gyr = ControlPanel.gyrAngle;
           mTTS.speak(
               String.valueOf(contents),
               TextToSpeech.QUEUE_FLUSH,null
               );           
           
            // Handle successful scan
        } else if (resultCode == RESULT_CANCELED) {
            // Handle cancel
        }
    }
    
  }
}
