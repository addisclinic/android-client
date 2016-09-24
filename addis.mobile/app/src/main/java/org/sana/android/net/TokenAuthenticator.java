package org.sana.android.net;

import android.util.Log;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Created by Albert on 4/2/2016.
 */
public class TokenAuthenticator implements Authenticator {
    private static final String TAG = TokenAuthenticator.class.getSimpleName();
    private static final int MAX_RETRY_COUNT = 1;
    private static final String AUTH_TOKEN_KEY = TAG + ".AUTH_TOKEN_KEY";
    private static final String USER_ID_KEY = TAG + ".USER_ID_KEY";
    private static int count = 0;
    private static String name;

    public TokenAuthenticator() {
    }

    public static void setServiceName(String serviceName) {
        name = serviceName;
    }

    public static void resetRetryCount() {
        count = 0;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        //Log.e(TAG, "Authenticate. " + CurrentSession.getCredentials().toString());

        if (hasRetriedTooManyTimes(response)) {
            Log.e(TAG, "Too many requests.");
            notifyInvalidCredentials();
            return null;
        }

        /*Credentials credentials = CurrentSession.getCredentials();
        Logger.e("Reauth with Creds. " + credentials.toString());
        if (Credentials.isValid()) {
            Call<Session> call = userService.refresh(credentials);
            Session body = call.execute().body();
            new Credentials();
        }*/

        // Add new header to rejected request and retry it
        return response.request().newBuilder()
                .header(USER_ID_KEY, "")
                .header(AUTH_TOKEN_KEY, "")
                .build();

    }

    private boolean hasRetriedTooManyTimes(Response response) {
        int result = 0;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result > MAX_RETRY_COUNT;
    }

    private void notifyInvalidCredentials() {
        //BusProvider.getInstance().post(new InvalidCredentialsEvent(name));
    }
}
