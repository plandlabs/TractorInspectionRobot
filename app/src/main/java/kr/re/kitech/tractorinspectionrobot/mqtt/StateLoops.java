// com/plandlabs/nepesarkmonitwatch/mqtt/StateLoops.java
package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.util.Log;

import com.hivemq.client.mqtt.datatypes.MqttQos;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/*
StateLoops (주기 작업 스케줄러)

ScheduledExecutorService로 두 루프 관리:

ping/<name> (기본 15초)

devices/<name>/state (기본 10초): 배터리, Wi-Fi RSSI, 타임스탬프 수집 후 퍼블리시

MQTT 연결 상태만 체크하고, 예외는 삼켜서 루프가 죽지 않게 함.
*/
public class StateLoops {
    private static final String TAG = "StateLoops";
    private final ScheduledExecutorService sch = Executors.newScheduledThreadPool(2);
    private final MqttClientManager mqtt; private final String name;
    private final BatteryManager bm; private final WifiManager wifi;
    private final long PING_SEC; private final long STATE_SEC;

    public StateLoops(MqttClientManager mqtt, String name, BatteryManager bm, WifiManager wifi, long pingSec, long stateSec) {
        this.mqtt = mqtt; this.name = name; this.bm = bm; this.wifi = wifi; this.PING_SEC=pingSec; this.STATE_SEC=stateSec;
    }

    public void start() {

//        sch.scheduleAtFixedRate(() -> {
//            try {
//                if (mqtt.isConnected()) {
//                    int bat = (bm != null) ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
//                    int rssi = -127;
//                    try { WifiInfo info = wifi.getConnectionInfo(); rssi = (info!=null)? info.getRssi():-127; } catch (SecurityException ignore) {}
//                    JSONObject state = new JSONObject().put("name", name).put("battery", bat).put("rssi", rssi).put("ts", System.currentTimeMillis());
//                    mqtt.publishJson("devices/"+name+"/state", state, MqttQos.AT_MOST_ONCE,false);
//                }
//            } catch (Throwable t) { Log.w(TAG,"state loop: "+t.getMessage()); }
//        }, 2, STATE_SEC, TimeUnit.SECONDS);
    }
    public void stop(){ sch.shutdownNow(); }
}
