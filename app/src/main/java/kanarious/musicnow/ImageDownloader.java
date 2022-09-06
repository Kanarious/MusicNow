package kanarious.musicnow;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

public abstract class ImageDownloader {
    private final String TAG = "ImageDownloader";
    private final DownloadManager downloadManager;
    private final Context mContext;
    private long downloadID;
    private String fullFilePath;
    protected abstract void onImageDownloadFinish(String fullFilePath);

    public ImageDownloader(DownloadManager downloadManager, Context context){
        this.downloadManager = downloadManager;
        this.mContext = context;
    }

    public void download(String url, String filename){
        //Create Request
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("Image Download");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,filename);
        setFullFilePath(Environment.DIRECTORY_DOWNLOADS,filename);
        //Queue Download
        boolean download_started = true;
        try{
            this.downloadID = downloadManager.enqueue(request);
        }
        catch (Exception e){
            Log.e(TAG, "download: Failed to queue image download request: "+e.getMessage());
            download_started = false;
        }
        if (!download_started) {
            return;
        }
        //Post Download
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long dwnId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                //Check for valid download
                if (!validDownload(dwnId)){
                    mContext.unregisterReceiver(this);
                    return;
                }
                //Check if download was for this file
                if (dwnId != downloadID){
                    mContext.unregisterReceiver(this);
                    return;
                }
                onImageDownloadFinish(fullFilePath);
                mContext.unregisterReceiver(this);
            }
        };
        mContext.registerReceiver(onComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void setFullFilePath(String dirType, String filename){
       this.fullFilePath = Environment.getExternalStoragePublicDirectory(dirType).getAbsolutePath()+'/'+filename; //DEPRECATED
//       this.fullFilePath = mContext.getExternalFilesDir(dirType).getAbsolutePath()+'/'+filename;
    }

    /**
     * Check if download was valid, see issue
     * http://code.google.com/p/android/issues/detail?id=18462
     * @param downloadId download manager download id (each download has its own unique id)
     * @return if download had a valid status
     */
    private boolean validDownload(long downloadId) {

        Log.d(TAG,"Checking download status for id: " + downloadId);

        //Verify if download is a success
        Cursor c= downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));

        if(c.moveToFirst()){
            @SuppressLint("Range")
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

            if(status == DownloadManager.STATUS_SUCCESSFUL){
                return true; //Download is valid, celebrate
            }else{
                @SuppressLint("Range")
                int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                Log.d(TAG, "Download not correct, status [" + status + "] reason [" + reason + "]");
                return false;
            }
        }
        return false;
    }
}
