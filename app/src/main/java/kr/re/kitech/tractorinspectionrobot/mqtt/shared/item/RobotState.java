package kr.re.kitech.tractorinspectionrobot.mqtt.shared.item;

import org.json.JSONException;
import org.json.JSONObject;import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotState {
    private int num;
    public final double x, y, z, s1, s2, s3; // 필드명 변경 및 s3 추가
    public final long ts;

    public RobotState(JSONObject object) throws JSONException {
        this.x = object.getDouble("x");
        this.y = object.getDouble("y");
        this.z = object.getDouble("z");
        this.s1 = object.getDouble("s1"); // panDeg -> s1
        this.s2 = object.getDouble("s2"); // tiltDeg -> s2
        this.s3 = object.getDouble("s3"); // s3 추가
        this.ts = object.getLong("ts");
    }
    public RobotState(double x, double y, double z, double s1, double s2, double s3, long ts) {
        this.x = x; this.y = y; this.z = z; this.s1 = s1; this.s2 = s2; this.s3 = s3; this.ts = ts;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("x", x)
                    .put("y", y)
                    .put("z", z)
                    .put("s1", s1) // panDeg -> s1
                    .put("s2", s2) // tiltDeg -> s2
                    .put("s3", s3) // s3 추가
                    .put("ts", ts);
        } catch (Exception e) { return new JSONObject(); }
    }
    public static RobotState fromJson(JSONObject o, RobotState fallback) throws JSONException {
        double x         = o.optDouble("x",         fallback == null ? 0.0 : fallback.x);
        double y         = o.optDouble("y",         fallback == null ? 0.0 : fallback.y);
        double z         = o.optDouble("z",         fallback == null ? 0.0 : fallback.z);
        double s1        = o.optDouble("s1",        fallback == null ? 0.0 : fallback.s1); // panDeg -> s1
        double s2        = o.optDouble("s2",        fallback == null ? 0.0 : fallback.s2); // tiltDeg -> s2
        double s3        = o.optDouble("s3",        fallback == null ? 0.0 : fallback.s3); // s3 추가
        long   ts        = o.optLong("ts",          System.currentTimeMillis());
        // BUG FIX: 생성자에 직접 값을 전달하여 JSONException 방지
        return new RobotState(x, y, z, s1, s2, s3, ts);
    }

    public static RobotState clamp(RobotState s) {
        return new RobotState(
                clamp(s.x,   0, 1500),
                clamp(s.y,   0, 1500),
                clamp(s.z,   0, 500),   // 필요 시 수정
                clamp(s.s1,  0, 180), // s1
                clamp(s.s2, 0, 180), // s2
                clamp(s.s3, 0, 360), // s3
                s.ts
        );
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
