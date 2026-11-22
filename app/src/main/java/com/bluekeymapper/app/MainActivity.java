package com.bluekeymapper.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Switch;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

public final class MainActivity extends Activity {
  static final int SCROLL_UP=-101, SCROLL_DOWN=-102, SCROLL_LEFT=-103, SCROLL_RIGHT=-104, BACK=-105;
  static final int[] TARGETS={
    KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_SPACE,KeyEvent.KEYCODE_ESCAPE,KeyEvent.KEYCODE_TAB,
    KeyEvent.KEYCODE_DEL,KeyEvent.KEYCODE_FORWARD_DEL,KeyEvent.KEYCODE_PAGE_UP,KeyEvent.KEYCODE_PAGE_DOWN,
    KeyEvent.KEYCODE_MOVE_HOME,KeyEvent.KEYCODE_MOVE_END,KeyEvent.KEYCODE_F1,KeyEvent.KEYCODE_F2,
    KeyEvent.KEYCODE_F3,KeyEvent.KEYCODE_F4,SCROLL_UP,SCROLL_DOWN,SCROLL_LEFT,SCROLL_RIGHT,BACK,KeyEvent.KEYCODE_FORWARD
  };
  static final String[] NAMES={"↑ 방향키","↓ 방향키","← 방향키","→ 방향키","Enter","Space","Esc","Tab",
    "Backspace","Delete","Page Up","Page Down","Home(커서)","End(커서)","F1","F2","F3","F4",
    "스크롤 위","스크롤 아래","스크롤 왼쪽","스크롤 오른쪽","Android 뒤로가기","앞으로가기(지원 앱 한정)"};
  private LinearLayout root, rows;
  private TextView status;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private long captureSeen;
  private boolean active;
  private final Runnable refresh=new Runnable(){ public void run(){
    if(!active)return;
    long n=prefs(MainActivity.this).getLong("seq",0);
    if(n!=captureSeen){
      captureSeen=n;
      String source=prefs(MainActivity.this).getString("captured","");
      if(!source.isEmpty())showTargetPicker(source);
    }
    handler.postDelayed(this,300);
  }};

