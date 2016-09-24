package org.sana.android.net.clients;

import org.sana.android.AddisApp;
import org.sana.android.events.NetworkConnectionEvent;
import org.sana.android.models.NetworkConnectionEnum;
import org.sana.android.net.NetworkGateway;
import org.sana.android.net.commands.ICommand;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Albert on 9/3/2016.
 */
public class BaseNetworkClient {
    private NetworkGateway gateway;
    private NetworkConnectionEnum connectionState;
    private ScheduledFuture<?> pingServer = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public BaseNetworkClient() {
        gateway = new NetworkGateway();
    }

    protected void executeCommand(ICommand command) {
        if (connectionState == NetworkConnectionEnum.DISCONNECTED) {
            notifyNoWiFi();
        }

        gateway.executeCommand(command);
    }

    private void notifyNoWiFi() {
        AddisApp.getInstance().getBus().post(new NetworkConnectionEvent(NetworkConnectionEvent.NETWORK_DISCONNECTED));
    }

    protected void notifyServerUnreachable(int delay) {
        AddisApp.getInstance().getBus().post(new NetworkConnectionEvent(NetworkConnectionEvent.SERVER_UNREACHABLE));
        // kick off a periodically scheduled test command
        if (pingServer == null || pingServer.isDone()) {
            pingServer = scheduler.schedule(testServerReachable, delay, TimeUnit.SECONDS);
        }
    }

    public void networkStateChanged(final NetworkConnectionEnum connectionEnum) {
        connectionState = connectionEnum;
        if (connectionEnum == NetworkConnectionEnum.CONNECTED) {
            AddisApp.getInstance().getBus().post(new NetworkConnectionEvent(NetworkConnectionEvent.NETWORK_CONNECTED));
            gateway.redoCommands();
        }
    }

    private Runnable testServerReachable = new Runnable() {
        @Override
        public void run() {

            /*ICommand command = new TestApiCommand(new JoinCallback<User>() {

                @Override
                public void onResponse(Response<User> response) {
                    // connection to server is stable, hit the redo
                    networkStateChanged(NetworkConnectionEnum.CONNECTED);
                    scheduler.schedule(new Runnable() {
                        @Override
                        public void run() {
                            pingServer.cancel(true);
                        }
                    }, 100, TimeUnit.MILLISECONDS);
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.i(NetworkClient.class.getSimpleName(), "sending test cmd to server.....");
                    // no need to send yet another NetworkApiEvent.NETWORK_FAIL, just keep trying
                    scheduler.schedule(testServerReachable, 25, TimeUnit.SECONDS);
                }
            });
            executeCommand(command);*/
        }
    };
}
