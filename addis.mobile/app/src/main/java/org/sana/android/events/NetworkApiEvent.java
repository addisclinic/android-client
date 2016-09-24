package org.sana.android.events;

import org.sana.android.net.commands.ICommand;

/**
 * Created by Albert on 9/3/2016.
 */
public class NetworkApiEvent extends BaseEvent {
    private static final String TAG =  NetworkApiEvent.class.getSimpleName();
    public static final String NETWORK_SUCCESS = TAG + ".SUCCESS";
    public static final String NETWORK_NOT_SUCCESS = TAG + ".NOT_SUCCESS";
    public static final String NETWORK_FAIL = TAG + ".FAIL";
    public static final String NOT_FOUND = TAG + ".NOT_FOUND";
    public static final String UNAUTHENTICATED = TAG + ".UNAUTHENTICATED";
    public static final String NETWORK_SSL_FAIL = TAG + ".NETWORK_SSL_FAIL";


    private ICommand command;
    private Throwable t;

    // For NETWORK_NOT_SUCCESS
    private String message;
    private int code;

    public NetworkApiEvent(String type, ICommand command) {
        super(type);
        this.command = command;
    }

    public NetworkApiEvent(String type, Throwable t) {
        super(type);
        this.t = t;
    }

    public NetworkApiEvent(String type, ICommand command, int code, String message) {
        super(type);
        this.command = command;
        this.code = code;
        this.message = message;
    }

    public ICommand getCommand() {
        return command;
    }

    public Throwable getThrowable() {
        return t;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
