package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModelBridge;
import kr.re.kitech.tractorinspectionrobot.net.NetworkHelper;
import kr.re.kitech.tractorinspectionrobot.utils.StringConvUtil;

/**
 * MQTT Foreground Service
 * - onCreate: 초기화만 (자동 연결 금지)
 * - ACTION_CONNECT: 명시적 연결 (RobotListActivity 에서 넘어온 설정 있으면 그걸로 연결)
 * - ACTION_DISCONNECT: 명시적 해제 + userPaused=true
 * - ACTION_QUERY_STATUS: 현재 상태 재브로드캐스트
 */
public class MqttForegroundService extends Service {
    private static final String CHANNEL_ID = "mqtt_fg";
    private static final int NOTI_ID = 1001;

    // 설정
    private String MQTT_URL;
    private String DEVICE_NAME;

    // 구성요소
    private Notifier notifier;
    private NetworkHelper net;
    public MqttClientManager mqtt;

    // 브로드캐스트/액션
    public static final String ACTION_MQTT_STATUS   = "kr.re.kitech.tractorinspectionrobot.MQTT_STATUS";
    public static final String ACTION_QUERY_STATUS  = "kr.re.kitech.tractorinspectionrobot.MQTT_QUERY_STATUS";
    public static final String ACTION_CONNECT       = "kr.re.kitech.tractorinspectionrobot.MQTT_CONNECT";
    public static final String ACTION_DISCONNECT    = "kr.re.kitech.tractorinspectionrobot.MQTT_DISCONNECT";

    public static final String EXTRA_STATUS = "status"; // "initializing" | "connected" | "reconnecting" | "disconnected" | "rejected"
    public static final String EXTRA_CAUSE  = "cause";

    public static final String ACTION_PUBLISH     = "kr.re.kitech.tractorinspectionrobot.MQTT_PUBLISH";
    public static final String EXTRA_PUB_TOPIC    = "pub_topic";
    public static final String EXTRA_PUB_PAYLOAD  = "pub_payload";
    public static final String EXTRA_PUB_QOS      = "pub_qos";     // int 0/1/2 (선택)
    public static final String EXTRA_PUB_RETAIN   = "pub_retain";  // boolean (선택)

    public static final String ACTION_MQTT_MESSAGE = "kr.re.kitech.tractorinspectionrobot.MQTT_MESSAGE";
    public static final String EXTRA_TOPIC         = "topic";
    public static final String EXTRA_PAYLOAD       = "payload";

    // ✅ RobotListActivity 에서 넘겨줄 수 있는 Extra (선택)
    public static final String EXTRA_MQTT_HOST        = "extra_mqtt_host";
    public static final String EXTRA_MQTT_PORT        = "extra_mqtt_port";
    public static final String EXTRA_MQTT_ROOT_TOPIC  = "extra_mqtt_root_topic";
    public static final String EXTRA_MQTT_BASE_TOPIC  = "extra_mqtt_base_topic";
    public static final String EXTRA_MQTT_USERNAME    = "extra_mqtt_username";
    public static final String EXTRA_MQTT_PASSWORD    = "extra_mqtt_password";
    public static final String EXTRA_DEVICE_NAME      = "extra_device_name";

    // 상태 캐시
    private volatile String  lastStatus = "initializing";
    private volatile String  lastCause  = null;
    private volatile boolean isConnected = false;

    // 사용자 의도(끊었으면 자동재연결 금지)
    public static final String PREF            = "mqtt_pref";
    public static final String KEY_USER_PAUSED = "user_paused";
    public static final String KEY_ROOT_TOPIC  = "root_topic";
    public static final String KEY_BASE_TOPIC  = "base_topic";
    private boolean userPaused = false;

    // 연결 전/중단 시 publish를 안전하게 보관할 간단한 큐
    private static class PendingPub {
        final String topic; final String payload; final int qos; final boolean retain;
        PendingPub(String t, String p, int q, boolean r) { topic=t; payload=p; qos=q; retain=r; }
    }
    private final Queue<PendingPub> pendingQueue = new ArrayDeque<>();

