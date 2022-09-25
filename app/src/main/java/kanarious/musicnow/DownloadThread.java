package kanarious.musicnow;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import java.io.File;
import org.apache.commons.io.FilenameUtils;

public abstract class DownloadThread extends Thread{
    private final DownloadManager downloadManager;
    private final Handler handler;
    private final Context mContext;
    private final YTFile ytFile;
    private final int id;
    private final String TAG;
    private long dl_id = -1;
    volatile boolean running = true;
    private boolean download_finished;
    private boolean download_failed;
    private final String download_title;

    public boolean downloadFinished(){ return download_finished; }
    public int threadID(){ return this.id; }
    public String getTitle(){ return this.download_title; }

    protected abstract void onDownloadFinished();

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
        //This will ensure the ffmpeg kit will find the file (this will NOT effect final downloaded file name)
        filename = filename.replace(' ','_');
        filename = filename.replace("'","");
        filename = filename.replace("/","");
        filename = filename.replace("\\","");
        filename = filename.replace(".","");
        download_filename = filename + Id3Editor.TEMP_NAME_ADD + fileExtension;
        return download_filename;
    }

    private boolean checkFile(String filename, String filepath){
        //Check if File Exists (with .mp4a extension)
        File f1 = new File(filepath+filename);
        //Check if File exists as .mp3
        String new_filename = FilenameUtils.removeExtension(filename) + ".mp3";
        File f2 = new File(filepath+new_filename);
        return f1.exists() || f2.exists();
    }

    private String reprocessFileName(String filename, String filepath){
        String new_filename = FilenameUtils.removeExtension(filename) + ".mp3";
        File f = new File(filepath+new_filename);
        if(f.exists()){
            //rename file name to the new existing file name
            return new_filename;
        }else{
            //mp3 file name does not exist
            return filename;
        }
    }

    protected void download(YTFile ytFile){
        //Create Download Request
        Log.i(TAG, "download: Creating Download Request");
        boolean download_started = false;
        String filename = processFileName(ytFile.getTitle(), ytFile.filetype);
        boolean file_exists = checkFile(filename,StorageAccess.getDownloadsFolder());
        filename = reprocessFileName(filename,StorageAccess.getDownloadsFolder());
        if (!file_exists) {
            Uri uri = Uri.parse(ytFile.getUrl());
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(ytFile.getTitle());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            ytFile.setLocation(StorageAccess.getDownloadsFolder());
            //Start Download Manager
            try {
                dl_id = this.downloadManager.enqueue(request);
                Log.d(TAG, "download: acquired download id " + dl_id);
                ytFile.setDlid(dl_id);
                download_started = true;
            } catch (Exception e) {
                Log.e(TAG, "download: Failed to queue download request: " + e.getMessage());
            }
        }
        else{
            download_started = true;
        }

        if(download_started) {
            sendUpdate(PanelUpdates.START);
            //Get Download Progress
            int progress = 0;
            int new_progress;
            sendUpdate(PanelUpdates.PROGRESS, progress);
            boolean isDownloadFinished = false;
            boolean isDownloadSuccessful = false;
            if(!file_exists) {
                while (!isDownloadFinished) {
                    Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(dl_id));
                    if (cursor.moveToFirst()) {
                        @SuppressLint("Range") int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        switch (downloadStatus) {
                            case DownloadManager.STATUS_RUNNING:
                                @SuppressLint("Range") long totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                                if (!running) {
                                    Log.d(TAG, "download: download cancelled...");
                                    downloadManager.remove(dl_id);
                                    sendUpdate(PanelUpdates.CANCEL);
                                    isDownloadFinished = true;
                                } else if (totalBytes > 0) {
                                    @SuppressLint("Range") long downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                    new_progress = (int) (downloadedBytes * 100 / totalBytes);
                                    if (progress != new_progress) {
                                        progress = new_progress;
                                        sendUpdate(PanelUpdates.PROGRESS, progress);
                                        Log.d(TAG, "download: downloading... " + progress + "%");
                                    }
                                }
                                break;
                            case DownloadManager.STATUS_SUCCESSFUL:
                                progress = 100;
                                sendUpdate(PanelUpdates.PROGRESS, progress);
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
            }
            if(!running){
                download_finished = true;
                onDownloadFinished();
                return;
            }

            //Perform Post Download Conversions
            if (isDownloadSuccessful || file_exists) {
                sendUpdate(PanelUpdates.META);
                postDownload(ytFile,filename);
                while(!download_finished){
                    //wait for other multi-threaded processes...
                    Log.d(TAG, "download: Service is waiting...");
                }
                if(download_failed){
                    Log.d(TAG, "download: Post Download Failed");
                }
                else{
                    Log.d(TAG, "download: Download Finished");
                }
            }
            //Download Error
            else{
                Log.d(TAG, "download: Download Error");
                sendUpdate(PanelUpdates.FAIL);
                download_finished = true;
                onDownloadFinished();
            }
        }
        //Download Did not Start
        else{
            Log.d(TAG, "download: Failed to Start Download");
            sendUpdate(PanelUpdates.FAIL);
            download_finished = true;
            onDownloadFinished();
        }
    }

    private void postDownload(YTFile ytFile, String filename){
        //Convert File to MP3
        final String new_filename = filename.replace(".mp4a",".mp3");
        final String cmd = "-y -i "+ytFile.getLocation()+filename+" -vn "+ytFile.getLocation()+new_filename;
        FFmpegKit.executeAsync(cmd, session -> {
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
                            onDownloadFinished();
                        }
                    };
                    id3Editor.setTitle(ytFile.getTitle());
                    if(ytFile.embedArtist()){
                        id3Editor.setArtist(ytFile.getArtist());
                    }
                    if(ytFile.embedImage()){
                        id3Editor.setAlbumCover(ytFile.getImageURL(),
                                                ytFile.getImageWidth(),
                                                ytFile.getImageHeight(),
                                                ytFile.getImageX(),
                                                ytFile.getImageY());
                    }
                    id3Editor.embedData();
                } catch (Exception e) {
                    sendUpdate(PanelUpdates.FAIL);
                    download_finished = true;
                    onDownloadFinished();
                    Log.e(TAG, "onReceive: Failed to embed image to " + ytFile.getLocation()+filename+ " : " + e.getMessage());
                }
            }
            //Converted File, no album image needed
            else if (ReturnCode.isSuccess(returnCode)){
                handler.post(() -> new SingleMediaScanner(mContext,new File(ytFile.getLocation()+new_filename)));
                sendUpdate(PanelUpdates.FINISH);
                download_finished = true;
                onDownloadFinished();
            }
            //Failed to Convert File
            else{
                sendUpdate(PanelUpdates.FAIL);
                download_failed = true;
                download_finished = true;
                onDownloadFinished();
            }
            Log.d(TAG, String.format("FFmpeg process exited with state %s and rc %s.%s", state, returnCode, session.getFailStackTrace()));
        });
    }

    private void sendUpdate(PanelUpdates update){
        PanelUpdates.sendUpdate(update, mContext, download_title, id);
    }

    private void sendUpdate(PanelUpdates update, int progress){
        PanelUpdates.sendUpdate(update, mContext, download_title, id, progress);
    }

}
