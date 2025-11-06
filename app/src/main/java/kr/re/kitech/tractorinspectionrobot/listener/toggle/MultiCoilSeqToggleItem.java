package kr.re.kitech.tractorinspectionrobot.listener.toggle;

import android.view.View;

public class MultiCoilSeqToggleItem {
    public View button;
    public int key;
    public Runnable beforeSend; // 전처리
    public Runnable afterSend; // 후처리

    private MultiCoilSeqToggleItem(View button, int key, Runnable beforeSend, Runnable afterSend) {
        this.button = button;
        this.key = key;
        this.beforeSend = beforeSend;
        this.afterSend = afterSend;
    }

    public static MultiCoilSeqToggleItem add(View button, int key) {
        return new MultiCoilSeqToggleItem(button, key, null,null);
    }

    public static MultiCoilSeqToggleItem add(View button, int key, Runnable beforeSend, Runnable afterSend) {
        return new MultiCoilSeqToggleItem(button, key, beforeSend, afterSend);
    }
}
