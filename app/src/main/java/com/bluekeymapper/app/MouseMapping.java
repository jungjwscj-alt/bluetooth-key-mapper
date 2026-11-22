package com.bluekeymapper.app;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

/** Mouse-style mapping. Synthetic events never leave this app's own views. */
public final class MouseMapping {
  public static final int WHEEL_UP=-201, WHEEL_DOWN=-202;
  public static final int WHEEL_LEFT=-203, WHEEL_RIGHT=-204;
  public static final int BUTTON_BACK=-205, BUTTON_FORWARD=-206;
  private MouseMapping(){}
  public static boolean isWheel(int a) { return a>=WHEEL_UP&&a<=WHEEL_RIGHT; }
  public static boolean isMouse(int a) { return a>=WHEEL_UP&&a<=BUTTON_FORWARD; }
  public static int fallback(int a){
    switch(a){
      case WHEEL_UP:return MainActivity.SCROLL_UP;
      case WHEEL_DOWN:return MainActivity.SCROLL_DOWN;
      case WHEEL_LEFT:return MainActivity.SCROLL_LEFT;
      case WHEEL_RIGHT:return MainActivity.SCROLL_RIGHT;
      case BUTTON_BACK:return MainActivity.BACK;
      default:return a;
    }
  }
  public static MotionEvent wheelEvent(int wheel) {
    if(!isWheel(wheel))return null;
    MotionEvent.PointerProperties prop=new MotionEvent.PointerProperties();
    prop.id=0;prop.toolType=MotionEvent.TOOL_TYPE_MOUSE;
    MotionEvent.PointerCoords coords=new MotionEvent.PointerCoords();
    coords.x=1;coords.y=1;
    switch(wheel){
      case WHEEL_UP:coords.setAxisValue(MotionEvent.AXIS_VSCROLL,1);break;
      case WHEEL_DOWN:coords.setAxisValue(MotionEvent.AXIS_VSCROLL,-1);break;
      case WHEEL_LEFT:coords.setAxisValue(MotionEvent.AXIS_HSCROLL,-1);break;
      case WHEEL_RIGHT:coords.setAxisValue(MotionEvent.AXIS_HSCROLL,1);break;
    }
    long t=SystemClock.uptimeMillis();
    return MotionEvent.obtain(t,t,MotionEvent.ACTION_SCROLL,1,
       new MotionEvent.PointerProperties[]{prop},new MotionEvent.PointerCoords[]{coords},
       0,0,1f,1f,0,0,InputDevice.SOURCE_MOUSE,0);
  }
}
