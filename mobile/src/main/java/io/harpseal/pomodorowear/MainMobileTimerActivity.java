package io.harpseal.pomodorowear;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainMobileTimerActivity extends Activity {

    final String TAG = MainMobileTimerActivity.class.getName();
    enum ProgressMode
    {
        NORMAL,WARNING,ERROR,IDLE
    }



    private Button mBtnSettings;
    private Button mBtnPrevious;
    private Button mBtnPlay;
    private Button mBtnStop;
    private Button mBtnNext;

    private TextView mTextCal;
    private TextView mTextCalPrefix;
    private TextView mTextType;
    private TextView mTextMin;
    private TextView mTextSec;
    private TextView mTextCenter;

    private boolean mIsForceScrAlwaysOn;
    private boolean mIsScrAlwaysOn;
    private boolean mIsScrAlwaysOnCharging;
    private long mDataTomatoPowerUpdated;
    private ImageView mImageScrAlwaysOnPower;
    private ImageView mImageScrAlwaysOnSun;
    private ImageView mImageScrAlwaysOnLock;
    private LinearLayout mLayoutScrAlwaysOn;
    //private TextView mTextMinAndSec;

    ProgressBar mProgressBar;

    private String mTomatoType = WatchFaceUtil.DEFAULT_TOMATO_TYPE;

    private int mDataTomatoWork = WatchFaceUtil.DEFAULT_TOMATO_WORK;
    private int mDataTomatoRelax = WatchFaceUtil.DEFAULT_TOMATO_RELAX;
    private int mDataTomatoRelaxLong = WatchFaceUtil.DEFAULT_TOMATO_RELAX_LONG;

    private long mDataTomatoTimeInMillisPre = System.currentTimeMillis();
    private long mDataTomatoDateStart = WatchFaceUtil.DEFAULT_TIMER_ZERO;
    private long mDataTomatoDateEnd = WatchFaceUtil.DEFAULT_TIMER_ZERO;

    public String mSelectedCalendarName = "";
    public String mSelectedCalendarAccountName = "";
    public int mSelectedCalendarColor = 0;
    private long mSelectedCalendarID = -1;

    private boolean mActivityShowed = false;
    private boolean mActivityPostDelayRunning = false;

    AlarmManager mAlarmManager = null;
    NotificationManager mNotificationManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_mobile_timer);

        mBtnSettings = findViewById(R.id.button_settings);
        mBtnPrevious = findViewById(R.id.button_previous);
        mBtnPlay = findViewById(R.id.button_play);
        mBtnStop = findViewById(R.id.button_stop);
        mBtnNext = findViewById(R.id.button_next);

        mTextCal = findViewById(R.id.textview_calendar);
        mTextCalPrefix = findViewById(R.id.textview_calendar_prefix);
        mTextType = findViewById(R.id.textview_mode);
        mTextMin = findViewById(R.id.textview_time_min);
        mTextSec = findViewById(R.id.textview_time_sec);
        //mTextMinAndSec = findViewById(R.id.textview_time_min_sec);

        mTextCenter = findViewById(R.id.textview_center_2);

        mIsScrAlwaysOn = false;
        mIsForceScrAlwaysOn = false;
        mIsScrAlwaysOnCharging = false;
        mDataTomatoPowerUpdated = System.currentTimeMillis();
        mImageScrAlwaysOnPower = findViewById(R.id.image_power);
        mImageScrAlwaysOnSun = findViewById(R.id.image_scr_always_on);
        mImageScrAlwaysOnLock = findViewById(R.id.image_scr_always_on_lock);
        mLayoutScrAlwaysOn = findViewById(R.id.layout_scr_always_on);
        mLayoutScrAlwaysOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsForceScrAlwaysOn = !mIsForceScrAlwaysOn;
                updatePowerStatus();
                updateScrAlwaysOnStateStatus();

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainMobileTimerActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(WatchFaceUtil.KEY_TOMATO_SCR_ALWAYS_ON, mIsForceScrAlwaysOn);
                editor.commit();
            }
        });



        mProgressBar = findViewById(R.id.circularProgressBar);
        mProgressBar.setProgress(0);


        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //getActionBar().hide();

