package kr.re.kitech.tractorinspectionrobot.views.component;

import android.content.Context;
import android.content.Intent;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.MqttForegroundService;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;

public class ControlEmergencyButtons extends LinearLayout {

    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;

    private View btnZeroAll;
    private View btnZeroVim;
    private View btnZeroServo;
    private View btnStop;

    public ControlEmergencyButtons(Context context) {
        super(context);
        init(context);
    }

    public ControlEmergencyButtons(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.component_control_emergency_buttons, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        btnZeroAll   = findViewById(R.id.btnZeroAll);
        btnZeroVim   = findViewById(R.id.btnZeroVim);
        btnZeroServo = findViewById(R.id.btnZeroServo);
        btnStop      = findViewById(R.id.btnStop);
    }

    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        if (viewModel == null) return;

        // 공통 진동 헬퍼
        View.OnClickListener noop = null;

        btnZeroAll.setOnClickListener(v -> {
            vibrateShort();

            // 1) x,y,z,s1,s2,s3 모두 0으로
            viewModel.applyStateAndPublish(0, 0, 0, 0, 0, 0);
            // 2) 만약 ForegroundService에서 프로그램이 돌고 있다면 중지
            Intent i = new Intent(getContext(), MqttForegroundService.class);
            i.setAction(MqttForegroundService.ACTION_PROGRAM_STOP);
            getContext().startService(i);
        });

        btnZeroVim.setOnClickListener(v -> {
            vibrateShort();

            // 1) 현재 서보값 유지, x,y,z만 0으로
            RobotState cur = viewModel.getState().getValue();
            if (cur == null) cur = new RobotState(0,0,0,0,0,0, System.currentTimeMillis());

            viewModel.applyStateAndPublish(
                    0, 0, 0,
                    cur.s1, cur.s2, cur.s3
            );
            // 2) 만약 ForegroundService에서 프로그램이 돌고 있다면 중지
            Intent i = new Intent(getContext(), MqttForegroundService.class);
            i.setAction(MqttForegroundService.ACTION_PROGRAM_STOP);
            getContext().startService(i);
        });

        btnZeroServo.setOnClickListener(v -> {
            vibrateShort();

            // 1) 현재 위치 유지, s1,s2,s3만 0으로
            RobotState cur = viewModel.getState().getValue();
            if (cur == null) cur = new RobotState(0,0,0,0,0,0, System.currentTimeMillis());

            viewModel.applyStateAndPublish(
                    cur.x, cur.y, cur.z,
                    0, 0, 0
            );
            // 2) 만약 ForegroundService에서 프로그램이 돌고 있다면 중지
            Intent i = new Intent(getContext(), MqttForegroundService.class);
            i.setAction(MqttForegroundService.ACTION_PROGRAM_STOP);
            getContext().startService(i);
        });

        btnStop.setOnClickListener(v -> {
            vibrateShort();

            // 1) 현재 상태를 그대로 ABS로 다시 보내서 "정지" 효과
            viewModel.publishCurrent();

            // 2) 만약 ForegroundService에서 프로그램이 돌고 있다면 중지
            Intent i = new Intent(getContext(), MqttForegroundService.class);
            i.setAction(MqttForegroundService.ACTION_PROGRAM_STOP);
            getContext().startService(i);
        });
    }

    private void vibrateShort() {
        if (mVibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(
                        VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                );
            } else {
                mVibrator.vibrate(30);
            }
        } catch (SecurityException ignore) {
            // 진동 퍼미션 없으면 그냥 무시
        }
    }
}
