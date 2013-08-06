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

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class PathOverLay extends Overlay {
    List<GeoPoint> points;  
    Paint paint;  
  
    /** 
     * 构造函数，使用GeoPoint List构造Polyline 
     *  
     * @param points 
     *            GeoPoint点List 
     */  
    public PathOverLay(List<GeoPoint> points) {  
        this.points = points;  
        paint = new Paint();  
        paint.setColor(Color.rgb(59,88,235));
        paint.setAlpha(150);  
        paint.setAntiAlias(true);  
        paint.setStyle(Paint.Style.FILL_AND_STROKE);  
        paint.setStrokeWidth(4);  
    }  
  
    /** 
     * 使用GeoPoint点List和Paint对象来构造Polyline 
     *  
     * @param points 
     *            GeoPoint点List，所有的拐点 
     * @param paint 
     *            Paint对象，用来控制划线样式 
     */  
    public PathOverLay(List<GeoPoint> points, Paint paint) {  
        this.points = points;  
        this.paint = paint;  
    }  
  
  
    /** 
     * 真正将线绘制出来 只需将线绘制到canvas上即可，主要是要转换经纬度到屏幕坐标 
     */  
    @Override  
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {  
      int i;
        if (!shadow) {// 不是绘制shadow层  
            Projection projection = mapView.getProjection();  
            if (points != null) {  
                if (points.size() >= 2) {  
                    Point start = projection.toPixels(points.get(0), null);// 需要转换坐标  
                    for ( i = 1; i < points.size(); i++) {  
                        Point end = projection.toPixels(points.get(i), null);                       
                        canvas.drawLine(start.x, start.y, end.x, end.y, paint);// 绘制到canvas上即可  
                        start = end;
                        if(i == points.size()-1)
                        {
                          paint.setColor(Color.rgb(250,20,85));
                          end = projection.toPixels(points.get(i), null);                       
//                          canvas.drawLine(start.x, start.y, end.x, end.y, paint);
                          canvas.drawCircle(end.x, end.y, 6, paint);
                          paint.setColor(Color.rgb(59,88,235));
                        }
                    }  

                }  
            }  
        }  
    }  
}  