package kr.re.kitech.tractorinspectionrobot.listener.toggle;

import android.view.View;

public class MultiSeqToggleItem {
    public View button;
    public int key;
    public Runnable beforeSend; // 전처리
    public Runnable afterSend; // 후처리

    private MultiSeqToggleItem(View button, int key, Runnable beforeSend, Runnable afterSend) {
        this.button = button;
        this.key = key;
        this.beforeSend = beforeSend;
        this.afterSend = afterSend;
    }

    public static MultiSeqToggleItem add(View button, int key) {
        return new MultiSeqToggleItem(button, key, null,null);
    }

    public static MultiSeqToggleItem add(View button, int key, Runnable beforeSend, Runnable afterSend) {
        return new MultiSeqToggleItem(button, key, beforeSend, afterSend);
    }
}