//        LayerDrawable progDrawable= (LayerDrawable)progressBar.getProgressDrawable();
//        if (progDrawable != null)
//        {
//            //progDrawable.findDrawableByLayerId(R.)
//            LayerDrawable shape = (LayerDrawable) getResources().getDrawable(R.drawable.progressbar);
//            shape.setColor(Color.Black); // changing to black color
//        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View view = findViewById(android.R.id.content);
//            int flags = view.getSystemUiVisibility();
//            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
//            view.setSystemUiVisibility(flags);
//            this.getWindow().setStatusBarColor(Color.WHITE);
            int flags = view.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
            this.getWindow().setStatusBarColor(0xFF000000);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            w.setStatusBarColor(0xFF000000);
        }

    }

    private void updateTimer(boolean isUpdateProgressBar, boolean enableProgressBarAnimation)
    {
        Long timeInMillis = System.currentTimeMillis();
        int progressPre = mProgressBar.getProgress();
        int progressCur = progressPre;
        String strMin = "00",strSec = "00";
        if (mDataTomatoDateStart == 0 || mDataTomatoDateEnd == 0 || mDataTomatoDateEnd == mDataTomatoDateStart)
        {
//            time_min = mDataTomatoWork % 60;
//            time_sec = mDataTomatoWork / 60;

            SimpleDateFormat sdfhour = new SimpleDateFormat("HH");
            SimpleDateFormat sdfmin = new SimpleDateFormat("mm");
            Date dateNow = new Date(timeInMillis);

            strMin = sdfhour.format(dateNow);
            strSec = sdfmin.format(dateNow);

            changeProgressBarMode(ProgressMode.IDLE);
            progressCur = (Calendar.getInstance().get(Calendar.SECOND)*100)/59;
            if (progressCur >=100)
                progressCur = 100;

            long timeInSec = timeInMillis/1000;
            if ((timeInSec & 1) != 0)
                mTextCenter.setAlpha(0.5f);
            else
                mTextCenter.setAlpha(1.0f);
        }
        else
        {
            long time_min = 0,time_sec = 0;
            if (timeInMillis < mDataTomatoDateStart) timeInMillis = mDataTomatoDateStart;

            time_sec = (mDataTomatoDateEnd - timeInMillis) / 1000;
            if (timeInMillis>=mDataTomatoDateEnd) {
                if (mDataTomatoTimeInMillisPre < mDataTomatoDateEnd) {
                    if (mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_WORK))
                        changeProgressBarMode(ProgressMode.WARNING);
                    else
                        changeProgressBarMode(ProgressMode.ERROR);
                }
                progressCur = 100;
                updateModeText(getResources().getString(R.string.text_timer_mode_out_of_time),"+");
            }
            else
            {
                long diffTime = (mDataTomatoDateEnd - timeInMillis)/1000;
//                if (diffTime < 1000)
//                    progressCur = 0;
//                else {
//                    long totalTime = mDataTomatoDateEnd - mDataTomatoDateStart;
//                    progressCur = (int) ((diffTime * 100) / (totalTime));
//                }
                long totalTime = (mDataTomatoDateEnd - mDataTomatoDateStart)/1000;
                progressCur = (int) ((diffTime * 100) / (totalTime));
            }

            if (progressCur>100) progressCur = 100;
            if (time_sec<0) time_sec = -time_sec;

            time_min = time_sec / 60;
            time_sec = time_sec % 60;
            if ((time_sec & 1) != 0)
                mTextCenter.setAlpha(0.5f);
            else
                mTextCenter.setAlpha(1.0f);

            strMin = String.format("%02d",time_min);
            strSec = String.format("%02d",time_sec);

        }

        if (strMin.length()>=4)
        {
            strMin = "999";
            strSec = "59";
        }

        mTextMin.setText(strMin);
        mTextSec.setText(strSec);

