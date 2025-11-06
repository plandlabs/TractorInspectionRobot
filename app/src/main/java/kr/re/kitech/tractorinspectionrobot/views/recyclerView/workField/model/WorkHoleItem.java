package kr.re.kitech.tractorinspectionrobot.views.recyclerView.workField.model;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkHoleItem {
    private int num;
    private int type = 0;
    private int area;
    private boolean working = false;

    public WorkHoleItem() {
    }
    public WorkHoleItem(JSONObject arr) throws JSONException {
        this.num = arr.getInt("num");
        this.type = arr.getInt("type");
        this.area = arr.getInt("area");
    }
}
