package org.moca.net.clients;

import com.squareup.otto.Subscribe;

import org.moca.AddisApp;
import org.moca.events.NetworkApiEvent;

import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

/**
 * Created by Albert on 4/2/2016.
 */
public class NetworkClient extends NotificationNetworkClient {
    public NetworkClient() {
        super();
        AddisApp.getInstance().getBus().register(this);
    }


    @Subscribe
    public void onNetworkFail(NetworkApiEvent event) {
        if (NetworkApiEvent.NETWORK_FAIL.equals(event.getType())) {
            //  handle SocketTimeouts, NoHostExceptions, etc. when WiFi state=NetworkConnectionEnum.CONNECTED
            Throwable e = event.getThrowable();
            //Log.i("NetworkClient", "Class: " + e.getClass().getSimpleName());
            if (e instanceof NoRouteToHostException || e instanceof UnknownHostException) {
                notifyServerUnreachable(15);
            }
            if (e instanceof SocketException) {
                if (e.getMessage().contains("ETIMEDOUT") || e.getMessage().contains("ENETUNREACH")) {
                    notifyServerUnreachable(14);
                }
            } else if (e instanceof SSLException) {
                if (e.getMessage().contains("Connection timed out")) {
                    notifyServerUnreachable(4);
                }
            }
        }
    }
}