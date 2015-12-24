package org.moca.net;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.moca.Constants;
import org.moca.db.Event;
import org.moca.db.MocaDB.BinarySQLFormat;
import org.moca.db.MocaDB.ImageSQLFormat;
import org.moca.db.MocaDB.ProcedureSQLFormat;
import org.moca.db.MocaDB.SavedProcedureSQLFormat;
import org.moca.db.MocaDB.SoundSQLFormat;
import org.moca.net.ssl.SimpleSSLProtocolSocketFactory;
import org.moca.procedure.Procedure;
import org.moca.procedure.ProcedureElement;
import org.moca.procedure.ProcedureParseException;
import org.moca.procedure.ProcedureElement.ElementType;
import org.moca.util.MocaUtil;
import org.moca.util.UserDatabase;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Interface for uploading to the Sana Mobile Dispatch Server(MDS).
 * <br/>
 * This is where all of the packetization and http posting occurs.  Other than
 * some database interactions, it is fairly independent of Android.
 * <br/>
 * The process for uploading a procedure is as follows (item number two takes
 * place on a remote server, the other steps take place in the code in this
 * source file):
 * <br/>
 * <ol>
 * <li>
 * Post question/response pairs from completed procedure via http, tagging 
 * it with procedure, patient, and phone IDs.</li>
 * <li>
 * Moca Dispatch Server (MDS) parses the questions to see if they include 
 * any binary elements (i.e. a page in the procedure that asks to take a 
 * picture). If there are pending binary uploads, MDS knows to expect them and 
 * does not send the completed upload to OpenMRS until all parts are received.
 * </li>
 * <li>
 * For each binary element, Moca uploads chunks of the element to the MDS. The 
 * size of these chunks starts at a default size. Each chunk is tagged with a 
 * procedure, patient, and phone ID as well as an element identifier and the 
 * start and end byte numbers (corresponding to the chunk location).
 * </li>
 * <li>
 * If the first chunk successfully uploads, the chunk size for the next chunk
 * transmission doubles. If the post fails, the chunk size halves.
 * </li>
 * <li> 
 * If the chunk size falls below a default "give up" threshold, the procedure
 * is tagged as not- finished-uploading, and Moca waits to transmit the rest
 * of the completed procedure at a later time. If the entire binary element
 * is successfully transmitted, it moves on to the next element.
 * </li>
 * <li>
 * It repeats steps 3-5 for subsequent elements, but instead of starting at the 
 * default chunk size for each transmission, it now has knowledge about the 
 * connection quality and uses the last successful transmission size from the 
 * last binary element as a starting point.
 * </li>
 * </ol>
 *    
 * @author Sana Dev Team
 */
public class MDSInterface {
	public static final String TAG = MDSInterface.class.getSimpleName();

	public static String[] savedProcedureProjection = new String[] {
		SavedProcedureSQLFormat._ID, 
		SavedProcedureSQLFormat.PROCEDURE_ID,
		SavedProcedureSQLFormat.PROCEDURE_STATE,
		SavedProcedureSQLFormat.FINISHED,
		SavedProcedureSQLFormat.GUID, 
		SavedProcedureSQLFormat.UPLOADED };

	/**
	 * Http request url for validating MRS credentials
	 * @param mdsURL host url
	 * @return the url as a string
	 */
	private static String constructValidateCredentialsURL(String mdsURL) {
		return mdsURL + Constants.VALIDATE_CREDENTIALS_PATTERN;
	}

	/**
	 * Http request url for submitting procedures
	 * @param mdsURL host url
	 * @return the url as a string
	 */
	private static String constructProcedureSubmitURL(String mdsURL) {
		return mdsURL + Constants.PROCEDURE_SUBMIT_PATTERN;
	}

	/**
	 * Http request url for submitting binary chunks
	 * @param mdsURL host url
	 * @return the url as a string
	 */
	private static String constructBinaryChunkSubmitURL(String mdsURL) {
		return mdsURL + Constants.BINARYCHUNK_SUBMIT_PATTERN;
	}

	/**
	 * Http request url for submitting binary chunks as base64 encoded text
	 * @param mdsURL host url
	 * @return the url as a string
	 */
	private static String constructBinaryChunkHackSubmitURL(String mdsURL) {
		return mdsURL + Constants.BINARYCHUNK_HACK_SUBMIT_PATTERN;
	}

	/**
	 * Http request url for getting updated patient data(all patients)
	 * @param mdsURL host url
	 * @return the url as a string
	 */
	private static String constructDatabaseDownloadURL(String mdsURL) {
		return mdsURL + Constants.DATABASE_DOWNLOAD_PATTERN;
	}


	/**
	 * Http request url for requesting patient info
	 * @param mdsURL host url
	 * @return the url as a string
	 */
	private static String constructUserInfoURL(String mdsURL, String id) {
		return mdsURL + Constants.USERINFO_DOWNLOAD_PATTERN + id + "/";
	}
	
	/**
	 * Http request url for submitting events
	 * @param mdsURL host url
	 * @return the url as a string
	 */
	private static String constructEventLogUrl(String mdsUrl) {
		return mdsUrl + Constants.EVENTLOG_SUBMIT_PATTERN;
	}

	/**
	 * Handles legacy url requests
	 * @param mdsUrl
	 * @return
	 */
	private static String checkMDSUrl(String mdsUrl) {
		if ("http://moca.media.mit.edu/mds".equals(mdsUrl)) {
			return "http://demo.sanamobile.org/mds";
		}
		return mdsUrl;
	}
	
