package kanarious.musicnow;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;


import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class DownloadForegroundService extends Service {
    private static final String TAG = "DownloadForegroundService";
    private final IBinder binder = new LocalBinder();
    private DownloadServiceThread main;

    //Intent Filter
    public static String DOWNLOAD_SERVICE = "DOWNLOAD_SERVICE";
    //Intent Actions
    public static String ACTION_STOP_SERVICE = "STOP_SERVICE";
    public static String ACTION_STOP_THREAD = "STOP_THREAD";
    public static String ACTION_START_THREAD = "START_THREAD";
    //Intent Extras
    public static String YTFILE = "YTFILE";
    public static String PANEL_ID = "PANEL_ID";
    public static int PANEL_ID_ERROR = -1;

    @Override
    public void onCreate() {
        main = new DownloadServiceThread(this);
        main.start();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int notification_id = 2147483647;
        startForeground(notification_id,NotificationCreator.createServiceNotification(this,"MusicNow Service","Download Service Active"));
        handleIntent(intent, startId);
        return START_NOT_STICKY;
    }

    private void handleIntent(Intent intent, int startId){
        String action = intent.getStringExtra(DOWNLOAD_SERVICE);
        if(action.equals(ACTION_STOP_SERVICE)){
            main.cleanUp();
            main.running = false;
            if(stopSelfResult(startId)) {
                Log.i(TAG, "handleIntent(): ACTION_STOP_SERVICE service stopped");
            }
            else{
                Log.e(TAG, "handleIntent(): ACTION_STOP_SERVICE service FAILED stopped");
            }
        }
        else if(action.equals(ACTION_STOP_THREAD)){
            int id = intent.getIntExtra(PANEL_ID,PANEL_ID_ERROR);
            if(id != PANEL_ID_ERROR) {
                Log.i(TAG, "handleIntent: ACTION_STOP_THREAD stopping thread "+id);
                main.stopThread(id);
            }
            else{
                Log.e(TAG, "handleIntent: ACTION_STOP_THREAD no ID found");
            }
        }
        else if(action.equals(ACTION_START_THREAD)){
            int id = intent.getIntExtra(PANEL_ID,PANEL_ID_ERROR);
            String yt_string = intent.getStringExtra(YTFILE);
            try {
                startThread(yt_string, id);
            }catch (Exception e){
                Log.e(TAG, "handleIntent: ACTION START_THREAD failed to start thread: ", e);
            }
        }
    }

    private void startThread(String yt_string, int id) throws JSONException {
        JSONObject inputJson = new JSONObject(yt_string);
        YTFile ytFile = new YTFile(inputJson) {
            @Override
            protected void postProcess() {
            }
        };
        DownloadThread thread = new DownloadThread((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE),
                new Handler(Looper.getMainLooper()),
                this,
                ytFile,
                id);
        main.addThread(thread);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: Service Unbinded");
        main.startService();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind: Service Rebinded");
        super.onRebind(intent);
    }

    public class LocalBinder extends Binder{
        DownloadForegroundService getService(){
            return DownloadForegroundService.this;
        }
    }
}
