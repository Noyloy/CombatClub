package com.bat.club.combatclub;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    // UI references.
    private EditText mUserView;
    private EditText mPasswordView;
    private CheckBox mSaveMeView;
    private ProgressDialog mProgressView;
    // case of remember me
    SharedPreferences mPrefs;
    final static String PREFS_NAME = "COMBAT_CLUB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up shared prefs
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        //if user is saved previously
        // reading username, password and id from SharedPreferences
        String tmpUsername = mPrefs.getString("Username", null);
        String tmpPassword = mPrefs.getString("Password", null);
        String tmpUserID = mPrefs.getString("UserID", null);

        // user was saved - start activity
        if (tmpPassword!=null && tmpUserID!=null && tmpUsername!=null){
            Intent intent = new Intent(getApplicationContext(),GameSelection.class);
            intent.putExtra("Username",tmpUsername);
            intent.putExtra("Password",tmpPassword);
            intent.putExtra("UserID",tmpUserID);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        // Set up real activity name
        setTitle(getString(R.string.title_activity_login));

        // Set up progress
        mProgressView = new ProgressDialog(LoginActivity.this);
        mProgressView.setMessage(getString(R.string.wait));

        // Set up the login form.
        mSaveMeView = (CheckBox) findViewById(R.id.save_me_cb);
        mUserView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mUserView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String username = mUserView.getText().toString();
        final String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(username)){
            mUserView.setError(getString(R.string.empty));
            focusView = mUserView;
            cancel = true;
        }

        if(TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.empty));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {

            mProgressView.show();

            RequestParams params = new RequestParams();
            params.put("name", username);
            params.put("pass", password);
            CombatClubRestClient.post("/PassConfirm", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String rawXml = new String(responseBody, "UTF-8");
                        String res = CombatClubRestClient.interprateResponse(rawXml);

                        // connection successful - result is the id
                        if (!res.equals("-1")) {
                            // need to save user
                            if (mSaveMeView.isChecked()) {
                                SharedPreferences.Editor edit = mPrefs.edit();
                                edit.putString("Username", username);
                                edit.putString("Password", password);
                                edit.putString("UserID", res);
                                edit.apply();
                            }

                            Intent intent = new Intent(getApplicationContext(), GameSelection.class);
                            intent.putExtra("Username", username);
                            intent.putExtra("Password", password);
                            intent.putExtra("UserID", res);
                            startActivity(intent);
                            finish();
                        }
                        // connection unsuccessful
                        else {
                            mUserView.setError(getString(R.string.error_incorrect_password));
                            mPasswordView.setError(getString(R.string.error_incorrect_password));
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mProgressView.hide();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    mProgressView.hide();
                    Toast.makeText(LoginActivity.this, getString(R.string.error) + " " + statusCode, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}

