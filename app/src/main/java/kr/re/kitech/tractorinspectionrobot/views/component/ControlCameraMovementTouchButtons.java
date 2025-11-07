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
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        btnUp.setOnTouchListener(
                new BtnTouchUpDownListener(
                        getContext(),
                        "y'",
                        R.color.touch_on,
                        R.color.touch_off,
                        true,
                        1,
                        300
                )
        );
        btnDown.setOnTouchListener(
                new BtnTouchUpDownListener(
                        getContext(),
                        "y'",
                        R.color.touch_on,
                        R.color.touch_off,
                        false,
                        1,
                        300
                )
        );
        btnRightward.setOnTouchListener(
                new BtnTouchUpDownListener(
                        getContext(),
                        "x'",
                        R.color.touch_on,
                        R.color.touch_off,
                        true,
                        1,
                        300
                )
        );
        btnLeftWard.setOnTouchListener(
                new BtnTouchUpDownListener(
                        getContext(),
                        "x'",
                        R.color.touch_on,
                        R.color.touch_off,
                        false,
                        1,
                        300
                )
        );
        // 관찰 가능!
//        viewModel.getSocketService().observe(lifecycleOwner, socketService -> {
//            if (socketService != null) {
//                resCustomSocketService = socketService;
//
//            }
//        });
//        viewModel.getCoilDataMap().observe(lifecycleOwner, coilDataMap -> {
//            if (coilDataMap != null) {
//                resCoilDataMap = coilDataMap;
//                // 예시: 11번 value를 UI에 표시
//
//            }
//        });
//        viewModel.getHoldingDataMap().observe(lifecycleOwner, holdingDataMap -> {
//            if (holdingDataMap != null) {
//                resHoldingDataMap = holdingDataMap;
//                // 예시: 11번 value를 UI에 표시
//                int coil11 = holdingDataMap.getValue(3);
//            }
//        });
    }

}
