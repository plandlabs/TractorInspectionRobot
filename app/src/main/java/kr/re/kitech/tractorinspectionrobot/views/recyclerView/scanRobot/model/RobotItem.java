package kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.model;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotItem {
    private String ssid;
    private String mac;
    private String password = "";
    private Boolean isScan = false;
    private Boolean isConnected = false;
    private Boolean isNotNetworkConnected = false;
    private Boolean isSaved = true;
    private String robot = "";
    private int rssi = Integer.MIN_VALUE;
    private int freq = 0;

    public RobotItem(){

    }
    public RobotItem(JSONObject arr) throws JSONException {
        this.ssid = arr.getString("ssid");
        this.mac = arr.getString("mac");
        this.robot = arr.getString("robot");
        if (arr.has("password")) {
            if (!arr.getString("password").isEmpty()) {
                this.password = arr.getString("password");
            }
        }
    }

    public JSONObject get(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ssid", this.ssid);
            jsonObject.put("mac", this.mac);
            jsonObject.put("robot", this.robot);
            jsonObject.put("password", this.password);
            jsonObject.put("isScan", this.isScan);
            jsonObject.put("isConnected", this.isConnected);
            jsonObject.put("isNotNetworkConnected", this.isNotNetworkConnected);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }
}
