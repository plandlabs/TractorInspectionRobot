package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

import kr.re.kitech.tractorinspectionrobot.mqtt.MqttForegroundService;

public class SharedMqttViewModel extends AndroidViewModel {

    private final Context app; // Application Context

    // 1) 직접 수신 메시지 (기존 기능 유지)
    private final MutableLiveData<MqttDirectMessage> directMessage = new MutableLiveData<>();
    public LiveData<MqttDirectMessage> getDirectMessage() { return directMessage; }
    public void postDirectMessage(String topic, String payload) {
        try { directMessage.postValue(new MqttDirectMessage(topic, new JSONObject(payload))); }
        catch (Exception e) { directMessage.postValue(new MqttDirectMessage(topic, payload)); }
    }

    // 2) 연결 상태
    private final MutableLiveData<Boolean> mqttConnected = new MutableLiveData<>(false);
    public LiveData<Boolean> getMqttConnected() { return mqttConnected; }

    // 상태 이벤트 수신
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || !MqttForegroundService.ACTION_MQTT_STATUS.equals(intent.getAction())) return;

            String status = intent.getStringExtra(MqttForegroundService.EXTRA_STATUS);
            if (status == null) return;

            if ("connected".equalsIgnoreCase(status)) {
                mqttConnected.postValue(true);
            } else if ("disconnected".equalsIgnoreCase(status)
                    || "rejected".equalsIgnoreCase(status)) {
                mqttConnected.postValue(false);
            }
            // "initializing", "reconnecting", "pong" 등은 UI 상태 유지
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public SharedMqttViewModel(@NonNull Application application) {
        super(application);
        this.app = application.getApplicationContext();

        IntentFilter filter = new IntentFilter(MqttForegroundService.ACTION_MQTT_STATUS);
        // Android 13+ 는 플래그 필요
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            application.registerReceiver(statusReceiver, filter);
        }
    }

    /** 서비스가 살아있으면 현재 상태를 즉시 재브로드캐스트해줌 */
    public void requestStatus() {
        Intent i = new Intent(app, MqttForegroundService.class);               // ★ 명시적 컴포넌트
        i.setAction(MqttForegroundService.ACTION_QUERY_STATUS);                // ★ 액션 지정
        ContextCompat.startForegroundService(app, i);
    }

    /** 서비스 시작 = 연결 시도 트리거 */
    public void startServiceForConnect() {
        Intent i = new Intent(app, MqttForegroundService.class);               // ★ 명시적 컴포넌트
        i.setAction(MqttForegroundService.ACTION_CONNECT);                     // ★ 액션 지정
        ContextCompat.startForegroundService(app, i);
    }

    /** 명시적 해제 요청 */
    public void requestDisconnect() {
        Intent i = new Intent(app, MqttForegroundService.class);               // ★ 명시적 컴포넌트
        i.setAction(MqttForegroundService.ACTION_DISCONNECT);                  // ★ 서비스 상수 사용
        app.startService(i);                                                   // 포그라운드 필요 X
    }

    @Override
    protected void onCleared() {
        try { getApplication().unregisterReceiver(statusReceiver); } catch (Exception ignore) {}
        super.onCleared();
    }

    // DTO 유지
    public static class MqttDirectMessage {
        public final String topic;
        public final JSONObject json; // null 가능
        public final String raw;
        public MqttDirectMessage(String topic, JSONObject json) { this.topic = topic; this.json = json; this.raw = json.toString(); }
        public MqttDirectMessage(String topic, String rawPayload) { this.topic = topic; this.json = null; this.raw = rawPayload; }
    }
}
