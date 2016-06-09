package org.moca.net.clients;

import com.squareup.otto.Subscribe;

import org.moca.AddisApp;
import org.moca.events.NetworkApiEvent;
import org.moca.model.LoginResult;
import org.moca.net.AddisCallback;
import org.moca.net.commands.ICommand;
import org.moca.net.commands.LoginCommand;

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

    public void login(AddisCallback<LoginResult> callback, String user, String password) {
        ICommand command = new LoginCommand(callback, user, password);
        executeCommand(command);
    }

    public void loginSynchronous(AddisCallback<LoginResult> callback, String user, String password) {
        LoginCommand command = new LoginCommand(callback, user, password);
        LoginResult result = command.executeSynchronous();
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
