package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotState {
    public final double x, y, z, pan, tilt;
    public final long ts;

    public RobotState(JSONObject object) throws JSONException {
        this.x = object.getDouble("x");
        this.y = object.getDouble("y");
        this.z = object.getDouble("z");
        this.pan = object.getDouble("pan");
        this.tilt = object.getDouble("tilt");
        this.ts = object.getLong("ts");
    }
    public RobotState(double x, double y, double z, double pan, double tilt, long ts) {
        this.x = x; this.y = y; this.z = z; this.pan = pan; this.tilt = tilt; this.ts = ts;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("x", x)
                    .put("y", y)
                    .put("z", z)
                    .put("pan", pan)
                    .put("tilt", tilt)
                    .put("ts", ts);
        } catch (Exception e) { return new JSONObject(); }
    }
    public static RobotState fromJson(JSONObject o, RobotState fallback) throws JSONException {
        double x    = o.optDouble("x",    fallback == null ? 0.0 : fallback.x);
        double y    = o.optDouble("y",    fallback == null ? 0.0 : fallback.y);
        double z    = o.optDouble("z",    fallback == null ? 0.0 : fallback.z);
        double pan  = o.optDouble("pan",  fallback == null ? 0.0 : fallback.pan);
        double tilt = o.optDouble("tilt", fallback == null ? 0.0 : fallback.tilt);
        long   ts   = o.optLong("ts", System.currentTimeMillis());
        return new RobotState(o);
    }

    public static RobotState clamp(RobotState s) {
        return new RobotState(
                clamp(s.x,   0, 1500),
                clamp(s.y,   0, 1500),
                clamp(s.z,   0, 1500),   // 필요 시 수정
                clamp(s.pan,  -0.6, 0.6),
                clamp(s.tilt, -0.4, 0.4),
                s.ts
        );
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}