//        if (strMin.length() >= 2)
//        {
//            mTextMinAndSec.setText(strMin+":"+strSec);
//            mTextCenter.setVisibility(View.INVISIBLE);
//            mTextMin.setVisibility(View.INVISIBLE);
//            mTextSec.setVisibility(View.INVISIBLE);
//            mTextMinAndSec.setVisibility(View.VISIBLE);
//        }
//        else
//        {
//            mTextMin.setText(strMin);
//            mTextSec.setText(strSec);
//            mTextCenter.setVisibility(View.VISIBLE);
//            mTextMin.setVisibility(View.VISIBLE);
//            mTextSec.setVisibility(View.VISIBLE);
//            mTextMinAndSec.setVisibility(View.INVISIBLE);
//        }

        if (timeInMillis - mDataTomatoPowerUpdated > 2 * 60 * 1000)//5 min/check
            updatePowerStatus();
        Log.d(TAG,"updateTimer " + (progressPre/100) + " -> " + progressCur + " " + (timeInMillis - mDataTomatoPowerUpdated));

        int progressDiff = Math.abs((progressPre/100) - progressCur);

        if (isUpdateProgressBar && progressCur != (progressPre/100))
        {
            if (progressDiff < 98 && enableProgressBarAnimation)
            {
                ObjectAnimator animation = ObjectAnimator.ofInt (mProgressBar, "progress", progressPre, progressCur*100); // see this max value coming back here, we animale towards that value
                animation.setDuration (800); //in milliseconds
                animation.setInterpolator (new DecelerateInterpolator());
                animation.start ();
            }
            else
                mProgressBar.setProgress(progressCur*100);
        }

        mDataTomatoTimeInMillisPre = timeInMillis;

