package kr.re.kitech.tractorinspectionrobot.mqtt.shared.item;

import org.json.JSONException;
import org.json.JSONObject;import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotState {
    private int num;
    private int move = 0;
    public final int x, y, z, s1, s2, s3; // 필드명 변경 및 s3 추가
    public final long ts;

    public RobotState(JSONObject object) throws JSONException {
        this.x = object.getInt("x");
        this.y = object.getInt("y");
        this.z = object.getInt("z");
        this.s1 = object.getInt("s1"); // panDeg -> s1
        this.s2 = object.getInt("s2"); // tiltDeg -> s2
        this.s3 = object.getInt("s3"); // s3 추가
        this.ts = object.getLong("ts");
        this.move = 0;
    }
    public RobotState(int x, int y, int z, int s1, int s2, int s3, long ts) {
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
                    .put("ts", ts)
                    .put("move", move);
        } catch (Exception e) { return new JSONObject(); }
    }
    public static RobotState fromJson(JSONObject o, RobotState fallback) throws JSONException {
        int x         = o.optInt("x",         fallback == null ? 0 : fallback.x);
        int y         = o.optInt("y",         fallback == null ? 0 : fallback.y);
        int z         = o.optInt("z",         fallback == null ? 0 : fallback.z);
        int s1        = o.optInt("s1",        fallback == null ? 0 : fallback.s1); // panDeg -> s1
        int s2        = o.optInt("s2",        fallback == null ? 0 : fallback.s2); // tiltDeg -> s2
        int s3        = o.optInt("s3",        fallback == null ? 0 : fallback.s3); // s3 추가
        long   ts        = o.optLong("ts",          System.currentTimeMillis());
        // BUG FIX: 생성자에 직접 값을 전달하여 JSONException 방지
        return new RobotState(x, y, z, s1, s2, s3, ts);
    }

    public static RobotState clamp(RobotState s) {
        return new RobotState(
                clamp(s.x,   0, 44000),
                clamp(s.y,   0, 25000),
                clamp(s.z,   0, 3500),   // 필요 시 수정
                clamp(s.s1,  0, 180), // s1
                clamp(s.s2, 0, 180), // s2
                clamp(s.s3, 0, 180), // s3
                s.ts
        );
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