    private String rootTopic;
    private String baseTopic;
    private String reqTopic;
    private String mqttUsername;
    private String mqttPassword;

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

        DEVICE_NAME = getString(R.string.controller_name);

        SharedPreferences sp = getSharedPreferences(PREF, MODE_PRIVATE);
        userPaused = sp.getBoolean(KEY_USER_PAUSED, false);

        notifier = new Notifier(this, CHANNEL_ID, NOTI_ID);
        notifier.ensureChannel();
        startForeground(NOTI_ID, notifier.build("Initializing..."));
        sendStatus("initializing", null);

        net = new NetworkHelper(this);
        net.acquireWifiHighPerf();
        net.bindWifiNetwork();

        // 기본 설정은 strings.xml 기준 (RobotListActivity 에서 안 넘겨줄 때 사용)
        MQTT_URL     = getString(R.string.mqtt_connect_url);
        rootTopic    = getString(R.string.mqtt_root_topic);
        baseTopic    = getString(R.string.mqtt_base_topic);
        mqttUsername = getString(R.string.mqtt_username);
        mqttPassword = getString(R.string.mqtt_password);
        reqTopic     = rootTopic + "/" + StringConvUtil.md5(baseTopic) + "/req";

        // ✅ 기본 토픽도 저장
        saveTopicConfig();

