package org.moca.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.moca.R;
import org.moca.db.MocaDB;
import org.moca.db.MocaDB.BinarySQLFormat;
import org.moca.db.MocaDB.ImageSQLFormat;
import org.moca.db.MocaDB.NotificationSQLFormat;
import org.moca.db.MocaDB.PatientSQLFormat;
import org.moca.db.MocaDB.ProcedureSQLFormat;
import org.moca.db.MocaDB.SavedProcedureSQLFormat;
import org.moca.db.MocaDB.SoundSQLFormat;
import org.moca.media.EducationResource;
import org.moca.procedure.Procedure;
import org.moca.procedure.ProcedureParseException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

//TODO
/**
 * Application utilities
 * 
 * @author Sana Development Team
 *
 */
public class MocaUtil {
	public static final String TAG = MocaUtil.class.toString();

    private static final String[] PROJECTION = new String[] { ProcedureSQLFormat._ID,
        ProcedureSQLFormat.TITLE, ProcedureSQLFormat.AUTHOR };
	
    private static final String alphabet = 
    	"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Generates a random string.
     * 
     * @param prefix a set string to prepend
     * @param length the total length of the 
     * @return a new randomized string.
     */
    public static String randomString(String prefix, int length) {
    	return randomString(prefix, length, alphabet);
    }
    
    /**
     * Generates a random string in a specified alphabet.
     * 
     * @param prefix a set string to prepend
     * @param length the total length of the 
     * @param alphabet the set of valid characters
     * @return a new random string
     */
    public static String randomString(String prefix, int length, String alphabet) 
    {
        StringBuilder sb = new StringBuilder(prefix);
        Random r = new Random();
        int alphabetlength = alphabet.length();
        
        for(int i=0; i<length; i++) {           
            sb.append(alphabet.charAt(r.nextInt(alphabetlength-1)));
        }
        
        return sb.toString();
    } 
    
    /**
     * Creates an error message as a dialog.
     * 
     * @param context the current Context
     * @param message the error message
     */
    public static void errorAlert(Context context, String message) {
    	if(context instanceof Activity){
    		if(!((Activity)context).isFinishing())
    			createDialog(context, "Error", message).show();
    	}
    }
    
