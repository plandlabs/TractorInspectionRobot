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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.text.DecimalFormat;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.detector.XScrollDetector;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.SharedMqttViewModel;
import kr.re.kitech.tractorinspectionrobot.views.activity.MainActivity;
import kr.re.kitech.tractorinspectionrobot.views.component.ControlDrivenTouchButtons;
import kr.re.kitech.tractorinspectionrobot.views.component.ControlMovementTouchButtons;
import kr.re.kitech.tractorinspectionrobot.views.component.ControlSpeedMonitAndButtons;
import okhttp3.OkHttpClient;

public class PageFragment1 extends Fragment {
    private Resources mResources;
    private Configuration mConfiguration;
    public static PageFragment1 fContext;
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

    private ControlDrivenTouchButtons controlDrivenTouchButtons;
    private ControlMovementTouchButtons controlMovementTouchButtons;
    private ControlSpeedMonitAndButtons controlSpeedMonitAndButtons;

    public static PageFragment1 newInstance(){
        PageFragment1 pageActivity = new PageFragment1();
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

        linearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_page_1, container, false);
        viewBox = (LinearLayout) linearLayout.findViewById(R.id.viewBox);
        leftWrap = (LinearLayout) linearLayout.findViewById(R.id.leftWrap);
        stateBox = (LinearLayout) linearLayout.findViewById(R.id.stateBox);
        ctrlBox = (LinearLayout) linearLayout.findViewById(R.id.ctrlBox);
        stateGrid = (GridLayout) linearLayout.findViewById(R.id.stateGrid);
        wrapLinear = (LinearLayout) linearLayout.findViewById(R.id.wrapLinear);

        userName = (TextView) linearLayout.findViewById(R.id.userName);
        userName.setText(setting.getString("USER_NM",""));


        controlDrivenTouchButtons = linearLayout.findViewById(R.id.control_driven_touch_buttons);
        controlDrivenTouchButtons.updateButtonGroups("ALL");
        controlMovementTouchButtons = linearLayout.findViewById(R.id.control_movement_touch_buttons);
        controlSpeedMonitAndButtons =  linearLayout.findViewById(R.id.control_speed_custom_view);

        this.onConfigurationChanged(mConfiguration);

        csName = (TextView) linearLayout.findViewById(R.id.csName);
        chargingStDt = (TextView) linearLayout.findViewById(R.id.chargingStDt);
        chargingKW = (TextView) linearLayout.findViewById(R.id.chargingKW);
        chargingCost = (TextView) linearLayout.findViewById(R.id.chargingCost);


        testBtn = (TextView) linearLayout.findViewById(R.id.testBtn);

        return linearLayout;

    }


    @Override
    public void onStart() {
        super.onStart();
//        viewModel = new ViewModelProvider(requireActivity()).get(SharedMqttViewModel.class);
//        viewModel.getCoilDataMap().observe(getViewLifecycleOwner(), coilDataMap -> {
//            if (coilDataMap != null) {
//                resCoilDataMap = coilDataMap;
//            }
//        });
//        viewModel.getHoldingDataMap().observe(getViewLifecycleOwner(), holdingDataMap -> {
//            if (holdingDataMap != null) {
//                resHoldingDataMap = holdingDataMap;
//            }
//        });
//        viewModel.getSocketService().observe(getViewLifecycleOwner(), socketService -> {
//            if (socketService != null) {
//                resCustomSocketService = socketService;
//            }
//        });
    }


    @Override
    public void onStop() {
        super.onStop();
        //mapViewContainer.removeAllViews();
    }

    @Override
    public void onResume() {
        super.onResume();

        controlDrivenTouchButtons.setViewModel(viewModel, getViewLifecycleOwner());
        controlMovementTouchButtons.setViewModel(viewModel, getViewLifecycleOwner());
        controlSpeedMonitAndButtons.setViewModel(viewModel, getViewLifecycleOwner());
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

