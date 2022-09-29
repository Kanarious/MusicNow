package kanarious.musicnow;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class NotificationCreator {
    private static final String TAG = "NotificationCreator";
    private static NotificationManager notificationManager = null;
    private static NotificationManager serviceNotificationManager = null;
    private static Class mOwner;

    //Download Notification Channel
    private static final String CHANNEL_ID = "DOWNLOADS_CHANNEL";
    private static final String CHANNEL_NAME = "Active Downloads";
    private static final String CHANNEL_DESCRIPTION = "Displays active downloads that are in progress";
    //Download Service Channel
    private static final String SERVICE_CHANNEL_ID = "SERVICE_CHANNEL";
    private static final String SERVICE_CHANNEL_NAME = "Download Service";
    private static final String SERVICE_CHANNEL_DESCRIPTION = "Displays active download service operating in the background";

    public static void createNotificationChannel(Context context, Class owner) {
        mOwner = owner;
        buildDownloadChannel(context);
        buildServiceChannel(context);
    }

    private static void buildDownloadChannel(Context context){
        //Create Channel
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
        channel.setDescription(CHANNEL_DESCRIPTION);
        //Register Channel
        notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private static void buildServiceChannel(Context context){
        //Build Channel
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(SERVICE_CHANNEL_ID, SERVICE_CHANNEL_NAME, importance);
        channel.setDescription(SERVICE_CHANNEL_DESCRIPTION);
        //Register Channel
        serviceNotificationManager = context.getSystemService(NotificationManager.class);
        serviceNotificationManager.createNotificationChannel(channel);
    }

    private static PendingIntent createIntent(Context context){
        Intent notifyIntent = new Intent(context, mOwner);
        // Set the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        // Create the PendingIntent
        return PendingIntent.getActivity(context, 0, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static Notification createDownloadProgressNotification(Context context, int id, String title, String description, int progress){
        if(notificationManager == null){
            Log.e(TAG, "createDownloadProgressNotification: Notification Manager is not Created");
            return null;
        }

        //Create Cancel Button Action
        Intent notifyIntent = new Intent(context,ServiceReceiver.class);
        notifyIntent.putExtra(ServiceReceiver.SERVICE_ACTION,DownloadForegroundService.ACTION_STOP_THREAD);
        notifyIntent.putExtra(ServiceReceiver.INTENT_ID,id);
        context.startService(notifyIntent);
        PendingIntent intent = PendingIntent.getBroadcast(context,0,notifyIntent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Action action = new Notification.Action.Builder(R.drawable.ic_cancelled_notification_icon, "Cancel", intent).build();

        return new Notification.Builder(context,NotificationCreator.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_notification_icon) //Create animation list https://stackoverflow.com/questions/34037962/how-to-animate-the-progress-notification-icon
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(createIntent(context))
                .setColorized(true)
                .setColor(context.getResources().getColor(R.color.dark_red,null))
                .setProgress(100,progress,false)
                .addAction(action)
                .build();
    }

    public static void notifyDownloadProgress(Context context, int id, String title, String description, int progress){
        if(notificationManager == null){
            Log.e(TAG, "notifyDownloadProgress: Notification Manager is not Created");
            return;
        }
        try{
            NotificationCreator.notificationManager.notify(id, createDownloadProgressNotification(context,id,title,description,progress));
        }catch (Exception e){
            Log.e(TAG, "notifyDownloadProgress: ", e);
        }
    }

    public static Notification createDownloadNotification(Context context, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "createDownloadNotification: Notification Manager is not Created");
            return null;
        }
        return new Notification.Builder(context,NotificationCreator.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_notification_icon) //Create animation list https://stackoverflow.com/questions/34037962/how-to-animate-the-progress-notification-icon
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(createIntent(context))
                .setProgress(0,0,true)
                .setColorized(true)
                .setColor(context.getResources().getColor(R.color.dark_red,null))
                .build();
    }

    public static void notifyDownload(Context context, int id, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "notifyDownload: Notification Manager is not Created");
            return;
        }
        try {
            NotificationCreator.notificationManager.notify(id, createDownloadNotification(context,title,description));
        }catch (Exception e){
            Log.e(TAG, "notifyDownload: ", e);
        }
    }

    public static Notification createCanceledNotification(Context context, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "createCanceledNotification: Notification Manager is not Created");
            return null;
        }
        return new Notification.Builder(context,NotificationCreator.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cancelled_notification_icon) //Create Cancelled Icon
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(createIntent(context))
                .setOnlyAlertOnce(true)
                .build();
    }

    public static void notifyCancel(Context context, int id, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "notifyCancel: Notification Manager is not Created");
            return;
        }
        try {
            NotificationCreator.notificationManager.notify(id, createCanceledNotification(context, title, description));
        } catch (Exception e){
            Log.e(TAG, "notifyCancel: ", e);
        }
    }

    public static Notification createFinishedNotification(Context context, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "createFinishedNotification: Notification Manager is not Created");
            return null;
        }
        return new Notification.Builder(context,NotificationCreator.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_done_notification_icon)
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(createIntent(context))
                .setOnlyAlertOnce(true)
                .build();
    }

    public static void notifyFinished(Context context, int id, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "notifyFinished: Notification Manager is not Created");
            return;
        }
        try{
            NotificationCreator.notificationManager.notify(id, createFinishedNotification(context,title,description));
        }catch(Exception e){
            Log.e(TAG, "notifyFinished: ", e);
        }
    }

    public static Notification createFailedNotification(Context context, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "createFailedNotification: Notification Manager is not Created");
            return null;
        }
        return new Notification.Builder(context,NotificationCreator.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cancelled_notification_icon) //Create Download Finished Icon
                .setContentTitle(title)
                .setContentText(description)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(createIntent(context))
                .setOnlyAlertOnce(true)
                .build();
    }

    public static void notifyFailed(Context context, int id, String title, String description){
        if(notificationManager == null){
            Log.e(TAG, "notifyFailed: Notification Manager is not Created");
            return;
        }
        try {
            NotificationCreator.notificationManager.notify(id, createFailedNotification(context,title,description));
        }catch (Exception e){
            Log.e(TAG, "createDownloadNotification: ", e);
        }
    }

    public static Notification createServiceNotification(Context context, String title, String description){
        if(serviceNotificationManager == null){
            Log.e(TAG, "createServiceNotification: Notification Manager is not Created");
            return null;
        }
        return new Notification.Builder(context,NotificationCreator.SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_now_icon_solo)
                .setContentTitle(title)
                .setContentText(description)
                .setContentIntent(createIntent(context))
                .setOnlyAlertOnce(true)
                .setColorized(true)
                .setColor(context.getResources().getColor(R.color.dark_red,null))
                .build();
    }

    public static void notifyServiceRunning(Context context, int id, String title, String description){
        if(serviceNotificationManager == null){
            Log.e(TAG, "notifyServiceRunning: Notification Manager is not Created");
            return;
        }
        try {
            NotificationCreator.serviceNotificationManager.notify(id, createDownloadNotification(context,title,description));
        }catch (Exception e){
            Log.e(TAG, "notifyDownload: ", e);
        }
    }
}
