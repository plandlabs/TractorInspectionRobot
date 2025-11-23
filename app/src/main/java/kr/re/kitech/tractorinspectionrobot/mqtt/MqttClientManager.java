package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.util.Log;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3ConnectBuilder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import kr.re.kitech.tractorinspectionrobot.utils.StringConvUtil;
import lombok.Getter;
import lombok.Setter;

/*
 MqttClientManager (MQTT 전담 레이어)

 - HiveMQ Mqtt3AsyncClient 초기화
 - automaticReconnect() 로 내부 재연결 (별도 Handler 루프 없음)
 - 연결/끊김/메시지 이벤트를 Listener로 위임

 Service 쪽에서는:
  - onConnected()에서 afterConnected() + 큐 flush
  - onDisconnected()에서 상태/알림/브로드캐스트만 처리
 */
public class MqttClientManager {

    public interface Listener {
        void onConnected();
        void onDisconnected(Throwable cause);
        void onMessage(String topic, String payload);
    }

    private static final String TAG = "MqttClientManager";

    private final String host;
    private final int port;
    private final String clientId;
    private final String rootTopic;
    private final String baseTopic;
    private final String username;
    private final String password;

    @Getter
    private final String reqTopic;
    @Getter
    private final String resTopic;
    @Getter
    private final String staTopic;
    @Getter
    private final String obsTopic;

    private Mqtt3AsyncClient mqtt;

    @Getter
    private volatile boolean connected = false;

    @Setter
    private Listener listener;

    public MqttClientManager(String url,
                             String clientId,
                             String rootTopic,
                             String baseTopic,
                             String username,
                             String password) {
        String s = url.replace("tcp://", "").replace("ssl://", "");
        int idx = s.indexOf(':');
        this.host = (idx > 0) ? s.substring(0, idx) : s;
        this.port = (idx > 0) ? Integer.parseInt(s.substring(idx + 1)) : 1883;

        this.clientId = clientId;
        this.rootTopic = rootTopic;
        this.baseTopic = baseTopic;
        this.username = (username != null && !username.isEmpty()) ? username : null;
        this.password = (password != null && !password.isEmpty()) ? password : null;

        String baseMd5 = StringConvUtil.md5(baseTopic);
        this.reqTopic = rootTopic + "/" + baseMd5 + "/req";
        this.resTopic = rootTopic + "/" + baseMd5 + "/res";
        this.staTopic = rootTopic + "/" + baseMd5 + "/sta";
        this.obsTopic = rootTopic + "/" + baseMd5 + "/obs";
    }

    public void init() {
        // automaticReconnect 설정: 내부에서 1~30초 사이 백오프 재연결
        mqtt = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .automaticReconnect()
                .initialDelay(1, TimeUnit.SECONDS)
                .maxDelay(30, TimeUnit.SECONDS)
                .applyAutomaticReconnect()
                .addConnectedListener(ctx -> {
                    connected = true;
                    Log.i(TAG, "MQTT connected: " + host + ":" + port);
                    if (listener != null) listener.onConnected();
                })
                .addDisconnectedListener((MqttClientDisconnectedListener) ctx -> {
                    connected = false;
                    Throwable cause = ctx.getCause();
                    Log.w(TAG, "MQTT disconnected. cause=" + (cause != null ? cause.getMessage() : "null"));

                    // 사실상 "connectionLost" 역할
                    if (listener != null) {
                        listener.onDisconnected(cause);
                    }
                    // automaticReconnect() 덕분에 여기서 별도 재연결 루프는 돌릴 필요 없음
                })
                .buildAsync();

        // 전체 publish 수신 → 우리가 관심 있는 토픽만 listener로 전달
        mqtt.publishes(MqttGlobalPublishFilter.ALL, p -> {
            String topic = p.getTopic().toString();
            String payload = new String(p.getPayloadAsBytes(), StandardCharsets.UTF_8);

            if (listener != null &&
                    (topic.equals(resTopic) ||
                            topic.equals(staTopic) ||
                            topic.equals(obsTopic))) {
                listener.onMessage(topic, payload);
            }
        });
    }

    public void connect() {
        if (mqtt == null) return;
        try {
            Mqtt3ConnectBuilder.Send<?> builder = mqtt.connectWith()
                    .keepAlive(60)
                    .cleanSession(true);

            if (username != null) {
                builder.simpleAuth()
                        .username(username)
                        .password(password != null ? password.getBytes(StandardCharsets.UTF_8) : null)
                        .applySimpleAuth();
            }

            builder.send();
        } catch (Exception e) {
            Log.e(TAG, "connect error: " + e.getMessage(), e);
        }
    }

    public void afterConnected() {
        if (mqtt == null) return;
        try {
            mqtt.subscribeWith()
                    .topicFilter(staTopic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();
            mqtt.subscribeWith()
                    .topicFilter(resTopic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();
            mqtt.subscribeWith()
                    .topicFilter(obsTopic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();
        } catch (Exception e) {
            Log.e(TAG, "afterConnected subscribe error", e);
        }
    }

    public void gracefulDisconnect() {
        if (mqtt != null) {
            try {
                mqtt.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "gracefulDisconnect error", e);
            }
        }
    }

    public void publish(String topic, String payload, int qos, boolean retain) {
        if (mqtt == null) return;
        try {
            mqtt.publishWith()
                    .topic(topic)
                    .qos(qos == 2 ? MqttQos.EXACTLY_ONCE
                            : qos == 1 ? MqttQos.AT_LEAST_ONCE
                            : MqttQos.AT_MOST_ONCE)
                    .retain(retain)
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .send();
        } catch (Exception e) {
            Log.e(TAG, "publish error", e);
        }
    }
}
