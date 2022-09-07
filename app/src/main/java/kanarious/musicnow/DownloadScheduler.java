package kanarious.musicnow;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class DownloadScheduler extends JobService {
    private static final String TAG = "DownloadScheduler";

    @Override
    public boolean onStartJob(JobParameters params) {
        params.getExtras().getString();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
