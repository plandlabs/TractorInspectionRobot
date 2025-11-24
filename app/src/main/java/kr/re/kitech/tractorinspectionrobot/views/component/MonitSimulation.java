package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import org.json.JSONObject;

import java.util.ArrayList;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;
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
    private View progress;


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
        progress = findViewById(R.id.progress);
        progress.setVisibility(View.GONE);
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
        viewModel.getMqttConnected().observe(lifecycleOwner, s -> {
            if (s){
                progress.setVisibility(View.VISIBLE);
            }else{
                progress.setVisibility(View.GONE);
            }
        });
        viewModel.getFirstConnectReceive().observe(lifecycleOwner, s -> {
            if (s){
                progress.setVisibility(View.VISIBLE);
            }else{
                progress.setVisibility(View.GONE);
            }
        });
        // 관찰 가능!
    }
    private void sendStateToWeb(RobotState s) {
        if (robotSimulation == null) return;
        // s.toJson() 사용: {"x":...,"y":...,"z":...,"s1":...,"s2":...,"s3":...,"ts":...}
        String json = s.toJson().toString();
        String js = "window.onStateUpdate && window.onStateUpdate(" + JSONObject.quote(json) + ");";
        runJs(js);
    }
    private void runJs(String js){
        if (!pageReady){ pendingJs.add(js); return; }
        robotSimulation.post(() -> {
            if (Build.VERSION.SDK_INT >= 19) {
                robotSimulation.evaluateJavascript(js, null);
            } else {
                robotSimulation.loadUrl("javascript:"+js);
            }
        });
    }
    private void flushPendingJs(){
        for (String js : pendingJs) runJs(js);
        pendingJs.clear();
    }

}