    /**
     * Creates a message dialog.
     * 
     * @param context the current Context
     * @param title the dialog title
     * @param message the dialog message
     * @return a new dialogf for alerting the user.
     */
    public static AlertDialog createDialog(Context context, String title, 
    		String message) 
    {
        Builder dialogBuilder = new Builder(context);
        dialogBuilder.setPositiveButton(context.getResources().getString(
        		R.string.general_ok), null);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);
        return dialogBuilder.create();
    }
    
    public static AlertDialog okCancelDialog(Context context, String title, 
    		String message, DialogInterface.OnClickListener okCancel) 
    {
        Builder dialogBuilder = new Builder(context);
        dialogBuilder.setPositiveButton(context.getResources().getString(
        		R.string.general_ok), okCancel);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);
        dialogBuilder.setNegativeButton(context.getResources().getString(
        		R.string.general_cancel), okCancel);
        return dialogBuilder.create();
    }
    
    //TODO
    /**
     * Retrieves the value for a xml Node attribute or a default if not found.
     * 
     * @param node The Node to fetch the value from.
     * @param name The attribute name.
     * @param defaultValue The default value to return if not found.
     * @return and attribute value or a default if not found.
     */
    public static String getNodeAttributeOrDefault(Node node, String name, 
    		String defaultValue) 
    {
        NamedNodeMap attributes = node.getAttributes();
        Node valueNode = attributes.getNamedItem(name);
        String value = defaultValue;
        if(valueNode != null) 
            value = valueNode.getNodeValue();
        return value;
    }
    
    //TODO
    /**
     * Retrieves the value for a xml Node attribute or fails if not found.
     * 
     * @param <T> the exception type to throw
     * @param node The Node to fetch the value from.
     * @param name The attribute name.
     * @param e an Exception instance
     * @return the attribute value
     * @throws T
     */
    public static <T extends Exception> String getNodeAttributeOrFail(Node node,
    		String name, T e) throws T 
    {
        NamedNodeMap attributes = node.getAttributes();
        Node valueNode = attributes.getNamedItem(name);
        if(valueNode == null)
            throw e;
        return valueNode.getNodeValue();
    }
    

    /**
     * Utility method for deleting all the elements from a given content URI. 
     * You have to provide the name of the primary key column.
     * 
     * @param ctx the context whose content resolver to use to lookup the URI
     * @param contentUri the content URI to delete all the items from
     * @param idColumn the column of the primary key for the URI
     */
    private static void deleteContentUri(Context ctx, Uri contentUri, 
    		String idColumn) 
    {
    	ctx.getContentResolver().delete(contentUri, null, null);
    }
    
    /**
     * Deletes all stored user content from the database including:
     * <ul>
     * <li>Procedures</li>
     * <li>SavedProcedures</li> 
     * <li>Images</li>
     * <li>Sounds</li>
     * <li>Notifications</li>
     *  </ul>
     * @param ctx the Context where the data is stored
     */
    public static void clearDatabase(Context ctx) {
    	deleteContentUri(ctx, ProcedureSQLFormat.CONTENT_URI, 
    			ProcedureSQLFormat._ID);
    	deleteContentUri(ctx, SavedProcedureSQLFormat.CONTENT_URI, 
    			SavedProcedureSQLFormat._ID);
    	deleteContentUri(ctx, ImageSQLFormat.CONTENT_URI, 
    			ImageSQLFormat._ID);
    	deleteContentUri(ctx, SoundSQLFormat.CONTENT_URI, 
    			SoundSQLFormat._ID);
    	deleteContentUri(ctx, NotificationSQLFormat.CONTENT_URI, 
    			NotificationSQLFormat._ID);
    	deleteContentUri(ctx, BinarySQLFormat.CONTENT_URI, 
    			BinarySQLFormat._ID);
    }
    
    /**
     * Removes all stored patient information
     * @param ctx the Context where the data is stored
     */
    public static void clearPatientData(Context ctx) {
    	deleteContentUri(ctx, PatientSQLFormat.CONTENT_URI, 
    			PatientSQLFormat._ID);
    }
    
    /**
     * Inserts a new procedure into the data store
     * @param ctx the Context where the data is stored
     * @param id the raw resource id
     */
    private static void insertProcedure(Context ctx, int id) {
        
        String title = MocaUtil.randomString("Procedure ", 10);
        String author = "";
        String guid = "";
        String xml;
        try {
            InputStream rs = ctx.getResources().openRawResource(id);
            byte[] data = new byte[rs.available()];
            rs.read(data);
            xml = new String(data);
            
            //Insert "Find Patient" pages in front of every procedure
            //Convert xml to string
            int idFindPatient = R.raw.findpatient;
            InputStream rsFindPatient = ctx.getResources().openRawResource(
            		idFindPatient);
            byte[] dataFindPatient = new byte[rsFindPatient.available()];
            rsFindPatient.read(dataFindPatient);
            String originalXMLFindPatient = new String(dataFindPatient);
            
            //Remove the Procedure XML header/footer from findpatient.xml
            String findPatientHeader = "<Procedure title=\"Find Patient\">";
            String findPatientFooter = "<//Procedure>";
            String xmlFindPatient = "";
            int strLength = originalXMLFindPatient.length();
            xmlFindPatient = originalXMLFindPatient.substring(
            		findPatientHeader.length()+1,
            		strLength-findPatientFooter.length()-1);            
            
            //Modify the procedure xml
            //Insert the "Find Patient" pages after the Procedure tag and before
            // the procedure's pages
            String startProcHeader = "<Procedure title=";
            String xmlFullProcedure = xml;
            if(xml.startsWith(startProcHeader))
            {	
            	int endOfProcedureTag = xml.indexOf(">")+1;
            	//Procedure Tag
            	String xmlHeader = xml.substring(0,endOfProcedureTag);
            	//Rest of Procedure
            	String xmlRestOfProcedure = xml.substring(endOfProcedureTag+1);
            	//New Complete Procedure with Find Patient XML
            	xmlFullProcedure = xmlHeader + xmlFindPatient
            						+ xmlRestOfProcedure;
            }
                        
            Procedure p = Procedure.fromXMLString(xmlFullProcedure);
            title = p.getTitle();
            author = p.getAuthor();
            guid = p.getGuid();
            
            ContentValues cv = new ContentValues();
            cv.put(ProcedureSQLFormat.TITLE, title);
            cv.put(ProcedureSQLFormat.AUTHOR, author);
            cv.put(ProcedureSQLFormat.GUID, guid);
             
            cv.put(ProcedureSQLFormat.PROCEDURE, xmlFullProcedure);
            ctx.getContentResolver().insert(ProcedureSQLFormat.CONTENT_URI, cv);
        } catch(Exception e) {
            Log.e(TAG, "Couldn't add procedure id=" + id + ", title = " + title 
            		+ ", to db. Exception : " + e.toString());
        }
    }
    
    /**
     * Code to insert procedure into database is a duplicate with 
     * insertProcedure this just takes the location from the sd card instead of 
     * an id from the resources.
     * @throws IOException 
     * @throws ProcedureParseException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    public static Integer insertProcedureFromSd(final Context ctx, String location) 
    	throws IOException, ParserConfigurationException, SAXException, 
    	ProcedureParseException
    {
        String title = MocaUtil.randomString("Procedure ", 10);
        String author = "";
        String guid = "";
        String xml;
        	Log.v(TAG, location);
        	
        	FileInputStream rs = new FileInputStream(location);
            byte[] data = new byte[rs.available()];
            rs.read(data);

            xml = new String(data);
        	//Log.v(TAG, xml);
            //Insert "Find Patient" pages in front of every procedure
            //Convert xml to string
            int idFindPatient = R.raw.findpatient;
            InputStream rsFindPatient = ctx.getResources().openRawResource(
            		idFindPatient);
            byte[] dataFindPatient = new byte[rsFindPatient.available()];
            rsFindPatient.read(dataFindPatient);
            String originalXMLFindPatient = new String(dataFindPatient);
            
            //Remove the Procedure XML header/footer from findpatient.xml
            String findPatientHeader = "<Procedure title=\"Find Patient\">";
            String findPatientFooter = "<//Procedure>";
            String xmlFindPatient = "";
            int strLength = originalXMLFindPatient.length();
            xmlFindPatient = originalXMLFindPatient.substring(
            		findPatientHeader.length()+1,
            		strLength-findPatientFooter.length()-1);            
            
            //Modify the procedure xml
            //Insert the "Find Patient" pages after the Procedure tag and before
            // the procedure's pages
            String startProcHeader = "<Procedure title=";
            String xmlFullProcedure = xml;
            if(xml.startsWith(startProcHeader))
            {	
            	int endOfProcedureTag = xml.indexOf(">")+1;
            	//Procedure Tag
            	String xmlHeader = xml.substring(0,endOfProcedureTag);
            	//Rest of Procedure
            	String xmlRestOfProcedure = xml.substring(endOfProcedureTag+1);
            	//New Complete Procedure with Find Patient XML
            	xmlFullProcedure = xmlHeader + xmlFindPatient 
            						+ xmlRestOfProcedure;
            }
                        
            Procedure p = Procedure.fromXMLString(xmlFullProcedure);
            title = p.getTitle();
            author = p.getAuthor();
            guid = p.getGuid();
            
            final ContentValues cv = new ContentValues();
            cv.put(ProcedureSQLFormat.TITLE, title);
            cv.put(ProcedureSQLFormat.AUTHOR, author);
            cv.put(ProcedureSQLFormat.GUID, guid);
            cv.put(ProcedureSQLFormat.PROCEDURE, xmlFullProcedure);

            if (searchDuplicateTitleAuthor(ctx, title, author)){
            	Log.i(TAG, "Duplicate found!");
            	//TODO Versioning
            	ctx.getContentResolver().update(p.getInstanceUri(), 
            			cv, null, null);
            	return 0;
            }else{
            	Log.i(TAG, "Inserting record.");
            	ctx.getContentResolver().insert(
            			ProcedureSQLFormat.CONTENT_URI, cv);
            }
            Log.i(TAG, "Acquired procedure record from local cache.");
		return 0;
    }
    

    private static boolean searchDuplicateTitleAuthor(Context ctx, String title, 
    		String author) 
    {

    	Cursor cursor = null;
    	try {
    		cursor = ctx.getContentResolver().query(
    				ProcedureSQLFormat.CONTENT_URI, PROJECTION, 
    				"(title LIKE\""+title+"\")", null, null);
    		if (cursor.getCount() > 0) {
    			return true;
    		}
    	} catch (Exception e) {
    		
    	} finally {
    		if (cursor != null)
    			cursor.close();
    	}
    	return false;
    }


    /**
     * Loading Sana with XML-described procedures is currently hard-coded. New 
     * files can be added or removed here.
     */
    public static void loadDefaultDatabase(Context ctx) {
    	/*
      	insertProcedure(ctx, R.raw.bronchitis);
      	insertProcedure(ctx, R.raw.cervicalcancer);
      	insertProcedure(ctx, R.raw.surgery_demo);

      	insertProcedure(ctx, R.raw.tbcontact);
      	insertProcedure(ctx, R.raw.multiupload_test); 
    	 */
    	/*
    	insertProcedure(ctx, R.raw.upload_test);
    	insertProcedure(ctx, R.raw.hiv);
    	insertProcedure(ctx, R.raw.cervicalcancer);
    	insertProcedure(ctx, R.raw.prenatal);
    	insertProcedure(ctx, R.raw.surgery);
    	insertProcedure(ctx, R.raw.derma);
    	insertProcedure(ctx, R.raw.teleradiology);
    	insertProcedure(ctx, R.raw.ophthalmology);
    	insertProcedure(ctx, R.raw.tbcontact2);
    	insertProcedure(ctx, R.raw.tbpatient);
    	insertProcedure(ctx, R.raw.cvd_protocol);
    	insertProcedure(ctx, R.raw.oral_cancer);
    	*/
    	//insertProcedure(ctx, R.raw.api_test);
    	insertProcedure(ctx, R.raw.surgery);
    }

    /** 
     * Returns true if the phone has telphony or wifi service
     * @param c - The current context
     * @return true if Android has either a wifi or cellular connection active
     */
	public static boolean checkConnection(Context c) {
		try {
			TelephonyManager telMan = (TelephonyManager) c.getSystemService(
					Context.TELEPHONY_SERVICE);
			WifiManager wifiMan = (WifiManager) c.getSystemService(
					Context.WIFI_SERVICE);
			
			if (telMan != null && wifiMan != null) {
				int dataState = telMan.getDataState();
				if (dataState == TelephonyManager.DATA_CONNECTED || 
						(wifiMan.isWifiEnabled() && wifiMan.pingSupplicant()))
					return true;
			}
			
			return false;
		}
		catch (Exception e) {
			Log.e(TAG, "Exception in checkConnection(): " + e.toString());
			return false;
		}
	}
	
	/**
	 * Utility for creating an returning a dialog with no click listener
	 * @param c The Context the dialog will be created in
	 * @param alertMessage The dialog text
	 * @return a new AlertDialog with no listener
	 */
	public static AlertDialog createAlertMessage(Context c, String alertMessage) 
	{
		return createAlertMessage(c, alertMessage, null);
	}
	
	/**
	 * Utility for creating an returning a dialog with a listener for receiving
	 * click value.
	 * @param c The Context the dialog will be created in
	 * @param alertMessage The dialog text
	 * @param listener A listener for receiving click events, may be <b>null</b>
	 * @return a new AlertDialog with a specified listener
	 */
	public static AlertDialog createAlertMessage(Context c, String alertMessage, 
			DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
		builder.setMessage(alertMessage).setCancelable(false)
				.setPositiveButton(
					c.getResources().getString(R.string.general_ok), listener);
		AlertDialog alert = builder.create();
		alert.show();
		return alert;
	}
	
	/**
	 * Format a list of primary keys into a SQLite-formatted list of ids. 
	 * 
	 * Ex 1,2,3 is formatted as (1,2,3)
	 */
	public static String formatPrimaryKeyList(List<Long> idList) {
		StringBuilder sb = new StringBuilder("(");
		Iterator<Long> it = idList.iterator();
		while (it.hasNext()) {
			sb.append(Long.toString(it.next()));
			if (it.hasNext()) {
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Convenience wrapper around Log.d to print a debug string as:
	 * 
	 * <code>onActivityResult: requestCode = <b>value</b>, resultCode = <b>value</b></code>
	 *  
	 * @param tag THe calling classes tag
	 * @param requestCode the request code used when launching the Activity
	 * @param resultCode the result code returned by the Activity
	 */
	public static void logActivityResult(String tag, int requestCode, 
			int resultCode)
	{
		Log.d(tag, "onActivityResult: requestCode = " +requestCode
				+ ", resultCode = " + resultCode);
	}
}
