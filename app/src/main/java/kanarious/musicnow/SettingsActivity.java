package kanarious.musicnow;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {

    private Switch includeArtistSWT;
    private Switch extractArtistSWT;
    private Switch includeCoverSWT;
    private Switch cropCoverSWT;
    private Switch removeExtrasSWT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SettingsController.initializeFile(this);

        //Get Views
        includeArtistSWT = findViewById(R.id.IncludeArtistSWT);
        extractArtistSWT = findViewById(R.id.ExtractArtistSWT);
        includeCoverSWT = findViewById(R.id.IncludeCoverSWT);
        cropCoverSWT = findViewById(R.id.CropCoverSWT);
        removeExtrasSWT = findViewById(R.id.RemoveExtrasSWT);

        //Set View States
        includeArtistSWT.setChecked(SettingsController.includeAlbumCover());
        extractArtistSWT.setChecked(SettingsController.autoExtractArtist());
        includeCoverSWT.setChecked(SettingsController.includeAlbumCover());
        cropCoverSWT.setChecked(SettingsController.autoCropAlbumCover());
        removeExtrasSWT.setChecked(SettingsController.removeTitleExtras());

        //Set onClick listeners
        includeArtistSWT.setOnClickListener(l->{
            SettingsController.setIncludeArtist(includeArtistSWT.isChecked());
        });
        extractArtistSWT.setOnClickListener(l->{
            SettingsController.setAutoExtractArtist(extractArtistSWT.isChecked());
        });
        includeCoverSWT.setOnClickListener(l->{
            SettingsController.setIncludeAlbumCover(includeCoverSWT.isChecked());
        });
        cropCoverSWT.setOnClickListener(l->{
            SettingsController.setAutoCropCover(cropCoverSWT.isChecked());
        });
        removeExtrasSWT.setOnClickListener(l->{
            SettingsController.setRemoveTitleExtras(removeExtrasSWT.isChecked());
        });
    }

    @Override
    protected void onStop() {
        SettingsController.updateFile(this);
        super.onStop();
    }
}