	/**
	 * Gets the value in the MDS url setting and add the correct scheme, i.e.
	 * http or https, depending on the value of the use secure transmission 
	 * setting.
	 * @param ctx The application context.
	 * @return The mds url with correct scheme.
	 */
	private static String getMDSUrl(SharedPreferences preferences){
		String host = preferences.getString(Constants.PREFERENCE_MDS_URL,
											Constants.DEFAULT_DISPATCH_SERVER);
		// Takes care of legacy issues
		host.replace("moca.mit.edu", "demo.sanamobile.org");
		boolean useSecure = preferences.getBoolean(
				Constants.PREFERENCE_SECURE_TRANSMISSION, false);
		String scheme = (useSecure)? "https": "http";
		String url = scheme + "://" + host;
		return url +"/"+ Constants.PATH_MDS;
	}

	/**
	 * Executes a POST method. Provides a wrapper around doExecute by 
	 * preparing the PostMethod.
	 * 
	 * @param ctx the current Context
	 * @param mUrl the request url
	 * @param postData the form data.
	 * @return 
	 */
	protected static MDSResult doPost(Context ctx, String mUrl, 
			List<NameValuePair> postData) 
	{
		PostMethod post = new PostMethod(mUrl);
		Log.d(TAG, "doPost(): " + mUrl + ", " + postData.size());
		for(NameValuePair pair: postData){
			post.addParameter(pair);
		}
		return MDSInterface.doExecute(ctx, post);
	}
	
	/**
	 * Executes a POST method. Provides a wrapper around doExecute by 
	 * preparing the PostMethod.
	 * 
	 * @param ctx the current Context
	 * @param mUrl the request url
	 * @param parts the form data.
	 * @return 
	 */
	protected static MDSResult doPost(Context ctx, String mUrl, Part[] parts) 
	{
		PostMethod post = new PostMethod(mUrl);
		post.setRequestEntity(new MultipartRequestEntity(parts,
				post.getParams()));
		return MDSInterface.doExecute(ctx, post);
	}
	
	/**
	 * Executes a client HttpMethod.
	 * 
	 * @param ctx The context which the method will be executed in
	 * @param method The Http
	 * @return
	 */
	protected static MDSResult doExecute(Context ctx, HttpMethod method){
		HttpClient client = new HttpClient();
		SharedPreferences preferences = 
			PreferenceManager.getDefaultSharedPreferences(ctx);
		MDSResult response = null;
		// If there's a proxy enabled, use it.
		String proxyHost = preferences.getString(
				Constants.PREFERENCE_PROXY_HOST, "");
		String sProxyPort = preferences.getString(
				Constants.PREFERENCE_PROXY_PORT, "0");
		boolean useSecure = preferences.getBoolean(
				Constants.PREFERENCE_SECURE_TRANSMISSION, false);
		
		int proxyPort = 0;
		try {
			if (!"".equals(sProxyPort)) 
				proxyPort = Integer.parseInt(sProxyPort);
		} catch(NumberFormatException e) {
			Log.w(TAG, "Invalid proxy port: " + sProxyPort);
		}
		if (!"".equals(proxyHost) && proxyPort != 0) {
			Log.i(TAG, "Setting proxy to " + proxyHost + ":" + proxyPort);
			HostConfiguration hc = new HostConfiguration();
			hc.setProxy(proxyHost, (int)proxyPort);
			client.setHostConfiguration(hc);
		}
		// execute the Http/https method
		try {
			if(useSecure){
				ProtocolSocketFactory ssl = new SimpleSSLProtocolSocketFactory();
				Protocol https = new Protocol("https", ssl, 443);
				Protocol.registerProtocol("https", https);
			}
			int status = client.executeMethod(method); 
			Log.d(TAG, "postResponses got response code " +  status);

			char buf[] = new char[20560];
			Reader reader = new InputStreamReader(
					method.getResponseBodyAsStream());
			int total = reader.read(buf, 0, 20560);
			String responseString = new String(buf);
			Log.d(TAG, "Received from MDS:" + responseString.length()+" chars");
			Gson gson = new Gson();
			response = gson.fromJson(responseString, MDSResult.class);

		} catch (IOException e1) {
			Log.e(TAG, e1.toString());
			e1.printStackTrace();
		} catch (JsonParseException e) {
			Log.e(TAG, "postResponses(): Error parsing MDS JSON response: " 
					+ e.getMessage());
		} finally {
			method.releaseConnection();
		}
		return response;
	}

	/**
	 * Posts the text responses from a procedure to the Mobile Dispatch Server
	 * <br/>
	 * We don't packetize the raw text responses since, generally speaking, the 
	 * total transmission size will be fairly small (probably less than the 
	 * default starting packet size).
	 * 
	 * @param savedProcedureGuid the encounter unique identifier
	 * @param responses the encounter text
	 * @return true if upload succeeds, otherwise false
	 */
	private static boolean postResponses(Context c, String savedProcedureGuid, 
			String jsonResponses)
	{
		SharedPreferences preferences = PreferenceManager
											.getDefaultSharedPreferences(c);
		String mdsURL = getMDSUrl(preferences);
		mdsURL = checkMDSUrl(mdsURL);
		String mUrl = constructProcedureSubmitURL(mdsURL);
		String phoneId = preferences.getString("s_phone_name", 
				Constants.PHONE_ID);
		String username = preferences.getString(
				Constants.PREFERENCE_EMR_USERNAME, Constants.DEFAULT_USERNAME);
		String password = preferences.getString(
				Constants.PREFERENCE_EMR_PASSWORD, Constants.DEFAULT_PASSWORD);
		List<NameValuePair> postData = new ArrayList<NameValuePair>();
		postData.add(new NameValuePair("savedproc_guid", savedProcedureGuid));
		postData.add(new NameValuePair("procedure_guid", Integer.toString(0)));
		postData.add(new NameValuePair("phone", phoneId));
		postData.add(new NameValuePair("username", username));
		postData.add(new NameValuePair("password", password));
		postData.add(new NameValuePair("responses", jsonResponses));
		MDSResult postResponse = MDSInterface.doPost(c, mUrl, postData);
		return (postResponse != null)? postResponse.succeeded(): false;
		
	}

