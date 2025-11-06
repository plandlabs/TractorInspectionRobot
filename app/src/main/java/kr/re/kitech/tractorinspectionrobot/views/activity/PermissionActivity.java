package kr.re.kitech.tractorinspectionrobot.views.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import kr.re.kitech.tractorinspectionrobot.R;

import java.util.BitSet;

public class PermissionActivity extends Activity {
    Button overlayPermissionButton, writeSettingPermissiononButton, accessibilityPermissionButton;
    public static Context mContext;
    private Handler backHandler;
    private boolean mFlag = false;


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setContentView(R.layout.popup_activity);
        setContentView(R.layout.activity_permission_set);
        mContext = this;

        //if (getSupportActionBar() != null) {
        //    getSupportActionBar().setTitle("권한");
        //}

        overlayPermissionButton = findViewById(R.id.button_overlay_permission);
        writeSettingPermissiononButton = findViewById(R.id.button_write_setting_permission);
        accessibilityPermissionButton = findViewById(R.id.button_accessibility_permission);


        overlayPermissionButton.setOnClickListener(v -> {
            if (!overlayPermissionButton.getText().equals("완료")) {
                startActivity(
                        new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                );
            }
        });

        backHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0) {
                    mFlag = false;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        BitSet bitSet = new BitSet();

        if ( checkDrawOverlayPermission(getApplicationContext()) ){
            bitSet.set(0);
            overlayPermissionButton.setText("완료");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                overlayPermissionButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimaryDark));
            }
        }
        /*
        if ( checkWriteSettingPermission() ){
            bitSet.set(1);
            writeSettingPermissiononButton.setText("완료");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                writeSettingPermissiononButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimaryDark));
            }
        }
        if( checkAccessibilityService(getApplicationContext()) ) {
            bitSet.set(2);
            accessibilityPermissionButton.setText("완료");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                accessibilityPermissionButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimaryDark));
            }
        }
         */

        if(bitSet.get(0)){
        //if(bitSet.get(0) && bitSet.get(1) && bitSet.get(2)){
            /*
            startActivity(new Intent(getApplicationContext(), SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION)
            );
             */
            finish();
        }
    }

    //오버레이 권한 확인
    //마시멜로 이상부터만 가능
    public boolean checkDrawOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                return true;
            } else {
                return false;
            }
        }
        else{
            return true;
        }
    }

    //시스템을 변경 할 수 있는 권한 으로 넘겨주는 부분
    //마시멜로 이상부터만 가능
    public boolean checkWriteSettingPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(getApplicationContext())) {
                return true;
            }
            else{
                return false;
            }
        }
        else {
            return true;
        }
    }

    // 접근성 권한이 있는지 없는지 확인하는 부분
    /*
    private boolean checkAccessibilityService(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + MyService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
//            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);

                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //바깥레이어 클릭시 안닫히게
        if(event.getAction()== MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // 백 키를 터치한 경우
        if(keyCode == KeyEvent.KEYCODE_BACK){


            if(!mFlag) {
                Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
                mFlag = true;
                backHandler.sendEmptyMessageDelayed(0, 2000); // 2초 내로 터치시
                return false;
            } else {
                finish();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

}
