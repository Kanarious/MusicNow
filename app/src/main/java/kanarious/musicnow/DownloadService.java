package kanarious.musicnow;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class DownloadService extends Service {


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra(Intent.EXTRA_TEXT);
        try {
            JSONObject inputJson = new JSONObject(input);
            YTFile ytFile = new YTFile(inputJson) {
                @Override
                protected void postProcess() {
                }
            };
            Notification notification = NotificationCreator.createDownloadNotification(ytFile.getTitle(),"Downloading");
            startForeground(startId,notification);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //INSERT TASK THREAD HERE

        stopSelf();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
