package org.moca.net.api;

import org.moca.model.LoginResult;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Albert on 4/16/2016.
 */
public interface LoginService {

    @POST("mds/json/validate/credentials/")
    Call<LoginResult> login(@Query("username") String username,
                            @Query("password") String password);
}
