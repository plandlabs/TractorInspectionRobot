package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotState {
    public final double x, y, z, panDeg, tiltDeg;
    public final long ts;

    public RobotState(JSONObject object) throws JSONException {
        this.x = object.getDouble("x");
        this.y = object.getDouble("y");
        this.z = object.getDouble("z");
        this.panDeg = object.getDouble("panDeg");
        this.tiltDeg = object.getDouble("tiltDeg");
        this.ts = object.getLong("ts");
    }
    public RobotState(double x, double y, double z, double panDeg, double tiltDeg, long ts) {
        this.x = x; this.y = y; this.z = z; this.panDeg = panDeg; this.tiltDeg = tiltDeg; this.ts = ts;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("x", x)
                    .put("y", y)
                    .put("z", z)
                    .put("panDeg", panDeg)
                    .put("tiltDeg", tiltDeg)
                    .put("ts", ts);
        } catch (Exception e) { return new JSONObject(); }
    }
    public static RobotState fromJson(JSONObject o, RobotState fallback) throws JSONException {
        double x    = o.optDouble("x",    fallback == null ? 0.0 : fallback.x);
        double y    = o.optDouble("y",    fallback == null ? 0.0 : fallback.y);
        double z    = o.optDouble("z",    fallback == null ? 0.0 : fallback.z);
        double panDeg  = o.optDouble("panDeg",  fallback == null ? 0.0 : fallback.panDeg);
        double tiltDeg = o.optDouble("tiltDeg", fallback == null ? 0.0 : fallback.tiltDeg);
        long   ts   = o.optLong("ts", System.currentTimeMillis());
        return new RobotState(o);
    }

    public static RobotState clamp(RobotState s) {
        return new RobotState(
                clamp(s.x,   -750, 750),
                clamp(s.y,   -750, 750),
                clamp(s.z,   0, 500),   // 필요 시 수정
                clamp(s.panDeg,  -60, 60),
                clamp(s.tiltDeg, -60, 60),
                s.ts
        );
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}