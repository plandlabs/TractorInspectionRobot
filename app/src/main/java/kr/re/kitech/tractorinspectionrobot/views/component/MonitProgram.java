package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.utils.JsonFileUtil;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.adapter.ProgramRecyclerView;

public class MonitProgram extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private ProgramRecyclerView programRecyclerViewAdapter;
    private final ArrayList<RobotState> robotStateItems = new ArrayList<>();
    private String programJson;


    public MonitProgram(Context context) {
        super(context);
        init(context);
    }

    public MonitProgram(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        inflate(context, R.layout.component_monit_program, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        RecyclerView programRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        programRecyclerViewAdapter = new ProgramRecyclerView(robotStateItems, context);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        programRecyclerView.setLayoutManager(mLayoutManager);
        programRecyclerView.setAdapter(programRecyclerViewAdapter);
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        programJson = JsonFileUtil.readJSONFromAsset(getContext(), "test.json");

        robotStateItems.clear();
        try {
            JSONArray jsonArray = new JSONArray(programJson);
            for (int j = 0; j < jsonArray.length(); j++) {
                JSONObject jsonObject = jsonArray.getJSONObject(j);
                RobotState robotStateItem = new RobotState(jsonObject);
                robotStateItem.setNum(jsonArray.length() - j);
                robotStateItems.add(j, robotStateItem);
            }
            programRecyclerViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // 관찰 가능!
    }

}
