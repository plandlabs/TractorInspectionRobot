package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModelBridge;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;
import kr.re.kitech.tractorinspectionrobot.net.NetworkHelper;
import kr.re.kitech.tractorinspectionrobot.utils.StringConvUtil;

public class MqttForegroundService extends Service {
    private static final String TAG = "MqttForegroundService";

    private static final String CHANNEL_ID = "mqtt_fg";
    private static final int NOTI_ID = 1001;

    // 설정
    private String MQTT_URL;
    private String DEVICE_NAME;

    // 구성요소
    private Notifier notifier;
    private NetworkHelper net;
    public MqttClientManager mqtt;

    // 브로드캐스트/액션 (MQTT 연결 상태)
    public static final String ACTION_MQTT_STATUS   = "kr.re.kitech.tractorinspectionrobot.MQTT_STATUS";
    public static final String ACTION_QUERY_STATUS  = "kr.re.kitech.tractorinspectionrobot.MQTT_QUERY_STATUS";
    public static final String ACTION_CONNECT       = "kr.re.kitech.tractorinspectionrobot.MQTT_CONNECT";
    public static final String ACTION_DISCONNECT    = "kr.re.kitech.tractorinspectionrobot.MQTT_DISCONNECT";

    public static final String EXTRA_STATUS = "status"; // "initializing" | "connected" | "reconnecting" | "disconnected" | "rejected"
    public static final String EXTRA_CAUSE  = "cause";

    // publish / message
    public static final String ACTION_PUBLISH     = "kr.re.kitech.tractorinspectionrobot.MQTT_PUBLISH";
    public static final String EXTRA_PUB_TOPIC    = "pub_topic";
    public static final String EXTRA_PUB_PAYLOAD  = "pub_payload";
    public static final String EXTRA_PUB_QOS      = "pub_qos";     // int 0/1/2 (선택)
    public static final String EXTRA_PUB_RETAIN   = "pub_retain";  // boolean (선택)

    public static final String ACTION_MQTT_MESSAGE = "kr.re.kitech.tractorinspectionrobot.MQTT_MESSAGE";
    public static final String EXTRA_TOPIC         = "topic";
    public static final String EXTRA_PAYLOAD       = "payload";

    // ✅ 프로그램 실행 관련 액션
    public static final String ACTION_PROGRAM_START   = "kr.re.kitech.tractorinspectionrobot.MQTT_PROGRAM_START";
    public static final String ACTION_PROGRAM_STOP    = "kr.re.kitech.tractorinspectionrobot.MQTT_PROGRAM_STOP";

    // UI/MonitProgram에게 프로그램 진행상황 알림용
    public static final String ACTION_PROGRAM_PROGRESS = "kr.re.kitech.tractorinspectionrobot.MQTT_PROGRAM_PROGRESS";
    public static final String EXTRA_PROGRAM_RUNNING   = "program_running";
    public static final String EXTRA_PROGRAM_INDEX     = "program_index";   // 0-based
    public static final String EXTRA_PROGRAM_TOTAL     = "program_total";
    public static final String EXTRA_PROGRAM_PHASE     = "program_phase";

    public static final String EXTRA_PROGRAM_JSON     = "program_json";
    public static final String EXTRA_INTERVAL_SECOND  = "interval_second";

    // 프로그램 포즈 브로드캐스트
    public static final String ACTION_PROGRAM_POSE = "kr.re.kitech.tractorinspectionrobot.MQTT_PROGRAM_POSE";
    public static final String EXTRA_POSE_JSON     = "pose_json";

    // RobotListActivity 에서 넘겨줄 수 있는 Extra (선택)
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
    private String staTopic;
    private String mqttUsername;
    private String mqttPassword;

    // ==========================
    // Notifier 텍스트 관리
    // ==========================
    private String mqttStatusLabel   = "Initializing...";
    private String programStatusLabel = "";
    private final int rollbackTargetStep = 10; // 0 ~ rollbackTargetStep 안에 들어오면

