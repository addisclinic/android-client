package org.moca.activity.settings;

import org.moca.Constants;
import org.moca.R;
import org.moca.activity.EducationResourceList;
import org.moca.activity.ProcedureSdImporter;
import org.moca.media.EducationResource.Audience;
import org.moca.task.ImageProcessingTask;
import org.moca.task.ImageProcessingTaskRequest;
import org.moca.util.MocaUtil;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.method.DigitsKeyListener;
import android.util.Log;

/**
 * Creates the settings window for configuring and accessing resources 
 * available to the application.
 * 
 * If a user does not specify their own values, default values are used. Most of
 * these are stored in Constants.
 * 
 * String values are stored as preferences and can be retrieved as follows:
 * PreferenceManager.getDefaultSharedPreferences(c).getString("key name")
 * 
 * @author Sana Dev Team
 */
public class ResourceSettings extends PreferenceActivity{
	public static final String TAG = ResourceSettings.class.getSimpleName();
	
	/** {@inheritDoc} */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(createPreferenceHierarchy());
	}

	/** Lays out the preference screeen */
	private PreferenceScreen createPreferenceHierarchy() {
		
		// TODO Eliminate programmatic generation of the preference items -- put
		// all this in an XML and inflate it.

		// TODO Also, all the key values for these preferences should be
		// constants! They are littered everywhere in the code!

		// Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
				this);
		
		// System Config Prefs
		PreferenceCategory dialogBasedPrefCat = new PreferenceCategory(this);
		dialogBasedPrefCat.setTitle("Sana Resource Configuration");
		root.addPreference(dialogBasedPrefCat);

		// Binary file location
		EditTextPreference binaryFileLocation = new EditTextPreference(this);
		binaryFileLocation.setDialogTitle(getString(
				R.string.setting_storage_directory));
		binaryFileLocation.setKey(Constants.PREFERENCE_STORAGE_DIRECTORY);
		binaryFileLocation.setTitle(getString(
				R.string.setting_storage_directory));
		binaryFileLocation.setSummary(
				getString(R.string.setting_storage_directory_summary));
		binaryFileLocation.setDefaultValue(
				Constants.DEFAULT_BINARY_FILE_FOLDER);
		dialogBasedPrefCat.addPreference(binaryFileLocation);
		
		// Image downscale factor
		EditTextPreference imageDownscale = new EditTextPreference(this);
		imageDownscale.setDialogTitle(getString(R.string.setting_image_scale));
		imageDownscale.setKey(Constants.PREFERENCE_IMAGE_SCALE);
		imageDownscale.setTitle(getString(R.string.setting_image_scale));
		imageDownscale.setSummary(getString(R.string.setting_image_scale));
		imageDownscale.setDefaultValue(Integer
				.toString(Constants.IMAGE_SCALE_FACTOR));
		imageDownscale.getEditText().setKeyListener(new DigitsKeyListener());
		dialogBasedPrefCat.addPreference(imageDownscale);
		
		// Whether to info button shows up on procedure pages
		CheckBoxPreference viewEducationResources = new CheckBoxPreference(this);
		viewEducationResources.setKey(Constants.PREFERENCE_EDUCATION_RESOURCE);
		viewEducationResources.setTitle(getString(R.string.setting_edu));
		viewEducationResources.setSummary(getString(
				R.string.setting_edu_summary));
		viewEducationResources.setDefaultValue(false);
		dialogBasedPrefCat.addPreference(viewEducationResources);
		
		// View all edu resources
        PreferenceScreen resourcePref = getPreferenceManager()
        								.createPreferenceScreen(this);

		Intent intent = EducationResourceList.getIntent(Intent.ACTION_PICK, 
				Audience.ALL);
		intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_VIEW));
        resourcePref.setIntent(intent);
        resourcePref.setTitle(getString(R.string.setting_edu_viewer));
        resourcePref.setSummary(getString(R.string.setting_edu_viewer_summary));
        dialogBasedPrefCat.addPreference(resourcePref);
		
        // SD card loading procedures
        PreferenceScreen intentPref = getPreferenceManager()
        								.createPreferenceScreen(this);
        intentPref.setIntent(new Intent("org.moca.activity.IMPORT_PROCEDURE"));
        //intentPref.setIntent(new Intent(ResourceSettings.this, 
        //		ProcedureSdImporter.class));
        intentPref.setTitle(getString(R.string.setting_procedure));
        intentPref.setSummary(getString(R.string.setting_procedure_summary));
        dialogBasedPrefCat.addPreference(intentPref);
        
        // return the preference screen
		return root;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			final Intent data) 
	{
		Log.d(TAG, "Returned. requestCode: " + requestCode);
		Log.d(TAG, "......... resultCode: " + resultCode);
		if(data == null)
			Log.d(TAG, "data: Returned null data intent");
		
		Log.d(TAG, "......... data: " + data.toUri(Intent.URI_INTENT_SCHEME));
		try{
			switch(resultCode){
			case(RESULT_OK):
				if(data.getAction().equals(Intent.ACTION_VIEW)){
					Log.d(TAG, "EducationResource intent: " + data.getType());
					if(data.getType().contains("text/plain")){
						String text = data.getStringExtra("text");
						String title = data.getStringExtra(Intent.EXTRA_TITLE);
						MocaUtil.createDialog(this, title, text).show();
					} else { 
						Log.d(TAG, "View intent.");
						startActivity(data);
					}
				}
			}
		} catch (Exception e){

		}
	}
}
