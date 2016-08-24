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

/* Android Imports */
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

/* AAD Imports */
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.example.dadaboli.simpleloginapp.helpers.Constants;
//import com.example.dadaboli.simpleloginapp.helpers.InMemoryCacheStore;

/* Http imports */
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import com.microsoft.aad.adal.PromptBehavior;
import android.os.StrictMode;
import java.io.IOException;
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
        mAuthContext = new AuthenticationContext(MainActivity.this, Constants.AUTHORITY_URL,
                false);

        if(Constants.CORRELATION_ID != null &&
                Constants.CORRELATION_ID.trim().length() != 0){
            mAuthContext.setRequestCorrelationId(UUID.fromString(Constants.CORRELATION_ID));
        }

        mAuthContext.acquireToken(MainActivity.this, Constants.PROTECTEDRESURL,
                Constants.CLIENT_ID, Constants.REDIRECT_URL, PromptBehavior.Always,
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

                                Map graphResult = getJson(Constants.PROTECTEDRESURL+"v1.0/me/", null, result.getAccessToken());

                                Log.v("d", "Results: " + graphResult.get("displayName").toString());

                                textView.append("\n\n\n\n\n\nGraph API Successful.\n\nDisplay Name: " + graphResult.get("displayName").toString() + ".");
                            } catch (Exception e) {
                                Log.v("d", "Exception Generated: " + e.toString());

                                textView.append("\n\n\n\n\n\nGraph API Failed.");
                            }
                        }
                    }
                });
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

    private static Map getJson(String url,
                              AccountManagerCallback<Bundle> callback, String accessToken)
            throws Exception {
        return new Gson().fromJson(makeRequest(HttpRequest.METHOD_GET, url, true, accessToken), Map.class);
    }

    private static String makeRequest(String method, String url,
                                      boolean retry, String accessToken)
            throws Exception {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Prepare a request with accessToken
        HttpRequest request = new HttpRequest(url, method);
        request = request.authorization("Bearer " + accessToken).acceptJson();

        if (request.ok()) {
            return request.body();
        } else {
            String requestContent = "";
            try {
                requestContent = request.body();
            } catch (HttpRequest.HttpRequestException e) {
                Log.v("V", "Exception: " + e.toString());
            }

            int code = request.code();
            if (retry && (code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN ||
                    (code == HTTP_BAD_REQUEST && (requestContent.contains("invalid_grant") || requestContent.contains("Access Token not valid"))))) {
                return makeRequest(method, url, false, accessToken);
            } else {
                throw new IOException(request.code() + " " + request.message() + " " + requestContent);
            }
        }
    }
}
