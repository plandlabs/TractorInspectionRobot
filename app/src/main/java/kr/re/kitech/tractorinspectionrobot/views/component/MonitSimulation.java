package kr.re.kitech.tractorinspectionrobot.views.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;

public class MonitSimulation extends LinearLayout {
    private Vibrator mVibrator;
    private SharedMqttViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GridLayout gridLayout;
    private WebView robotSimulation;


    public MonitSimulation(Context context) {
        super(context);
        init(context);
    }

    public MonitSimulation(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        inflate(context, R.layout.component_monit_simulation, this);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        robotSimulation = (WebView) findViewById(R.id.robotSimulation);
        WebSettings s = robotSimulation.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        if (Build.VERSION.SDK_INT >= 16) {
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
        }
        robotSimulation.loadUrl("file:///android_asset/simulation/index.html");
    }
    @SuppressLint("ClickableViewAccessibility")
    public void setViewModel(SharedMqttViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        // 관찰 가능!
    }

}
