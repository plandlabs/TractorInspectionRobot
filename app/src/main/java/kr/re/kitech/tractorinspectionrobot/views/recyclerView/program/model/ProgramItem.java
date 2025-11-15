package kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.model;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgramItem {
    private int num;
    private int vx;
    private int vy;
    private int vz;
    private int cx;
    private int cy;
    private int cz;
    private String datetime;

    public ProgramItem(JSONObject arr) throws JSONException {
        this.vx = arr.getInt("x");
        this.vy = arr.getInt("y");
        this.vz = arr.getInt("z");
        this.cx = arr.getInt("x'");
        this.cy = arr.getInt("y'");
        this.cz = arr.getInt("z'");
        this.datetime = arr.getString("datetime");
    }
}
