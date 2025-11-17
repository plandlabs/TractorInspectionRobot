package kr.re.kitech.tractorinspectionrobot.views.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.adapter.OnItemClickListener;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.adapter.RobotRecyclerView;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.model.RobotItem;
import kr.re.kitech.tractorinspectionrobot.wifi.WifiConnector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RobotListActivity extends Activity {

    private RecyclerView robotRecyclerView;
    private RobotRecyclerView robotRecyclerViewAdapter;
    private ArrayList<RobotItem> robotItemArrayList;
    private SharedPreferences setting;
    private SharedPreferences.Editor editor;
    private SharedMqttViewModel viewModel;
    private Vibrator mVibrator;
    private View btnBack;
    private boolean isMqttConnected = false;
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_list);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setting = getSharedPreferences("setting", 0);
        editor = setting.edit();
        robotItemArrayList = new ArrayList<>();
        btnBack = (View) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(view -> {
            mVibrator.vibrate(100);
            finish();
        });

        viewModel = new ViewModelProvider((ViewModelStoreOwner) this).get(SharedMqttViewModel.class);

        // 옵저버: 캐시 + UI 동기화
        viewModel.getMqttConnected().observe((LifecycleOwner) this, connected -> {
            isMqttConnected = Boolean.TRUE.equals(connected);
        });

        initUI();

        this.cleanUpRobotsJson();  // 여기서 한 번 호출! 불필요한(빈 값) 데이터 제거 후 저장
    }

    private void initUI() {
        robotRecyclerView = findViewById(R.id.robotRecyclerView);
        robotRecyclerViewAdapter = new RobotRecyclerView(robotItemArrayList, this);
        robotRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        robotRecyclerView.setAdapter(robotRecyclerViewAdapter);

        robotRecyclerViewAdapter.setOnItemClickListener(new OnItemClickListener() {
            private void promptPasswordAndConnect(RobotItem item) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext(), R.style.DarkAlertDialog);

                LinearLayout layout = new LinearLayout(getApplicationContext());
                layout.setPadding(50, 40, 50, 10);
                final EditText input = new EditText(getApplicationContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                if(item.getIsConnected()) {
                    builder.setTitle("비밀번호 등록(SSID : " + item.getSsid() + ")");
                    input.setHint("Wi-Fi 비밀번호를 입력하세요. 앱에 저장하려면 입력한 비밀번호로 실제 연결을 재시도합니다.");
                }else {
                    builder.setTitle("비밀번호 입력(SSID : " + item.getSsid() + ")");
                    input.setHint("Wi-Fi 비밀번호를 입력하세요.");
                }
                // 자판 클릭시마다 진동 추가
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mVibrator.vibrate(100);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
                layout.addView(input);
                builder.setView(layout);

                builder.setPositiveButton("확인", (dialog, which) -> {
                    mVibrator.vibrate(100);
                    String password = input.getText().toString();
                });

                builder.setNegativeButton("취소", null);
                builder.show();
            }
            private void removeRobotFromSavedList(String ssid, String mac) {
                try {
                    String robotsJson = setting.getString("ROBOTS_JSON", "[]");
                    JSONArray jsonArray = new JSONArray(robotsJson);
                    JSONArray newArray = new JSONArray();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        if (!(obj.optString("ssid").equals(ssid) && obj.optString("mac").equals(mac))) {
                            newArray.put(obj);
                        }
                    }
                    editor.putString("ROBOTS_JSON", newArray.toString());
                    editor.apply();
                } catch (JSONException e) {
                    Log.e("RemoveRobot", "로봇 삭제 중 오류", e);
                }
            }

            @Override
            public void onItemClick(View v, int pos) {
                mVibrator.vibrate(100);
                RobotItem item = robotItemArrayList.get(pos);
                if (item.getIsConnected() && item.getIsSaved()) {
                    Toast.makeText(getApplicationContext(), "이미 연결된 로봇입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            @Override
            public void onItemDeleteClick(View v, int pos) {
                mVibrator.vibrate(100);
                AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext(), R.style.DarkAlertDialog);
                builder.setTitle("로봇 삭제");
                builder.setMessage("해당 로봇을 삭제하시겠습니까?");
                builder.setPositiveButton("확인", (dialog, which) -> {
                    mVibrator.vibrate(100);
                    RobotItem targetItem = robotItemArrayList.get(pos);
                    String robotsJson = setting.getString("ROBOTS_JSON", "[]");
                    try {
                        JSONArray jsonArray = new JSONArray(robotsJson);
                        JSONArray newArray = new JSONArray();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            if (!obj.optString("mac").equals(targetItem.getMac())) {
                                newArray.put(obj);
                            }
                        }
                        editor.putString("ROBOTS_JSON", newArray.toString());
                        editor.apply();
                        robotItemArrayList.remove(pos);
                        robotRecyclerViewAdapter.notifyItemRemoved(pos);
                    } catch (JSONException e) {
                        Log.e("DeleteRobot", "삭제 중 오류", e);
                    }
                });
                builder.setNegativeButton("취소", null);
                builder.show();
            }

            @Override
            public void onItemAddClick(View v, int pos) {
                Log.d("ItemAdd", "추가 클릭됨: " + pos);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void cleanUpRobotsJson() {
        try {
            String robotsJson = setting.getString("ROBOTS_JSON", "[]");
            JSONArray jsonArray = new JSONArray(robotsJson);
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String ssid = obj.optString("ssid", "");
                String mac = obj.optString("mac", "");
                // ssid, mac이 비어있지 않은 경우에만 추가
                if (ssid != null && !ssid.isEmpty() && mac != null && !mac.isEmpty()) {
                    newArray.put(obj);
                }
            }

            editor.putString("ROBOTS_JSON", newArray.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e("CleanUpRobot", "정리 중 오류", e);
        }
    }

}