//        switch (mTomatoType)
//        {
//
//            case WatchFaceUtil.KEY_TOMATO_WORK:
//                mTextType.setText(getResources().getString(R.string.text_timer_mode_work));
//                break;
//            case WatchFaceUtil.KEY_TOMATO_RELAX:
//                mTextType.setText(getResources().getString(R.string.text_timer_mode_relax));
//                break;
//            case WatchFaceUtil.KEY_TOMATO_RELAX_LONG:
//                mTextType.setText(getResources().getString(R.string.text_timer_mode_relax_long));
//                break;
//
//            case WatchFaceUtil.KEY_TOMATO_IDLE:
//            default:
//                mTextType.setText(getResources().getString(R.string.text_timer_mode_idle));
//                break;
//        }

    }

    private void updatePowerStatus()
    {
        boolean isScrAlwaysOnChargingPre = mIsScrAlwaysOnCharging;
        mIsScrAlwaysOnCharging = isPowerConnected(this);
        Log.d(TAG,"isPowerConnected " + mIsScrAlwaysOnCharging);
//        if (isScreenOn) {
//            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//            //mImageScrAlwaysOnPower.setVisibility(View.VISIBLE);
//        }
//        else {
//            mIsScrAlwaysOnCharging
//            //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//            //mImageScrAlwaysOnPower.setVisibility(View.INVISIBLE);
//        }
        mDataTomatoPowerUpdated = System.currentTimeMillis();
        if (isScrAlwaysOnChargingPre != mIsScrAlwaysOnCharging)
            updateScrAlwaysOnStateStatus();
    }

    private void updateScrAlwaysOnStateStatus()
    {
        mImageScrAlwaysOnPower.setVisibility(mIsScrAlwaysOnCharging?View.VISIBLE:View.GONE);
        mImageScrAlwaysOnLock.setVisibility(mIsForceScrAlwaysOn?View.VISIBLE:View.GONE);

        if (mIsScrAlwaysOnCharging || mIsForceScrAlwaysOn)
        {
            if (!mIsScrAlwaysOn)
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mImageScrAlwaysOnSun.setAlpha(0.6f);
            mIsScrAlwaysOn = true;
        }
        else
        {
            if (mIsScrAlwaysOn)
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mImageScrAlwaysOnSun.setAlpha(0.2f);
            mIsScrAlwaysOn = false;
        }
    }

    private void changeProgressBarMode(ProgressMode mode)
    {
        Drawable new_drawable = null;
        switch (mode)
        {
            case ERROR:
                new_drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar_red, null);
                mTextCalPrefix.setTextColor(0xFFF44336);
                break;
            case WARNING:
                new_drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar_yellow, null);
                mTextCalPrefix.setTextColor(0xFFFFEB3B);
                break;
            case IDLE:
                new_drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar_idle, null);
                mTextCalPrefix.setTextColor(0xFFFFFFFF);
                break;
            case NORMAL:
            default:
                new_drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar, null);
                mTextCalPrefix.setTextColor(mSelectedCalendarColor);
        }
        if (new_drawable != null)
        {
            mProgressBar.setProgressDrawable(new_drawable);
        }
    }

    private static final int SEND_EVENT=123;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG,"onActivityResult "+requestCode + " " + resultCode + " " + (data!=null ? data.toString(): "null"));
        //data.getExtras().getString("B")
        if(resultCode == Activity.RESULT_CANCELED) return;
        switch(requestCode){
            case SEND_EVENT:
                onButtonClick(mBtnPrevious);
                break;

        }
    }

    public void onButtonClick(View v)
    {
        if (v == mBtnSettings)
        {
            Intent myIntent = new Intent(MainMobileTimerActivity.this, MainConfigActivity.class);
            //myIntent.putExtra("key", value); //Optional parameters
            MainMobileTimerActivity.this.startActivity(myIntent);
        }
        else if (v == mBtnPlay && false)
        {
            Long timeInMillis = System.currentTimeMillis();
            Intent i = new Intent(getApplicationContext(), TomatoBuilderActivity.class);
            i.putExtra(WatchFaceUtil.KEY_TOMATO_DATE_START,timeInMillis);
            i.putExtra(WatchFaceUtil.KEY_TOMATO_DATE_END,timeInMillis + 30*60*1000);
            i.putExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR,mSelectedCalendarColor);
            i.putExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME,mSelectedCalendarName);
            i.putExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID, mSelectedCalendarID);
            //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(i,SEND_EVENT);
        }
        else
        {
            Long timeInMillis = System.currentTimeMillis();
            boolean isModifiedConfig = false;


            long dtstart,dtend;
            dtstart = 0;
            dtend = 0;

            if (mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_WORK) && mDataTomatoDateEnd!=0 && timeInMillis>=mDataTomatoDateEnd)
            {
                dtstart = mDataTomatoDateStart;
                dtend = timeInMillis;
            }

            final int startDelay = 1100;

            if (v == mBtnStop)
            {
                mDataTomatoDateStart = mDataTomatoDateEnd = 0;
                mTomatoType = WatchFaceUtil.KEY_TOMATO_IDLE;
                isModifiedConfig = true;

                mBtnPlay.setVisibility(View.VISIBLE);
                mBtnStop.setVisibility(View.GONE);
                mBtnPrevious.setVisibility(View.GONE);
                mBtnNext.setVisibility(View.GONE);
                changeProgressBarMode(ProgressMode.IDLE);

            }
            else if (v == mBtnPlay && (mDataTomatoDateStart == 0 || mDataTomatoDateEnd == 0 || mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_IDLE)))
            {
                changeProgressBarMode(ProgressMode.NORMAL);
                mDataTomatoDateStart = timeInMillis + startDelay;
                mDataTomatoDateEnd = mDataTomatoDateStart+ (mDataTomatoWork) *1000;
                mTomatoType = WatchFaceUtil.KEY_TOMATO_WORK;
                isModifiedConfig = true;
                mBtnPlay.setVisibility(View.GONE);
                mBtnStop.setVisibility(View.VISIBLE);
                mBtnPrevious.setVisibility(View.VISIBLE);
                mBtnNext.setVisibility(View.VISIBLE);

            }
            else if (v == mBtnPrevious)
            {
                long diffPre = (mDataTomatoDateEnd - mDataTomatoDateStart)/1000;
                if (!mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_IDLE) && diffPre > 0) {
                    if (mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_WORK))
                        changeProgressBarMode(ProgressMode.NORMAL);
                    else
                        changeProgressBarMode(ProgressMode.WARNING);
                    mDataTomatoDateStart = timeInMillis + startDelay;
                    mDataTomatoDateEnd = mDataTomatoDateStart + (diffPre) * 1000;
                    isModifiedConfig = true;
                }
            }
            else if (v == mBtnNext)//out of time
            {
                if (mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_WORK))
                {
                    changeProgressBarMode(ProgressMode.WARNING);
                    mDataTomatoDateStart = timeInMillis + startDelay;
                    mDataTomatoDateEnd = mDataTomatoDateStart+ (mDataTomatoRelax) *1000;
                    mTomatoType = WatchFaceUtil.KEY_TOMATO_RELAX;
                    isModifiedConfig = true;
                }
                else // relax , long relax
                {
                    changeProgressBarMode(ProgressMode.NORMAL);
                    mDataTomatoDateStart = timeInMillis + startDelay;
                    mDataTomatoDateEnd = mDataTomatoDateStart+ (mDataTomatoWork) *1000;
                    mTomatoType = WatchFaceUtil.KEY_TOMATO_WORK;
                    isModifiedConfig = true;
                }
            }
