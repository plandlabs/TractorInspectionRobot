package kr.re.kitech.tractorinspectionrobot.views.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.tabs.TabLayout;
import kr.re.kitech.tractorinspectionrobot.R;

import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.views.tapPager.NonSwipeViewPager;
import kr.re.kitech.tractorinspectionrobot.views.tapPager.TabFragmentPagerAdapter;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FragmentManager fragmentManager = getSupportFragmentManager();
    private FragmentTransaction transaction;
    private Resources mResources;
    private Configuration mConfiguration;
    private String[] recieveKey;
    public static Context mContext;
    private BottomNavigationView mBottomNavigationView;
    private NavigationRailView mRightNavigationView;
    private Handler backHandler;
    private DrawerLayout drawerLayout;
    private boolean mFlag = false;
    private Vibrator mVibrator;
    public SharedPreferences setting;
    public SharedPreferences.Editor editor;
    private int landBottomMenuWidth = 80;   //dp

    final private String TAG =  "MainActivity";

    int PERMISSION_ALL = 1;

    String[] PERMISSIONS = null;

    String[] PERMISSIONS_M = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    String[] PERMISSIONS_S = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.POST_NOTIFICATIONS,
            //Manifest.permission.READ_EXTERNAL_STORAGE,
            //Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final static int REQUEST_ACCESS_FINE_LOCATION = 100;
    private final static int REQUEST_READ_PHONE_STATE = 101;
    private static final int REQUEST_CAMERA = 102;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 103;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 103;

    private NavigationView mNavView;
    private TextView memberNm;
    private static final int SERVER_PORT = 3000; // 서버 포트
    // LiveData 또는 ViewModel로 데이터 전달을 위해 선언
    private SharedMqttViewModel viewModel;


    TabFragmentPagerAdapter mSectionsPagerAdapter;
    NonSwipeViewPager mViewPager;
    TabLayout mTabLayout;

    int tabCnt = 2;
    private Handler socketHandler = new Handler();

    private View btnOpen, btnRobotChange;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mContext = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSIONS = PERMISSIONS_S;
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PERMISSIONS = PERMISSIONS_M;
        }

        checkPermissions();

        backHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0) {
                    mFlag = false;
                }
            }
        };

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setting = (SharedPreferences) getSharedPreferences("setting", 0);
        editor = setting.edit();


        mResources = Resources.getSystem();
        mConfiguration = mResources.getConfiguration();

        // 첫 화면 지정
        transaction = (FragmentTransaction) fragmentManager.beginTransaction();


        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(listener);

        mViewPager = (NonSwipeViewPager) findViewById(R.id.viewPager);
        mTabLayout = (TabLayout) findViewById(R.id.mTabLayout);
        mSectionsPagerAdapter = new TabFragmentPagerAdapter(fragmentManager, tabCnt, mContext, setting);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));


        mNavView = (NavigationView) findViewById(R.id.navView);
        if (mNavView != null) {
            mNavView.setNavigationItemSelectedListener(this);
        }

        btnOpen = (View) findViewById(R.id.btnOpen);
        btnOpen.setOnClickListener(v -> {
            mVibrator.vibrate(100);
            drawerLayout.openDrawer(mNavView);
        });
        btnRobotChange = (View) findViewById(R.id.btn_robot_change);
        btnRobotChange.setOnClickListener(v -> {
            mVibrator.vibrate(100);
        });


        viewModel = new ViewModelProvider(this).get(SharedMqttViewModel.class);


        this.onConfigurationChanged(mConfiguration);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        mVibrator.vibrate(100);
        Log.e("item.getItemId()", "============>"+item.getItemId());

        if(item.getItemId() == R.id.nav_side_2){

        }else if(item.getItemId() == R.id.nav_side_3){
            LogoutAlertDialog();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    DrawerLayout.DrawerListener listener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            //슬라이드 했을때
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            //Drawer가 오픈된 상황일때 호출
            try {
                memberNm = (TextView) findViewById(R.id.member_nm);
                memberNm.setText(setting.getString("USER_NM", "") + "(" + setting.getString("ID", "") + ")님");
            } catch (NullPointerException e){
                Log.e("mUserNm", "mUserNm is null!");
            }
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            // 닫힌 상황일 때 호출
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            // 특정상태가 변결될 때 호출
        }
    };
    private void checkPermissions() {
        if (!hasPermissions(this, PERMISSIONS)) {
            Log.e(TAG, "hasPermissions = false !!");
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            Log.e(TAG, "hasPermissions = true !!");
        }
    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    private void startPermissionRequest(int REQUEST_PERMISSIONS_REQUEST_CODE, String PERMISSIONS_NAME) {
        ActivityCompat.requestPermissions(this,
                new String[] {PERMISSIONS_NAME} , REQUEST_PERMISSIONS_REQUEST_CODE);
    }
    private class splashhandler implements Runnable {
        public void run() {
            setting = getSharedPreferences("setting", 0);
            //startActivity(new Intent(getApplication(), LoginActivity.class)); // 로딩이 끝난후 이동할 Activity
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        int grantResultsAll = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if(grantResults.length > 0) {

                for (int i=0; i<grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Permission: " + permissions[i] + " was " + grantResults[i]);
                    }
                    else {
                        Log.d(TAG, "Permission: " + permissions[i] + " was " + grantResults[i]);
                    }

                    grantResultsAll += grantResults[i];
                }

            }
            checkBatteryOptimization();

        }

        if (grantResultsAll == 0) {

        } else {
            Toast.makeText(this, "승인되지 않은 권한이 있습니다.", Toast.LENGTH_LONG).show();
        }
    }
    private final Map<String, AlertDialog> errorDialogMap = new HashMap<>();

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
// Tab 커스텀 뷰 설정 후
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab != null) {
                View customTabView = LayoutInflater.from(this).inflate(R.layout.tabview_custom_item, null);
                TextView tabText = customTabView.findViewById(R.id.tab_text);
                tabText.setText(mSectionsPagerAdapter.getPageTitle(i));
                tab.setCustomView(customTabView);

                // ✅ 첫 번째 탭은 선택 배경으로 초기화
                if (i == 0) {
                    customTabView.setBackgroundResource(R.drawable.tab_selected_background);
                }
            }
        }

        // Set TabSelectedListener
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mVibrator.vibrate(50);
                View tabView = tab.getCustomView();
                if (tabView != null) {
                    tabView.setBackgroundResource(R.drawable.tab_selected_background);
                }
                int position = tab.getPosition();

                // 관리자 여부
                boolean isAdmin = "admin".equals(setting.getString("ID", ""));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View tabView = tab.getCustomView();
                if (tabView != null) {
                    tabView.setBackgroundResource(R.drawable.tab_unselected_background);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }

        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int dpi = displayMetrics.densityDpi;
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1){
            if(resultCode==RESULT_OK){
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View focusView = getCurrentFocus();
        if (focusView != null) {
            Rect rect = new Rect();
            focusView.getGlobalVisibleRect(rect);
            int x = (int) ev.getX(), y = (int) ev.getY();
            if (!rect.contains(x, y)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
                focusView.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @SuppressLint("GestureBackNavigation")
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // 백 키를 터치한 경우
        if(keyCode == KeyEvent.KEYCODE_BACK){

            drawerLayout.closeDrawer(GravityCompat.START);
            if(!mFlag) {
                mVibrator.vibrate(100);
                Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료하실 수 있습니다.", Toast.LENGTH_SHORT).show();
                mFlag = true;
                backHandler.sendEmptyMessageDelayed(0, 2000); // 2초 내로 터치시
                return false;
            } else {
                mVibrator.vibrate(100);
                finish();
                return false;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    protected void LogoutAlertDialog() {
        drawerLayout.closeDrawer(GravityCompat.START);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.DarkAlertDialog);
        builder.setTitle("로그아웃");
        builder.setMessage("로그아웃 하시겠습니까?");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mVibrator.vibrate(100);

                        editor.remove("ID");
                        editor.remove("PW");
                        editor.remove("Auto_Login_enabled");
                        editor.commit();
                        startActivity(new Intent(getApplication(), LoginActivity.class));

                        Toast.makeText(getApplication(),"로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mVibrator.vibrate(100);
                    }
                });
        builder.show();
    }
    private void checkBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();

        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            // 배터리 최적화 예외 처리 요청
            showBatteryOptimizationDialog();
        }
    }

    private void showBatteryOptimizationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.DarkAlertDialog);
        builder.setTitle("배터리 최적화 예외 요청");
        builder.setMessage("이 앱이 백그라운드에서 안정적으로 동작하려면 배터리 최적화에서 제외해야 합니다. 설정 화면으로 이동하시겠습니까?");
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 설정 화면으로 이동
                requestBatteryOptimizationException();
            }
        });
        builder.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 사용자가 거부했을 때 처리할 코드
                Toast.makeText(MainActivity.this, "배터리 최적화로 인해 앱이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestBatteryOptimizationException() {
        try {
//            Intent intent = new Intent();
//            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//            startActivity(intent);
            Intent i = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if(pm.isIgnoringBatteryOptimizations(packageName)){
                i.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            } else {
                i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + packageName));
                startActivity(i);
            }
        } catch (Exception e) {
            Log.e("BatteryOptimization", "설정 화면으로 이동할 수 없습니다.", e);
        }
    }

}


