package kr.re.kitech.tractorinspectionrobot.ui;

import android.widget.Toast;

public final class BackPressHandler {
    private static final long INTERVAL_MS = 1500L; // 두 번 누르기 허용 간격
    private long lastBackPressed = 0L;

    /** true를 반환하면 백키 이벤트를 소모(consumed)했다고 보면 됩니다. */
    public boolean handle(Runnable finishAction, Toast toast) {
        long now = System.currentTimeMillis();
        if (now - lastBackPressed <= INTERVAL_MS) {
            try { toast.cancel(); } catch (Exception ignored) {}
            finishAction.run();
            return true;
        } else {
            lastBackPressed = now;
            toast.show();
            return true;
        }
    }

    public void reset() { lastBackPressed = 0L; }
}
