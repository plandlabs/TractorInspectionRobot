package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
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
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.utils.JsonFileUtil;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.adapter.ProgramRecyclerView;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.model.ProgramItem;

public class MonitProgram extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private RecyclerView programRecyclerView;
    private ProgramRecyclerView programRecyclerViewAdapter;
    private ArrayList<ProgramItem> programItems = new ArrayList<>();
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
        programRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        programRecyclerViewAdapter = new ProgramRecyclerView(programItems, context);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        programRecyclerView.setLayoutManager(mLayoutManager);
        programRecyclerView.setAdapter(programRecyclerViewAdapter);
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        programJson = JsonFileUtil.readJSONFromAsset(getContext(), "test.json");

        programItems.clear();
        try {
            JSONArray jsonArray = new JSONArray(programJson);
            for (int j = 0; j < jsonArray.length(); j++) {
                JSONObject jsonObject = jsonArray.getJSONObject(j);
                ProgramItem programItem = new ProgramItem(jsonObject);
                programItem.setNum(jsonArray.length() - j);
                programItems.add(j, programItem);
            }
            programRecyclerViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // 관찰 가능!
    }

}
