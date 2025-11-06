package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

/**
 * MqttForegroundService에서 수신한 onDirect 메시지를
 * 전역적으로 관찰할 수 있도록 하는 ViewModel.
 */
public class SharedMqttViewModel extends ViewModel {

    // MQTT 직접 수신 메시지
    private final MutableLiveData<MqttDirectMessage> directMessage = new MutableLiveData<>();

    public LiveData<MqttDirectMessage> getDirectMessage() {
        return directMessage;
    }

    public void postDirectMessage(String topic, String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            directMessage.postValue(new MqttDirectMessage(topic, json));
        } catch (Exception e) {
            // 파싱 실패 시 원문 저장
            directMessage.postValue(new MqttDirectMessage(topic, payload));
        }
    }

    /**
     * 메시지 객체 (토픽 + JSON 데이터)
     */
    public static class MqttDirectMessage {
        public final String topic;
        public final JSONObject json;
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
