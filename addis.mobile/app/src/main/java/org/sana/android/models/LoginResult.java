package org.sana.android.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Albert on 4/16/2016.
 */
public class LoginResult {
    @SerializedName( "status" )
    public String status;

    @SerializedName( "data" )
    public String data;
}
