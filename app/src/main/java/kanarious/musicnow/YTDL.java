package kanarious.musicnow;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.example.messagesutil.UIMessages;

import org.json.JSONObject;

import java.io.File;

/**
 * Download Process: Download(downloads mp4a) -> Receiver -> PostDownload(FFmpeg cmd)
 */
public abstract class YTDL {

    private final String TAG = "YTDL";
    private final Context mContext;
    private final DownloadManager downloadManager;

    protected abstract void onDownloadFail();
    protected abstract void onDownloadStart();
    protected abstract void onDownloadCanceled();
    protected abstract void onDownloadFinish();

    /**
     * Class Constructor
     */
    public YTDL(Context activity_context, DownloadManager downloadManager){
        //Set Context
        this.mContext = activity_context;
        this.downloadManager = downloadManager;
    }

    public boolean download(YTFile ytFile){
        //Check for assigned download manager
        if (this.downloadManager == null){
            Log.e(TAG, "download: Download Manager NULL");
            return false;
        }
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
        //Start Download
        boolean download_started = false;
        try{
            long id = this.downloadManager.enqueue(request);
            Log.d(TAG, "download: acquired download id "+id);
            ytFile.setDlid(id);
            download_started = true;
        }
        catch (Exception e){
            Log.e(TAG, "download: Failed to queue download request: "+e.getMessage());
            UIMessages.showToast(mContext,"Failed to download");
        }

        //Assign Receiver to Convert File to mp3 After Download
        if (download_started) {
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //Check Download
                    if (!validateDownload(intent,ytFile)){
                        mContext.unregisterReceiver(this);
                        return;
                    }
                    postDownload(ytFile,filename);
                    //Unregister Receiver for Cleanup
                    mContext.unregisterReceiver(this);
                }
            };
            mContext.registerReceiver(onComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            onDownloadStart();
        }
        else{
            onDownloadFail();
        }
        return download_started;
    }

    public void cancelDownload(YTFile ytFile){
        if (downloadManager == null) {
            Log.e(TAG, "download: Download Manager NULL");
        }
        else if (downloadManager.remove(ytFile.getDlid()) > 0) {
            onDownloadCanceled();
        }
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

    /**
     *
     * @param filename name of the file
     * @param fileExtension extension for the file
     * @return New download file name
     */
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

    private boolean validateDownload(Intent intent, YTFile ytFile){
        long dwnId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        //Check for valid download
        if (!validDownload(dwnId)){
            return false;
        }
        //Check if download was for this file
        return dwnId == ytFile.getDlid();
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
                                new SingleMediaScanner(mContext,new File(fullFilePath));
                                UIMessages.showToast(mContext,"DOWNLOAD FINISHED");
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
                        onDownloadFinish();
                    } catch (Exception e) {
                        onDownloadFail();
                        Log.e(TAG, "onReceive: Failed to embed image to " + ytFile.getLocation()+filename+ " : " + e.getMessage());
                    }
                }
                //Converted File, no album image needed
                else if (ReturnCode.isSuccess(returnCode)){
                    new SingleMediaScanner(mContext,new File(ytFile.getLocation()+new_filename));
                    UIMessages.showToast(mContext,"DOWNLOAD FINISHED");
                }
                //Failed to Convert File
                else{
                    UIMessages.showToast(mContext,"DOWNLOAD FAILED");
                }

                Log.d(TAG, String.format("FFmpeg process exited with state %s and rc %s.%s", state, returnCode, session.getFailStackTrace()));
            }
        });
    }
}
