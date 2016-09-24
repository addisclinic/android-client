package org.sana.android.net;

import com.crashlytics.android.Crashlytics;

import org.sana.BuildConfig;
import org.sana.android.AddisApp;
import org.sana.android.events.NetworkApiEvent;
import org.sana.android.net.commands.ICommand;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UnknownFormatConversionException;

import javax.net.ssl.SSLException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Albert on 9/3/2016.
 */
public class NetworkCallback<T> implements Callback<T>  {

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response != null) {
            AddisApp.getInstance().getBus().post(new NetworkApiEvent(NetworkApiEvent.NETWORK_SUCCESS, (ICommand)null));
            if( !response.isSuccessful() ) {
                AddisApp.getInstance().getBus().post(new NetworkApiEvent(NetworkApiEvent.NETWORK_NOT_SUCCESS, null, response.code(), response.message()));
                return;
            }
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (t != null) {
            AddisApp.getInstance().getBus().post(new NetworkApiEvent(getNetworkErrorCode(t), t));

            if( !BuildConfig.DEBUG ) {
                Crashlytics.logException(t);
            }
        }
    }

    private String getNetworkErrorCode(Throwable t) {
        if (t instanceof SocketException || t instanceof UnknownHostException || t instanceof SocketTimeoutException || t instanceof IOException) {
            return NetworkApiEvent.NETWORK_FAIL;
        } else if (t instanceof SSLException) {
            return NetworkApiEvent.NETWORK_SSL_FAIL;
        } else {
            throw new UnknownFormatConversionException("unknown exception: " + t.getClass().getSimpleName() + " : " + t.getMessage());
        }

    }
}
