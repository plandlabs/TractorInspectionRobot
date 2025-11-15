package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import org.json.JSONException;
import org.json.JSONObject;import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotState {
    public final double x, y, z, xPrimeDeg, yPrimeDeg, zPrimeDeg; // 필드명 변경 및 zPrimeDeg 추가
    public final long ts;

    public RobotState(JSONObject object) throws JSONException {
        this.x = object.getDouble("x");
        this.y = object.getDouble("y");
        this.z = object.getDouble("z");
        this.xPrimeDeg = object.getDouble("xPrimeDeg"); // panDeg -> xPrimeDeg
        this.yPrimeDeg = object.getDouble("yPrimeDeg"); // tiltDeg -> yPrimeDeg
        this.zPrimeDeg = object.getDouble("zPrimeDeg"); // zPrimeDeg 추가
        this.ts = object.getLong("ts");
    }
    public RobotState(double x, double y, double z, double xPrimeDeg, double yPrimeDeg, double zPrimeDeg, long ts) {
        this.x = x; this.y = y; this.z = z; this.xPrimeDeg = xPrimeDeg; this.yPrimeDeg = yPrimeDeg; this.zPrimeDeg = zPrimeDeg; this.ts = ts;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("x", x)
                    .put("y", y)
                    .put("z", z)
                    .put("xPrimeDeg", xPrimeDeg) // panDeg -> xPrimeDeg
                    .put("yPrimeDeg", yPrimeDeg) // tiltDeg -> yPrimeDeg
                    .put("zPrimeDeg", zPrimeDeg) // zPrimeDeg 추가
                    .put("ts", ts);
        } catch (Exception e) { return new JSONObject(); }
    }
    public static RobotState fromJson(JSONObject o, RobotState fallback) throws JSONException {
        double x         = o.optDouble("x",         fallback == null ? 0.0 : fallback.x);
        double y         = o.optDouble("y",         fallback == null ? 0.0 : fallback.y);
        double z         = o.optDouble("z",         fallback == null ? 0.0 : fallback.z);
        double xPrimeDeg = o.optDouble("xPrimeDeg", fallback == null ? 0.0 : fallback.xPrimeDeg); // panDeg -> xPrimeDeg
        double yPrimeDeg = o.optDouble("yPrimeDeg", fallback == null ? 0.0 : fallback.yPrimeDeg); // tiltDeg -> yPrimeDeg
        double zPrimeDeg = o.optDouble("zPrimeDeg", fallback == null ? 0.0 : fallback.zPrimeDeg); // zPrimeDeg 추가
        long   ts        = o.optLong("ts", System.currentTimeMillis());
        // BUG FIX: 생성자에 직접 값을 전달하여 JSONException 방지
        return new RobotState(x, y, z, xPrimeDeg, yPrimeDeg, zPrimeDeg, ts);
    }

    public static RobotState clamp(RobotState s) {
        return new RobotState(
                clamp(s.x,   0, 1500),
                clamp(s.y,   0, 1500),
                clamp(s.z,   0, 500),   // 필요 시 수정
                clamp(s.xPrimeDeg,  0, 180), // xPrimeDeg
                clamp(s.yPrimeDeg, 0, 180), // yPrimeDeg
                clamp(s.zPrimeDeg, 0, 360), // zPrimeDeg
                s.ts
        );
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
