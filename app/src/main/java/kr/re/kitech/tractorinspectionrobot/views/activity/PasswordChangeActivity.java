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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import kr.re.kitech.tractorinspectionrobot.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A login screen that offers login via email/password.
 */
public class PasswordChangeActivity extends Activity {
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
    private Button btnPasswordChange;
    private EditText passwordCur, passwordEditNew, passwordEditNewChk;
    private View mProgressView;
    private OkHttpClient okHttpClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change);
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


        passwordCur = (EditText) findViewById(R.id.passwordCur);
        passwordEditNew = (EditText) findViewById(R.id.passwordEditNew);
        passwordEditNewChk = (EditText) findViewById(R.id.passwordEditNewChk);
        btnPasswordChange = (Button) findViewById(R.id.btnPasswordChange);
        btnPasswordChange.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mVibrator.vibrate(10);
                fnPasswordChange();
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



    public void fnPasswordChange(){
        String passwordCurStr = passwordCur.getText().toString();
        String passwordEditNewStr = passwordEditNew.getText().toString();
        String passwordEditNewChkStr = passwordEditNewChk.getText().toString();
        if(passwordCurStr.isEmpty() || passwordCurStr.length() < 4){
            Toast.makeText(getApplicationContext(), "현재 사용중인 패스워드를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(passwordEditNewStr.length() < 4){
            Toast.makeText(getApplicationContext(), "아이디는 세 글자 이상 입력하셔야합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(passwordEditNewStr.isEmpty() || !passwordEditNewStr.equals(passwordEditNewChkStr)){
            Toast.makeText(getApplicationContext(), "새로운 패스워드를 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("user_id", setting.getString("ID", ""))
                .add("user_passwd", passwordCurStr)
                .add("user_passwd_new", passwordEditNewChkStr)
                .build();

        // 요청 만들기
        String url = getResources().getString(R.string.http_connect_url)+"/userPasswordChange.json";
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // 응답 실패
                    Log.i("tag", "응답실패");
                } else {
                    // 응답 성공
                    Log.i("tag", "응답 성공");
                    final String responseData = response.body().string();
                    // 서브 스레드 Ui 변경 할 경우 에러
                    // 메인스레드 Ui 설정
                    Log.e("responseData", responseData);
                    try {
                        JSONObject mJSONObject = new JSONObject(responseData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {

                                    if(mJSONObject.getBoolean("check")) {
                                        editor.putString("PW", passwordEditNewChkStr);
                                        editor.commit();

                                        AlertDialog.Builder builder = new AlertDialog.Builder(PasswordChangeActivity.this, R.style.DarkAlertDialog);
                                        builder.setTitle("패스워드 변경완료");
                                        builder.setMessage("패스워드가 변경되었습니다.");
                                        builder.setPositiveButton("확인",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        mVibrator.vibrate(10);
                                                        finish();
                                                    }
                                                });
                                        builder.show();
                                    }else{
                                        Toast.makeText(PasswordChangeActivity.this, "현재 패스워드를 확인해주세요.", Toast.LENGTH_SHORT).show();
                                    }

                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }
}

