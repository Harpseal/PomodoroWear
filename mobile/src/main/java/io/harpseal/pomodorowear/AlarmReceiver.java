package io.harpseal.pomodorowear;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Harpseal on 18/1/7.
 */

public class AlarmReceiver extends BroadcastReceiver
{
    final String TAG = AlarmReceiver.class.getName();

    public final String NOTIFICATION_ID = TAG + "-notification-id";
    public final String NOTIFICATION = TAG + "-notification";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        long [] vibrate_array = null;
        Bundle bData = intent.getExtras();

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Long timeInMillis = System.currentTimeMillis();
        SimpleDateFormat timeTxtFormat = new SimpleDateFormat("HH:mm:ss");
        String dateText = timeTxtFormat.format(new Date(timeInMillis));

        String noTitle = "";
        String noText = "";
        //vibrate_array = new long[]{10, 200, 200, 200, 500, 250};
        //Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if(bData.get("msg").equals("play_tomato_alarm"))
        {
            noTitle = context.getResources().getString(R.string.text_timer_mode_out_of_time) +
                    context.getResources().getString(R.string.text_timer_mode_relax) +
                    context.getResources().getString(R.string.text_timer_postfix_doing);
            noText = context.getResources().getString(R.string.text_timer_mode_relax) +
                    context.getResources().getString(R.string.text_builder_time_end) + " @ " + dateText;
            //Log.d(TAG,"play_tomato_alarm " + v.hasVibrator());
        }
        else if(bData.get("msg").equals("play_tomato_warning"))
        {
            noTitle = context.getResources().getString(R.string.text_timer_mode_out_of_time) +
                    context.getResources().getString(R.string.text_timer_mode_work) +
                    context.getResources().getString(R.string.text_timer_postfix_doing);
            noText = context.getResources().getString(R.string.text_timer_mode_work) +
                    context.getResources().getString(R.string.text_builder_time_end) + " @ " + dateText;
            Log.d(TAG,"play_tomato_warning");
        }

        if (noText.length() != 0 || noTitle.length() != 0)
        {
            Intent timerIntent = new Intent(context, io.harpseal.pomodorowear.MainMobileTimerActivity.class);
            timerIntent.setAction(Intent.ACTION_MAIN);
            timerIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    timerIntent, 0);

            Notification notification = new Notification.Builder(context)
                    //.setPriority(NotificationManager.IMPORTANCE_HIGH)
                    .setContentIntent(pendingIntent)
                    .setContentTitle(noTitle)
                    .setContentText(noText)
                    .setSmallIcon(R.mipmap.icon_tomato_color)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build();

//            NotificationChannel chan2 = new NotificationChannel(NOTIFICATION_ID,
//                    NOTIFICATION, NotificationManager.IMPORTANCE_HIGH);
////            chan2.setLightColor(Color.BLUE);
////            chan2.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//            mNotificationManager.createNotificationChannel(chan2);

            mNotificationManager.notify(
                    NOTIFICATION_ID,0,  // <-- Place your notification id here
                    notification);
        }

        //v.vibrate(vibrate_array, -1);
    }
}
