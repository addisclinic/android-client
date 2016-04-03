package org.moca.net;

import com.google.gson.annotations.SerializedName;

import org.moca.model.MDSNotifyPayload;

/**
 * A representation of a notification sent from the MDS. This may be one segment
 * of a multi-part message.
 * 
 * @author Sana Development Team
 *
 */
public class MDSNotification {
    @SerializedName( "status" )
	public String status;

    @SerializedName( "data" )
	public MDSNotifyPayload payload;
	
	/** The patient identifier. */ 
	//public String p;
	
	/** This notification's count -- formatted like this: 
	 * <br/>
	 * <code>(?P<this_message>\d+)/(?P<total_messages>\d+)</code> 
	 */
	//public String d;
}
