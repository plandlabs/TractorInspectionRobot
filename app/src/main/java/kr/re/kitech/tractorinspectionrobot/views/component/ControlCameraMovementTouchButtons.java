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
    public Button btn_y_up, btn_y_down, btnForward, btnBackward, btn_x_up, btn_x_down;
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
        btn_y_up = findViewById(R.id.btn_y_up);
        btn_y_down = findViewById(R.id.btn_y_down);

        btn_x_up = findViewById(R.id.btn_x_up);
        btn_x_down = findViewById(R.id.btn_x_down);

        btnBackward = findViewById(R.id.btn_backward);

        btnForward = findViewById(R.id.btn_forward);
        btnBackward = findViewById(R.id.btn_backward);
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
//                viewModel.publishCurrent(deviceName);
            }
        };
        final int on = R.color.touch_on;
        final int off = R.color.touch_off;
        final float step = Float.parseFloat(getContext().getString(R.string.camera_move_step));
        final int intervalMillis = Integer.parseInt(getContext().getString(R.string.interval_millis_c));

        // xPrimeDeg (카메라 헤드 서보모터 상/하) - s1에서 변경
        btn_x_up.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "xPrimeDeg", on, off, true, step, intervalMillis, req));
        btn_x_down.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "xPrimeDeg", on, off, false, step, intervalMillis, req));

        // yPrimeDeg (중앙 서보모터 상/하) - s2에서 변경
        btn_y_up.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "yPrimeDeg", on, off, true, step, intervalMillis, req));
        btn_y_down.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "yPrimeDeg", on, off, false, step, intervalMillis, req));

        // zPrimeDeg (끝 서보모터 회전) - s3에서 변경
        btnForward.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "zPrimeDeg", on, off, true, step, intervalMillis, req));
        btnBackward.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "zPrimeDeg", on, off, false, step, intervalMillis, req));
    }

}
