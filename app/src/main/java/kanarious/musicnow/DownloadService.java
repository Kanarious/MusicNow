package kanarious.musicnow;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import org.json.JSONObject;
import java.util.ArrayList;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    private PowerManager.WakeLock wakeLock;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int id;
    private String download_title = "";
    private DownloadManager downloadManager;
    private Context mContext;
    private BroadcastReceiver update;
    private DownloadServiceThread main;

    public static final String ACTION_STOP_THREAD = "STOP_THREAD";
    public static final String ACTION_STOP_SERVICE = "STOP_SERVICE";
    public static final String THREAD_ID = "THREAD_ID";
    public static final int THREAD_ID_ERROR = -1;
    public static final String DOWNLOAD_SERVICE = "DOWNLOAD_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        //Power Manager
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicNow.DownloadService:Wakelock");
        wakeLock.acquire();
        Log.d(TAG, "onCreate: WakeLock Acquired");
        //Download Manager
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //Create Broadcast Receiver for Download Updates
        update = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra(DOWNLOAD_SERVICE);
                int id = intent.getIntExtra(THREAD_ID, THREAD_ID_ERROR);
                if(input.equals(ACTION_STOP_THREAD)){
                    main.stopThread(id);
                }
                if(input.equals(ACTION_STOP_SERVICE)){
                    stopSelf();
                }
            }
        };
        this.registerReceiver(update,new IntentFilter(DOWNLOAD_SERVICE));
        //Main Thread
        main = new DownloadServiceThread(handler, mContext) {
            @Override
            protected void onFinish() {
            }
        };
        main.start();

        Log.d(TAG, "onCreate: Download Service is Created");
    }

    @Override
    public void onDestroy() {
        //Power Manager
        wakeLock.release();
        //Broadcast Receiver
        unregisterReceiver(update);
        //Main Thread
        main.running = false;
        Log.d(TAG, "onDestroy: Download Service is Destoryed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return START_NOT_STICKY;
    }

    protected void onHandleIntent(Intent intent) {
        //Get JSON Input
        String input = intent.getStringExtra(Intent.EXTRA_TEXT);
        //Get Respected Panel ID
        id = intent.getIntExtra(Intent.EXTRA_COMPONENT_NAME,0);

        try {
            //Get YTFile
            JSONObject inputJson = new JSONObject(input);
            YTFile ytFile = new YTFile(inputJson) {
                @Override
                protected void postProcess() {
                }
            };
            download_title = ytFile.getTitle();

            //Post Start
            startForeground(id,NotificationCreator.createCanceledNotification(ytFile.getTitle(),"Downloading"));

            //Start Download
            DownloadThread downloadThread = new DownloadThread(downloadManager, handler, mContext, ytFile, id);
            main.addThread(downloadThread);
            downloadThread.start();
            main.startService();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
