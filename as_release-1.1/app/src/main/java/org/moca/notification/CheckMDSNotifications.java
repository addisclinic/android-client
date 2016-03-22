package org.moca.notification;

import android.os.CountDownTimer;

import org.moca.AddisApp;

/**
 * Created by Albert on 3/19/2016.
 */
public class CheckMDSNotifications extends CountDownTimer{

    public CheckMDSNotifications(long millisInFuture) {
        super(millisInFuture, 1000);
    }

    @Override
    public void onTick(long millisUntilFinished) {

    }

    @Override
    public void onFinish() {
        doCheck();
        start();
    }

    public void doCheck() {
        AddisApp.getInstance().getNetworkService().checkNotifications();
    }
}
