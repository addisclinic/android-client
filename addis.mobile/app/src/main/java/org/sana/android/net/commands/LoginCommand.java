package org.sana.android.net.commands;

import com.crashlytics.android.Crashlytics;

import org.sana.android.models.LoginResult;
import org.sana.android.net.MDSNetwork;
import org.sana.android.net.NetworkCallback;
import org.sana.android.net.api.LoginService;
import org.sana.android.util.UserSettings;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Albert on 9/3/2016.
 */
public class LoginCommand extends BaseNetworkCommand {
    private NetworkCallback<LoginResult> callback;
    private String mdsUser;
    private String mdsPassword;


    public LoginCommand(NetworkCallback<LoginResult> callback, String user, String password) {
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
        call = service.login(user, password);
        //loginCallback.registerCommand(this);
        call.enqueue(loginCallback);
    }


    private NetworkCallback<LoginResult> loginCallback = new NetworkCallback<LoginResult>() {
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
