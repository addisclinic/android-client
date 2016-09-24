package org.sana.android.net;

import com.squareup.otto.Subscribe;

import org.sana.android.AddisApp;
import org.sana.android.events.NetworkApiEvent;
import org.sana.android.net.commands.ICommand;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Albert on 9/3/2016.
 */
public class NetworkGateway {
    private List<ICommand> commands = new CopyOnWriteArrayList<ICommand>();
    private boolean canRedo;

    public NetworkGateway() {
        canRedo = true;
        AddisApp.getInstance().getBus().register(this);
    }

    private void addCommand(ICommand command) {
        synchronized (this) {
            commands.add(command);
        }
    }

    private void removeCommand(ICommand command) {
        synchronized (this) {
            commands.remove(command);
        }
    }

    public void executeCommand(ICommand command) {
        addCommand(command);
        command.execute();
    }

    public void redoCommands() {
        if (canRedo) {
            canRedo = false;

            synchronized (this) {
                for (ICommand command : commands) {
                    command.redo();

                }

                canRedo = true;
            }
        }
    }

    public void clearCommand(Class commandClass) {
        synchronized (this) {
            Iterator<ICommand> iter = commands.iterator();

            for (ICommand command : commands) {
                if (command.getClass() == commandClass) {
                    commands.remove(command);
                }
            }
        }
    }

    @Subscribe
    public void onNetworkSuccess(NetworkApiEvent event) {
        if (NetworkApiEvent.NETWORK_SUCCESS.equals(event.getType())) {
            removeCommand(event.getCommand());
        }
    }
}