	/**
	 * Executes an Http POST call with a binary chunk as base64 encoded text 
	 * 
	 * @param c the current context
	 * @param savedProcedureId The encounter id
	 * @param elementId the observation id
	 * @param fileGuid the unique id of the file
	 * @param type the observation type
	 * @param fileSize total byte count
	 * @param start offset from 0 of this chunk
	 * @param end offset + size
	 * @param byte_data the binary chunk that is being sent
	 * @return true if successful
	 */
	private static boolean postBinaryAsEncodedText(Context c, 
			String savedProcedureId, String elementId, String fileGuid, 
			ElementType type, int fileSize, int start, int end, 
			byte byte_data[]) 
	{

		SharedPreferences preferences = PreferenceManager
											.getDefaultSharedPreferences(c);
		String mdsURL = getMDSUrl(preferences);
		mdsURL = checkMDSUrl(mdsURL);
		String mUrl = constructBinaryChunkHackSubmitURL(mdsURL);
		
		List<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new NameValuePair("procedure_guid", savedProcedureId));
		post.add(new NameValuePair("element_id", elementId));
		post.add(new NameValuePair("binary_guid", fileGuid));
		post.add(new NameValuePair("element_type", type.toString()));
		post.add(new NameValuePair("file_size", Integer.toString(fileSize)));
		post.add(new NameValuePair("byte_start", Integer.toString(start)));
		post.add(new NameValuePair("byte_end", Integer.toString(end)));

		// Encode byte_data in Base64
		byte[] encoded_data = new Base64().encode(byte_data);
		post.add(new NameValuePair("byte_data", new String(encoded_data)));
		
