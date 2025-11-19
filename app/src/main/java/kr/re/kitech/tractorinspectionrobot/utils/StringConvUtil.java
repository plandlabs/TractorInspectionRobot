package kr.re.kitech.tractorinspectionrobot.utils;


public class StringConvUtil {
    public static String md5(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return s; // 실패하면 원본 문자열 사용
        }
    }
}
