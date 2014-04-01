package org.moca.activity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.moca.Constants;
import org.moca.R;
import org.moca.db.BinaryDAO;
import org.moca.db.EncounterDAO;
import org.moca.db.EventDAO;
import org.moca.db.PatientInfo;
import org.moca.db.ProcedureDAO;
import org.moca.db.MocaDB.BinarySQLFormat;
import org.moca.db.MocaDB.ProcedureSQLFormat;
import org.moca.db.MocaDB.SavedProcedureSQLFormat;
import org.moca.db.MocaDB.EventSQLFormat.EventType;
import org.moca.media.EducationResource.Audience;
import org.moca.net.MDSInterface;
import org.moca.procedure.PatientIdElement;
import org.moca.procedure.PictureElement;
import org.moca.procedure.Procedure;
import org.moca.procedure.ProcedureElement;
import org.moca.procedure.ProcedurePage;
import org.moca.procedure.ProcedureParseException;
import org.moca.procedure.ValidationError;
import org.moca.service.BackgroundUploader;
import org.moca.service.PluginService;
import org.moca.service.ServiceConnector;
import org.moca.service.ServiceListener;
import org.moca.task.ImageProcessingTask;
import org.moca.task.ImageProcessingTaskRequest;
import org.moca.task.PatientLookupListener;
import org.moca.task.PatientLookupTask;
import org.moca.util.MocaUtil;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.AsyncTask.Status;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

/**
 * Activity which loops through the available steps within a procedure including
 * handling any branching logic. Individual procedure steps are rendered to a 
 * view which is wrapped in a container which presents buttons for paging.
 * 
 * Additional logic is built into this class to handle launching and capturing 
 * returned values from Activities used to capture data along with initiating 
 * procedure saving, reloading, and uploading.
 * 
 * @author Sana Development Team
 *
 */
