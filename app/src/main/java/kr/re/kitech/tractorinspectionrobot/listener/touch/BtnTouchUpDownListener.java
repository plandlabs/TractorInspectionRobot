package kr.re.kitech.tractorinspectionrobot.listener.touch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

public class BtnTouchUpDownListener implements View.OnTouchListener {

    public interface DeltaRequester {
        void applyDelta(String axis, double delta);
        void onStop();
    }

    private final Context context;
    private final String axisKey;    // "x"|"y"|"z"|"pan"|"tilt"
    private final int colorActive;
    private final int colorDefault;
    private final boolean isIncrease;
    private final double step;       // 증감 단위
    private final int intervalMillis;
    private final DeltaRequester requester;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isPressed = false;
    private volatile boolean isRunning = false;

    public BtnTouchUpDownListener(
            Context context,
            String axisKey,
            int colorActive,
            int colorDefault,
            boolean isIncrease,
            double step,
            int intervalMillis,
            DeltaRequester requester
    ) {
        this.context = context;
        this.axisKey = axisKey;
        this.colorActive = colorActive;
        this.colorDefault = colorDefault;
        this.isIncrease = isIncrease;
        this.step = step;
        this.intervalMillis = intervalMillis;
        this.requester = requester;
    }

    private final Runnable updateTask = new Runnable() {
        @Override public void run() {
            if (!isPressed || !isRunning) return;

            // 주기적으로 진동 + delta 적용
            vibrateTick(12);
            requester.applyDelta(axisKey, isIncrease ? step : -step);

            if (isPressed && isRunning) {
                handler.postDelayed(this, intervalMillis);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                setTint(v, colorActive);

                // ✅ 버튼 누르는 순간 바로 한 번 진동
                vibrateTick(12);

                if (!isRunning) {
                    isRunning = true;
                    handler.post(updateTask); // 즉시 1틱
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float x = event.getX(), y = event.getY();
                if (x < 0 || x > v.getWidth() || y < 0 || y > v.getHeight()) {
                    stopUpdating(v, true);
                } else {
                    if (!isPressed || !isRunning) {
                        isPressed = true;
                        isRunning = true;
                        handler.post(updateTask);
                    }
                    setTint(v, colorActive);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopUpdating(v, true);
                return true;
        }
        return false;
    }

    private void stopUpdating(View v, boolean notifyStop) {
        isPressed = false;
        isRunning = false;
        handler.removeCallbacks(updateTask);
        handler.removeCallbacksAndMessages(null);
        setTint(v, colorDefault);
        if (notifyStop && requester != null) requester.onStop();
    }

    private void setTint(View v, int colorRes) {
        v.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
        );
    }

    private void vibrateTick(int ms) {
        try {
            Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vib == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vib.vibrate(ms);
            }
        } catch (Throwable ignore) {}
    }
}
