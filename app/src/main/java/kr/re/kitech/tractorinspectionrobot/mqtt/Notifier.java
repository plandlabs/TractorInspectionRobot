// com/plandlabs/nepesarkmonitwatch/ui/Notifier.java
package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import kr.re.kitech.tractorinspectionrobot.R;

/*
Notifier (포그라운드 알림)

- MQTT 상태 + 프로그램 진행 상태를 함께 표시.
- mqttStatusText, programStatusText를 합쳐서 ContentText/BigText로 사용.
*/
public class Notifier {
    private final Context ctx;
    private final String channelId;
    private final int notiId;
    private final NotificationManager nm;

    // 알림에 표시할 텍스트들
    private String mqttStatusText = "MQTT: 초기화 중";
    private String programStatusText = "";   // 비어 있으면 표시 X

    public Notifier(Context ctx, String channelId, int notiId) {
        this.ctx = ctx.getApplicationContext();
        this.channelId = channelId;
        this.notiId = notiId;
        this.nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    channelId,
                    "MQTT-01",
                    NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(ch);
        }
    }

    // 두 상태를 합쳐서 하나의 문자열로 만든다.
    private String composeText() {
        if (programStatusText == null || programStatusText.isEmpty()) {
            // 프로그램 상태가 없으면 MQTT 상태만
            return mqttStatusText;
        }
        // 두 줄로 보고 싶으면 "\n" 사용
        // return mqttStatusText + "\n" + programStatusText;

        // 한 줄로 보고 싶으면 중간에 · 로 구분
        return mqttStatusText + " · " + programStatusText;
    }

    private Notification buildInternal() {
        String text = composeText();

        Notification.Builder b = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(ctx, channelId)
                : new Notification.Builder(ctx);

        // BigTextStyle을 써서 텍스트가 길어져도 다 보이게
        b.setContentTitle(ctx.getString(R.string.app_name))
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.outline_robot_2_24)
                .setOngoing(true);

        return b.build();
    }

    // ✅ MQTT 상태만 바꾸고 알림 갱신
    public void updateMqttStatus(String text) {
        this.mqttStatusText = text;
        nm.notify(notiId, buildInternal());
    }

    // ✅ 프로그램 상태만 바꾸고 알림 갱신
    public void updateProgramStatus(String text) {
        this.programStatusText = text;
        nm.notify(notiId, buildInternal());
    }

    // ✅ 프로그램 상태만 지우고(MQTT만 표시)
    public void clearProgramStatus() {
        this.programStatusText = "";
        nm.notify(notiId, buildInternal());
    }

    // 기존 코드와의 호환용 (원래 있던 build(text) / update(text) 스타일 유지하려면)
    public Notification build(String text) {
        // 이건 "그냥 한 줄짜리 대신 쓰고 싶을 때" 용
        this.mqttStatusText = text;
        this.programStatusText = "";
        return buildInternal();
    }

    public void update(String text) {
        this.mqttStatusText = text;
        this.programStatusText = "";
        nm.notify(notiId, buildInternal());
    }
}
