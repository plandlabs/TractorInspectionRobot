package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

public class MonitCamera extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private WebView robotCam;
    private View progress;


    public MonitCamera(Context context) {
        super(context);
        init(context);
    }

    public MonitCamera(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    @SuppressLint("SetJavaScriptEnabled")
    private void init(Context context) {
        inflate(context, R.layout.component_monit_camera, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        robotCam = (WebView) findViewById(R.id.robotCam);
        WebSettings s = robotCam.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        progress = findViewById(R.id.progress);
        progress.setVisibility(View.GONE);
        robotCam.loadUrl("file:///android_asset/simulation/cam.html");
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

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

}
