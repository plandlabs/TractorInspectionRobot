package kr.re.kitech.tractorinspectionrobot.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import kr.re.kitech.tractorinspectionrobot.R;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NetworkUtil {
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int TYPE_NOT_CONNECTED = 3;

    // 기본 게이트웨이 주소를 가져오는 함수
    public static String getGatewayIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            int gateway = dhcpInfo.gateway;

            // 게이트웨이 IP 주소를 정수에서 변환하여 반환
            return intToInetAddress(gateway).getHostAddress();
        }
        return null;
    }
    public static String getInternalIpAddress(Context context) {
        SharedPreferences setting = context.getSharedPreferences("setting", 0);
        if(setting.getString("SOCKET_IP", "").isEmpty()) {
            return context.getString(R.string.fixed_internal_ip);
        }else{
            return setting.getString("SOCKET_IP", "");
        }
    }
    public static String getInternalSocketPort(Context context) {
        return context.getString(R.string.fixed_socket_port);
    }

    // 내부 IP 주소를 가져오는 함수
    public static String getDeviceIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();

            // IP 주소를 정수에서 변환하여 반환
            return intToInetAddress(ipAddress).getHostAddress();
        }
        return null;
    }

    // 정수형 IP 주소를 InetAddress 객체로 변환하는 함수
    private static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(hostAddress).array();
        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String getConnectedWifiBSSID(Context context) {
        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }

        // WifiManager 가져오기
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return null;
        }

        // 현재 연결된 Wi-Fi 정보 가져오기
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return null;
        }

        String bssid = wifiInfo.getBSSID(); // BSSID는 공유기의 MAC 주소입니다.
        return bssid;
    }
    public static String getConnectedWifiSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        // WiFi 상태 확인
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID(); // 연결된 Wi-Fi의 SSID 가져오기

                if (ssid != null) {
                    // SSID는 따옴표로 감싸져 있을 수 있으므로 이를 제거
                    if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    return ssid;
                } else {
                    Log.e("WifiUtils", "SSID is null. Maybe not connected to a Wi-Fi network.");
                }
            }
        } else {
            Log.e("WifiUtils", "Wi-Fi is disabled or WifiManager is null.");
        }
        return null;
    }

    public static int getConnectivityStatus(Context context){ //해당 context의 서비스를 사용하기위해서 context객체를 받는다.
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if(networkInfo != null){
            int type = networkInfo.getType();
            if(type == ConnectivityManager.TYPE_MOBILE){//쓰리지나 LTE로 연결된것(모바일을 뜻한다.)
                return TYPE_MOBILE;
            }else if(type == ConnectivityManager.TYPE_WIFI){//와이파이 연결된것
                return TYPE_WIFI;
            }
        }
        return TYPE_NOT_CONNECTED;  //연결이 되지않은 상태
    }
}
