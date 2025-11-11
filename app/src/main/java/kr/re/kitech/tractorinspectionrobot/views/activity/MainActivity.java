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

import androidx.activity.OnBackPressedCallback;
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
import kr.re.kitech.tractorinspectionrobot.ui.BackPressHandler;
import kr.re.kitech.tractorinspectionrobot.ui.TouchHideKeyboardHelper;
import kr.re.kitech.tractorinspectionrobot.views.tapPager.NonSwipeViewPager;
import kr.re.kitech.tractorinspectionrobot.views.tapPager.TabFragmentPagerAdapter;

public class MainActivity extends FragmentActivity implements NavigationView.OnNavigationItemSelectedListener {
    private BackPressHandler backPress = new BackPressHandler();
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private FragmentTransaction transaction;
    private Resources mResources;
    private Configuration mConfiguration;
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
    };

    String[] PERMISSIONS_S = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private NavigationView mNavView;
    private TextView memberNm;

    // LiveData 전달용
    private SharedMqttViewModel viewModel;

    TabFragmentPagerAdapter mSectionsPagerAdapter;
    NonSwipeViewPager mViewPager;
    TabLayout mTabLayout;

    int tabCnt = 2;

    private View btnOpen, btnRobotChange;
    private boolean isMqttConnected = false;

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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PERMISSIONS = PERMISSIONS_M;
        }
        checkPermissions();

        backHandler = new Handler() { @Override public void handleMessage(Message msg) { if(msg.what == 0) mFlag = false; } };

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setting = getSharedPreferences("setting", 0);
        editor = setting.edit();

        mResources = Resources.getSystem();
        mConfiguration = mResources.getConfiguration();

        transaction = fragmentManager.beginTransaction();

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(listener);

        mViewPager = findViewById(R.id.viewPager);
        mTabLayout  = findViewById(R.id.mTabLayout);
        mSectionsPagerAdapter = new TabFragmentPagerAdapter(fragmentManager, tabCnt, mContext, setting);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        btnRobotChange = findViewById(R.id.btn_robot_change);
        btnOpen        = findViewById(R.id.btnOpen);

        mNavView = findViewById(R.id.navView);
        if (mNavView != null) mNavView.setNavigationItemSelectedListener(this);

        btnOpen.setOnClickListener(v -> {
            mVibrator.vibrate(100);
            drawerLayout.openDrawer(mNavView);
        });

        viewModel = new ViewModelProvider(this).get(SharedMqttViewModel.class);

        // 옵저버: 캐시 + UI 동기화
        viewModel.getMqttConnected().observe(this, connected -> {
            isMqttConnected = Boolean.TRUE.equals(connected);
            updateRobotChangeButton(isMqttConnected);
        });

        // 토글 버튼
        btnRobotChange.setOnClickListener(v -> {
            mVibrator.vibrate(80);
            if (isMqttConnected) {
                showDisconnectConfirm();
            } else {
                showConnectConfirm();;
            }
        });

        this.onConfigurationChanged(mConfiguration);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 1) 드로어가 열려 있으면 먼저 닫기
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    mVibrator.vibrate(10);
                    return;
                }

                // 2) ViewPager가 첫 탭이 아니면 첫 탭으로 이동
                if (mViewPager != null && mViewPager.getCurrentItem() != 0) {
                    mViewPager.setCurrentItem(0, true);
                    mVibrator.vibrate(10);
                    return;
                }

                // 3) 프래그먼트 백스택이 있으면 pop
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    mVibrator.vibrate(10);
                    return;
                }

                // 4) 위 케이스 모두 아니면: 두 번 눌러 종료
                mVibrator.vibrate(10);
                backPress.handle(
                        () -> {
                            // finish 동작
                            finish();
                            // 필요하면 애니메이션 제거
                            // overridePendingTransition(0, 0);
                        },
                        Toast.makeText(
                                MainActivity.this,
                                "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.",
                                Toast.LENGTH_SHORT
                        )
                );
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        mVibrator.vibrate(100);
        if(item.getItemId() == R.id.nav_side_3){
            LogoutAlertDialog();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }// 뒤로 두 번 눌러 종료
    @SuppressLint("GestureBackNavigation")
    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) {
            mVibrator.vibrate(10);
            return backPress.handle(this::finish,
                    Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT));
        }
        return super.onKeyDown(code, e);
    }

    DrawerLayout.DrawerListener listener = new DrawerLayout.DrawerListener() {
        @Override public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}
        @Override public void onDrawerOpened(@NonNull View drawerView) {
            try {
                memberNm = findViewById(R.id.member_nm);
                memberNm.setText(setting.getString("USER_NM", "") + "(" + setting.getString("ID", "") + ")님");
            } catch (NullPointerException e){ Log.e("mUserNm", "mUserNm is null!"); }
        }
        @Override public void onDrawerClosed(@NonNull View drawerView) {}
        @Override public void onDrawerStateChanged(int newState) {}
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkBatteryOptimization();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        // 탭 커스텀
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab != null) {
                View customTabView = LayoutInflater.from(this).inflate(R.layout.tabview_custom_item, null);
                TextView tabText = customTabView.findViewById(R.id.tab_text);
                tabText.setText(mSectionsPagerAdapter.getPageTitle(i));
                tab.setCustomView(customTabView);
                if (i == 0) customTabView.setBackgroundResource(R.drawable.tab_selected_background);
            }
        }
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                mVibrator.vibrate(50);
                View tabView = tab.getCustomView();
                if (tabView != null) tabView.setBackgroundResource(R.drawable.tab_selected_background);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {
                View tabView = tab.getCustomView();
                if (tabView != null) tabView.setBackgroundResource(R.drawable.tab_unselected_background);
            }
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 즉시 반영(캐시)
        Boolean last = viewModel.getMqttConnected().getValue();
        isMqttConnected = (last != null && last);
        updateRobotChangeButton(isMqttConnected);

        // 서비스에 현재 상태 요청 → 브로드캐스트 수신 시 옵저버로 최종 동기화
        viewModel.requestStatus();
    }

    @Override protected void onPause() { super.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int dpi = displayMetrics.densityDpi;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        TouchHideKeyboardHelper.dispatch(this, ev);
        return super.dispatchTouchEvent(ev);
    }

    protected void LogoutAlertDialog() {
        drawerLayout.closeDrawer(GravityCompat.START);
        new AlertDialog.Builder(mContext, R.style.DarkAlertDialog)
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> {
                    mVibrator.vibrate(100);
                    editor.remove("ID");
                    editor.remove("PW");
                    editor.remove("Auto_Login_enabled");
                    editor.commit();
                    startActivity(new Intent(getApplication(), LoginActivity.class));
                    Toast.makeText(getApplication(),"로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("취소", (dialog, which) -> mVibrator.vibrate(100))
                .show();
    }

    private void checkBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            showBatteryOptimizationDialog();
        }
    }

    private void showBatteryOptimizationDialog() {
        new AlertDialog.Builder(mContext, R.style.DarkAlertDialog)
                .setTitle("배터리 최적화 예외 요청")
                .setMessage("이 앱이 백그라운드에서 안정적으로 동작하려면 배터리 최적화에서 제외해야 합니다. 설정 화면으로 이동하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> requestBatteryOptimizationException())
                .setNegativeButton("아니요", (dialog, which) ->
                        Toast.makeText(MainActivity.this, "배터리 최적화로 인해 앱이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show()
                )
                .setCancelable(false)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestBatteryOptimizationException() {
        try {
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

    private void updateRobotChangeButton(boolean connected) {
        if (btnRobotChange == null) return;
        isMqttConnected = connected; // 캐시 동기화

        int color = androidx.core.content.ContextCompat.getColor(
                this, connected ? R.color.mqtt_connected : R.color.mqtt_disconnected
        );

        try {
            if (btnRobotChange instanceof android.widget.ImageView) {
                ((android.widget.ImageView) btnRobotChange)
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            } else {
                btnRobotChange.setBackgroundColor(color);
            }

            if (btnRobotChange instanceof android.widget.TextView) {
                ((android.widget.TextView) btnRobotChange).setText(
                        connected ? "연결됨 • 누르면 해제" : "연결하기"
                );
            }
            btnRobotChange.setContentDescription(
                    connected ? "MQTT 연결됨, 누르면 연결 해제" : "MQTT 연결하기"
            );

        } catch (Exception e) {
            btnRobotChange.setBackgroundColor(color);
        }
    }

    private void showConnectConfirm() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("MQTT 연결")
                .setMessage("서버와 MQTT 연결을 하시겠습니까?")
                .setPositiveButton("예", (d, w) -> {
                    mVibrator.vibrate(80);
                    viewModel.startServiceForConnect();
                    Toast.makeText(this, "MQTT 연결 시도…", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("아니오", (d, w) -> mVibrator.vibrate(50))
                .show();
    }

    private void showDisconnectConfirm() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("MQTT 연결 중지")
                .setMessage("연결을 중지할까요?")
                .setPositiveButton("예", (d, w) -> {
                    mVibrator.vibrate(80);
                    viewModel.requestDisconnect();
                    Toast.makeText(this, "MQTT 연결 해제", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("아니오", (d, w) -> mVibrator.vibrate(50))
                .show();
    }
}
