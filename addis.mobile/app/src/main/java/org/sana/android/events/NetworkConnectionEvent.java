package org.sana.android.events;

/**
 * Created by Albert on 9/3/2016.
 */
public class NetworkConnectionEvent extends BaseEvent{
    public static final String NETWORK_CONNECTED = "networkConnectedEvent";
    public static final String NETWORK_DISCONNECTED = "networkDisconnectedEvent";
    public static final String SERVER_UNREACHABLE  = "networkServerUnreachableEvent";
    public static final String INITIALIZATION_FAILED = "INITIALIZATION_FAILED";

    public NetworkConnectionEvent(String type) {
        super(type);
    }
}
