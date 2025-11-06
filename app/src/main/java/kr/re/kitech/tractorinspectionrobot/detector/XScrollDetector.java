package kr.re.kitech.tractorinspectionrobot.detector;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class XScrollDetector extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return Math.abs(distanceX) > Math.abs(distanceY);
    }
}
