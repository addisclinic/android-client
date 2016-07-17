package org.sana.android;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Albert on 7/16/2016.
 */
public class AddisApp extends Application {

    private static AddisApp singleton;

    private static  Bus BUS;
    //private StoreNotifications networkService;
    //private NetworkClient networkClient;

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
        Fabric.with(this, new Crashlytics());
        //networkService = new StoreNotifications();
        BUS = new Bus(ThreadEnforcer.ANY, AddisApp.class.getSimpleName());
        //networkClient = new NetworkClient();
        //UserSettings user = new UserSettings();
        //user.setDjangoServerCredentials("root", "ark9.SD13");
        //user.setPatientId("33333");
    }

    /*public StoreNotifications getNetworkService() {
        return networkService;
    }*/

    /*public NetworkClient getNetworkClient() {
        return networkClient;
    }
*/
    public Bus getBus() {
        return BUS;
    }

}
