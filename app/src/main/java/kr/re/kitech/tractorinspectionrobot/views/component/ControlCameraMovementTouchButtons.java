package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.listener.touch.BtnTouchUpDownListener;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

public class ControlCameraMovementTouchButtons extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    public Button btnUp, btnDown, btnLeftWard, btnRightward;
    private String deviceName;


    public ControlCameraMovementTouchButtons(Context context) {
        super(context);
        init(context);
    }

    public ControlCameraMovementTouchButtons(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        inflate(context, R.layout.component_control_camera_movement_touch_buttons, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        btnUp = findViewById(R.id.btn_up);
        btnDown = findViewById(R.id.btn_down);
        btnLeftWard = findViewById(R.id.btn_leftward);
        btnRightward = findViewById(R.id.btn_rightward);
        deviceName = context.getString(R.string.controller_name);
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        BtnTouchUpDownListener.DeltaRequester req = new BtnTouchUpDownListener.DeltaRequester() {
            @Override
            public void applyDelta(String axis, double delta) {
                viewModel.applyDeltaAndPublish(deviceName, axis, delta);
            }

            @Override
            public void onStop() {
                // 선택: 손 뗄 때 현재 전체 상태 한 번 더 전송
                viewModel.publishCurrent(deviceName);
            }
        };
        final int on = R.color.touch_on;
        final int off = R.color.touch_off;
        final float step = Float.parseFloat(getContext().getString(R.string.camera_move_step));
        final int intervalMillis = Integer.parseInt(getContext().getString(R.string.interval_millis));

        // tiltDeg (상/하)
        btnUp.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "tiltDeg", on, off, true, step, intervalMillis, req));
        btnDown.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "tiltDeg", on, off, false, step, intervalMillis, req));

        // panDeg
        btnRightward.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "panDeg", on, off, true, step, intervalMillis, req));
        btnLeftWard.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "panDeg", on, off, false, step, intervalMillis, req));

    }

}