		//execute
		MDSResult postResponse = MDSInterface.doPost(c, mUrl, post);
		return (postResponse != null)? postResponse.succeeded(): false;
	}

	/**
	 * A chunk of byte data and filename
	 * 
	 * @author Sana Development Team
	 */
	private static class BytePartSource implements PartSource {
		private String filename;
		private byte[] data;

		public BytePartSource(byte[] data, String filename) {
			this.data = data;
			this.filename = filename;
		}

		@Override
		public InputStream createInputStream() throws IOException {
			return new ByteArrayInputStream(data);
		}
		
		@Override
		public String getFileName() {
			return filename;
		}

		public long getLength() {
			return data.length;
		}

	}
	
	/**
	 * Executes an Http POST call with a binary chunk as a file 
	 * 
	 * @param c the current context
	 * @param savedProcedureId The encounter id
	 * @param elementId the observation id
	 * @param fileGuid the unique id of the file
	 * @param type the observation type
	 * @param fileSize total byte count
	 * @param start offset from 0 of this chunk
	 * @param end offset + size
	 * @param byte_data the binary chunk that is being sent
	 * @return true if successful
	 */
	private static boolean postBinaryAsFile(Context c, String savedProcedureId, 
			String elementId, String fileGuid, ElementType type, int fileSize, 
			int start, int end, byte byte_data[]) 
	{
		SharedPreferences preferences = 
			PreferenceManager.getDefaultSharedPreferences(c);
		String mdsUrl = getMDSUrl(preferences);
		mdsUrl = checkMDSUrl(mdsUrl);
		String mUrl = constructBinaryChunkSubmitURL(mdsUrl);

		Part[] parts = {
				new StringPart("procedure_guid", savedProcedureId),
				new StringPart("element_id", elementId),
				new StringPart("binary_guid", fileGuid),
				new StringPart("element_type", type.toString()),
				new StringPart("file_size", Integer.toString(fileSize)),
				new StringPart("byte_start", Integer.toString(start)),
				new StringPart("byte_end", Integer.toString(end)),
				new FilePart("byte_data", new BytePartSource(byte_data, 
											type.getFilename())),
		};
		
		//execute
		MDSResult postResponse = MDSInterface.doPost(c, mUrl, parts);
		return (postResponse != null)? postResponse.succeeded(): false;
	}

	/**
	 * Posts a single chunk of a binary file.
	 * 
	 * @param c current context
	 * @param savedProcedureId The encounter id
	 * @param elementId The observation within the encounter
	 * @param type binary type (ie picture, sound, etc.)
	 * @param start first byte index in binary file (since this presumably is a 
	 * 			chunk of a larger file)
	 * @param end last byte index in binary file of the chunk being uploaded
	 * @param byte_data a byte array containing the file chunk data
	 * @return true on successful upload, otherwise false
	 */
	private static boolean postBinary(Context c, String savedProcedureId, 
			String elementId, String fileGuid, ElementType type, int fileSize, 
			int start, int end, byte byte_data[]) 
	{
		SharedPreferences preferences = PreferenceManager
											.getDefaultSharedPreferences(c);
		boolean hacksMode = preferences.getBoolean(
				Constants.PREFERENCE_UPLOAD_HACK, false);
		// check if we want to post as base64 encoded text
		if(hacksMode) {
			return postBinaryAsEncodedText(c, savedProcedureId, elementId, 
					fileGuid, type, fileSize, start, end, byte_data);
		} else {
			return postBinaryAsFile(c, savedProcedureId, elementId, fileGuid, 
					type, fileSize, start, end, byte_data);
		}
	}
	
	/**
	 * Checks whether an encounter is already uploaded
	 * 
	 * @param uri The saved procedure uri
	 * @param context current context
	 * @return
	 */
	public static boolean isProcedureAlreadyUploaded(Uri uri, Context context) {
		Cursor cursor = context.getContentResolver().query(uri, 
							savedProcedureProjection, null, null, null);
		// First get the saved procedure...
		cursor.moveToFirst();
		int procedureId = cursor.getInt(1);
		String answersJson = cursor.getString(2);
		boolean savedProcedureUploaded = cursor.getInt(5) != 0;
		cursor.close();

		Uri procedureUri = ContentUris.withAppendedId(
							ProcedureSQLFormat.CONTENT_URI, procedureId);
		Log.i(TAG, "Getting procedure " + procedureUri.toString());
		cursor = context.getContentResolver().query(procedureUri, 
				new String[] { ProcedureSQLFormat.PROCEDURE }, null,null,null);
		cursor.moveToFirst();
		String procedureXml = cursor.getString(0);
		cursor.close();
		
		if (!savedProcedureUploaded) return false;

		Map<String, Map<String,String>> elementMap = null;
		try {
			Procedure p = Procedure.fromXMLString(procedureXml);
			p.setInstanceUri(uri);

			JSONTokener tokener = new JSONTokener(answersJson);
			JSONObject answersDict = new JSONObject(tokener);

			Map<String,String> answersMap = new HashMap<String,String>();
			Iterator<?> it = answersDict.keys();
			while(it.hasNext()) {
				String key = (String)it.next();
				answersMap.put(key, answersDict.getString(key));
				Log.i(TAG, "onCreate() : answer '" + key + "' : '" 
							+ answersDict.getString(key) +"'");
			}
			Log.i(TAG, "onCreate() : restoreAnswers");
			p.restoreAnswers(answersMap);
			elementMap = p.toElementMap();

		} catch (IOException e2) {
			Log.e(TAG, e2.toString());
		} catch (ParserConfigurationException e2) {
			Log.e(TAG, e2.toString());
		} catch (SAXException e2) {
			Log.e(TAG, e2.toString());
		} catch (ProcedureParseException e2) {
			Log.e(TAG, e2.toString());
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		}

		if(elementMap == null) {
			Log.i(TAG, "Empty answers from " + uri + ". Not uploading.");
			return false;
		}

		class ElementAnswer {
			public String answer;
			public String type;
			public ElementAnswer(String id, String answer, String type) {
				this.answer = answer;
				this.type = type;
			}
		}

		int totalBinaries = 0;
		List<ElementAnswer> binaries = new ArrayList<ElementAnswer>();
		for(Entry<String,Map<String,String>> e : elementMap.entrySet()) {
			
			String id = e.getKey();
			String type = e.getValue().get("type");
			String answer = e.getValue().get("answer");

			// Find elements that require binary uploads
			if(type.equals(ElementType.PICTURE.toString()) ||
					type.equals(ElementType.BINARYFILE.toString()) ||
					type.equals(ElementType.SOUND.toString()) ||
					type.equals(ElementType.PLUGIN.toString())) {
				binaries.add(new ElementAnswer(id, answer, type));
				if(!"".equals(answer)) {
					String[] ids = answer.split(",");
					totalBinaries += ids.length;
				}
			}
		}
		// upload each binary file
		for(ElementAnswer e : binaries) {
			if("".equals(e.answer))
				continue;

			String[] ids = e.answer.split(",");
			for(String binaryId : ids) {
				Uri binUri = null;
				ElementType type = ElementType.INVALID;
				try {
					type = ElementType.valueOf(e.type);
				} catch(IllegalArgumentException ex) {
				}
				if (type == ElementType.PICTURE) {
					binUri = ContentUris.withAppendedId(
							ImageSQLFormat.CONTENT_URI, 
							Long.parseLong(binaryId));	
				} else if (type == ElementType.SOUND) {
					binUri = ContentUris.withAppendedId(
							SoundSQLFormat.CONTENT_URI, 
							Long.parseLong(binaryId));
				} else if (type == ElementType.PLUGIN) {
					binUri = ContentUris.withAppendedId(
							BinarySQLFormat.CONTENT_URI, 
							Long.parseLong(binaryId));
				} else if (type == ElementType.BINARYFILE) {
					binUri = Uri.fromFile(new File(e.answer));
					// We can't tell if a BINARYFILE has been uploaded before.
					// Maybe if we grab the mtime/filesize on the file and store
					// it when we upload it.
				}

				try {
					Log.i(TAG, "Checking if " + binUri + " has been uploaded");
					// reset the new packet size each time to the last 
					// successful transmission size
					boolean alreadyUploaded = false;
					Cursor cur = null;
					switch(type) {
					case PICTURE:
						cur = context.getContentResolver().query(binUri, 
								new String[] { ImageSQLFormat.UPLOADED }, null, 
								null, null);
						cur.moveToFirst();
						alreadyUploaded = cur.getInt(0) != 0;
						if (!alreadyUploaded) return false;
						if (cur != null)
							cur.close();
						break;
					case SOUND:
						cur = context.getContentResolver().query(binUri, 
								new String[] { SoundSQLFormat.UPLOADED }, null, 
								null, null);
						cur.moveToFirst();
						alreadyUploaded = cur.getInt(0) != 0;
						if (!alreadyUploaded) return false;
						cur.deactivate();
						break;

					case PLUGIN:
						cur = context.getContentResolver().query(binUri, 
								new String[] { BinarySQLFormat.UPLOADED }, null, 
								null, null);
						cur.moveToFirst();
						alreadyUploaded = cur.getInt(0) != 0;
						if (!alreadyUploaded) return false;
						cur.deactivate();
						break;
					case BINARYFILE:
					default:
						// Can't do anything since its not in the DB. Sigh.
						break;
					}
				} catch (Exception x) {
					Log.i(TAG, "Error checking if the binary files have been "
							+"uploaded: " + x.toString());
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Send the entire completed procedure to the Sana Mobile Dispatch Server 
	 * (MDS). This procedure sends the answer/response pairs and all the binary 
	 * data (sounds, pictures, etc.) to the MDS in a packetized fashion.
	 * 
	 * @param uri uri of procedure in database
	 * @param context current context
	 * @return true if upload was successful, false if not
	 */
	public static boolean postProcedureToDjangoServer(Uri uri, Context context) {
		Log.i(TAG, "In Post procedure to Django server for background uploading service.");
		Cursor cursor = context.getContentResolver().query(
				uri, savedProcedureProjection, null,
				null, null);
		// First get the saved procedure...
		cursor.moveToFirst();
		int savedProcedureId = cursor.getInt(0);
		int procedureId = cursor.getInt(1);
		String answersJson = cursor.getString(2);
		boolean finished = cursor.getInt(3) != 0;
		String savedProcedureGUID = cursor.getString(4);
		boolean savedProcedureUploaded = cursor.getInt(5) != 0;
		cursor.close();

		Uri procedureUri = ContentUris.withAppendedId(
				ProcedureSQLFormat.CONTENT_URI, procedureId);
		Log.i(TAG, "Getting procedure " + procedureUri.toString());
		cursor = context.getContentResolver().query(procedureUri, 
				new String[] { ProcedureSQLFormat.TITLE, 
							   ProcedureSQLFormat.PROCEDURE },
				null, null, null);
		cursor.moveToFirst();
		String procedureTitle = cursor.getString(
				cursor.getColumnIndex(ProcedureSQLFormat.TITLE));
		String procedureXml = cursor.getString(
				cursor.getColumnIndex(ProcedureSQLFormat.PROCEDURE));
		cursor.close();
		
		if(!finished) {
			Log.i(TAG, "Not finished. Not uploading. (just kidding)" 
					+ uri.toString());
			//return false;
		}
		// Map of all of the Procedure Elements; i.e. observation data
		// binaries get parsed out later
		Map<String, Map<String,String>> elementMap = null;
		try {
			Procedure p = Procedure.fromXMLString(procedureXml);
			p.setInstanceUri(uri);

			JSONTokener tokener = new JSONTokener(answersJson);
			JSONObject answersDict = new JSONObject(tokener);

			Map<String,String> answersMap = new HashMap<String,String>();
			Iterator<?> it = answersDict.keys();
			while(it.hasNext()) {
				String key = (String)it.next();
				answersMap.put(key, answersDict.getString(key));
				Log.i(TAG, "onCreate() : answer '" + key + "' : '" 
						+ answersDict.getString(key) +"'");
			}
			Log.i(TAG, "onCreate() : restoreAnswers");
			p.restoreAnswers(answersMap);
			elementMap = p.toElementMap();

		} catch (IOException e2) {
			Log.e(TAG, e2.toString());
		} catch (ParserConfigurationException e2) {
			Log.e(TAG, e2.toString());
		} catch (SAXException e2) {
			Log.e(TAG, e2.toString());
		} catch (ProcedureParseException e2) {
			Log.e(TAG, e2.toString());
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		}
		// check that we don't have empty map
		if(elementMap == null) {
			Log.i(TAG, "Could not encounter text " + uri + ". Not uploading.");
			return false;
		}
		
		// Add in procedureTitle as a fake answer
		Map<String,String> titleMap = new HashMap<String,String>();
		titleMap.put("answer", procedureTitle);
		titleMap.put("id", "procedureTitle");
		titleMap.put("type", "HIDDEN");
		elementMap.put("procedureTitle", titleMap);
		
		// We need a String -> String map to convert to JSON
		Map<String,String> enrolledMap = new HashMap<String,String>();
		enrolledMap.put("answer", "Yes");
		enrolledMap.put("id", "patientEnrolled");
		enrolledMap.put("type", ProcedureElement.ElementType.RADIO.toString());
		enrolledMap.put("question", "Does the patient already have an ID card?");
		elementMap.put("patientEnrolled", enrolledMap);
		
		// Utility wrapper for answers
		class ElementAnswer {
			public String id;
			public String answer;
			public String type;
			public ElementAnswer(String id, String answer, String type) {
				this.id = id;
				this.answer = answer;
				this.type = type;
			}
		}
		// Convert saved procedure to JSON 
		JSONObject jsono = new JSONObject();
		int totalBinaries = 0;
		ArrayList<ElementAnswer> binaries = new ArrayList<ElementAnswer>();
		for(Entry<String,Map<String,String>> e : elementMap.entrySet()) {
			try {
				jsono.put(e.getKey(), new JSONObject(e.getValue()));
			} catch (JSONException e1) {
				Log.e(TAG, "JSON conversion fail: " + e1.getMessage());
			}

			String id = e.getKey();
			String type = e.getValue().get("type");
			String answer = e.getValue().get("answer");
			
			if (id == null || type == null || answer == null)
				continue;

			// Find elements that require binary uploads
			if(type.equals(ElementType.PICTURE.toString()) ||
					type.equals(ElementType.BINARYFILE.toString()) ||
					type.equals(ElementType.SOUND.toString()) ||
					type.equals(ElementType.PLUGIN.toString())){
				binaries.add(new ElementAnswer(id, answer, type));
				if(!"".equals(answer)) {
					String[] ids = answer.split(",");
					totalBinaries += ids.length;
				}
			}
		}

		Log.i(TAG, "About to post responses.");
		// check if it is already uploaded
		if(savedProcedureUploaded) {
			Log.i(TAG, "Responses have already been sent to MDS, not posting.");
		} else {
			// upload the question and answer pairs text, without packetization
			String json = jsono.toString();
			Log.i(TAG, "json string: " + json);
			
			// try repeating upload on fail to some preset number
			int tries = 0;
			final int MAX_TRIES = 5;
			while(tries < MAX_TRIES) {
				if (postResponses(context, savedProcedureGUID, json)) {
					// Mark the procedure text as uploaded in the database
					ContentValues cv = new ContentValues();
					cv.put(SavedProcedureSQLFormat.UPLOADED, true);
					context.getContentResolver().update(uri, cv, null, null);
					Log.i(TAG, "Responses were uploaded successfully.");
					break;
				}
				tries++;
			}
			// if tries >= maximum we bail and try again later
			if(tries == MAX_TRIES) {
				Log.e(TAG, "Could not post responses, bailing.");
				return false;
			}

		}
		Log.i(TAG, "Posted responses, now sending " + totalBinaries 
				+ " binaries.");
		// lookup starting packet size
		int newPacketSize;
		try {
			newPacketSize = Integer.parseInt(
					PreferenceManager.getDefaultSharedPreferences(context)
						.getString("s_packet_init_size", 
						Integer.toString(Constants.DEFAULT_INIT_PACKET_SIZE)));
		} catch (NumberFormatException e) {
			newPacketSize = Constants.DEFAULT_INIT_PACKET_SIZE;
		}
		// adjust from KB to bytes
		newPacketSize *= 1000;

		int totalProgress = 1+totalBinaries;
		int thisProgress = 2;
		// upload each binary file where each binary should be represented by 
		// one value in a comma separated list of ints starting
		for(ElementAnswer e : binaries) {
			if(TextUtils.isEmpty(e.answer))
				continue;
			// parse csv list
			String[] ids = e.answer.split(",");
			// loop over each value
			for(String binaryId : ids) {
				Uri binUri = null;
				ElementType type = ElementType.INVALID;
				try {
					type = ElementType.valueOf(e.type);
				} catch(IllegalArgumentException ex) {
					Log.e(TAG, ex.getMessage());
				}
				
				if (type == ElementType.PICTURE) {
					binUri = ContentUris.withAppendedId(
								ImageSQLFormat.CONTENT_URI, 
								Long.parseLong(binaryId));	
				} else if (type == ElementType.SOUND) {
					binUri = ContentUris.withAppendedId(
								SoundSQLFormat.CONTENT_URI, 
								Long.parseLong(binaryId));
				} else if (type == ElementType.PLUGIN) {
					binUri = ContentUris.withAppendedId(
							BinarySQLFormat.CONTENT_URI, 
							Long.parseLong(binaryId));
				} else if (type == ElementType.BINARYFILE) {
					binUri = Uri.fromFile(new File(e.answer));
					// We can't tell if a BINARYFILE has been uploaded before.
					// Maybe if we grab the mtime/filesize on the file and store
					// it when we upload it.
				}

				try {
					Log.i(TAG, "Uploading " + binUri);
					// reset the new packet size each time to the last 
					// successful transmission size
					newPacketSize = transmitBinary(context, savedProcedureGUID, 
												   e.id, binaryId, type, binUri,
												   newPacketSize);
					// Delete the file!
					switch(type) {
					case PICTURE:
					case SOUND:
						//This was deleting the pictures after upload - should 
						// not happen, leave commented out!
						//context.getContentResolver().delete(binUri,null,null);
						break;
					default:
					}
				} catch (Exception x) {
					Log.i(TAG, "Uploading " + binUri + " failed : " 
							+ x.toString());
					return false;
				}
				thisProgress++;
			}
		}
		// TODO Tag entire procedure in db as done transmitting
		return true;   
	}
	
	/**
	 * Sends an entire binary file in a packetized fashion. This method is where
	 * the automatic ramping packetization takes place. Uploading occurs in the 
	 * background
	 * 
	 * @param c current Context
	 * @param savedProcedureId the unique identifier of the procedure within
	 * 			the phone domain
	 * @param elementId the id attribute of the Element within a Procedure
	 * @param type binary type (ie picture, sound, etc.)
	 * @param binaryUri uri of the file to be transmitted
	 * @param startPacketSize the starting packet size for each chunk; this will
	 * 		  be throttled up or down depending on connection strength
	 * @return the last successful chunk transmission size on success so that it
	 * 		  can be used for future transmissions as the startPacketSize
	 * @throws Exception on upload failure
	 */
	private static int transmitBinary(Context c, String savedProcedureId, 
			String elementId, String binaryGuid, ElementType type, 
			Uri binaryUri, int startPacketSize) throws Exception 
	{
		int packetSize, fileSize;
		ContentValues cv = new ContentValues();
		packetSize = startPacketSize;
		boolean alreadyUploaded = false;
		int currPosition = 0;
		Cursor cur = null;
		switch(type) {
		case PICTURE:
			cur = c.getContentResolver().query(binaryUri, new String[] { 
					ImageSQLFormat.UPLOADED, ImageSQLFormat.UPLOAD_PROGRESS }, 
					null, null, null);
			cur.moveToFirst();
			alreadyUploaded = cur.getInt(0) != 0;
			currPosition = cur.getInt(1);
			break;
		case SOUND:
			cur = c.getContentResolver().query(binaryUri, new String[] { 
					SoundSQLFormat.UPLOADED, SoundSQLFormat.UPLOAD_PROGRESS }, 
					null, null, null);
			cur.moveToFirst();
			alreadyUploaded = cur.getInt(0) != 0;
			currPosition = cur.getInt(1);
			break;
		case PLUGIN:
			//TODO Add the BinaryProvider
			cur = c.getContentResolver().query(binaryUri, new String[] { 
					BinarySQLFormat.UPLOADED, BinarySQLFormat.UPLOAD_PROGRESS }, 
					null, null, null);
			cur.moveToFirst();
			alreadyUploaded = cur.getInt(0) != 0;
			currPosition = cur.getInt(1);
			break;
		case BINARYFILE:
		default:
			// Can't do anything since its not in the DB. Sigh.
			break;

		}
		if (cur != null){
			cur.close();
		}
		if(alreadyUploaded) {
			Log.i(TAG, binaryUri + " was already uploaded. Skipping.");
			return startPacketSize;
		}
		// Ope the input stream
		InputStream is = c.getContentResolver().openInputStream(binaryUri);
		fileSize = is.available();

		// Skip forward by the progress we've made previously.
		is.skip(currPosition);
		int progress = (int)(100.0 * currPosition / fileSize);

		int bytesRemaining = fileSize - currPosition;
		Log.i(TAG, "transmitBinary uploading " + binaryUri + " " 
				+ bytesRemaining + " total bytes remaining. Starting at " 
				+ packetSize + " packet size");

		// reference packet rate byte/msec
		double basePacketRate = 0.0;
		while(bytesRemaining > 0) {
			// get starting time of packet transmission
			long transmitStartTime = new Date().getTime();
			// if transmission rate is acceptable 
			// (comparison between currPacketRate and basePacketRate)
			boolean efficient = false;

			int bytesToRead = Math.min(packetSize, bytesRemaining);
			byte[] chunk = new byte[bytesToRead];
			int bytesRead = is.read(chunk, 0, bytesToRead);

			boolean success = false;
			while(!success) {
				Log.i(TAG, "Trying to upload " + bytesRead + " bytes for " 
						+ savedProcedureId + ":" + elementId + ".");
				success = postBinary(c, savedProcedureId, elementId, binaryGuid,
										type, fileSize, currPosition, 
										currPosition+bytesRead, chunk);
				efficient = false;
				// new rate is compared to 80% of previous rate
				basePacketRate *= 0.8;
				if(success) {
					long transmitEndTime = new Date().getTime();
					// get new packet rate
					double currPacketRate = (double)packetSize/
								(double)(transmitEndTime-transmitStartTime);
					Log.i(TAG, "packet rate = (current) " + currPacketRate 
								+ ", (base) " + basePacketRate);
					if(currPacketRate > basePacketRate) {
						basePacketRate = currPacketRate;
						efficient = true;
					}
				}
				// update packet size
				if(efficient) {
					packetSize *= 2;
					Log.i(TAG, "Shifting packet size *2 =" + packetSize);
				} else {
					packetSize /= 2;
					Log.i(TAG, "Shifting packet size /2 =" + packetSize);
				}
				// close if packet size becomes too small
				if(packetSize < Constants.MIN_PACKET_SIZE * 1000) {
					// TODO(rryan) : fail at some point
					is.close();
					throw new IOException("Could not upload " + binaryUri 
							+". failed after " + (fileSize-bytesRemaining) 
							+ " bytes.");
				}
			}
			// update progress
			bytesRemaining -= bytesRead;
			currPosition += bytesRead;
			progress = (int)(100.0 * currPosition / fileSize);

			// write current progress to database
			cv.clear();
			switch(type) {
			case PICTURE:
				cv.put(ImageSQLFormat.UPLOAD_PROGRESS, currPosition);
				c.getContentResolver().update(binaryUri, cv, null, null);
				break;
			case SOUND:
				cv.put(SoundSQLFormat.UPLOAD_PROGRESS, currPosition);
				c.getContentResolver().update(binaryUri, cv, null, null);
				break;
			}
		}

		// Mark file as uploaded in the database
		cv.clear();
		switch(type) {
		case PICTURE:
			cv.put(ImageSQLFormat.UPLOADED, true);
			c.getContentResolver().update(binaryUri, cv, null, null);
			break;
		case SOUND:
			cv.put(SoundSQLFormat.UPLOADED, true);
			c.getContentResolver().update(binaryUri, cv, null, null);
			break;
		}
		is.close();
		return packetSize;
	}

	/**
	 * Validates authorization credentials with permanent record store.
	 * 
	 * @param c the current Context
	 * @return true if the dispatch server reports success
	 * @throws IOException
	 */
	public static boolean validateCredentials(Context c) throws IOException {
		Log.i(TAG, "validateCredentials()");
		SharedPreferences preferences = PreferenceManager
											.getDefaultSharedPreferences(c);
		String mdsURL = getMDSUrl(preferences);
		mdsURL = checkMDSUrl(mdsURL);
		String mUrl = constructValidateCredentialsURL(mdsURL);
		String username = preferences.getString(
				Constants.PREFERENCE_EMR_USERNAME, Constants.DEFAULT_USERNAME);
		String password = preferences.getString(
				Constants.PREFERENCE_EMR_PASSWORD, Constants.DEFAULT_PASSWORD);
		
		List<NameValuePair> postData = new ArrayList<NameValuePair>();
		postData.add(new NameValuePair("username", username));
		postData.add(new NameValuePair("password", password));
		MDSResult postResponse = MDSInterface.doPost(c, mUrl, postData);
		boolean result = (postResponse != null)? postResponse.succeeded():false;
		Log.i(TAG, "MDS reports " + (result ? "success" : "failure")
				+ " for credentials");
		return result;
	}


	/**
	 * Sync patient database on phone with permanent record store.
	 * 
	 * @param c the current Context
	 * @return true if successfully updated
	 */
	public static boolean updatePatientDatabase(Context c, ContentResolver cr) {
		Log.i(TAG, "updatePatientDatabase():");
		SharedPreferences preferences = PreferenceManager
											.getDefaultSharedPreferences(c);
		String mdsURL = getMDSUrl(preferences);
		mdsURL = checkMDSUrl(mdsURL);
		String mUrl = constructDatabaseDownloadURL(mdsURL);
		String username = preferences.getString(
				Constants.PREFERENCE_EMR_USERNAME, Constants.DEFAULT_USERNAME);
		String password = preferences.getString(
				Constants.PREFERENCE_EMR_PASSWORD, Constants.DEFAULT_PASSWORD);
		
		List<NameValuePair> postData = new ArrayList<NameValuePair>();
		postData.add(new NameValuePair("username", username));
		postData.add(new NameValuePair("password", password));
		MDSResult postResponse = MDSInterface.doPost(c, mUrl, postData);
		boolean result = (postResponse != null)? postResponse.succeeded():false;
		try{
			if (result){
				String toparse = postResponse.getData();
				MocaUtil.clearPatientData(c);
		
				//the following line needs to be uncommented eventually
				UserDatabase.addDataToUsers(cr, toparse);
			}
		} catch (Exception e){
			result = false;
			Log.e(TAG, "updatePatientDatabase(): " + e.getMessage());
		}
		return result;
	}

	/**
	 * Gets patient database from MRS
	 * 
	 * @param c the current Context
	 * @return The string representation of a patient
	 */
	public static String getUserInfo(Context c, String userid) {
	
		Log.i(TAG, "getUserInfo(): " + userid);
		SharedPreferences preferences = PreferenceManager
											.getDefaultSharedPreferences(c);
		String mdsURL = getMDSUrl(preferences);
		mdsURL = checkMDSUrl(mdsURL);
		String mUrl = constructUserInfoURL(mdsURL,userid);
		String username = preferences.getString(
				Constants.PREFERENCE_EMR_USERNAME, Constants.DEFAULT_USERNAME);
		String password = preferences.getString(
				Constants.PREFERENCE_EMR_PASSWORD, Constants.DEFAULT_PASSWORD);
		
		List<NameValuePair> postData = new ArrayList<NameValuePair>();
		postData.add(new NameValuePair("username", username));
		postData.add(new NameValuePair("password", password));
		MDSResult postResponse = MDSInterface.doPost(c, mUrl, postData);
		String result = (postResponse != null)? postResponse.getData(): null;
		Log.i(TAG, "getUserInfo(): Patient data: " + result);
		return result;
	}
	

	/**
	 * Checks whether patient exists in the permanent record store.
	 * 
	 * @param c the application Context
	 * @param userid the id to verify
	 * @return true if the id is not in use
	 */
	public static boolean isNewPatientIdValid(Context c, String userid) {
		Log.i(TAG, "isNewPatientValid(): " + userid);
		String data = MDSInterface.getUserInfo(c, userid);

		if (!TextUtils.isEmpty(data)) {
			Log.i(TAG, "isNewPatientValid(): Id already in use.");
			return false;
		}
		Log.i(TAG, "isNewPatientValid(): Id is not in use and is valid");
		return true;
	}
	
	
	/**
	 * Sends a list of events to the dispatch server
	 * 
	 * @param c the application Context
	 * @param eventsList a list of events
	 * @return true if successfully sent
	 */
	public static boolean submitEvents(Context c, List<Event> eventsList) {
		Log.i(TAG, "submitEvents(): " + eventsList.size());

		SharedPreferences preferences = PreferenceManager
											.getDefaultSharedPreferences(c);
		String mdsURL = getMDSUrl(preferences);
		mdsURL = checkMDSUrl(mdsURL);
		String mUrl = constructEventLogUrl(mdsURL);
		String phoneId = preferences.getString("s_phone_name", 
				Constants.PHONE_ID);
		String username = preferences.getString(
				Constants.PREFERENCE_EMR_USERNAME, Constants.DEFAULT_USERNAME);
		String password = preferences.getString(
				Constants.PREFERENCE_EMR_PASSWORD, Constants.DEFAULT_PASSWORD);
		
		List<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new NameValuePair("username", username));
		post.add(new NameValuePair("password", password));
		post.add(new NameValuePair("client_id", phoneId));
		Gson g = new Gson();
		post.add(new NameValuePair("events", g.toJson(eventsList)));
		MDSResult postResponse = MDSInterface.doPost(c, mUrl, post);
		return (postResponse != null)? postResponse.succeeded(): false;
	}
}
