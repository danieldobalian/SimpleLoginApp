package com.example.dadaboli.taskapplication.helpers;


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

/**
 * An incomplete class that illustrates how to make API requests with the Access Token.
 *
 * @author Leo Nikkilä
 * @author Camilo Montes
 *
 * Modified by Daniel Dobalian to use ADAL w/ Graph endpoint
 */
public class APIHelper {

    /**
     * Makes a GET request and parses the received JSON string as a Map.
     */
    public static Map getJson(String url,
                              AccountManagerCallback<Bundle> callback, String accessToken)
            throws Exception {

        String jsonString = makeRequest(HttpRequest.METHOD_GET, url, callback, accessToken);
        Log.v("v", "Returned from graph json: " + jsonString);
        return new Gson().fromJson(jsonString, Map.class);
    }

    /**
     * Makes an arbitrary HTTP request using the provided account.
     *
     * If the request doesn't execute successfully on the first try, the tokens will be refreshed
     * and the request will be retried. If the second try fails, an exception will be raised.
     */
    public static String makeRequest(String method, String url,
                                     AccountManagerCallback<Bundle> callback, String accessToken)
            throws Exception {

        return makeRequest(method, url, true, callback, accessToken);
    }

    private static String makeRequest(String method, String url,
                                      boolean doRetry, AccountManagerCallback<Bundle> callback, String accessToken)
            throws Exception {

        /* Not proper style, but allows us to do a network operation on the main thread
         * Do not use in production applications.
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        // Prepare an API request using the accessToken
        HttpRequest request = new HttpRequest(url, method);
        request = prepareApiRequest(request, accessToken);
        Log.v("v", "Preparing Network Call: " + url);
        if (request.ok()) {
            return request.body();
        } else {
            int code = request.code();

            String requestContent = "empty body";
            try {
                requestContent = request.body();
            } catch (HttpRequest.HttpRequestException e) {
                //Nothing to do, the response has no body or couldn't fetch it
                Log.v("v", "Failed Graph API call");
                e.printStackTrace();
            }

            if (doRetry && (code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN ||
                    (code == HTTP_BAD_REQUEST && (requestContent.contains("invalid_grant") || requestContent.contains("Access Token not valid"))))) {
                // We're being denied access on the first try, let's renew the token and retry
                // accountManager.invalidateAuthTokens(account);

                return makeRequest(method, url, false, callback, accessToken);
            } else {
                // An unrecoverable error or the renewed token didn't work either
                throw new IOException(request.code() + " " + request.message() + " " + requestContent);
            }
        }
    }

    /**
     * Prepares an arbitrary API request by injecting an ID Token into an HttpRequest. Uses an
     * external library to make my life easier, but you can modify this to use whatever in case you
     * don't like the (small) dependency.
     */
    public static HttpRequest prepareApiRequest(HttpRequest request, String idToken)
            throws IOException {

        return request.authorization("Bearer " + idToken).acceptJson();
    }
}