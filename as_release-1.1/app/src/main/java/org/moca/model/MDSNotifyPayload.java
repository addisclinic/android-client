package org.moca.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Albert on 4/3/2016.
 */
public class MDSNotifyPayload {
    @SerializedName( "patient_id" )
    public String patientId;

    @SerializedName( "procedure_id" )
    public String procedureId;

    public String message;
}
