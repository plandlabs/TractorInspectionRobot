// app/src/main/java/kr/re/kitech/tractorinspectionrobot/views/activity/RobotListActivity.java
package kr.re.kitech.tractorinspectionrobot.views.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.MqttForegroundService;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.adapter.OnItemClickListener;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.adapter.OnItemLongClickListener;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.adapter.RobotRecyclerViewAdapter;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.model.RobotItem;

public class RobotListActivity extends Activity {

    private SharedPreferences setting;
    private SharedPreferences.Editor editor;
    private View btnBack;
    private Vibrator mVibrator;
    private RecyclerView robotRecyclerView;
    private RobotRecyclerViewAdapter robotRecyclerViewAdapter;
    private ArrayList<RobotItem> robotItemArrayList;
    private FloatingActionButton fabAdd;

    // 간단하게 SharedPreferences 에 저장 (원하면 Room 으로 바꿀 수 있음)
    private static final String PREF_NAME = "robot_list_pref";
    private static final String KEY_LAST_SELECTED = "last_selected_robot";

    private String robotsJson;
    private BroadcastReceiver mqttStatusReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_list);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setting = getSharedPreferences("setting", 0);
        editor = setting.edit();
        robotsJson = setting.getString("Robots_json", "[]");

        robotRecyclerView = findViewById(R.id.robotRecyclerView);
        robotItemArrayList = new ArrayList<>();
        fabAdd = findViewById(R.id.fabAddRobot);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(view -> {
            mVibrator.vibrate(100);
            finish();
        });

        robotRecyclerViewAdapter = new RobotRecyclerViewAdapter(robotItemArrayList, this);
        robotRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        robotRecyclerView.setAdapter(robotRecyclerViewAdapter);

        // 아이템 클릭 시: 연결 여부 확인 후 MQTT 연결
        robotRecyclerViewAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View v, int pos) {
                RobotItem item = robotItemArrayList.get(pos);
                new AlertDialog.Builder(RobotListActivity.this, R.style.DarkAlertDialog)
                        .setTitle("MQTT 연결")
                        .setMessage("[" + item.getDeviceName() + "] 로봇에 MQTT로 연결하시겠습니까?")
                        .setPositiveButton("연결", (dialog, which) -> {
                            connectToRobot(item);
                            finish();
                        })
                        .setNegativeButton("취소", null)
                        .show();
            }

            @Override
            public void onItemDeleteClick(View v, int pos) {
                if (pos < 0 || pos >= robotItemArrayList.size()) return;
                RobotItem item = robotItemArrayList.get(pos);

                // ✅ 삭제 확인 다이얼로그
                new AlertDialog.Builder(RobotListActivity.this, R.style.DarkAlertDialog)
                        .setTitle("로봇 삭제")
                        .setMessage("[" + item.getDeviceName() + "] 로봇을 삭제하시겠습니까?")
                        .setPositiveButton("삭제", (dialog, which) -> {
                            deleteRobotAt(pos);
                        })
                        .setNegativeButton("취소", null)
                        .show();
            }
        });

        // 롱클릭 시: 로봇 정보 수정 다이얼로그
        robotRecyclerViewAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View v, int pos) {
                if (pos < 0 || pos >= robotItemArrayList.size()) return;
                RobotItem item = robotItemArrayList.get(pos);
                showEditRobotDialog(pos, item);
            }
        });

        // + 버튼: 로봇 등록 다이얼로그
        fabAdd.setOnClickListener(v -> showAddRobotDialog());

        mqttStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!MqttForegroundService.ACTION_MQTT_STATUS.equals(intent.getAction())) return;

                String status = intent.getStringExtra(MqttForegroundService.EXTRA_STATUS);
                if (status == null) return;

                if ("connected".equals(status)) {
                    // 마지막으로 connectToRobot에서 선택한 로봇 이름을 가져와 연결 표시
                    SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    String lastDeviceName = sp.getString(KEY_LAST_SELECTED, null);
                    updateConnectedRobot(lastDeviceName, true);
                } else if ("disconnected".equals(status)) {
                    // 전부 해제
                    updateConnectedRobot(null, false);
                }
                // "reconnecting" 같은 상태도 필요하면 처리 가능
            }
        };
    }
    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mqttStatusReceiver,
                new IntentFilter(MqttForegroundService.ACTION_MQTT_STATUS));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mqttStatusReceiver);
    }
    private void updateConnectedRobot(@Nullable String deviceName, boolean connected) {
        for (RobotItem item : robotItemArrayList) {
            if (deviceName != null && deviceName.equals(item.getDeviceName()) && connected) {
                item.setConnected(true);
            } else {
                item.setConnected(false);
            }
        }
        robotRecyclerViewAdapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        // 항상 최신 Robots_json 기준으로 리스트를 다시 구성
        robotsJson = setting.getString("Robots_json", "[]");
        robotItemArrayList.clear();
        try {
            JSONArray jsonArray = new JSONArray(robotsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                RobotItem robotItem = new RobotItem(obj);
                robotItemArrayList.add(robotItem);
            }
            robotRecyclerViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Log.e("RobotList", "리스트 불러오는 중 오류", e);
        }
        // ✅ 현재 MQTT 상태 한번 물어보기 → 서비스가 ACTION_MQTT_STATUS 브로드캐스트 날려줌
        Intent q = new Intent(this, MqttForegroundService.class);
        q.setAction(MqttForegroundService.ACTION_QUERY_STATUS);
        startService(q);
    }

    private void showAddRobotDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_robot_edit, null, false);

        EditText editDeviceName = dialogView.findViewById(R.id.editDeviceName);
        EditText editHost = dialogView.findViewById(R.id.editMqttHost);
        EditText editPort = dialogView.findViewById(R.id.editMqttPort);
        EditText editUsername = dialogView.findViewById(R.id.editMqttUsername);
        EditText editPassword = dialogView.findViewById(R.id.editMqttPassword);
        EditText editRootTopic = dialogView.findViewById(R.id.editMqttRootTopic);
        EditText editBaseTopic = dialogView.findViewById(R.id.editMqttBaseTopic);

        // 다크 테마용 텍스트 색
        editDeviceName.setTextColor(Color.WHITE);
        editHost.setTextColor(Color.WHITE);
        editPort.setTextColor(Color.WHITE);
        editUsername.setTextColor(Color.WHITE);
        editPassword.setTextColor(Color.WHITE);
        editRootTopic.setTextColor(Color.WHITE);
        editBaseTopic.setTextColor(Color.WHITE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("로봇 등록");
        builder.setView(dialogView);
        builder.setPositiveButton("저장", (dialog, which) -> {
            String deviceName = editDeviceName.getText().toString().trim();
            String host = editHost.getText().toString().trim();
            String portStr = editPort.getText().toString().trim();
            String username = editUsername.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            String rootTopic = editRootTopic.getText().toString().trim();
            String baseTopic = editBaseTopic.getText().toString().trim();

            if (deviceName.isEmpty() || host.isEmpty() || portStr.isEmpty()) {
                Toast.makeText(this, "이름, 호스트, 포트는 필수입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            int port = 1883;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "포트 숫자가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            }

            RobotItem item = new RobotItem(
                    deviceName,
                    host,
                    port,
                    username,
                    password,
                    rootTopic,
                    baseTopic
            );
            robotItemArrayList.add(item);

            try {
                JSONObject newItem = item.getObject();
                JSONArray jsonArray = new JSONArray(robotsJson);
                jsonArray.put(newItem);
                robotsJson = jsonArray.toString();
                editor.putString("Robots_json", robotsJson);
                editor.apply();

                robotRecyclerViewAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                Log.e("RobotList", "로봇 추가 저장 중 오류", e);
            }
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    /**
     * 롱클릭 시 호출: 기존 RobotItem 수정용 다이얼로그
     */
    private void showEditRobotDialog(int position, RobotItem item) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_robot_edit, null, false);

        EditText editDeviceName = dialogView.findViewById(R.id.editDeviceName);
        EditText editHost = dialogView.findViewById(R.id.editMqttHost);
        EditText editPort = dialogView.findViewById(R.id.editMqttPort);
        EditText editUsername = dialogView.findViewById(R.id.editMqttUsername);
        EditText editPassword = dialogView.findViewById(R.id.editMqttPassword);
        EditText editRootTopic = dialogView.findViewById(R.id.editMqttRootTopic);
        EditText editBaseTopic = dialogView.findViewById(R.id.editMqttBaseTopic);

        // 다크 테마용 텍스트 색
        editDeviceName.setTextColor(Color.WHITE);
        editHost.setTextColor(Color.WHITE);
        editPort.setTextColor(Color.WHITE);
        editUsername.setTextColor(Color.WHITE);
        editPassword.setTextColor(Color.WHITE);
        editRootTopic.setTextColor(Color.WHITE);
        editBaseTopic.setTextColor(Color.WHITE);

        // 기존 값 세팅
        editDeviceName.setText(item.getDeviceName());
        editHost.setText(item.getMqttConnectUrl());
        editPort.setText(String.valueOf(item.getPort()));
        editUsername.setText(item.getMqttUsername());
        editPassword.setText(item.getMqttPassword());
        editRootTopic.setText(item.getMqttRootTopic());
        editBaseTopic.setText(item.getMqttBaseTopic());

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("로봇 수정");
        builder.setView(dialogView);
        builder.setPositiveButton("저장", (dialog, which) -> {
            String deviceName = editDeviceName.getText().toString().trim();
            String host = editHost.getText().toString().trim();
            String portStr = editPort.getText().toString().trim();
            String username = editUsername.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            String rootTopic = editRootTopic.getText().toString().trim();
            String baseTopic = editBaseTopic.getText().toString().trim();

            if (deviceName.isEmpty() || host.isEmpty() || portStr.isEmpty()) {
                Toast.makeText(this, "이름, 호스트, 포트는 필수입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            int port = 1883;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "포트 숫자가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            }

            // 메모리 상 리스트 수정
            RobotItem updated = new RobotItem(
                    deviceName,
                    host,
                    port,
                    username,
                    password,
                    rootTopic,
                    baseTopic
            );
            robotItemArrayList.set(position, updated);
            robotRecyclerViewAdapter.notifyItemChanged(position);

            // Robots_json 도 수정
            try {
                String jsonStr = setting.getString("Robots_json", "[]");
                JSONArray jsonArray = new JSONArray(jsonStr);
                JSONObject updatedObj = updated.getObject();

                if (position >= 0 && position < jsonArray.length()) {
                    jsonArray.put(position, updatedObj); // 해당 인덱스 덮어쓰기
                } else {
                    // 혹시 사이즈 꼬였으면 그냥 뒤에 추가
                    jsonArray.put(updatedObj);
                }

                robotsJson = jsonArray.toString();
                editor.putString("Robots_json", robotsJson);
                editor.apply();
            } catch (JSONException e) {
                Log.e("RobotList", "로봇 수정 저장 중 오류", e);
            }
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    /**
     * 실제 삭제 처리: 리스트 & SharedPreferences(JSON)
     */
    private void deleteRobotAt(int position) {
        if (position < 0 || position >= robotItemArrayList.size()) return;

        // 메모리 상 리스트에서 제거
        robotItemArrayList.remove(position);
        robotRecyclerViewAdapter.notifyItemRemoved(position);

        // Robots_json에서도 제거
        try {
            String jsonStr = setting.getString("Robots_json", "[]");
            JSONArray jsonArray = new JSONArray(jsonStr);
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < jsonArray.length(); i++) {
                if (i == position) continue; // 삭제할 인덱스는 건너뛰기
                newArray.put(jsonArray.getJSONObject(i));
            }

            robotsJson = newArray.toString();
            editor.putString("Robots_json", robotsJson);
            editor.apply();
        } catch (JSONException e) {
            Log.e("RobotList", "로봇 삭제 저장 중 오류", e);
        }

        Toast.makeText(this, "로봇이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void connectToRobot(RobotItem item) {
        // (선택) 마지막 선택 로봇 기억
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        sp.edit()
                .putString(KEY_LAST_SELECTED, item.getDeviceName())
                .apply();

        // 서비스에 직접 정보 전달
        Intent intent = new Intent(this, MqttForegroundService.class);
        intent.setAction(MqttForegroundService.ACTION_CONNECT);

        intent.putExtra(MqttForegroundService.EXTRA_MQTT_HOST, item.getMqttConnectUrl());
        intent.putExtra(MqttForegroundService.EXTRA_MQTT_PORT, item.getPort());
        intent.putExtra(MqttForegroundService.EXTRA_MQTT_USERNAME, item.getMqttUsername());
        intent.putExtra(MqttForegroundService.EXTRA_MQTT_PASSWORD, item.getMqttPassword());
        intent.putExtra(MqttForegroundService.EXTRA_MQTT_ROOT_TOPIC, item.getMqttRootTopic());
        intent.putExtra(MqttForegroundService.EXTRA_MQTT_BASE_TOPIC, item.getMqttBaseTopic());
        intent.putExtra(MqttForegroundService.EXTRA_DEVICE_NAME, item.getDeviceName());

        ContextCompat.startForegroundService(this, intent);

        Toast.makeText(this, "MQTT 연결 시도 중...", Toast.LENGTH_SHORT).show();
    }
}
