package kanarious.musicnow;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import org.json.JSONObject;
import java.util.ArrayList;

public class DownloadService extends IntentService {
    private static final String TAG = "DownloadService";
    private PowerManager.WakeLock wakeLock;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int id;
    private String download_title = "";
    private DownloadManager downloadManager;
    private Context mContext;
    private BroadcastReceiver update;

    private ArrayList<DownloadThread> threads;

    public static final String ACTION_STOP = "STOP";
    public static final String THREAD_ID = "THREAD_ID";
    public static final int THREAD_ID_ERROR = -1;
    public static final String DOWNLOAD_SERVICE = "DOWNLOAD_SERVICE";

    public DownloadService() {
        super("DownloadService");
        setIntentRedelivery(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        threads = new ArrayList<>();
        //Power Manager
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicNow.DownloadService:Wakelock");
        wakeLock.acquire();
        //Download Manager
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //Create Broadcast Receiver for Download Updates
        update = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra(DOWNLOAD_SERVICE);
                int id = intent.getIntExtra(THREAD_ID, THREAD_ID_ERROR);
                if(input.equals(ACTION_STOP)){
                    for(DownloadThread thread:threads){
                        if(thread.threadID() == id){
                            thread.running = false;
                        }
                    }
                }
            }
        };
        this.registerReceiver(update,new IntentFilter(DOWNLOAD_SERVICE));
        Log.d(TAG, "onCreate: WakeLock Acquired");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Power Manager
        wakeLock.release();
        //Broadcast Receiver
        unregisterReceiver(update);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
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
            startForeground(id,NotificationCreator.createDownloadNotification(ytFile.getTitle(),"Downloading"));

            //Start Download
            DownloadThread downloadThread = new DownloadThread(downloadManager, handler, mContext, ytFile, id);
            threads.add(downloadThread);
            downloadThread.start();

            //Keep Service Alive while Downloads are in progress
            while(threads.size() > 0){
                for(DownloadThread thread:threads){
                    //Check if Thread is still running
                    if(!thread.isAlive()){
                        //Check if Thread finished task properly
                        if(!thread.downloadFinished()){
                            //Notify Task Failed
                            PanelUpdates.sendUpdate(PanelUpdates.FAIL,handler,mContext,thread.getTitle(),thread.threadID());
                        }
                        //Remove Thread from live list
                        threads.remove(thread);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
