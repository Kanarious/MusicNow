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

import java.io.File;
import java.util.ArrayList;

public class DownloadForegroundService extends Service {

    public class FileContainer {
        private final String file;
        private final int id;
        private PanelUpdates state;

        public FileContainer(String file, int id){
            this.file = file;
            this.id = id;
            this.state = PanelUpdates.START;
        }

        public String getFile(){ return file; }
        public int getId(){ return id; }
        public PanelUpdates getState() { return state; }
        public void setState(PanelUpdates state){ this.state = state; }
    }

    private static final String TAG = "DownloadForegroundService";
    private static final ArrayList<DownloadThread> threads = new ArrayList<>();
    private static final ArrayList<FileContainer> ytfiles = new ArrayList<>();
    private final IBinder binder = new LocalBinder();
    private int lastStartId;

    //Intent Filter
    public static String DOWNLOAD_SERVICE = "DOWNLOAD_SERVICE";
    //Intent Actions
    public static String ACTION_STOP_SERVICE = "STOP_SERVICE";
    public static String ACTION_STOP_THREAD = "STOP_THREAD";
    public static String ACTION_START_THREAD = "START_THREAD";
    public static String ACTION_FINISHED_THREAD = "FINISHED_THREAD";
    //Intent Extras
    public static String YTFILE = "YTFILE";
    public static String PANEL_ID = "PANEL_ID";
    public static String THREAD_ID = "THREAD_ID";
    public static int ID_ERROR = -1;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        threads.clear();
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
        lastStartId = startId;
        String action = intent.getStringExtra(DOWNLOAD_SERVICE);
        if(action.equals(ACTION_STOP_SERVICE)){
            stopThreads();
            if(stopSelfResult(startId)) {
                Log.i(TAG, "handleIntent(): ACTION_STOP_SERVICE service stopped");
            }
            else{
                Log.e(TAG, "handleIntent(): ACTION_STOP_SERVICE service FAILED stopped");
            }
        }
        else if(action.equals(ACTION_STOP_THREAD)){
            int id = intent.getIntExtra(PANEL_ID, ID_ERROR);
            if(id != ID_ERROR) {
                Log.i(TAG, "handleIntent: ACTION_STOP_THREAD stopping thread "+id);
                stopThread(id);
            }
            else{
                Log.e(TAG, "handleIntent: ACTION_STOP_THREAD no ID found");
            }
        }
        else if(action.equals(ACTION_START_THREAD)){
            int id = intent.getIntExtra(PANEL_ID, ID_ERROR);
            String yt_string = intent.getStringExtra(YTFILE);
            FileContainer new_file = new FileContainer(yt_string,id);
            ytfiles.add(new_file);
            try {
                startThread(yt_string, id);
            }catch (Exception e){
                Log.e(TAG, "handleIntent: ACTION START_THREAD failed to start thread: ", e);
            }
        }
        else if(action.equals(ACTION_FINISHED_THREAD)){
            int id = intent.getIntExtra(THREAD_ID, ID_ERROR);
            if(id == ID_ERROR){
                Log.e(TAG, "handleIntent: ACTION_FINISHED_THREAD no ID found");
            }
            else{
                removeFile(id);
                removeThread(id);
            }
        }
    }

    private void startThread(String yt_string, int id) throws JSONException {
        JSONObject inputJson = new JSONObject(yt_string);
        YTFile ytFile = new YTFile(inputJson) {
            @Override
            protected void postProcess() {
            }
            @Override
            protected void notifyExtraction(String message) {
            }
        };
        DownloadThread thread = new DownloadThread((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE),
                new Handler(Looper.getMainLooper()),
                this,
                ytFile,
                id) {
            @Override
            protected void onDownloadFinished() {
                removeFile(id);
                removeThread(id);
            }

            @Override
            protected void onUpdateState(PanelUpdates state) {
                updateFileState(id,state);
            }
        };
        addThread(thread);
    }

    private void stopThread(int id){
        for(DownloadThread thread:threads){
            if(thread.threadID()==id){
                thread.running = false;
                removeFile(id);
                removeThread(thread);
                break;
            }
        }
    }

    private void removeThread(int id){
        for(DownloadThread thread:threads){
            if(thread.threadID()==id){
                threads.remove(thread);
                checkThreadCount();
                break;
            }
        }
    }

    private void removeThread(DownloadThread thread){
        threads.remove(thread);
        checkThreadCount();
    }

    private void checkThreadCount(){
        if(threads.size() == 0){
            stopService();
        }
    }

    private void removeFile(int id){
        ytfiles.removeIf(container -> container.getId() == id);
    }

    private void stopService(){
        if(stopSelfResult(lastStartId)) {
            Log.i(TAG, "stopService(): service stopped");
        }
        else{
            Log.e(TAG, "stopService(): service FAILED stopped");
        }
    }

    private void stopThreads(){
        if (threads.size() > 0) {
            for (DownloadThread thread : threads) {
                if (thread.isAlive()) {
                    thread.running = false;
                }
            }
        }
    }

    private void addThread(DownloadThread thread){
        threads.add(thread);
        if(!thread.isAlive()) {
            thread.start();
        }
    }

    private void updateFileState(int id, PanelUpdates state){
        for(FileContainer container:ytfiles){
            if(container.getId() == id){
                container.setState(state);
                return;
            }
        }
    }

    public ArrayList<FileContainer> getYtfiles(){ return ytfiles; }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: Service Unbinded");
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
