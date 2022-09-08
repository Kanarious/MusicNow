package kanarious.musicnow;

import android.app.DownloadManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class DownloadScheduler extends JobService {
    private static final String TAG = "DownloadScheduler";
//    private DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//    private Handler handler = new Handler(Looper.getMainLooper());
    private Context mContext = this;
//    private DownloadThread downloadThread;
//    private int id;



    private DownloadServiceThread main;
    private BroadcastReceiver update;

    //Intent Filter
    public static final String DOWNLOAD_SERVICE = "DOWNLOAD_SERVICE";
    //Intent Actions
    public static final String ACTION_STOP_THREAD = "STOP_THREAD";
    public static final String ACTION_STOP_SERVICE = "STOP_SERVICE";
    public static final String ACTION_KILL_SERVICE = "KILL_SERVICE";
    public static final String ACTION_DOWNLOAD_YTFILE = "DOWNLOAD_YTFILE";
    //Intent Values
    public static final String YTFILE = "YTFILE";
    public static final String THREAD_ID = "THREAD_ID";
    public static final int THREAD_ID_ERROR = -1;



    @Override
    public void onCreate() {
        super.onCreate();
        //Create Broadcast Receiver for Download Updates
        update = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getStringExtra(DOWNLOAD_SERVICE);
                int id = intent.getIntExtra(THREAD_ID, THREAD_ID_ERROR);
                if(action.equals(ACTION_STOP_THREAD)){
                    main.stopThread(id);
                }
                if(action.equals(ACTION_STOP_SERVICE)){
                    //Set Main thread to die when all download threads finish
                    main.running = false;
//                    main.startService();
                }
                if(action.equals(ACTION_KILL_SERVICE)){
                    stopSelf();
                }
                if(action.equals(ACTION_DOWNLOAD_YTFILE)){
                    String jsonInputString = intent.getStringExtra(YTFILE);
                    DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    Handler handler = new Handler(Looper.getMainLooper());
                    try {
                        JSONObject jsonInput = new JSONObject(jsonInputString);
                        YTFile ytFile = new YTFile(jsonInput){
                            @Override
                            protected void postProcess() {
                            }
                        };
                        DownloadThread downloadThread = new DownloadThread(downloadManager, handler, mContext, ytFile, id);
                        main.addThread(downloadThread);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }
        };
        this.registerReceiver(update,new IntentFilter(DOWNLOAD_SERVICE));
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        //if main thread has not started
        Handler handler = new Handler(Looper.getMainLooper());
        main = new DownloadServiceThread(handler, mContext) {
            @Override
            protected void onFinish() {
                jobFinished(params, false);
            }
        };
        main.start();

//        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//        Handler handler = new Handler(Looper.getMainLooper());
//        try {
//            JSONObject inputJson = new JSONObject(params.getExtras().getString(YTFILE));
//            int id = params.getExtras().getInt(PANEL_ID);
//            YTFile ytFile = new YTFile(inputJson){
//                @Override
//                protected void postProcess() {
//                }
//            };
//            DownloadThread downloadThread = new DownloadThread(downloadManager, handler, mContext, ytFile, id);
//            main.addThread(downloadThread);
//            downloadThread.start();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Job Service Destroyed");
//        unregisterReceiver(update);
//        if (main.isAlive()) {
//            main.running = false;
//        }
        super.onDestroy();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob: Job Service Stopped");
//        if(main.isAlive()){
//            main.running = false;
//        }
        return false;
    }

}
