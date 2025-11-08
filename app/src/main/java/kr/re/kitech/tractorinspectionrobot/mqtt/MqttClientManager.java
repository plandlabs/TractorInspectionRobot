// com/plandlabs/nepesarkmonitwatch/mqtt/MqttClientManager.java
package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.util.Log;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
/*
MqttClientManager (MQTT 전담)

HiveMQ Mqtt3AsyncClient 초기화/자동재연결/콜백 관리.

connect() 시 LWT(offline) 설정 후 연결, afterConnected()에서 구독·presence·register 처리.

publishJson()/publishText()로 모든 퍼블리시 통로 단일화.

수신 토픽 라우팅: register/reject, pong/<name>, direct/<name>/# → 외부 Listener에 전달.

gracefulDisconnect()로 정상 종료(offline retain 후 disconnect).
 */
public class MqttClientManager {
    public interface Listener {
        void onConnected();
        void onDisconnected(Throwable cause);
        void onReject(String payload);
        void onDirect(String subTopic, String payload);
    }

    private static final String TAG = "MqttClientManager";
    private final String host;
    private final int port;
    private final String name;
    private final String clientId;
    private Mqtt3AsyncClient mqtt;
    private volatile boolean connected = false;
    private Listener listener;

    public MqttClientManager(String url, String name, String clientId) {
        String s = url.replace("tcp://","").replace("ssl://","");
        int idx = s.indexOf(':'); this.host = (idx>0)? s.substring(0,idx):s; this.port = (idx>0)? Integer.parseInt(s.substring(idx+1)):1883;
        this.name = name; this.clientId = clientId;
    }
    public void setListener(Listener l){ this.listener = l; }
    public boolean isConnected(){ return connected; }

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
            if (topic.equals("register/reject/" + clientId)) { if (listener!=null) listener.onReject(payload); }
            else if (topic.startsWith("direct/" + name + "/")) {
                String sub = topic.substring(("direct/" + name + "/").length());
                if (listener!=null) listener.onDirect(sub, payload);
            } else if (topic.startsWith("robot/simulation/")) {
                if (listener!=null) listener.onDirect(topic, payload); // subTopic 자리에 full topic 전달
            }
        });
    }

    public void connect() {
        if (mqtt == null) return;
        try {
            JSONObject will = new JSONObject().put("name", name).put("clientId", clientId).put("status","offline").put("ts", System.currentTimeMillis());
            mqtt.connectWith().keepAlive(90).cleanSession(false)
                    .willPublish().topic("presence/"+clientId).qos(MqttQos.AT_LEAST_ONCE).retain(true)
                    .payload(will.toString().getBytes()).applyWillPublish().send();
        } catch (Exception e) { Log.e(TAG,"connect error: "+e.getMessage()); }
    }

    public void afterConnected() {
        try {
            mqtt.subscribeWith().topicFilter("register/reject/"+clientId).qos(MqttQos.AT_LEAST_ONCE).send();
            mqtt.subscribeWith().topicFilter("direct/"+name+"/#").qos(MqttQos.AT_LEAST_ONCE).send();
            mqtt.subscribeWith().topicFilter("pong/"+name).qos(MqttQos.AT_MOST_ONCE).send();
            mqtt.subscribeWith().topicFilter("robot/simulation/" + name).qos(MqttQos.AT_MOST_ONCE).send();

            publishJson("register", new JSONObject().put("name", name), MqttQos.AT_LEAST_ONCE,false);
            publishJson("presence/"+clientId, new JSONObject().put("name", name).put("status","online").put("ts", System.currentTimeMillis()), MqttQos.AT_LEAST_ONCE,true);
        } catch (Exception ignore) {}
    }

    public void gracefulDisconnect() {
        try {
            publishJson("presence/"+clientId, new JSONObject().put("name", name).put("status","offline").put("ts", System.currentTimeMillis()), MqttQos.AT_LEAST_ONCE,true);
        } catch (Exception ignore) {}
        if (mqtt!=null) mqtt.disconnect();
    }

    public void publishJson(String topic, JSONObject json, MqttQos qos, boolean retain) {
        mqtt.publishWith().topic(topic).qos(qos).retain(retain).payload(json.toString().getBytes()).send();
    }
    public void publishText(String topic, String text, MqttQos qos, boolean retain) {
        mqtt.publishWith().topic(topic).qos(qos).retain(retain).payload(text.getBytes()).send();
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
}
