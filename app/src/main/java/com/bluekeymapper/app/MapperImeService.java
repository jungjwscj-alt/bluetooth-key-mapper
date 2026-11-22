package com.bluekeymapper.app;

import android.inputmethodservice.InputMethodService;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.os.SystemClock;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.view.InputDevice;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** IME sends mapped keyboard events only to the current focused editor. */
public final class MapperImeService extends InputMethodService {
  private static volatile MapperImeService instance;
  @Override public void onCreate(){super.onCreate();instance=this;}
  @Override public void onDestroy(){if(instance==this)instance=null;super.onDestroy();}
  private boolean selected(){
    String current=Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
    return current!=null&&new ComponentName(this,MapperImeService.class).equals(ComponentName.unflattenFromString(current));
  }
  static boolean send(int code,KeyEvent original){
    MapperImeService ime=instance;
    if(ime==null||!ime.selected())return false;
    InputConnection c=ime.getCurrentInputConnection();
    if(c==null)return false;
    try{
      KeyEvent key=new KeyEvent(original.getDownTime(),original.getEventTime(),original.getAction(),
        code,original.getRepeatCount(),original.getMetaState(),KeyCharacterMap.VIRTUAL_KEYBOARD,
        0,KeyEvent.FLAG_SOFT_KEYBOARD,InputDevice.SOURCE_KEYBOARD);
      return c.sendKeyEvent(key);
    }catch(RuntimeException ex){return false;}
  }
  static void release(int code){
    MapperImeService ime=instance;
    if(ime==null||!ime.selected())return;
    InputConnection c=ime.getCurrentInputConnection();
    if(c==null)return;
    try{c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}catch(RuntimeException ignored){}
  }
  @Override public View onCreateInputView(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.rgb(228,234,250));root.setPadding(3,6,3,6);
    TextView note=new TextView(this);note.setText("블루투스 키 맵퍼 · 간이 영문 입력기");
    note.setGravity(Gravity.CENTER);root.addView(note);
    addRow(root,"qwertyuiop");
    addRow(root,"asdfghjkl");
    addRow(root,"zxcvbnm");
    addButtons(root,new String[]{"←","↑","↓","→","⌫"});
    addButtons(root,new String[]{"Space","Enter","Tab","🌐"});
    return root;
  }
  private void addRow(LinearLayout root,String letters){
    String[] arr=new String[letters.length()];
    for(int i=0;i<letters.length();i++)arr[i]=String.valueOf(letters.charAt(i));
    addButtons(root,arr);
  }
  private void addButtons(LinearLayout root,String[] labels){
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
    for(String label:labels){
      Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setAllCaps(false);
      b.setPadding(0,0,0,0);
      b.setOnClickListener(v->type(label));
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,48*(int)getResources().getDisplayMetrics().density,1);
      row.addView(b,p);
    }
    root.addView(row);
  }
  private void type(String s){
    if("🌐".equals(s)){
      InputMethodManager im=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
      if(im!=null)im.showInputMethodPicker();return;
    }
    InputConnection c=getCurrentInputConnection();if(c==null)return;
    int code=0;
    switch(s){
      case "←":code=KeyEvent.KEYCODE_DPAD_LEFT;break;
      case "→":code=KeyEvent.KEYCODE_DPAD_RIGHT;break;
      case "↑":code=KeyEvent.KEYCODE_DPAD_UP;break;
      case "↓":code=KeyEvent.KEYCODE_DPAD_DOWN;break;
      case "⌫":code=KeyEvent.KEYCODE_DEL;break;
      case "Enter":code=KeyEvent.KEYCODE_ENTER;break;
      case "Tab":code=KeyEvent.KEYCODE_TAB;break;
      case "Space":c.commitText(" ",1);return;
      default:c.commitText(s,1);return;
    }
    c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));
    c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));
  }
}
