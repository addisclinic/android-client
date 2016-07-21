package org.moca.task;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.moca.AddisApp;
import org.moca.events.LoginIOExceptionEvent;
import org.moca.model.LoginResult;
import org.moca.net.MDSResult;
import org.moca.util.MocaUtil;
import org.moca.util.UserSettings;

/**
 * A Task for validating authorization.
 * 
 * @author Sana Development Team
 *
 */
public class CheckCredentialsTask extends AsyncTask<Context, Void, Integer> {
	public static final String TAG = CheckCredentialsTask.class.getSimpleName();
	
	/** Indicates a connection could not be established to validate. */
	public static final int CREDENTIALS_NO_CONNECTION = 0;
	
	/** 
	 * Indicates a connection was established but that the credentials were
	 * not valid.
	 */
	public static final int CREDENTIALS_INVALID = 1;
	
	/** 
	 * Indicates a connection was established but and the credentials were 
	 * valid.
	 */
	public static final int CREDENTIALS_VALID = 2;
	
	private ValidationListener validationListener = null;
	
	/**
	 * Sets the current listener
	 * @param listener the new ValidationListener
	 */
	public void setValidationListener(ValidationListener listener) {
		this.validationListener = listener;
	}

	/** {@inheritDoc} */
	@Override
	protected Integer doInBackground(Context... params) {
		Log.i(TAG, "Executing CheckCredentialsTask");
		Context c = params[0];
		Integer result = CREDENTIALS_NO_CONNECTION;
		
		if (MocaUtil.checkConnection(c)) {

			UserSettings settings = new UserSettings();
			String username = settings.getUsername();
			String password = settings.getPassword();
			if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
				return CREDENTIALS_INVALID;
			}

			LoginResult loginResult =  AddisApp.getInstance()
											   .getNetworkClient()
											   .loginSynchronous(null, username, password);
            if (loginResult == null) {
                AddisApp.getInstance().getBus().post(new LoginIOExceptionEvent());
                return -1;
            }
			result = loginResult.status.equals(MDSResult.SUCCESS_STRING) ?
					CREDENTIALS_VALID : CREDENTIALS_INVALID;

		}
		return result;
	}
	
	/** {@inheritDoc} */
	@Override
	protected void onPostExecute(Integer result) {

		if (validationListener != null) {
			Log.i(TAG, "Completed CheckCredentialsTask");
			validationListener.onValidationComplete(result);
			// Free the reference to help prevent leaks.
			validationListener = null;
		}
    }
}