    private void updateNotification() {
        String text = mqttStatusLabel;
        if (programStatusLabel != null && !programStatusLabel.isEmpty()) {
            text = mqttStatusLabel + " · " + programStatusLabel;
        }
        notifier.update(text);
    }

    // ==========================
    // 프로그램 상태머신 (분리된 클래스)
    // ==========================
    private ProgramStateMachine programStateMachine;

    // ==========================

    private void sendStatus(String status, @Nullable String cause) {
        lastStatus = status;
        lastCause  = cause;
        Intent intent = new Intent(ACTION_MQTT_STATUS);
        intent.setPackage(getPackageName()); // 앱 내부로만
        intent.putExtra(EXTRA_STATUS, status);
        if (cause != null) intent.putExtra(EXTRA_CAUSE, cause);
        sendBroadcast(intent);
    }

    private void broadcastProgramPose(RobotState s) {
        try {
            JSONObject j = s.toJson();
            Intent intent = new Intent(ACTION_PROGRAM_POSE);
            intent.setPackage(getPackageName());
            intent.putExtra(EXTRA_POSE_JSON, j.toString());
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "broadcastProgramPose error", e);
        }
    }

    @Override public void onCreate() {
        super.onCreate();

        DEVICE_NAME = getString(R.string.controller_name);

        SharedPreferences sp = getSharedPreferences(PREF, MODE_PRIVATE);
        userPaused = sp.getBoolean(KEY_USER_PAUSED, false);

        notifier = new Notifier(this, CHANNEL_ID, NOTI_ID);
        notifier.ensureChannel();

        // 초기 알림
        Notification noti = notifier.build(mqttStatusLabel);
        startForeground(NOTI_ID, noti);
        sendStatus("initializing", null);

        net = new NetworkHelper(this);
        net.acquireWifiHighPerf();
        net.bindWifiNetwork();

        // 기본 설정은 strings.xml 기준
        MQTT_URL     = getString(R.string.mqtt_connect_url);
        rootTopic    = getString(R.string.mqtt_root_topic);
        baseTopic    = getString(R.string.mqtt_base_topic);
        mqttUsername = getString(R.string.mqtt_username);
        mqttPassword = getString(R.string.mqtt_password);
        reqTopic     = rootTopic + "/" + StringConvUtil.md5(baseTopic) + "/req";
        staTopic     = rootTopic + "/" + StringConvUtil.md5(baseTopic) + "/sta";

        saveTopicConfig();

        // 프로그램 상태머신 생성
        programStateMachine = new ProgramStateMachine(
                new Handler(Looper.getMainLooper()),
                baseTopic,
                reqTopic,
                rollbackTargetStep,
                new ProgramStateMachine.Callback() {
                    @Override
                    public void publish(String topic, String payload, int qos, boolean retain) {
                        // 상태머신에서 보내는 명령도 기존 publish 경로 사용
                        handlePublish(topic, payload, qos, retain);
                    }

                    @Override
                    public void onProgramStatusLabelChanged(String label) {
                        programStatusLabel = label;
                        updateNotification();
                    }

                    @Override
                    public void onProgramProgressChanged(boolean running, int index, int total, int phase) {
                        Intent intent = new Intent(ACTION_PROGRAM_PROGRESS);
                        intent.setPackage(getPackageName());
                        intent.putExtra(EXTRA_PROGRAM_RUNNING, running);
                        intent.putExtra(EXTRA_PROGRAM_INDEX, index);
                        intent.putExtra(EXTRA_PROGRAM_TOTAL, total);
                        intent.putExtra(EXTRA_PROGRAM_PHASE, phase);
                        sendBroadcast(intent);
                    }

                    @Override
                    public void onProgramPose(RobotState pose) {
                        broadcastProgramPose(pose);
                    }
                }
        );

        initMqttClient();
    }

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
            @Override
            public void onConnected() {
                isConnected = true;
                lastStatus = "connected";
                lastCause  = null;

                mqttStatusLabel = "Connected";
                updateNotification();

                sendStatus("connected", null);

                mqtt.afterConnected();
                flushPendingPublishes();
            }

            @Override
            public void onDisconnected(Throwable cause) {
                isConnected = false;
                lastCause  = (cause != null ? cause.getMessage() : null);

                // 사용자가 명시적으로 끊은 경우 → 자동 재연결 UI/상태 안 띄움
                if (userPaused) {
                    lastStatus = "disconnected";
                    mqttStatusLabel = "Disconnected";
                    updateNotification();
                    sendStatus("disconnected", null);
                    return;
                }

                // 서버 다운 / 네트워크 문제 등
                // HiveMQ automaticReconnect()가 내부에서 재연결 시도하므로
                // 여기서는 상태/알림만 "Reconnecting..." 으로 두면 됨.
                lastStatus = "reconnecting";
                mqttStatusLabel = "Reconnecting...";
                updateNotification();
                sendStatus("reconnecting", lastCause);
            }

            @Override
            public void onMessage(String topic, String payload) {
                // 기존 ViewModel 브리지 + 브로드캐스트
                SharedMqttViewModelBridge
                        .getInstance()
                        .postDirectMessage(topic, payload);

                Intent intent = new Intent(ACTION_MQTT_MESSAGE);
                intent.setPackage(getPackageName());
                intent.putExtra(EXTRA_TOPIC, topic);
                intent.putExtra(EXTRA_PAYLOAD, payload);
                sendBroadcast(intent);

                // 프로그램 실행용 STA 처리
                handleProgramStaIfNeeded(topic, payload);
            }
        });

        mqtt.init();
    }

    @Override public int onStartCommand(Intent i, int f, int id) {
        if (i != null && i.getAction() != null) {
            switch (i.getAction()) {
                case ACTION_QUERY_STATUS: {
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
                        String host = hostExtra;
                        int    port = portExtra;

                        MQTT_URL = "tcp://" + host + ":" + port;

                        if (rootExtra != null && !rootExtra.isEmpty()) {
                            rootTopic = rootExtra;
                        }
                        if (baseExtra != null && !baseExtra.isEmpty()) {
                            baseTopic = baseExtra;
                        }
                        if (usernameExtra != null && !usernameExtra.isEmpty()) {
                            mqttUsername = usernameExtra;
                        }
                        if (passwordExtra != null) {
                            mqttPassword = passwordExtra;
                        }

                        reqTopic = rootTopic + "/" + StringConvUtil.md5(baseTopic) + "/req";
                        staTopic = rootTopic + "/" + StringConvUtil.md5(baseTopic) + "/sta";

                        saveTopicConfig();

                        if (mqtt != null) {
                            mqtt.gracefulDisconnect();
                            isConnected = false;
                        }

                        // 토픽/베이스 변경 시 프로그램 상태머신도 다시 만들어 주는 게 안전
                        programStateMachine = new ProgramStateMachine(
                                new Handler(Looper.getMainLooper()),
                                baseTopic,
                                reqTopic,
                                rollbackTargetStep,
                                new ProgramStateMachine.Callback() {
                                    @Override
                                    public void publish(String topic, String payload, int qos, boolean retain) {
                                        handlePublish(topic, payload, qos, retain);
                                    }

                                    @Override
                                    public void onProgramStatusLabelChanged(String label) {
                                        programStatusLabel = label;
                                        updateNotification();
                                    }

                                    @Override
                                    public void onProgramProgressChanged(boolean running, int index, int total, int phase) {
                                        Intent intent = new Intent(ACTION_PROGRAM_PROGRESS);
                                        intent.setPackage(getPackageName());
                                        intent.putExtra(EXTRA_PROGRAM_RUNNING, running);
                                        intent.putExtra(EXTRA_PROGRAM_INDEX, index);
                                        intent.putExtra(EXTRA_PROGRAM_TOTAL, total);
                                        intent.putExtra(EXTRA_PROGRAM_PHASE, phase);
                                        sendBroadcast(intent);
                                    }

                                    @Override
                                    public void onProgramPose(RobotState pose) {
                                        broadcastProgramPose(pose);
                                    }
                                }
                        );

                        initMqttClient();
                    }

                    if (mqtt != null && !mqtt.isConnected()) {
                        lastStatus = "reconnecting";
                        mqttStatusLabel = "Reconnecting...";
                        updateNotification();
                        sendStatus("reconnecting", null);
                        mqtt.connect();
                    } else {
                        mqttStatusLabel = isConnected ? "Connected" : "Disconnected";
                        updateNotification();
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

                    if (programStateMachine != null) {
                        programStateMachine.stopProgram();
                    }

                    if (mqtt != null) mqtt.gracefulDisconnect();
                    isConnected = false;
                    lastStatus = "disconnected";
                    mqttStatusLabel = "Disconnected";
                    updateNotification();
                    sendStatus("disconnected", null);
                    stopSelf();
                    break;
                }

                case ACTION_PUBLISH: {
                    String topic   = i.getStringExtra(EXTRA_PUB_TOPIC);
                    String payload = i.getStringExtra(EXTRA_PUB_PAYLOAD);
                    int    qos     = i.getIntExtra(EXTRA_PUB_QOS, 1);
                    boolean retain = i.getBooleanExtra(EXTRA_PUB_RETAIN, false);

                    if (topic != null && payload != null) {
                        handlePublish(topic, payload, qos, retain);
                    }
                    break;
                }

                // ✅ 프로그램 시작
                case ACTION_PROGRAM_START: {
                    String programJson = i.getStringExtra(EXTRA_PROGRAM_JSON);
                    int intervalSec = i.getIntExtra(EXTRA_INTERVAL_SECOND, 10);
                    if (programStateMachine != null) {
                        programStateMachine.startProgram(programJson, intervalSec);
                    }
                    break;
                }

                // ✅ 프로그램 정지
                case ACTION_PROGRAM_STOP: {
                    if (programStateMachine != null) {
                        programStateMachine.stopProgram();
                    }
                    break;
                }

                default:
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    private void handlePublish(String topic, String payload, int qos, boolean retain) {
        topic = Objects.toString(topic, "");
        payload = Objects.toString(payload, "");
        if (topic.isEmpty()) return;

        if (mqtt != null && mqtt.isConnected()) {
            mqtt.publish(topic, payload, qos, retain);
        } else {
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

    // STA 토픽만 골라서 상태머신에 전달
    private void handleProgramStaIfNeeded(String topic, String payload) {
        if (programStateMachine == null) return;
        if (staTopic == null || !staTopic.equals(topic)) return;
        programStateMachine.onStaPayload(payload);
    }

    @Override public void onDestroy() {
        super.onDestroy();

        if (programStateMachine != null) {
            programStateMachine.stopProgram();
        }

        if (mqtt != null) mqtt.gracefulDisconnect();
        if (net != null) {
            net.releaseWifiHighPerf();
            net.unbindWifiNetwork();
        }
        isConnected = false;
        lastStatus = "disconnected";
        mqttStatusLabel = "Disconnected";
        updateNotification();
        sendStatus("disconnected", null);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    // ==========================
    // 토픽 설정 저장
    // ==========================
    private void saveTopicConfig() {
        SharedPreferences sp = getSharedPreferences(PREF, MODE_PRIVATE);
        sp.edit()
                .putString(KEY_ROOT_TOPIC, rootTopic)
                .putString(KEY_BASE_TOPIC, baseTopic)
                .apply();
    }
}
