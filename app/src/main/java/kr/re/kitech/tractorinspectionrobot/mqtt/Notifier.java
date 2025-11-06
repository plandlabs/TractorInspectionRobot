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

알림 채널 생성 및 startForeground에 쓸 알림 생성.

상태 변동 시 텍스트 업데이트(“Initializing…/Connected/Reconnecting…/Disconnected”).
*/
public class Notifier {
    private final Context ctx;
    private final String channelId;
    private final int notiId;

    public Notifier(Context ctx, String channelId, int notiId) {
        this.ctx = ctx.getApplicationContext();
        this.channelId = channelId; this.notiId = notiId;
    }

    public void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(channelId, "MQTT", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    public Notification build(String text) {
        Notification.Builder b = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(ctx, channelId) : new Notification.Builder(ctx);
        return b.setContentTitle("Wear MQTT").setContentText(text)
                .setSmallIcon(R.drawable.outline_action_key_24).setOngoing(true).build();
    }

    public void update(String text) {
        ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(notiId, build(text));
    }
}