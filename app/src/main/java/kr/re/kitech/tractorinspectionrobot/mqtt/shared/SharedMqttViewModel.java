package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.MqttForegroundService;

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

    // 3) 로봇 전체 상태 (x,y,z,s1,s2,s3,ts)
    private final MutableLiveData<RobotState> state =
            new MutableLiveData<>(new RobotState(0, 0, 0, 0, 0, 0, 0));
    public LiveData<RobotState> getState() { return state; }

    private RobotState getOrDefault() {
        RobotState s = state.getValue();
        return (s == null) ? new RobotState(0, 0, 0, 0, 0, 0, 0) : s;
    }

    // ---- 연결 상태 수신 ----
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null ||
                    !MqttForegroundService.ACTION_MQTT_STATUS.equals(intent.getAction())) return;

            String status = intent.getStringExtra(MqttForegroundService.EXTRA_STATUS);
            if (status == null) return;

            if ("connected".equalsIgnoreCase(status)) {
                mqttConnected.postValue(true);
                Toast.makeText(app, "연결되었습니다.", Toast.LENGTH_SHORT).show();
                // ✅ MQTT 연결 성립 시, 서보를 0도로 초기화 명령 1회 전송
                sendInitialServoZero();
            } else if ("disconnected".equalsIgnoreCase(status)
                    || "rejected".equalsIgnoreCase(status)) {
                mqttConnected.postValue(false);
                Toast.makeText(app, "연결이 해제되었습니다.", Toast.LENGTH_SHORT).show();
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

            // 모든 메시지를 directMessage에도 남김(디버깅/로그용)
            postDirectMessage(topic, payload);

            // STA 토픽이면 로봇 상태로 반영
            if (topic.equals(staTopic)) {
                handleStaPayload(payload);
            }
        }
    };

    /**
     * STA(JSON) → RobotState 갱신
     * 현재 STA는
     * {
     *   "mt": "sta",
     *   "ct": {
     *     "motion": { "pos": { "x":..., "y":..., "z":... }, ... }
     *   }
     * }
     * 형태이고, servo 정보는 없으므로 x,y,z만 갱신되고
     * s1,s2,s3는 기존 값을 유지한다.
     */
    private void handleStaPayload(String payload) {
        try {
            JSONObject root = new JSONObject(payload);
            JSONObject ct = root.optJSONObject("ct");
            if (ct == null) return;

            RobotState cur = getOrDefault();
            double x = cur.x;
            double y = cur.y;
            double z = cur.z;
            double s1 = cur.s1;
            double s2 = cur.s2;
            double s3 = cur.s3;

            // motion.pos → x,y,z
            JSONObject motion = ct.optJSONObject("motion");
            if (motion != null) {
                JSONObject pos = motion.optJSONObject("pos");
                if (pos != null) {
                    x = pos.optDouble("x", x);
                    y = pos.optDouble("y", y);
                    z = pos.optDouble("z", z);
                }
            }

            // 현재 STA에는 servo가 없으므로, servo 파트가 없으면 기존 값 유지
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
                s1 = servo.optDouble("s1", s1);
                s2 = servo.optDouble("s2", s2);
                s3 = servo.optDouble("s3", s3);
            }

            long ts = System.currentTimeMillis();
            RobotState next = new RobotState(x, y, z, s1, s2, s3, ts);
            next = RobotState.clamp(next);
            state.postValue(next);
        } catch (Exception ignore) {}
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public SharedMqttViewModel(@NonNull Application application) {
        super(application);
        this.app = application.getApplicationContext();

        String rootTopic = application.getString(R.string.mqtt_root_topic); // ex) "ingsys"
        baseTopic = application.getString(R.string.mqtt_base_topic);        // ex) "ing_w00001"

        staTopic = rootTopic + "/" + baseTopic + "/sta";
        reqTopic = rootTopic + "/" + baseTopic + "/req";

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
     * - axis가 s1,s2,s3 → cmd=3002 (ABS, s1,s2,s3)
     */
    public void applyDeltaAndPublish(String deviceName, String axis, double delta) {
        RobotState cur = getOrDefault();
        double x         = cur.x;
        double y         = cur.y;
        double z         = cur.z;
        double s1 = cur.s1;
        double s2 = cur.s2;
        double s3 = cur.s3;

        boolean movedPos   = false;
        boolean movedServo = false;

        switch (axis) {
            case "x":
                x = clamp(cur.x + delta, 0, 1500);
                movedPos = true;
                break;
            case "y":
                y = clamp(cur.y + delta, 0, 1500);
                movedPos = true;
                break;
            case "z":
                z = clamp(cur.z + delta, 0, 500);
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
                s3 = clamp(cur.s3 + delta, 0, 360); // 필요 시 범위 조정
                movedServo = true;
                break;
            default:
                return;
        }

        long ts = System.currentTimeMillis();
        RobotState next = new RobotState(x, y, z, s1, s2, s3, ts);
        next = RobotState.clamp(next);

        // UI 즉시 반영
        state.setValue(next);
        Boolean connected = mqttConnected.getValue();
        if (connected == null || !connected) {
            Log.w(TAG, "applyDeltaAndPublish() called while MQTT not connected. Ignored.");
            long now = System.currentTimeMillis();
            if (now - lastNotConnectedToastMs > 2_000) {
                Toast.makeText(app, "현재 MQTT 미연결 상태입니다.", Toast.LENGTH_SHORT).show();
                lastNotConnectedToastMs = now;
            }
            return;
        }
        // 🔀 분기: 좌표/서보 각각 해당하는 cmd만 전송
        if (movedPos) {
            publishMoveAbs(next);   // cmd=2001, x,y,z
        }
        if (movedServo) {
            publishServoAbs(next);  // cmd=3002, s1,s2,s3
        }
    }

    /**
     * 현재 상태를 그대로 다시 보내고 싶을 때 (손 뗄 때 등)
     * - 좌표/서보 모두 ABS로 재전송
     */
    public void publishCurrent(String deviceName) {
        RobotState s = getOrDefault();
        publishMoveAbs(s);
        publishServoAbs(s);
    }

    /**
     * MQTT 연결 직후 한 번 호출되는 서보 초기화:
     * s1(s1), s2(s2), s3(s3) = 0도로 맞추는 ABS 명령
     *
     * 토픽: ingsys/<baseTopic>/req
     * JSON:
     * {
     *   "mt": "req",
     *   "tm": "...",
     *   "fp": "pc-controller",
     *   "ct": {
     *     "tg": "ing_w00001",
     *     "cmd": 3002,
     *     "opid": N,
     *     "param": { "mode": "abs", "s1": 0, "s2": 0, "s3": 0 }
     *   }
     * }
     */
    private void sendInitialServoZero() {
        try {
            JSONObject root = new JSONObject();
            root.put("mt", "req");
            root.put("tm", nowIso());
            root.put("fp", FP);

            JSONObject ct = new JSONObject();
            ct.put("tg", baseTopic);
            ct.put("cmd", 3002);
            ct.put("opid", opidCounter++);

            JSONObject p = new JSONObject();
            p.put("mode", "abs");
            p.put("s1", 0);
            p.put("s2", 0);
            p.put("s3", 0);

            ct.put("param", p);
            root.put("ct", ct);

            sendMqtt(reqTopic, root.toString());

            // 내부 상태도 같이 0으로 맞춰주고 싶으면 주석 해제
            /*
            long ts = System.currentTimeMillis();
            RobotState cur = getOrDefault();
            RobotState next = new RobotState(cur.x, cur.y, cur.z, 0, 0, 0, ts);
            state.postValue(next);
            */
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
            ct.put("tg", baseTopic);   // ex) "ing_w00001"
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
     * ServoMove ABS (cmd=3002, s1,s2,s3)
     */
    private void publishServoAbs(RobotState s) {
        try {
            JSONObject root = new JSONObject();
            root.put("mt", "req");
            root.put("tm", nowIso());
            root.put("fp", FP);

            JSONObject ct = new JSONObject();
            ct.put("tg", baseTopic);
            ct.put("cmd", 3002);
            ct.put("opid", opidCounter++);

            JSONObject p = new JSONObject();
            p.put("mode", "abs");
            p.put("s1", s.s1); // s1
            p.put("s2", s.s2); // s2
            p.put("s3", s.s3); // s3

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

    private static double clamp(double v, double min, double max) {
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
}