public class ProcedureRunner extends Activity implements View.OnClickListener, 
	ServiceListener<BackgroundUploader>, PatientLookupListener 
{
	public static final String TAG = ProcedureRunner.class.getSimpleName();
	public static final String INTENT_KEY_STRING = "intentKey";
	public static final String INTENT_EXTRAS_KEY = "extras";
	public static final String PLUGIN_INTENT_KEY = "pluginIntent";
	
	// Intent
	public static final int CAMERA_INTENT_REQUEST_CODE = 1;
	public static final int BARCODE_INTENT_REQUEST_CODE = 2;
    public static final int INFO_INTENT_REQUEST_CODE = 7;
    public static final int PLUGIN_INTENT_REQUEST_CODE = 4;
    public static final int IMPLICIT_PLUGIN_INTENT_REQUEST_CODE = 8;

    // Options
	public static final int OPTION_SAVE_EXIT = 0;
	public static final int OPTION_DISCARD_EXIT = 1;
	public static final int OPTION_VIEW_PAGES = 2;
	public static final int OPTION_HELP = 3;

    
	// Dialog
	private static final int DIALOG_ALREADY_UPLOADED = 7;
	private static final int DIALOG_LOOKUP_PROGRESS = 1;
	private static final int DIALOG_LOAD_PROGRESS = 2;
	private ProgressDialog lookupProgress = null;
	private ProgressDialog loadProgressDialog = null;

	// State key strings
	private static final String STATE_PROCEDURE = "_procedure";
	private static final String STATE_SP_URI = "savedProcedureUri";
	private static final String STATE_ENCOUNTER = "_encounter";
	private static final String STATE_CURRENT_PAGE = "currentPage";
	private static final String STATE_ON_DONE_PAGE = "onDonePage";
	private static final String STATE_PATIENT = "patientInfo";
	private static final String STATE_LOAD_PROCEDURE = "_loading_procedure";
	private static final String STATE_PROC_LOAD_BUNDLE = "_proc_load_bundle";
	private static final String STATE_PROC_LOAD_INTENT = "_proc_load_intent";
	private static final String STATE_LOOKUP_PATIENT = "_lookup_patient";
	private static final String STATE_PATIENT_ID = "_patientId";
	
	// State instance fields
	private Procedure p = null;
	private Uri thisSavedProcedure;
	private Intent mEncounterState = new Intent();
	private boolean wasOnDonePage = false;
	private int startPage = 0;
	private boolean onDonePage = false;
	private static String[] params;
	private Bundle mSavedState = null;
	private ProcedureLoaderTask procedureLoaderTask = null;
	private PatientLookupTask patientLookupTask= null;
	
	
	// Views
	private Button next, prev, info;
	private ViewAnimator baseViews;
	
	// Service
	private ServiceConnector mConnector = new ServiceConnector();
    private BackgroundUploader mUploadService = null;
    
    private void logException(Throwable t) {
    	String stackTrace = EventDAO.getStackTrace(t);
    	EventType et = EventType.EXCEPTION;
    	if (t instanceof OutOfMemoryError) {
    		et = EventType.OUT_OF_MEMORY;
    	}
    	logEvent(et, stackTrace);
    }
    
    private void logEvent(EventType type, String value) {
    	String savedProcedureGuid = "";
    	String patientId = "";
    	String userId = "";
    	
    	if (thisSavedProcedure != null) {
    		savedProcedureGuid = EncounterDAO.getEncounterGuid(this, 
    				thisSavedProcedure);
    	}
    	
    	if (p != null) {
    		PatientInfo pi = p.getPatientInfo();
    		if (pi != null) {
    			patientId = pi.getPatientIdentifier();
    		} else {
    			// TODO find the patient ID in the form and look at its answer
    		}
    	}
    	// TODO lookup current user
    	EventDAO.registerEvent(this, type, value, savedProcedureGuid, patientId, 
    			userId);
    }

    /**
     * For connecting to the BackgroundUploader.
     */
	public void onConnect(BackgroundUploader uploadService) {
		Log.i(TAG, "onServiceConnected");
		mUploadService = uploadService;
	}

    /**
     * For disconnecting from the BackgroundUploader.
     */
	public void onDisconnect(BackgroundUploader uploadService) {
		Log.i(TAG, "onServiceDisconnected");
		mUploadService = null;
	}
	
	/**
	 * Callback to handle when a patient look up fails. Will result in an alert 
	 * being displayed prompting the user to input whether the patient should be
	 * considered new.
	 */
	public void onPatientLookupFailure(final String patientIdentifier) {
		logEvent(EventType.ENCOUNTER_LOOKUP_PATIENT_FAILED, patientIdentifier);
		
		Log.e(TAG, "Couldn't lookup patient. They might exist, but we don't "
				+"have their details.");
		if (lookupProgress != null) {
			lookupProgress.dismiss();
			lookupProgress = null;
		}
		
		StringBuilder message = new StringBuilder();
		message.append("Could not find patient record for ");
		message.append(patientIdentifier);
		message.append(". Entering a new patient. Continue?");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setCancelable(false).setPositiveButton(
				getResources().getString(R.string.general_yes),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						PatientInfo pi = new PatientInfo();
						pi.setPatientIdentifier(patientIdentifier);
						p.setPatientInfo(pi);
						nextPage();
					}
				}).setNegativeButton(
				getResources().getString(R.string.general_no),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		if(!isFinishing())
			alert.show();
	}
	
	/**
	 * Callback to handle when a patient look up succeeds. Will result in an 
	 * alert being displayed prompting the user to confirm that it is the 
	 * correct patient.
	 */
	public void onPatientLookupSuccess(final PatientInfo patientInfo) {
		logEvent(EventType.ENCOUNTER_LOOKUP_PATIENT_SUCCESS, 
				patientInfo.getPatientIdentifier());
		if (lookupProgress != null) {
			lookupProgress.dismiss();
			lookupProgress = null;
		}
		
		StringBuilder message = new StringBuilder();
		message.append("Found patient record for ID ");
		message.append(patientInfo.getPatientIdentifier());
		message.append("\n");
		
		message.append("First Name: ");
		message.append(patientInfo.getPatientFirstName());
		message.append("\n");
		
		message.append("Last Name: ");
		message.append(patientInfo.getPatientLastName());
		message.append("\n");
		
		message.append("Gender: ");
		message.append(patientInfo.getPatientGender());
		message.append("\n");
		
		message.append("Is this the correct patient?");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setCancelable(false).setPositiveButton(
				getResources().getString(R.string.general_yes),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						p.setPatientInfo(patientInfo);
						nextPage();
					}
				}).setNegativeButton(
				getResources().getString(R.string.general_no),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						p.current().getPatientIdElement()
							.setAndRefreshAnswer("");
					}
				});
		AlertDialog alert = builder.create();
		if(!isFinishing())
			alert.show();
	}
	
	// starts a new patient look up task
	private void lookupPatient(String patientId) {
		logEvent(EventType.ENCOUNTER_LOOKUP_PATIENT_START, patientId);
		if (lookupProgress == null) {
			lookupProgress = new ProgressDialog(this);
			lookupProgress.setMessage("Looking up patient \""+patientId +"\""); // TODO i18n
	    	lookupProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    	if(!isFinishing())
	    		lookupProgress.show();
	    		
			if(patientLookupTask == null || 
					patientLookupTask.getStatus() == Status.FINISHED){
				patientLookupTask = new PatientLookupTask(this);
				patientLookupTask.setPatientLookupListener(this);
				patientLookupTask.execute(patientId);
			}
		}
	}
	
	// page forward
	private synchronized boolean nextPage() {
		boolean succeed = true;
		try {
			p.current().validate();
		} catch (ValidationError e) {
			String message = e.getMessage();
			logEvent(EventType.ENCOUNTER_PAGE_VALIDATION_FAILED, message);
			MocaUtil.createAlertMessage(this, message);
			return false;
		}
		
		ProcedureElement patientId = p.current().getElementByType("patientId");
		if (patientId != null && p.getPatientInfo() == null) {
			// The patient ID question is on this page. Look up the ID in an 
			// AsyncTask
			lookupPatient(patientId.getAnswer());
			return false;
		}

		// Save answers 
		storeCurrentProcedure(false);
		if(!p.hasNextShowable()) {
			if(!onDonePage) {
				baseViews.setInAnimation(this,R.anim.slide_from_right);
				baseViews.setOutAnimation(this,R.anim.slide_to_left);
				baseViews.showNext();
				onDonePage = true;
				setProgress(10000);
			} else {
				succeed = false;
			}
		} else {     
			p.advance();
			
			logEvent(EventType.ENCOUNTER_NEXT_PAGE, Integer.toString(
					p.getCurrentIndex()));

			// Hide the keyboard
			InputMethodManager imm = (InputMethodManager)getSystemService(
					Context.INPUT_METHOD_SERVICE);
			if (imm != null && p != null && p.getCachedView() != null)
				imm.hideSoftInputFromWindow(p.getCachedView().getWindowToken(), 
						0);
			
			Log.v(TAG, "In nextPage(), current page index is: " 
					+ Integer.toString(p.getCurrentIndex()));
			setProgress(currentProg());
			
			// Tell the current page to play its first audio prompt
			p.current().playFirstPrompt();
		}

		updateNextPrev();
		return succeed;
	}
	
	// displays prev page
	private synchronized boolean prevPage() {
		boolean succeed = true;
		
		if(onDonePage) {
			baseViews.setInAnimation(this,R.anim.slide_from_left);
			baseViews.setOutAnimation(this,R.anim.slide_to_right);
			baseViews.showPrevious();
			onDonePage = false;
			setProgress(currentProg());
			
			// Tell the current page to play its first audio prompt
			p.current().playFirstPrompt();
		} 				
		//If was on start of procedures page
		//Back button will go back to procedures list page 
		else if(!p.hasPrevShowable()){   
			// This quits when you hit back and have nowhere else to go back to.
			setResult(RESULT_CANCELED, null);
			logEvent(EventType.ENCOUNTER_EXIT_NO_SAVE, "");
			finish();
			return succeed;
		}
		else if(p.hasPrevShowable()){
			p.back();
			Log.v("prev", Integer.toString(p.getCurrentIndex()));
			setProgress(currentProg());
			
			logEvent(EventType.ENCOUNTER_PREVIOUS_PAGE, 
					Integer.toString(p.getCurrentIndex()));
			
			// Save answers 
			storeCurrentProcedure(false);
			
			// Hide the keyboard
			InputMethodManager imm = (InputMethodManager)getSystemService(
					Context.INPUT_METHOD_SERVICE);
			if (imm != null && p != null && p.getCachedView() != null)
				imm.hideSoftInputFromWindow(p.getCachedView().getWindowToken(), 
						0);
			
			// Tell the current page to play its first audio prompt
			p.current().playFirstPrompt();
		}

		updateNextPrev();
		return succeed;
	}
	
	// current progress in the procedure
	private int currentProg() {
		int pageCount = p.getVisiblePageCount();
		if (pageCount == 0)
			return 10000;
		return (int) (10000 * (double)(p.getCurrentVisibleIndex())/pageCount);
	}
	
	/** The back key will activate previous page. */
	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
		switch(keycode) {
		case KeyEvent.KEYCODE_BACK:
			if (!this.wasOnDonePage)
				prevPage();
			setContentView(baseViews);
			wasOnDonePage = false;
			return true;
		default:
			return false;
		}   
	}
	/** {@inheritDoc} */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, OPTION_SAVE_EXIT, 0, "Save & Exit");
		menu.add(0, OPTION_DISCARD_EXIT, 1, "Discard & Exit");
		menu.add(0, OPTION_VIEW_PAGES, 2, "View Pages");
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				Constants.PREFERENCE_EDUCATION_RESOURCE, false))
			menu.add(0, OPTION_HELP, 3, "Help");
		return true;
	}

	ReentrantLock lock = new ReentrantLock();
	public boolean onOptionsItemSelected(MenuItem item){
		lock.lock();
		try{
			switch (item.getItemId()) {
			case OPTION_SAVE_EXIT:
				storeCurrentProcedure(false);
				setResult(RESULT_OK, null);
				logEvent(EventType.ENCOUNTER_SAVE_QUIT, "");
				finish();
				return true;
			case OPTION_DISCARD_EXIT:
				deleteCurrentProcedure();
				logEvent(EventType.ENCOUNTER_DISCARD, "");
				setResult(RESULT_CANCELED, null);
				finish();
				return true;
			case OPTION_VIEW_PAGES:
				this.wasOnDonePage = true;
				pageList();
				return true;
			case OPTION_HELP:
				showInfo(Audience.WORKER);
				return true;
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ALREADY_UPLOADED:
			return new AlertDialog.Builder(this)
			.setTitle(getResources().getString(
					R.string.general_alert))
			.setMessage(getResources().getString(
					R.string.dialog_already_uploaded))
			.setNeutralButton(getResources().getString(R.string.general_ok), 
					new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int whichButton) {
					// close without saving
					setResult(RESULT_OK, null);
				}
			})
			.setCancelable(false)
			.create();		
		default:
			break;
		}
		//this.onRestoreInstanceState(null);
		return null;
	}
	
	// sets the list of procedure pages
	private void pageList() {
		ListView mList = new ListView(this);
		List<String> pList = p.toStringArray();

		//int currentPage = p.getCurrentIndex(); 

		mList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.pageslist_item, pList));

		mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentView, View childView, 
					int position, long id) {
				onDonePage = false;
				wasOnDonePage = false;

				logEvent(EventType.ENCOUNTER_JUMP_TO_QUESTION, 
						Integer.toString(position));
				p.jumpToVisiblePage(position);
				baseViews.setDisplayedChild(0);
				setProgress(currentProg());
				updateNextPrev();
				setContentView(baseViews);
			}
		});
		setContentView(mList);
	}
	
	/**
	 * Launches the EducationList activity. Audience may be patient or worker.
	 * 
	 * @param audience the target audience.
	 */
	public synchronized void showInfo(Audience audience){
		Log.d(TAG, "Launching Help, audience: " + audience);
		// Gets the elements of the current page which have help
		Intent i = p.current().educationResources(audience);
		if(i == null){
			Toast.makeText(this, getString(R.string.dialog_no_help_available), 
					Toast.LENGTH_SHORT).show();
		} else {
			try{
				startActivityForResult(i, INFO_INTENT_REQUEST_CODE);
			} catch(ActivityNotFoundException e){
				Log.e(TAG, e.getMessage());
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void onClick(View v) {
		
		if(v == next) {
			nextPage();
		} else if(v == prev) {
			prevPage();
		} else if(v == info) {
			showInfo(Audience.WORKER);
		} else {
			switch(v.getId()) {
			case R.id.procedure_done_back:
				prevPage();
				break;
			case R.id.procedure_done_upload:
				uploadProcedureInBackground();
				break;
			default:
				Log.e(TAG, "Got onClick from unexpected id " + v.getId());
			}
		}

	}

	// A static temporary image file
	private static File getTemporaryImageFile() {
		return new File(Environment.getExternalStorageDirectory(), "sana.jpg");
	}

	/**
	 * Handles launching data capture Activities.
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		int description = intent.getExtras().getInt(INTENT_KEY_STRING);
		Log.i(TAG, "description = " + description);
		try {
			switch (description) {
			case 0: // intent comes from PictureElement to launch camera app
				params = intent.getStringArrayExtra(PictureElement.PARAMS_NAME);

				// For Android 1.1: 
				//Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE"); 

				// For Android >=1.5:
				Intent cameraIntent = new Intent(
						MediaStore.ACTION_IMAGE_CAPTURE);

				// EXTRA_OUTPUT is broken on a lot of phones. The HTC G1, Tattoo,
				// and Wildfire return a majorly downsampled version of the
				// image. In the HTC Sense UI, this is a bug with their camera.
				// With vanilla Android, it's a bug in 1.6. 

				Uri tempImageUri = Uri.fromFile(getTemporaryImageFile());
				// This extra tells the camera to return a larger image - only works in >=1.5
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri);
				startActivityForResult(cameraIntent,CAMERA_INTENT_REQUEST_CODE);
				break;
			case PLUGIN_INTENT_REQUEST_CODE:
				Log.d(TAG, "Got request to start plugin activity.");
				mEncounterState = new Intent();
				mEncounterState.setDataAndType(intent.getData(),
												intent.getType());
				Log.d(TAG,"State: " + mEncounterState.toUri(
										Intent.URI_INTENT_SCHEME));
				Intent plug = intent.getParcelableExtra(Intent.EXTRA_INTENT);
				Log.d(TAG, "Plug: " + plug.toUri(Intent.URI_INTENT_SCHEME));
				startActivityForResult(plug, PLUGIN_INTENT_REQUEST_CODE);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			for(Object o : e.getStackTrace())
				Log.e(TAG, "...." + o);
			Toast.makeText(this, this.getString(R.string.msg_err_no_plugin), 
					Toast.LENGTH_SHORT).show();
		}
	}
	
	/** 
	 * Handles the results of Activities launched for data capture.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			final Intent data) 
	{
		Log.d(TAG, "Returned. requestCode: " + requestCode);
		Log.d(TAG, "......... resultCode : " + resultCode);
		Log.d(TAG, "......... obs        : " + mEncounterState.getData());
		Log.d(TAG, "......... type       : " + mEncounterState.getType());
		String answer = "";
		switch(resultCode){
		case(RESULT_OK):
			try{
				switch(requestCode){
				/*
				case (BARCODE_INTENT_REQUEST_CODE):
					String contents = data.getStringExtra("SCAN_RESULT");
					String format = data.getStringExtra("SCAN_RESULT_FORMAT");
					Log.i(TAG, "Got result from barcode intent: " + contents);
					ProcedurePage pp = p.current();
					PatientIdElement patientId = pp.getPatientIdElement();
					patientId.setAndRefreshAnswer(contents);
					break;
				*/
				// TODO the camera should get removed.
				case (CAMERA_INTENT_REQUEST_CODE):
					ImageProcessingTaskRequest request = 
							new ImageProcessingTaskRequest();
					request.savedProcedureId = params[0];
					request.elementId = params[1];
					request.tempImageFile = getTemporaryImageFile();
					request.c = this;
					request.intent = data;
					Log.i(TAG, "savedProcedureId " + request.savedProcedureId 
								+ " and elementId " + request.elementId);

					// Handles making a thumbnail of the image and moving it 
					// from the temporary location.
					ImageProcessingTask imageTask = new ImageProcessingTask();
					imageTask.execute(request);
					break;
				case (INFO_INTENT_REQUEST_CODE):
					Log.d(TAG, "EducationResource intent: " + data.getType());
					if(data.getType().contains("text/plain")){
						String text = data.getStringExtra("text");
						String title = data.getStringExtra(Intent.EXTRA_TITLE);
						MocaUtil.createDialog(this, title, text).show();
					} else {
						startActivity(data);
					}
					break;
				case PLUGIN_INTENT_REQUEST_CODE:
					Uri mObs = mEncounterState.getData();
					String mObsType = mEncounterState.getType();
					Intent result = PluginService.renderPluginActivityResult(
							getContentResolver(), data, mObs, mObsType);
					String type = result.getType();
					Uri rData = result.getData();
					
					// Check if we get plain text first
					if(type.equals("text/plain")){
						answer = rData.getFragment();
						
					// Otherwise we have binary blob so we insert
					} else {
						Uri uri = BinaryDAO.updateOrCreate(
									getContentResolver(), 
									mObs.getPathSegments().get(1), 
									mObs.getPathSegments().get(2), 
									rData,type);
						Log.d(TAG, "Binary insert uri: " + uri.toString());
						answer = BinaryDAO.getUUID(uri);
					}
					break;
				default:
					Log.e(TAG, "Unknown activity");
					answer = "";
					break;
				} 

				p.current().setElementValue(
						mEncounterState.getData().getPathSegments().get(2), 
						answer);
				Log.d(TAG, "Got answer: " + answer);
			} catch (Exception e){
				e.printStackTrace();
				Log.e(TAG, "Error capturing answer from RESULT_OK: " 
						+ e.toString());
			}
			break;
		default:
			Log.i(TAG, "Activity cancelled.");
			break;
		}
		
	}
	
	/**
	 * Adds current procedure to queue for upload.
	 */
	public void uploadProcedureInBackground() {
		storeCurrentProcedure(true);
		//First check to make sure procedure has not already been uploaded
		if (MDSInterface.isProcedureAlreadyUploaded(thisSavedProcedure, 
				getBaseContext())) {
			showDialog(DIALOG_ALREADY_UPLOADED);
		}
		else {
			Log.i(TAG, "Adding current procedure to background upload queue");
			if (mUploadService != null) {
				mUploadService.addProcedureToQueue(thisSavedProcedure);
			}
			logEvent(EventType.ENCOUNTER_SAVE_UPLOAD, "");
			finish();
		}
	}

	/**
	 * Updates the next and previous page references.
	 */
	public void updateNextPrev() {
		prev.setEnabled(p.hasPrev());
		prev.setText(getResources().getString(
				R.string.procedurerunner_previous));
		if(p.hasNext()) {
			next.setText(getResources().getString(
					R.string.procedurerunner_next));
		} else {
			next.setText(getResources().getString(
					R.string.procedurerunner_done));
		}
	}

	/**
	 * A request for loading a procedure.
	 * 
	 * @author Sana Development Team
	 *
	 */
	class ProcedureLoadRequest {
		Bundle instance;
		Intent intent;
	}
	
	/**
	 * The result of loading a procedure
	 * 
	 * @author Sana Development Team
	 *
	 */
	class ProcedureLoadResult {
		Uri procedureUri;
		Uri savedProcedureUri;
		Procedure p = null;
		boolean success = false;
		String errorMessage = "";
	}
	
	/**
	 * A task for loading a procedure.
	 *  
	 * @author Sana Development Team
	 *
	 */
	class ProcedureLoaderTask extends AsyncTask<ProcedureLoadRequest, Void, 
		ProcedureLoadResult> 
	{
		Bundle instance;
		Intent intent;
		
		/** {@inheritDoc} */
		@Override
		protected ProcedureLoadResult doInBackground(
				ProcedureLoadRequest... params) 
		{
			ProcedureLoadRequest load = params[0];
			instance = load.instance;
			intent = load.intent;

			ProcedureLoadResult result = new ProcedureLoadResult();
			if (instance == null && !intent.hasExtra("savedProcedureUri")) {
				Uri procedure = intent.getData();
				int procedureId = Integer.parseInt(
						procedure.getPathSegments().get(1));
				String procedureXml = ProcedureDAO.getXMLForProcedure(
						ProcedureRunner.this, procedure);

				// Record that we are starting a new encounter
				logEvent(EventType.ENCOUNTER_LOAD_NEW_ENCOUNTER, 
						procedure.toString());
				
				ContentValues cv = new ContentValues();
				cv.put(SavedProcedureSQLFormat.PROCEDURE_ID, procedureId);
				cv.put(SavedProcedureSQLFormat.PROCEDURE_STATE, "");
				cv.put(SavedProcedureSQLFormat.FINISHED, true);
				cv.put(SavedProcedureSQLFormat.UPLOADED, false);     
				cv.put(SavedProcedureSQLFormat.UPLOAD_STATUS, 0);
				
				thisSavedProcedure = getContentResolver().insert(
						SavedProcedureSQLFormat.CONTENT_URI, cv);

				Log.i(TAG, "onCreate() : uri = " + procedure.toString() 
						+ " savedUri=" + thisSavedProcedure);

				Procedure p = null;
				try {
					p = Procedure.fromXMLString(procedureXml);
				} catch (IOException e) {
					Log.e(TAG, "Error loading procedure from XML: " 
							+ e.toString());
					e.printStackTrace();
					logException(e);
				} catch (ParserConfigurationException e) {
					Log.e(TAG, "Error loading procedure from XML: " 
							+ e.toString());
					e.printStackTrace();
					logException(e);
				} catch (SAXException e) {
					Log.e(TAG, "Error loading procedure from XML: " 
							+ e.toString());
					e.printStackTrace();
					logException(e);
				} catch (ProcedureParseException e) {
					Log.e(TAG, "Error loading procedure from XML: " 
							+ e.toString());
					e.printStackTrace();
					logException(e);
				} catch (OutOfMemoryError e) {
					Log.e(TAG, "Can't load procedure, out of memory.");
					result.errorMessage = "Out of Memory."; 
					e.printStackTrace();
					logException(e);
				}
				if (p != null) {
					p.setInstanceUri(thisSavedProcedure);
				}
				
				result.p = p;
				result.success = p != null;
				result.procedureUri = procedure;
				result.savedProcedureUri = thisSavedProcedure;
				
			} else {
				startPage = 0;
				PatientInfo pi = null;
				// This is a warm-boot. Restore the state from the saved state.
				if(instance == null) { 
					Log.v(TAG, "No instance on warm boot.");
					startPage = 0;
					onDonePage = false;
					String savedProcedureUri = intent.getStringExtra(
							"savedProcedureUri");
					if (savedProcedureUri != null)
						thisSavedProcedure = Uri.parse(savedProcedureUri);
					
					// Record that we are restoring a saved encounter
					logEvent(EventType.ENCOUNTER_LOAD_SAVED, "");
					
					pi = new PatientInfo();
				} else {
					Log.v(TAG, "Instance present on warm boot.");
					
					startPage = instance.getInt("currentPage");
					onDonePage = instance.getBoolean("onDonePage");
					String savedProcedureUri = instance.getString(
							"savedProcedureUri");

					pi = instance.getParcelable("patientInfo");
					if (pi == null) {
						Log.e(TAG, "Could not get patient info from parcel.");
					} else {
						Log.v(TAG, "Restored patient info from parcel.");
					}
					
					if (savedProcedureUri != null)
						thisSavedProcedure = Uri.parse(savedProcedureUri);
					
					// Record that we are doing a hot-load
					logEvent(EventType.ENCOUNTER_LOAD_HOTLOAD, "");

				}

				if (thisSavedProcedure == null) {
					Log.e(TAG, "Couldn't determine the URI to warm boot with, "+
							"bailing.");
					return result;
				}
				Log.i(TAG, "Warm boot occured for " + thisSavedProcedure 
						+ ", startPage:" + startPage + " onDonePage: " 
						+ onDonePage);

				Cursor c = null;
				int procedureId = -1;
				String answersJson = "";
				try {
					c = getContentResolver().query(thisSavedProcedure, 
							new String [] { 
								SavedProcedureSQLFormat.PROCEDURE_ID,
								SavedProcedureSQLFormat.PROCEDURE_STATE }, 
							null, null, null);        
					c.moveToFirst();
					procedureId = c.getInt(0);
					answersJson = c.getString(1);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
					e.printStackTrace();
				} finally {
					if (c != null) {
						c.close();
					}
				}

				Map<String,String> answersMap = new HashMap<String,String>();
				try {
					JSONTokener tokener = new JSONTokener(answersJson);
					JSONObject answersDict = new JSONObject(tokener);
					Iterator it = answersDict.keys();
					while(it.hasNext()) {
						String key = (String)it.next();
						answersMap.put(key, answersDict.getString(key));
						Log.i(TAG, "ProcedureLoaderTask loaded answer '" 
								+ key + "' : '" + answersDict.getString(key) 
								+"'");
					}
				} catch(JSONException e) {
					Log.e(TAG, "onCreate() -- JSONException " + e.toString());	
					e.printStackTrace();
				}
				
				Uri procedureUri = ContentUris.withAppendedId(
						ProcedureSQLFormat.CONTENT_URI, procedureId);
				String procedureXml = ProcedureDAO.getXMLForProcedure(
						ProcedureRunner.this, procedureUri);
				Procedure procedure = null;
				try {
					procedure = Procedure.fromXMLString(procedureXml);
					procedure.setInstanceUri(thisSavedProcedure);
					procedure.restoreAnswers(answersMap);
				} catch (IOException e) {
					Log.e(TAG, "onCreate() -- IOException " + e.toString());
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					Log.e(TAG, "onCreate() -- couldn't create parser");
					e.printStackTrace();
				} catch (SAXException e) {
					Log.e(TAG, "onCreate() -- Couldn't parse XML");
					e.printStackTrace();
				} catch (ProcedureParseException e) {
					Log.e(TAG, "Error in Procedure.fromXML() : " 
							+ e.toString());
					e.printStackTrace();
				} catch (OutOfMemoryError e) {
					Log.e(TAG, "Can't load procedure, out of memory.");
					result.errorMessage = "Out of Memory."; 
					e.printStackTrace();
					
				}
				Log.i(TAG, "onCreate() : warm-booted from uri =" 
						+ thisSavedProcedure);
				
				if (procedure != null && pi != null) {
					procedure.setPatientInfo(pi);
				}
				
				result.p = procedure;
				result.success = procedure != null;
				result.savedProcedureUri = thisSavedProcedure;
				result.procedureUri = procedureUri;
			}

			return result;
		}
		
		/** {@inheritDoc} */
		@Override
		protected void onPostExecute(ProcedureLoadResult result) {
			super.onPostExecute(result);
			if (loadProgressDialog != null) {
				loadProgressDialog.dismiss();
				loadProgressDialog = null;
			}
			if (result != null && result.success) {
				p = result.p;
				thisSavedProcedure = result.savedProcedureUri;
				logEvent(EventType.ENCOUNTER_LOAD_FINISHED, "");
				if(p != null)
					createView();
				else
					logEvent(EventType.ENCOUNTER_LOAD_FAILED, "Null procedure");
					
			} else {
				// Show error
				logEvent(EventType.ENCOUNTER_LOAD_FAILED, "");
				ProcedureRunner.this.finish();
			}
		}
	}
	
	/**
	 * Creates the base view of this object.
	 */
	public void createView() {
		if (p == null)
			return;	
		setTitle(p.getTitle());
		View procedureView = wrapViewWithInterface(p.toView(this));

		// Now that the view is active, go to the correct page.
		if(p.getCurrentIndex() != startPage) {
			p.jumpToPage(startPage);
			updateNextPrev();            
		}

		baseViews = new ViewAnimator(this);
		baseViews.setBackgroundResource(android.R.drawable.alert_dark_frame);
		baseViews.setInAnimation(AnimationUtils.loadAnimation(this,
				R.anim.slide_from_right));
		baseViews.setOutAnimation(AnimationUtils.loadAnimation(this,
				R.anim.slide_to_left));
		baseViews.addView(procedureView);

		// This should add it to baseViews, so don't add it manually.
		View procedureDonePage = getLayoutInflater().inflate(
				R.layout.procedure_runner_done, baseViews);
		((TextView)procedureDonePage.findViewById(R.id.procedure_done_text))
			.setTextAppearance(this, android.R.style.TextAppearance_Large);
		procedureDonePage.findViewById(R.id.procedure_done_back)
			.setOnClickListener(this);
		procedureDonePage.findViewById(R.id.procedure_done_upload)
			.setOnClickListener(this);

		if(onDonePage) {
			baseViews.setInAnimation(null);
			baseViews.setOutAnimation(null);
			baseViews.showNext();
			baseViews.setInAnimation(AnimationUtils.loadAnimation(this,
					R.anim.slide_from_right));
			baseViews.setOutAnimation(AnimationUtils.loadAnimation(this,
					R.anim.slide_to_left));
		}

		setContentView(baseViews);
		setProgressBarVisibility(true);
		setProgress(0);
	}
	
	/** {@inheritDoc} */
	@Override
	public void onCreate(Bundle instance) {
		super.onCreate(instance);
		Log.v(TAG, "onCreate");

		try {
        	mConnector.setServiceListener(this);
        	mConnector.connect(this);
        }
        catch (Exception e) {
        	Log.e(TAG, "Exception starting background upload service: " 
        			+ e.toString());
        	e.printStackTrace();
        }

		logEvent(EventType.ENCOUNTER_ACTIVITY_START_OR_RESUME, "");

		requestWindowFeature(Window.FEATURE_PROGRESS);
		
		if (p == null) {
			ProcedureLoadRequest request = new ProcedureLoadRequest();
			request.instance = instance;
			request.intent = getIntent();

			logEvent(EventType.ENCOUNTER_LOAD_STARTED, "");
			procedureLoaderTask = 
				(ProcedureLoaderTask) new ProcedureLoaderTask().execute(request);
			loadProgressDialog = new ProgressDialog(this);
		    loadProgressDialog.setMessage("Loading procedure."); // TODO i18n
		    loadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    if(!isFinishing())
		    	loadProgressDialog.show();
		}
	}

	/**
	 * Takes a view and wraps it with next/previous buttons for navigating.
	 * 
	 * @param sub - the view which is to be wrapped
	 * @return - a new view which is <param>sub</param> wrapped with next/prev 
	 * 			buttons.
	 */
	public View wrapViewWithInterface(View sub) {
		//View sub = state.current().toView(this);
		//RelativeLayout rl = new RelativeLayout(this);
		//rl.setLayoutParams( new ViewGroup.LayoutParams( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT ) );

		LinearLayout base = new LinearLayout(this);
		base.setOrientation(LinearLayout.VERTICAL);

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		next = new Button(this);
		next.setOnClickListener(this);
		info = new Button(this);
		info.setOnClickListener(this);
		info.setText(getResources().getString(
				R.string.procedurerunner_info));
		info.setText(getResources().getString(R.string.procedurerunner_info));
		prev = new Button(this);
		prev.setOnClickListener(this);

		updateNextPrev();
		// Are we dispalying Info button
		boolean showEdu = PreferenceManager.getDefaultSharedPreferences(this)
			.getBoolean(Constants.PREFERENCE_EDUCATION_RESOURCE, false);
		float nextWeight = showEdu? 0.333f: 0.5f;
		float infoWeight = showEdu? 0.334f: 0.0f;
		float prevWeight = showEdu? 0.333f: 0.5f;
		
		ll.addView(prev,new LinearLayout.LayoutParams(-2,-1, prevWeight));
		// Only show info button if Education Resource Setting is true
		if(showEdu)
			ll.addView(info,new LinearLayout.LayoutParams(-2,-1, infoWeight));
		ll.addView(next,new LinearLayout.LayoutParams(-2,-1, nextWeight));
		ll.setWeightSum(1.0f);

		//RelativeLayout.LayoutParams ll_lp = new RelativeLayout.LayoutParams(-1,100);
		//ll_lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//rl.addView(ll, ll_lp);

		//RelativeLayout.LayoutParams sub_lp = new RelativeLayout.LayoutParams(-1,-1);
		//sub_lp.addRule(RelativeLayout.ABOVE, ll.getId());
		//rl.addView(sub, sub_lp);

		//ScrollView sv = new ScrollView(this);
		//sv.addView(sub, new ViewGroup.LayoutParams(-1,-1));
		//base.addView(sv, new LinearLayout.LayoutParams(-1,-2,0.99f));
		base.addView(sub, new LinearLayout.LayoutParams(-1,-2,0.99f));
		base.addView(ll, new LinearLayout.LayoutParams(-1,-2,0.01f));

		base.setWeightSum(1.0f);

		return base;
	}
	
	/** {@inheritDoc} */
	@Override
	public void onResume() {
		super.onResume();
        if (mSavedState != null) restoreLocalTaskState(mSavedState);
		Log.i(TAG, "onResume()");
	}

	/**
	 * Serializes the current procedure to the database. Takes the answers map
	 * from the procedure, serializes it to JSON, and stores it. If finished is
	 * set, then it will set the procedure's row to finished. This will signal
	 * to the upload service that it is ready for upload.
	 * 
	 * @param finished
	 *            -- Whether to set the procedure as ready for upload.
	 */
	public void storeCurrentProcedure(boolean finished) {
		if(p != null && thisSavedProcedure != null) {
			JSONObject answersMap = new JSONObject(p.toAnswers());
			String json = answersMap.toString();

			ContentValues cv = new ContentValues();
			cv.put(SavedProcedureSQLFormat.PROCEDURE_STATE, json);

			if(finished) 
				cv.put(SavedProcedureSQLFormat.FINISHED, finished);

			int updatedObjects = getContentResolver().update(thisSavedProcedure, 
					cv, null, null);
			Log.i(TAG, "storeCurrentProcedure updated " + updatedObjects 
					+ " objects. (SHOULD ONLY BE 1)");
		}
	}

	/**
	 * Removes the current procedure form the database.
	 */
	public void deleteCurrentProcedure() {
		getContentResolver().delete(thisSavedProcedure, null, null);
	}
	
	/** {@inheritDoc} */
	@Override
	public void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");

		// This is the last method in which we are guaranteed not to be killed,
		// so save state here.
		storeCurrentProcedure(false);

		if (lookupProgress != null) {
			lookupProgress.dismiss();
			lookupProgress = null;
		}
		if (loadProgressDialog != null) {
			loadProgressDialog.dismiss();
			loadProgressDialog = null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		if (mConnector != null) {
			try {
				mConnector.disconnect(this);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "While disconnecting service got exception: " 
						+ e.toString());
				e.printStackTrace();
			}
		}
		if (p != null) {
			p.clearCachedViews();
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void onStop() {
		super.onStop();
		Log.i(TAG,"onStop");
	}

	/**
	 * If we are warm booted, then we need to know that we saved our procedure
	 * state at some URI. We also need to know where the user was in the process
	 * of filling out the form, etc. So when the activity is closing, we store
	 * the state in a bundle so that we can read it when we are created again.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveLocalTaskState(outState);
		Log.v(TAG, "onSaveInstanceState");
		
		if (thisSavedProcedure != null) 
			outState.putParcelable(STATE_SP_URI, thisSavedProcedure);
			
		if (p != null) {
			outState.putInt(STATE_CURRENT_PAGE, p.getCurrentIndex());
			// In a future, backwards incompatible version, we could do this differently.
			PatientInfo pi = p.getPatientInfo();
			if (pi != null)
				outState.putParcelable(STATE_PATIENT, pi);
		}
		outState.putBoolean(STATE_ON_DONE_PAGE, onDonePage);
		outState.putParcelable(STATE_ENCOUNTER, mEncounterState);
	}
	
    private void saveLocalTaskState(Bundle outState){
    	final ProcedureLoaderTask task = procedureLoaderTask;
        if (task != null && task.getStatus() != Status.FINISHED) {
        	task.cancel(true);
        	outState.putBoolean(STATE_LOAD_PROCEDURE, true);
        	outState.putParcelable(STATE_PROC_LOAD_BUNDLE, task.instance);
        	outState.putParcelable(STATE_PROC_LOAD_INTENT, task.intent);
        }

    	final PatientLookupTask pTask = patientLookupTask;
        if (pTask != null && pTask.getStatus() != Status.FINISHED) {
        	pTask.cancel(true);
        	outState.putBoolean(STATE_LOOKUP_PATIENT, true);
			outState.putString(STATE_PATIENT_ID, pTask.patientId);
        }
    }
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		Log.v(TAG, "onSaveInstanceState");
		restoreLocalTaskState(savedInstanceState);
		thisSavedProcedure = savedInstanceState.getParcelable(STATE_SP_URI);
		mEncounterState = savedInstanceState.getParcelable(STATE_ENCOUNTER);
        mSavedState = null;
	}
	
    /** Restores any tasks running on this thread */
    private void restoreLocalTaskState(Bundle savedInstanceState){
    	if (savedInstanceState.getBoolean(STATE_LOAD_PROCEDURE)){
    		final ProcedureLoaderTask task = new ProcedureLoaderTask();
    		final ProcedureLoadRequest request = new ProcedureLoadRequest(); 
			request.instance = savedInstanceState.getBundle(
					STATE_PROC_LOAD_BUNDLE);
			request.intent = savedInstanceState.getParcelable(
					STATE_PROC_LOAD_INTENT);
    		procedureLoaderTask = (ProcedureLoaderTask) task.execute(request);
    	}

    	if (savedInstanceState.getBoolean(STATE_LOOKUP_PATIENT)){
    		final PatientLookupTask plTask = new PatientLookupTask(this);
    		plTask.setPatientLookupListener(this);
    		String patientId = savedInstanceState.getString(STATE_PATIENT_ID);
    		patientLookupTask = (PatientLookupTask) plTask.execute(patientId);
    	}
    }
}

