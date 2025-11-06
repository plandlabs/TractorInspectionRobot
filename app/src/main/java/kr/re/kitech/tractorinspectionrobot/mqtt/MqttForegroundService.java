package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModelBridge;
import kr.re.kitech.tractorinspectionrobot.net.NetworkHelper;

import org.json.JSONObject;

import java.util.UUID;
/*
MqttService (조립·오케스트레이션)

서비스 수명주기 관리(onCreate/onDestroy).

각 컴포넌트 생성/연결: Notifier, NetworkHelper, MqttClientManager, StateLoops.

착/미착 이벤트를 받아 연결/해제 트리거만 호출.

설정값(브로커 URL, 디바이스 이름, 주기, INVERT_OFFBODY)을 한 곳에서 정의.
 */
public class MqttForegroundService extends Service {
    private SharedPreferences setting;

    private static final String TAG="MqttService";
    private static final String CHANNEL_ID="mqtt_fg"; private static final int NOTI_ID=1001;

    // 설정
    private String MQTT_URL;
    private String  DEVICE_NAME;
    private static final long PING_SEC = 15, STATE_SEC = 10;
    private static final boolean INVERT_OFFBODY = true;

    // 구성요소
    private Notifier notifier;
    private NetworkHelper net;
    public MqttClientManager mqtt;
    private StateLoops loops;
    public static final String ACTION_MQTT_STATUS = "kr.re.kitech.tractorinspectionrobot.MQTT_STATUS";
    public static final String ACTION_QUERY_STATUS = "kr.re.kitech.tractorinspectionrobot.MQTT_QUERY_STATUS"; // ← 추가
    public static final String EXTRA_STATUS = "status";   // "initializing" | "connected" | "reconnecting" | "disconnected" | "rejected" | "pong"
    public static final String EXTRA_CAUSE  = "cause";    // 오류 메시지 옵션

    // 마지막 상태 캐시
    private volatile String lastStatus = "initializing";
    private volatile String lastCause  = null;

    private void sendStatus(String status, @Nullable String cause) {
        lastStatus = status;               // ← 캐시 업데이트
        lastCause  = cause;

        Intent intent = new Intent(ACTION_MQTT_STATUS);
        intent.setPackage(getPackageName()); // 앱 내부로만
        intent.putExtra(EXTRA_STATUS, status);
        if (cause != null) intent.putExtra(EXTRA_CAUSE, cause);
        sendBroadcast(intent);
    }

    @Override public void onCreate() {
        super.onCreate();
        setting = getSharedPreferences("setting", 0);
        DEVICE_NAME = setting.getString("DEVICE_NAME", "");
        notifier = new Notifier(this, CHANNEL_ID, NOTI_ID);
        notifier.ensureChannel();
        startForeground(NOTI_ID, notifier.build("Initializing..."));
        // 초기 브로드캐스트 1회
        sendStatus("initializing", null);   // ← 추가

        net = new NetworkHelper(this);
        net.acquireWifiHighPerf();
        net.bindWifiNetwork();

        String clientId = DEVICE_NAME + "-" + UUID.randomUUID().toString().substring(0,8);
        MQTT_URL = getString(R.string.mqtt_connect_url);
        mqtt = new MqttClientManager(MQTT_URL, DEVICE_NAME, clientId);
        mqtt.setListener(new MqttClientManager.Listener() {
            @Override public void onConnected() {
                notifier.update("Connected");
                sendStatus("connected", null);
                mqtt.afterConnected();
            }
            @Override public void onDisconnected(Throwable cause) {
                notifier.update("Reconnecting...");
                sendStatus("disconnected", cause != null ? cause.getMessage() : null);
            }
            @Override public void onReject(String payload) {
                sendStatus("rejected", payload);
                stopSelf();
            }
            @Override public void onPong(String payload) {
                // 필요 시 pong도 캐시
                sendStatus("pong", null);
            }
            @Override public void onDirect(String sub, String payload) {
                try {
                    JSONObject msg = new JSONObject(payload);
                    SharedMqttViewModelBridge.getInstance().postDirectMessage(sub, payload);
                    if ("getBattery".equals(sub) && msg.has("__replyTo")) {
                        // 필요하면 배터리/상태 등 바로 응답
                        mqtt.publishJson(msg.getString("__replyTo"),
                                new JSONObject().put("ok", true).put("ts", System.currentTimeMillis()),
                                MqttQos.AT_MOST_ONCE,false);
                    }
                } catch (Exception ignore) {}
            }
        });
        mqtt.init();

        loops = new StateLoops(mqtt, DEVICE_NAME,
                (BatteryManager)getSystemService(BATTERY_SERVICE),
                (android.net.wifi.WifiManager)getSystemService(WIFI_SERVICE),
                PING_SEC, STATE_SEC);
        loops.start();
    }

    @Override public int onStartCommand(Intent i, int f, int id) {
        // 액티비티의 상태 질의 처리
        if (i != null && ACTION_QUERY_STATUS.equals(i.getAction())) {
            sendStatus(lastStatus, lastCause);   // ← 현재 캐시를 즉시 재브로드캐스트
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (loops != null) loops.stop();
        if (mqtt != null) mqtt.gracefulDisconnect();
        if (net != null) { net.releaseWifiHighPerf(); net.unbindWifiNetwork(); }
        notifier.update("Disconnected");
        sendStatus("disconnected", null);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}