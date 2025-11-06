package kr.re.kitech.tractorinspectionrobot.views.recyclerView.logError.model;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorLogItem {
    private int num;
    private String datetime;
    private String type;
    private int addr;
    private boolean value;
    private String message;

    public ErrorLogItem(JSONObject arr) throws JSONException {
        this.datetime = arr.getString("datetime");
        this.type = arr.getString("type");
        this.addr = arr.getInt("addr");
        this.value = arr.getBoolean("value");
        this.message = arr.getString("message");
    }
}