//            else
//            {
//                if (mTouchCoordinateY<mTouchHeight/2)
//                {
//                    mDataTomatoDateStart = timeInMillis;
//                    mDataTomatoDateEnd = mDataTomatoDateStart+ mDataTomatoWork *1000;
//                    mTomatoType = WatchFaceUtil.KEY_TOMATO_WORK;
//                    isModifiedConfig = true;
//                }
//                else if (mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_WORK))
//                {
//                    mDataTomatoDateStart = mDataTomatoDateEnd = 0;
//                    mTomatoType = WatchFaceUtil.KEY_TOMATO_IDLE;
//                    isModifiedConfig = true;
//                }
//                //canvas.drawText("Start to work!", centerX, centerY-height/6, mInteractionTextPaint);
//                //canvas.drawText("Cancel", centerX-width/4, centerY+height/4,mInteractionTextPaint);
//                //canvas.drawText("Idle", centerX+width/4, centerY+height/4,mInteractionTextPaint);
//            }
            if (isModifiedConfig)
            {

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(WatchFaceUtil.KEY_TOMATO_DATE_START, mDataTomatoDateStart);
                editor.putLong(WatchFaceUtil.KEY_TOMATO_DATE_END, mDataTomatoDateEnd);
                editor.putString(WatchFaceUtil.KEY_TOMATO_TYPE,mTomatoType);
                editor.commit();
                updateModeText("","");
                updateTimer(false, false);


                if (mDataTomatoDateEnd != 0 && !mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_IDLE)) {
                    Intent intent = new Intent(this, AlarmReceiver.class);
                    intent.putExtra("msg",
                            mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_WORK) ? "play_tomato_warning" : "play_tomato_alarm");

                    PendingIntent pi = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

                    //AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    mAlarmManager.set(AlarmManager.RTC_WAKEUP, mDataTomatoDateEnd, pi);

                    updateNotification();
                }
                else
                {
                    Intent updateServiceIntent = new Intent(this, AlarmReceiver.class);
                    //PendingIntent pendingUpdateIntent = PendingIntent.getService(this, 0, updateServiceIntent, 0);
                    PendingIntent pendingUpdateIntent = PendingIntent.getBroadcast(this, 1, updateServiceIntent, PendingIntent.FLAG_CANCEL_CURRENT);


                    // Cancel alarms
                    try {
                        mAlarmManager.cancel(pendingUpdateIntent);
                        Log.d(TAG, "AlarmManager update was canceled. ");
                    } catch (Exception e) {
                        Log.e(TAG, "AlarmManager update was not canceled. " + e.toString());
                    }
                    mNotificationManager.cancelAll();
                }



            }

            if (mSelectedCalendarID != -1 && dtstart!=0 && dtend>=dtstart)
            {
                Intent i = new Intent(getApplicationContext(), TomatoBuilderActivity.class);
                i.putExtra(WatchFaceUtil.KEY_TOMATO_DATE_START,dtstart);
                i.putExtra(WatchFaceUtil.KEY_TOMATO_DATE_END,dtend);
                i.putExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR,mSelectedCalendarColor);
                i.putExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME,mSelectedCalendarName);
                i.putExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID, mSelectedCalendarID);
                //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(i,SEND_EVENT);

