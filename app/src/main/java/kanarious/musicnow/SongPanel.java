package kanarious.musicnow;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class SongPanel {
    private Context mContext;
    private View view;
    private CheckBox artistCheckBox;
    private CheckBox albumCheckBox;
    private EditText artistEditText;
    private EditText titleEditText;
    private ImageButton imageButton;
    private ImageButton closePanelBTN;
    private ProgressBar loadBar;
    private ImageView albumView;
    private YTFile ytFile;
    private YTDL ytdl;

    private enum ButtonStates{
        DOWNLOAD,
        CANCEL,
        RETRY,
        DONE,
        LOAD
    }

    private ButtonStates buttonState;
    private int ID;
    protected abstract void closePanel(SongPanel songPanel);

    public SongPanel(Context context, int id, YTFile file, DownloadManager downloadManager){
        mContext = context;
        ytFile = file;
        ID = id;
        view = LayoutInflater.from(mContext).inflate(R.layout.song_panel,null);
        SongPanel self = this;

        //Assign GUI Elements
        artistEditText = view.findViewById(R.id.ArtistEditText);
        titleEditText = view.findViewById(R.id.TitleEditText);
        albumCheckBox = view.findViewById(R.id.AlbumCheckBox);
        artistCheckBox = view.findViewById(R.id.ArtistCheckBox);
        imageButton = view.findViewById(R.id.ImageButton);
        loadBar = view.findViewById(R.id.LoadBar);
        albumView = view.findViewById(R.id.AlbumView);
        closePanelBTN = view.findViewById(R.id.ClosePanelBTN);

        //Create YTDL
        ytdl = new YTDL(mContext, downloadManager) {
            @Override
            protected void onDownloadFail() {
                setButtonState(ButtonStates.RETRY);
                NotificationCreator.notifyFailed(ID,ytFile.getTitle(),"Download Failed");
            }

            @Override
            protected void onDownloadStart() {
                setButtonState(ButtonStates.CANCEL);
                NotificationCreator.notifyDownload(ID,ytFile.getTitle(),"Downloading");
            }

            @Override
            protected void onDownloadCanceled() {
                setButtonState(ButtonStates.RETRY);
                NotificationCreator.notifyCancel(ID,ytFile.getTitle(),"Download Cancelled");
            }

            @Override
            protected void onDownloadFinish() {
                setButtonState(ButtonStates.DONE);
                NotificationCreator.notifyFinished(ID,ytFile.getTitle(),"Download Finished");
            }
        };

        //Initialize GUI Elements
        setButtonState(ButtonStates.DOWNLOAD);
        titleEditText.setText(ytFile.getTitle(), TextView.BufferType.EDITABLE);

        //Set OnClick Listeners
        artistCheckBox.setOnClickListener(v -> {
            CheckBox checkBox = (CheckBox)v;
            artistEditText.setEnabled(checkBox.isChecked());
        });

        imageButton.setOnClickListener(v-> {
            switch (buttonState){
                case DOWNLOAD:
                case RETRY:
                case DONE: {
                    setButtonState(ButtonStates.LOAD);
                    prepYTFile();
                    ytdl.download(ytFile);
//                    startService(ytFile);
                    break;
                }
                case CANCEL:{
                    setButtonState(ButtonStates.LOAD);
                    ytdl.cancelDownload(ytFile);
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

        new displayImageTask(albumView).execute(file.getImageURL());
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
            String artist = text.substring(0,end_artist_index+1);
            //Grab everything after dash for title
            String title = text.substring(start_title_index);
            //Set text
            titleEditText.setText(title, TextView.BufferType.EDITABLE);
            artistEditText.setText(artist, TextView.BufferType.EDITABLE);
        }


    }

    public void cropImage(){

    }

    public void destroyNotification(){
        if(this.buttonState == ButtonStates.LOAD){
            NotificationCreator.notifyCancel(ID,"Couldn't Finish","App Closed");
        }
    }

    private void startService(YTFile ytFile){
        Intent serviceIntent = new Intent(mContext,DownloadService.class);
        serviceIntent.putExtra(Intent.EXTRA_TEXT,ytFile.toString());
        mContext.startService(serviceIntent);
    }

}