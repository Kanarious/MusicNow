package kanarious.musicnow;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public enum PanelUpdates {
    FAIL(0),
    START(1),
    FINISH(2),
    CANCEL(3),
    PROGRESS(4),
    META(5),
    ERROR(-99);

    public static final String PANEL_UPDATE = "PANEL_UPDATE";
    public static final String PANEL_ID = "PANEL_ID";

    private final int value;
    PanelUpdates(int value) {
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    protected static void sendUpdate(PanelUpdates update, Handler handler, Context context, String title, int id){
        Intent updateIntent = new Intent(PanelUpdates.PANEL_UPDATE);
        updateIntent.putExtra(PanelUpdates.PANEL_UPDATE,update.getValue());
        updateIntent.putExtra(PanelUpdates.PANEL_ID,id);
        context.sendBroadcast(updateIntent);

        switch (update){
            case START:{
                break;
            }
            case FINISH:{
                handler.post(() -> NotificationCreator.notifyFinished(id,title,"Download Finished"));
                break;
            }
            case CANCEL:{
                handler.post(()->NotificationCreator.notifyCancel(id,title,"Download Cancelled"));
                break;
            }
            case FAIL:{
                handler.post(() -> NotificationCreator.notifyFailed(id,title,"Download Failed"));
                break;
            }
            case META:{
                handler.post(() -> NotificationCreator.notifyDownload(id,title,"Embedding Data"));
                break;
            }
        }
    }

    protected static void sendUpdate(PanelUpdates update, Handler handler, Context context, String title, int id, int progress){
        sendUpdate(update, handler, context, title, id);
        if(update == PanelUpdates.PROGRESS){
            handler.post(()->NotificationCreator.notifyDownloadProgress(id,title,"Downloading",progress));
        }
    }
}
