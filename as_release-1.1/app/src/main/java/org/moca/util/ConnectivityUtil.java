package org.moca.util;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import org.moca.AddisApp;
import org.moca.model.NetworkConnectionEnum;

/**
 * Created by Albert on 12/26/2015.
 */
public class ConnectivityUtil extends BroadcastReceiver{
    private boolean isConnected = false;
    private boolean bounded = false;

    public  interface NetworkListener {
        public void onNetworkAvailable();
        public void onNetworkUnavailable(final String reason);
    }
    private NetworkListener listener;

    public ConnectivityUtil(NetworkListener listener) {
        super();
        this.listener = listener;
        bind();
    }

    public void bind() {
        if (!bounded) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            filter.setPriority(1000);
            AddisApp.getInstance().registerReceiver(this, filter);
            bounded = true;
        }
        checkIfConnected();
    }

    public void unbind() {
        AddisApp.getInstance().unregisterReceiver(this);
        bounded = false;
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) AddisApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean noConnection = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        if (noConnection) {
            if (isConnected) {
                isConnected = false;
                notifyNetworkStateChange(NetworkConnectionEnum.DISCONNECTED,
                        intent.getStringExtra(ConnectivityManager.EXTRA_REASON));
            }
        } else {
            if (!isConnected) {
                checkIfConnected();
            }
        }
    }


    private void notifyNetworkStateChange(NetworkConnectionEnum state, String reason) {

        if (listener != null) {
            if (state == NetworkConnectionEnum.DISCONNECTED) {
                listener.onNetworkUnavailable(reason);
            } else {
                listener.onNetworkAvailable();
            }
        }
    }

    private void dumpNetworkInfo(Intent intent, String tag) {
        Log.i(tag, "action: " + intent.getAction());
        Log.i(tag, "component: " + intent.getComponent());
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key: extras.keySet()) {
                Log.i(tag, "key [" + key + "]: " + extras.get(key));
            }
        }
        else {
            Log.i(tag, "no extras");
        }
    }

    private void checkIfConnected() {
        ConnectivityManager cm = (ConnectivityManager) AddisApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if ( (activeNetwork != null) && activeNetwork.isConnected() ) {
            if (!isConnected) {
                isConnected = true;
                notifyNetworkStateChange(NetworkConnectionEnum.CONNECTED, null);
            }
        } else {
            if (isConnected) {
                isConnected = false;
                String reason = activeNetwork != null ? activeNetwork.getReason() : "";
                notifyNetworkStateChange(NetworkConnectionEnum.DISCONNECTED, reason);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public NetworkConnectionEnum getNetworkState() {
        checkIfConnected();
        return isConnected ? NetworkConnectionEnum.CONNECTED : NetworkConnectionEnum.DISCONNECTED;
    }
}
