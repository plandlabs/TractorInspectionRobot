package kr.re.kitech.tractorinspectionrobot.utils;


import android.util.Log;

public class StringConvUtil {
    public static String md5(String s) {
        Log.w("topic s", s);
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b & 0xff));
            }
            Log.w("topic sb", sb.toString());
            return sb.toString();
        } catch (Exception e) {
            return s; // 실패하면 원본 문자열 사용
        }
    }
}
