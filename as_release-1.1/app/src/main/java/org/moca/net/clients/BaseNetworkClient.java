package org.moca.net.clients;

import android.util.Log;

import com.squareup.otto.Bus;

import org.moca.AddisApp;
import org.moca.events.NetworkConnectionEvent;
import org.moca.model.NetworkConnectionEnum;
import org.moca.net.AddisCallback;
import org.moca.net.MDSNotification;
import org.moca.net.NetworkGateway;
import org.moca.net.commands.ICommand;
import org.moca.net.commands.TestApiCommand;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Albert on 4/3/2016.
 */
public class BaseNetworkClient {
    private NetworkGateway gateway;
    private NetworkConnectionEnum connectionState;
    private ScheduledFuture<?> pingServer = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Bus bus;

    public BaseNetworkClient() {
        gateway = new NetworkGateway();
        bus = AddisApp.getInstance().getBus();
    }

    protected void executeCommand(ICommand command) {
        if (connectionState == NetworkConnectionEnum.DISCONNECTED) {
            notifyNoWiFi();
        }

        gateway.executeCommand(command);
    }

    private void notifyNoWiFi() {
        bus.post(new NetworkConnectionEvent(NetworkConnectionEvent.NETWORK_DISCONNECTED));
    }

    protected void notifyServerUnreachable(int delay) {
        bus.post(new NetworkConnectionEvent(NetworkConnectionEvent.SERVER_UNREACHABLE));
        // kick off a periodically scheduled test command
        if (pingServer == null || pingServer.isDone()) {
            pingServer = scheduler.schedule(testServerReachable, delay, TimeUnit.SECONDS);
        }
    }

    public void networkStateChanged(final NetworkConnectionEnum connectionEnum) {
        connectionState = connectionEnum;
        if (connectionEnum == NetworkConnectionEnum.CONNECTED) {
            bus.post(new NetworkConnectionEvent(NetworkConnectionEvent.NETWORK_CONNECTED));
            gateway.redoCommands();
        }
    }

    private Runnable testServerReachable = new Runnable() {
        @Override
        public void run() {

            ICommand command = new TestApiCommand(new AddisCallback<MDSNotification>() {

                @Override
                public void onResponse(Call<MDSNotification> call, Response<MDSNotification> response) {
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
                public void onFailure(Call<MDSNotification> call, Throwable t) {
                    Log.i(NetworkClient.class.getSimpleName(), "sending test cmd to server.....");
                    // no need to send yet another NetworkApiEvent.NETWORK_FAIL, just keep trying
                    scheduler.schedule(testServerReachable, 25, TimeUnit.SECONDS);
                }
            });
            executeCommand(command);
        }
    };
}
