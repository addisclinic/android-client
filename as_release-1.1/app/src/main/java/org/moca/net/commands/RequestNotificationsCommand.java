package org.moca.net.commands;

import org.moca.net.AddisCallback;
import org.moca.net.MDSNetwork;
import org.moca.model.MDSNotification;
import org.moca.net.api.NotificationService;
import org.moca.util.UserSettings;

import retrofit2.Call;

/**
 * Created by Albert on 4/3/2016.
 */
public class RequestNotificationsCommand extends BaseNetworkCommand {
    private AddisCallback<MDSNotification> callback;
    private String patientId;

    public RequestNotificationsCommand(AddisCallback<MDSNotification> callback, String patientId) {
        this.patientId = patientId;
        this.callback = callback;
        callback.registerCommand(this);
    }

    @Override
    public void execute() {
        NotificationService service = MDSNetwork.getInstance().getNotificationService();
        UserSettings userSettings = new UserSettings();
        String mdsUser = userSettings.getDjangoUsername();
        String mdsPassword = userSettings.getDjangoPassword();
        Call<MDSNotification> call = service.requestNotification(patientId, mdsUser, mdsPassword);
        call.enqueue(callback);
    }
}
