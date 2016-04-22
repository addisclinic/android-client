package org.moca.net.commands;

import org.moca.model.LoginResult;
import org.moca.net.AddisCallback;
import org.moca.net.MDSNetwork;
import org.moca.net.api.LoginService;
import org.moca.util.UserSettings;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Albert on 4/16/2016.
 */
public class LoginCommand extends BaseNetworkCommand {
    private AddisCallback<LoginResult> callback;
    private String mdsUser;
    private String mdsPassword;

    public LoginCommand(AddisCallback<LoginResult> callback, String user, String password) {
        this.callback = callback;
        this.mdsUser = user;
        this.mdsPassword = password;
    }

    @Override
    public void execute() {
        LoginService service = MDSNetwork.getInstance().getLoginService();
        UserSettings userSettings = new UserSettings();
        //String mdsUser = userSettings.getUsername();
        //String mdsPassword = userSettings.getPassword();
        Call<LoginResult> call = service.login(mdsUser, mdsPassword);
        call.enqueue(loginCallback);
    }

    private AddisCallback<LoginResult> loginCallback = new AddisCallback<LoginResult>() {
        @Override
        public void onResponse(Call<LoginResult> call, Response<LoginResult> response) {
            super.onResponse(call, response);
            if (response.isSuccessful()) {
                UserSettings userSettings = new UserSettings();
                userSettings.setCredentials(mdsUser, mdsPassword);
                callback.onResponse(call, response);
            }
        }
    };

    public LoginResult executeSynchronous() {
        LoginService service = MDSNetwork.getInstance().getLoginService();

        //String mdsUser = userSettings.getUsername();
        //String mdsPassword = userSettings.getPassword();
        Call<LoginResult> call = service.login(mdsUser, mdsPassword);
        try {
            Response<LoginResult> result = call.execute();
            if (result.isSuccessful()) {
                UserSettings userSettings = new UserSettings();
                userSettings.setCredentials(mdsUser, mdsPassword);
            }
            return  result.body();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
