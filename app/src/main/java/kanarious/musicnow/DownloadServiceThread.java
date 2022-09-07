package kanarious.musicnow;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadServiceThread extends Thread{
    volatile boolean running = true;
    private ArrayList<DownloadThread> threads;
    private Handler handler;
    private Context mContext;
    private final ReentrantLock threadslock = new ReentrantLock();
    private boolean called_stop = false;
    private boolean service_started = false;

    DownloadServiceThread(Handler handler, Context context){
        this.handler = handler;
        this.mContext = context;
        threads = new ArrayList<>();
    }

    public void addThread(DownloadThread thread){
        threadslock.lock();
        try {
            threads.add(thread);
//            thread.start();
        }
        finally {
            threadslock.unlock();
        }
    }

    public void stopThread(int id){
        threadslock.lock();
        try {
            for(DownloadThread thread:threads){
                if(thread.threadID()==id){
                    thread.running = false;
                    break;
                }
            }
        }finally {
            threadslock.unlock();
        }
    }

    public void startService(){
        this.service_started = true;
    }

    @Override
    public void run() {
        //Keep thread running until called to stop
        while(running){
            threadslock.lock();
            try {
                if (threads.size() > 0) {
                    for(int i = 0; i<threads.size()-1; i++){
                        DownloadThread thread = threads.get(i);
                        if (!thread.isAlive()) {
                            //Check if Thread finished task properly
                            if (!thread.downloadFinished()) {
                                //Notify Task Failed
                                PanelUpdates.sendUpdate(PanelUpdates.FAIL, handler, mContext, thread.getTitle(), thread.threadID());
                            }
                            //Remove Thread from live list
                            threads.remove(i);
                        }
                    }
                } else if (!called_stop && service_started) {
                    stopService();
                    called_stop = true;
                }
            }finally {
                threadslock.unlock();
            }
        }
        super.run();
    }

    private void stopService(){
        Intent intent = new Intent(DownloadService.DOWNLOAD_SERVICE);
        intent.putExtra(DownloadService.DOWNLOAD_SERVICE,DownloadService.ACTION_STOP_SERVICE);
        mContext.sendBroadcast(intent);
    }


}
