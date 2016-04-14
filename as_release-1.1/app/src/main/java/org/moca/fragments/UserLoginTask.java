package org.moca.fragments;

/**
 * Created by Albert on 4/12/2016.
 */

import android.os.AsyncTask;
import android.os.Build;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

    private final String mEmail;
    private final String mPassword;
    public interface UserLoginListener {
        public void onComplete(boolean success);
        public void onCancelled();
    }
    private UserLoginListener listener;

    public UserLoginTask(String email, String password, UserLoginListener listener) {
        mEmail = email;
        mPassword = password;
        this.listener = listener;
    }

    public void start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void)null);
        } else {
            this.execute((Void)null);
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        // TODO: attempt authentication against a network service.

        try {
            // Simulate network access.
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return false;
        }

        /*for (String credential : DUMMY_CREDENTIALS) {
            String[] pieces = credential.split(":");
            if (pieces[0].equals(mEmail)) {
                // Account exists, return true if the password matches.
                return pieces[1].equals(mPassword);
            }
        }*/

        // TODO: register the new account here.
        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (listener != null) {
            listener.onComplete(success);
        }

    }

    @Override
    protected void onCancelled() {
        if (listener != null) {
            listener.onCancelled();
        }

    }
}
