/*
Copyright (c) Microsoft
All Rights Reserved
Apache 2.0 License
 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */

package com.example.dadaboli.simpleloginapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dadaboli.simpleloginapp.helpers.APIHelper;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.example.dadaboli.simpleloginapp.helpers.Constants;
import com.example.dadaboli.simpleloginapp.helpers.InMemoryCacheStore;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {

    private AuthenticationContext mAuthContext;
    private ProgressDialog mLoginProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_todo_items);
        getActionBar().hide();
    }

    public void login(View v) {
        mLoginProgressDialog = new ProgressDialog(this);
        mLoginProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoginProgressDialog.setMessage("Login in progress...");
        mLoginProgressDialog.show();

        // Ask for token and provide callback
        try {
            mAuthContext = new AuthenticationContext(MainActivity.this, Constants.AUTHORITY_URL,
                    false, InMemoryCacheStore.getInstance());

            if(Constants.CORRELATION_ID != null &&
                    Constants.CORRELATION_ID.trim().length() !=0){
                mAuthContext.setRequestCorrelationId(UUID.fromString(Constants.CORRELATION_ID));
            }

            mAuthContext.acquireToken(MainActivity.this, Constants.PROTECTEDRESURL,
                    Constants.CLIENT_ID, Constants.REDIRECT_URL, Constants.USER_HINT,
                    "nux=1&" + Constants.EXTRA_QP,
                    new AuthenticationCallback<AuthenticationResult>() {
                        @Override
                        public void onError(Exception exc) {
                            if (mLoginProgressDialog.isShowing()) {
                                mLoginProgressDialog.dismiss();
                            }
                            SimpleAlertDialog.showAlertDialog(MainActivity.this,
                                    "Failed to get token", exc.getMessage());
                        }

                        @Override
                        public void onSuccess(AuthenticationResult result) {
                            if (mLoginProgressDialog.isShowing()) mLoginProgressDialog.dismiss();

                            if (result != null && !result.getAccessToken().isEmpty()) {
                                setLocalToken(result);
                                updateLoggedInUser();

                                TextView textView = (TextView) findViewById(R.id.userLoggedIn);
                                try {
                                    Log.v("d", "Attempting Graph API Call");

                                    Map graphResult = APIHelper.getJson(Constants.PROTECTEDRESURL+"v1.0/me/", null, result.getAccessToken());

                                    Log.v("d", "Results: " + graphResult.get("displayName").toString());

                                    textView.append("\n\n\n\n\n\nGraph API Successful.\n\nDisplay Name: " + graphResult.get("displayName").toString() + ".");
                                } catch (Exception e) {
                                    Log.v("d", "Exception Generated: " + e.toString());

                                    textView.append("\n\n\n\n\n\nGraph API Failed.");
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            SimpleAlertDialog.showAlertDialog(getApplicationContext(), "Exception caught", e.getMessage());
        }
    }

    private void updateLoggedInUser() {
        TextView textView = (TextView) findViewById(R.id.userLoggedIn);
        textView.setText("Login Failed.\n\nUser: N/A");

        if (Constants.CURRENT_RESULT != null) {
            if (Constants.CURRENT_RESULT.getIdToken() != null) {
                /* hide button and bring out textview */
                findViewById(R.id.login).setVisibility(View.INVISIBLE);
                findViewById(R.id.userLoggedIn).setVisibility(View.VISIBLE);

                Log.v("v", "Token: " + Constants.CURRENT_RESULT.getUserInfo());

                textView.setText("\n\n\n\nLogin Successful.\n\nGiven Name: " + Constants.CURRENT_RESULT.getUserInfo().getGivenName() + ".");
            } else {
                textView.setText("User with No ID Token");
            }
        }
    }
    private void setLocalToken(AuthenticationResult newToken) {
        Constants.CURRENT_RESULT = newToken;
    }

    @Override
    public void onResume() {
        super.onResume(); // Always call the superclass method first

        updateLoggedInUser();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }
}
