package kanarious.musicnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
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

import com.example.messagesutil.UIMessages;

import java.util.ArrayList;

public class QuickActivity extends AppCompatActivity {

    private final String TAG = "QuickActivity";
    private YTFile ytFile;
    private Intent prev_intent = null;
    private Context mContext;
    private ViewGroup viewGroup;
    private final ArrayList<SongPanel> panels = new ArrayList<>();
    private DownloadForegroundService foregroundService;
    private boolean isServiceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownloadForegroundService.LocalBinder binder = (DownloadForegroundService.LocalBinder) service;
            foregroundService = binder.getService();
            isServiceBound = true;
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
        viewGroup = findViewById(R.id.MainView);

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
                        if(!ytFile.isInvalid()) {
                            if(!ytFile.isEmpty()) {
                                addNewSongPanel(ytFile);
                            }
                            else{
                                UIMessages.showToast(mContext,"Couldn't Find mp3 Data, try again");
                            }
                        }
                        else{
                            UIMessages.showToast(mContext,"INVALID URL");
                        }
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
        super.onDestroy();
    }

    private void addNewSongPanel(YTFile ytFile){
        //Get new ID
        int id = IdTracker.getID();
        //Create Panel
        SongPanel songPanel = new SongPanel(mContext, id, ytFile) {
            @Override
            protected void closePanel(SongPanel songPanel) {
                panels.remove(songPanel);
                IdTracker.freeID(songPanel.getID());
                Transition t = TransitionInflater.from(mContext).inflateTransition(R.transition.song_panel_hide);
                TransitionManager.beginDelayedTransition(viewGroup,t);
                viewGroup.removeView(songPanel.getView());
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
        if(SettingsController.autoCropAlbumCover()){
            songPanel.cropImage();
        }
        //Get View & Set Transition
        View songPanelView = songPanel.getView();
        Transition t = TransitionInflater.from(mContext).inflateTransition(R.transition.song_panel_trans);
        TransitionManager.beginDelayedTransition(viewGroup,t);
        //Show Panel
        viewGroup.addView(songPanelView,0);
    }

    private void stopService(){
        if(isServiceBound && foregroundService != null){
            unbindService(serviceConnection);
        }
    }

}