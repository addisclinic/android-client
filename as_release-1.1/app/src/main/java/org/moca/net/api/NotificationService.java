package org.moca.net.api;

import org.moca.model.MDSNotification;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Albert on 4/2/2016.
 */
public interface NotificationService {

    @POST("mds/notifications/")
    Call<MDSNotification> requestNotification(@Query("patient_id") String patientId,
                                           @Query("username") String username,
                                           @Query("password") String password);
}
