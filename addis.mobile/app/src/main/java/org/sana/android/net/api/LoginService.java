package org.sana.android.net.api;

import org.sana.android.models.LoginResult;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by Albert on 4/16/2016.
 */
public interface LoginService {
    @Multipart
    @POST("mds/json/validate/credentials/")
    Call<LoginResult> login(@Part("username") RequestBody username,
                            @Part("password") RequestBody password);
}
