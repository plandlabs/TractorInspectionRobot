package kr.re.kitech.tractorinspectionrobot.wifi;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class WifiScanService extends Service {

    private final IBinder binder = new LocalBinder();
    private WifiManager wifiManager;
    private final MutableLiveData<List<ScanResult>> wifiScanResultsLiveData = new MutableLiveData<>();
    private final Handler scanHandler = new Handler();
    private List<ScanResult> lastScanResults;

    private WifiScanReceiver mWifiScanReceiver;

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (wifiManager != null) {
                boolean success = wifiManager.startScan();
                if (!success) {
                    Log.w("WifiScanService", "startScan() failed");
                }
                // 다음 스캔은 10초 후
                scanHandler.postDelayed(this, 5_000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            scanHandler.post(scanRunnable);
        }
        mWifiScanReceiver = new WifiScanReceiver();
        registerReceiver(mWifiScanReceiver, new android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public class LocalBinder extends Binder {
        public WifiScanService getService() {
            return WifiScanService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanHandler.removeCallbacks(scanRunnable);
        if(mWifiScanReceiver != null) {
            unregisterReceiver(mWifiScanReceiver);
        }
    }

    public MutableLiveData<List<ScanResult>> getWifiScanResultsLiveData() {
        return wifiScanResultsLiveData;
    }

    public List<ScanResult> getLastScanResults() {
        return lastScanResults != null ? lastScanResults : List.of();
    }

    public void updateScanResults(List<ScanResult> results) {
        this.lastScanResults = results;
        wifiScanResultsLiveData.postValue(results);
    }

    // 내부 BroadcastReceiver
    private class WifiScanReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (wifiManager != null) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                updateScanResults(results);
                Log.d("WifiScanService", "스캔 결과 업데이트됨. count: " + results.size());
            }
        }
    }
}



