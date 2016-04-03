package org.moca.events;

import org.moca.net.commands.ICommand;

/**
 * Created by Albert on 4/3/2016.
 */
public class NetworkApiEvent extends BaseEvent {
    private static final String TAG =  NetworkApiEvent.class.getSimpleName();
    public static final String NETWORK_SUCCESS = TAG + ".SUCCESS";
    public static final String NETWORK_FAIL = TAG + ".FAIL";
    public static final String NOT_FOUND = TAG + ".NOT_FOUND";
    public static final String UNAUTHENTICATED = TAG + ".UNAUTHENTICATED";

    private ICommand command;
    private Throwable t;

    public NetworkApiEvent(String type, ICommand command) {
        super(type);
        this.command = command;
    }

    public NetworkApiEvent(String type, Throwable t) {
        super(type);
        this.t = t;
    }

    public ICommand getCommand() {
        return command;
    }

    public Throwable getThrowable() {
        return t;
    }
}
