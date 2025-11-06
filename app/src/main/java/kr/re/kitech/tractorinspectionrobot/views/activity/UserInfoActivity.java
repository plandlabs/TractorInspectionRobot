package kr.re.kitech.tractorinspectionrobot.views.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import kr.re.kitech.tractorinspectionrobot.R;

import okhttp3.OkHttpClient;

/**
 * A login screen that offers login via email/password.
 */
public class UserInfoActivity extends Activity {
    InputMethodManager imm;
    LinearLayout ll;

    SharedPreferences setting;
    SharedPreferences.Editor editor;
    private Vibrator mVibrator;

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    // UI references.
    private TextView id, name, phone, email;
    private Button btnUserInfoChange, btnPasswordChange;
    private View mProgressView;
    private OkHttpClient okHttpClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        // Set up the login form.
        ll = (LinearLayout)findViewById(R.id.ll);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        okHttpClient = new OkHttpClient();

        LinearLayout mBtnBack = (LinearLayout) findViewById(R.id.btnBack);
        //ll.setOnClickListener(myClickListener);
        //mLogInButton.setOnClickListener(myClickListener);
        mBtnBack.setOnClickListener(v -> {
            finish();
        });
        mProgressView = findViewById(R.id.progressCircle);

        setting = getSharedPreferences("setting", 0);
        editor= setting.edit();
        id = (TextView) findViewById(R.id.id);
        name = (TextView) findViewById(R.id.name);
        phone = (TextView) findViewById(R.id.phone);
        email = (TextView) findViewById(R.id.email);

        btnUserInfoChange = (Button) findViewById(R.id.btnUserInfoChange);
        btnUserInfoChange.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), UserInfoChangeActivity.class));
            }
        });
        btnPasswordChange = (Button) findViewById(R.id.btnPasswordChange);
        btnPasswordChange.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), PasswordChangeActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
//        fnUserInfoCall();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);


            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View focusView = getCurrentFocus();
        if (focusView != null) {
            Rect rect = new Rect();
            focusView.getGlobalVisibleRect(rect);
            int x = (int) ev.getX(), y = (int) ev.getY();
            if (!rect.contains(x, y)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
                focusView.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void fnUserInfoCall(){
        // POST 파라미터 추가
//        RequestBody formBody = new FormBody.Builder()
//                .add("user_id", setting.getString("ID", ""))
//                .build();
//
//        // 요청 만들기
//        String url = getResources().getString(R.string.http_connect_url)+"/callData/userInfo.json";
//        Request request = new Request.Builder()
//                .url(url)
//                .post(formBody)
//                .build();
//
//        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, final Response response) throws IOException {
//                if (!response.isSuccessful()) {
//                    // 응답 실패
//                    Log.i("tag", "응답실패");
//                } else {
//                    // 응답 성공
//                    Log.i("tag", "응답 성공");
//                    final String responseData = response.body().string();
//                    // 서브 스레드 Ui 변경 할 경우 에러
//                    // 메인스레드 Ui 설정
//                    Log.e("responseData", responseData);
//                    try {
//                        JSONObject mJSONObject = new JSONObject(responseData);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    id.setText(mJSONObject.getString("user_id"));
//                                    name.setText(mJSONObject.getString("user_nm"));
//                                    phone.setText(mJSONObject.getString("user_phone"));
//                                    email.setText(mJSONObject.getString("user_mail"));
//                                } catch (JSONException e) {
//                                    throw new RuntimeException(e);
//                                }
//                            }
//                        });
//
//                    } catch (JSONException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//        });
    }

}

