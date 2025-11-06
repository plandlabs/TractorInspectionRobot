package kr.re.kitech.tractorinspectionrobot.helper;

import android.animation.ValueAnimator;
import android.view.animation.Interpolator;
import android.widget.TextView;

import java.text.DecimalFormat;

public class NumberAnimator {

    // int용
    public static void animate(final TextView textView,
                               final int fromValue,
                               final int toValue,
                               long duration,
                               final NumberFormatter formatter,
                               Interpolator interpolator) {

        ValueAnimator animator = ValueAnimator.ofInt(fromValue, toValue);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            if (formatter != null) {
                textView.setText(formatter.format(value));
            } else {
                textView.setText(String.valueOf(value));
            }
        });
        animator.start();
    }

    // float용
    public static void animate(final TextView textView,
                               final float fromValue,
                               final float toValue,
                               long duration,
                               final NumberFormatter formatter,
                               Interpolator interpolator) {

        ValueAnimator animator = ValueAnimator.ofFloat(fromValue, toValue);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (formatter != null) {
                textView.setText(formatter.formatFloat(value));
            } else {
                textView.setText(String.valueOf(value));
            }
        });
        animator.start();
    }

    /**
     * int → 콤마포맷
     */
    public static void animateWithComma(final TextView textView,
                                        final int fromValue,
                                        final int toValue,
                                        long duration,
                                        Interpolator interpolator) {
        DecimalFormat numberFormat = new DecimalFormat("#,###");
        animate(textView, fromValue, toValue, duration,
                new NumberFormatter() {
                    @Override
                    public String format(int value) {
                        return numberFormat.format(value);
                    }
                    @Override
                    public String formatFloat(float value) {
                        return numberFormat.format(value); // fallback, not used here
                    }
                },
                interpolator
        );
    }

    /**
     * float/double → 콤마포맷 + 소수점(최대 2자리)
     */
    public static void animateWithComma(final TextView textView,
                                        final float fromValue,
                                        final float toValue,
                                        long duration,
                                        Interpolator interpolator) {
        DecimalFormat numberFormat = new DecimalFormat("#,###.##");
        animate(textView, fromValue, toValue, duration,
                new NumberFormatter() {
                    @Override
                    public String format(int value) {
                        return numberFormat.format(value);
                    }
                    @Override
                    public String formatFloat(float value) {
                        return numberFormat.format(value);
                    }
                },
                interpolator
        );
    }

    // double 오버로드 필요하면 float과 동일하게 작성하면 됩니다.
    public static void animateWithComma(final TextView textView,
                                        final double fromValue,
                                        final double toValue,
                                        long duration,
                                        Interpolator interpolator) {
        DecimalFormat numberFormat = new DecimalFormat("#,###.##");
        animate(textView, (float) fromValue, (float) toValue, duration,
                new NumberFormatter() {
                    @Override
                    public String format(int value) {
                        return numberFormat.format(value);
                    }
                    @Override
                    public String formatFloat(float value) {
                        return numberFormat.format(value);
                    }
                },
                interpolator
        );
    }
}