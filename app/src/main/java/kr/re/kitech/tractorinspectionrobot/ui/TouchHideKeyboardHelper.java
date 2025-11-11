package kr.re.kitech.tractorinspectionrobot.ui;

import android.app.Activity;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public final class TouchHideKeyboardHelper {
    private TouchHideKeyboardHelper(){}

    public static boolean dispatch(Activity a, MotionEvent ev){
        View f = a.getCurrentFocus();
        if (f == null) return false;
        Rect r = new Rect();
        f.getGlobalVisibleRect(r);
        int x = (int) ev.getX(), y = (int) ev.getY();
        if (!r.contains(x,y)){
            InputMethodManager imm = (InputMethodManager) a.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(f.getWindowToken(),0);
            f.clearFocus();
        }
        return false; // 기본 흐름 계속
    }
}
