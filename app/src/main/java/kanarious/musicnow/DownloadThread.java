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
import android.os.Handler;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;

import java.io.File;

public class DownloadThread extends Thread{
    private DownloadManager downloadManager;
    private final Handler handler;
    private final Context mContext;
    private YTFile ytFile;
    private final int id;
    private final String TAG;
    private long dl_id = -1;
    volatile boolean running = true;
    private boolean download_finished;
    private final String download_title;

    public boolean downloadFinished(){ return download_finished; }
    public int threadID(){ return this.id; }
    public String getTitle(){ return this.download_title; }

    DownloadThread(DownloadManager downloadManager, Handler handler, Context context, YTFile ytFile, int id){
        this.downloadManager = downloadManager;
        this.handler = handler;
        this.mContext = context;
        this.ytFile = ytFile;
        this.download_title = ytFile.getTitle();
        this.id = id;
        this.TAG = "DownloadThread"+id;
    }

    @Override
    public void run() {
        download(ytFile);
        super.run();
    }

    private String processFileName(String filename, String fileExtension){
        String download_filename;
        download_filename = filename + Id3Editor.TEMP_NAME_ADD + fileExtension;
        download_filename = download_filename.replace(' ','_');
        download_filename = download_filename.replace("'","");
        download_filename = download_filename.replace("/","");
        download_filename = download_filename.replace("\\","");
        return download_filename;
    }

    private void checkFile(String filename, String filepath){
        //Check if File Exists
        File f = new File(filepath+filename);
        if (f.exists()){
            //Delete Existing File
            if (!f.delete()){
                Log.e(TAG, "checkFile: Failed to delete exisiting temp file "+filepath+filename);
            }
        }
    }

    protected void download(YTFile ytFile){
        //Create Download Request
        Log.i(TAG, "download: Creating Download Request");
        String filename = processFileName(ytFile.getTitle(), ytFile.filetype);
        checkFile(filename,StorageAccess.getDownloadsFolder());
        Uri uri = Uri.parse(ytFile.getUrl());
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(ytFile.getTitle());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        ytFile.setLocation(StorageAccess.getDownloadsFolder());
        //Start Download Manager
        boolean download_started = false;
        try{
            dl_id = this.downloadManager.enqueue(request);
            Log.d(TAG, "download: acquired download id "+dl_id);
            ytFile.setDlid(dl_id);
            download_started = true;
        }
        catch (Exception e){
            Log.e(TAG, "download: Failed to queue download request: "+e.getMessage());
        }

        if(download_started) {
            sendUpdate(PanelUpdates.START);
            //Get Download Progress
            int progress = 0;
            int new_progress=0;
            sendUpdate(PanelUpdates.PROGRESS, progress);
            boolean isDownloadFinished = false;
            boolean isDownloadSuccessful = false;
            while (!isDownloadFinished) {
                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(dl_id));
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    switch (downloadStatus) {
                        case DownloadManager.STATUS_RUNNING:
                            @SuppressLint("Range") long totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            if(!running){
                                Log.d(TAG, "download: download cancelled...");
                                downloadManager.remove(dl_id);
                                sendUpdate(PanelUpdates.CANCEL);
                                isDownloadFinished = true;
                            }
                            else if (totalBytes > 0) {
                                @SuppressLint("Range") long downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                new_progress = (int) (downloadedBytes * 100 / totalBytes);
                                if(progress != new_progress) {
                                    progress = new_progress;
                                    sendUpdate(PanelUpdates.PROGRESS, progress);
                                    Log.d(TAG, "download: downloading... "+progress+"%");
                                }
                            }
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            progress = 100;
                            sendUpdate(PanelUpdates.PROGRESS,progress);
                            isDownloadFinished = true;
                            isDownloadSuccessful = true;
                            break;
                        case DownloadManager.STATUS_PAUSED:
                        case DownloadManager.STATUS_PENDING:
                            break;
                        case DownloadManager.STATUS_FAILED:
                            isDownloadFinished = true;
                            isDownloadSuccessful = false;
                            break;
                    }
                }
            }
            if(!running){
                download_finished = true;
                return;
            }

            //Perform Post Download Conversions
            if (isDownloadSuccessful) {
                handler.post(()->sendUpdate(PanelUpdates.META));
                postDownload(ytFile,filename);
                while(!download_finished){
                    //wait for other multi-threaded processes...
                    Log.d(TAG, "download: Service is waiting...");
                }
            }
            //Download Error
            else{
                sendUpdate(PanelUpdates.FAIL);
                download_finished = true;
                Log.d(TAG, "download: Download Error");
            }

        }
        //Download Did not Start
        else{
            sendUpdate(PanelUpdates.FAIL);
            download_finished = true;
            Log.d(TAG, "download: Failed to Start Download");
        }
    }

    private void postDownload(YTFile ytFile, String filename){
        //Convert File to MP3
        final String new_filename = filename.replace(".mp4a",".mp3");
        final String cmd = "-y -i "+ytFile.getLocation()+filename+" -vn "+ytFile.getLocation()+new_filename;
        FFmpegKit.executeAsync(cmd, new FFmpegSessionCompleteCallback() {
            @Override
            public void apply(FFmpegSession session) {
                //Get Status
                SessionState state = session.getState();
                ReturnCode returnCode = session.getReturnCode();
                //Converted File and Embed Album Image
                if(ReturnCode.isSuccess(returnCode)) {
                    //delete old file
                    if (!new File(ytFile.getLocation()+filename).delete()){
                        Log.w(TAG, "postDownloadApply: failed to delete: "+ytFile.getLocation()+filename);
                    }
                    //Edit id3
                    try {
                        Id3Editor id3Editor = new Id3Editor(ytFile.getLocation(), new_filename, downloadManager, mContext) {
                            @Override
                            protected void onId3EditFinish(String fullFilePath) {
                                handler.post(() -> new SingleMediaScanner(mContext,new File(fullFilePath)));
                                sendUpdate(PanelUpdates.FINISH);
                                download_finished = true;
                            }
                        };
                        id3Editor.setTitle(ytFile.getTitle());
                        if(ytFile.embedArtist()){
                            id3Editor.setArtist(ytFile.getArtist());
                        }
                        if(ytFile.embedImage()){
                            id3Editor.setAlbumCover(ytFile.getImageURL());
                        }
                        id3Editor.embedData();
                    } catch (Exception e) {
                        sendUpdate(PanelUpdates.FAIL);
                        download_finished = true;
                        Log.e(TAG, "onReceive: Failed to embed image to " + ytFile.getLocation()+filename+ " : " + e.getMessage());
                    }
                }
                //Converted File, no album image needed
                else if (ReturnCode.isSuccess(returnCode)){
                    handler.post(() -> new SingleMediaScanner(mContext,new File(ytFile.getLocation()+new_filename)));
                    sendUpdate(PanelUpdates.FINISH);
                }
                //Failed to Convert File
                else{
                    sendUpdate(PanelUpdates.FAIL);
                    download_finished = true;
                }
                Log.d(TAG, String.format("FFmpeg process exited with state %s and rc %s.%s", state, returnCode, session.getFailStackTrace()));
            }
        });
    }

    private void sendUpdate(PanelUpdates update){
        PanelUpdates.sendUpdate(update, handler, mContext, download_title, id);
    }

    private void sendUpdate(PanelUpdates update, int progress){
        PanelUpdates.sendUpdate(update, handler, mContext, download_title, id, progress);
    }
}
