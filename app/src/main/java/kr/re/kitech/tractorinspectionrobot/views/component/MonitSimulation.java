package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import org.json.JSONObject;

import java.util.ArrayList;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.RobotState;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

public class MonitSimulation extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private WebView robotSimulation;
    private boolean pageReady = false;
    private final ArrayList<String> pendingJs = new ArrayList<>();
    private RobotState lastState = null;


    public MonitSimulation(Context context) {
        super(context);
        init(context);
    }

    public MonitSimulation(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    @SuppressLint("SetJavaScriptEnabled")
    private void init(Context context) {
        inflate(context, R.layout.component_monit_simulation, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        robotSimulation = (WebView) findViewById(R.id.robotSimulation);
        WebSettings s = robotSimulation.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        WebView.setWebContentsDebuggingEnabled(true); // 콘솔 로그를 Logcat에서 보려면
        robotSimulation.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                // 대기 중이던 JS 처리
                flushPendingJs();
            }
        });
        robotSimulation.loadUrl("file:///android_asset/simulation/index.html");
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        viewModel.getState().observe(lifecycleOwner, s -> {
            if (s == null) return;
            lastState = s;                    // 페이지 재로딩 시 재전송용 캐시
            sendStateToWeb(s);
        });
        // 관찰 가능!
    }
    private void sendStateToWeb(RobotState s) {
        if (robotSimulation == null) return;
        // s.toJson() 사용: {"x":...,"y":...,"z":...,"pan":...,"tilt":...,"ts":...}
        String json = s.toJson().toString();
        String js = "window.onStateUpdate && window.onStateUpdate(" + JSONObject.quote(json) + ");";
        runJs(js);
    }
    private void handleMqttMessage(String topic, String payload) {
        if (robotSimulation == null) return;

        // JS 문자열 리터럴로 안전하게 인코딩
        String js = "window.onMqttMessage && window.onMqttMessage("
                + JSONObject.quote(topic)   // 'topic' 안전화
                + ","
                + JSONObject.quote(payload) // 'payload'를 "JSON 문자열"로 보냄
                + ");";

        runJs(js);
    }
    private void runJs(String js){
        if (!pageReady){ pendingJs.add(js); return; }
        robotSimulation.post(() -> {
            if (Build.VERSION.SDK_INT >= 19) {
                Log.w("robotSimulation.post 1",js);
                robotSimulation.evaluateJavascript(js, null);
            } else {
                Log.w("robotSimulation.post 2",js);
                robotSimulation.loadUrl("javascript:"+js);
            }
        });
    }
    private void flushPendingJs(){
        for (String js : pendingJs) runJs(js);
        pendingJs.clear();
    }

}
