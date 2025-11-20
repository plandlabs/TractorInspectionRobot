package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.MqttForegroundService;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.adapter.OnItemClickListener;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.adapter.ProgramRecyclerView;

public class MonitProgram extends LinearLayout {

    private static final String KEY_PROGRAM_JSON    = "Program_json";
    private static final String KEY_INTERVAL_SECOND = "p_second";

    // move: 0 기본, 1 대기, 2 이동중, 3 실행중(대기), 4 완료

    // MqttForegroundService 의 phase 값과 동일해야 함
    private static final int PHASE_IDLE      = 0;
    private static final int PHASE_LIFTING   = 1;
    private static final int PHASE_MOVING_XY = 2;
    private static final int PHASE_MOVING_Z  = 3;
    private static final int PHASE_WAITING   = 4;

    private SharedPreferences setting;
    private SharedPreferences.Editor editor;
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private ProgramRecyclerView programRecyclerViewAdapter;
    private final ArrayList<RobotState> robotStateItems = new ArrayList<>();
    private RobotState lastState = null;

    private SeekBar seekBarSecond;
    private TextView textSeekSecond;
    private int intervalSecond;

    private View btnListSave, btnListLoad, btnListStop;

    // 프로그램 실행 중 여부(UI 관점)
    private boolean programLoad = false;

    // 서비스에서 보내는 진행상황 수신용
    private BroadcastReceiver programProgressReceiver = null;

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

        // RecyclerView
        RecyclerView programRecyclerView = findViewById(R.id.recycler_view);
        programRecyclerViewAdapter = new ProgramRecyclerView(robotStateItems, context);
        programRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        programRecyclerView.setAdapter(programRecyclerViewAdapter);

        // 버튼
        btnListSave = findViewById(R.id.btnListSave);
        btnListLoad = findViewById(R.id.btnListLoad);
        btnListStop = findViewById(R.id.btnListStop);

        programLoad = false;
        updateProgramButtons();

