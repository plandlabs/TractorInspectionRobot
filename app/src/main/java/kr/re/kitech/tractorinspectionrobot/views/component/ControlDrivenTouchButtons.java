package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import kr.re.kitech.tractorinspectionrobot.R;

import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

public class ControlDrivenTouchButtons extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private LinearLayout topTilting, bottomTilting, sliding, slidingCaster, lift, stabilizer;
    private Button btnControl01, btnControl02, //상부 틸팅
            btnControl03, btnControl04,  //하부 틸팅
            btnControl05, btnControl06,  //슬라이딩
            btnControl07, btnControl08,  //슬라이딩 캐스터
            btnControl09, btnControl10,  //리프트
            btnControl11, btnControl12;  //스테빌라이저


    public ControlDrivenTouchButtons(Context context) {
        super(context);
        init(context);
    }

    public ControlDrivenTouchButtons(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        inflate(context, R.layout.component_control_driven_touch_buttons, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        gridLayout = findViewById(R.id.grid_layout);
        topTilting = findViewById(R.id.topTilting);
        bottomTilting = findViewById(R.id.bottomTilting);
        sliding = findViewById(R.id.sliding);
        slidingCaster = findViewById(R.id.slidingCaster);
        lift = findViewById(R.id.lift);
        stabilizer = findViewById(R.id.stabilizer);

        btnControl01 = (Button) findViewById(R.id.btn_control_01);
        btnControl02 = (Button) findViewById(R.id.btn_control_02);
        btnControl03 = (Button) findViewById(R.id.btn_control_03);
        btnControl04 = (Button) findViewById(R.id.btn_control_04);
        btnControl05 = (Button) findViewById(R.id.btn_control_05);
        btnControl06 = (Button) findViewById(R.id.btn_control_06);
        btnControl07 = (Button) findViewById(R.id.btn_control_07);
        btnControl08 = (Button) findViewById(R.id.btn_control_08);
        btnControl09 = (Button) findViewById(R.id.btn_control_09);
        btnControl10 = (Button) findViewById(R.id.btn_control_10);

    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        // 관찰 가능!
    }
    public void updateButtonGroups(String mode) {
        // 일단 전부 숨김
        gridLayout.removeAllViews(); // 빈칸 방지
    }

}
