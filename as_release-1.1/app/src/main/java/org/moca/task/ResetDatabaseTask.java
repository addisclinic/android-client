package org.moca.task;

import org.moca.R;
import org.moca.util.MocaUtil;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Task for resetting the application database.
 * 
 * @author Sana Development Team
 *
 */
public class ResetDatabaseTask extends AsyncTask<Context, Void, Integer> {
	private static final String TAG = ResetDatabaseTask.class.getSimpleName();
	
	private ProgressDialog progressDialog;
	private Context mContext = null; // TODO context leak?
	
	/**
	 * A new task for resetting the database.
	 * @param c the current Context.
	 */
	public ResetDatabaseTask(Context c) {
		this.mContext = c;
	}

	/** {@inheritDoc} */
	@Override
	protected Integer doInBackground(Context... params) {
		Log.i(TAG, "Executing ResetDatabaseTask");
		Context c = params[0];
		try{
			MocaUtil.clearDatabase(c);
			MocaUtil.loadDefaultDatabase(c);	
		} catch(Exception e){
			Log.e(TAG, "Could not sync. " + e.toString());
		}	
		return 0;
	}

	/** {@inheritDoc} */	
	@Override
	protected void onPreExecute() {
		Log.i(TAG, "About to execute ResetDatabaseTask");
		if (progressDialog != null) {
    		progressDialog.dismiss();
    		progressDialog = null;
    	}
    	progressDialog = new ProgressDialog(mContext);
    	progressDialog.setMessage("Clearing Database"); // TODO i18n
    	progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	progressDialog.show();
	}

	/** {@inheritDoc} */
	@Override
	protected void onPostExecute(Integer result) {
		Log.i(TAG, "Completed ResetDatabaseTask");
		if (progressDialog != null) {
    		progressDialog.dismiss();
    		progressDialog = null;
    	}
	}

}
