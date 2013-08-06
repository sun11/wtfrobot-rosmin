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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;

public class ArduinoControl
{
  private Activity mActivity;

  public static final byte MOTOR_COMMAND = 2;
  public static final byte IMAGE_COMMAND = 3;
  
  private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

  private UsbManager mUsbManager;
  private PendingIntent mPermissionIntent;
  private boolean mPermissionRequestPending;

  UsbAccessory mAccessory;
  ParcelFileDescriptor mFileDescriptor;
  FileOutputStream mOutputStream;
  FileInputStream mInputStream;

  public ArduinoControl(Activity activity) {
    mActivity = activity;
    mUsbManager = (UsbManager) activity
        .getSystemService(Context.USB_SERVICE);
    mPermissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(
        ACTION_USB_PERMISSION), 0);
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
    activity.registerReceiver(mUsbReceiver, filter);

    if (activity.getLastNonConfigurationInstance() != null) {
      mAccessory = (UsbAccessory) activity
          .getLastNonConfigurationInstance();
      openAccessoryInternal(mAccessory);
    }
  }

  public void onResume() {

    if (mOutputStream != null) {
      return;
    }
    openAccessory();
  }

  public void onPause() {
    closeAccessory();
  }

  public void onDestroy() {
    mActivity.unregisterReceiver(mUsbReceiver);
  }

  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
          if (intent.getBooleanExtra(
              UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            openAccessoryInternal(accessory);
          } else {
            mLoger.info("permission denied for accessory "
                + accessory);
          }
          mPermissionRequestPending = false;
        }
      } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
        UsbAccessory accessory = (UsbAccessory) intent
            .getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null && accessory.equals(mAccessory)) {
          closeAccessory();
        }
      }
      
    }
  };

  private void openAccessory() {
    UsbAccessory[] accessories = mUsbManager.getAccessoryList();
    UsbAccessory accessory = (accessories == null ? null : accessories[0]);
    if (accessory != null) {
      if (mUsbManager.hasPermission(accessory)) {
        openAccessoryInternal(accessory);
      } else {
        synchronized (mUsbReceiver) {
          if (!mPermissionRequestPending) {
            mUsbManager.requestPermission(accessory,
                mPermissionIntent);
            mPermissionRequestPending = true;
          }
        }
      }
    } else {
      mLoger.info("mAccessory is null");
    }
  }

  
  private void openAccessoryInternal(UsbAccessory accessory) {
    mFileDescriptor = mUsbManager.openAccessory(accessory);
    if (mFileDescriptor != null) {
      mAccessory = accessory;
      FileDescriptor fd = mFileDescriptor.getFileDescriptor();
      FileDescriptor fd2 = mFileDescriptor.getFileDescriptor();
      mOutputStream = new FileOutputStream(fd);
      mInputStream = new FileInputStream(fd2);
      mLoger.info("accessory opened");
    } else {
      mLoger.info("accessory open fail");
    }
  }

  private void closeAccessory() {

    try {
      if (mFileDescriptor != null) {
        mFileDescriptor.close();
        mOutputStream.close();
        mInputStream.close();
      }
    } catch (IOException e) {
    } finally {
      mFileDescriptor = null;
      mAccessory = null;
      mOutputStream = null;
      mInputStream = null;
    }
  }

  public void sendCommand(byte command, byte target, int valuex ,int valuey) {
    byte[] buffer = new byte[4];
    if (valuex > 255)
      valuex = 255;
    if(valuey > 255)
      valuey = 255;

    buffer[0] = command;
    buffer[1] = target;
    buffer[2] = (byte) valuex;
    buffer[3] = (byte) valuey;
    if (mOutputStream != null && buffer[1] != -1) {
      try {
        mOutputStream.write(buffer);
      } catch (IOException e) {
        mLoger.info("write failed" + e.getMessage());
      }
    }
  }
  public byte[] getCommand(){
	  byte[] buffer2 = new byte[4];
	  try {
		if(mInputStream!= null){
			mInputStream.read(buffer2);
		  }
	} catch (IOException e) {
		e.printStackTrace();
	}
	return buffer2;
  }

  private static Logger mLoger = Logger.getLogger(ArduinoControl.class
      .getName());

}
