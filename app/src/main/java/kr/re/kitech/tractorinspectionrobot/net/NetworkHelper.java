// com/plandlabs/nepesarkmonitwatch/net/NetworkHelper.java
package kr.re.kitech.tractorinspectionrobot.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
/*
NetworkHelper (네트워크 품질 고정)

WifiLock(WIFI_MODE_FULL_HIGH_PERF) 획득/해제 → 절전 중에도 Wi-Fi 유지.

ConnectivityManager.requestNetwork() + bindProcessToNetwork()로 프로세스 트래픽을 Wi-Fi에 고정.

서비스 종료 시 콜백 해제/언바인딩.
*/
public class NetworkHelper {
    private static final String TAG = "NetworkHelper";
    private final Context ctx;
    private final WifiManager wifi;
    private WifiManager.WifiLock wifiLock;
    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback cb;

    public NetworkHelper(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.wifi = (WifiManager)this.ctx.getSystemService(Context.WIFI_SERVICE);
    }

    public void acquireWifiHighPerf() {
        if (wifiLock == null || !wifiLock.isHeld()) {
            wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "watch-mqtt");
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
            Log.i(TAG, "WifiLock acquired");
        }
    }
    public void releaseWifiHighPerf() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release(); Log.i(TAG, "WifiLock released");
        }
    }

    public void bindWifiNetwork() {
        cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        cb = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network n) {
                if (Build.VERSION.SDK_INT >= 23) cm.bindProcessToNetwork(n);
                Log.i(TAG, "Wi-Fi available & bound");
            }
            @Override public void onLost(Network n) {
                if (Build.VERSION.SDK_INT >= 23) cm.bindProcessToNetwork(null);
                Log.w(TAG, "Wi-Fi lost");
            }
        };
        cm.requestNetwork(req, cb);
    }

    public void unbindWifiNetwork() {
        if (cm != null && cb != null) { try { cm.unregisterNetworkCallback(cb); } catch (Exception ignore) {} }
        if (Build.VERSION.SDK_INT >= 23 && cm != null) cm.bindProcessToNetwork(null);
    }
}
