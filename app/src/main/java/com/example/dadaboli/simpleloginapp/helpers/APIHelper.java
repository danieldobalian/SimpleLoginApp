package com.example.dadaboli.simpleloginapp.helpers;


import android.accounts.AccountManagerCallback;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Map;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class APIHelper {

    public static Map getJson(String url,
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