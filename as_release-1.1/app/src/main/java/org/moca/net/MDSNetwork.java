package org.moca.net;

import android.content.Context;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.moca.AddisApp;
import org.moca.R;
import org.moca.net.api.LoginService;
import org.moca.net.api.NotificationService;
import org.moca.util.UserSettings;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Albert on 4/2/2016.
 *
 */
public class MDSNetwork {

    private static final String TAG = MDSNetwork.class.getSimpleName();
    private static final int[] sslCertificateArray = {R.raw.mds_dev, R.raw.mds_prod};
    private static MDSNetwork singleton;
    public  Gson gson = new GsonBuilder().create();

    private  Retrofit retrofit;
    private  Retrofit authRetrofit;
    private  OkHttpClient.Builder httpClient;

    private  Retrofit.Builder builder;

    private  NotificationService userService;
    private  LoginService loginService;

    public static MDSNetwork getInstance() {
        if (singleton == null) {
            singleton = new MDSNetwork();
        }
        return singleton;
    }

    private  MDSNetwork() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(interceptor);

        try {
            httpClient.sslSocketFactory(getSocketFactory());
            httpClient.hostnameVerifier(hostnameVerifier);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

        Gson gson = gsonBuilder.create();

        builder = new Retrofit.Builder()
                .baseUrl(new UserSettings().getMDSUrl())
                .addConverterFactory(GsonConverterFactory.create(gson));
    }

    private HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            if (new UserSettings().getHostname().equalsIgnoreCase(hostname)) {
                return true;
            }
            Log.e(TAG, "invalid host name: " + hostname);
            return false;
        }
    };

    private SSLSocketFactory getSocketFactory() throws GeneralSecurityException, IOException {
        Context context = AddisApp.getInstance().getApplicationContext();
        // creating a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);

        for (int i=0; i < sslCertificateArray.length; i++) {
            // loading CAs from an InputStream
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream cert = context.getResources().openRawResource(sslCertificateArray[i]);
            Certificate ca;
            try {
                ca = cf.generateCertificate(cert);
            } finally {
                cert.close();
            }

            keyStore.setCertificateEntry("ca" + i, ca);
        }
        // creating a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // creating an SSLSocketFactory that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        return sslContext.getSocketFactory();
    }

    public  <S> S createService(Class<S> serviceClass) {
        TokenAuthenticator.resetRetryCount();
        if (retrofit == null)
            retrofit = builder.client(httpClient.build()).build();
        return retrofit.create(serviceClass);
    }

    public  <S> S createServiceWithAuth(Class<S> serviceClass) {
        TokenAuthenticator.setServiceName(serviceClass.getSimpleName());
        TokenAuthenticator.resetRetryCount();
        if (authRetrofit == null) {
            httpClient.authenticator(new TokenAuthenticator());

            OkHttpClient client = httpClient.build();
            authRetrofit = builder.client(client).build();
        }
        return authRetrofit.create(serviceClass);
    }

    public NotificationService getNotificationService() {
        if (userService == null)
            userService = createServiceWithAuth(NotificationService.class);

        TokenAuthenticator.resetRetryCount();
        return userService;
    }

    public LoginService getLoginService() {
        if (loginService == null)
            loginService = createServiceWithAuth(LoginService.class);

        TokenAuthenticator.resetRetryCount();
        return loginService;
    }
}
