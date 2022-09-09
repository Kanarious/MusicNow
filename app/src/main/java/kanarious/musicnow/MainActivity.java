package kanarious.musicnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends AppCompatActivity {
    private Context mContext;
    private final String TAG = "MainActivity";
    ViewGroup viewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.MusicNowToolbar));
        SettingsController.initializeFile(this);
        NotificationCreator.createNotificationChannel(this,this.getClass());
        mContext = this;
        viewGroup = (ViewGroup)findViewById(R.id.MainView);
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

    private void addNewSongPanel(){
//        View songPanelView = new SongPanel(mContext).getView();
//        //Set Transition
//        Transition t = TransitionInflater.from(mContext).inflateTransition(R.transition.song_panel_trans);
//        TransitionManager.beginDelayedTransition(viewGroup,t);
//        viewGroup.addView(songPanelView,0);
    }

    public void testClick(View v){

    }
}