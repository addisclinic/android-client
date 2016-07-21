package org.moca.net.commands;

import com.crashlytics.android.Crashlytics;

import org.moca.model.LoginResult;
import org.moca.net.AddisCallback;
import org.moca.net.MDSNetwork;
import org.moca.net.api.LoginService;
import org.moca.util.UserSettings;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
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
        super();
        this.callback = callback;
        this.mdsUser = user.trim();
        this.mdsPassword = password.trim();
    }

    @Override
    public void execute() {
        LoginService service = MDSNetwork.getInstance().getLoginService();
        RequestBody user = RequestBody.create(MediaType.parse("text/plain"), mdsUser);
        RequestBody password = RequestBody.create(MediaType.parse("text/plain"), mdsPassword);
        Call<LoginResult> call = service.login(user, password);
        loginCallback.registerCommand(this);
        call.enqueue(loginCallback);
    }

    private AddisCallback<LoginResult> loginCallback = new AddisCallback<LoginResult>() {
        @Override
        public void onResponse(Call<LoginResult> call, Response<LoginResult> response) {
            super.onResponse(call, response);
            if (response.isSuccessful() && response.body().status.equals("SUCCESS")) {
                UserSettings userSettings = new UserSettings();
                userSettings.setCredentials(mdsUser, mdsPassword);
            }
            if (callback != null) {
                callback.onResponse(call, response);
            }
        }

        @Override
        public void onFailure(Call<LoginResult> call, Throwable t) {
            super.onFailure(call, t);
            if (callback != null) {
                callback.onFailure(call, t);
            }
        }
    };

    public LoginResult executeSynchronous() {
        LoginService service = MDSNetwork.getInstance().getLoginService();
        RequestBody user = RequestBody.create(MediaType.parse("text/plain"), mdsUser);
        RequestBody password = RequestBody.create(MediaType.parse("text/plain"), mdsPassword);

        Call<LoginResult> call = service.login(user, password);
        try {
            Response<LoginResult> result = call.execute();
            if (result.isSuccessful()) {
                UserSettings userSettings = new UserSettings();
                userSettings.setCredentials(mdsUser, mdsPassword);
            }
            return  result.body();

        } catch (IOException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            return null;
        }
    }
}
