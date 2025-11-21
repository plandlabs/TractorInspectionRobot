package kr.re.kitech.tractorinspectionrobot.views.fragment;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.detector.XScrollDetector;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModelBridge;
import kr.re.kitech.tractorinspectionrobot.views.activity.MainActivity;
import kr.re.kitech.tractorinspectionrobot.views.component.ControlCameraMovementTouchButtons;
import kr.re.kitech.tractorinspectionrobot.views.component.ControlEmergencyButtons;
import kr.re.kitech.tractorinspectionrobot.views.component.ControlVimMovementTouchButtons;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.text.DecimalFormat;

import kr.re.kitech.tractorinspectionrobot.views.component.MonitCamera;
import kr.re.kitech.tractorinspectionrobot.views.component.ControlProgram;
import kr.re.kitech.tractorinspectionrobot.views.component.MonitSimulation;
import kr.re.kitech.tractorinspectionrobot.views.component.MonitTarget;
import okhttp3.OkHttpClient;

public class PageFragment0 extends Fragment {
    private Resources mResources;
    private Configuration mConfiguration;
    public static PageFragment0 fContext;
    private SharedMqttViewModel viewModel;
    private Activity activity;
    private OkHttpClient okHttpClient;
    public SharedPreferences setting;
    public SharedPreferences.Editor editor;
    private Vibrator mVibrator;
    public SwipeRefreshLayout mSwipeRefreshLayout = null;
    private DotsIndicator indicator;

    private TextView userName, emptyCharging, emptyCharged;
    private TableLayout chargingInfo;

    private LinearLayout linearLayout, viewBox;
    //fragmemt_page1_left
    private LinearLayout leftWrap, stateBox, ctrlBox;
    private GridLayout stateGrid;


    private TextView csName, chargingStDt, chargingKW, chargingCost;
    private DecimalFormat costDf;

    private GestureDetector mGestureDetector;
    private boolean isLockOnHorizontialAxis;

    private TextView testBtn;
    private MainActivity mainActivity;
    private int ctrlBoolForward = 0;
    private LinearLayout wrapLinear;

    private MonitSimulation monitSimulation;
    private ControlProgram controlProgram;
    private MonitTarget monitTarget;
    private ControlVimMovementTouchButtons controlVimMovementTouchButtons;
    private ControlEmergencyButtons controlEmergencyButtons;
    private MonitCamera monitCamera;
    private ControlCameraMovementTouchButtons controlCameraMovementTouchButtons;

    public static PageFragment0 newInstance(){
        PageFragment0 pageActivity = new PageFragment0();
        return pageActivity;
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fContext = this;
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        mResources = Resources.getSystem();
        mConfiguration = mResources.getConfiguration();
        okHttpClient = new OkHttpClient();
        setting = getActivity().getSharedPreferences("setting", 0);
        editor= setting.edit();

        mGestureDetector = new GestureDetector(getContext(), new XScrollDetector());
        costDf = (DecimalFormat) new DecimalFormat("#,###");
//        socketTask = ((MainActivity) MainActivity.mContext).socketTask;
//        socketClient = socketTask.socketClient;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            activity = (Activity) context;
        }

    }


    @SuppressLint({"MissingPermission", "ClickableViewAccessibility"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @NonNull Bundle savedInstanceState) {

        linearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_page_0, container, false);
        viewBox = (LinearLayout) linearLayout.findViewById(R.id.viewBox);
        leftWrap = (LinearLayout) linearLayout.findViewById(R.id.leftWrap);
        stateBox = (LinearLayout) linearLayout.findViewById(R.id.stateBox);
        ctrlBox = (LinearLayout) linearLayout.findViewById(R.id.ctrlBox);
        wrapLinear = (LinearLayout) linearLayout.findViewById(R.id.wrapLinear);

        userName = (TextView) linearLayout.findViewById(R.id.userName);
        userName.setText(setting.getString("USER_NM",""));


        monitSimulation = linearLayout.findViewById(R.id.monit_simulation);
        controlProgram = linearLayout.findViewById(R.id.control_program);
        monitTarget = linearLayout.findViewById(R.id.monit_target);
        controlVimMovementTouchButtons = linearLayout.findViewById(R.id.control_vim_movement_touch_buttons);
        controlEmergencyButtons = linearLayout.findViewById(R.id.control_emergency_buttons);
        monitCamera = linearLayout.findViewById(R.id.monit_camera);
        controlCameraMovementTouchButtons = linearLayout.findViewById(R.id.control_camera_movement_touch_buttons);

        this.onConfigurationChanged(mConfiguration);


        testBtn = (TextView) linearLayout.findViewById(R.id.testBtn);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedMqttViewModel.class);
        SharedMqttViewModelBridge.getInstance().setViewModel(viewModel);

        monitSimulation.setViewModel(viewModel, getViewLifecycleOwner());
        controlProgram.setViewModel(viewModel, getViewLifecycleOwner());
        monitTarget.setViewModel(viewModel, getViewLifecycleOwner());
        controlVimMovementTouchButtons.setViewModel(viewModel, getViewLifecycleOwner());
        controlEmergencyButtons.setViewModel(viewModel, getViewLifecycleOwner());
        monitCamera.setViewModel(viewModel, getViewLifecycleOwner());
        controlCameraMovementTouchButtons.setViewModel(viewModel, getViewLifecycleOwner());

        return linearLayout;

    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        //mapViewContainer.removeAllViews();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public ActivityResultLauncher<Intent> startActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "MainActivity로 돌아왔다. ");
                    }
                }
            });

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("onDestroy","onDestroy");
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics = getContext().getResources().getDisplayMetrics();
        int dpi = displayMetrics.densityDpi;
    }



}

