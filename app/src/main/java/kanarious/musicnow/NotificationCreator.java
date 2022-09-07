package kanarious.musicnow;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class NotificationCreator {
    private static final String TAG = "NotificationCreator";
    private static final String CHANNEL_ID = "DOWNLOADS_CHANNEL";
    private static final String CHANNEL_NAME = "Active Downloads";
    private static final String CHANNEL_DESCRIPTION = "Displays active downloads that are in progress";
    private static NotificationManager notificationManager = null;
    private static Context mContext;
    private static Class mOwner;

    public static void createNotificationChannel(Context context, Class owner) {
        mContext = context;
        mOwner = owner;
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = CHANNEL_NAME;
            String description = CHANNEL_DESCRIPTION;
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private static PendingIntent createIntent(){
        Intent notifyIntent = new Intent(mContext, mOwner);
        // Set the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        // Create the PendingIntent
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                mContext, 0, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT// | PendingIntent.FLAG_IMMUTABLE
        );
        return notifyPendingIntent;
    }

    public static Notification createDownloadProgressNotification(String title, String description, int progress){
        if(notificationManager == null){
            Log.e(TAG, "createDownloadProgressNotification: Notification Manager is not Created");
            return null;
        }
        Notification notification = new Notification.Builder(mContext,NotificationCreator.getChannelId())
                .setSmallIcon(R.drawable.ic_download_notification_icon) //Create animation list https://stackoverflow.com/questions/34037962/how-to-animate-the-progress-notification-icon
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(createIntent())
                .setColorized(true)
                .setColor(mContext.getResources().getColor(R.color.dark_red))
                .setProgress(100,progress,false)
                .build();
        return notification;
    }

    public static void notifyDownloadProgress(int id, String title, String description, int progress){
        if(notificationManager == null){
            Log.e(TAG, "notifyDownloadProgress: Notification Manager is not Created");
            return;
        }
        try{
            NotificationCreator.notificationManager.notify(id, createDownloadProgressNotification(title,description,progress));
        }catch (Exception e){
            Log.e(TAG, "notifyDownloadProgress: ", e);
        }
    }

    public static Notification createDownloadNotification(String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "createDownloadNotification: Notification Manager is not Created");
            return null;
        }
        Notification notification = new Notification.Builder(mContext,NotificationCreator.getChannelId())
                .setSmallIcon(R.drawable.ic_download_notification_icon) //Create animation list https://stackoverflow.com/questions/34037962/how-to-animate-the-progress-notification-icon
                .setContentTitle(title)
                .setContentText(description)
//                .setOngoing(false)
//                .setOnlyAlertOnce(true)
                .setContentIntent(createIntent())
                .setProgress(0,0,true)
                .setColorized(true)
                .setColor(mContext.getResources().getColor(R.color.dark_red))
                .build();
        return notification;
    }

    public static void notifyDownload(int id, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "notifyDownload: Notification Manager is not Created");
            return;
        }
        try {
            NotificationCreator.notificationManager.notify(id, createDownloadNotification(title,description));
        }catch (Exception e){
            Log.e(TAG, "notifyDownload: ", e);
        }
    }

    public static Notification createCanceledNotification(String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "createCanceledNotification: Notification Manager is not Created");
            return null;
        }
        Notification notification = new Notification.Builder(mContext,NotificationCreator.getChannelId())
                .setSmallIcon(R.drawable.ic_cancelled_notification_icon) //Create Cancelled Icon
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(createIntent())
                .setOnlyAlertOnce(true)
                .build();
        return notification;
    }

    public static void notifyCancel(int id, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "notifyCancel: Notification Manager is not Created");
            return;
        }
        try {
            NotificationCreator.notificationManager.notify(id, createCanceledNotification(title, description));
        } catch (Exception e){
            Log.e(TAG, "notifyCancel: ", e);
        }
    }

    public static Notification createFinishedNotification(String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "createFinishedNotification: Notification Manager is not Created");
            return null;
        }
        Notification notification = new Notification.Builder(mContext,NotificationCreator.getChannelId())
                .setSmallIcon(R.drawable.ic_done_notification_icon)
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(createIntent())
                .setOnlyAlertOnce(true)
                .build();
        return notification;
    }

    public static void notifyFinished(int id, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "notifyFinished: Notification Manager is not Created");
            return;
        }
        try{
            NotificationCreator.notificationManager.notify(id, createFinishedNotification(title,description));
        }catch(Exception e){

        }
    }

    public static Notification createFailedNotification(String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "createFailedNotification: Notification Manager is not Created");
            return null;
        }
        Notification notification = new Notification.Builder(mContext,NotificationCreator.getChannelId())
                .setSmallIcon(R.drawable.ic_cancelled_notification_icon) //Create Download Finished Icon
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(createIntent())
                .setOnlyAlertOnce(true)
                .build();
        return notification;
    }

    public static void notifyFailed(int id, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "notifyFailed: Notification Manager is not Created");
            return;
        }
        try {
            NotificationCreator.notificationManager.notify(id, createFailedNotification(title,description));
        }catch (Exception e){
            Log.e(TAG, "createDownloadNotification: ", e);
        }
    }

    public static String getChannelId(){
        return CHANNEL_ID;
    }
}
