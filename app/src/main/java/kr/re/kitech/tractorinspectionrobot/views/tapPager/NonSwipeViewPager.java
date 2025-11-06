package kr.re.kitech.tractorinspectionrobot.views.tapPager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class NonSwipeViewPager extends ViewPager {

    public NonSwipeViewPager(Context context) {
        super(context);
    }

    public NonSwipeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // 스와이프 터치 이벤트를 무시
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false; // 터치 인터셉트 안 함
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false; // 터치 자체도 무시
    }
}
