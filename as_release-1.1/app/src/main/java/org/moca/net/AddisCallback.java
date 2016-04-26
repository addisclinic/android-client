package org.moca.net;

import android.util.Log;

import org.moca.AddisApp;
import org.moca.events.NetworkApiEvent;
import org.moca.net.commands.ICommand;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Albert on 4/3/2016.
 */
public abstract class AddisCallback<T> implements Callback<T> {
    private ICommand command;

    public AddisCallback() {}
    public AddisCallback(final ICommand command) {
        this.command = command;
    }

    public void registerCommand(final ICommand command) { this.command = command; }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response != null) {
            AddisApp.getInstance().getBus().post(new NetworkApiEvent(NetworkApiEvent.NETWORK_SUCCESS, command));
            if( !response.isSuccessful() ) {
                // TODO: post bus event to Show error message.
                return;
            }
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (t != null) {
            AddisApp.getInstance().getBus().post(new NetworkApiEvent(NetworkApiEvent.NETWORK_FAIL, t));
            if (command != null) {
                Log.e(command.getClass().getSimpleName(), t.getMessage());
            } else {
                Log.e("UNKNOWN CLASS: FIX ME", t.getMessage());
            }

        }
    }
}
