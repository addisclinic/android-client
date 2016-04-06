package org.moca.net.commands;

import org.moca.net.AddisCallback;
import org.moca.model.MDSNotification;

/**
 * Created by Albert on 4/3/2016.
 */
public class TestApiCommand extends RequestNotificationsCommand {

    public TestApiCommand(AddisCallback<MDSNotification> callback) {
        super(callback, "0");
    }

    @Override
    public void redo() {}
}