//                Intent i = new Intent(getApplicationContext(), TomatoBuilderActivity.class);
//                i.putExtra(CalendarContract.Events.DTSTART,dtstart);
//                i.putExtra(CalendarContract.Events.DTEND,timeInMillis);
//                i.putExtra(CalendarContract.Events.CALENDAR_ID,mCalendarID);
//                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(i);
            }

        }
    }

    public static boolean isPowerConnected(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        //return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");

        mActivityShowed = true;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSelectedCalendarName = prefs.getString(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME,"");
        mSelectedCalendarAccountName = prefs.getString(WatchFaceUtil.KEY_TOMATO_CALENDAR_ACCOUNT_NAME,"");
        mSelectedCalendarColor = prefs.getInt(WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR,0);
        mSelectedCalendarID = prefs.getLong(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID,-1);

        mDataTomatoWork = prefs.getInt(WatchFaceUtil.KEY_TOMATO_WORK,mDataTomatoWork);
        mDataTomatoRelax = prefs.getInt(WatchFaceUtil.KEY_TOMATO_RELAX,mDataTomatoRelax);
        mDataTomatoRelaxLong = prefs.getInt(WatchFaceUtil.KEY_TOMATO_RELAX_LONG,mDataTomatoRelaxLong);

        mDataTomatoDateStart = prefs.getLong(WatchFaceUtil.KEY_TOMATO_DATE_START, mDataTomatoDateStart);
        mDataTomatoDateEnd = prefs.getLong(WatchFaceUtil.KEY_TOMATO_DATE_END, mDataTomatoDateEnd);

        mIsForceScrAlwaysOn = prefs.getBoolean(WatchFaceUtil.KEY_TOMATO_SCR_ALWAYS_ON,mIsForceScrAlwaysOn);
        updateScrAlwaysOnStateStatus();

        mTomatoType = prefs.getString(WatchFaceUtil.KEY_TOMATO_TYPE,mTomatoType);

        if (mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_WORK))
            changeProgressBarMode(ProgressMode.NORMAL);
        else if (mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_IDLE))
            changeProgressBarMode(ProgressMode.IDLE);
        else // relax , long relax
            changeProgressBarMode(ProgressMode.WARNING);

        if (mDataTomatoDateStart == 0 || mDataTomatoDateEnd == 0 || mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_IDLE))
        {
            mBtnPlay.setVisibility(View.VISIBLE);
            mBtnStop.setVisibility(View.GONE);
            mBtnPrevious.setVisibility(View.GONE);
            mBtnNext.setVisibility(View.GONE);

            mDataTomatoDateStart = 0;
            mDataTomatoDateEnd = 0;
        }
        else
        {
            mDataTomatoTimeInMillisPre = mDataTomatoDateStart;
            mBtnPlay.setVisibility(View.GONE);
            mBtnStop.setVisibility(View.VISIBLE);
            mBtnPrevious.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
        }

        if (mSelectedCalendarID != -1 && mSelectedCalendarName.length()!=0) {
            mTextCal.setText(mSelectedCalendarName);
        }
        else
        {
            mTextCal.setText("--");
        }
        updateModeText("","");
        //updateTimer(true, false);
        updateTimer(false, false);

        if (!mActivityPostDelayRunning) {
            mActivityPostDelayRunning = true;
            final View rootView = getWindow().getDecorView().getRootView();
            rootView.postDelayed(new Runnable() {
                public void run() {
                    if (mActivityShowed) {
                        updateTimer(true, true);
                        rootView.postDelayed(this, 1000);
                    }
                    else {
                        mActivityPostDelayRunning = false;
                        Log.d(TAG,"mActivityPostDelayRunning = false");
                    }
                }
            }, 1000);
        }
        if (mTomatoType != WatchFaceUtil.KEY_TOMATO_IDLE)
            updateNotification();

        updatePowerStatus();
    }
    @Override
    public void onPause() {
        Log.d(TAG,"onPause");
        super.onPause();
        mActivityShowed = false;
        mIsScrAlwaysOn = false;
        mIsScrAlwaysOnCharging = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    @Override
    public void onStop() {
        super.onStop();

    }

    private boolean isNotificationVisible(int checkid) {
        StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == checkid) {
                return true;
            }
        }
        return false;
    }

    private void updateNotification()
    {
        if (mDataTomatoDateStart == mDataTomatoDateEnd) return;

        int statTextID = mTomatoType == WatchFaceUtil.KEY_TOMATO_WORK? R.string.text_timer_mode_work :
                         mTomatoType == WatchFaceUtil.KEY_TOMATO_RELAX ? R.string.text_timer_mode_relax :
                         mTomatoType == WatchFaceUtil.KEY_TOMATO_RELAX_LONG ? R.string.text_timer_mode_relax : R.string.text_timer_mode_idle;

        Long timeInMillis = System.currentTimeMillis();
        SimpleDateFormat timeTxtFormat = new SimpleDateFormat("HH:mm:ss");
        String noTitle, noText;
        int iconID;
        if (mDataTomatoDateEnd<timeInMillis)
        {
            iconID = R.mipmap.icon_tomato_color;
            noTitle = getResources().getString(R.string.text_timer_mode_out_of_time) +
                    getResources().getString(R.string.text_timer_mode_relax) +
                    getResources().getString(R.string.text_timer_postfix_doing);
            noText = getResources().getString(R.string.text_timer_mode_relax) +
                    getResources().getString(R.string.text_builder_time_end) + " @ " + timeTxtFormat.format(new Date(mDataTomatoDateEnd));
        }
        else {
            iconID = R.mipmap.icon_tomato_color_light;
            noTitle = getResources().getString(statTextID) +
                    getResources().getString(R.string.text_timer_postfix_doing);
            noText = getResources().getString(statTextID) +
                    getResources().getString(R.string.text_builder_time_start) + " @ " + timeTxtFormat.format(new Date(mDataTomatoDateStart)) + "  ⇨  " +
                    getResources().getString(R.string.text_builder_time_end) + " @ " + timeTxtFormat.format(new Date(mDataTomatoDateEnd));
        }


        updateNotification(noTitle,noText,iconID,false,false);
    }

    private void updateNotification(String title, String text, int iconID, boolean isVirbrate, boolean isSound)
    {
        Intent timerIntent = new Intent(this, io.harpseal.pomodorowear.MainMobileTimerActivity.class);
        timerIntent.setAction(Intent.ACTION_MAIN);
        timerIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                timerIntent, 0);

