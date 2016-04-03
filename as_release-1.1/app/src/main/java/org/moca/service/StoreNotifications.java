package org.moca.service;

import org.moca.net.MDSInterface;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Created by Albert on 3/19/2016.
 */
public class StoreNotifications {

    private static final String TAG = StoreNotifications.class.getSimpleName();
    private Executor networkExecutor;

    public StoreNotifications() {
        networkExecutor = Executors.newFixedThreadPool(getNumCores());
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

    /*private void processNotificationMessage(Context context,
                                    MDSNotification notificationHeader, String message)
    {
        Gson g = new Gson();

        if (notificationHeader.n == null) {
            Log.e(TAG, "Received mal-formed notification GUID -- none provided.");
        }

        Cursor c = context.getContentResolver().query(MocaDB.NotificationSQLFormat.CONTENT_URI,
                new String[] { MocaDB.NotificationSQLFormat._ID,
                        MocaDB.NotificationSQLFormat.PATIENT_ID,
                        MocaDB.NotificationSQLFormat.PROCEDURE_ID,
                        MocaDB.NotificationSQLFormat.MESSAGE },
                MocaDB.NotificationSQLFormat.NOTIFICATION_GUID+"=?",
                new String[] { notificationHeader.n }, null);

        ContentValues cv = new ContentValues();
        String patientId = null;
        if (notificationHeader.p != null) {
            patientId = notificationHeader.p;
            cv.put(MocaDB.NotificationSQLFormat.PATIENT_ID, notificationHeader.p);
        }
        if (notificationHeader.c != null) {
            cv.put(MocaDB.NotificationSQLFormat.PROCEDURE_ID, notificationHeader.c);
        }
        Uri notificationUri;

        boolean complete = false;
        String fullMessage = "";
        Pattern pattern = Pattern.compile("^(\\d+)/(\\d+)$");
        if (c.moveToFirst()) {
            // Notification already exists
            int notificationId = c.getInt(c.getColumnIndex(
                    MocaDB.NotificationSQLFormat._ID));
            String storedMessage = c.getString(c.getColumnIndexOrThrow(
                    MocaDB.NotificationSQLFormat.MESSAGE));

            NotificationMessage m = g.fromJson(storedMessage,
                    NotificationMessage.class);

            if (patientId == null) {
                patientId = c.getString(c.getColumnIndex(
                        MocaDB.NotificationSQLFormat.PATIENT_ID));
            }

            if (notificationHeader.d != null) {
                Matcher matcher = pattern.matcher(notificationHeader.d);
                if (matcher.matches()) {
                    Integer current = Integer.parseInt(matcher.group(1));
                    Integer total = Integer.parseInt(matcher.group(2));
                    m.receivedMessages++;
                    assert(m.totalMessages == total);
                    m.messages.put(current, message);

                    if (m.totalMessages == m.receivedMessages) {
                        complete = true;
                        StringBuilder sbFullMessage = new StringBuilder();
                        for (int i = 1; i <= m.totalMessages; i++) {
                            sbFullMessage.append(m.messages.get(i));
                        }
                        fullMessage = sbFullMessage.toString();
                        cv.put(MocaDB.NotificationSQLFormat.FULL_MESSAGE, fullMessage);
                        cv.put(MocaDB.NotificationSQLFormat.DOWNLOADED, 1);
                    }
                }
            } else {
                Log.e(TAG, "Received mal-formed Notification Message length: "
                        + notificationHeader.d);
            }

            c.close();

            storedMessage = g.toJson(m);
            cv.put(MocaDB.NotificationSQLFormat.MESSAGE, storedMessage);

            notificationUri = ContentUris.withAppendedId(
                    MocaDB.NotificationSQLFormat.CONTENT_URI, notificationId);
            int rowsUpdated = context.getContentResolver().update(
                    notificationUri, cv, null, null);
            if (rowsUpdated != 1) {
                Log.e(TAG, "Failed updating notification URI: "
                        + notificationUri);
            }
        } else {
            // Notification is new, create one.
            NotificationMessage m = new NotificationMessage();
            if (notificationHeader.d != null) {
                // This is a multipart message
                Log.i(TAG, "Received multi-part SMS");
                String parts = notificationHeader.d;

                Matcher matcher = pattern.matcher(notificationHeader.d);
                if (matcher.matches()) {
                    Integer current = Integer.parseInt(matcher.group(1));
                    Integer total = Integer.parseInt(matcher.group(2));
                    m.totalMessages = total;
                    m.receivedMessages = 1;
                    m.messages.put(current, message);

                    if (m.totalMessages == m.receivedMessages) {
                        complete = true;
                        StringBuilder sbFullMessage = new StringBuilder();
                        for (int i = 1; i <= m.totalMessages; i++) {
                            sbFullMessage.append(m.messages.get(i));
                        }
                        fullMessage = sbFullMessage.toString();
                        cv.put(MocaDB.NotificationSQLFormat.FULL_MESSAGE, fullMessage);
                        cv.put(MocaDB.NotificationSQLFormat.DOWNLOADED, 1);
                    }

                } else {
                    Log.e(TAG, "Received malformed Notification Message length:"
                            + " " + notificationHeader.d);
                }
            } else {
                // This is a single message.
                Log.i(TAG, "Received single-part SMS");
                m.totalMessages = 1;
                m.receivedMessages = 1;
                m.messages.put(1, message);
                cv.put(MocaDB.NotificationSQLFormat.FULL_MESSAGE, message);
                cv.put(MocaDB.NotificationSQLFormat.DOWNLOADED, 1);
                complete = true;
            }

            String storedMessage = g.toJson(m);
            cv.put(MocaDB.NotificationSQLFormat.MESSAGE, storedMessage);
            cv.put(MocaDB.NotificationSQLFormat.NOTIFICATION_GUID,
                    notificationHeader.n);
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
    }*/
}
