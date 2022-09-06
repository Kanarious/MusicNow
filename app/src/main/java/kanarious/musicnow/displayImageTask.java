package kanarious.musicnow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import java.io.InputStream;

public class displayImageTask extends AsyncTask {
    private final String TAG = "displayImageTask";
    private ImageView view;

    public displayImageTask(ImageView view){
        this.view = view;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        String url = (String) objects[0];
        Bitmap image = null;
        try{
            InputStream ins = new java.net.URL(url).openStream();
            image = BitmapFactory.decodeStream(ins);
        } catch (Exception e) {
            Log.e(TAG, "doInBackground: ",e);
        }
        return image;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        view.setImageBitmap((Bitmap) o);
    }
}
