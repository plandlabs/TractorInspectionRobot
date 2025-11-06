package kr.re.kitech.tractorinspectionrobot.views.activity;

import static kr.re.kitech.tractorinspectionrobot.utils.NetworkUtil.TYPE_MOBILE;
import static kr.re.kitech.tractorinspectionrobot.utils.NetworkUtil.TYPE_WIFI;
import static kr.re.kitech.tractorinspectionrobot.utils.NetworkUtil.getConnectivityStatus;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import kr.re.kitech.tractorinspectionrobot.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SplashActivity extends Activity {
    //기기 리스트 테스트
    //public String machineList = "{\"data\" :[{\"name\":\"hub1002\",\"pass\":\"1234\"},{\"name\":\"hub1001\",\"pass\":\"1234\"}]}";
    SharedPreferences setting;
    SharedPreferences.Editor editor;
    public static Context mContext;
    private static final int REQ_NOTI = 200;
    static final int PERMISSIONS_REQUEST = 0x0000001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mContext = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ Manifest.permission.POST_NOTIFICATIONS }, REQ_NOTI);
            } else {
                // ✅ 이미 허용된 상태면 바로 다음 단계로
                onNextMainChange();
            }
        } else {
            // ✅ 13 미만은 권한 필요 없음 → 바로 다음 단계로
            onNextMainChange();
        }

    }

//    public boolean onCheckPermission(){
//        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                || ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
//                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
//            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
//
//                Toast.makeText(this, "위치 권한을 설정하셔야합니다.",Toast.LENGTH_LONG).show();
//
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.CHANGE_NETWORK_STATE},
//                        PERMISSIONS_REQUEST);
//            } else {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.CHANGE_NETWORK_STATE},
//                        PERMISSIONS_REQUEST);
//            }
//            return false;
//        }else {
//            return true;
//
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_NOTI) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 설정되었습니다.", Toast.LENGTH_LONG).show();
                onNextMainChange();
            } else {
                Toast.makeText(this, "알림 권한이 거부되어 앱을 종료하였습니다.", Toast.LENGTH_LONG).show();

                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onNextMainChange(){
        Handler hd = new Handler();

        int status = getConnectivityStatus(getApplicationContext());
        if (status == TYPE_MOBILE) {
            Toast.makeText(this, "모바일 데이터로 연결었습니다.", Toast.LENGTH_SHORT).show();
            hd.postDelayed(new splashhandler(), 3000); // 3초 후에 hd Handler 실행
        } else
        if (status == TYPE_WIFI) {
            Toast.makeText(this, "와이파이로 연결었습니다.", Toast.LENGTH_SHORT).show();
            hd.postDelayed(new splashhandler(), 3000); // 3초 후에 hd Handler 실행
        } else {
            Toast.makeText(this, "인터넷이 연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
            hd.postDelayed(new splashhandler(), 3000); // 3초 후에 hd Handler 실행
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private class splashhandler implements Runnable {
        public void run() {
            setting = getSharedPreferences("setting", 0);
            if(!setting.getString("ID", "").isEmpty() && setting.getBoolean("Auto_Login_enabled", true)) {            //기기 리스트가 있을때

                startActivity(new Intent(getApplication(), MainActivity.class)); // 로딩이 끝난후 이동할 Activity
                SplashActivity.this.finish(); // 로딩페이지 Activity Stack에서 제거
            }else{
                startActivity(new Intent(getApplication(), LoginActivity.class)); // 로딩이 끝난후 이동할 Activity
                SplashActivity.this.finish(); // 로딩페이지 Activity Stack에서 제거
            }
        }
    }

}
