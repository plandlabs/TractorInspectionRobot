package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
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
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.utils.JsonFileUtil;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.adapter.ProgramRecyclerView;

public class MonitProgram extends LinearLayout {
    private SharedPreferences setting;
    private SharedPreferences.Editor editor;
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private ProgramRecyclerView programRecyclerViewAdapter;
    private final ArrayList<RobotState> robotStateItems = new ArrayList<>();
    private String programJson;
    private RobotState lastState = null;
    private View btnListSave;


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
        setting = context.getSharedPreferences("setting", 0);
        editor = setting.edit();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        RecyclerView programRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        programRecyclerViewAdapter = new ProgramRecyclerView(robotStateItems, context);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        programRecyclerView.setLayoutManager(mLayoutManager);
        programRecyclerView.setAdapter(programRecyclerViewAdapter);

        btnListSave = findViewById(R.id.btn_list_save);
        btnListSave.setOnClickListener(view -> {
            mVibrator.vibrate(50);
            // 마지막 상태가 없는 경우
            if (lastState == null) {
                Toast.makeText(getContext(), "현재 로봇 상태가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            RobotState item = null;
            try {
                item = new RobotState(lastState.toJson());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            JSONObject json = item.toJson();
            double x  = json.optDouble("x", 0);
            double y  = json.optDouble("y", 0);
            double z  = json.optDouble("z", 0);
            double s1 = json.optDouble("s1", 0);
            double s2 = json.optDouble("s2", 0);
            double s3 = json.optDouble("s3", 0);

            String msg = String.format(
                    "추가할 프로그램 항목:\n" +
                            "X: %.1f\nY: %.1f\nZ: %.1f\n" +
                            "S1: %.1f\nS2: %.1f\nS3: %.1f\n\n저장하시겠습니까?",
                    x, y, z, s1, s2, s3
            );

            // 저장 여부 확인 다이얼로그
            new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                    .setTitle("프로그램 저장")
                    .setMessage(msg)
                    .setPositiveButton("예", (dialog, which) -> {

                        if (mVibrator != null) mVibrator.vibrate(50);

                        // RobotState 복사하여 리스트에 추가
                        RobotState newState = null;
                        try {
                            newState = new RobotState(lastState.toJson());
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        newState.setNum(robotStateItems.size() + 1);
                        robotStateItems.add(0, newState);

                        programRecyclerViewAdapter.notifyDataSetChanged();

                        // JSONArray로 변환 후 저장
                        JSONArray saveArray = new JSONArray();
                        for (RobotState rs : robotStateItems) {
                            saveArray.put(rs.toJson());
                        }

                        editor.putString("Program_json", saveArray.toString());
                        editor.apply();

                        Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", (dialog, which) -> {
                        dialog.dismiss();   // 아무것도 안 하고 닫기
                    })
                    .show();
        });
        programRecyclerViewAdapter.setOnItemClickListener((v, pos) -> {

            if (pos < 0 || pos >= robotStateItems.size()) return;
            mVibrator.vibrate(50);
            RobotState item = robotStateItems.get(pos);

            // RobotState 내부 값 꺼내기
            JSONObject json = item.toJson();
            double x  = json.optDouble("x", 0);
            double y  = json.optDouble("y", 0);
            double z  = json.optDouble("z", 0);
            double s1 = json.optDouble("s1", 0);
            double s2 = json.optDouble("s2", 0);
            double s3 = json.optDouble("s3", 0);

            String msg = String.format(
                    "이동할 항목:\n" +
                            "X: %.1f\nY: %.1f\nZ: %.1f\n" +
                            "S1: %.1f\nS2: %.1f\nS3: %.1f\n\n이동하시겠습니까?",
                    x, y, z, s1, s2, s3
            );

            new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                    .setTitle("이동 확인")
                    .setMessage(msg)
                    .setPositiveButton("예", (dialog, which) -> {

                        if (mVibrator != null) mVibrator.vibrate(50);


                        viewModel.applyStateAndPublish(item);
                        Toast.makeText(getContext(), "이동이 입력되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                    .show();
        });
        programRecyclerViewAdapter.setOnItemLongClickListener((v, pos) -> {
            mVibrator.vibrate(50);
            if (pos < 0 || pos >= robotStateItems.size()) return;

            RobotState item = robotStateItems.get(pos);

            // RobotState 내부 값 꺼내기
            JSONObject json = item.toJson();
            double x  = json.optDouble("x", 0);
            double y  = json.optDouble("y", 0);
            double z  = json.optDouble("z", 0);
            double s1 = json.optDouble("s1", 0);
            double s2 = json.optDouble("s2", 0);
            double s3 = json.optDouble("s3", 0);

            String msg = String.format(
                    "삭제할 항목:\n" +
                            "X: %.1f\nY: %.1f\nZ: %.1f\n" +
                            "S1: %.1f\nS2: %.1f\nS3: %.1f\n\n삭제하시겠습니까?",
                    x, y, z, s1, s2, s3
            );

            new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                    .setTitle("삭제 확인")
                    .setMessage(msg)
                    .setPositiveButton("예", (dialog, which) -> {

                        if (mVibrator != null) mVibrator.vibrate(50);

                        // 항목 삭제
                        robotStateItems.remove(pos);

                        // 번호 재정렬
                        for (int i = 0; i < robotStateItems.size(); i++) {
                            robotStateItems.get(i).setNum(robotStateItems.size() - i);
                        }

                        programRecyclerViewAdapter.notifyDataSetChanged();

                        // 다시 JSON 저장
                        JSONArray saveArray = new JSONArray();
                        for (RobotState rs : robotStateItems) {
                            saveArray.put(rs.toJson());
                        }
                        editor.putString("Program_json", saveArray.toString());
                        editor.apply();

                        Toast.makeText(getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                    .show();
        });




    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
        viewModel.getState().observe(lifecycleOwner, s -> {
            if (s == null) return;
            lastState = s;                    // 페이지 재로딩 시 재전송용 캐시
        });
        programJson = setting.getString("Program_json", "[]");

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
