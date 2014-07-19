package org.moca.activity.settings;

import org.moca.Constants;
import org.moca.R;

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.method.DialerKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;

/**
 * Creates the settings window for communicating with the Sana network 
 * layer
 * 
 * If a user does not specify their own values, default values are used. Most of
 * these are stored in Constants. The default phone name is the phone's number.
 * 
 * String values are stored as preferences and can be retrieved as follows:
 * PreferenceManager.getDefaultSharedPreferences(c).getString("key name")
 * 
 * @author Sana Dev Team
 */
public class NetworkSettings extends PreferenceActivity{
	public static final String TAG = NetworkSettings.class.getSimpleName();
	
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
		
		// Network Config Prefs
		PreferenceCategory dialogBasedPrefCat = new PreferenceCategory(this);
		dialogBasedPrefCat.setTitle(getString(R.string.settings_network_title));
		root.addPreference(dialogBasedPrefCat);

		// Moca Dispatch Server URL
		EditTextPreference mdsUrl = new EditTextPreference(this);
		mdsUrl.setDialogTitle(getString(R.string.setting_mds_url));
		mdsUrl.setKey(Constants.PREFERENCE_MDS_URL);
		mdsUrl.setTitle(getString(R.string.setting_mds_url));
		mdsUrl.setSummary(getString(R.string.setting_mds_url_summary));
		mdsUrl.setDefaultValue(Constants.DEFAULT_DISPATCH_SERVER);
		dialogBasedPrefCat.addPreference(mdsUrl);

		// Whether to enable upload hacks for strict carriers
		CheckBoxPreference useSecureTransmission = new CheckBoxPreference(this);
		useSecureTransmission.setKey(Constants.PREFERENCE_SECURE_TRANSMISSION);
		useSecureTransmission.setTitle(getString(R.string.setting_secure));
		useSecureTransmission.setSummary(getString(
				R.string.setting_secure_summary));
		useSecureTransmission.setDefaultValue(false);
		dialogBasedPrefCat.addPreference(useSecureTransmission);
		
		// Initial packet size
		EditTextPreference initialPacketSize = new EditTextPreference(this);
		initialPacketSize.setDialogTitle("Starting packet size in KB");
		initialPacketSize.setKey(Constants.PREFERENCE_PACKET_SIZE);
		initialPacketSize.setTitle(getString(R.string.setting_pkt_size));
		initialPacketSize.setSummary(getString(
				R.string.setting_pkt_size_summary));
		initialPacketSize.setDefaultValue(Integer
				.toString(Constants.DEFAULT_INIT_PACKET_SIZE));
		initialPacketSize.getEditText().setKeyListener(new DigitsKeyListener());
		dialogBasedPrefCat.addPreference(initialPacketSize);

		// How often the database gets refreshed
		EditTextPreference databaseRefresh = new EditTextPreference(this);
		databaseRefresh.setDialogTitle(getString(R.string.setting_emr_refresh));
		databaseRefresh.setKey(Constants.PREFERENCE_DATABASE_UPLOAD);
		databaseRefresh.setTitle(getString(R.string.setting_emr_refresh));
		databaseRefresh.setDefaultValue(Integer.toString(
				Constants.DEFAULT_DATABASE_UPLOAD));
		databaseRefresh.setSummary(
				getString(R.string.setting_emr_refresh_summary));
		databaseRefresh.getEditText().setKeyListener(new DigitsKeyListener());
		dialogBasedPrefCat.addPreference(databaseRefresh);
		
		// Proxy host settings
		EditTextPreference proxyHost = new EditTextPreference(this);
		proxyHost.setDialogTitle(getString(R.string.setting_proxy_host));
		proxyHost.setKey(Constants.PREFERENCE_PROXY_HOST);
		proxyHost.setTitle(getString(R.string.setting_proxy_host));
		proxyHost.setSummary(getString(R.string.setting_proxy_host_summary));
		proxyHost.setDefaultValue("");
		dialogBasedPrefCat.addPreference(proxyHost);
		
		// Proxy port settings
		EditTextPreference proxyPort = new EditTextPreference(this);
		proxyPort.setDialogTitle(getString(R.string.setting_proxy_port));
		proxyPort.setKey(Constants.PREFERENCE_PROXY_PORT);
		proxyPort.setTitle(getString(R.string.setting_proxy_port));
		proxyPort.setSummary(getString(R.string.setting_proxy_port_summary));
		proxyPort.setDefaultValue("");
		proxyPort.getEditText().setKeyListener(new DialerKeyListener());
		dialogBasedPrefCat.addPreference(proxyPort);
		
		// Estimated network bandwidth
		EditTextPreference estimatedNetworkBandwidth = 
				new EditTextPreference(this);
		estimatedNetworkBandwidth.setDialogTitle(
				getString(R.string.setting_bandwidth));
		estimatedNetworkBandwidth.setKey("s_network_bandwidth");
		estimatedNetworkBandwidth.setTitle(
				getString(R.string.setting_bandwidth));
		estimatedNetworkBandwidth.setSummary(
				getString(R.string.setting_bandwidth_summary));
		estimatedNetworkBandwidth.setDialogMessage(
				getString(R.string.setting_bandwidth_dialog));
		estimatedNetworkBandwidth.setDefaultValue(Float
				.toString(Constants.ESTIMATED_NETWORK_BANDWIDTH));
		estimatedNetworkBandwidth.getEditText().setKeyListener(
				new DigitsKeyListener());
		dialogBasedPrefCat.addPreference(estimatedNetworkBandwidth);
		
		// Whether to enable upload hacks for strict carriers
		CheckBoxPreference enableUploadHack = new CheckBoxPreference(this);
		enableUploadHack.setKey(Constants.PREFERENCE_UPLOAD_HACK);
		enableUploadHack.setTitle(getString(R.string.setting_upload_hack));
		enableUploadHack.setSummary(getString(R.string.setting_upload_hack_summary));
		enableUploadHack.setDefaultValue(false);
		dialogBasedPrefCat.addPreference(enableUploadHack);
		
        // return the preference screen
		return root;
	}
}