        // MQTT 클라이언트 초기화 (connect는 호출하지 않음)
        initMqttClient();
    }

    /**
     * 현재 필드 값(MQTT_URL, rootTopic, baseTopic, mqttUsername, mqttPassword, DEVICE_NAME)을 기반으로
     * MqttClientManager를 새로 생성하고 리스너/초기화까지 수행한다.
     */
    private void initMqttClient() {
        String clientId = DEVICE_NAME + "_ING_Client_" + UUID.randomUUID().toString().substring(0, 8);

        mqtt = new MqttClientManager(
                MQTT_URL,
                clientId,
                rootTopic,
                baseTopic,
                mqttUsername,
                mqttPassword
        );
        mqtt.setListener(new MqttClientManager.Listener() {
            @Override public void onConnected() {
                isConnected = true;
                notifier.update("Connected");
                sendStatus("connected", null);
                mqtt.afterConnected();
                flushPendingPublishes();
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

            @Override public void onMessage(String topic, String payload) {
                SharedMqttViewModelBridge.getInstance().postDirectMessage(topic, payload);

                Intent intent = new Intent(ACTION_MQTT_MESSAGE);   // "kr.re.kitech.tractorinspectionrobot.MQTT_MESSAGE"
                intent.setPackage(getPackageName());               // 앱 내부로만
                intent.putExtra(EXTRA_TOPIC, topic);               // "topic"
                intent.putExtra(EXTRA_PAYLOAD, payload);           // "payload"
                sendBroadcast(intent);
            }
        });

        mqtt.init();
        // ❌ 여기서 connect()는 호출하지 않는다. (ACTION_CONNECT에서만 명시적으로 호출)
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
                    getSharedPreferences(PREF, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_USER_PAUSED, false)
                            .apply();

                    // ✅ RobotListActivity 에서 넘어온 설정이 있으면 여기서 덮어쓴다.
                    String hostExtra       = i.getStringExtra(EXTRA_MQTT_HOST);
                    int    portExtra       = i.getIntExtra(EXTRA_MQTT_PORT, -1);
                    String rootExtra       = i.getStringExtra(EXTRA_MQTT_ROOT_TOPIC);
                    String baseExtra       = i.getStringExtra(EXTRA_MQTT_BASE_TOPIC);
                    String usernameExtra   = i.getStringExtra(EXTRA_MQTT_USERNAME);
                    String passwordExtra   = i.getStringExtra(EXTRA_MQTT_PASSWORD);
                    String deviceNameExtra = i.getStringExtra(EXTRA_DEVICE_NAME);

                    boolean hasHostConfig = (hostExtra != null && !hostExtra.isEmpty() && portExtra > 0);

                    if (deviceNameExtra != null && !deviceNameExtra.isEmpty()) {
                        DEVICE_NAME = deviceNameExtra;
                    }

                    if (hasHostConfig) {
                        // RobotListActivity에서 보낸 정보로 MQTT 설정 변경
                        String host = hostExtra;
                        int    port = portExtra;

                        MQTT_URL = "tcp://" + host + ":" + port;

                        if (rootExtra != null && !rootExtra.isEmpty()) {
                            rootTopic = rootExtra;
                        }
                        if (baseExtra != null && !baseExtra.isEmpty()) {
                            // 🔥 baseExtra → MD5 해시로 변환하여 baseTopic 설정
                            baseTopic = baseExtra;
                        }
                        if (usernameExtra != null && !usernameExtra.isEmpty()) {
                            mqttUsername = usernameExtra;
                        }
                        if (passwordExtra != null) { // 비밀번호는 빈 문자열도 유효할 수 있음
                            mqttPassword = passwordExtra;
                        }

                        reqTopic = rootTopic + "/" + StringConvUtil.md5(baseTopic) + "/req";

                        // ✅ 최종 토픽 설정 저장
                        saveTopicConfig();

                        // 기존 mqtt가 있으면 정리하고 새로 만든다.
                        if (mqtt != null) {
                            mqtt.gracefulDisconnect();
                            isConnected = false;
                        }
                        initMqttClient();
                    }

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
                    getSharedPreferences(PREF, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_USER_PAUSED, true)
                            .apply();

                    if (mqtt != null) mqtt.gracefulDisconnect();
                    isConnected = false;
                    notifier.update("Disconnected");
                    sendStatus("disconnected", null);
                    stopSelf();
                    break;
                }

                // 외부에서 들어온 publish 요청 처리
                case ACTION_PUBLISH: {
                    String topic   = i.getStringExtra(EXTRA_PUB_TOPIC);
                    String payload = i.getStringExtra(EXTRA_PUB_PAYLOAD);
                    int    qos     = i.getIntExtra(EXTRA_PUB_QOS, 1);       // 기본 QoS1
                    boolean retain = i.getBooleanExtra(EXTRA_PUB_RETAIN, false);

                    if (topic != null && payload != null) {
                        handlePublish(topic, payload, qos, retain);
                    }
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

    private void handlePublish(String topic, String payload, int qos, boolean retain) {
        // null 방지
        topic = Objects.toString(topic, "");
        payload = Objects.toString(payload, "");
        if (topic.isEmpty()) return;

        if (mqtt != null && mqtt.isConnected()) {
            mqtt.publish(topic, payload, qos, retain);
        } else {
            // 아직 연결 전이라면 보관
            pendingQueue.offer(new PendingPub(topic, payload, qos, retain));
        }
    }

    private void flushPendingPublishes() {
        if (mqtt == null || !mqtt.isConnected()) return;
        PendingPub p;
        while ((p = pendingQueue.poll()) != null) {
            mqtt.publish(p.topic, p.payload, p.qos, p.retain);
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (mqtt != null) mqtt.gracefulDisconnect();
        if (net != null) {
            net.releaseWifiHighPerf();
            net.unbindWifiNetwork();
        }
        isConnected = false;
        notifier.update("Disconnected");
        sendStatus("disconnected", null);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    // ==========================
    // 유틸: 토픽 설정 저장
    // ==========================
    private void saveTopicConfig() {
        SharedPreferences sp = getSharedPreferences(PREF, MODE_PRIVATE);
        sp.edit()
                .putString(KEY_ROOT_TOPIC, rootTopic)
                .putString(KEY_BASE_TOPIC, baseTopic)
                .apply();
    }
}