        // ===== 저장 버튼 =====
        btnListSave.setOnClickListener(view -> {
            if (mVibrator != null) mVibrator.vibrate(50);

            if (lastState == null) {
                Toast.makeText(getContext(), "현재 로봇 상태가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            RobotState item;
            try {
                item = new RobotState(lastState.toJson());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            JSONObject json = item.toJson();
            int x  = json.optInt("x", 0);
            int y  = json.optInt("y", 0);
            int z  = json.optInt("z", 0);
            int s1 = json.optInt("s1", 0);
            int s2 = json.optInt("s2", 0);
            int s3 = json.optInt("s3", 0);

            String msg = String.format(
                    "추가할 프로그램 항목:\n" +
                            "X: %d\nY: %d\nZ: %d\n" +
                            "S1: %d\nS2: %d\nS3: %d\n\n저장하시겠습니까?",
                    x, y, z, s1, s2, s3
            );

            new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                    .setTitle("프로그램 저장")
                    .setMessage(msg)
                    .setPositiveButton("예", (dialog, which) -> {
                        if (mVibrator != null) mVibrator.vibrate(50);

                        RobotState newState;
                        try {
                            newState = new RobotState(lastState.toJson());
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        newState.setNum(robotStateItems.size() + 1);
                        newState.setMove(0);  // 기본 상태 0
                        robotStateItems.add(0, newState);

                        programRecyclerViewAdapter.notifyDataSetChanged();

                        JSONArray saveArray = new JSONArray();
                        for (RobotState rs : robotStateItems) {
                            saveArray.put(rs.toJson());
                        }
                        editor.putString(KEY_PROGRAM_JSON, saveArray.toString());
                        editor.apply();

                        Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // ===== 실행 버튼 =====
        btnListLoad.setOnClickListener(view -> {
            if (mVibrator != null) mVibrator.vibrate(50);

            if (viewModel == null) {
                Toast.makeText(getContext(), "뷰모델이 아직 연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (robotStateItems.isEmpty()) {
                Toast.makeText(getContext(), "저장된 프로그램이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 프로그램 JSON 생성
            JSONArray arr = new JSONArray();
            for (RobotState rs : robotStateItems) {
                arr.put(rs.toJson());
            }
            String programJson = arr.toString();

            // ForegroundService 에 프로그램 실행 요청
            Intent svc = new Intent(getContext(), MqttForegroundService.class);
            svc.setAction(MqttForegroundService.ACTION_PROGRAM_START);
            svc.putExtra(MqttForegroundService.EXTRA_PROGRAM_JSON, programJson);
            svc.putExtra(MqttForegroundService.EXTRA_INTERVAL_SECOND, intervalSecond);
            ContextCompat.startForegroundService(getContext(), svc);

            // UI 상으로는 0번을 이동중, 나머지를 대기로 표시
            for (int i = 0; i < robotStateItems.size(); i++) {
                if (i == 0) robotStateItems.get(i).setMove(2); // 이동중
                else        robotStateItems.get(i).setMove(1); // 대기
            }
            programRecyclerViewAdapter.notifyDataSetChanged();

            programLoad = true;
            updateProgramButtons();

            Toast.makeText(getContext(),
                    "프로그램 실행 시작 (1 / " + robotStateItems.size() + ")",
                    Toast.LENGTH_SHORT).show();
        });

        btnListLoad.setOnLongClickListener(view -> {
            // 필요하면 롱클릭 기능 추가
            return false;
        });

        // ===== 정지 버튼 =====
        btnListStop.setOnClickListener(view -> {
            if (mVibrator != null) mVibrator.vibrate(50);

            // 1) 서비스 쪽 프로그램 정지
            Intent svc = new Intent(getContext(), MqttForegroundService.class);
            svc.setAction(MqttForegroundService.ACTION_PROGRAM_STOP);
            getContext().startService(svc);

            // 2) UI 상태 리셋
            programLoad = false;
            for (RobotState rs : robotStateItems) {
                rs.setMove(0);
            }
            programRecyclerViewAdapter.notifyDataSetChanged();
            updateProgramButtons();

            // 3) 현재 lastState 를 ABS로 한 번 더 보내서 "현재 위치를 목표로" 하도록 → 실제 정지 효과
            if (viewModel != null && lastState != null) {
                viewModel.applyStateAndPublish(lastState);
            }

            Toast.makeText(getContext(), "프로그램 실행을 중지했습니다.", Toast.LENGTH_SHORT).show();
        });

        // ===== 단일 항목 클릭 → 즉시 이동 (한 번에) =====
        programRecyclerViewAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View v, int pos) {
                if (pos < 0 || pos >= robotStateItems.size()) return;
                if (mVibrator != null) mVibrator.vibrate(50);

                RobotState item = robotStateItems.get(pos);

                JSONObject json = item.toJson();
                int x  = json.optInt("x", 0);
                int y  = json.optInt("y", 0);
                int z  = json.optInt("z", 0);
                int s1 = json.optInt("s1", 0);
                int s2 = json.optInt("s2", 0);
                int s3 = json.optInt("s3", 0);

                String msg = String.format(
                        "이동할 항목:\n" +
                                "X: %d\nY: %d\nZ: %d\n" +
                                "S1: %d\nS2: %d\nS3: %d\n\n이동하시겠습니까?",
                        x, y, z, s1, s2, s3
                );

                new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                        .setTitle("이동 확인")
                        .setMessage(msg)
                        .setPositiveButton("예", (dialog, which) -> {
                            if (mVibrator != null) mVibrator.vibrate(50);

                            if (viewModel != null) {
                                // 단일 클릭은 기존처럼 한 번에 이동
                                viewModel.applyStateAndPublish(item);
                                Toast.makeText(getContext(), "이동이 입력되었습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "뷰모델이 아직 연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onItemDeleteClick(View v, int pos) {
                if (mVibrator != null) mVibrator.vibrate(50);
                if (pos < 0 || pos >= robotStateItems.size()) return;

                RobotState item = robotStateItems.get(pos);

                JSONObject json = item.toJson();
                int x  = json.optInt("x", 0);
                int y  = json.optInt("y", 0);
                int z  = json.optInt("z", 0);
                int s1 = json.optInt("s1", 0);
                int s2 = json.optInt("s2", 0);
                int s3 = json.optInt("s3", 0);

                String msg = String.format(
                        "삭제할 항목:\n" +
                                "X: %d\nY: %d\nZ: %d\n" +
                                "S1: %d\nS2: %d\nS3: %d\n\n삭제하시겠습니까?",
                        x, y, z, s1, s2, s3
                );

                new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                        .setTitle("삭제 확인")
                        .setMessage(msg)
                        .setPositiveButton("예", (dialog, which) -> {
                            if (mVibrator != null) mVibrator.vibrate(50);

                            robotStateItems.remove(pos);

                            for (int i = 0; i < robotStateItems.size(); i++) {
                                robotStateItems.get(i).setNum(robotStateItems.size() - i);
                            }

                            programRecyclerViewAdapter.notifyDataSetChanged();

                            JSONArray saveArray = new JSONArray();
                            for (RobotState rs : robotStateItems) {
                                saveArray.put(rs.toJson());
                            }
                            editor.putString(KEY_PROGRAM_JSON, saveArray.toString());
                            editor.apply();

                            Toast.makeText(getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });

        programRecyclerViewAdapter.setOnItemLongClickListener((v, pos) -> {
            // 사용 안 함
        });

        // ========= intervalSecond SeekBar =========
        seekBarSecond = findViewById(R.id.seekBarSecond);
        textSeekSecond = findViewById(R.id.textSeekSecond);

        int defaultSecond = Integer.parseInt(getContext().getString(R.string.interval_second_p));
        intervalSecond = setting.getInt(KEY_INTERVAL_SECOND, 0) > 0
                ? setting.getInt(KEY_INTERVAL_SECOND, 0)
                : defaultSecond;

        if (intervalSecond < 10) intervalSecond = 10;
        if (intervalSecond > 60) intervalSecond = 60;

        seekBarSecond.setMax(5); // 0~5 → 10~60
        seekBarSecond.setProgress((intervalSecond / 10) - 1);
        textSeekSecond.setText(intervalSecond + " 초");

        seekBarSecond.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mVibrator != null) mVibrator.vibrate(20);

                intervalSecond = (progress + 1) * 10;
                textSeekSecond.setText(intervalSecond + " 초");

                editor.putInt(KEY_INTERVAL_SECOND, intervalSecond);
                editor.apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // 저장된 프로그램 불러오기
        String programJson = setting.getString(KEY_PROGRAM_JSON, "[]");
        robotStateItems.clear();
        try {
            JSONArray jsonArray = new JSONArray(programJson);
            for (int j = 0; j < jsonArray.length(); j++) {
                JSONObject jsonObject = jsonArray.getJSONObject(j);
                RobotState robotStateItem = new RobotState(jsonObject);
                robotStateItem.setNum(jsonArray.length() - j);
                robotStateItem.setMove(0);
                robotStateItems.add(j, robotStateItem);
            }
            programRecyclerViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        // STA 상태 -> lastState 저장 (버튼 저장/정지용)
        viewModel.getState().observe(lifecycleOwner, s -> {
            if (s == null) return;
            lastState = s;
        });

        // 프로그램 진행 브로드캐스트 수신
        if (programProgressReceiver == null) {
            programProgressReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null ||
                            !MqttForegroundService.ACTION_PROGRAM_PROGRESS.equals(intent.getAction())) {
                        return;
                    }

                    boolean running = intent.getBooleanExtra(MqttForegroundService.EXTRA_PROGRAM_RUNNING, false);
                    int index       = intent.getIntExtra(MqttForegroundService.EXTRA_PROGRAM_INDEX, -1);
                    int total       = intent.getIntExtra(MqttForegroundService.EXTRA_PROGRAM_TOTAL, robotStateItems.size());
                    int phase       = intent.getIntExtra(MqttForegroundService.EXTRA_PROGRAM_PHASE, PHASE_IDLE);

                    if (!running) {
                        // 프로그램 종료/정지 → 모두 0
                        programLoad = false;
                        for (RobotState rs : robotStateItems) {
                            rs.setMove(0);
                        }
                        programRecyclerViewAdapter.notifyDataSetChanged();
                        updateProgramButtons();
                        return;
                    }

                    programLoad = true;

                    // index/total이 리스트 범위를 넘어가면 방어
                    if (total != robotStateItems.size()) {
                        // 리스트 길이가 바뀌었어도 일단 index 기준으로만 처리
                        // (필요하면 동기화 로직 추가)
                    }

                    for (int i = 0; i < robotStateItems.size(); i++) {
                        RobotState item = robotStateItems.get(i);
                        if (i < index) {
                            item.setMove(4); // 완료
                        } else if (i == index) {
                            if (phase == PHASE_WAITING) {
                                item.setMove(3); // 실행중(대기)
                            } else {
                                item.setMove(2); // 이동중
                            }
                        } else { // i > index
                            item.setMove(1); // 대기
                        }
                    }

                    programRecyclerViewAdapter.notifyDataSetChanged();
                    updateProgramButtons();
                }
            };

            IntentFilter f = new IntentFilter(MqttForegroundService.ACTION_PROGRAM_PROGRESS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(programProgressReceiver, f, Context.RECEIVER_NOT_EXPORTED);
            } else {
                ContextCompat.registerReceiver(getContext(), programProgressReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED);
            }

            // lifecycleOwner 파괴 시 receiver 정리
            lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    try {
                        getContext().unregisterReceiver(programProgressReceiver);
                    } catch (Exception ignore) {}
                }
            });
        }
    }

    // 버튼 가시성
    private void updateProgramButtons() {
        if (programLoad) {
            btnListLoad.setVisibility(View.GONE);
            btnListStop.setVisibility(View.VISIBLE);
        } else {
            btnListLoad.setVisibility(View.VISIBLE);
            btnListStop.setVisibility(View.GONE);
        }
    }
}
