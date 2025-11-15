package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

import kr.re.kitech.tractorinspectionrobot.mqtt.MqttForegroundService;

public class SharedMqttViewModel extends AndroidViewModel {

    private final Context app;

    // 1) 임의 직접 메시지 (기존 호환)
    private final MutableLiveData<MqttDirectMessage> directMessage = new MutableLiveData<>();
    public LiveData<MqttDirectMessage> getDirectMessage() { return directMessage; }
    public void postDirectMessage(String topic, String payload) {
        try { directMessage.postValue(new MqttDirectMessage(topic, new JSONObject(payload))); }
        catch (Exception e) { directMessage.postValue(new MqttDirectMessage(topic, payload)); }
    }

    // 2) MQTT 연결 상태
    private final MutableLiveData<Boolean> mqttConnected = new MutableLiveData<>(false);
    public LiveData<Boolean> getMqttConnected() { return mqttConnected; }

    // 3) 로봇 전체 상태 (x,y,z,xPrimeDeg,yPrimeDeg,zPrimeDeg,ts)
    private final MutableLiveData<RobotState> state = new MutableLiveData<>(new RobotState(0,0,0,0,0,0,0));
    public LiveData<RobotState> getState() { return state; }
    private RobotState getOrDefault() {
        RobotState s = state.getValue();
        return (s == null) ? new RobotState(0,0,0,0,0,0,0) : s;
    }

    // ---- 연결 상태 수신 ----
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || !MqttForegroundService.ACTION_MQTT_STATUS.equals(intent.getAction())) return;
            String status = intent.getStringExtra(MqttForegroundService.EXTRA_STATUS);
            if (status == null) return;
            if ("connected".equalsIgnoreCase(status)) {
                mqttConnected.postValue(true);
            } else if ("disconnected".equalsIgnoreCase(status) || "rejected".equalsIgnoreCase(status)) {
                mqttConnected.postValue(false);
            }
        }
    };

    // ---- 수신 메시지(예: robot/simulation) 수신 ----
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || !MqttForegroundService.ACTION_MQTT_MESSAGE.equals(intent.getAction())) return;
            String topic   = intent.getStringExtra(MqttForegroundService.EXTRA_TOPIC);
            String payload = intent.getStringExtra(MqttForegroundService.EXTRA_PAYLOAD);
            if (topic == null || payload == null) return;

            // 필요시 모든 메시지를 directMessage로도 남겨둠(디버깅/로그)
            postDirectMessage(topic, payload);

            if (topic.startsWith("robot/simulation/")) {        // topic이 'robot/simulation' 일때 RobotState 아이템을 만듬
                try {
                    JSONObject o = new JSONObject(payload);
                    RobotState cur = getOrDefault();
                    RobotState next = RobotState.fromJson(o, cur);
                    next = RobotState.clamp(next);
                    state.postValue(next);
                } catch (Exception ignore) {}
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public SharedMqttViewModel(@NonNull Application application) {
        super(application);
        this.app = application.getApplicationContext();

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

    /** 버튼 델타 적용 → 전체 상태로 publish */
    public void applyDeltaAndPublish(String deviceName, String axis, double delta) {
        RobotState cur = getOrDefault();
        double x=cur.x, y=cur.y, z=cur.z, xPrimeDeg=cur.xPrimeDeg, yPrimeDeg=cur.yPrimeDeg, zPrimeDeg=cur.zPrimeDeg;

        switch (axis) {
            case "x":         x         = clamp(cur.x         + delta, -750, 750); break;
            case "y":         y         = clamp(cur.y         + delta, -750, 750); break;
            case "z":         z         = clamp(cur.z         + delta, 0,   500); break;
            case "xPrimeDeg": xPrimeDeg = clamp(cur.xPrimeDeg + delta, -60,  60); break;
            case "yPrimeDeg": yPrimeDeg = clamp(cur.yPrimeDeg + delta, -60,  60); break;
            case "zPrimeDeg": zPrimeDeg = clamp(cur.zPrimeDeg + delta, -90,  90); break; // zPrimeDeg 범위 (필요 시 수정)
            default: return;
        }

        long ts = System.currentTimeMillis();
        RobotState next = new RobotState(x, y, z, xPrimeDeg, yPrimeDeg, zPrimeDeg, ts);
        state.setValue(next); // UI 즉시 반영
        publishFull(deviceName, next);
    }

    /** 손을 뗄 때 등, 현재 전체 상태를 재전송하고 싶을 때 */
    public void publishCurrent(String deviceName) {
        publishFull(deviceName, getOrDefault());
    }

    private void publishFull(String deviceName, RobotState s) {
        String topic = "robot/send/" + deviceName;
        String payload = s.toJson().toString();
        Intent i = new Intent(app, MqttForegroundService.class);
        i.setAction(MqttForegroundService.ACTION_PUBLISH);
        i.putExtra(MqttForegroundService.EXTRA_PUB_TOPIC, topic);
        i.putExtra(MqttForegroundService.EXTRA_PUB_PAYLOAD, payload);
        app.startService(i);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
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
        public MqttDirectMessage(String topic, JSONObject json) { this.topic = topic; this.json = json; this.raw = json.toString(); }
        public MqttDirectMessage(String topic, String rawPayload) { this.topic = topic; this.json = null; this.raw = rawPayload; }
    }
}
