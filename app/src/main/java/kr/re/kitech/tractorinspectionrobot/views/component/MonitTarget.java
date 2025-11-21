package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.adapter.ProgramRecyclerView;

public class MonitTarget extends LinearLayout {
    private SharedPreferences setting;
    private SharedPreferences.Editor editor;
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private RobotState lastState = null;
    private RobotState targetState = null;
    private LinearLayout targetBox;
    private TextView curX, curY, curZ, tarX, tarY, tarZ;


    public MonitTarget(Context context) {
        super(context);
        init(context);
    }

    public MonitTarget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        inflate(context, R.layout.component_monit_target, this);
        setting = context.getSharedPreferences("setting", 0);
        editor = setting.edit();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        targetBox = findViewById(R.id.target_box);
        curX = findViewById(R.id.cur_x);
        curY = findViewById(R.id.cur_y);
        curZ = findViewById(R.id.cur_z);
        tarX = findViewById(R.id.tar_x);
        tarY = findViewById(R.id.tar_y);
        tarZ = findViewById(R.id.tar_z);
    }
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
        viewModel.getState().observe(lifecycleOwner, s -> {
            Log.w("MonitTarget",
                    "state observer:" + s
                            + " vm=" + System.identityHashCode(viewModel));
            if (s == null) return;
            lastState = s;                    // 페이지 재로딩 시 재전송용 캐시

            curX.setText("x : " + lastState.getX());
            curY.setText("y : " + lastState.getY());
            curZ.setText("z : " + lastState.getZ());
        });
        viewModel.getCommandState().observe(lifecycleOwner, s -> {
            Log.w("MonitTarget",
                    "commandState observer:" + s
                            + " vm=" + System.identityHashCode(viewModel));
            if (s == null) return;
            targetState = s;                    // 페이지 재로딩 시 재전송용 캐시
            targetBox.setVisibility(View.VISIBLE);
            tarX.setText("x : " + targetState.getX());
            tarY.setText("y : " + targetState.getY());
            tarZ.setText("z : " + targetState.getZ());
        });
    }

}
