package io.harpseal.pomodorowear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

/**
 * Created by Harpseal on 18/1/7.
 */

public class AlarmReceiver extends BroadcastReceiver
{
    final String TAG = AlarmReceiver.class.getName();
    @Override
    public void onReceive(Context context, Intent intent)
    {
        long [] vibrate_array = null;
        Bundle bData = intent.getExtras();
        if(bData.get("msg").equals("play_tomato_alarm"))
        {
            //vibrate_array = new long[]{10, 150};
            vibrate_array = new long[]{10, 200, 200, 200, 500, 250};
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            //v.vibrate(100);
            v.vibrate(vibrate_array, -1);
            Log.d(TAG,"play_tomato_alarm " + v.hasVibrator());

//            try {
//                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                Ringtone r = RingtoneManager.getRingtone(context, notification);
//                r.play();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
        else if(bData.get("msg").equals("play_tomato_warning"))
        {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            v.vibrate(500);
            Log.d(TAG,"play_tomato_warning");
        }
    }
}
