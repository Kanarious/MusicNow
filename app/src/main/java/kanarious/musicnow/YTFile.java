package kanarious.musicnow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

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
    private int image_width = 0;
    private int image_height = 0;
    private int image_x = 0;
    private int image_y = 0;

    private static final String TITLE_KEY = "title";
    private static final String ARTIST_KEY = "artist";
    private static final String DL_URL_KEY = "dlurl";
    private static final String IMAGE_URL_KEY = "imageURL";
    private static final String LOCATION_KEY = "location";
    private static final String EXTRACTION_STATUS_KEY = "extraction_status";
    private static final String EMBED_IMAGE_KEY = "embedImage";
    private static final String EMBED_ARTIST_KEY = "embedArtist";
    private static final String INVALID_KEY = "invalid";
    private static final String IMAGE_HEIGHT_KEY = "imageHeight";
    private static final String IMAGE_WIDTH_KEY = "imageWidth";
    private static final String IMAGE_X_KEY = "ImageX";
    private static final String IMAGE_Y_KEY = "ImageY";

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
    public boolean isInvalid() { return invalid; }
    public boolean isEmpty(){return this.isEmpty; }
    public int getImageWidth(){return this.image_width; }
    public int getImageHeight(){return this.image_height; }
    public int getImageX(){return this.image_x; }
    public int getImageY(){return this.image_y; }
    public void setImageParams(int width, int height, int x, int y){
        this.image_width = width;
        this.image_height = height;
        this.image_x = x;
        this.image_y = y;
    }

    protected abstract void postProcess();
    protected abstract void notifyExtraction(String message);

    public YTFile(Context context, String url){
        this.mContext = context;
        this.isCopy = false;
        url = processURL(url);
        if (checkURL(url)) {
            this.invalid = false;
            notifyExtraction("Extracting Data");
            extractData(url);
        }
        else{
            this.invalid = true;
            notifyExtraction("Invalid URL");
            Log.e(TAG, "YTFile: Invalid URL");
        }
    }

    public YTFile(JSONObject json) throws JSONException {
        this.isCopy = true;
        this.title = (String) json.get(TITLE_KEY);
        this.artist = (String) json.get(ARTIST_KEY);
        this.dlurl = (String) json.get(DL_URL_KEY);
        this.imageURL = (String) json.get(IMAGE_URL_KEY);
        this.location = (String) json.get(LOCATION_KEY);
        this.extraction_status = (boolean) json.get(EXTRACTION_STATUS_KEY);
        this.embedImage = (boolean) json.get(EMBED_IMAGE_KEY);
        this.embedArtist = (boolean) json.get(EMBED_ARTIST_KEY);
        this.invalid = (boolean) json.get(INVALID_KEY);
        this.image_height = (int) json.get(IMAGE_HEIGHT_KEY);
        this.image_width = (int) json.get(IMAGE_WIDTH_KEY);
        this.image_x = (int) json.get(IMAGE_X_KEY);
        this.image_y = (int) json.get(IMAGE_Y_KEY);
    }

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
                            postProcess();
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
        for (int i = 0, itag; i < Objects.requireNonNull(ytFiles).size(); i++){
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
            json.put(IMAGE_URL_KEY,this.imageURL);
            json.put(LOCATION_KEY,this.location);
            json.put(EXTRACTION_STATUS_KEY,this.extraction_status);
            json.put(EMBED_IMAGE_KEY,this.embedImage);
            json.put(EMBED_ARTIST_KEY,this.embedArtist);
            json.put(INVALID_KEY,this.invalid);
            json.put(IMAGE_WIDTH_KEY,this.image_width);
            json.put(IMAGE_HEIGHT_KEY,this.image_height);
            json.put(IMAGE_X_KEY,this.image_x);
            json.put(IMAGE_Y_KEY,this.image_y);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }
}
