package kanarious.musicnow;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import java.io.InputStream;

public abstract class displayImageTask extends AsyncTask {
    private final String TAG = "displayImageTask";
    @SuppressLint("StaticFieldLeak")
    private final ImageView view;

    protected abstract void displayFinished(Bitmap bm);

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
//        super.onPostExecute(o);
        Bitmap bm = (Bitmap) o;
        view.setImageBitmap(bm);
        displayFinished(bm);
    }
}
