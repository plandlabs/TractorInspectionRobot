package kr.re.kitech.tractorinspectionrobot.helper;

public class SpeedConverter {
    // 변환 상수: 1 rpm = 0.00023 m/s
    // 변환 상수: 50 rpm = 0.0115 m/s
    private static final double CONVERSION_FACTOR = 0.00023;

    public static double rpmToMps(int rpm) {
        return rpm * CONVERSION_FACTOR;
    }

    public static double rpmToMps(double rpm) {
        return rpm * CONVERSION_FACTOR;
    }
}
