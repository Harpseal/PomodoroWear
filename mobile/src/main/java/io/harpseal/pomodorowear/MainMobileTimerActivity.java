package io.harpseal.pomodorowear;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainMobileTimerActivity extends Activity {

    final String TAG = MainMobileTimerActivity.class.getName();
    enum ProgressMode
    {
        NORMAL,WARNING,ERROR
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
        mTextCenter = findViewById(R.id.textview_center_2);

        mProgressBar = findViewById(R.id.circularProgressBar);
        mProgressBar.setProgress(0);


        //getActionBar().hide();

//        LayerDrawable progDrawable= (LayerDrawable)progressBar.getProgressDrawable();
//        if (progDrawable != null)
//        {
//            //progDrawable.findDrawableByLayerId(R.)
//            LayerDrawable shape = (LayerDrawable) getResources().getDrawable(R.drawable.progressbar);
//            shape.setColor(Color.Black); // changing to black color
//        }


    }

    private void updateTimer()
    {
        Long timeInMillis = System.currentTimeMillis();
        int progressPre = mProgressBar.getProgress();
        int progressCur = progressPre;
        if (mDataTomatoDateStart == 0 || mDataTomatoDateEnd == 0 || mDataTomatoDateEnd == mDataTomatoDateStart)
        {
//            time_min = mDataTomatoWork % 60;
//            time_sec = mDataTomatoWork / 60;

            SimpleDateFormat sdfhour = new SimpleDateFormat("HH");
            SimpleDateFormat sdfmin = new SimpleDateFormat("mm");
            Date dateNow = new Date(timeInMillis);
            mTextMin.setText(sdfhour.format(dateNow) );
            mTextSec.setText(sdfmin.format(dateNow));

            changeProgressBarMode(ProgressMode.NORMAL);
            progressCur = 0;

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
                progressCur = (int)(((mDataTomatoDateEnd - timeInMillis) * 100) / (mDataTomatoDateEnd - mDataTomatoDateStart));
            }

            if (time_sec<0) time_sec = -time_sec;

            time_min = time_sec / 60;
            time_sec = time_sec % 60;
            if ((time_sec & 1) != 0)
                mTextCenter.setAlpha(0.5f);
            else
                mTextCenter.setAlpha(1.0f);

            mTextMin.setText(String.format("%02d",time_min));
            mTextSec.setText(String.format("%02d",time_sec));
        }
        Log.d(TAG,"updateTimer " + (progressPre/100) + " -> " + progressCur);

        if (progressCur != (progressPre/100))
        {
            //if (Math.abs(progressCur - progressPre) > 1)
            {
                ObjectAnimator animation = ObjectAnimator.ofInt (mProgressBar, "progress", progressPre, progressCur*100); // see this max value coming back here, we animale towards that value
                animation.setDuration (800); //in milliseconds
                animation.setInterpolator (new DecelerateInterpolator());
                animation.start ();
            }
//            else
//                mProgressBar.setProgress(progressCur);
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

    private void changeProgressBarMode(ProgressMode mode)
    {
        Drawable new_drawable = null;
        switch (mode)
        {
            case ERROR:
                new_drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar_red, null);
                break;
            case WARNING:
                new_drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar_yellow, null);
                break;
            case NORMAL:
            default:
                new_drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar, null);
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

            final int startDelay = 1500;
            if (v == mBtnStop)
            {

                mDataTomatoDateStart = mDataTomatoDateEnd = 0;
                mTomatoType = WatchFaceUtil.KEY_TOMATO_IDLE;
                isModifiedConfig = true;

                mBtnPlay.setVisibility(View.VISIBLE);
                mBtnStop.setVisibility(View.GONE);
                mBtnPrevious.setVisibility(View.GONE);
                mBtnNext.setVisibility(View.GONE);
                changeProgressBarMode(ProgressMode.NORMAL);

                Intent updateServiceIntent = new Intent(this, AlarmReceiver.class);
                PendingIntent pendingUpdateIntent = PendingIntent.getService(this, 0, updateServiceIntent, 0);

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                // Cancel alarms
                try {
                    alarmManager.cancel(pendingUpdateIntent);
                    Log.d(TAG, "AlarmManager update was canceled. ");
                } catch (Exception e) {
                    Log.e(TAG, "AlarmManager update was not canceled. " + e.toString());
                }

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
                    changeProgressBarMode(ProgressMode.NORMAL);
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
                updateTimer();

                if (mDataTomatoDateEnd != 0 && !mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_IDLE)) {
                    Intent intent = new Intent(this, AlarmReceiver.class);
                    intent.putExtra("msg", "play_tomato_alarm");

                    PendingIntent pi = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

                    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.RTC_WAKEUP, mDataTomatoDateEnd, pi);
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

        mTomatoType = prefs.getString(WatchFaceUtil.KEY_TOMATO_TYPE,mTomatoType);

        if (mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_WORK) || mTomatoType.equals(WatchFaceUtil.KEY_TOMATO_IDLE))
            changeProgressBarMode(ProgressMode.NORMAL);
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
            mTextCalPrefix.setTextColor(mSelectedCalendarColor);
        }
        else
        {
            mTextCal.setText("--");
        }
        updateModeText("","");
        updateTimer();

        if (!mActivityPostDelayRunning) {
            mActivityPostDelayRunning = true;
            final View rootView = getWindow().getDecorView().getRootView();
            rootView.postDelayed(new Runnable() {
                public void run() {
                    if (mActivityShowed) {
                        updateTimer();
                        rootView.postDelayed(this, 1000);
                    }
                    else {
                        mActivityPostDelayRunning = false;
                        Log.d(TAG,"mActivityPostDelayRunning = false");
                    }
                }
            }, 1000);
        }
    }
    @Override
    public void onPause() {
        Log.d(TAG,"onPause");
        super.onPause();
        mActivityShowed = false;
    }
    @Override
    public void onStop() {
        super.onStop();

    }

    private void updateModeText(String prefix, String postfix){
        switch (mTomatoType)
        {

            case WatchFaceUtil.KEY_TOMATO_WORK:
                mTextCal.setVisibility(View.VISIBLE);
                mTextCalPrefix.setVisibility(View.VISIBLE);
                mTextType.setText(prefix + getResources().getString(R.string.text_timer_mode_work)+ postfix);
                break;
            case WatchFaceUtil.KEY_TOMATO_RELAX:
                mTextCal.setVisibility(View.VISIBLE);
                mTextCalPrefix.setVisibility(View.VISIBLE);
                mTextType.setText(prefix + getResources().getString(R.string.text_timer_mode_relax)+ postfix);
                break;
            case WatchFaceUtil.KEY_TOMATO_RELAX_LONG:
                mTextCal.setVisibility(View.VISIBLE);
                mTextCalPrefix.setVisibility(View.VISIBLE);
                mTextType.setText(prefix + getResources().getString(R.string.text_timer_mode_relax_long)+ postfix);
                break;

            case WatchFaceUtil.KEY_TOMATO_IDLE:
            default:
                mTextCal.setVisibility(View.GONE);
                mTextCalPrefix.setVisibility(View.GONE);
                mTextType.setText(prefix + getResources().getString(R.string.text_timer_mode_idle)+ postfix);
                break;
        }
    }


}
