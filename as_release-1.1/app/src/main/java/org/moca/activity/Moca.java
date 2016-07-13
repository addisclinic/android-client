package org.moca.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.moca.AddisApp;
import org.moca.Constants;
import org.moca.R;
import org.moca.activity.settings.Settings;
import org.moca.db.MocaDB.NotificationSQLFormat;
import org.moca.db.MocaDB.ProcedureSQLFormat;
import org.moca.db.MocaDB.SavedProcedureSQLFormat;
import org.moca.events.LoginFailedEvent;
import org.moca.fragments.BaseDialog;
import org.moca.fragments.LoginFragment;
import org.moca.media.EducationResource;
import org.moca.notification.StoreNotifications;
import org.moca.procedure.Procedure;
import org.moca.service.BackgroundUploader;
import org.moca.service.ServiceConnector;
import org.moca.service.ServiceListener;
import org.moca.task.CheckCredentialsTask;
import org.moca.task.MDSSyncTask;
import org.moca.task.ResetDatabaseTask;
import org.moca.task.ValidationListener;
import org.moca.util.MocaUtil;
import org.moca.util.UserSettings;

/**
 * Main Sana activity. When Sana is launched, this activity runs, allowing the 
 * user to either run a procedure, view notifications, or view pending 
 * transfers.
 * 
 * @author Sana Dev Team
 */
public class Moca extends AppCompatActivity implements View.OnClickListener, LoginFragment.LoginFragmentListener {
    public static final String TAG = Moca.class.getSimpleName();

    // Option menu codes
    private static final int OPTION_RELOAD_DATABASE = 0;
    private static final int OPTION_SETTINGS = 1;
	private static final int OPTION_SYNC = 2;
    private static final int OPTION_ABOUT = 3;

    // Activity request codes
	/** Intent request code for picking a procedure */
    public static final int PICK_PROCEDURE = 0;
    
    /** Intent request code for picking a saved procedure */
    public static final int PICK_SAVEDPROCEDURE = 1;
    
    /** Intent request code for picking a notification */
    public static final int PICK_NOTIFICATION = 2;
    
    /** Intent request code to start running a procedure */
    public static final int RUN_PROCEDURE = 3;
    
    /** Intent request code to resume running a saved procedure*/
    public static final int RESUME_PROCEDURE = 4;
    
    /** INtent request code to view settings */
    public static final int SETTINGS = 6;
    
    //Alert dialog codes
    private static final int DIALOG_INCORRECT_PASSWORD = 0;
	private static final int DIALOG_NO_CONNECTIVITY = 1;
	private static final int DIALOG_NO_PHONE_NAME = 2;
    
    private ServiceConnector mConnector = new ServiceConnector();
    private BackgroundUploader mUploadService = null;
    private CheckCredentialsTask mCredentialsTask;
    private ResetDatabaseTask mResetDatabaseTask;
    private MDSSyncTask mSyncTask;
    private boolean appInForeground = false;
    
    // State 
    private Bundle mSavedState;
    static final String STATE_CHECK_CREDENTIALS = "_credentials";
    static final String STATE_MDS_SYNC = "_mdssync";
    static final String STATE_RESET_DB = "_resetdb";
    /**
     * Background listener for taking action when network service is available
     * 
     * @author Sana Development Team
     *
     */
    private class BackgroundUploaderConnectionListener implements 
    	ServiceListener<BackgroundUploader> 
    {
		public void onConnect(BackgroundUploader uploadService) {
			Log.i(TAG, "onServiceConnected");
			mUploadService = uploadService;
		}
		
		public void onDisconnect(BackgroundUploader uploadService) {
			Log.i(TAG, "onServiceDisconnected");
			mUploadService = null;
		}
    }
    
    /**
     * Background listener to signal that credentials have been validated with
     * the permanent data store.
     * 
     * @author Sana Development Team
     */
    private ValidationListener credentialsListener = new ValidationListener()  {
    	/**
         * Called when CheckCredentialsTask completes
         */
        @Override
    	public void onValidationComplete(Integer validationResult) {
            Log.w(TAG, "result : " + validationResult);
    		switch(validationResult){
                case(CheckCredentialsTask.CREDENTIALS_NO_CONNECTION):
                    Log.i(TAG, "Cannot validate EMR credentials -"+
                        "no network connectivity!");
                    if(!isFinishing()) {
                        BaseDialog dialog = BaseDialog.getInstance(R.string.general_error, R.string.msg_no_connection);
                        dialog.show();
                    }
                    break;
                case(CheckCredentialsTask.CREDENTIALS_INVALID):
                    Log.i(TAG, "Could not validate EMR username/password");
                    if (mUploadService != null) {
                        mUploadService.onCredentialsChanged(false);
                    }
                    break;
                case(CheckCredentialsTask.CREDENTIALS_VALID):
                    Log.i(TAG, "Username/Password for EMR correct");
                    if (mUploadService != null) {
                        mUploadService.onCredentialsChanged(true);
                    }
                    break;
            }
    	}
    };

