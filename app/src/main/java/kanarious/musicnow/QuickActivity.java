package kanarious.musicnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
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
    private DownloadManager downloadManager;
    private Intent prev_intent = null;
    private Context mContext;
    private ViewGroup viewGroup;
    private ArrayList<SongPanel> panels = new ArrayList<>();

    private final int JOB_SERVICE_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_download);
        setSupportActionBar(findViewById(R.id.MusicNowToolbar));
        SettingsController.initializeFile(this);
        NotificationCreator.createNotificationChannel(this,this.getClass());
        mContext = this;
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        viewGroup = findViewById(R.id.MainView);

        //Create Job Service
        ComponentName componentName = new ComponentName(mContext,DownloadScheduler.class);
        JobInfo info = new JobInfo.Builder(JOB_SERVICE_ID,componentName)
                .build();
        JobScheduler scheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if(resultCode == JobScheduler.RESULT_SUCCESS){
            Log.d(TAG, "startService: Job Scheduled");
        }
        else{
            Log.e(TAG, "startService: Job Scheduling Failed");
        }
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
//            songPanel.destroyNotification();
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
                if(panels.contains(songPanel)){
                    panels.remove(songPanel);
                }
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
        Intent intent = new Intent(DownloadScheduler.DOWNLOAD_SERVICE);
        intent.putExtra(DownloadScheduler.DOWNLOAD_SERVICE,DownloadScheduler.ACTION_STOP_SERVICE);
        mContext.sendBroadcast(intent);
    }

}