package org.moca;

import android.app.Application;

import org.moca.service.NetworkService;

/**
 * Created by Albert on 12/30/2015.
 */
public class AddisApp extends Application {

    private static AddisApp singleton;
    private NetworkService networkService;

    public static AddisApp getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        singleton = this;
        networkService = new NetworkService();
    }

    public NetworkService getNetworkService() {
        return networkService;
    }
}
