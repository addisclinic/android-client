package org.sana.android.net.commands;

import org.sana.android.models.LoginResult;

import retrofit2.Call;

/**
 * Created by Albert on 9/3/2016.
 */
public abstract class BaseNetworkCommand implements ICommand {
    protected Call<LoginResult> call;

    @Override
    public void redo() {
        execute();
    }

    @Override
    public void cancel() {
        if (call != null) {
            call.cancel();
        }
    }
}
