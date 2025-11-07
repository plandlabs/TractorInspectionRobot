package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.UUID;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModelBridge;
import kr.re.kitech.tractorinspectionrobot.net.NetworkHelper;

/**
 * MQTT Foreground Service
 * - onCreate: 초기화만 (자동 연결 금지)
 * - ACTION_CONNECT: 명시적 연결
 * - ACTION_DISCONNECT: 명시적 해제 + userPaused=true
 * - ACTION_QUERY_STATUS: 현재 상태 재브로드캐스트
 */
public class MqttForegroundService extends Service {
    private SharedPreferences setting;

    private static final String TAG = "MqttService";
    private static final String CHANNEL_ID = "mqtt_fg";
    private static final int NOTI_ID = 1001;

    // 설정
    private String MQTT_URL;
    private String DEVICE_NAME;
    private static final long PING_SEC = 15, STATE_SEC = 10;

    // 구성요소
    private Notifier notifier;
    private NetworkHelper net;
    public  MqttClientManager mqtt;
    private StateLoops loops;

    // 브로드캐스트/액션
    public static final String ACTION_MQTT_STATUS   = "kr.re.kitech.tractorinspectionrobot.MQTT_STATUS";
    public static final String ACTION_QUERY_STATUS  = "kr.re.kitech.tractorinspectionrobot.MQTT_QUERY_STATUS";
    public static final String ACTION_CONNECT       = "kr.re.kitech.tractorinspectionrobot.MQTT_CONNECT";
    public static final String ACTION_DISCONNECT    = "kr.re.kitech.tractorinspectionrobot.MQTT_DISCONNECT";

    public static final String EXTRA_STATUS = "status"; // "initializing" | "connected" | "reconnecting" | "disconnected" | "rejected"
    public static final String EXTRA_CAUSE  = "cause";

    // 상태 캐시
    private volatile String  lastStatus = "initializing";
    private volatile String  lastCause  = null;
    private volatile boolean isConnected = false;

    // 사용자 의도(끊었으면 자동재연결 금지)
    private static final String PREF = "mqtt_pref";
    private static final String KEY_USER_PAUSED = "user_paused";
    private boolean userPaused = false;

    private void sendStatus(String status, @Nullable String cause) {
        lastStatus = status;
        lastCause  = cause;
        Intent intent = new Intent(ACTION_MQTT_STATUS);
        intent.setPackage(getPackageName()); // 앱 내부로만
        intent.putExtra(EXTRA_STATUS, status);
        if (cause != null) intent.putExtra(EXTRA_CAUSE, cause);
        sendBroadcast(intent);
    }

    @Override public void onCreate() {
        super.onCreate();

        // setting = getSharedPreferences("setting", 0);
        // DEVICE_NAME = setting.getString("DEVICE_NAME", "tester");
        DEVICE_NAME = "tester";

        SharedPreferences sp = getSharedPreferences(PREF, MODE_PRIVATE);
        userPaused = sp.getBoolean(KEY_USER_PAUSED, false);

        notifier = new Notifier(this, CHANNEL_ID, NOTI_ID);
        notifier.ensureChannel();
        startForeground(NOTI_ID, notifier.build("Initializing..."));
        sendStatus("initializing", null);

        net = new NetworkHelper(this);
        net.acquireWifiHighPerf();
        net.bindWifiNetwork();

        String clientId = DEVICE_NAME + "-" + UUID.randomUUID().toString().substring(0, 8);
        MQTT_URL = getString(R.string.mqtt_connect_url);

        mqtt = new MqttClientManager(MQTT_URL, DEVICE_NAME, clientId);
        mqtt.setListener(new MqttClientManager.Listener() {
            @Override public void onConnected() {
                isConnected = true;
                notifier.update("Connected");
                sendStatus("connected", null);
                mqtt.afterConnected();

                // 연결 성공 후 루프 시작
                if (loops == null) {
                    loops = new StateLoops(
                            mqtt,
                            DEVICE_NAME,
                            (BatteryManager) getSystemService(BATTERY_SERVICE),
                            (android.net.wifi.WifiManager) getSystemService(WIFI_SERVICE),
                            PING_SEC, STATE_SEC
                    );
                }
                loops.start();
            }

            @Override public void onDisconnected(Throwable cause) {
                isConnected = false;
                notifier.update("Reconnecting...");
                // 사용자가 끊었다면 재연결 알림/시도 금지
                if (userPaused) {
                    sendStatus("disconnected", null);
                    return;
                }
                sendStatus("reconnecting", (cause != null ? cause.getMessage() : null));
            }

            @Override public void onReject(String payload) {
                isConnected = false;
                sendStatus("rejected", payload);
                stopSelf();
            }

            @Override public void onDirect(String topicOrSub, String payload) {
                SharedMqttViewModelBridge.getInstance().postDirectMessage(topicOrSub, payload);
            }
        });

        mqtt.init();
        // ❌ 자동 연결 금지: mqtt.connect() 호출하지 않음
        // ❌ 루프도 연결 성공 후에만 시작
    }

    @Override public int onStartCommand(Intent i, int f, int id) {
        if (i != null && i.getAction() != null) {
            switch (i.getAction()) {
                case ACTION_QUERY_STATUS: {
                    // userPaused면 무조건 disconnected를 돌려 UI가 재연결로 오해하지 않도록
                    String status = isConnected ? "connected" : (userPaused ? "disconnected" : lastStatus);
                    sendStatus(status, lastCause);
                    break;
                }

                case ACTION_CONNECT: {
                    userPaused = false;
                    getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean(KEY_USER_PAUSED, false).apply();

                    if (mqtt != null && !mqtt.isConnected()) {
                        sendStatus("reconnecting", null);
                        mqtt.connect(); // 명시적 연결 지점
                    } else {
                        sendStatus(isConnected ? "connected" : "disconnected", lastCause);
                    }
                    break;
                }

                case ACTION_DISCONNECT: {
                    userPaused = true;
                    getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean(KEY_USER_PAUSED, true).apply();

                    if (loops != null) loops.stop();
                    if (mqtt != null) mqtt.gracefulDisconnect();
                    isConnected = false;
                    notifier.update("Disconnected");
                    sendStatus("disconnected", null);
                    stopSelf();
                    break;
                }

                default:
                    // 기본 분기에서는 아무 것도 하지 않음 (자동 재연결 금지)
                    break;
            }
        }
        // 사용자가 끊은 뒤 OS가 임의로 재시작해도 자동 복구되지 않게
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (loops != null) loops.stop();
        if (mqtt != null) mqtt.gracefulDisconnect();
        if (net != null) { net.releaseWifiHighPerf(); net.unbindWifiNetwork(); }
        isConnected = false;
        notifier.update("Disconnected");
        sendStatus("disconnected", null);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
