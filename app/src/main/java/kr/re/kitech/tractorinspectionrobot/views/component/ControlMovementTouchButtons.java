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

import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

public class ControlMovementTouchButtons extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    public Button btnForward, btnBackward, btnLeftWard, btnRightward;


    public ControlMovementTouchButtons(Context context) {
        super(context);
        init(context);
    }

    public ControlMovementTouchButtons(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        inflate(context, R.layout.component_control_movement_touch_buttons, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        btnForward = findViewById(R.id.btn_forward);
        btnBackward = findViewById(R.id.btn_backward);
        btnLeftWard = findViewById(R.id.btn_leftward);
        btnRightward = findViewById(R.id.btn_rightward);
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

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
