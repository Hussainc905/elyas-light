package com.elyastech.lightcontrol;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Arabic Android control panel for ESP32 V1.6 Bluetooth Classic SPP. */
public final class MainActivity extends Activity implements BluetoothClient.Listener {
    private static final int ASK_BT = 39;
    private static final int BG = 0xFF0A1424;
    private static final int SURFACE = 0xFF132338;
    private static final int SURFACE_2 = 0xFF192D45;
    private static final int STROKE = 0xFF294159;
    private static final int WHITE = 0xFFF4FAFF;
    private static final int MUTED = 0xFF9CB0C6;
    private static final int CYAN = 0xFF28D1C2;
    private static final int GOLD = 0xFFF8C662;
    private static final int RED = 0xFFFF667C;
    private static final int BLUE = 0xFF5EAFF5;
    private static final int[] SPEEDS = {100,150,200,300,450,600,800,1000,1400,2000,3000};

    private BluetoothClient bluetooth;
    private final ProtocolState state = new ProtocolState();
    private boolean connected = false;
    private boolean connecting = false;
    private boolean authorized = false;
    private boolean draggingSpeed = false;
    private String connectedDevice = "";

    private TextView connectButton, pinButton, linkValue, linkHint, receiverValue;
    private TextView skinButton, pair1Button, pair2Button, pair3Button, fourButton, sixButton;
    private TextView flashButton, waveButton, autoButton, manualButton, allOffButton, strongOffButton;
    private TextView speedValue, statusInfo;
    private SeekBar speedSeek;
    private final List<TextView> controlViews = new ArrayList<>();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 29) getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        bluetooth = new BluetoothClient(this);
        buildScreen();
        refreshUi();
    }

    @Override public void onDestroy() {
        if (bluetooth != null) bluetooth.disconnect();
        super.onDestroy();
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private GradientDrawable fill(int color, float radius, int border) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (border != 0) d.setStroke(dp(1), border);
        return d;
    }

    private GradientDrawable banner() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF163951, 0xFF113E47, 0xFF10283B});
        d.setCornerRadius(dp(23));
        d.setStroke(dp(1), 0xFF275A66);
        return d;
    }

    private LinearLayout col() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return ll;
    }

    private LinearLayout row() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return ll;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(top);
        return p;
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextColor(color);
        tv.setTextSize(sp);
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        tv.setIncludeFontPadding(true);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private TextView action(String label, int color) {
        TextView t = text(label, 16, WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(63));
        t.setPadding(dp(10),dp(14),dp(10),dp(14));
        t.setBackground(fill(SURFACE_2, 17, STROKE));
        t.setClickable(true);
        t.setFocusable(true);
        t.setContentDescription(label);
        controlViews.add(t);
        return t;
    }

    private LinearLayout card() {
        LinearLayout box = col();
        box.setPadding(dp(17), dp(16), dp(17), dp(18));
        box.setBackground(fill(SURFACE, 21, STROKE));
        return box;
    }

    private void sectionTitle(LinearLayout target, String heading, String caption) {
        target.addView(text(heading, 19, WHITE, true), lp(-1,-2,24));
        if (caption != null && !caption.isEmpty()) {
            target.addView(text(caption, 12, MUTED, false), lp(-1,-2,5));
        }
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout page = col();
        page.setPadding(dp(18),dp(20),dp(18),dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1,-2));
        setContentView(scroll);

        LinearLayout hero = col();
        hero.setPadding(dp(19),dp(20),dp(19),dp(19));
        hero.setBackground(banner());
        page.addView(hero, lp(-1,-2,0));

        LinearLayout heroTop = row();
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text("ϟ",37,GOLD,true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(fill(0xFF183F53,18,0xFF357E83));
        heroTop.addView(icon, new LinearLayout.LayoutParams(dp(56),dp(56)));
        LinearLayout name = col();
        name.setPadding(dp(13),0,0,0);
        name.addView(text("ELYAS-TECH",22,WHITE,true));
        name.addView(text("LIGHT CONTROL",13,CYAN,true));
        heroTop.addView(name, new LinearLayout.LayoutParams(0,-2,1));
        hero.addView(heroTop);
        hero.addView(text("تحكم ذكي بإضاءة السيارة",15,WHITE,true),lp(-1,-2,18));
        hero.addView(text("من الهاتف إلى الشاشة بالبلوتوث، ومن الشاشة إلى الريليات عبر ESP-NOW",12,0xFFB6CED9,false),lp(-1,-2,5));

        LinearLayout conn = card();
        page.addView(conn, lp(-1,-2,15));
        LinearLayout statusRow = row();
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        linkValue = text("غير متصل",18,GOLD,true);
        statusRow.addView(linkValue,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge = text("Bluetooth SPP",12,CYAN,true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10),dp(8),dp(10),dp(8));
        badge.setBackground(fill(0xFF183E49,24,0xFF267677));
        statusRow.addView(badge);
        conn.addView(statusRow);
        linkHint = text("اضغط اتصال واختر ELYAS-LIGHT من الأجهزة المقترنة",12,MUTED,false);
        conn.addView(linkHint,lp(-1,-2,9));
        receiverValue = text("تأكيد المستقبل: بانتظار الاتصال",12,MUTED,false);
        conn.addView(receiverValue,lp(-1,-2,8));
        LinearLayout connButtons = row();
        connectButton = action("اتصال بالشاشة",CYAN);
        connectButton.setBackground(fill(0xFF1B726F,15,0xFF289D93));
        connectButton.setOnClickListener(v -> onConnectClick());
        pinButton = action("رمز الدخول",MUTED);
        pinButton.setOnClickListener(v -> editPin(false));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,dp(53),1.3f);
        p1.setMarginEnd(dp(7));
        connButtons.addView(connectButton,p1);
        connButtons.addView(pinButton,new LinearLayout.LayoutParams(0,dp(53),1));
        conn.addView(connButtons,lp(-1,-2,16));

        sectionTitle(page,"الإضاءة الهادئة","السكن مستقل عن الإضاءة القوية والحركات");
        skinButton = action("◉  السكن",CYAN);
        skinButton.setOnClickListener(v -> toggle("SKIN", ProtocolState.SKIN));
        page.addView(skinButton,lp(-1,-2,11));

        sectionTitle(page,"الضوء القوي","الأزواج موصولة حسب ترتيب الريليات في المستقبل");
        LinearLayout pairs = row();
        pair1Button = action("◉  1 + 2",GOLD);
        pair2Button = action("◉  3 + 4",GOLD);
        pair3Button = action("◉  5 + 6",GOLD);
        pair1Button.setOnClickListener(v -> toggle("PAIR1", ProtocolState.PAIR1));
        pair2Button.setOnClickListener(v -> toggle("PAIR2", ProtocolState.PAIR2));
        pair3Button.setOnClickListener(v -> toggle("PAIR3", ProtocolState.PAIR3));
        addEqual(pairs,pair1Button,true);
        addEqual(pairs,pair2Button,true);
        addEqual(pairs,pair3Button,false);
        page.addView(pairs,lp(-1,-2,11));

        LinearLayout grouped = row();
        fourButton = action("✦  القوي 4",GOLD);
        sixButton = action("✦  القوي 6",GOLD);
        fourButton.setOnClickListener(v -> toggle("FOUR", ProtocolState.FOUR));
        sixButton.setOnClickListener(v -> toggle("SIX", ProtocolState.SIX));
        addEqual(grouped,fourButton,true);
        addEqual(grouped,sixButton,false);
        page.addView(grouped,lp(-1,-2,9));

        sectionTitle(page,"حركات الإضاءة","الاختيار نفسه مرة ثانية يوقف الحركة");
        LinearLayout fx1 = row();
        flashButton = action("ϟ  رماش سريع",GOLD);
        waveButton = action("↔  موجة الأزواج",CYAN);
        flashButton.setOnClickListener(v -> switchMode(1,"FLASH"));
        waveButton.setOnClickListener(v -> switchMode(2,"WAVE"));
        addEqual(fx1,flashButton,true);
        addEqual(fx1,waveButton,false);
        page.addView(fx1,lp(-1,-2,11));

        LinearLayout fx2 = row();
        autoButton = action("✧  العرض التلقائي",BLUE);
        manualButton = action("■  إيقاف الحركات",MUTED);
        autoButton.setOnClickListener(v -> switchMode(3,"AUTO"));
        manualButton.setOnClickListener(v -> command("MODE MANUAL"));
        addEqual(fx2,autoButton,true);
        addEqual(fx2,manualButton,false);
        page.addView(fx2,lp(-1,-2,9));

        LinearLayout speedCard = card();
        page.addView(speedCard,lp(-1,-2,15));
        LinearLayout speedHeading = row();
        TextView speedLabel = text("سرعة الحركة",17,WHITE,true);
        speedHeading.addView(speedLabel,new LinearLayout.LayoutParams(0,-2,1));
        speedValue = text("200 ms",18,CYAN,true);
        speedHeading.addView(speedValue);
        speedCard.addView(speedHeading);
        speedCard.addView(text("أسرع   ◀                                      ▶   أبطأ",11,MUTED,false),lp(-1,-2,10));
        speedSeek = new SeekBar(this);
        speedSeek.setMax(SPEEDS.length-1);
        speedSeek.setProgress(2);
        if(Build.VERSION.SDK_INT>=21) {
            speedSeek.setProgressTintList(ColorStateList.valueOf(CYAN));
            speedSeek.setThumbTintList(ColorStateList.valueOf(CYAN));
        }
        speedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (draggingSpeed || fromUser) speedValue.setText(SPEEDS[progress] + " ms");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { draggingSpeed = true; }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                draggingSpeed = false;
                command("SPEED " + SPEEDS[bar.getProgress()]);
            }
        });
        speedCard.addView(speedSeek,lp(-1,dp(48),9));
        speedCard.addView(text("تنبيه: سرعات 100–200 ms قد تستهلك عمر الريليات الميكانيكية بسرعة.",12,0xFFE4B985,false),lp(-1,-2,4));

        sectionTitle(page,"الإيقاف","أوامر مباشرة للمستقبل عبر شاشة ESP32");
        LinearLayout offRow = row();
        strongOffButton = action("إطفاء القوي",MUTED);
        allOffButton = action("⏻  إطفاء الجميع",RED);
        strongOffButton.setOnClickListener(v -> command("STRONG OFF"));
        allOffButton.setOnClickListener(v -> command("ALL OFF"));
        allOffButton.setBackground(fill(0xFF722C3B,17,0xFFB34E61));
        addEqual(offRow,strongOffButton,true);
        addEqual(offRow,allOffButton,false);
        page.addView(offRow,lp(-1,-2,11));

        statusInfo = text("أوامر التطبيق متوافقة مع برنامج الشاشة V1.6 Bluetooth.",12,MUTED,false);
        statusInfo.setGravity(Gravity.CENTER);
        page.addView(statusInfo,lp(-1,-2,23));
        TextView footer = text("ELYAS-TECH  •  ANDROID V1.0  •  OFFLINE",11,0xFF58718A,true);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer,lp(-1,-2,12));
    }

    private void addEqual(LinearLayout parent, View view, boolean margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,-2,1);
        if (margin) p.setMarginEnd(dp(8));
        parent.addView(view,p);
    }

    private String accessPin() {
        return getPreferences(MODE_PRIVATE).getString("app_access_code", "462913");
    }

    private void editPin(boolean retry) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        field.setText(accessPin());
        field.setSelection(field.length());
        LinearLayout space = col();
        space.setPadding(dp(24),dp(8),dp(24),dp(2));
        space.addView(field);
        new AlertDialog.Builder(this)
                .setTitle("رمز دخول الشاشة")
                .setMessage("لازم يطابق BT_ACCESS_CODE الموجود بكود ESP32. الرمز الافتراضي 462913.")
                .setView(space)
                .setNegativeButton("إلغاء", (d,w)->{})
                .setPositiveButton("حفظ", (d,w)-> {
                    String s = field.getText().toString().trim();
                    if (!s.matches("[0-9]{6}")) { toast("الرمز لازم 6 أرقام"); return; }
                    getPreferences(MODE_PRIVATE).edit().putString("app_access_code",s).apply();
                    toast("تم حفظ رمز التطبيق");
                    if (retry && connected) bluetooth.send("AUTH " + s);
                }).show();
    }

    private void onConnectClick() {
        if (connected) {
            bluetooth.disconnect();
            return;
        }
        if (Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},ASK_BT);
            return;
        }
        showPaired();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if (requestCode==ASK_BT) {
            if (results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) showPaired();
            else toast("فعّل إذن الأجهزة المجاورة حتى يتصل التطبيق بالبلوتوث");
        }
    }

    @SuppressWarnings("MissingPermission")
    private void showPaired() {
        BluetoothManager manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager==null ? null : manager.getAdapter();
        if (adapter==null) { toast("هذا الهاتف لا يدعم Bluetooth Classic"); return; }
        if (!adapter.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("البلوتوث مطفأ")
                    .setMessage("شغّل Bluetooth من إعدادات الهاتف، ثم ارجع للتطبيق.")
                    .setNegativeButton("إلغاء",null)
                    .setPositiveButton("الإعدادات",(d,w)->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                    .show();
            return;
        }
        Set<BluetoothDevice> paired;
        try { paired = adapter.getBondedDevices(); }
        catch (SecurityException e) { toast("يرجى السماح بإذن البلوتوث"); return; }
        if (paired==null || paired.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("ماكو أجهزة مقترنة")
                    .setMessage("افتح إعدادات Bluetooth واقترن أولاً مع ELYAS-LIGHT، ثم ارجع للتطبيق. إذا برنامج Bluetooth Terminal متصل، افصله أولاً.")
                    .setPositiveButton("الإعدادات",(d,w)->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                    .setNegativeButton("إلغاء",null).show();
            return;
        }
        ArrayList<BluetoothDevice> devices = new ArrayList<>(paired);
        Collections.sort(devices,(a,b)-> {
            String an = deviceName(a), bn = deviceName(b);
            if (an.equalsIgnoreCase("ELYAS-LIGHT")) return -1;
            if (bn.equalsIgnoreCase("ELYAS-LIGHT")) return 1;
            return an.compareToIgnoreCase(bn);
        });
        String[] choices = new String[devices.size()];
        for(int i=0;i<devices.size();i++) {
            BluetoothDevice d=devices.get(i);
            choices[i] = deviceName(d) + "\n" + d.getAddress();
        }
        new AlertDialog.Builder(this)
                .setTitle("اختر شاشة ELYAS-LIGHT")
                .setItems(choices,(dialog,which)-> {
                    authorized=false;
                    state.resetForNewConnection();
                    refreshUi();
                    bluetooth.connect(devices.get(which),accessPin());
                }).setNegativeButton("إلغاء",null).show();
    }

    @SuppressWarnings("MissingPermission")
    private String deviceName(BluetoothDevice device) {
        try { String s=device.getName(); return s==null || s.trim().isEmpty() ? "جهاز بدون اسم" : s; }
        catch (SecurityException e) { return "جهاز Bluetooth"; }
    }

    private void toggle(String name, int mask) {
        if (!needAuthorization()) return;
        command(name + (ProtocolState.enabled(state.requestedBits,mask) ? " OFF" : " ON"));
    }

    private void switchMode(int mode,String value) {
        if (!needAuthorization()) return;
        command("MODE " + (state.mode == mode ? "MANUAL" : value));
    }

    private boolean needAuthorization() {
        if (!connected || !authorized) {
            toast("اتصل بالشاشة أولاً وانتظر قبول رمز الدخول");
            return false;
        }
        return true;
    }

    private void command(String ascii) {
        if (!needAuthorization()) return;
        bluetooth.send(ascii);
        statusInfo.setText("تم إرسال: " + ascii + " — بانتظار تحديث الحالة");
    }

    @Override public void onConnecting(String label) {
        connected=false; connecting=true; authorized=false;
        connectedDevice=label;
        linkValue.setText("جاري الاتصال...");
        linkHint.setText("محاولة الاتصال بـ " + label);
        refreshUi();
    }

    @Override public void onConnected(String label) {
        connected=true; connecting=false;
        connectedDevice=label;
        linkHint.setText("تم الاتصال بـ " + label + " — جاري إرسال رمز الدخول");
        refreshUi();
    }

    @Override public void onDisconnected(String reason) {
        connected=false;connecting=false;authorized=false;
        connectedDevice="";
        linkHint.setText("اختر ELYAS-LIGHT للاتصال. " + (reason==null?"":reason));
        refreshUi();
    }

    @Override public void onLine(String line) {
        if (line.startsWith("OK AUTH")) {
            authorized=true;
            statusInfo.setText("تم قبول رمز الدخول بنجاح");
            bluetooth.send("STATUS");
        } else if (line.startsWith("ERR AUTH") || line.startsWith("ERR UNAUTHORIZED")) {
            authorized=false;
            statusInfo.setText("رمز الدخول غير مطابق لكود ESP32");
            new AlertDialog.Builder(this)
                    .setTitle("رمز الدخول غير صحيح")
                    .setMessage("تأكد من رمز BT_ACCESS_CODE الموجود في برنامج V1.6. يمكن تعديله من زر رمز الدخول.")
                    .setNegativeButton("إغلاق",null)
                    .setPositiveButton("تعديل الرمز",(d,w)->editPin(true)).show();
        } else if (line.startsWith("ERR ")) {
            statusInfo.setText("خطأ من الشاشة: " + line);
        } else if (line.startsWith("OK")) {
            statusInfo.setText("استلمت الشاشة الأمر؛ بانتظار تأكيد المستقبل إن توفر");
        }
        if (state.consume(line)) refreshUi();
        else if (line.startsWith("OK AUTH")) refreshUi();
    }

    private void styleControl(TextView t,boolean selected,int selectedColor,boolean danger) {
        if (t==null) return;
        int bg = selected ? selectedColor : (danger ? 0xFF63283B : SURFACE_2);
        int border = selected ? selectedColor : (danger ? 0xFFAD4559 : STROKE);
        t.setBackground(fill(bg,17,border));
        t.setTextColor(selected && !danger ? BG : WHITE);
        t.setAlpha(authorized ? 1f : 0.68f);
    }

    private void refreshUi() {
        if (linkValue==null) return;
        linkValue.setText(connecting ? "جاري الاتصال..." : !connected ? "غير متصل" : authorized ? "متصل وجاهز" : "متصل — تحقق الرمز");
        linkValue.setTextColor(connecting ? BLUE : !connected ? GOLD : authorized ? CYAN : BLUE);
        connectButton.setText(connecting ? "اتصال جارٍ..." : connected ? "فصل الاتصال" : "اتصال بالشاشة");
        connectButton.setEnabled(!connecting);
        if (connected && authorized) linkHint.setText("اتصال Bluetooth Classic مع " + connectedDevice);

        if (!connected) {
            receiverValue.setText("تأكيد المستقبل: غير متصل بالشاشة");
        } else if (!authorized) {
            receiverValue.setText("تأكيد المستقبل: بانتظار قبول رمز الدخول");
        } else if (!state.ackReceivedThisConnection) {
            receiverValue.setText("ESP-NOW: " + (state.localRadioReady==1?"مُهيأ":"غير جاهز") + "  •  لم يصل تأكيد من المستقبل بعد");
        } else {
            receiverValue.setText(String.format(Locale.US,
                    "آخر تأكيد من المستقبل #%d  •  مخارج %02X",state.lastAckSequence,state.lastAckBits));
        }

        if (state.hasState) {
            if (!draggingSpeed) {
                int best=0, distance=Integer.MAX_VALUE;
                for(int i=0;i<SPEEDS.length;i++) {
                    int delta=Math.abs(SPEEDS[i]-state.speedMs);
                    if (delta < distance) {distance=delta;best=i;}
                }
                speedSeek.setProgress(best);
                speedValue.setText(state.speedMs + " ms");
            }
        }

        int b = state.requestedBits;
        styleControl(skinButton,ProtocolState.enabled(b,ProtocolState.SKIN),CYAN,false);
        styleControl(pair1Button,ProtocolState.enabled(b,ProtocolState.PAIR1),GOLD,false);
        styleControl(pair2Button,ProtocolState.enabled(b,ProtocolState.PAIR2),GOLD,false);
        styleControl(pair3Button,ProtocolState.enabled(b,ProtocolState.PAIR3),GOLD,false);
        styleControl(fourButton,ProtocolState.enabled(b,ProtocolState.FOUR),GOLD,false);
        styleControl(sixButton,ProtocolState.enabled(b,ProtocolState.SIX),GOLD,false);
        styleControl(flashButton,state.mode==1,GOLD,false);
        styleControl(waveButton,state.mode==2,CYAN,false);
        styleControl(autoButton,state.mode==3,BLUE,false);
        styleControl(manualButton,state.mode==0,BLUE,false);
        styleControl(strongOffButton,false,MUTED,false);
        styleControl(allOffButton,false,RED,true);
        for(TextView v:controlViews) {
            if (v != connectButton && v != pinButton) v.setEnabled(authorized);
        }
        speedSeek.setEnabled(authorized);
    }

    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
}
