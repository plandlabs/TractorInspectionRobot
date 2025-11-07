package kr.re.kitech.tractorinspectionrobot.listener.touch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.function.Supplier;

public class BtnTouchUpDownListener implements View.OnTouchListener {
    private final Context context;
    private final String key;
    private final int colorActive;
    private final int colorDefault;
    private final boolean isIncrease;
    private final int step;
    private final int intervalMillis;

    private Handler handler = new Handler();
    private int currentValue;
    private boolean isPressed = false;
    private Supplier<Integer> currentValueSupplier = null;
    private boolean isRunning = false;

    public BtnTouchUpDownListener(Context context, String key, int colorActive, int colorDefault,
                                  boolean isIncrease, int step, int intervalMillis) {
        this.context = context;
        this.key = key;
        this.colorActive = colorActive;
        this.colorDefault = colorDefault;
        this.isIncrease = isIncrease;
        this.step = step;
        this.intervalMillis = intervalMillis;
    }

    public BtnTouchUpDownListener setCurrentValueSupplier(Supplier<Integer> supplier) {
        this.currentValueSupplier = supplier;
        return this;
    }

    private final Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            if (!isPressed || !isRunning) {
                Log.w("DEBUG", "Stopped - isPressed=" + isPressed + ", isRunning=" + isRunning);
                return;
            } else {
                Log.w("DEBUG", "RUNNING - isPressed=" + isPressed + ", isRunning=" + isRunning);
            }
            Vibrator mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            mVibrator.vibrate(100);

            currentValue += isIncrease ? step : -step;

            // 💡 여기서 다시 조건 체크해서 post
            if (isPressed && isRunning) {
                handler.postDelayed(this, intervalMillis);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                // 여기서 값을 가져와야 매번 갱신 가능
                if (currentValueSupplier != null) {
                    try {
                        currentValue = currentValueSupplier.get();
                    } catch (Exception e) {
                        currentValue = 0;
                    }
                }

                v.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorActive)));
                if (!isRunning) {
                    isRunning = true;
                    handler.post(updateTask);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopUpdating(v);
                break;

            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                if (x < 0 || x > v.getWidth() || y < 0 || y > v.getHeight()) {
                    stopUpdating(v);
                } else {
                    if (!isPressed || !isRunning) {
                        isPressed = true;
                        isRunning = true;
                        handler.post(updateTask);
                    }
                    v.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorActive)));
                }
                break;
        }
        return true;
    }
    private void stopUpdating(View v) {
        isPressed = false;
        isRunning = false;
        handler.removeCallbacks(updateTask); // ✅ 이건 유지
        handler.removeCallbacksAndMessages(null); // ✅ 모든 메시지 제거 (혹시 남아있을 때 대비)
        v.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorDefault)));
    }

}
