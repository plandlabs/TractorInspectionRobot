package kr.re.kitech.tractorinspectionrobot.views.recyclerView.logWork.model;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkLogItem {
    private int num;
    private String startDatetime;
    private String endDatetime;
    private int diff = 0;
    private String type;
    private int addr;
    private boolean value = false;
    private int holdingAddr;
    private int holdingValue;
    private int field;
    private int floor;
    private boolean forced = false;

    public WorkLogItem(JSONObject arr) throws JSONException {
        this.startDatetime = arr.getString("start_datetime");
        if (arr.has("end_datetime") && !arr.isNull("end_datetime")) {
            this.endDatetime = arr.getString("end_datetime");
        }else{
            this.endDatetime = "-";
        }
        if (arr.has("diff") && !arr.isNull("diff")) {
            this.diff = arr.getInt("diff");
        }
        this.type = arr.getString("type");
        this.addr = arr.getInt("addr");
        this.value = arr.getBoolean("value");
        this.holdingAddr = arr.getInt("holding_addr");
        this.holdingValue = arr.getInt("holding_value");
        this.field = arr.getInt("field");
        this.floor = arr.getInt("floor");
        if (arr.has("forced")) {
            if (arr.getBoolean("forced")) {
                this.forced = arr.getBoolean("forced");
            }
        }
    }
}
