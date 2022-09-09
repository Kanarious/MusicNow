package kanarious.musicnow;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class CustomSnackbar{
    private final View customSnackView;
    private final Context mContext;
    private final View parent;
    private final TextView messageText;
    private final ProgressBar progressBar;


    CustomSnackbar(Context context, View parent, String message){
        this.mContext = context;
        this.parent = parent;
        this.customSnackView = LayoutInflater.from(mContext).inflate(R.layout.custom_snackbar,null);
        this.messageText = customSnackView.findViewById(R.id.Snackbar_Message);
        this.progressBar = customSnackView.findViewById(R.id.Snackbar_ProgressBar);

        messageText.setText(message);
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void setProgressVisible(boolean visible){
        if(visible) {
            progressBar.setVisibility(View.VISIBLE);
        }
        else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void show(){
        Snackbar snackbar = Snackbar.make(parent,"",Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(Color.TRANSPARENT);
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
        snackbarLayout.setPadding(0,0,0,0);
        snackbarLayout.addView(customSnackView,0);
        snackbar.show();
    }

}
