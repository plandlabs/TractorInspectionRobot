package kr.re.kitech.tractorinspectionrobot.wifi;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class WifiScanServiceOld extends Service {

    private WifiManager wifiManager;
    private final IBinder binder = new LocalBinder();
    private WifiScanObservable wifiScanObservable;
    private Handler handler;
    private Runnable wifiScanTask;
    private int scanRetryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;  // 최대 재시도 횟수
    private static final long SCAN_INTERVAL = 30 * 1000; // 30초 간격
    private static final long SCAN_DELAY = 1000; // 최초 1초 후 스캔

    @Override
    public void onCreate() {
        super.onCreate();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanObservable = new WifiScanObservable();
        handler = new Handler();

        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        // 스캔 타이머 설정: 최초 3초 후, 10초마다 실행
        wifiScanTask = new Runnable() {
            @Override
            public void run() {
                startWifiScan();
                handler.postDelayed(this, SCAN_INTERVAL); // 30초 주기 스캔
            }
        };
        handler.postDelayed(wifiScanTask, SCAN_DELAY); // 최초 1초 후 스캔
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        // Wi-Fi 스캔 시작
//        startWifiScan();
//        return START_STICKY;
//    }

    private void startWifiScan() {
        boolean success = wifiManager.startScan();
        if (!success) {
            Log.e("WifiScanService", "Wi-Fi 스캔 실패. 재시도 중...");
//            handleScanFailure();  // 스캔 실패 시 재시도 처리
        } else {
            scanRetryCount = 0;  // 성공 시 재시도 카운트 초기화
        }
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanSuccess();
            } else {
                scanFailure();
            }
        }
    };

    private void scanSuccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        wifiScanObservable.setScanResults(results);  // 옵저버에게 알림
        Log.d("WifiScanService", "Wi-Fi 스캔 성공. 결과 전달.");
    }

    private void scanFailure() {
        Log.e("WifiScanService", "Wi-Fi 스캔 실패");
        handleScanFailure();
    }

    // 스캔 실패 처리 (최대 3번 재시도)
    private void handleScanFailure() {
        if (scanRetryCount < MAX_RETRY_COUNT) {
            scanRetryCount++;
            Log.d("WifiScanService", "스캔 실패. " + scanRetryCount + "번째 재시도 예정.");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startWifiScan();  // 3초 후 재시도
                }
            }, 3000);  // 3초 후 재시도
        } else {
            Log.e("WifiScanService", "Wi-Fi 스캔 3회 재시도 실패. 다음 주기까지 대기.");
            scanRetryCount = 0;  // 재시도 횟수 초기화
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiScanReceiver);
        handler.removeCallbacks(wifiScanTask); // 주기적 스캔 중지
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public WifiScanServiceOld getService() {
            return WifiScanServiceOld.this;
        }
    }

    public WifiScanObservable getWifiScanObservable() {
        return wifiScanObservable;
    }

    // Observable을 사용해 Wi-Fi 스캔 결과를 전달
    public class WifiScanObservable extends Observable {
        private List<ScanResult> scanResults = new ArrayList<>();

        public List<ScanResult> getScanResults() {
            return scanResults;
        }

        public void setScanResults(List<ScanResult> results) {
            this.scanResults = results;
            setChanged();  // 데이터 변경
            notifyObservers(results);  // 옵저버에게 알림
        }
    }
}
