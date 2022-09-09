package kanarious.musicnow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceReceiver extends BroadcastReceiver {
    public static final String INTENT_ID = "MUSIC_NOW_PANEL_ID";
    public static final String SERVICE_ACTION = "MUSIC_NOW_SERVICE_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(INTENT_ID,-1);
        String action = intent.getStringExtra(SERVICE_ACTION);

        Intent serviceIntent = new Intent(context,DownloadForegroundService.class);
        serviceIntent.putExtra(DownloadForegroundService.DOWNLOAD_SERVICE,action);
        serviceIntent.putExtra(DownloadForegroundService.PANEL_ID,id);
        context.startService(serviceIntent);
    }
}
