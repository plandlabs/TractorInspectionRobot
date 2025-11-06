package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.helper.NumberFormatter;
import kr.re.kitech.tractorinspectionrobot.helper.NumberTextViewBinder;

import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

import java.text.DecimalFormat;

public class ControlSpeedMonitAndButtons extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private Button btnSpeedUp, btnSpeedDown,
            btnSpeedLow, btnSpeedMiddle, btnSpeedHigh,
            btnAngleZero;
    private TextView disSpeed, topFrameAngle;
    private DecimalFormat numberFormat = new DecimalFormat("#,###.####");
    private NumberFormatter numberFormatter = new NumberFormatter() {
        public String format(int value) { return numberFormat.format(value); }
        public String formatFloat(float value) { return numberFormat.format(value); }
    };

    Interpolator interpolator = new AccelerateDecelerateInterpolator();
    long duration = 400;
    private NumberTextViewBinder disSpeedBinder, topFrameAngleBinder;


    public ControlSpeedMonitAndButtons(Context context) {
        super(context);
        init(context);
    }

    public ControlSpeedMonitAndButtons(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        inflate(context, R.layout.component_control_speed_monit_and_buttons, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        btnSpeedUp = (Button) findViewById(R.id.btn_speed_up);
        btnSpeedDown = (Button) findViewById(R.id.btn_speed_down);
        disSpeed = (TextView) findViewById(R.id.dis_speed);
        disSpeedBinder = new NumberTextViewBinder(disSpeed, 0, duration, numberFormatter, interpolator);

        btnSpeedLow = (Button) findViewById(R.id.btn_speed_low);
        btnSpeedMiddle = (Button) findViewById(R.id.btn_speed_middle);
        btnSpeedHigh = (Button) findViewById(R.id.btn_speed_high);

        topFrameAngle = (TextView) findViewById(R.id.top_frame_angle);
        topFrameAngleBinder = new NumberTextViewBinder(topFrameAngle, 0.0f, duration, numberFormatter, interpolator);

        btnAngleZero = (Button) findViewById(R.id.btn_angle_zero);
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        // 관찰 가능!
//        viewModel.getSocketService().observe(lifecycleOwner, socketService -> {
//            if (socketService != null) {
//                resCustomSocketService = socketService;
//            }
//        });
//        viewModel.getCoilDataMap().observe(lifecycleOwner, coilDataMap -> {
//            if (coilDataMap != null) {
//                resCoilDataMap = coilDataMap;
//                // 예시: 11번 value를 UI에 표시
//            }
//        });
//        viewModel.getHoldingDataMap().observe(lifecycleOwner, holdingDataMap -> {
//            if (holdingDataMap != null) {
//                resHoldingDataMap = holdingDataMap;
//
//                disSpeedBinder.update(SpeedConverter.rpmToMps(holdingDataMap.getValue(20)));      //속도
//                topFrameAngleBinder.update(holdingDataMap.getValue(1) / 100.0);      // 상부 프레임 각도
//            }
//        });
    }

}
