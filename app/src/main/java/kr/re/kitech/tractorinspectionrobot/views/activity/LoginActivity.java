package kr.re.kitech.tractorinspectionrobot.views.activity;

import static android.Manifest.permission.READ_CONTACTS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.utils.JsonFileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {
    InputMethodManager imm;
    CheckBox autoLogIn;
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
    private AutoCompleteTextView mIdView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Handler mHandler;
    private boolean mFlag = false;
    private ContentValues values = new ContentValues();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mIdView = (AutoCompleteTextView) findViewById(R.id.id);
        autoLogIn = (CheckBox) findViewById(R.id.auto_login);

        autoLogIn.setChecked(true);

        populateAutoComplete();
        ll = (LinearLayout)findViewById(R.id.ll);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });


        Button mBtnLogin = (Button) findViewById(R.id.btnLogin);
        mBtnLogin.setOnClickListener(v -> {
            attemptLogin();
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.progressCircle);

        setting = getSharedPreferences("setting", 0);
        editor= setting.edit();

        if(setting.getBoolean("Auto_Login_enabled", false)){
            mIdView.setText(setting.getString("ID", ""));
            mPasswordView.setText(setting.getString("PW", ""));
            autoLogIn.setChecked(true);
            //Toast.makeText(this, "자동 로그인 테스트.", Toast.LENGTH_SHORT).show();
            mBtnLogin.performClick();
        }

        autoLogIn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if(isChecked){

                    String ID = mIdView.getText().toString();
                    String PW = mPasswordView.getText().toString();

                    editor.putString("ID", ID);
                    editor.putString("PW", PW);
                    editor.putBoolean("Auto_Login_enabled", true);
                    editor.commit();
                }else{
                    /**
                     * remove로 지우는것은 부분삭제
                     * clear로 지우는것은 전체 삭제 입니다
                     */
                    editor.remove("ID");
                    editor.remove("PW");
                    editor.remove("Auto_Login_enabled");
//                    editor.clear();
                    editor.commit();
                }
            }
        });

        // 핸들러 객체 설정 (onCreate() 메소드 안에 구현!!!)
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0) {
                    mFlag = false;
                }
            }
        };
        // 안드로이드 단말에서 "뒤로가기" 버튼을 터치했을때, 실행된다.

    }

    @SuppressLint("GestureBackNavigation")
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // 백 키를 터치한 경우
        if(keyCode == KeyEvent.KEYCODE_BACK){


                if(!mFlag) {
                    Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
                    mFlag = true;
                    mHandler.sendEmptyMessageDelayed(0, 2000); // 2초 내로 터치시
                    return false;
                } else {
                    finish();
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mIdView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        mVibrator.vibrate(10);
        // Reset errors.
        mIdView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String id = mIdView.getText().toString();
        String password = mPasswordView.getText().toString();
        values.put("mber_id",id);
        values.put("mber_pwd",password);
        boolean cancel = false;
        View focusView = null;
        boolean memberChk = false;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(id)) {
            mIdView.setError(getString(R.string.error_field_required));
            focusView = mIdView;
            cancel = true;
        } /*else if (!isEmailValid(email)) {
            mIdView.setError(getString(R.string.error_invalid_email));
            focusView = mIdView;
            cancel = true;
        }
        */
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            String memberJson = JsonFileUtil.readJSONFromAsset(this, "member.json");
            Log.e("member.json",memberJson);
            try {
                JSONArray mJsonArray = new JSONArray(memberJson);
                String userName = null;
                for(int i = 0; i < mJsonArray.length(); i++){
                    JSONObject mJsonObject = new JSONObject(String.valueOf(mJsonArray.get(i)));
                    Log.e("id", mJsonObject.getString("id"));
                    Log.e("password", mJsonObject.getString("password"));
                    if(mJsonObject.getString("id").equals(id)
                            && mJsonObject.getString("password").equals(password)){
                        memberChk = true;
                        userName = mJsonObject.getString("name");
                    }
                }

                if(memberChk){
                    Toast.makeText(getApplicationContext(), "로그인 되었습니다.",Toast.LENGTH_SHORT).show();
                    editor.putString("ID", id);
                    editor.putString("PW", password);
                    editor.putString("USER_IDX", "1");
                    editor.putString("USER_NM", userName);
                    editor.putBoolean("Auto_Login_enabled", true);
                    editor.commit();
                    startActivity(new Intent(getApplication(), MainActivity.class)); // 로딩이 끝난후 이동할 Activity
                    showProgress(false);
                    finish();
                }else{
                    Toast.makeText(getApplicationContext(), "로그인에 실패하였습니다.",Toast.LENGTH_SHORT).show();
                    showProgress(false);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 3;
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

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

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

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mIdView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }
    private void hideKeyboard()
    {
        imm.hideSoftInputFromWindow(mIdView.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
    }
}

