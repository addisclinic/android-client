package org.sana.android.net.clients;

import com.squareup.otto.Subscribe;

import org.sana.android.AddisApp;
import org.sana.android.events.NetworkApiEvent;
import org.sana.android.models.LoginResult;
import org.sana.android.net.NetworkCallback;
import org.sana.android.net.commands.ICommand;
import org.sana.android.net.commands.LoginCommand;

import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

/**
 * Created by Albert on 9/3/2016.
 */
public class NetworkClient extends BaseNetworkClient {
    public NetworkClient() {
        super();
        AddisApp.getInstance().getBus().register(this);
    }

    public void login(NetworkCallback<LoginResult> callback, String user, String password) {
        ICommand command = new LoginCommand(callback, user, password);
        executeCommand(command);
    }

    public LoginResult loginSynchronous(NetworkCallback<LoginResult> callback, String user, String password) {
        LoginCommand command = new LoginCommand(callback, user, password);
        return command.executeSynchronous();
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
