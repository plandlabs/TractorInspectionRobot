package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.listener.touch.BtnTouchUpDownListener;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

public class ControlCameraMovementTouchButtons extends LinearLayout {
    private SharedPreferences setting;
    private SharedPreferences.Editor editor;
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    public Button btn_y_up, btn_y_down, btnForward, btnBackward, btn_x_up, btn_x_down;
    private SeekBar seekBarMills, seekBarStep;
    private TextView textSeekMills, textSeekStep;
    private int intervalMillis, step;
    private int colorOn;
    private int colorOff;


    public ControlCameraMovementTouchButtons(Context context) {
        super(context);
        init(context);
    }

    public ControlCameraMovementTouchButtons(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    @SuppressLint("SetTextI18n")
    private void init(Context context) {
        inflate(context, R.layout.component_control_camera_movement_touch_buttons, this);
        setting = context.getSharedPreferences("setting", 0);
        editor = setting.edit();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        btn_y_up = findViewById(R.id.btn_y_up);
        btn_y_down = findViewById(R.id.btn_y_down);

        btn_x_up = findViewById(R.id.btn_x_up);
        btn_x_down = findViewById(R.id.btn_x_down);

        btnBackward = findViewById(R.id.btn_backward);

        btnForward = findViewById(R.id.btn_forward);
        btnBackward = findViewById(R.id.btn_backward);

        colorOn = R.color.touch_on;
        colorOff = R.color.touch_off;

        // ========= step SeekBar =========
        seekBarStep = findViewById(R.id.seekBarStep);
        textSeekStep = findViewById(R.id.textSeekStep);

        int defaultStep = Integer.parseInt(getContext().getString(R.string.camera_move_step));
        step = setting.getInt("c_step", 0) > 0
                ? setting.getInt("c_step", 0)
                : defaultStep;

        // 범위 1 ~ 10 보정
        if (step < 1) step = 1;
        if (step > 10) step = 10;

        // SeekBar 범위: 0 ~ (10 - 1)
        seekBarStep.setMax(10 - 1);
        seekBarStep.setProgress(step - 1);
        textSeekStep.setText(step + " °");

        seekBarStep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVibrator.vibrate(20);
                // 1 ~ 10 매핑
                step = 1 + progress;
                textSeekStep.setText(step + " °");

                editor.putInt("c_step", step);
                editor.apply();

                // 최신 값으로 리스너 다시 세팅
                attachTouchListeners();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // ========= millis SeekBar =========
        seekBarMills = findViewById(R.id.seekBarMills);
        textSeekMills = findViewById(R.id.textSeekMills);

        int defaultMillis = Integer.parseInt(getContext().getString(R.string.interval_millis_c));
        intervalMillis = setting.getInt("c_millis", 0) > 0
                ? setting.getInt("c_millis", 0)
                : defaultMillis;
//        intervalMillis = defaultMillis;

        // 범위 100 ~ 1000 보정
        if (intervalMillis < 100) intervalMillis = 100;
        if (intervalMillis > 1000) intervalMillis = 1000;

// SeekBar 범위: 0~9  (100씩 증가)
        seekBarMills.setMax(9);
        seekBarMills.setProgress((intervalMillis / 100) - 1);
        textSeekMills.setText(intervalMillis + " ㎳");

        seekBarMills.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVibrator.vibrate(20);

                // ✔ 100~1000 매핑 (100 단위 증가)
                intervalMillis = (progress + 1) * 100;

                textSeekMills.setText(intervalMillis + " ㎳");

                editor.putInt("c_millis", intervalMillis);
                editor.apply();

                attachTouchListeners();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }
    @SuppressLint("ClickableViewAccessibility")
    private void attachTouchListeners() {
        BtnTouchUpDownListener.DeltaRequester req = new BtnTouchUpDownListener.DeltaRequester() {
            @Override
            public void applyDelta(String axis, int delta) {
                viewModel.applyDeltaAndPublish(axis, delta);
            }

            @Override
            public void onStop() {}
        };

        // s1 (카메라 헤드 서보모터 상/하) - s1에서 변경
        btn_x_up.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "s1", colorOn, colorOff, true, step, intervalMillis, req));
        btn_x_down.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "s1", colorOn, colorOff, false, step, intervalMillis, req));

        // s2 (중앙 서보모터 상/하) - s2에서 변경
        btn_y_up.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "s2", colorOn, colorOff, true, step, intervalMillis, req));
        btn_y_down.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "s2", colorOn, colorOff, false, step, intervalMillis, req));

        // s3 (끝 서보모터 회전) - s3에서 변경
        btnForward.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "s3", colorOn, colorOff, true, step, intervalMillis, req));
        btnBackward.setOnTouchListener(new BtnTouchUpDownListener(getContext(), "s3", colorOn, colorOff, false, step, intervalMillis, req));
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        attachTouchListeners();
    }

}
