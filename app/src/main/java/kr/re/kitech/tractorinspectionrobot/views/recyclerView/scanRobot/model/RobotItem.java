package kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.model;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotItem {

    private long id;
    private String deviceName;
    private String mqttConnectUrl;
    private int port;
    private String mqttUsername;
    private String mqttPassword;
    private String mqttRootTopic;
    private String mqttBaseTopic;

    // ✅ 현재 이 로봇이 연결 중인지 표시
    private boolean connected = false;

    private JSONObject obj;

    public RobotItem(long id,
                     String deviceName,
                     String mqttConnectUrl,
                     int port,
                     String mqttUsername,
                     String mqttPassword,
                     String mqttRootTopic,
                     String mqttBaseTopic) {
        this.id = id;
        this.deviceName = deviceName;
        this.mqttConnectUrl = mqttConnectUrl;
        this.port = port;
        this.mqttUsername = mqttUsername;
        this.mqttPassword = mqttPassword;
        this.mqttRootTopic = mqttRootTopic;
        this.mqttBaseTopic = mqttBaseTopic;
    }

    public RobotItem(JSONObject obj) throws JSONException {
        this.id                 = obj.getInt("id");
        this.deviceName         = obj.getString("deviceName");
        this.mqttConnectUrl     = obj.getString("mqttConnectUrl");
        this.port               = obj.getInt("port");
        this.mqttUsername       = obj.getString("mqttUsername");
        this.mqttPassword       = obj.getString("mqttPassword");
        this.mqttRootTopic      = obj.getString("mqttRootTopic");
        this.mqttBaseTopic      = obj.getString("mqttBaseTopic");
        this.obj = obj;
    }

    public RobotItem(String deviceName,
                     String mqttConnectUrl,
                     int port,
                     String mqttUsername,
                     String mqttPassword,
                     String mqttRootTopic,
                     String mqttBaseTopic) {
        this(-1, deviceName, mqttConnectUrl, port, mqttUsername, mqttPassword, mqttRootTopic, mqttBaseTopic);
    }

    public JSONObject getObject() throws JSONException {
        JSONObject newItem = new JSONObject();
        newItem.put("id", this.id);
        newItem.put("deviceName", this.deviceName);
        newItem.put("mqttConnectUrl", this.mqttConnectUrl);
        newItem.put("port", this.port);
        newItem.put("mqttUsername", this.mqttUsername);
        newItem.put("mqttPassword", this.mqttPassword);
        newItem.put("mqttRootTopic", this.mqttRootTopic);
        newItem.put("mqttBaseTopic", this.mqttBaseTopic);
        // connected는 UI용 상태라 JSON에는 안 넣어도 됨
        return newItem;
    }
}
