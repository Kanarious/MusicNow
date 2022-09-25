package kanarious.musicnow;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadServiceThread extends Thread{
    volatile boolean running = true;
    private final ArrayList<DownloadThread> threads;
    private final Context mContext;
    private final ReentrantLock threads_lock = new ReentrantLock();
    private boolean service_started = false;

    DownloadServiceThread(Context context){
        this.mContext = context;
        threads = new ArrayList<>();
    }

    public void addThread(DownloadThread thread){
        threads_lock.lock();
        try {
            threads.add(thread);
            if(!thread.isAlive()) {
                thread.start();
            }
        }
        finally {
            threads_lock.unlock();
        }
    }

    public void stopThread(int id){
        threads_lock.lock();
        try {
            for(DownloadThread thread:threads){
                if(thread.threadID()==id){
                    thread.running = false;
                    break;
                }
            }
        }finally {
            threads_lock.unlock();
        }
    }

    public void startService(){
        this.service_started = true;
    }

    public void cleanUp(){
        threads_lock.lock();
        try {
            if (threads.size() > 0) {
                for (DownloadThread thread : threads) {
                    if (thread.isAlive()) {
                        thread.running = false;
                    }
                }
            }
        }
        finally {
            threads_lock.unlock();
        }
    }

    @Override
    public void run() {
        //Keep thread running until called to stop
        while(running){
            threads_lock.lock();
            try {
                if (threads.size() > 0) {
                    for(int i = threads.size() - 1; i>=0; i--){
                        DownloadThread thread = threads.get(i);
                        if (!thread.isAlive()) {
                            //Check if Thread finished task properly
                            if (!thread.downloadFinished()) {
                                //Notify Task Failed
                                PanelUpdates.sendUpdate(PanelUpdates.FAIL, mContext, thread.getTitle(), thread.threadID());
                            }
                            //Remove Thread from live list
                            threads.remove(i);
                        }
                    }
                } else if (service_started) {
                    stopService();
                }
            }finally {
                threads_lock.unlock();
            }
        }
    }

    private void stopService(){
        Intent serviceIntent = new Intent(mContext,DownloadForegroundService.class);
        serviceIntent.putExtra(DownloadForegroundService.DOWNLOAD_SERVICE,DownloadForegroundService.ACTION_STOP_SERVICE);
        mContext.startService(serviceIntent);
    }


}
