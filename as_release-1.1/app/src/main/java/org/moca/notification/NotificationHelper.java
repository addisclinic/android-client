package org.moca.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.moca.AddisApp;
import org.moca.R;
import org.moca.activity.Moca;

/**
 * Created by Albert on 3/20/2016.
 */
public class NotificationHelper {

    private static final int MDS_NOTIFICATION_ID = NotificationHelper.class.hashCode();
    private static final int MDS_NOTIFICATION_REQUEST_CODE = MDS_NOTIFICATION_ID + 1;
    private NotificationManager mNotifyManager;

    public NotificationHelper() {
        mNotifyManager = (NotificationManager)AddisApp.getInstance().getApplicationContext()
                                                                    .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void dismiss() {
        if (mNotifyManager != null) {
            mNotifyManager.cancel(MDS_NOTIFICATION_ID);
        }
    }
    public void show(String title, String textMessage) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(AddisApp.getInstance().getApplicationContext())
                                                    .setSmallIcon(R.drawable.icon2)
                                                    .setAutoCancel(true)
                                                    .setContentTitle(title)
                                                    .setContentText(textMessage)
                                                    .setTicker("Patient Diagnosis Received");

        Context ctxt = AddisApp.getInstance().getApplicationContext();
        // The PendingIntent launches the Notification Viewer for the particular
        // alert
        Intent resultIntent = new Intent(ctxt, Moca.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(ctxt,
                                                                MDS_NOTIFICATION_REQUEST_CODE,
                                                                resultIntent,
                                                                PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(contentIntent);
        // After a 100ms delay, vibrate for 200ms, pause for 100 ms and
        // then vibrate for 300ms.
        mBuilder.setVibrate(new long[]{100, 200, 100, 300});
        mBuilder.setWhen(System.currentTimeMillis());

        // Use this line if you want a new persistent notification each time:
        // nm.notify((int)Math.round((Math.random() * 32000)), notif);

        // Or use this to overwrite the last notification each time:
        mNotifyManager.notify(MDS_NOTIFICATION_ID, mBuilder.build());
    }
}