  static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences("mapper",Context.MODE_PRIVATE);}
  static String descriptor(Context c){return prefs(c).getString("device","");}
  static String key(KeyEvent e){return e.getScanCode()!=0?"s"+e.getScanCode()+":k"+e.getKeyCode():"k"+e.getKeyCode();}
  static boolean matches(Context c,InputDevice dev){return dev!=null&&!descriptor(c).isEmpty()&&descriptor(c).equals(dev.getDescriptor());}
  static boolean mappingOn(Context c){return prefs(c).getBoolean("enabled",false);}
  static Integer lookup(Context c,KeyEvent e){
    if(!mappingOn(c)||!matches(c,e.getDevice()))return null;
    String name="map."+key(e);
    SharedPreferences p=prefs(c);
    return p.contains(name)?p.getInt(name,0):null;
  }
  static boolean learn(Context c,KeyEvent e){
    SharedPreferences p=prefs(c);
    if(!p.getBoolean("learning",false)||!matches(c,e.getDevice())||e.getAction()!=KeyEvent.ACTION_DOWN||e.getRepeatCount()!=0)return false;
    p.edit().putBoolean("learning",false).putString("captured",key(e)).putLong("seq",p.getLong("seq",0)+1).commit();
    return true;
  }
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    captureSeen=prefs(this).getLong("seq",0);
    screen();
  }
  @Override protected void onResume(){super.onResume();active=true;update();handler.post(refresh);}
  @Override protected void onPause(){active=false;handler.removeCallbacks(refresh);super.onPause();}
  @Override public boolean dispatchKeyEvent(KeyEvent e){
    if(learn(this,e)){return true;}
    return super.dispatchKeyEvent(e);
  }
  private Button button(String text,Runnable task){
    Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->task.run());
    root.addView(b);return b;
  }
  private TextView label(String text){
    TextView t=new TextView(this);t.setText(text);t.setTextColor(Color.rgb(30,45,70));t.setTextSize(16);
    t.setPadding(8,10,8,10);root.addView(t);return t;
  }
  private void screen(){
    ScrollView scroll=new ScrollView(this);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,18,20,24);
    root.setBackgroundColor(Color.rgb(245,248,255));scroll.addView(root);setContentView(scroll);
    TextView title=label("블루투스 키 맵퍼  v1.1");title.setTextSize(26);title.setTextColor(Color.rgb(35,78,205));
    label("연결된 Bluetooth HID 입력장치를 Android에서 먼저 페어링한 뒤 선택하세요.");
    status=label("");
    button("① 연결된 입력장치 선택",this::chooseDevice);
    button("② 버튼 학습하고 매핑 추가",()->{
      if(descriptor(this).isEmpty()){toast("먼저 기기를 선택하세요");return;}
      prefs(this).edit().putBoolean("learning",true).apply();toast("선택한 장치의 버튼을 누르세요");
    });
    Switch sw=new Switch(this);sw.setText("키 매핑 활성화");
    sw.setChecked(mappingOn(this));sw.setTextSize(17);
    sw.setOnCheckedChangeListener((v,checked)->prefs(this).edit().putBoolean("enabled",checked).apply());
    root.addView(sw);
    button("③ 접근성 서비스 활성화",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    button("④ 입력기 활성화",()->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
    button("⑤ 기본 입력기 선택",()->{
      InputMethodManager im=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
      if(im!=null)im.showInputMethodPicker();
    });
    label("매핑 목록");
    rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);root.addView(rows);
    label("테스트 입력란 (IME 키 출력 확인)");
    EditText test=new EditText(this);test.setHint("이곳을 눌러 방향키, Enter, Space 테스트");
    root.addView(test);
    label("제약: 방향키와 Enter 등은 현재 연결된 입력 대상에만 전달됩니다. 앱 전체에 실제 하드웨어 키를 주입하지 않습니다. 스크롤/뒤로는 접근성 제스처를 사용합니다.");
    update();
  }
  private void chooseDevice(){
    ArrayList<InputDevice> devices=new ArrayList<>();
    ArrayList<String> labels=new ArrayList<>();
    for(int id:InputDevice.getDeviceIds()){
      InputDevice d=InputDevice.getDevice(id);
      if(d==null||d.isVirtual()||(!d.isExternal()&&(d.getSources()&InputDevice.SOURCE_GAMEPAD)!=InputDevice.SOURCE_GAMEPAD))continue;
      if((d.getSources()&InputDevice.SOURCE_KEYBOARD)!=InputDevice.SOURCE_KEYBOARD &&
         (d.getSources()&InputDevice.SOURCE_GAMEPAD)!=InputDevice.SOURCE_GAMEPAD &&
         (d.getSources()&InputDevice.SOURCE_DPAD)!=InputDevice.SOURCE_DPAD)continue;
      devices.add(d);labels.add(d.getName()+" (ID "+id+")");
    }
    if(devices.isEmpty()){toast("연결된 외부 입력장치가 없습니다");return;}
    new AlertDialog.Builder(this).setTitle("입력장치 선택").setItems(labels.toArray(new String[0]),(dialog,pos)->{
      InputDevice d=devices.get(pos);
      prefs(this).edit().putString("device",d.getDescriptor()).putString("deviceName",d.getName())
        .putBoolean("learning",false).apply();update();
    }).show();
  }
  private void showTargetPicker(String source){
    new AlertDialog.Builder(this).setTitle("입력 "+source+" → 출력 기능")
      .setItems(NAMES,(dialog,choice)->{
        prefs(this).edit().putInt("map."+source,TARGETS[choice]).apply();update();
      }).setNegativeButton("취소",null).show();
  }
  private void update(){
    if(status==null||rows==null)return;
    String device=prefs(this).getString("deviceName","선택되지 않음");
    status.setText("선택한 장치: "+device+(mappingOn(this)?" · 매핑 ON":" · 매핑 OFF"));
    rows.removeAllViews();
    Map<String,?> values=new TreeMap<>(prefs(this).getAll());
    int count=0;
    for(Map.Entry<String,?> e:values.entrySet()){
      if(!e.getKey().startsWith("map.")||!(e.getValue() instanceof Integer))continue;
      String source=e.getKey().substring(4);int target=(Integer)e.getValue();
      String name=KeyEvent.keyCodeToString(target);
      for(int i=0;i<TARGETS.length;i++)if(TARGETS[i]==target){name=NAMES[i];break;}
      Button b=new Button(this);
      b.setAllCaps(false);b.setText(source+" → "+name+"    (수정/삭제)");
      b.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(source)
        .setItems(new String[]{"매핑 수정","매핑 삭제"},(d,which)->{
          if(which==0)showTargetPicker(source);else{
            prefs(this).edit().remove("map."+source).apply();update();
          }
        }).show());
      rows.addView(b);count++;
    }
    if(count==0){TextView t=new TextView(this);t.setText("등록된 키가 없습니다.");rows.addView(t);}
  }
  private void toast(String s){android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_SHORT).show();}
}
