package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.MqttForegroundService;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;
import kr.re.kitech.tractorinspectionrobot.utils.StringConvUtil;
import lombok.Getter;

public class SharedMqttViewModel extends AndroidViewModel {

    private final Context app;
    private final String staTopic;
    private final String reqTopic;
    private final String baseTopic;

    // MQTT opid 증가용
    private int opidCounter = 1;

    // from-point (클라이언트 식별자)
    private static final String FP = "pc-controller";

    // 1) 임의 직접 메시지 (기존 호환용)
    private final MutableLiveData<MqttDirectMessage> directMessage = new MutableLiveData<>();
    public LiveData<MqttDirectMessage> getDirectMessage() { return directMessage; }
    private long lastNotConnectedToastMs = 0L;
    public void postDirectMessage(String topic, String payload) {
        try {
            directMessage.postValue(new MqttDirectMessage(topic, new JSONObject(payload)));
        } catch (Exception e) {
            directMessage.postValue(new MqttDirectMessage(topic, payload));
        }
    }

    // 2) MQTT 연결 상태
    private final MutableLiveData<Boolean> mqttConnected = new MutableLiveData<>(false);
    public LiveData<Boolean> getMqttConnected() { return mqttConnected; }

    // 최초 STA 1회 구분용
    private final MutableLiveData<Boolean> firstConnectReceive = new MutableLiveData<>(false);
    public LiveData<Boolean> getFirstConnectReceive() { return firstConnectReceive; }

    // 프로그램 실행 여부
    private final MutableLiveData<Boolean> programState = new MutableLiveData<>(false);
    public LiveData<Boolean> getProgramState() { return programState; }

    // 3) 로봇 전체 상태 (x,y,z,s1,s2,s3,ts)
    private final MutableLiveData<RobotState> state =
            new MutableLiveData<>(new RobotState(0, 0, 0, 0, 0, 0, 0));
    public LiveData<RobotState> getState() { return state; }

    private RobotState getOrDefault() {
        RobotState s = state.getValue();
        return (s == null) ? new RobotState(0, 0, 0, 0, 0, 0, 0) : s;
    }

    // 버튼으로 만든 명령 상태 (state와는 별개)
    private final MutableLiveData<RobotState> commandState = new MutableLiveData<>();
    public LiveData<RobotState> getCommandState() { return commandState; }

    private RobotState getCommandDefault() {
        RobotState s = commandState.getValue();
        return (s == null) ? new RobotState(0, 0, 0, 0, 0, 0, 0) : s;
    }

