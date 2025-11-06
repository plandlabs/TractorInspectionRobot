package kr.re.kitech.tractorinspectionrobot.wifi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.function.Consumer;

public class WifiConnector {
    private final WifiManager wifiManager;
    private final Context context;
    private final ConnectivityManager connectivityManager;

    public WifiConnector(Context context) {
        // WifiManager 초기화
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);;
    }

    public boolean connectToRegisteredSSID(String ssid) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false;
        }
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configuredNetworks) {
            if (config.SSID != null && config.SSID.equals("\"" + ssid + "\"")) { // SSID는 따옴표로 감싸야 함
                wifiManager.disconnect();
                wifiManager.enableNetwork(config.networkId, true);
                wifiManager.reconnect();
                Toast.makeText(context, "Connecting SSID " + ssid + " ...", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        Toast.makeText(context, "SSID not found or not registered", Toast.LENGTH_SHORT).show();
        return false;
    }

    // Wi-Fi 연결 메서드
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void connectWithNetworkSpecifier(String ssid, @Nullable String password, Consumer<Boolean> callback) {
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid);

        if (password != null && !password.isEmpty()) {
            builder.setWpa2Passphrase(password);
        }

        WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                connectivityManager.bindProcessToNetwork(network); // 앱에서 이 네트워크 사용
                callback.accept(true);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                callback.accept(false);
            }
        };

        connectivityManager.requestNetwork(request, networkCallback);
    }
}
