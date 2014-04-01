package org.moca.activity.settings;

import org.moca.Constants;
import org.moca.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;

/**
 * Creates the settings window for specifying the Sana application.
 * 
 * If a user does not specify their own values, default values are used. Most of
 * these are stored in Constants. The default phone name is the phone's number.
 * 
 * String values are stored as preferences and can be retrieved as follows:
 * PreferenceManager.getDefaultSharedPreferences(c).getString("key name")
 * 
 * @author Sana Dev Team
 */
public class Settings extends PreferenceActivity {
	
	public static final String TAG = Settings.class.getSimpleName();
	
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
		dialogBasedPrefCat.setTitle("Sana Configuration");
		root.addPreference(dialogBasedPrefCat);

		// Phone name
		String phoneNum = ((TelephonyManager) getSystemService(
				Context.TELEPHONY_SERVICE))
				.getLine1Number();
		Log.d(TAG, "Phone number of this phone: " + phoneNum);
		if (TextUtils.isEmpty(phoneNum)) phoneNum = "5555555555";
		EditTextPreference phoneName = new EditTextPreference(this);
		phoneName.setDialogTitle(getString(R.string.setting_phone_name));
		phoneName.setKey(Constants.PREFERENCE_PHONE_NAME);
		phoneName.setTitle(getString(R.string.setting_phone_name));
		phoneName.setSummary(getString(R.string.setting_phone_name_summary));
		// default value is the phone number of the phone
		phoneName.setDefaultValue(phoneNum);
		dialogBasedPrefCat.addPreference(phoneName);

		// Health worker username for OpenMRS
		EditTextPreference emrUsername = new EditTextPreference(this);
		emrUsername.setDialogTitle(getString(R.string.setting_emr_username));
		emrUsername.setKey(Constants.PREFERENCE_EMR_USERNAME);
		emrUsername.setTitle(getString(R.string.setting_emr_username));
		emrUsername.setSummary(getString(R.string.setting_emr_username_summary));
		emrUsername.setDefaultValue(Constants.DEFAULT_USERNAME);
		dialogBasedPrefCat.addPreference(emrUsername);
		
		// Health worker password for OpenMRS
		EditTextPreference emrPassword = new EditTextPreference(this);
		emrPassword.setDialogTitle(getString(R.string.setting_emr_password));
		emrPassword.setKey(Constants.PREFERENCE_EMR_PASSWORD);
		emrPassword.setTitle(getString(R.string.setting_emr_password));
		emrPassword.setSummary(getString(R.string.setting_emr_password_summary));
		emrPassword.setDefaultValue(Constants.DEFAULT_PASSWORD);
		dialogBasedPrefCat.addPreference(emrPassword);
		emrPassword.getEditText().setTransformationMethod(
				new PasswordTransformationMethod());
		
		// Whether barcode reading is enabled on the phone
		/*CheckBoxPreference barcodeEnabled = new CheckBoxPreference(this);
		barcodeEnabled.setKey(Constants.PREFERENCE_BARCODE_ENABLED);
		barcodeEnabled.setTitle("Enable barcode reading");
		barcodeEnabled.setSummary("Enable barcode reading of patient and physician ids");
		barcodeEnabled.setDefaultValue(false);
		dialogBasedPrefCat.addPreference(barcodeEnabled);*/
		
		// Launches network preferences
        PreferenceScreen intentPref = getPreferenceManager()
        								.createPreferenceScreen(this);
        intentPref.setIntent(new Intent(Settings.this, 
        		NetworkSettings.class));
        intentPref.setTitle(getString(R.string.setting_network));
        intentPref.setSummary(getString(R.string.setting_network_summary));
        dialogBasedPrefCat.addPreference(intentPref);
		
        // Launches resource preferences
        PreferenceScreen resourcePref = getPreferenceManager()
        								.createPreferenceScreen(this);
        resourcePref.setIntent(new Intent(Settings.this, 
        		ResourceSettings.class));
        resourcePref.setTitle(getString(R.string.setting_resource));
        resourcePref.setSummary(getString(R.string.setting_resource_summary));
        dialogBasedPrefCat.addPreference(resourcePref);
	
        // return the preference screen
		return root;
	}
}
