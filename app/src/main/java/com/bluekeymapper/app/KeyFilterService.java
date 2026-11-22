package com.bluekeymapper.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import java.util.HashMap;
import java.util.Map;

/** This service filters mapped hardware keys; it cannot inject global keyboard events. */
public final class KeyFilterService extends AccessibilityService {
  private final Map<String,Integer> active=new HashMap<>();
  private String learningHeld;
  @Override protected void onServiceConnected(){
    super.onServiceConnected();
    AccessibilityServiceInfo info=getServiceInfo();
    if(info!=null){info.flags|=AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;setServiceInfo(info);}
  }
  private String id(KeyEvent e){return e.getDeviceId()+":"+MainActivity.key(e);}
  @Override protected boolean onKeyEvent(KeyEvent e){
    if(e==null)return false;
    String id=id(e);int action=e.getAction();
    if(action==KeyEvent.ACTION_UP){
      Integer mapped=active.remove(id);
      if(mapped!=null){
        if(mapped>0)MapperImeService.send(mapped,e);
        return true;
      }
      if(id.equals(learningHeld)){learningHeld=null;return true;}
    }
    if(id.equals(learningHeld))return true;
    if(!MainActivity.matches(this,e.getDevice()))return false;
    if(MainActivity.learn(this,e)){learningHeld=id;return true;}
    if(!MainActivity.mappingOn(this))return false;
    Integer target=MainActivity.lookup(this,e);
    if(target==null)return false;
    if(action!=KeyEvent.ACTION_DOWN)return false;
    if(active.containsKey(id)){
      Integer old=active.get(id);
      if(old>0)MapperImeService.send(old,e);
      return true;
    }
    boolean accepted=target<0?systemAction(target):MapperImeService.send(target,e);
    if(accepted){active.put(id,target);return true;}
    return false;
  }
  private boolean systemAction(int target){
    if(target==MainActivity.BACK)return performGlobalAction(GLOBAL_ACTION_BACK);
    DisplayMetrics dm=getResources().getDisplayMetrics();
    float w=dm.widthPixels,h=dm.heightPixels;
    if(w<=0||h<=0)return false;
    float x=w*.5f,y=h*.5f,x2=x,y2=y;
    switch(target){
      case MainActivity.SCROLL_UP: y=h*.3f;y2=h*.75f;break;
      case MainActivity.SCROLL_DOWN: y=h*.75f;y2=h*.3f;break;
      case MainActivity.SCROLL_LEFT:x=w*.25f;x2=w*.75f;break;
      case MainActivity.SCROLL_RIGHT:x=w*.75f;x2=w*.25f;break;
      default:return false;
    }
    Path path=new Path();path.moveTo(x,y);path.lineTo(x2,y2);
    GestureDescription gesture=new GestureDescription.Builder()
      .addStroke(new GestureDescription.StrokeDescription(path,0,260)).build();
    return dispatchGesture(gesture,null,null);
  }
  private void cleanup(){
    for(Integer mapped:active.values())if(mapped>0)MapperImeService.release(mapped);
    active.clear();learningHeld=null;
  }
  @Override public void onAccessibilityEvent(AccessibilityEvent e){}
  @Override public void onInterrupt(){cleanup();}
  @Override public void onDestroy(){cleanup();super.onDestroy();}
}
