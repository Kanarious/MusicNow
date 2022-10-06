package kanarious.musicnow;

import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

public abstract class SongPanel {
    private final String TAG;
    private final Context mContext;
    private final View view;
    private final View parent;
    private final CheckBox artistCheckBox;
    private final CheckBox albumCheckBox;
    private final EditText artistEditText;
    private final EditText titleEditText;
    private final ImageButton imageButton;
    private final ProgressBar loadBar;
    private final ImageView albumView;
    private final ImageButton swapButton;
    private final YTFile ytFile;
    private final BroadcastReceiver updates;
    private Bitmap original_cover = null;

    private boolean autocrop = false;

    public void setAutoCrop(boolean crop){ autocrop = crop; }

    private enum ButtonStates{
        DOWNLOAD,
        CANCEL,
        RETRY,
        DONE,
        LOAD
    }

    private ButtonStates buttonState;
    private final int ID;
    protected abstract void closePanel(SongPanel songPanel);

    public SongPanel(Context context, View parent, int id, YTFile file){
        TAG = "SongPanel"+id;
        mContext = context;
        ytFile = file;
        ID = id;
        view = LayoutInflater.from(mContext).inflate(R.layout.song_panel, null);
        this.parent = parent;
        SongPanel self = this;

        //Assign GUI Elements
        artistEditText = view.findViewById(R.id.ArtistEditText);
        titleEditText = view.findViewById(R.id.TitleEditText);
        albumCheckBox = view.findViewById(R.id.AlbumCheckBox);
        artistCheckBox = view.findViewById(R.id.ArtistCheckBox);
        imageButton = view.findViewById(R.id.ImageButton);
        loadBar = view.findViewById(R.id.LoadBar);
        albumView = view.findViewById(R.id.AlbumView);
        swapButton = view.findViewById(R.id.SwapButton);
        ImageButton closePanelBTN = view.findViewById(R.id.ClosePanelBTN);

        //Create Broadcast Receiver
        updates = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int id = intent.getIntExtra(PanelUpdates.PANEL_ID,PanelUpdates.ERROR.getValue());
                if(!(ID == id)){
                    return;
                }
                int update_code = intent.getIntExtra(PanelUpdates.PANEL_UPDATE,PanelUpdates.ERROR.getValue());
                if(update_code == PanelUpdates.START.getValue()){
                    setButtonState(ButtonStates.CANCEL);
                }
                else if (update_code == PanelUpdates.CANCEL.getValue()){
                    setButtonState(ButtonStates.RETRY);
                }
                else if (update_code == PanelUpdates.FAIL.getValue()){
                    setButtonState(ButtonStates.RETRY);
                    CustomSnackbar snackBar = new CustomSnackbar(mContext,parent,"Failed to download");
                    snackBar.show();
                }
                else if (update_code == PanelUpdates.FINISH.getValue()){
                    setButtonState(ButtonStates.DONE);
                    CustomSnackbar snackBar = new CustomSnackbar(mContext,parent,"Download Finished");
                    snackBar.show();
                }
            }
        };
        mContext.registerReceiver(updates,new IntentFilter(PanelUpdates.PANEL_UPDATE));

        //Initialize GUI Elements
        setButtonState(ButtonStates.DOWNLOAD);
        titleEditText.setText(ytFile.getTitle().trim(), TextView.BufferType.EDITABLE);

        //Set OnClick Listeners
        artistCheckBox.setOnClickListener(v -> {
            CheckBox checkBox = (CheckBox)v;
            artistEditText.setEnabled(checkBox.isChecked());
        });

        albumView.setOnClickListener(v ->{
            cropImage();
        });

        swapButton.setOnClickListener(v ->{
            swapTitleArtist();
        });

        imageButton.setOnClickListener(v-> {
            switch (buttonState){
                case DOWNLOAD:
                case RETRY:
                case DONE: {
                    setButtonState(ButtonStates.LOAD);
                    prepYTFile();
                    startDownload(ytFile);
                    break;
                }
                case CANCEL:{
                    setButtonState(ButtonStates.LOAD);
                    stopDownload();
                    break;
                }
                default: break;
            }
        });

        closePanelBTN.setOnClickListener(v -> {
            switch (buttonState){
                case LOAD:
                case CANCEL: {
                    break;
                }
                case DOWNLOAD:
                case DONE:
                case RETRY:{
                    closePanel(self);
                }

            }
        });

         new displayImageTask(albumView) {
            @Override
            protected void displayFinished(Bitmap bm) {
                original_cover = bm;
                if(autocrop){
                    cropImage();
                }
            }
        }.execute(file.getImageURL());
    }

    public void destroy(){
        mContext.unregisterReceiver(updates);
    }

    private void prepYTFile(){
        //Album Image
        ytFile.setEmbedImage(albumCheckBox.isChecked());

        //Song Title
        String title = titleEditText.getText().toString();
        if(title.isEmpty()){
            title = "title";
        }
        ytFile.setTitle(title);

        //Artist
        if(artistCheckBox.isChecked()){
            String artistName = artistEditText.getText().toString();
            if (!artistName.isEmpty()){
                ytFile.setArtist(artistName);
                ytFile.setEmbedArtist(true);
            }
        }
    }

    private void setButtonState(ButtonStates buttonState){
        //Set Button Image
        switch (buttonState){
            case DOWNLOAD:{
                imageButton.setImageResource(R.drawable.download_button_selector);
                loadBar.setVisibility(View.INVISIBLE);
                break;
            }
            case RETRY:{
                imageButton.setImageResource(R.drawable.retry_button_selector);
                loadBar.setVisibility(View.INVISIBLE);
                break;
            }
            case CANCEL:{
                imageButton.setImageResource(R.drawable.cancel_button_selector);
                loadBar.setVisibility(View.VISIBLE);
                break;
            }
            case DONE:{
                imageButton.setImageResource(R.drawable.done_button_selector);
                loadBar.setVisibility(View.INVISIBLE);
                break;
            }
            default: break;
        }
        this.buttonState = buttonState;
    }

    public View getView(){
        return this.view;
    }

    public int getID(){
        return this.ID;
    }

    public void setArtistCheckBox(boolean state){
        this.artistCheckBox.setChecked(state);
        this.artistEditText.setEnabled(state);
    }

    public void setAlbumCheckBox(boolean state){
        this.albumCheckBox.setChecked(state);
    }

    public void extractArtist(){
        //Get File Title String
        String text = this.ytFile.getTitle();

        //Check if title contains format
        if(text.contains("-")){
            //Get dash index
            int index = text.indexOf("-");
            //Find end of artist index
            int end_artist_index = index;
            do {
                end_artist_index = end_artist_index - 1;
            }while (text.charAt(end_artist_index) == ' ');
            //Find title start index
            int start_title_index = index;
            do {
                start_title_index = start_title_index + 1;
            }while (text.charAt(start_title_index) == ' ');
            //Grab everything before dash for artist
            String artist = text.substring(0,end_artist_index+1).trim();
            //Grab everything after dash for title
            String title = text.substring(start_title_index).trim();
            //Set text
            titleEditText.setText(title, TextView.BufferType.EDITABLE);
            artistEditText.setText(artist, TextView.BufferType.EDITABLE);
        }
    }

    private String removeTitleExtra(String title, String start_bracket, String end_bracket){
        if(title.contains(start_bracket) && title.contains(end_bracket)){
            while(title.contains(start_bracket) && title.contains(end_bracket)){
                int start_index = title.indexOf(start_bracket);
                int end_index = title.indexOf(end_bracket);
                String extra_text = title.substring(start_index,end_index+1);
                title = title.replace(extra_text,"").trim();
            }
            return title;
        }
        else {
            return title;
        }
    }

    public void removeTitleExtras(){
        //Get Title Field String
        String title = titleEditText.getText().toString();

        //Remove Title Extras
        title = removeTitleExtra(title,"[","]");
        title = removeTitleExtra(title,"(",")");

        titleEditText.setText(title,TextView.BufferType.EDITABLE);
    }

    private void swapTitleArtist(){
        String title = titleEditText.getText().toString();
        String artist = artistEditText.getText().toString();
        titleEditText.setText(artist,TextView.BufferType.EDITABLE);
        artistEditText.setText(title,TextView.BufferType.EDITABLE);
    }

    private Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public void cropImage(){
        if(original_cover == null){
            Log.e(TAG, "cropImage: No image available for cropping");
            return;
        }

        /*LATER OPEN CROPPER TO GET NEW CROPPING COORDINATES, CURRENTLY CROP 1:1 RATIO*/

        Bitmap bm = ((BitmapDrawable)albumView.getDrawable()).getBitmap();
        //Bitmap width and height are equal therefore undo 1:1 ratio
        if(bm.getWidth() == bm.getHeight()){
            bm = original_cover;
            ytFile.setImageParams(bm.getWidth(),bm.getHeight(),0,0);
            albumView.setImageBitmap(bm);
        }
        //Crop Bitmap to 1:1 Ratio
        else{
            float width = bm.getWidth();
            float height = bm.getHeight();
            int new_width;
            int new_height;
            int x_starting_point;
            int y_starting_point;

            //Create New Bitmap
            if(width > height){
                new_width = (int) height;
                new_height = (int) height;
                x_starting_point = round((width - height)/2);
                y_starting_point = 0;
            }
            else{
                new_height = (int) width;
                new_width = (int) width;
                x_starting_point = 0;
                y_starting_point = round((height - width)/2);
            }
            bm = Bitmap.createBitmap(original_cover,x_starting_point,y_starting_point,new_width,new_height);

            //Set New Image Parameters to YTFile
            ytFile.setImageParams(new_width,new_height,x_starting_point,y_starting_point);

            //Round Out Bitmap for Panel Visuals
            RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(mContext.getResources(),bm);
            dr.setCornerRadius(30);
            bm = drawableToBitmap(dr);

            //Show Bitmap
            albumView.setImageBitmap(bm);
        }

    }

    private void startDownload(YTFile ytFile){
        Intent serviceIntent = new Intent(mContext,DownloadForegroundService.class);
        serviceIntent.putExtra(DownloadForegroundService.DOWNLOAD_SERVICE,DownloadForegroundService.ACTION_START_THREAD);
        serviceIntent.putExtra(DownloadForegroundService.YTFILE,ytFile.toString());
        serviceIntent.putExtra(DownloadForegroundService.PANEL_ID,this.ID);
        mContext.startService(serviceIntent);
    }

    private void stopDownload(){
        Intent serviceIntent = new Intent(mContext,DownloadForegroundService.class);
        serviceIntent.putExtra(DownloadForegroundService.DOWNLOAD_SERVICE,DownloadForegroundService.ACTION_STOP_THREAD);
        serviceIntent.putExtra(DownloadForegroundService.PANEL_ID,this.ID);
        mContext.startService(serviceIntent);
    }

    public void setState(PanelUpdates state){
        switch (state){
            case START:{
                setButtonState(ButtonStates.DOWNLOAD);
                break;
            }
            case META:
            case PROGRESS:
            case CANCEL:{
                setButtonState(ButtonStates.CANCEL);
                break;
            }
            case FINISH:{
                setButtonState(ButtonStates.DONE);
            }
            case FAIL:
            case ERROR:{
                setButtonState(ButtonStates.RETRY);
            }
        }
    }

}