package kanarious.musicnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class QuickActivity extends AppCompatActivity {

    private final String TAG = "QuickActivity";
    private YTFile ytFile;
    private Intent prev_intent = null;
    private Context mContext;
    private ViewGroup mainView;
    private final ArrayList<SongPanel> panels = new ArrayList<>();
    private DownloadForegroundService foregroundService;
    private boolean isServiceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownloadForegroundService.LocalBinder binder = (DownloadForegroundService.LocalBinder) service;
            foregroundService = binder.getService();
            isServiceBound = true;
            try {
                restoreSongPanels();
            } catch (Exception e){
                Log.e(TAG, "intentLaunch(): Failed to Restore Song Panels: ", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_download);
        setSupportActionBar(findViewById(R.id.MusicNowToolbar));
        SettingsController.initializeFile(this);
        NotificationCreator.createNotificationChannel(this,this.getClass());
        mContext = this;
        mainView = findViewById(R.id.MainView);

        //Start Service
        Intent intent = new Intent(this, DownloadForegroundService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.overflow_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.item_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //Hide App if there are any song panels
        if(panels.size()>0){
            moveTaskToBack(true);
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        intentLaunch();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void intentLaunch(){
        //Get and Check Intent
        Intent intent = getIntent();
        if (intent == null){
            Log.w(TAG, "intentLaunch: No intent Found");
            return;
        }
        if( (prev_intent != null) && (prev_intent == intent) ){
            Log.i(TAG, "intentLaunch: No new intent");
            return;
        }
        
        //Check for proper intent type
        if (Intent.ACTION_SEND.equals(intent.getAction()) &&
                intent.getType() != null &&
                intent.getType().equals("text/plain")) {
            //Save Intent
            prev_intent = intent;
            //Check Permissions
            if(StorageAccess.checkPermissions(this)){
                //Extract Youtube Video Data
                ytFile = new YTFile(this, intent.getStringExtra(Intent.EXTRA_TEXT)) {
                    @Override
                    protected void postProcess() {
                        if(!ytFile.isEmpty()) {
                            addNewSongPanel(ytFile);
                        }
                        else{
                            showSnackBar("Couldn't Find mp3 Data, try again");
                        }
                    }

                    @Override
                    protected void notifyExtraction(String message) {
                        showSnackBar(message);
                    }
                };
            }
        }
    }

    @Override
    protected void onDestroy() {
        //Destroy Panels
        for(SongPanel songPanel:panels){
            songPanel.destroy();
        }
        //Unbind Service
        stopService();
        //Clear Panel IDs
        IdTracker.clearIDs();
        super.onDestroy();
    }

    private void restoreSongPanels() throws JSONException {
        //Get YTFiles in String Format From Binded Service
        ArrayList<DownloadForegroundService.FileContainer> files = foregroundService.getYtfiles();

        //Bring All Panels Back
        for(DownloadForegroundService.FileContainer file:files){
            //Get YTFile Info for panel
            JSONObject inputJson = new JSONObject(file.getFile());
            YTFile ytFile = new YTFile(inputJson) {
                @Override
                protected void postProcess() {
                }
                @Override
                protected void notifyExtraction(String message) {
                }
            };
            SongPanel panel = addNewSongPanel(ytFile, file.getId());
            panel.setState(file.getState());
        }
    }

    private SongPanel addNewSongPanel(YTFile ytFile){
        //Get new ID
        int id = IdTracker.getID();
        return doAddSongPanel(ytFile,id);
   }

   private SongPanel addNewSongPanel(YTFile ytFile, int id){
        //Register In-use ID
        int checked_id = IdTracker.getID(id);
        return doAddSongPanel(ytFile,checked_id);
   }

    private void stopService(){
        if(isServiceBound && foregroundService != null){
            unbindService(serviceConnection);
        }
    }

    private SongPanel doAddSongPanel(YTFile ytFile, int id){
        //Create Panel
        SongPanel songPanel = new SongPanel(mContext, findViewById(R.id.MainLayout), id, ytFile) {
            @Override
            protected void closePanel(SongPanel songPanel) {
                panels.remove(songPanel);
                IdTracker.freeID(songPanel.getID());
                Transition t = TransitionInflater.from(mContext).inflateTransition(R.transition.song_panel_hide);
                TransitionManager.beginDelayedTransition(mainView,t);
                mainView.removeView(songPanel.getView());
            }
        };
        if(!panels.contains(songPanel)) {
            panels.add(songPanel);
        }

        //Initialize Panel
        songPanel.setArtistCheckBox(SettingsController.includeArtist());
        songPanel.setAlbumCheckBox(SettingsController.includeAlbumCover());
        if(SettingsController.autoExtractArtist()){
            songPanel.extractArtist();
        }
        if(SettingsController.removeTitleExtras()){
            songPanel.removeTitleExtras();
        }
        songPanel.setAutoCrop(SettingsController.autoCropAlbumCover());
        //Get View & Set Transition
        View songPanelView = songPanel.getView();
        Transition t = TransitionInflater.from(mContext).inflateTransition(R.transition.song_panel_trans);
        TransitionManager.beginDelayedTransition(mainView,t);
        //Show Panel
        mainView.addView(songPanelView,0);
        return songPanel;
    }

    private void showSnackBar(String message){
        CustomSnackbar snackBar = new CustomSnackbar(mContext,findViewById(R.id.MainLayout),message);
        if(message.equals("Extracting Data")){
            snackBar.setProgressVisible(true);
        }
        snackBar.show();
    }
}