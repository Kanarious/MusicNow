package kanarious.musicnow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.messagesutil.UIMessages;

import org.json.JSONException;
import org.json.JSONObject;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public abstract class YTFile {

    private final String TAG = "YTFile";
    public final String filetype = ".mp4a";
    private Context mContext = null;
    private String title = "";
    private String artist = "";
    private String dlurl = "";
    private long dlid = -1L;
    private String imageURL = "";
    private String location = "";
    private boolean extraction_status = false;
    private boolean embedImage = false;
    private boolean embedArtist = false;
    private final boolean invalid;
    private final boolean isCopy;
    private boolean isEmpty = false;

    private static final String TITLE_KEY = "title";
    private static final String ARTIST_KEY = "artist";
    private static final String DL_URL_KEY = "dlurl";
//    private static final String DL_ID_KEY = "dlid";
    private static final String IMAGE_URL_KEY = "imageURL";
    private static final String LOCATION_KEY = "location";
    private static final String EXTRACTION_STATUS_KEY = "extraction_status";
    private static final String EMBED_IMAGE_KEY = "embedImage";
    private static final String EMBED_ARTIST_KEY = "embedArtist";
    private static final String INVALID_KEY = "invalid";

    public YTFile(Context context, String url){
        this.mContext = context;
        this.isCopy = false;
        url = processURL(url);
        if (checkURL(url)) {
            this.invalid = false;
            UIMessages.showToast(mContext, "Extracting Data");
            extractData(url);
        }
        else{
            this.invalid = true;
            UIMessages.showToast(mContext,"Invalid URL");
            Log.e(TAG, "YTFile: Invalid URL");
        }
    }

    public YTFile(JSONObject json) throws JSONException {
        this.isCopy = true;
        this.title = (String) json.get(TITLE_KEY);
        this.artist = (String) json.get(ARTIST_KEY);
        this.dlurl = (String) json.get(DL_URL_KEY);
//        this.dlid = (long) json.get(DL_ID_KEY);
        this.imageURL = (String) json.get(IMAGE_URL_KEY);
        this.location = (String) json.get(LOCATION_KEY);
        this.extraction_status = (boolean) json.get(EXTRACTION_STATUS_KEY);
        this.embedImage = (boolean) json.get(EMBED_IMAGE_KEY);
        this.embedArtist = (boolean) json.get(EMBED_ARTIST_KEY);
        this.invalid = (boolean) json.get(INVALID_KEY);
    }

    /** Setters/Getters **/
    public String getUrl(){ return this.dlurl; }
    public String getTitle(){ return this.title; }
    public void setTitle(String title){this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getImageURL(){ return this.imageURL; }
    public String getLocation(){ return this.location; }
    public void setLocation(String location){ this.location = location; }
    public long getDlid(){ return this.dlid; }
    public void setDlid(long id ){ this.dlid = id; }
    public boolean embedImage(){return this.embedImage; }
    public void setEmbedImage(boolean embed){this.embedImage = embed; }
    public boolean embedArtist(){return this.embedArtist; }
    public void setEmbedArtist(boolean embed){this.embedArtist = embed; }
    public boolean isExtracted(){return this.extraction_status;}
    public boolean isInvalid() { return invalid; }
    public boolean isUrlEmpty(){ return (dlurl == null) || (dlurl.isEmpty()); }
    public boolean isEmpty(){return this.isEmpty; }

    protected abstract void postProcess();

    private String processURL(String url){
        final String yt_music_link = "music.";
        if (url.contains(yt_music_link)){
            url = url.replace(yt_music_link,"");
        }
        return url;
    }

    private boolean checkURL(String url){
        if (url.isEmpty()){
            return false;
        }
        else{
            return url.contains("://youtu.be/") || url.contains("youtube.com/watch?v=");
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void extractData(String url){
        if(!isCopy) {
            try {
                new YouTubeExtractor(mContext) {
                    @Override
                    protected void onExtractionComplete(@Nullable SparseArray<YtFile> ytFiles,
                                                        @Nullable VideoMeta videoMeta) {
                        Log.d(TAG, "onExtractionComplete: Post Extraction Process Starting...");
                        //Check if received extracted files
                        if (ytFiles == null) {
                            Log.w(TAG, "onExtractionComplete: ytFiles are empty");
                            isEmpty = true;
                            return;
                        }
                        //Search for valid video meta data
                        searchMetaData(ytFiles, videoMeta);
                    }
                }.extract(url);
            } catch (Exception e) {
                Log.e(TAG, "Failed to extract data: " + e.getMessage());
            }
        }
    }

    private void searchMetaData(@Nullable SparseArray<YtFile> ytFiles, @Nullable VideoMeta videoMeta){
        for (int i = 0, itag; i < ytFiles.size(); i++){
            Log.d(TAG, "onExtractionComplete: Getting iTag");
            //Get Youtube File from iTag
            itag = ytFiles.keyAt(i);
            YtFile ytFile = ytFiles.get(itag);

            //Check if Youtube File is assigned
            Log.d(TAG, "onExtractionComplete: checking if ytfile null");
            if (ytFile == null){
                Log.w(TAG, "onExtractionComplete: ytFile found to be NULL");
                continue;
            }

            //Check if Youtube File is an audio file
            Log.d(TAG, "onExtractionComplete: checking if ytfile is audio file");
            if (ytFile.getFormat().getHeight() != -1){
                continue;
            }
            Log.i(TAG, "onExtractionComplete: Audio iTag found: "+ itag);

            //Check Download Url Assignment
            dlurl = ytFile.getUrl();
            if (dlurl == null || dlurl.isEmpty()){
                Log.w(TAG, "onExtractionComplete: Failed to get download URL");
                continue;
            }

            //Check MetaData
            if(videoMeta == null){
                Log.w(TAG, "onExtractionComplete: Failed to get video metadata");
                continue;
            }

            //Get Title
            title = videoMeta.getTitle();
            if(title == null){
                title = "";
                Log.w(TAG, "onExtractionComplete: No Title Found");
            }

            //Get Thumbnail Image
            Log.d(TAG, "onExtractionComplete: Checking Thumbnail");
            imageURL = videoMeta.getMqImageUrl(); // 320 x 180
            if (imageURL == null){
                imageURL = "";
                Log.w(TAG, "onExtractionComplete: No Image Found");
            }
            break;
        }
        Log.d(TAG, "onExtractionComplete: Finished Checking itags");
        extraction_status = true;
        /**INSERT UI UPDATE ID HERE**/// Data Extracted
        postProcess();
    }

    @NonNull
    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put(TITLE_KEY,this.title);
            json.put(ARTIST_KEY,this.artist);
            json.put(DL_URL_KEY,this.dlurl);
//            json.put(DL_ID_KEY,this.dlid);
            json.put(IMAGE_URL_KEY,this.imageURL);
            json.put(LOCATION_KEY,this.location);
            json.put(EXTRACTION_STATUS_KEY,this.extraction_status);
            json.put(EMBED_IMAGE_KEY,this.embedImage);
            json.put(EMBED_ARTIST_KEY,this.embedArtist);
            json.put(INVALID_KEY,this.invalid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }
}
