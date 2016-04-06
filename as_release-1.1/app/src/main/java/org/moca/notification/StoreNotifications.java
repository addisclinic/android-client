package org.moca.notification;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.moca.AddisApp;
import org.moca.db.MocaDB;
import org.moca.db.NotificationMessage;
import org.moca.net.AddisCallback;
import org.moca.net.MDSInterface;
import org.moca.model.MDSNotification;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Albert on 3/19/2016.
 */
public class StoreNotifications extends AddisCallback<MDSNotification> {

    private static final String TAG = StoreNotifications.class.getSimpleName();
    private Executor networkExecutor;

    public StoreNotifications() {
        networkExecutor = Executors.newFixedThreadPool(getNumCores());
    }

    @Override
    public void onResponse(Call<MDSNotification> call, Response<MDSNotification> response) {
        super.onResponse(call, response);
        Log.i(TAG, response.body().toString());
        if (response.isSuccessful()) {

            processNotificationMessage(response.body());
        }
    }

    private int getNumCores() {
        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    //Check if filename is "cpu", followed by a single digit number
                    return Pattern.matches("cpu[0-9]+", pathname.getName());
                }
            });
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Default to return 1 core
            return 1;
        }
    }

    public void checkNotifications() {
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = MDSInterface.requestNotifications();
                    // TODO: parse header and body before calling processNotificationMessage
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void processNotificationMessage(MDSNotification notification)
    {
        Gson g = new Gson();
        String message = notification.message;
        Context context = AddisApp.getInstance().getApplicationContext();
        String[] query = new String[] { MocaDB.NotificationSQLFormat._ID,
                                        MocaDB.NotificationSQLFormat.PATIENT_ID,
                                        MocaDB.NotificationSQLFormat.PROCEDURE_ID,
                                        MocaDB.NotificationSQLFormat.MESSAGE };
        Cursor c = context.getContentResolver().query(MocaDB.NotificationSQLFormat.CONTENT_URI,
                                                    query,
                                                    MocaDB.NotificationSQLFormat.NOTIFICATION_GUID+"=?",
                                                    new String[] { notification.mdsNotificationId }, null);

        ContentValues cv = new ContentValues();
        String patientId  = notification.patientId;
        if (patientId != null) {
            cv.put(MocaDB.NotificationSQLFormat.PATIENT_ID, patientId);
        }
        String procedureId = notification.procedureId;
        if (procedureId != null) {
            cv.put(MocaDB.NotificationSQLFormat.PROCEDURE_ID, procedureId);
        }
        Uri notificationUri;

        boolean complete = false;
        String fullMessage = "";
        Pattern pattern = Pattern.compile("^(\\d+)/(\\d+)$");
        if (c != null && c.moveToFirst()) {
            // Notification already exists
            int notificationId = c.getInt(c.getColumnIndex(MocaDB.NotificationSQLFormat._ID));
            String storedMessage = c.getString(c.getColumnIndexOrThrow(MocaDB.NotificationSQLFormat.MESSAGE));

            NotificationMessage m = g.fromJson(storedMessage, NotificationMessage.class);

            if (patientId == null) {
                patientId = c.getString(c.getColumnIndex(MocaDB.NotificationSQLFormat.PATIENT_ID));
            }

            c.close();

            storedMessage = g.toJson(m);
            cv.put(MocaDB.NotificationSQLFormat.MESSAGE, storedMessage);

            notificationUri = ContentUris.withAppendedId(
                    MocaDB.NotificationSQLFormat.CONTENT_URI, notificationId);
            int rowsUpdated = context.getContentResolver().update(notificationUri, cv, null, null);
            if (rowsUpdated != 1) {
                Log.e(TAG, "Failed updating notification URI: " + notificationUri);
            }
        } else {
            // Notification is new, create one.
            NotificationMessage m = new NotificationMessage();

                // This is a single message.
                Log.i(TAG, "Received single-part SMS");
                m.totalMessages = 1;
                m.receivedMessages = 1;
                m.messages.put(1, message);
                cv.put(MocaDB.NotificationSQLFormat.FULL_MESSAGE, message);
                cv.put(MocaDB.NotificationSQLFormat.DOWNLOADED, 1);
                complete = true;


            String storedMessage = g.toJson(m);
            cv.put(MocaDB.NotificationSQLFormat.MESSAGE, storedMessage);
            cv.put(MocaDB.NotificationSQLFormat.NOTIFICATION_GUID, notification.mdsNotificationId);
            notificationUri = context.getContentResolver().insert(
                    MocaDB.NotificationSQLFormat.CONTENT_URI, cv);
        }


        if (complete) {
            // Show Toast that a notification was received
            String notifHdr = "DIAGNOSIS RECEIVED\nPatient ID# "
                    + patientId + "\n";
            Toast.makeText(context, notifHdr, Toast.LENGTH_LONG).show();

            Intent viewIntent = new Intent(Intent.ACTION_VIEW, notificationUri);
            new NotificationHelper().show("Patient ID# " + patientId, fullMessage);
        }
    }
}
