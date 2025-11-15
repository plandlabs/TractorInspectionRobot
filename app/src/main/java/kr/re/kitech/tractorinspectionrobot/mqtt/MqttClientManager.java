// com/plandlabs/nepesarkmonitwatch/mqtt/MqttClientManager.java
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

import lombok.Getter;
import lombok.Setter;

/*
MqttClientManager (MQTT 전담)

HiveMQ Mqtt3AsyncClient 초기화/자동재연결/콜백 관리.

connect() 시 사용자 이름/비밀번호를 포함한 기본 연결만 수행하고,
afterConnected()에서 ingsys/<base>/sta,res,obs 토픽을 구독한다.

publish()는 외부에서 만든 페이로드를 그대로 퍼블리시한다.

수신 토픽은 Listener.onMessage()로 그대로 위임한다.
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
    private final String reqTopic;
    private final String resTopic;
    private final String staTopic;
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
        this.reqTopic = rootTopic + "/" + baseTopic + "/req";
        this.resTopic = rootTopic + "/" + baseTopic + "/res";
        this.staTopic = rootTopic + "/" + baseTopic + "/sta";
        this.obsTopic = rootTopic + "/" + baseTopic + "/obs";
    }

    public void init() {
        mqtt = MqttClient.builder().useMqttVersion3().identifier(clientId)
                .serverHost(host).serverPort(port)
                .automaticReconnect().initialDelay(1, TimeUnit.SECONDS).maxDelay(30, TimeUnit.SECONDS).applyAutomaticReconnect()
                .addConnectedListener(ctx -> { connected = true; if (listener!=null) listener.onConnected(); })
                .addDisconnectedListener((MqttClientDisconnectedListener) ctx -> {
                    connected = false; if (listener!=null) listener.onDisconnected(ctx.getCause());
                }).buildAsync();

        mqtt.publishes(MqttGlobalPublishFilter.ALL, p -> {
            String topic = p.getTopic().toString();
            String payload = new String(p.getPayloadAsBytes(), StandardCharsets.UTF_8);
            if (listener != null
                    && (topic.equals(resTopic)
                    || topic.equals(staTopic)
                    || topic.equals(obsTopic))) {
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
        } catch (Exception e) { Log.e(TAG,"connect error: "+e.getMessage()); }
    }

    public void afterConnected() {
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
        } catch (Exception ignore) {}
    }

    public void gracefulDisconnect() {
        if (mqtt!=null) mqtt.disconnect();
    }

    public void publish(String topic, String payload, int qos, boolean retain) {
        if (mqtt == null) return;
        mqtt.publishWith()
                .topic(topic)
                .qos(qos == 2 ? com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
                        : qos == 1 ? com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE
                        : com.hivemq.client.mqtt.datatypes.MqttQos.AT_MOST_ONCE)
                .retain(retain)
                .payload(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .send();
    }

    public String getReqTopic() {
        return reqTopic;
    }

    public String getResTopic() {
        return resTopic;
    }

    public String getStaTopic() {
        return staTopic;
    }

    public String getObsTopic() {
        return obsTopic;
    }
}
