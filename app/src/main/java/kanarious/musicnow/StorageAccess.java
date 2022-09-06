package kanarious.musicnow;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.File;

public class StorageAccess {
    private final static String TAG = "StorageAccess";

    public static boolean checkPermissions(Activity activity){
        boolean result = true;
        //In-App write permission
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Write Permission is granted");
        } else {
            Log.v(TAG, "Write Permission is revoked");
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            result = false;
        }

        //In-App read permission
        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Read Permission is granted");
        } else {
            Log.v(TAG, "Read Storage Permission is revoked");
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            result = false;
        }
        return result;
    }

    public static String getDownloadsFolder(){
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(); //DEPRECATED
        return path+'/';
    }

    public static String getFilesFolder(Context context){
        File path = context.getFilesDir();
        String dir = path.getAbsolutePath();
        return dir+"/";
    }
}
