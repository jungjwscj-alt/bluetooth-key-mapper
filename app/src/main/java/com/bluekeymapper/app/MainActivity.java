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
import android.view.MotionEvent;
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
  private TextView mouseStatus;
  private View mouseView;
  private int nextWheel;
  static final int[] TARGETS={
    KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_SPACE,KeyEvent.KEYCODE_ESCAPE,KeyEvent.KEYCODE_TAB,
    KeyEvent.KEYCODE_DEL,KeyEvent.KEYCODE_FORWARD_DEL,KeyEvent.KEYCODE_PAGE_UP,KeyEvent.KEYCODE_PAGE_DOWN,
    KeyEvent.KEYCODE_MOVE_HOME,KeyEvent.KEYCODE_MOVE_END,KeyEvent.KEYCODE_F1,KeyEvent.KEYCODE_F2,
    KeyEvent.KEYCODE_F3,KeyEvent.KEYCODE_F4,SCROLL_UP,SCROLL_DOWN,SCROLL_LEFT,SCROLL_RIGHT,BACK,KeyEvent.KEYCODE_FORWARD,
    MouseMapping.WHEEL_UP,MouseMapping.WHEEL_DOWN,MouseMapping.WHEEL_LEFT,MouseMapping.WHEEL_RIGHT,
    MouseMapping.BUTTON_BACK,MouseMapping.BUTTON_FORWARD
  };
  static final String[] NAMES={"↑ 방향키","↓ 방향키","← 방향키","→ 방향키","Enter","Space","Esc","Tab",
    "Backspace","Delete","Page Up","Page Down","Home(커서)","End(커서)","F1","F2","F3","F4",
    "스크롤 위","스크롤 아래","스크롤 왼쪽","스크롤 오른쪽","Android 뒤로가기","앞으로가기(지원 앱 한정)",
    "마우스 휠 위","마우스 휠 아래","마우스 휠 왼쪽","마우스 휠 오른쪽",
    "마우스 측면 뒤로","마우스 측면 앞으로"};
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
    if(learn(this,e))return true;
    Integer target=lookup(this,e);
    if(target!=null && MouseMapping.isMouse(target)){
      if(e.getAction()==KeyEvent.ACTION_DOWN && e.getRepeatCount()==0)showMouseTest(target);
      return true;
    }
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
    TextView title=label("블루투스 키 맵퍼  v1.3");title.setTextSize(26);title.setTextColor(Color.rgb(35,78,205));
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
    if(!"lite".equals(BuildConfig.FLAVOR))button("③ 접근성 서비스 활성화",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    button("④ 입력기 활성화",()->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
    button("⑤ 기본 입력기 선택",()->{
      InputMethodManager im=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
      if(im!=null)im.showInputMethodPicker();
    });
    label("마우스 매핑 실험 — 이 앱 안에서만 진짜 휠 이벤트 생성");
    mouseStatus=label("외부 앱의 마우스 이벤트 주입은 지원되지 않습니다.");
    mouseView=new View(this){
      @Override public boolean onGenericMotionEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_SCROLL){
          mouseStatus.setText("마우스 휠 감지: 세로 "+e.getAxisValue(MotionEvent.AXIS_VSCROLL)+
            " / 가로 "+e.getAxisValue(MotionEvent.AXIS_HSCROLL)+" (앱 내부)");
          return true;
        }
        return super.onGenericMotionEvent(e);
      }
    };
    root.addView(mouseView,new LinearLayout.LayoutParams(1,1));
    button("마우스 휠 이벤트 자체 테스트",()->{
      int[] a={MouseMapping.WHEEL_UP,MouseMapping.WHEEL_DOWN,MouseMapping.WHEEL_LEFT,MouseMapping.WHEEL_RIGHT};
      showMouseTest(a[(nextWheel++)%a.length]);
    });
    label("매핑 목록");
    rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);root.addView(rows);
    label("테스트 입력란 (IME 키 출력 확인)");
    EditText test=new EditText(this);test.setHint("이곳을 눌러 방향키, Enter, Space 테스트");
    root.addView(test);
    label("lite".equals(BuildConfig.FLAVOR) ? "Lite: 접근성 서비스가 없습니다. IME를 켜고 현재 편집 중인 입력란에서만 매핑을 시도할 수 있습니다. 다른 앱의 스크롤, 뒤로가기와 전역 매핑은 지원하지 않습니다. IME 지원 여부는 앱마다 다릅니다." : "방향키/Enter 등은 현재 연결된 입력 대상에만 전달합니다. 스크롤/뒤로는 접근성 제스처로 실행하며 앱별로 제한될 수 있습니다.");
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
    boolean lite="lite".equals(BuildConfig.FLAVOR);
    final int[] targets;
    final String[] names;
    if(lite){
      targets=new int[24];names=new String[24];
      System.arraycopy(TARGETS,0,targets,0,18);
      System.arraycopy(NAMES,0,names,0,18);
      System.arraycopy(TARGETS,24,targets,18,6);
      for(int i=0;i<6;i++)names[18+i]=NAMES[24+i]+" (앱 내부 시험)";
    }else{targets=TARGETS;names=NAMES;}
    new AlertDialog.Builder(this).setTitle("입력 "+source+" → 출력 기능")
      .setItems(names,(dialog,choice)->{
        prefs(this).edit().putInt("map."+source,targets[choice]).apply();update();
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
  private void showMouseTest(int action){
    if(mouseStatus==null)return;
    if(MouseMapping.isWheel(action)&&mouseView!=null){
      MotionEvent ev=MouseMapping.wheelEvent(action);
      if(ev!=null){try{mouseView.dispatchGenericMotionEvent(ev);}finally{ev.recycle();}}
    }else{
      mouseStatus.setText(action==MouseMapping.BUTTON_BACK ?
        "마우스 뒤로: 앱 내부 감지됨. 외부 앱에서는 접근성 Android 뒤로로 대체" :
        "마우스 앞으로: 앱 내부 감지됨. 외부 앱에서는 IME KEYCODE_FORWARD만 제한적으로 시도");
    }
  }
  private void toast(String s){android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_SHORT).show();}
}
