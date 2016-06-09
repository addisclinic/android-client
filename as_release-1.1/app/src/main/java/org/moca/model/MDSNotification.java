package org.moca.model;

import com.google.gson.annotations.SerializedName;

/**
 * A representation of a notification sent from the MDS. This may be one segment
 * of a multi-part message.
 * 
 * @author Sana Development Team
 *
 */
public class MDSNotification {

    @SerializedName( "mds_id" )
    public String mdsNotificationId;

    @SerializedName( "patient_id" )
    public String patientId;

    @SerializedName( "procedure_id" )
    public String procedureId;

    @SerializedName( "timestamp" )
    public String timestamp;

    public String message;
	
	/** The patient identifier. */ 
	//public String p;
	
	/** This notification's count -- formatted like this: 
	 * <br/>
	 * <code>(?P<this_message>\d+)/(?P<total_messages>\d+)</code> 
	 */
	//public String d;
}
