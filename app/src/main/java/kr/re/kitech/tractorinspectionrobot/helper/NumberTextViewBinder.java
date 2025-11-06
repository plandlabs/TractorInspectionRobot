package kr.re.kitech.tractorinspectionrobot.helper;

import android.view.animation.Interpolator;
import android.widget.TextView;

/**
 * TextView에 숫자 애니메이션(업/다운) 적용 + 상태 자동 관리
 */
public class NumberTextViewBinder {
    private int lastIntValue;
    private float lastFloatValue;
    private boolean isFloatMode;

    private final TextView textView;
    private final NumberFormatter formatter;
    private final long duration;
    private final Interpolator interpolator;

    // int 초기화
    public NumberTextViewBinder(TextView textView, int initialValue, long duration, NumberFormatter formatter, Interpolator interpolator) {
        this.textView = textView;
        this.lastIntValue = initialValue;
        this.lastFloatValue = initialValue;
        this.formatter = formatter;
        this.duration = duration;
        this.interpolator = interpolator;
        this.isFloatMode = false;
        textView.setText(formatter.format(initialValue));
    }

    // float/double 초기화
    public NumberTextViewBinder(TextView textView, float initialValue, long duration, NumberFormatter formatter, Interpolator interpolator) {
        this.textView = textView;
        this.lastIntValue = (int) initialValue;
        this.lastFloatValue = initialValue;
        this.formatter = formatter;
        this.duration = duration;
        this.interpolator = interpolator;
        this.isFloatMode = true;
        textView.setText(formatter.formatFloat(initialValue));
    }

    // int값 update (float모드에서도 자동 전환)
    public void update(int newValue) {
        if (isFloatMode) {
            update((float) newValue);
            return;
        }
        if (lastIntValue != newValue) {
            NumberAnimator.animate(textView, lastIntValue, newValue, duration, formatter, interpolator);
        } else {
            textView.setText(formatter.format(newValue));
        }
        lastIntValue = newValue;
        lastFloatValue = newValue;
    }

    // float/double값 update (애니메이션 소수점까지)
    public void update(float newValue) {
        isFloatMode = true;
        if (lastFloatValue != newValue) {
            NumberAnimator.animate(textView, lastFloatValue, newValue, duration, formatter, interpolator);
        } else {
            textView.setText(formatter.formatFloat(newValue));
        }
        lastFloatValue = newValue;
        lastIntValue = (int) newValue;
    }

    public void update(double newValue) {
        update((float) newValue);
    }

    // 편의상 초기화+최초값 갱신까지 한 번에 (딱 1회만! 반복 호출 금지)
    public static NumberTextViewBinder bindNumber(TextView tv, Number value, long duration, NumberFormatter formatter, Interpolator interpolator) {
        if (value instanceof Float || value instanceof Double) {
            return new NumberTextViewBinder(tv, value.floatValue(), duration, formatter, interpolator);
        } else {
            return new NumberTextViewBinder(tv, value.intValue(), duration, formatter, interpolator);
        }
    }

    // 마지막 값 조회
    public int getLastIntValue() { return lastIntValue; }
    public float getLastFloatValue() { return lastFloatValue; }
}
