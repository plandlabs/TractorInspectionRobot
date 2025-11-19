package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.listener.touch.BtnTouchUpDownListener;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

public class ControlVimMovementTouchButtons extends LinearLayout {
    private SharedPreferences setting;
    private SharedPreferences.Editor editor;
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;

    private Button btnUp, btnDown, btnForward, btnBackward, btnLeftWard, btnRightward;
    private SeekBar seekBarMills, seekBarStep;
    private TextView textSeekMills, textSeekStep;
    private int intervalMillis, step;

    private int colorOn;
    private int colorOff;

    public ControlVimMovementTouchButtons(Context context) {
        super(context);
        init(context);
    }

    public ControlVimMovementTouchButtons(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @SuppressLint("SetTextI18n")
    private void init(Context context) {
        inflate(context, R.layout.component_control_vim_movement_touch_buttons, this);
        setting = context.getSharedPreferences("setting", 0);
        editor = setting.edit();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        btnUp = findViewById(R.id.btn_up);
        btnDown = findViewById(R.id.btn_down);
        btnLeftWard = findViewById(R.id.btn_leftward);
        btnRightward = findViewById(R.id.btn_rightward);
        btnForward = findViewById(R.id.btn_forward);
        btnBackward = findViewById(R.id.btn_backward);

        colorOn = R.color.touch_on;
        colorOff = R.color.touch_off;

        // ========= step SeekBar =========
        seekBarStep = findViewById(R.id.seekBarStep);
        textSeekStep = findViewById(R.id.textSeekStep);

        int defaultStep = Integer.parseInt(getContext().getString(R.string.vim_move_step));
        step = setting.getInt("v_step", 0) > 0
                ? setting.getInt("v_step", 0)
                : defaultStep;

        // 범위 1 ~ 2000 보정
        if (step < 1) step = 1;
        if (step > 2000) step = 2000;

        // SeekBar 범위: 0 ~ (2000 - 1)
        seekBarStep.setMax(2000 - 1);
        seekBarStep.setProgress(step - 1);
        textSeekStep.setText(step + " ㎜");

        seekBarStep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVibrator.vibrate(20);
                // 1 ~ 2000 매핑
                step = 1 + progress;
                textSeekStep.setText(step + " ㎜");

                editor.putInt("v_step", step);
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

        int defaultMillis = Integer.parseInt(getContext().getString(R.string.interval_millis_v));
        intervalMillis = setting.getInt("v_millis", 0) > 0
                ? setting.getInt("v_millis", 0)
                : defaultMillis;
//        intervalMillis = defaultMillis;

        // 범위 10 ~ 1000 보정
        if (intervalMillis < 10) intervalMillis = 10;
        if (intervalMillis > 1000) intervalMillis = 1000;

        // SeekBar 범위: 0 ~ (1000 - 10)
        seekBarMills.setMax((1000 - 10) / 10); // 199
        seekBarMills.setProgress((intervalMillis - 10) / 10);
        textSeekMills.setText(intervalMillis + " ㎳");

        seekBarMills.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVibrator.vibrate(20);
                // 10 ~ 1000 매핑
                intervalMillis = (progress * 10) + 10;
                textSeekMills.setText(intervalMillis + " ㎳");

                editor.putInt("v_millis", intervalMillis);
                editor.apply();

                // 최신 값으로 리스너 다시 세팅
                attachTouchListeners();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    /**
     * step / intervalMillis 값이 바뀔 때마다
     * 항상 이 메서드를 호출해서 버튼 리스너를 최신 값으로 교체한다.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void attachTouchListeners() {
        if (viewModel == null) {
            Log.w("touch", "viewModel is null");
            return; // 아직 ViewModel 안 들어왔으면 패스
        }

        BtnTouchUpDownListener.DeltaRequester req = new BtnTouchUpDownListener.DeltaRequester() {
            @Override
            public void applyDelta(String axis, double delta) {
                viewModel.applyDeltaAndPublish(axis, delta);
            }

            @Override
            public void onStop() {
                // 필요하면 여기에서 publishCurrent 등 호출
                // viewModel.publishCurrent(...);
            }
        };

        // Y축 (상/하)
        btnUp.setOnTouchListener(
                new BtnTouchUpDownListener(getContext(), "y", colorOn, colorOff, true, step, intervalMillis, req)
        );
        btnDown.setOnTouchListener(
                new BtnTouchUpDownListener(getContext(), "y", colorOn, colorOff, false, step, intervalMillis, req)
        );

        // X축 (좌/우)
        btnRightward.setOnTouchListener(
                new BtnTouchUpDownListener(getContext(), "x", colorOn, colorOff, true, step, intervalMillis, req)
        );
        btnLeftWard.setOnTouchListener(
                new BtnTouchUpDownListener(getContext(), "x", colorOn, colorOff, false, step, intervalMillis, req)
        );

        // Z축 (전/후) — forward=+z, backward=-z
        btnForward.setOnTouchListener(
                new BtnTouchUpDownListener(getContext(), "z", colorOn, colorOff, true, step, intervalMillis, req)
        );
        btnBackward.setOnTouchListener(
                new BtnTouchUpDownListener(getContext(), "z", colorOn, colorOff, false, step, intervalMillis, req)
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        attachTouchListeners();

//        BtnTouchUpDownListener.DeltaRequester req = new BtnTouchUpDownListener.DeltaRequester() {
//            @Override
//            public void applyDelta(String axis, double delta) {
//                viewModel.applyDeltaAndPublish(axis, delta);
//            }
//
//            @Override
//            public void onStop() {
//                // 필요하면 여기에서 publishCurrent 등 호출
//                // viewModel.publishCurrent(...);
//            }
//        };

        // ViewModel 세팅될 때 현재 step/intervalMillis 기준으로 리스너 부착

//        btnUp.setOnTouchListener(
//                new BtnTouchUpDownListener(getContext(), "y", colorOn, colorOff, true, step, intervalMillis, req)
//        );
//        btnDown.setOnTouchListener(
//                new BtnTouchUpDownListener(getContext(), "y", colorOn, colorOff, false, step, intervalMillis, req)
//        );
//
//        // X축 (좌/우)
//        btnRightward.setOnTouchListener(
//                new BtnTouchUpDownListener(getContext(), "x", colorOn, colorOff, true, step, intervalMillis, req)
//        );
//        btnLeftWard.setOnTouchListener(
//                new BtnTouchUpDownListener(getContext(), "x", colorOn, colorOff, false, step, intervalMillis, req)
//        );
//
//        // Z축 (전/후) — forward=+z, backward=-z
//        btnForward.setOnTouchListener(
//                new BtnTouchUpDownListener(getContext(), "z", colorOn, colorOff, true, step, intervalMillis, req)
//        );
//        btnBackward.setOnTouchListener(
//                new BtnTouchUpDownListener(getContext(), "z", colorOn, colorOff, false, step, intervalMillis, req)
//        );
    }
}