    // ---- 연결 상태 수신 ----
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null ||
                    !MqttForegroundService.ACTION_MQTT_STATUS.equals(intent.getAction())) return;

            String status = intent.getStringExtra(MqttForegroundService.EXTRA_STATUS);
            if (status == null) return;

            if ("connected".equalsIgnoreCase(status)) {

                Boolean prev = mqttConnected.getValue();
                mqttConnected.postValue(true);
                firstConnectReceive.postValue(true);
                Toast.makeText(app, "연결되었습니다.", Toast.LENGTH_SHORT).show();
                sendInitialServoZero();  // 서보 0도 초기화도 이때만

                // ✅ 이전 상태가 null/false일 때만 "새로 연결"로 간주하고 싶으면 아래로
                // if (prev == null || !prev) {
                //     Toast.makeText(app, "연결되었습니다.", Toast.LENGTH_SHORT).show();
                //     sendInitialServoZero();
                // }

            } else if ("disconnected".equalsIgnoreCase(status)
                    || "rejected".equalsIgnoreCase(status)) {

                Boolean prev = mqttConnected.getValue();
                mqttConnected.postValue(false);
                firstConnectReceive.postValue(false);
                Toast.makeText(app, "연결이 해제되었습니다.", Toast.LENGTH_SHORT).show();

                // if (prev != null && prev) {
                //     Toast.makeText(app, "연결이 해제되었습니다.", Toast.LENGTH_SHORT).show();
                // }
            }
        }
    };

    // ---- 수신 메시지 수신 (MQTT → ForegroundService → 브로드캐스트) ----
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null ||
                    !MqttForegroundService.ACTION_MQTT_MESSAGE.equals(intent.getAction())) return;

            String topic   = intent.getStringExtra(MqttForegroundService.EXTRA_TOPIC);
            String payload = intent.getStringExtra(MqttForegroundService.EXTRA_PAYLOAD);
            if (topic == null || payload == null) return;
            Log.w("topic", topic);
            Log.w("topic sta", staTopic);

            // 모든 메시지를 directMessage에도 남김(디버깅/로그용)
            postDirectMessage(topic, payload);

            // STA 토픽이면 로봇 상태로 반영
            if (topic.equals(staTopic)) {
                handleStaPayload(payload);
            }
        }
    };

    // ✅ 프로그램 진행상태 수신 (running true/false 등)
    private final BroadcastReceiver programProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null ||
                    !MqttForegroundService.ACTION_PROGRAM_PROGRESS.equals(intent.getAction()))
                return;

            boolean running = intent.getBooleanExtra(
                    MqttForegroundService.EXTRA_PROGRAM_RUNNING,
                    false
            );
            int index = intent.getIntExtra(MqttForegroundService.EXTRA_PROGRAM_INDEX, -1);
            int total = intent.getIntExtra(MqttForegroundService.EXTRA_PROGRAM_TOTAL, 0);
            int phase = intent.getIntExtra(MqttForegroundService.EXTRA_PROGRAM_PHASE, 0);

            // 프로그램 실행 여부만 LiveData로 반영
            programState.postValue(running);

            Log.d("SharedMqttViewModel",
                    "PROGRAM_PROGRESS running=" + running +
                            " index=" + index + "/" + total +
                            " phase=" + phase);
        }
    };

    /**
     * STA(JSON) → RobotState 갱신
     * 현재 STA는
     * {
     *   "mt": "sta",
     *   "ct": {
     *     "motion": { "pos": { "x":..., "y":..., "z":... }, ... },
     *     "servo": {
     *         "angles": { "s1":..., "s2":..., "s3":... } 또는
     *         "s1":..., "s2":..., "s3":...
     *     }
     *   }
     * }
     */
    private void handleStaPayload(String payload) {
        try {
            JSONObject root = new JSONObject(payload);
            JSONObject ct = root.optJSONObject("ct");
            if (ct == null) return;

            RobotState cur = getOrDefault();
            int x  = cur.x;
            int y  = cur.y;
            int z  = cur.z;
            int s1 = cur.s1;
            int s2 = cur.s2;
            int s3 = cur.s3;

            // motion.pos → x,y,z
            JSONObject motion = ct.optJSONObject("motion");
            if (motion != null) {
                JSONObject pos = motion.optJSONObject("pos");
                if (pos != null) {
                    x = pos.optInt("x", x);
                    y = pos.optInt("y", y);
                    z = pos.optInt("z", z);
                }
            }

            // servo.angles → s1,s2,s3 (또는 servo.s1/s2/s3 직접)
            JSONObject servo = null;
            JSONObject servoContainer = ct.optJSONObject("servo");
            if (servoContainer != null) {
                servo = servoContainer.optJSONObject("angles");
                if (servo == null && servoContainer.has("s1")) {
                    // angles 없이 바로 s1/s2/s3가 있을 수도 있음
                    servo = servoContainer;
                }
            }
            if (servo != null) {
                s1 = servo.optInt("s1", s1);
                s2 = servo.optInt("s2", s2);
                s3 = servo.optInt("s3", s3);
            }

            long ts = System.currentTimeMillis();
            RobotState next = new RobotState(x, y, z, s1, s2, s3, ts);
            next = RobotState.clamp(next);
            state.postValue(next);

            // 최초 STA 1회일 때만 commandState에도 복사
            if (Boolean.TRUE.equals(firstConnectReceive.getValue())) {
                commandState.postValue(next);
            }
            firstConnectReceive.postValue(false);
        } catch (Exception ignore) {}
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public SharedMqttViewModel(@NonNull Application application) {
        super(application);
        this.app = application.getApplicationContext();

        // ✅ ForegroundService에서 저장한 root/base 토픽을 우선 사용
        SharedPreferences sp =
                app.getSharedPreferences(MqttForegroundService.PREF, Context.MODE_PRIVATE);

        String rootTopicPref = sp.getString(
                MqttForegroundService.KEY_ROOT_TOPIC,
                application.getString(R.string.mqtt_root_topic)  // 없으면 기본값
        );
        String baseTopicPref = sp.getString(
                MqttForegroundService.KEY_BASE_TOPIC,
                application.getString(R.string.mqtt_base_topic)  // 없으면 기본값
        );

        this.baseTopic = baseTopicPref;
        this.staTopic  = rootTopicPref + "/" + StringConvUtil.md5(baseTopicPref) + "/sta";
        this.reqTopic  = rootTopicPref + "/" + StringConvUtil.md5(baseTopicPref) + "/req";

        // 상태 브로드캐스트 수신
        IntentFilter f1 = new IntentFilter(MqttForegroundService.ACTION_MQTT_STATUS);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(statusReceiver, f1, Context.RECEIVER_NOT_EXPORTED);
        } else {
            application.registerReceiver(statusReceiver, f1);
        }

        // 메시지 브로드캐스트 수신
        IntentFilter f2 = new IntentFilter(MqttForegroundService.ACTION_MQTT_MESSAGE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(messageReceiver, f2, Context.RECEIVER_NOT_EXPORTED);
        } else {
            application.registerReceiver(messageReceiver, f2);
        }

        // 프로그램 포즈 수신
        IntentFilter f3 = new IntentFilter(MqttForegroundService.ACTION_PROGRAM_POSE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(programPoseReceiver, f3, Context.RECEIVER_NOT_EXPORTED);
        } else {
            application.registerReceiver(programPoseReceiver, f3);
        }

        // ✅ 프로그램 진행상태 수신
        IntentFilter f4 = new IntentFilter(MqttForegroundService.ACTION_PROGRAM_PROGRESS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(programProgressReceiver, f4, Context.RECEIVER_NOT_EXPORTED);
        } else {
            application.registerReceiver(programProgressReceiver, f4);
        }
    }

    /** 서비스에게 현재 상태 브로드캐스트를 요청 */
    public void requestStatus() {
        Intent i = new Intent(app, MqttForegroundService.class);
        i.setAction(MqttForegroundService.ACTION_QUERY_STATUS);
        ContextCompat.startForegroundService(app, i);
    }

    /** 서비스 시작(연결 시도) */
    public void startServiceForConnect() {
        Intent i = new Intent(app, MqttForegroundService.class);
        i.setAction(MqttForegroundService.ACTION_CONNECT);
        ContextCompat.startForegroundService(app, i);
    }

    /** 명시적 해제 */
    public void requestDisconnect() {
        Intent i = new Intent(app, MqttForegroundService.class);
        i.setAction(MqttForegroundService.ACTION_DISCONNECT);
        app.startService(i);
    }

    /**
     * 버튼 델타 적용 → 내부 상태 갱신 + 분기해서 브로커에 publish
     *
     * - axis가 x,y,z → cmd=2001 (ABS, x,y,z)
     * - axis가 s1,s2,s3 → cmd=2003 (ABS, s1,s2,s3)
     */
    public void applyDeltaAndPublish(String axis, int delta) {
        RobotState cur = getCommandDefault();
        int x  = cur.x;
        int y  = cur.y;
        int z  = cur.z;
        int s1 = cur.s1;
        int s2 = cur.s2;
        int s3 = cur.s3;

        boolean movedPos   = false;
        boolean movedServo = false;

        switch (axis) {
            case "x":
                x = clamp(cur.x + delta, 0, 44000);
                movedPos = true;
                break;
            case "y":
                y = clamp(cur.y + delta, 0, 25000);
                movedPos = true;
                break;
            case "z":
                z = clamp(cur.z + delta, 0, 3500);
                movedPos = true;
                break;
            case "s1":
                s1 = clamp(cur.s1 + delta, 0, 180);
                movedServo = true;
                break;
            case "s2":
                s2 = clamp(cur.s2 + delta, 0, 180);
                movedServo = true;
                break;
            case "s3":
                s3 = clamp(cur.s3 + delta, 0, 180); // 필요 시 범위 조정
                movedServo = true;
                break;
            default:
                return;
        }

        long ts = System.currentTimeMillis();
        RobotState next = new RobotState(x, y, z, s1, s2, s3, ts);
        next = RobotState.clamp(next);

        // ✅ 버튼으로 만들어진 목표 포즈는 항상 commandState에 반영
        commandState.setValue(next);

        Boolean connected = mqttConnected.getValue();
        if (connected == null || !connected) {
            Log.w(TAG, "applyDeltaAndPublish() called while MQTT not connected. Ignored.");
            long now = System.currentTimeMillis();
            // 미연결시에만 UI 즉시 반영
            state.setValue(next);
            if (now - lastNotConnectedToastMs > 2_000) {
                Toast.makeText(app, "현재 MQTT 미연결 상태입니다. 연결상태를 확인하세요.", Toast.LENGTH_SHORT).show();
                lastNotConnectedToastMs = now;
            }
            return;
        } else {
            // 연결시에 s1,s2,s3는 UI 즉시 반영
            if (axis.equals("s1") || axis.equals("s2") || axis.equals("s3")) state.setValue(next);
        }

        // 🔀 분기: 좌표/서보 각각 해당하는 cmd만 전송
        if (movedPos) {
            publishMoveAbs(next);   // cmd=2001, x,y,z
        }
        if (movedServo) {
            publishServoAbs(next);  // cmd=2003, s1,s2,s3
        }
    }

    /**
     * 완성된 RobotState를 한 번에 적용 + MQTT로 전송
     *
     * - 내부 상태(state LiveData) 갱신
     * - cmd=2001 (x,y,z ABS) 전송
     * - cmd=2003 (s1,s2,s3 ABS) 전송
     */
    public void applyStateAndPublish(RobotState target) {
        // null 대비 + 범위 클램프
        long ts = System.currentTimeMillis();
        int x  = clamp(target.x,  0, 44000);
        int y  = clamp(target.y,  0, 25000);
        int z  = clamp(target.z,  0, 3500);
        int s1 = clamp(target.s1, 0, 180);
        int s2 = clamp(target.s2, 0, 180);
        int s3 = clamp(target.s3, 0, 180);

        RobotState next = new RobotState(x, y, z, s1, s2, s3, ts);
        next = RobotState.clamp(next);

        // ✅ 버튼으로 만들어진 목표 포즈는 항상 commandState에 반영
        commandState.setValue(next);

        // MQTT 연결 여부 체크
        Boolean connected = mqttConnected.getValue();
        if (connected == null || !connected) {
            Log.w(TAG, "applyStateAndPublish() called while MQTT not connected. Ignored.");
            long now = System.currentTimeMillis();
            // 미연결시에만 UI 즉시 반영
            state.setValue(next);
            if (now - lastNotConnectedToastMs > 2_000) {
                Toast.makeText(app, "현재 MQTT 미연결 상태입니다. 연결상태를 확인하세요.", Toast.LENGTH_SHORT).show();
                lastNotConnectedToastMs = now;
            }
            return;
        } else {
            // 연결시에 s1,s2,s3만 UI 즉시 반영, x,y,z는 현재값 유지
            RobotState servoOnly = new RobotState(
                    Objects.requireNonNull(state.getValue()).x,
                    Objects.requireNonNull(state.getValue()).y,
                    Objects.requireNonNull(state.getValue()).z,
                    s1, s2, s3, ts);
            state.setValue(servoOnly);
        }

        // 위치 + 서보 모두 ABS로 전송
        publishMoveAbs(next);   // cmd=2001, x,y,z
        publishServoAbs(next);  // cmd=2003, s1,s2,s3
    }

    public void applyStateAndPublish(int x, int y, int z,
                                     int s1, int s2, int s3) {
        RobotState target = new RobotState(x, y, z, s1, s2, s3, System.currentTimeMillis());
        applyStateAndPublish(target);
    }

    /**
     * 현재 상태를 그대로 다시 보내고 싶을 때 (손 뗄 때 등)
     * - 좌표/서보 모두 ABS로 재전송
     */
    public void publishCurrent() {
        RobotState s = getOrDefault();
        publishMoveAbs(s);
        publishServoAbs(s);
    }

    /**
     * MQTT 연결 직후 한 번 호출되는 서보 초기화:
     * s1, s2, s3 = 0도로 맞추는 ABS 명령
     */
    private void sendInitialServoZero() {
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
            p.put("s1", 0);
            p.put("s2", 0);
            p.put("s3", 0);

            ct.put("param", p);
            root.put("ct", ct);

            sendMqtt(reqTopic, root.toString());

            long ts = System.currentTimeMillis();
            RobotState cur = getOrDefault();
            RobotState next = new RobotState(cur.x, cur.y, cur.z, 0, 0, 0, ts);
            state.postValue(next);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * MoveAxes ABS (cmd=2001, x,y,z)
     */
    private void publishMoveAbs(RobotState s) {
        try {
            JSONObject root = new JSONObject();
            root.put("mt", "req");
            root.put("tm", nowIso());
            root.put("fp", FP);

            JSONObject ct = new JSONObject();
            ct.put("tg", baseTopic);   // ex) "ing_xyz_001"
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

            sendMqtt(reqTopic, root.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ServoMove ABS (cmd=2003, s1,s2,s3)
     */
    private void publishServoAbs(RobotState s) {
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

            sendMqtt(reqTopic, root.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 실제로 ForegroundService에 MQTT publish 요청 */
    private void sendMqtt(String topic, String payload) {
        Intent i = new Intent(app, MqttForegroundService.class);
        i.setAction(MqttForegroundService.ACTION_PUBLISH);
        i.putExtra(MqttForegroundService.EXTRA_PUB_TOPIC, topic);
        i.putExtra(MqttForegroundService.EXTRA_PUB_PAYLOAD, payload);
        app.startService(i);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** "yyyy-MM-dd HH:mm:ss" 형식 현재 시각 */
    private static String nowIso() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    protected void onCleared() {
        try { getApplication().unregisterReceiver(statusReceiver); } catch (Exception ignore) {}
        try { getApplication().unregisterReceiver(messageReceiver); } catch (Exception ignore) {}
        try { getApplication().unregisterReceiver(programPoseReceiver); } catch (Exception ignore) {}
        try { getApplication().unregisterReceiver(programProgressReceiver); } catch (Exception ignore) {}
        super.onCleared();
    }

    // DTO (기존 유지)
    public static class MqttDirectMessage {
        public final String topic;
        public final JSONObject json; // null 가능
        public final String raw;
        public MqttDirectMessage(String topic, JSONObject json) {
            this.topic = topic;
            this.json = json;
            this.raw = json.toString();
        }
        public MqttDirectMessage(String topic, String rawPayload) {
            this.topic = topic;
            this.json = null;
            this.raw = rawPayload;
        }
    }

    private final BroadcastReceiver programPoseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null ||
                    !MqttForegroundService.ACTION_PROGRAM_POSE.equals(intent.getAction())) return;

            String json = intent.getStringExtra(MqttForegroundService.EXTRA_POSE_JSON);
            if (json == null) return;

            try {
                JSONObject obj = new JSONObject(json);
                RobotState pose = new RobotState(obj);

                long ts = System.currentTimeMillis();

                // 1) 현재 실제 위치는 그대로 유지
                RobotState cur = getOrDefault();

                // 2) commandState 에는 "프로그램이 보내려는 전체 타겟 포즈"를 그대로 넣고
                RobotState cmd = new RobotState(
                        pose.x, pose.y, pose.z,
                        pose.s1, pose.s2, pose.s3,
                        ts
                );
                commandState.postValue(cmd);

                // 3) UI(state)는 x,y,z는 현재값 유지하고, s1,s2,s3만 프로그램 값으로 업데이트
                RobotState next = new RobotState(
                        cur.x, cur.y, cur.z,
                        pose.s1, pose.s2, pose.s3,
                        ts
                );
                next = RobotState.clamp(next);
                state.postValue(next);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
