package org.moca.task;

import java.util.List;

import org.moca.Constants;
import org.moca.R;
import org.moca.util.MocaUtil;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/** 
 * A task for importing a list of Procedures into the database.
 * 
 * @author Sana Development Team
 *
 */
public class ImportProcedureAll extends AsyncTask<Context, Void, Integer> {
	private static final String TAG = ImportProcedureAll.class.getSimpleName();
	
	private ProgressDialog progressDialog;
	private Context mContext = null; // TODO context leak?
	private List<String> locations;
	private Integer completed = 0;
	private Integer duplicates = 0;
	private Integer importError = 0;
	private String err = "";
	
	/**
	 * A new task for importing a list of procedures.
	 * 
	 * @param c the Context to import into
	 * @param locations a list of Procedure file paths
	 */
	public ImportProcedureAll(Context c, List<String> locations) {
		this.mContext = c;
		this.locations = locations;
	}

	/** {@inheritDoc} */
	@Override
	protected Integer doInBackground(Context... params) {
		Log.i(TAG, "Executing Import from SD");
		String mount = Environment.getExternalStorageState();
		if(!mount.equals(Environment.MEDIA_MOUNTED))
			return 2;
		String path = Environment.getExternalStorageDirectory() 
							+ Constants.PATH_PROCEDURE;
		Context c = params[0];
		try {
			for (String location:locations){
				switch(MocaUtil.insertProcedureFromSd(c, path + location)){
				case 0:
					completed +=1;
					break;
				case 1:
					duplicates +=1;
					break;
				case 2:
					importError +=1;
					break;			
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			err = e.getMessage();
			Log.v(TAG, err);
			importError +=1;
		}

		if (importError>0){
			return 2;
		}else if(duplicates>0){
			return 1;
		}
		return 0;

	}

	/** {@inheritDoc} */
	@Override
	protected void onPreExecute() {
		Log.i(TAG, "About to execute ImportProcedureAll");
		if (progressDialog != null) {
    		progressDialog.dismiss();
    		progressDialog = null;
    	}
    	progressDialog = new ProgressDialog(mContext);
    	progressDialog.setMessage(mContext.getString(R.string.import_proc_all));
    	progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	progressDialog.show();
	}

	/** {@inheritDoc} */
	@Override
	protected void onPostExecute(Integer result) {
		Log.i(TAG, "Completed ImportProcedure");
		if (progressDialog != null) {
    		progressDialog.dismiss();
    		progressDialog = null;
    	}
		switch (result) {
		case 0:
			Toast.makeText(mContext, "All Procedures inserted into Database.",
                   Toast.LENGTH_LONG).show();
			break;
		case 1:
			MocaUtil.errorAlert(this.mContext, "Done: "+completed
					+"\nDuplicates: "+duplicates+"\nImport Errors: "
					+importError);
			break;
		case 2:
			MocaUtil.errorAlert(this.mContext, "Done: "+completed+
					"\nDuplicates: "+duplicates+"\nImport Errors: "
					+importError);
			break;
		}
	}

}