//        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_notification);
//        contentView.setImageViewResource(R.id.image, iconID);
//        contentView.setTextViewText(R.id.title, noTitle);
//        contentView.setTextViewText(R.id.text, noText);

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                //.setPriority(NotificationManager.IMPORTANCE_HIGH)
                //.setCustomContentView(contentView)
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(iconID)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),iconID))

                .setPriority(Notification.PRIORITY_HIGH);

        int settings = Notification.DEFAULT_LIGHTS;
        if (isVirbrate)
            settings |= Notification.DEFAULT_VIBRATE;
        else
            notificationBuilder.setVibrate(new long[]{0L}); // Passing null here silently fails
        if (isSound)
            settings |= Notification.DEFAULT_SOUND;
        notificationBuilder.setDefaults(settings);


        mNotificationManager.cancel(WatchFaceUtil.ID_TOMATO_NOTIFICATION_ID, 0);
        mNotificationManager.notify(
                WatchFaceUtil.ID_TOMATO_NOTIFICATION_ID, 1,  // <-- Place your notification id here
                notificationBuilder.build());
    }

    private void updateModeText(String prefix, String postfix){
        switch (mTomatoType)
        {

            case WatchFaceUtil.KEY_TOMATO_WORK:
                mTextCal.setVisibility(View.VISIBLE);
                mTextCalPrefix.setVisibility(View.VISIBLE);
                mTextType.setText(prefix +
                        getResources().getString(R.string.text_timer_mode_work) +
                        getResources().getString(R.string.text_timer_postfix_doing) + postfix);
                break;
            case WatchFaceUtil.KEY_TOMATO_RELAX:
                mTextCal.setVisibility(View.VISIBLE);
                mTextCalPrefix.setVisibility(View.VISIBLE);
                mTextType.setText(prefix + getResources().getString(R.string.text_timer_mode_relax) +
                        getResources().getString(R.string.text_timer_postfix_doing) + postfix);
                break;
            case WatchFaceUtil.KEY_TOMATO_RELAX_LONG:
                mTextCal.setVisibility(View.VISIBLE);
                mTextCalPrefix.setVisibility(View.VISIBLE);
                mTextType.setText(prefix + getResources().getString(R.string.text_timer_mode_relax_long) +
                        getResources().getString(R.string.text_timer_postfix_doing) + postfix);
                break;

            case WatchFaceUtil.KEY_TOMATO_IDLE:
            default:
                mTextCal.setVisibility(View.GONE);
                mTextCalPrefix.setVisibility(View.GONE);
                //mTextType.setText(prefix + getResources().getString(R.string.text_timer_mode_idle)+ postfix);
                mTextType.setText("");
                break;
        }
    }


}
