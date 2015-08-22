package com.auriferous.atch.Activities;

import android.content.Intent;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.auriferous.atch.ActionEditText;
import com.auriferous.atch.Callbacks.FuncCallback;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends FragmentActivity {
    private GoogleMap map;
    private ActionEditText usernameView;

    private boolean signUpScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (map == null) {
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        }

        ParseUser currUser = ParseUser.getCurrentUser();
        if (accountIsAlreadyCreatedWithUsername(currUser) && ParseFacebookUtils.isLinked(currUser) && isLoggedIn())
            startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));

        Button mSignUpSwitchButton = (Button) findViewById(R.id.sign_up_switch_button);
        mSignUpSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpScreen = true;
                switchViews();
            }
        });

        Button mLogInButton = (Button) findViewById(R.id.log_in_button);
        mLogInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });


        usernameView = (ActionEditText) findViewById(R.id.username);
        usernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    attemptSignUp();
                    return true;
                }
                return false;
            }
        });

        Button mSignUpButton = (Button) findViewById(R.id.sign_up_button);
        mSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignUp();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if(signUpScreen){
            signUpScreen = false;
            switchViews();
        }
        else
            super.onBackPressed();
    }


    private void attemptLogin() {
        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, ParseAndFacebookUtils.permissions, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user != null) {
                    if (user.isNew() || !accountIsAlreadyCreatedWithUsername(user)){
                        signUpScreen = true;
                        switchViews();
                    }
                    else {
                        setupParseInstallation();
                        startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
                    }
                }
            }
        });
    }
    private void attemptSignUp() {
        usernameView.setError(null);
        View focusView = usernameView;

        final String username = usernameView.getText().toString();

        ParseUser usernameUser = getUserWithUsername(username);

        if (isBadUsername(username)) {
            focusView.requestFocus();
            return;
        }
        if (usernameUser != null) {
            //todo maybe make message for if you're the user who took the username
            usernameView.setError(getString(R.string.error_taken_username));
            focusView.requestFocus();
            return;
        }

        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, ParseAndFacebookUtils.permissions, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user != null) {
                    if (user.isNew() || !accountIsAlreadyCreatedWithUsername(user)) {
                        setupParseInstallation();
                        GraphRequest request = GraphRequest.newMeRequest(
                                AccessToken.getCurrentAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        try {
                                            ParseUser.getCurrentUser().put("fbid", object.getString("id"));
                                            ParseUser.getCurrentUser().put("fullname", object.getString("name"));
                                            ParseUser.getCurrentUser().put("usernameSet", "t");
                                            ParseUser.getCurrentUser().setUsername(username);
                                            ParseUser.getCurrentUser().saveInBackground();
                                        } catch (JSONException e) {}
                                    }
                                });
                        request.executeAsync();
                    }

                    //switch to the atch agreement activity
                    startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
                }
            }
        });
    }

    private boolean isBadUsername(String username){
        if (TextUtils.isEmpty(username)) {
            usernameView.setError(getString(R.string.error_username_required));
            return true;
        }
        return false;
    }
    //todo blocking
    private ParseUser getUserWithUsername(String username) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", username);
        try {
            return query.getFirst();
        } catch (ParseException e) {}
        return null;
    }
    private boolean accountIsAlreadyCreatedWithUsername(ParseUser user){
        return (user != null && user.get("usernameSet") != null && user.get("usernameSet").equals("t"));
    }

    private boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        //todo reexamine
        return (accessToken != null && !accessToken.isExpired()/* && accessToken.getPermissions().size() == ParseAndFacebookUtils.permissions.size()*/);
    }

    private void setupParseInstallation() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("userId", ParseUser.getCurrentUser().getObjectId());
        installation.saveInBackground();
        ParsePush.subscribeInBackground("global");
    }


    private void switchViews() {
        RelativeLayout oldLayout = (RelativeLayout) findViewById(signUpScreen?R.id.buttons_layout:R.id.sign_up_layout);
        oldLayout.setVisibility(View.GONE);
        RelativeLayout newLayout = (RelativeLayout) findViewById(signUpScreen?R.id.sign_up_layout:R.id.buttons_layout);
        newLayout.setVisibility(View.VISIBLE);
    }
}