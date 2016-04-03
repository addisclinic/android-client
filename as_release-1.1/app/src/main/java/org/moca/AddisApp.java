package org.moca;

import android.app.Application;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.moca.net.clients.NetworkClient;
import org.moca.service.StoreNotifications;
import org.moca.util.UserSettings;

/**
 * Created by Albert on 12/30/2015.
 */
public class AddisApp extends Application {

    private static AddisApp singleton;

    private static  Bus BUS;
    private StoreNotifications networkService;
    private NetworkClient networkClient;

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
        networkService = new StoreNotifications();
        BUS = new Bus(ThreadEnforcer.ANY, AddisApp.class.getSimpleName());
        networkClient = new NetworkClient();
        UserSettings user = new UserSettings();
        user.setDjangoServerCredentials("root", "ark9.SD13");
        user.setPatientId("33333");
    }

    /*public StoreNotifications getNetworkService() {
        return networkService;
    }*/

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public Bus getBus() {
        return BUS;
    }

}
