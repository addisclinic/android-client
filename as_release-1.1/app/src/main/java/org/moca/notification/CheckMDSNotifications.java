package org.moca.notification;

import android.os.CountDownTimer;

import org.moca.AddisApp;
import org.moca.net.AddisCallback;
import org.moca.model.MDSNotification;
import org.moca.util.UserSettings;

import retrofit2.Call;
import retrofit2.Response;

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
        String patientId = new UserSettings().getPatientId();
        AddisApp.getInstance().getNetworkClient().requestNotifications(patientId, new AddisCallback<MDSNotification>() {
            @Override
            public void onResponse(Call<MDSNotification> call, Response<MDSNotification> response) {
                super.onResponse(call, response);
            }
        });
    }
}
