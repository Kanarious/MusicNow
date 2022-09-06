package kanarious.musicnow;

import android.app.DownloadManager;
import android.content.Context;
import android.util.Log;
import com.example.messagesutil.UIMessages;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Id3Editor {
    private final String TAG = "Id3Editor";
    private DownloadManager downloadManager;
    private Context context;
    private String filename;
    private String filepath;
    private String fullFilePath;
    private final String FILE_EXT = ".mp3";
    public static final String TEMP_NAME_ADD = "-ID3TEMP";

    public enum Contents{
        TITLE,
        COVER,
        ARTIST
    }

    private String Title;
    private String AlbumCover; //Full File Path
    private String Artist;

    private List<Contents> mContents;

    public void setTitle(String title) {
        Title = title;
        mContents.add(Contents.TITLE);
    }

    public void setAlbumCover(String albumCover){
        AlbumCover = albumCover;
        mContents.add(Contents.COVER);
    }

    public void setArtist(String artist){
        Artist = artist;
        mContents.add(Contents.ARTIST);
    }

    protected abstract void onId3EditFinish(String fullFilePath);
    /**
     * ID3 Editor Constructor
     * @param filename = full path included with file name (mp3 files only)
     */
    public Id3Editor(String filepath, String filename, DownloadManager downloadManager, Context context) throws Exception {
        if (filename.contains(FILE_EXT)) {
            this.downloadManager = downloadManager;
            this.context = context;
            this.filename = filename;
            this.filepath = filepath;
            this.fullFilePath = filepath+filename;
            if(!fileExists(fullFilePath)){
                throw new Exception("File does not exist");
            }
        }
        else{
            throw new Exception("Invalid File, Only mp3 Files allowed");
        }
        mContents = new ArrayList<Contents>();
    }

    /**
     * Add jpg image to mp3 file ID3 tag using url stream.
     * @throws InvalidDataException
     * @throws UnsupportedTagException
     * @throws IOException
     */
    public void embedData() throws InvalidDataException,
                                            UnsupportedTagException,
                                            IOException {
        //Open mp3 file
        Mp3File mp3File = new Mp3File(fullFilePath);

        //Get id3 tag
        ID3v2 id3v2Tag = getTag(mp3File);

        //Download Image
        String new_filename = filename.replace(TEMP_NAME_ADD,"");
        if(mContents.contains(Contents.COVER)) {
            String imageName = new_filename.replace(".mp3",".jpg");
            new ImageDownloader(downloadManager, context) {
                @Override
                protected void onImageDownloadFinish(String fullFilePath) {
                    try {
                        //Embed Contents
                        id3v2Tag.setAlbumImage(getImage(fullFilePath), "image/jpeg");
                        doEmbedData(mp3File, id3v2Tag, new_filename);
                    } catch (Exception e) {
                        Log.e(TAG, "imageDownloaderOnFinish: Failed to save mp3 file", e);
                    }
                    //Delete Downloaded Image
                    if (!new File(fullFilePath).delete()) {
                        Log.w(TAG, "imageDownloaderOnFinish: Failed to delete " + fullFilePath);
                    }
                }
            }.download(AlbumCover, imageName);
        }
        else{
            try {
                doEmbedData(mp3File, id3v2Tag, new_filename);
            } catch (Exception e){
                Log.e(TAG, "embedData: Failed to embed data: ",e);
            }

        }
    }

    private void doEmbedData(Mp3File mp3File, ID3v2 id3v2Tag, String new_filename) throws IOException, NotSupportedException {
        addContents(id3v2Tag);
        mp3File.save(filepath + new_filename);
        onId3EditFinish(filepath + new_filename);
        //delete temp mp3 file
        if (!new File(filepath + filename).delete()) {
            Log.w(TAG, "doEmbedData: Failed to delete " + filepath + filename);
        }
    }

    private void addContents(ID3v2 id3v2Tag) throws IOException {
        if(mContents.contains(Contents.TITLE)){
            id3v2Tag.setTitle(Title);
            id3v2Tag.setAlbum(Title);
        }
        if(mContents.contains(Contents.ARTIST)){
            id3v2Tag.setArtist(Artist);
        }
    }

    private boolean fileExists(String string) {
        File f = new File(string);
        return f.exists();
    }

    private ID3v2 getTag(Mp3File file){
        //Get Existing Tag if one exists
        ID3v2 id3v2Tag;
        if(file.hasId3v2Tag()){
            id3v2Tag = file.getId3v2Tag();
        }
        //Create New Tag
        else{
            id3v2Tag = new ID3v24Tag();
            file.setId3v2Tag(id3v2Tag);
        }
        return id3v2Tag;
    }

    private byte[] getImage(String fullFilePath) throws IOException {
        File file = new File(fullFilePath);
        int size = (int) file.length();
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        byte[] buffer = new byte[4096];
        byte[] fileBytes = new byte[size];
        int bytesRead;
        int currentIndex = 0;
        while( (bytesRead = inputStream.read(buffer)) != -1 ){
            for (int i = 0; i < bytesRead; i++){
                fileBytes[currentIndex] = buffer[i];
                currentIndex++;
            }
        }
        inputStream.close();
        return fileBytes;
    }
}
