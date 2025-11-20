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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
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

    // 프로그램 포즈 브로드캐스트
    public static final String ACTION_PROGRAM_POSE = "kr.re.kitech.tractorinspectionrobot.MQTT_PROGRAM_POSE";
    public static final String EXTRA_POSE_JSON     = "pose_json";

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

    // opid 증가용
    private int opidCounter = 1;
    private static final String FP = "pc-controller";

    // ==========================
    // Notifier 텍스트 관리
    // ==========================
    private String mqttStatusLabel   = "Initializing...";
    private String programStatusLabel = "";

    private void updateNotification() {
        String text = mqttStatusLabel;
        if (programStatusLabel != null && !programStatusLabel.isEmpty()) {
            text = mqttStatusLabel + " · " + programStatusLabel;
        }
        notifier.update(text);
    }

    // ==========================
    // 프로그램 실행 상태머신
    // ==========================
    // per-item 내부 이동 단계
    private static final int PHASE_IDLE      = 0;
    private static final int PHASE_LIFTING   = 1; // Z→0
    private static final int PHASE_MOVING_XY = 2; // XY→타겟, Z=0
    private static final int PHASE_MOVING_Z  = 3; // Z→타겟 Z
    private static final int PHASE_WAITING   = 4; // intervalSecond 대기

    // 위치 허용 오차 (XYZ 공통)
    private static final int POS_TOL = 10;

    private final Handler programHandler = new Handler(Looper.getMainLooper());
    private boolean programRunning = false;
    private int programPhase = PHASE_IDLE;
    private int programIndex = -1;
    private boolean programStepScheduled = false;
    private int programIntervalSecond = 10;

    private final List<RobotState> programList = new ArrayList<>();
    private RobotState currentProgramTarget = null;
    private RobotState liftTarget = null;
    private RobotState xyTarget = null;
    private RobotState finalTarget = null;

    // STA로부터 받은 마지막 로봇 상태
    private RobotState lastStaState = new RobotState(0,0,0,0,0,0,0);

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
            @Override public void onConnected() {
                isConnected = true;
                mqttStatusLabel = "Connected";
                updateNotification();

                sendStatus("connected", null);
                mqtt.afterConnected();
                flushPendingPublishes();
            }

            @Override public void onDisconnected(Throwable cause) {
                isConnected = false;

                if (userPaused) {
                    mqttStatusLabel = "Disconnected";
                    updateNotification();
                    sendStatus("disconnected", null);
                    return;
                }

                mqttStatusLabel = "Reconnecting...";
                updateNotification();
                sendStatus("reconnecting", (cause != null ? cause.getMessage() : null));
            }

            @Override public void onMessage(String topic, String payload) {
                // 기존 ViewModel 브리지 + 브로드캐스트
                SharedMqttViewModelBridge.getInstance().postDirectMessage(topic, payload);

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
                        initMqttClient();
                    }

                    if (mqtt != null && !mqtt.isConnected()) {
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

                    stopProgramInternal(); // 프로그램도 같이 정지

                    if (mqtt != null) mqtt.gracefulDisconnect();
                    isConnected = false;
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
                    startProgram(programJson, intervalSec);
                    break;
                }

                // ✅ 프로그램 정지
                case ACTION_PROGRAM_STOP: {
                    stopProgramInternal();
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

    @Override public void onDestroy() {
        super.onDestroy();
        stopProgramInternal();

        if (mqtt != null) mqtt.gracefulDisconnect();
        if (net != null) {
            net.releaseWifiHighPerf();
            net.unbindWifiNetwork();
        }
        isConnected = false;
        mqttStatusLabel = "Disconnected";
        updateNotification();
        sendStatus("disconnected", null);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    // ==========================
    // 프로그램 상태머신 구현부
    // ==========================

    private void startProgram(String programJson, int intervalSec) {
        stopProgramInternal(); // 기존 것 정리

        if (programJson == null || programJson.isEmpty()) {
            Log.w(TAG, "startProgram: programJson is empty");
            return;
        }

        try {
            JSONArray arr = new JSONArray(programJson);
            programList.clear();
            for (int j = 0; j < arr.length(); j++) {
                JSONObject obj = arr.getJSONObject(j);
                RobotState rs = new RobotState(obj);
                programList.add(rs);
            }
        } catch (Exception e) {
            Log.e(TAG, "startProgram: invalid program json", e);
            programList.clear();
            return;
        }

        if (programList.isEmpty()) {
            Log.w(TAG, "startProgram: programList is empty");
            return;
        }

        programIntervalSecond = Math.max(1, intervalSec);

        programRunning = true;
        programIndex = 0;
        programPhase = PHASE_LIFTING;
        programStepScheduled = false;
        currentProgramTarget = programList.get(0);
        liftTarget = xyTarget = finalTarget = null;

        prepareProgramPhaseTargets();

        if (liftTarget != null) {
            sendMoveAndServo(liftTarget);
        } else if (finalTarget != null) {
            // 시작 위치가 이미 목적지인 경우 등
            sendMoveAndServo(finalTarget);
        }

        Log.i(TAG, "Program started, total items=" + programList.size());

        updateProgramStatusLabel();
        broadcastProgramProgress();
    }

    private void stopProgramInternal() {
        if (!programRunning && programPhase == PHASE_IDLE) {
            // 이미 멈춰있으면 중복 브로드캐스트 방지
            programList.clear();
            programStatusLabel = "";
            updateNotification();
            broadcastProgramProgress();
            return;
        }

        programRunning = false;
        programHandler.removeCallbacksAndMessages(null);
        programPhase = PHASE_IDLE;
        programIndex = -1;
        programStepScheduled = false;
        currentProgramTarget = null;
        liftTarget = xyTarget = finalTarget = null;

        Log.i(TAG, "Program stopped");

        updateProgramStatusLabel();
        broadcastProgramProgress();
    }

    private void handleProgramStaIfNeeded(String topic, String payload) {
        if (!programRunning) return;
        if (staTopic == null || !staTopic.equals(topic)) return;

        try {
            JSONObject root = new JSONObject(payload);
            JSONObject ct = root.optJSONObject("ct");
            if (ct == null) return;

            int x  = lastStaState.x;
            int y  = lastStaState.y;
            int z  = lastStaState.z;
            int s1 = lastStaState.s1;
            int s2 = lastStaState.s2;
            int s3 = lastStaState.s3;

            JSONObject motion = ct.optJSONObject("motion");
            if (motion != null) {
                JSONObject pos = motion.optJSONObject("pos");
                if (pos != null) {
                    x = pos.optInt("x", x);
                    y = pos.optInt("y", y);
                    z = pos.optInt("z", z);
                }
            }

            JSONObject servoContainer = ct.optJSONObject("servo");
            if (servoContainer != null) {
                JSONObject servo = servoContainer.optJSONObject("angles");
                if (servo == null && servoContainer.has("s1")) {
                    servo = servoContainer;
                }
                if (servo != null) {
                    s1 = servo.optInt("s1", s1);
                    s2 = servo.optInt("s2", s2);
                    s3 = servo.optInt("s3", s3);
                }
            }

            long ts = System.currentTimeMillis();
            lastStaState = new RobotState(x, y, z, s1, s2, s3, ts);

            onProgramStateUpdatedBySta();
        } catch (Exception e) {
            Log.e(TAG, "handleProgramStaIfNeeded parse error", e);
        }
    }

    private void onProgramStateUpdatedBySta() {
        if (!programRunning || currentProgramTarget == null) return;

        switch (programPhase) {
            case PHASE_LIFTING:
                // Z=0 근처(±POS_TOL)까지 들어왔을 때 도달로 간주 (XYZ 모두 ±POS_TOL)
                if (reachedLiftHeight(lastStaState, liftTarget, POS_TOL)) {
                    programPhase = PHASE_MOVING_XY;
                    if (xyTarget != null) {
                        sendMoveAndServo(xyTarget);
                    }
                    updateProgramStatusLabel();
                    broadcastProgramProgress();
                }
                break;

            case PHASE_MOVING_XY:
                if (sameXYZ(lastStaState, xyTarget)) {
                    programPhase = PHASE_MOVING_Z;
                    if (finalTarget != null) {
                        sendMoveAndServo(finalTarget);
                    }
                    updateProgramStatusLabel();
                    broadcastProgramProgress();
                }
                break;

            case PHASE_MOVING_Z:
                // 위치(x,y,z)만 맞으면 도착으로 간주
                if (sameXYZ(lastStaState, finalTarget) && !programStepScheduled) {
                    programPhase = PHASE_WAITING;
                    updateProgramStatusLabel();
                    broadcastProgramProgress();
                    scheduleNextProgramStep();
                }
                break;

            case PHASE_WAITING:
            case PHASE_IDLE:
            default:
                break;
        }
    }

    private void scheduleNextProgramStep() {
        if (!programRunning) return;

        programStepScheduled = true;

        programHandler.postDelayed(() -> {
            if (!programRunning) return;

            programIndex++;

            if (programIndex >= programList.size()) {
                // 전체 완료
                Log.i(TAG, "Program finished");
                stopProgramInternal();
                return;
            }

            currentProgramTarget = programList.get(programIndex);
            programPhase = PHASE_LIFTING;
            programStepScheduled = false;
            liftTarget = xyTarget = finalTarget = null;

            prepareProgramPhaseTargets();

            if (liftTarget != null) {
                sendMoveAndServo(liftTarget);
            } else if (finalTarget != null) {
                sendMoveAndServo(finalTarget);
            }

            Log.i(TAG, "Program step: " + (programIndex + 1) + " / " + programList.size());

            updateProgramStatusLabel();
            broadcastProgramProgress();
        }, programIntervalSecond * 1000L);
    }

    private void prepareProgramPhaseTargets() {
        if (currentProgramTarget == null) return;

        RobotState pose = (lastStaState != null) ? lastStaState : currentProgramTarget;

        int startX = pose.x;
        int startY = pose.y;
        int startZ = pose.z;

        long ts = System.currentTimeMillis();

        liftTarget = new RobotState(
                startX,
                startY,
                0,
                currentProgramTarget.s1,
                currentProgramTarget.s2,
                currentProgramTarget.s3,
                ts
        );

        xyTarget = new RobotState(
                currentProgramTarget.x,
                currentProgramTarget.y,
                0,
                currentProgramTarget.s1,
                currentProgramTarget.s2,
                currentProgramTarget.s3,
                ts
        );

        finalTarget = new RobotState(
                currentProgramTarget.x,
                currentProgramTarget.y,
                currentProgramTarget.z,
                currentProgramTarget.s1,
                currentProgramTarget.s2,
                currentProgramTarget.s3,
                ts
        );

        // 이미 Z=0이고 XY도 같은 경우 → 바로 Z 이동 단계로
        if (startZ == 0 &&
                startX == currentProgramTarget.x &&
                startY == currentProgramTarget.y) {
            liftTarget = null;
            xyTarget = null;
            programPhase = PHASE_MOVING_Z;
        }
    }

    private void sendMoveAndServo(RobotState s) {
        sendMoveAbs(s);
        sendServoAbs(s);
        broadcastProgramPose(s);
    }

    private void sendMoveAbs(RobotState s) {
        try {
            JSONObject root = new JSONObject();
            root.put("mt", "req");
            root.put("tm", nowIso());
            root.put("fp", FP);

            JSONObject ct = new JSONObject();
            ct.put("tg", baseTopic);
            ct.put("cmd", 2001);
            ct.put("opid", opidCounter++);

            JSONObject p = new JSONObject();
            p.put("mode", "abs");
            p.put("x", s.x);
            p.put("y", s.y);
            p.put("z", s.z);
            p.put("scurve", true);

            ct.put("param", p);
            root.put("ct", ct);

            handlePublish(reqTopic, root.toString(), 1, false);
        } catch (Exception e) {
            Log.e(TAG, "sendMoveAbs error", e);
        }
    }

    private void sendServoAbs(RobotState s) {
        try {
            JSONObject root = new JSONObject();
            root.put("mt", "req");
            root.put("tm", nowIso());
            root.put("fp", FP);

            JSONObject ct = new JSONObject();
            ct.put("tg", baseTopic);
            ct.put("cmd", 2003);
            ct.put("opid", opidCounter++);

            JSONObject p = new JSONObject();
            p.put("mode", "abs");
            p.put("s1", s.s1);
            p.put("s2", s.s2);
            p.put("s3", s.s3);

            ct.put("param", p);
            root.put("ct", ct);

            handlePublish(reqTopic, root.toString(), 1, false);
        } catch (Exception e) {
            Log.e(TAG, "sendServoAbs error", e);
        }
    }

    private boolean samePose(RobotState a, RobotState b) {
        if (a == null || b == null) return false;
        return a.x == b.x &&
                a.y == b.y &&
                a.z == b.z &&
                a.s1 == b.s1 &&
                a.s2 == b.s2 &&
                a.s3 == b.s3;
    }

    private boolean sameXYZ(RobotState a, RobotState b) {
        if (a == null || b == null) return false;
        return a.x == b.x &&
                a.y == b.y &&
                a.z == b.z;
    }

    // ✅ Z 리프트 단계에서 사용할 "XYZ ±tol 안이면 OK" 판정
    private boolean reachedLiftHeight(RobotState cur, RobotState target, int tol) {
        if (cur == null || target == null) return false;
        return Math.abs(cur.x - target.x) <= tol &&
                Math.abs(cur.y - target.y) <= tol &&
                Math.abs(cur.z - target.z) <= tol;
    }

    private static String nowIso() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    // ==========================
    // 프로그램 진행 상황 브로드캐스트 + Notifier 텍스트 갱신
    // ==========================

    private String phaseToLabel(int phase) {
        switch (phase) {
            case PHASE_LIFTING:   return "Z→0";
            case PHASE_MOVING_XY: return "XY 이동";
            case PHASE_MOVING_Z:  return "Z 이동";
            case PHASE_WAITING:   return "대기";
            case PHASE_IDLE:
            default:              return "";
        }
    }

    private void updateProgramStatusLabel() {
        if (!programRunning || programList.isEmpty() || programIndex < 0 || programIndex >= programList.size()) {
            programStatusLabel = "";
        } else {
            String phaseStr = phaseToLabel(programPhase);
            String stepStr  = (programIndex + 1) + "/" + programList.size();
            if (phaseStr.isEmpty()) {
                programStatusLabel = "Program " + stepStr;
            } else {
                programStatusLabel = "Program " + stepStr + " (" + phaseStr + ")";
            }
        }
        updateNotification();
    }

    private void broadcastProgramProgress() {
        Intent intent = new Intent(ACTION_PROGRAM_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PROGRAM_RUNNING, programRunning);
        intent.putExtra(EXTRA_PROGRAM_INDEX, programIndex);
        intent.putExtra(EXTRA_PROGRAM_TOTAL, programList.size());
        intent.putExtra(EXTRA_PROGRAM_PHASE, programPhase);
        sendBroadcast(intent);
    }

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
