package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public class MessageHelper{
    private static final String TAG = "relevamiento-territorial";

    public static void createAndShowDialog(final Activity activity, final String message, final String title) {
        final int duration = Toast.LENGTH_LONG;

        if(activity!=null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(activity, message, duration);
                    toast.show();
                }
            });
        } else {
            Log.e(TAG, message);
        }

    }
    public static void createAndShowDialog(Activity activity, Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(activity, ex.getMessage(), title);
    }

}