    /** {@inheritDoc} */
	@Override
	public void onDestroy() {
		super.onDestroy();
        AddisApp.getInstance().getBus().unregister(this);
		try {
			mConnector.disconnect(this);
			mUploadService = null;
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "While disconnecting service got exception: " 
					+ e.getMessage());
			e.printStackTrace();
		}
	}

	/** {@inheritDoc} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        inflateViews();
        AddisApp.getInstance().getBus().register(this);
        // Create a connection to the background upload service. 
        // This starts the service when the app starts.
        try {
        	mConnector.setServiceListener(
        			new BackgroundUploaderConnectionListener());
        	mConnector.connect(this);
        }
        catch (Exception e) {
        	Log.e(TAG, "Exception starting background upload service: " 
        			+ e.getMessage());
        	e.printStackTrace();
        }
        // Make sure directory structure is in place on external drive
        EducationResource.intializeDevice();
        Procedure.intializeDevice();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSavedState != null) restoreLocalTaskState(mSavedState);
        appInForeground = true;
        showLoginDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appInForeground = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void inflateViews() {
        View openProcedure = findViewById(R.id.moca_main_procedure);
        openProcedure.setOnClickListener(this);

        View viewTransfers = findViewById(R.id.moca_main_transfers);
        viewTransfers.setOnClickListener(this);

        View viewNotifications = findViewById(R.id.moca_main_notifications);
        viewNotifications.setOnClickListener(this);
    }

    /** Activates selecting a procedure and to start a new encounter */
    private void pickProcedure() {
    	Intent i = new Intent(Intent.ACTION_PICK);
        i.setType(ProcedureSQLFormat.CONTENT_TYPE);
        i.setData(ProcedureSQLFormat.CONTENT_URI);
        startActivityForResult(i, PICK_PROCEDURE);
    }
    
    /** Starts Activity for selecting and then viewing a previous encounter */
    private void pickSavedProcedure() {
    	Intent i = new Intent(Intent.ACTION_PICK);
    	i.setType(SavedProcedureSQLFormat.CONTENT_TYPE);
    	i.setData(SavedProcedureSQLFormat.CONTENT_URI);
    	startActivityForResult(i, PICK_SAVEDPROCEDURE);
    }

    /** Starts Activity for selecting and then viewing notifications */
    private void pickNotification() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType(NotificationSQLFormat.CONTENT_TYPE);
        i.setData(NotificationSQLFormat.CONTENT_URI);
        startActivityForResult(i, PICK_NOTIFICATION);
    }
    
	/** {@inheritDoc} */
    @Override
    public void onClick(View arg0) {
		Log.d(TAG, "Button: " + arg0.getId());
		switch (arg0.getId()) {
		// buttons on the main screen
		case R.id.moca_main_procedure:
			pickProcedure();
			break;
		case R.id.moca_main_transfers:
			pickSavedProcedure();
			break;
		case R.id.moca_main_notifications:
            requestNotifications();
			pickNotification();
			break;
		}
	}

    private void requestNotifications() {
        String patientId = new UserSettings().getPatientId();
        AddisApp.getInstance().getNetworkClient().requestNotifications(patientId, new StoreNotifications());
    }
    /** {@inheritDoc} */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
    		Intent data) 
    {
    	MocaUtil.logActivityResult(TAG, requestCode, resultCode);
        switch (resultCode) {
        case RESULT_CANCELED:
        	if(requestCode == RUN_PROCEDURE) {
        		pickProcedure();
        	} else if(requestCode == RESUME_PROCEDURE) {
        		pickSavedProcedure();
        	} else if(requestCode == SETTINGS) {
        		//Check to make sure there is a phone number entered, 
        		// otherwise will not connect to MDS
        		String phoneNum = PreferenceManager
        							.getDefaultSharedPreferences(this)
        							.getString(Constants.PREFERENCE_PHONE_NAME, 
        										null);
        		Log.d(TAG, "phoneNum from preferences is: " + phoneNum);
        		if (phoneNum == null || phoneNum.equals("")) {
        			Log.d(TAG, "No phone number entered - showing dialog now");
        			if(!isFinishing())
        				showDialog(DIALOG_NO_PHONE_NAME);
        		}
        		// Attempt to validate the credentials changed in the settings.
        		if(mCredentialsTask == null ||
        			(mCredentialsTask != null && mCredentialsTask.getStatus() == Status.FINISHED))
        		{
        				mCredentialsTask = new CheckCredentialsTask();
        				mCredentialsTask.setValidationListener(credentialsListener);
        				mCredentialsTask.execute(this);
        		}
        	}
            break;
        case RESULT_OK:
        	Uri uri = null;
        	if(data != null) {
        		uri = data.getData();
        	}
        	if(requestCode == PICK_PROCEDURE) {
        		assert(uri != null);
        		doPerformProcedure(uri);
        	} else if(requestCode == PICK_SAVEDPROCEDURE) {
        		assert(uri != null);
        		doResumeProcedure(uri);
        	} else if(requestCode == PICK_NOTIFICATION) {
        		assert(uri != null);
        		doShowNotification(uri);
        	} else if (requestCode == RUN_PROCEDURE || 
        				requestCode == RESUME_PROCEDURE) {
        		pickSavedProcedure();
        	}
            break;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_INCORRECT_PASSWORD:
        	return new AlertDialog.Builder(this)
        	.setTitle("Error!")
            .setMessage(getString(R.string.dialog_incorrect_credentials))
            .setPositiveButton(getString(R.string.general_change_settings), 
            		new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	// Dismiss dialog and return to settings                	
                	Intent i = new Intent(Intent.ACTION_PICK);
                    i.setClass(Moca.this, Settings.class);
                    startActivityForResult(i, SETTINGS);
                	setResult(RESULT_OK, null);
                	dialog.dismiss();
                }
            })
            .setCancelable(true)
            .setNegativeButton(getString(R.string.general_cancel), 
            		new OnClickListener() 
            {
				public void onClick(DialogInterface dialog, int whichButton) {
					setResult(RESULT_CANCELED, null);
                	dialog.dismiss();
				}
            })
            .create();
        case DIALOG_NO_CONNECTIVITY:
        	return new AlertDialog.Builder(this)
        	.setTitle(getString(R.string.general_error))
            .setMessage(getString(R.string.dialog_no_network))
            .setPositiveButton(getString(R.string.general_ok), 
            		new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int whichButton) {
                	// Dismiss dialog and return to settings
                	setResult(RESULT_OK, null);
                	dialog.dismiss();
                }
            })
            .create();
        case DIALOG_NO_PHONE_NAME:
        	return new AlertDialog.Builder(this)
        	.setTitle(getString(R.string.general_error))
            .setMessage(getString(R.string.dialog_no_phone_name))
            .setPositiveButton(getString(R.string.general_change_settings), 
            		new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int whichButton) {
                	// Dismiss dialog and return to settings                	
                	Intent i = new Intent(Intent.ACTION_PICK);
                    i.setClass(Moca.this, Settings.class);
                    startActivityForResult(i, SETTINGS);
                	setResult(RESULT_OK, null);
                	dialog.dismiss();
                }
            })
            .setCancelable(true)
            .setNegativeButton(getString(R.string.general_cancel), 
            		new OnClickListener() 
            {
				public void onClick(DialogInterface dialog, int whichButton) {
					setResult(RESULT_CANCELED, null);
                	dialog.dismiss();
				}
            })
            .create();
        }
        return null;
    }

    /**
     * Starts Activity for viewing a Notification.
     * 
     * @param uri The notification to view.
     */
    private void doShowNotification(Uri uri) {
    	try {
    		Intent i = new Intent(Intent.ACTION_VIEW, uri);
    		startActivity(i);
    	} catch(Exception e) {
    		Log.e(TAG, "While showing notification " + uri 
    				+ " an exception occured: " + e.toString());
    	}
    }
    
    /**
     * Starts Activity for resuming a saved procedure
     * 
     * @param uri The saved procedure to restart
     */
    private void doResumeProcedure(Uri uri) {
    	try {
    		Intent i = new Intent(Intent.ACTION_VIEW, uri);
    		i.putExtra("savedProcedureUri", uri.toString());
    		startActivityForResult(i, RESUME_PROCEDURE);
    	} catch(Exception e) {
    		Log.e(TAG, "While resuming procedure " 
    				+ uri + " an exception occured: " + e.toString());
    	}
    }
    
    /**
     * Starts an Activity for running a new Procedure
     * 
     * @param uri The Procedure to run
     */
    private void doPerformProcedure(final Uri uri) {
        Log.i(TAG, "doPerformProcedure uri=" + uri.toString());
        try {
        	Intent i = new Intent(Intent.ACTION_VIEW, uri);
    		startActivityForResult(i, RUN_PROCEDURE);
        } catch (Exception e) {
            MocaUtil.errorAlert(this, e.toString());
            Log.e(TAG, "While running procedure " + uri 
            		+ " an exception occured: " + e.toString());
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        menu.add(0, OPTION_RELOAD_DATABASE, 0,
        		getString(R.string.menu_reload_db));
        menu.add(0, OPTION_SETTINGS, 1, getString(R.string.menu_settings));
		menu.add(0, OPTION_SYNC, 2, getString(R.string.menu_sync));
        menu.add(0, OPTION_ABOUT, 3, "About");
        return true;
    }
    
    /** Executes a task to clear out the database */
    private void doClearDatabase() {
    	// TODO: context leak
    	if(mResetDatabaseTask == null || 
    			(mResetDatabaseTask == null && mResetDatabaseTask.getStatus() == Status.FINISHED))
    		{
    			mResetDatabaseTask = 
    					(ResetDatabaseTask) new ResetDatabaseTask(this).execute(this);
    		}
    }
    
    /** Syncs the Patient database with MDS */
    private void doUpdatePatientDatabase() {
    	if(mSyncTask == null || 
    			(mSyncTask != null && mSyncTask.getStatus() == Status.FINISHED))
    	{
    		mSyncTask = (MDSSyncTask) new MDSSyncTask(this).execute(this);
    	}
    }

    private  AlertDialog dialog;
    /** {@inheritDoc} */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case OPTION_RELOAD_DATABASE:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setPositiveButton("Yes", new OnClickListener() {
                    public void onClick(DialogInterface i, int v) {
                        doClearDatabase();
                    }
                });

                builder.setMessage(getString(R.string.dialog_no_reload_db_warn));
                builder.setCancelable(true);
                builder.setNegativeButton(getString(R.string.general_no), null);

                dialog = builder.create();
                if(!isFinishing())
                    dialog.show();
                return true;
            case OPTION_SETTINGS:
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setClass(this, Settings.class);
                startActivityForResult(i, SETTINGS);
                return true;
            case OPTION_SYNC:
                doUpdatePatientDatabase();
                return true;
            case OPTION_ABOUT:
                AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
                aboutBuilder.setPositiveButton("OK", null);
                PackageInfo pInfo = null;
                try {
                    pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                String version = pInfo.versionName;
                int versionCode = pInfo.versionCode;
                String message = String.format("Addis Clinic App: %s(%d)", version, versionCode);
                aboutBuilder.setMessage(message);
                aboutBuilder.setCancelable(true);
                dialog = aboutBuilder.create();
                if(!isFinishing())
                    dialog.show();
                return true;
        }
        return false;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveLocalTaskState(outState);
        mSavedState = outState;
    }
    
    private void saveLocalTaskState(Bundle outState){
    	final CheckCredentialsTask task = mCredentialsTask;
        if (task != null && task.getStatus() != Status.FINISHED) {
        	task.cancel(true);
        	outState.putBoolean(STATE_CHECK_CREDENTIALS, true);
        }
    	final MDSSyncTask mTask = mSyncTask;
        if (mTask != null && mTask.getStatus() != Status.FINISHED) {
        	mTask.cancel(true);
        	outState.putBoolean(STATE_MDS_SYNC, true);
        }
    	final ResetDatabaseTask rTask = mResetDatabaseTask;
        if (rTask != null && rTask.getStatus() != Status.FINISHED) {
        	rTask.cancel(true);
        	outState.putBoolean(STATE_RESET_DB, true);
        }
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalTaskState(savedInstanceState);
        mSavedState = null;
    }
    
    /** Restores any tasks running on this thread */
    private void restoreLocalTaskState(Bundle savedInstanceState){
    	if (savedInstanceState.getBoolean(STATE_CHECK_CREDENTIALS)){
			final CheckCredentialsTask task = new CheckCredentialsTask();
			task.setValidationListener(credentialsListener);
			mCredentialsTask = (CheckCredentialsTask) task.execute(this);
    	}
    	if (savedInstanceState.getBoolean(STATE_MDS_SYNC))
    		mSyncTask = (MDSSyncTask) new MDSSyncTask(this).execute(this);
    	if (savedInstanceState.getBoolean(STATE_RESET_DB))
    		mResetDatabaseTask = 
    			(ResetDatabaseTask) new ResetDatabaseTask(this).execute(this);
    }
    

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
    private boolean shouldShowLoginDialog = false;

    private void showLoginDialog() {
        if (shouldShowLoginDialog) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LoginFragment login = LoginFragment.getInstance(1);
                    login.show(getFragmentManager(), LoginFragment.class.getSimpleName());
                }
            });
            shouldShowLoginDialog = false;
        }
    }
    @Subscribe
    public void onFailedLogin(LoginFailedEvent event) {
        shouldShowLoginDialog = true;
        if (appInForeground) {
            showLoginDialog();
        }
    }
}
