package kanarious.musicnow;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SettingsController {
    private static final String TAG = "SettingsController";
    private static final int version = 0;
    private static final String filename = "settings.json";
    private static final String VERSION_KEY = "version";
    private static final String INCLUDE_ARTIST_KEY = "IncludeArtist";
    private static final String AUTO_EXTRACT_ARTIST_KEY = "ExtractArtist";
    private static final String INCLUDE_COVER_KEY = "IncludeCover";
    private static final String AUTO_CROP_COVER_KEY = "CropCover";

    private static int read_version = -1;
    private static boolean include_artist = false;
    private static boolean auto_extract_artist = false;
    private static boolean include_cover = false;
    private static boolean auto_crop_cover = false;

    private static String getFilePath(Context context){
        return StorageAccess.getFilesFolder(context)+filename;
    }

    private static void createFile(Context context) throws JSONException, IOException {
        JSONObject json = new JSONObject();
        json.put(VERSION_KEY,version);

        json.put(INCLUDE_ARTIST_KEY,include_artist);
        json.put(AUTO_EXTRACT_ARTIST_KEY,auto_extract_artist);
        json.put(INCLUDE_COVER_KEY,include_cover);
        json.put(AUTO_CROP_COVER_KEY,auto_crop_cover);

        File file = new File(getFilePath(context));
        FileWriter fileWriter = new FileWriter(file,false);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(json.toString());
        bufferedWriter.close();
    }

    private static void readSettingsFile(Context context) throws IOException, JSONException {
        //Create Reader
        File file = new File(getFilePath(context));
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuilder stringBuilder = new StringBuilder();

        //Read JSON File
        String line = bufferedReader.readLine();
        while(line != null){
            stringBuilder.append(line).append("\n");
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        String contents = stringBuilder.toString();

        //Create JSON Object
        JSONObject json = new JSONObject(contents);

        //Record JSON Contents
        read_version = (int)json.get(VERSION_KEY);
        if (read_version > -1) {
            include_artist = (boolean) json.get(INCLUDE_ARTIST_KEY);
            auto_extract_artist = (boolean) json.get(AUTO_EXTRACT_ARTIST_KEY);
            include_cover = (boolean) json.get(INCLUDE_COVER_KEY);
            auto_crop_cover = (boolean) json.get(AUTO_CROP_COVER_KEY);
        }
    }

    /**
     * Check if file exists, if it doesn't then create default settings file.
     * if the file exists then the json values will be saved to be read
     * faster for later app use.
     */
    public static void initializeFile(Context context){
        File file = new File(getFilePath(context));
        if (file.exists()){
            try {
                readSettingsFile(context);
            }catch (Exception e){
                Log.e(TAG, "initializeFile: Failed to read settings file: ",e);
            }
        }
        else{
            try {
                createFile(context);
            } catch(Exception e){
                Log.e(TAG, "initializeFile: Failed to create default file: ",e);
            }
        }
    }

    public static void updateFile(Context context){
        try {
            createFile(context);
        } catch(Exception e){
            Log.e(TAG, "initializeFile: Failed to create default file: ",e);
        }
    }

    public static void setIncludeArtist(boolean value){
        include_artist = value;
    }

    public static void setAutoExtractArtist(boolean value){
        auto_extract_artist = value;
    }

    public static void setIncludeAlbumCover(boolean value){
        include_cover = value;
    }

    public static void setAutoCropCover(boolean value){
        auto_crop_cover = value;
    }

    public static boolean includeArtist(){
        return include_artist;
    }

    public static boolean autoExtractArtist(){
        return auto_extract_artist;
    }

    public static boolean includeAlbumCover(){
        return include_cover;
    }

    public static boolean autoCropAlbumCover(){
        return auto_crop_cover;
    }
}
