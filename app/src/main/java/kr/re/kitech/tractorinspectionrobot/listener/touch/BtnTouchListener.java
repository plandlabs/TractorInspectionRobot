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

public class BtnTouchListener implements View.OnTouchListener {

    public interface DeltaRequester {
        void applyDelta(String axis, int delta);
        void onStop();
    }

    private final Context context;
    private final String axisKey;
    private final int colorActive;
    private final int colorDefault;
    private final boolean isIncrease;
    private final int step;
    private final DeltaRequester requester;

    public BtnTouchListener(
            Context context,
            String axisKey,
            int colorActive,
            int colorDefault,
            boolean isIncrease,
            int step,
            DeltaRequester requester
    ) {
        this.context = context;
        this.axisKey = axisKey;
        this.colorActive = colorActive;
        this.colorDefault = colorDefault;
        this.isIncrease = isIncrease;
        this.step = step;
        this.requester = requester;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                // 색 변경 + 진동
                setTint(v, colorActive);
                vibrateTick(20);

                // 🔥 단 1회만 명령 실행
                if (requester != null) {
                    requester.applyDelta(axisKey, isIncrease ? step : -step);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 색 되돌리기 + 정지 알림
                setTint(v, colorDefault);
                if (requester != null) requester.onStop();
                return true;
        }
        return false;
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

