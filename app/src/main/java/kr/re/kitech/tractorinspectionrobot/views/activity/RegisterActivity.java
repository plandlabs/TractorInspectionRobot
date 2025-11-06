package kr.re.kitech.tractorinspectionrobot.views.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import kr.re.kitech.tractorinspectionrobot.R;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * A login screen that offers login via email/password.
 */
public class RegisterActivity extends Activity {
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
    private LinearLayout idViewBlock, idConfBlock;
    private AutoCompleteTextView idView, name, phone, email;
    private TextView idConf;
    private Boolean idCheck = false;
    private CheckBox agree;
    private Button btnIdCheck, btnIdChange, btnJoin;
    private EditText passwordEdit, passwordEditChk;
    private View mProgressView;
    private OkHttpClient okHttpClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Set up the login form.
        ll = (LinearLayout)findViewById(R.id.ll);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        okHttpClient = new OkHttpClient();

        LinearLayout mBtnBack = (LinearLayout) findViewById(R.id.btnBack);
        //ll.setOnClickListener(myClickListener);
        //mLogInButton.setOnClickListener(myClickListener);
        mBtnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mProgressView = findViewById(R.id.progressCircle);

        setting = getSharedPreferences("setting", 0);
        editor= setting.edit();

        idViewBlock = (LinearLayout) findViewById(R.id.idViewBlock);
        idConfBlock = (LinearLayout) findViewById(R.id.idConfBlock);
        idConfBlock.setVisibility(View.GONE);
        idView = (AutoCompleteTextView) findViewById(R.id.idView);
        idConf = (TextView) findViewById(R.id.idConf);
        btnIdCheck = (Button) findViewById(R.id.btnIdCheck);
        btnIdChange = (Button) findViewById(R.id.btnIdChange);
        btnIdCheck.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mVibrator.vibrate(10);
                fnUserIdCheckCall();
            }
        });

        btnIdChange.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mVibrator.vibrate(10);
                idCheck = false;
                idConfBlock.setVisibility(View.GONE);
                idViewBlock.setVisibility(View.VISIBLE);
            }
        });

        passwordEdit = (EditText) findViewById(R.id.passwordEdit);
        passwordEditChk = (EditText) findViewById(R.id.passwordEditChk);
        name = (AutoCompleteTextView) findViewById(R.id.name);
        phone = (AutoCompleteTextView) findViewById(R.id.phone);
        email = (AutoCompleteTextView) findViewById(R.id.email);
        agree = (CheckBox) findViewById(R.id.agree);
        btnJoin = (Button) findViewById(R.id.btnJoin);
        btnJoin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mVibrator.vibrate(10);
                fnUserRegister();
            }
        });

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

    public void fnUserIdCheckCall(){
        String checkId = idView.getText().toString();
        if(checkId.isEmpty()){
            Toast.makeText(getApplicationContext(), "아이디를 입력하셔야합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(checkId.length() < 4){
            Toast.makeText(getApplicationContext(), "아이디는 세 글자 이상 입력하셔야합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("user_id", checkId)
                .build();

        // 요청 만들기
//        String url = getResources().getString(R.string.http_connect_url)+"/userCheck.json";
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
//                                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this, R.style.DarkAlertDialog);
//                                    builder.setTitle("아이디 중복체크");
//                                    if(mJSONObject.getBoolean("check")) {
//                                        builder.setMessage("중복된 아이디가 있습니다.");
//                                        builder.setPositiveButton("확인",
//                                                new DialogInterface.OnClickListener() {
//                                                    public void onClick(DialogInterface dialog, int which) {
//                                                        mVibrator.vibrate(10);
//                                                        idConf.setText(null);
//                                                        idCheck = false;
//                                                    }
//                                                });
//                                    }else{
//                                        builder.setMessage("사용하실 수 있는 아이디입니다.");
//                                        builder.setPositiveButton("확인",
//                                                new DialogInterface.OnClickListener() {
//                                                    public void onClick(DialogInterface dialog, int which) {
//                                                        mVibrator.vibrate(10);
//                                                        idConf.setText(idView.getText());
//                                                        idCheck = true;
//                                                        idConfBlock.setVisibility(View.VISIBLE);
//                                                        idViewBlock.setVisibility(View.GONE);
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

    public void fnUserRegister(){
        String passwordEditStr = passwordEdit.getText().toString();
        String passwordEditChkStr = passwordEditChk.getText().toString();
        String nameStr = name.getText().toString();
        String phoneStr = phone.getText().toString();
        String emailStr = email.getText().toString();
        if(!idCheck){
            Toast.makeText(getApplicationContext(), "아이디 중복체크를 하셔야합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(passwordEditStr.isEmpty() || !passwordEditStr.equals(passwordEditChkStr)){
            Toast.makeText(getApplicationContext(), "패스워드를 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
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
        if(!agree.isChecked()){
            Toast.makeText(getApplicationContext(), "개인정보이용에 동의하셔야합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        // POST 파라미터 추가
//        RequestBody formBody = new FormBody.Builder()
//                .add("user_id", idConf.getText().toString())
//                .add("user_passwd", passwordEditChkStr)
//                .add("user_nm", nameStr)
//                .add("user_phone", phoneStr)
//                .add("user_mail", emailStr)
//                .build();
//
//        // 요청 만들기
//        String url = getResources().getString(R.string.http_connect_url)+"/userInsert.json";
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
//                                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this, R.style.DarkAlertDialog);
//                                    builder.setTitle("회원가입 완료");
//                                    if(mJSONObject.getInt("affectedRows") > 0) {
//                                        builder.setMessage("회원가입이 완료되었습니다.");
//                                        builder.setPositiveButton("확인",
//                                                new DialogInterface.OnClickListener() {
//                                                    public void onClick(DialogInterface dialog, int which) {
//                                                        mVibrator.vibrate(10);
//                                                        finish();
//                                                    }
//                                                });
//                                    }else{
//                                        builder.setMessage("회원가입이 완료되지 않았습니다.");
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

