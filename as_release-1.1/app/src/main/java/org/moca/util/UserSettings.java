package org.moca.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.moca.AddisApp;
import org.moca.Constants;

/**
 * Created by Albert on 3/19/2016.
 */
public class UserSettings {

    private static final String TAG = UserSettings.class.getSimpleName();
    private String APP_PREFS_FILE = TAG + ".APP_PREFS_FILE";
    private SharedPreferences settings;

    public String getPatientId() {
        return settings.contains(UserPrefKey.PATIENT_ID_KEY.get()) ? getUserStringPref(UserPrefKey.PATIENT_ID_KEY) : null;
    }

    public void setPatientId(String patientId) {
        setUserPref(UserPrefKey.PATIENT_ID_KEY, patientId);
    }

    private enum UserPrefKey {
        FIRST_TIME_KEY              ( TAG + ".FIRST_TIME_KEY"           ),
        USERNAME_KEY                ( Constants.PREFERENCE_EMR_USERNAME ),
        PASSWORD_KEY                ( Constants.PREFERENCE_EMR_PASSWORD ),
        DJANGO_USERNAME_KEY         ( TAG + ".DJANGO_USERNAME_KEY"),
        DJANGO_PASSWORD_KEY         ( TAG + ".DJANGO_PASSWORD_KEY"),
        PATIENT_ID_KEY              ( TAG + ".PATIENT_ID_KEY"),
        PREFERENCE_MDS_URL          ( Constants.PREFERENCE_MDS_URL),
        PREFERENCE_SECURE_TRANSMISSION (Constants.PREFERENCE_SECURE_TRANSMISSION);

        private String key;

        UserPrefKey(String key) {
            this.key = key;
        }

        public String get() {
            return key;
        }
    }

    private UserSettings(Context ctxt) {
        Context mApplicationContext = ctxt.getApplicationContext();
        //settings = mApplicationContext.getSharedPreferences(APP_PREFS_FILE, Context.MODE_PRIVATE);
        settings = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
    }

    public UserSettings() {
        this(AddisApp.getInstance().getApplicationContext());
    }

    private void remove(UserPrefKey key) {
        settings.edit().remove(key.get()).apply();
    }

    private void setUserPref(UserPrefKey key, String pref) {
        settings.edit().putString(key.get(), pref).apply();
    }

    private void setUserPref(UserPrefKey key, boolean pref) {
        settings.edit().putBoolean(key.get(), pref).apply();
    }

    private void setUserPref(UserPrefKey key, int pref) {
        settings.edit().putInt(key.get(), pref).apply();
    }

    private String getUserStringPref(UserPrefKey key) {
        return settings.getString(key.get(), "");
    }

    private int getUserIntPref(UserPrefKey key) {
        return settings.getInt(key.get(), -1);
    }

    private boolean getUserBoolPref(UserPrefKey key) {
        return settings.getBoolean(key.get(), false);
    }


    public void setCredentials (String userName, String password) {
        setUserPref(UserPrefKey.USERNAME_KEY, userName);
        setUserPref(UserPrefKey.PASSWORD_KEY, password);
    }

    public void clearCredentials() {
        remove(UserPrefKey.USERNAME_KEY);
        remove(UserPrefKey.PASSWORD_KEY);
    }

    public void setDjangoServerCredentials(String userName, String password) {
        setUserPref(UserPrefKey.DJANGO_USERNAME_KEY, userName);
        setUserPref(UserPrefKey.DJANGO_PASSWORD_KEY, password);
    }
    
    public String getDjangoUsername () {
        return settings.contains(UserPrefKey.DJANGO_USERNAME_KEY.get()) ? getUserStringPref(UserPrefKey.DJANGO_USERNAME_KEY) : null;
    }

    public String getDjangoPassword () {
        return settings.contains(UserPrefKey.DJANGO_PASSWORD_KEY.get()) ? getUserStringPref(UserPrefKey.DJANGO_PASSWORD_KEY) : null;
    }

    public String getUsername () {
        return settings.contains(UserPrefKey.USERNAME_KEY.get()) ? getUserStringPref(UserPrefKey.USERNAME_KEY) : null;
    }

    public String getPassword () {
        return settings.contains(UserPrefKey.PASSWORD_KEY.get()) ? getUserStringPref(UserPrefKey.PASSWORD_KEY) : null;
    }

    public String getHostname () {
        return settings.contains(UserPrefKey.PREFERENCE_MDS_URL.get()) ? getUserStringPref(UserPrefKey.PREFERENCE_MDS_URL) : Constants.DEFAULT_DISPATCH_SERVER;
    }

    public String getMDSUrl() {
        String host = settings.contains(UserPrefKey.PREFERENCE_MDS_URL.get()) ? getUserStringPref(UserPrefKey.PREFERENCE_MDS_URL) : Constants.DEFAULT_DISPATCH_SERVER;

        // Takes care of legacy issues
        host.replace("moca.mit.edu", "demo.sanamobile.org");
        boolean useSecure = getUserBoolPref(UserPrefKey.PREFERENCE_SECURE_TRANSMISSION);
        String scheme = (useSecure)? "https": "http";
        String url = scheme + "://" + host;
        return url +"/"; //+ Constants.PATH_MDS + "/";
    }

    public void setSecureTransmission() {
        setUserPref(UserPrefKey.PREFERENCE_SECURE_TRANSMISSION, true);
    }
}
