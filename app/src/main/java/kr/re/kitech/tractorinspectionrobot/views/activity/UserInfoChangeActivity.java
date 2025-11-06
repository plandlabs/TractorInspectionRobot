package kr.re.kitech.tractorinspectionrobot.views.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import kr.re.kitech.tractorinspectionrobot.R;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * A login screen that offers login via email/password.
 */
public class UserInfoChangeActivity extends Activity {
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
    private LinearLayout idConfBlock;
    private TextView id;
    private AutoCompleteTextView name, phone, email;
    private Button btnJoin;
    private View mProgressView;
    private OkHttpClient okHttpClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info_change);
        // Set up the login form.
        ll = (LinearLayout)findViewById(R.id.ll);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        okHttpClient = new OkHttpClient();

        LinearLayout mBtnBack = (LinearLayout) findViewById(R.id.btnBack);
        //ll.setOnClickListener(myClickListener);
        mBtnBack.setOnClickListener(v -> {
            finish();
        });
        mProgressView = findViewById(R.id.progressCircle);

        setting = getSharedPreferences("setting", 0);
        editor= setting.edit();

        idConfBlock = (LinearLayout) findViewById(R.id.idConfBlock);
        id = (TextView) findViewById(R.id.id);
        id.setText(setting.getString("ID", ""));

        name = (AutoCompleteTextView) findViewById(R.id.name);
        phone = (AutoCompleteTextView) findViewById(R.id.phone);
        email = (AutoCompleteTextView) findViewById(R.id.email);
        btnJoin = (Button) findViewById(R.id.btnJoin);
        btnJoin.setOnClickListener(v -> {
            mVibrator.vibrate(100);
            AlertDialog.Builder builder = new AlertDialog.Builder(UserInfoChangeActivity.this, R.style.DarkAlertDialog);
            builder.setTitle("회원정보 변경");
            builder.setMessage("회원정보 변경을 하시겠습니까?");
            builder.setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mVibrator.vibrate(10);
                            fnUserRegister();
                        }
                    });
            builder.setNegativeButton("취소",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            builder.show();
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        //this.fnUserInfoCall();
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

    public void fnUserRegister(){
        String nameStr = name.getText().toString();
        String phoneStr = phone.getText().toString();
        String emailStr = email.getText().toString();
        if(nameStr.isEmpty()){
            Toast.makeText(getApplicationContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(phoneStr.isEmpty()){
            Toast.makeText(getApplicationContext(), "휴대폰번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(emailStr.isEmpty()){
            Toast.makeText(getApplicationContext(), "이메일주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("user_id", setting.getString("ID", ""))
                .add("user_nm", nameStr)
                .add("user_phone", phoneStr)
                .add("user_mail", emailStr)
                .build();

        // 요청 만들기
//        String url = getResources().getString(R.string.http_connect_url)+"/userInfoChange.json";
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
//                                    AlertDialog.Builder builder = new AlertDialog.Builder(UserInfoChangeActivity.this, R.style.DarkAlertDialog);
//                                    builder.setTitle("회원정보 변경완료");
//                                    if(mJSONObject.getInt("affectedRows") > 0) {
//                                        builder.setMessage("회원정보 변경이 완료되었습니다.");
//                                        builder.setPositiveButton("확인",
//                                                new DialogInterface.OnClickListener() {
//                                                    public void onClick(DialogInterface dialog, int which) {
//                                                        mVibrator.vibrate(10);
//                                                        finish();
//                                                    }
//                                                });
//                                    }else{
//                                        builder.setMessage("회원정보 변경이 완료되지 않았습니다.");
//                                        builder.setPositiveButton("확인",
//                                                new DialogInterface.OnClickListener() {
//                                                    public void onClick(DialogInterface dialog, int which) {
//                                                        mVibrator.vibrate(10);
//                                                    }
//                                                });
//                                    }
//                                    builder.show();
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

