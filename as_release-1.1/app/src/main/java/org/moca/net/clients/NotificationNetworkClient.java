package org.moca.net.clients;

import org.moca.net.AddisCallback;
import org.moca.model.MDSNotification;
import org.moca.net.commands.ICommand;
import org.moca.net.commands.RequestNotificationsCommand;

/**
 * Created by Albert on 4/2/2016.
 */
public class NotificationNetworkClient extends BaseNetworkClient {


    public void requestNotifications(String patientId, AddisCallback<MDSNotification> callback) {
        ICommand command = new RequestNotificationsCommand(callback, patientId);
        executeCommand(command);
    }
}
