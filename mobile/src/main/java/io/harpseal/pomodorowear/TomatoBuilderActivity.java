package io.harpseal.pomodorowear;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class TomatoBuilderActivity extends Activity {
    final String TAG = TomatoBuilderActivity.class.getName();


    private long mDataTomatoDateStart = WatchFaceUtil.DEFAULT_TIMER_ZERO;
    private long mDataTomatoDateEnd = WatchFaceUtil.DEFAULT_TIMER_ZERO;
    public String mSelectedCalendarName = "";
    public int mSelectedCalendarColor = 0;
    public long mSelectedCalendarID = -1;

    private Button mBtnCancel;
    private Button mBtnOk;

    private EditText mEditTextTitle;
    private EditText mEditTextDetails;

    final String mEditTextTitlePrefix = "[30]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tomato_builder);

        mDataTomatoDateStart = getIntent().getLongExtra(WatchFaceUtil.KEY_TOMATO_DATE_START,mDataTomatoDateStart);
        mDataTomatoDateEnd = getIntent().getLongExtra(WatchFaceUtil.KEY_TOMATO_DATE_END,mDataTomatoDateEnd);
        mSelectedCalendarName = getIntent().getStringExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME);
        mSelectedCalendarColor = getIntent().getIntExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR,mSelectedCalendarColor);
        mSelectedCalendarID = getIntent().getLongExtra(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID,mSelectedCalendarID);

        SimpleDateFormat sdfs = new SimpleDateFormat("yy/MM/dd HH:mm");
        SimpleDateFormat sdfe = new SimpleDateFormat("HH:mm");
        TextView text_view;
        text_view = findViewById(R.id.textview_event_time_start);
        text_view.setText(sdfs.format(new Date(mDataTomatoDateStart)) + " " + getResources().getString(R.string.text_builder_time_to) + " " + sdfe.format(new Date(mDataTomatoDateEnd)) );

        text_view = findViewById(R.id.textview_event_time_end);
        text_view.setVisibility(View.GONE);

//        TextView text_view = findViewById(R.id.textview_event_time_start);
//        Date resultdate = new Date(mDataTomatoDateStart);
//        text_view.setText(getResources().getString(R.string.text_builder_time_start) + ": " + sdf.format(resultdate));
//
//        text_view = findViewById(R.id.textview_event_time_end);
//        resultdate = new Date(mDataTomatoDateEnd);
//        text_view.setText(getResources().getString(R.string.text_builder_time_end) + ": " + sdf.format(resultdate));


        mBtnCancel = findViewById(R.id.button_cancel);
        mBtnOk = findViewById(R.id.button_ok);
        mEditTextTitle = findViewById(R.id.textinput_event_title);
        mEditTextTitle.setText(mEditTextTitlePrefix);
        mEditTextTitle.setSelection(mEditTextTitle.getText().length());
        mEditTextDetails = findViewById(R.id.textinput_event_details);
        mEditTextDetails.requestFocus();



        ArrayList<String> tag_list = new ArrayList<String>();

        boolean isSetTag = false;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String prefTagsListString = prefs.getString(WatchFaceUtil.KEY_TOMATO_TAG_LIST, "");
        if (prefTagsListString.length() != 0)
        {

            DataMap dataMap = DataMap.fromByteArray(Base64.decode(prefTagsListString, Base64.DEFAULT));
            ArrayList<DataMap> arrayMap = dataMap.getDataMapArrayList(WatchFaceUtil.KEY_TOMATO_TAG_LIST);
            for (DataMap map:arrayMap)
            {
                if (map.containsKey(WatchFaceUtil.KEY_TOMATO_TAG_NAME)) {
                    tag_list.add(map.getString(WatchFaceUtil.KEY_TOMATO_TAG_NAME, ""));
                    isSetTag = true;
                }
            }
        }
        if (!isSetTag)
        {
            for (String tag:WatchFaceUtil.DEFAULT_TOMATO_TAGS)
                tag_list.add(tag);
        }

        ArrayAdapter<String> adapter = null;
        adapter = new ArrayAdapter<>(this,R.layout.list_item_tomato_builder, tag_list);

        ListView list_view = findViewById(R.id.listview_tags);
        list_view.setAdapter(adapter);

        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                //Toast.makeText(getApplicationContext(),
                //        ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
                mEditTextTitle.setText(mEditTextTitle.getText().toString() + ((TextView) view).getText());
                mEditTextTitle.setSelection(mEditTextTitle.getText().length());

            }
        });
    }

    public void onButtonClick(View v) {
        if (v == mBtnCancel)
        {
            if (mEditTextTitle.getText().toString().equals(mEditTextTitlePrefix)) {
                new AlertDialog.Builder(TomatoBuilderActivity.this)
                        .setMessage(R.string.text_builder_finish_alarm)
                        .setPositiveButton(R.string.text_builder_finish_alarm_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TomatoBuilderActivity.this.setResult(RESULT_OK);
                                TomatoBuilderActivity.this.finish();
                            }
                        })
                        .setNegativeButton(R.string.text_builder_finish_alarm_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Toast.makeText(getApplicationContext(), R.string.i_am_hungry, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        })
//                        .setNeutralButton(R.string.not_hungry, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                Toast.makeText(getApplicationContext(), R.string.diet, Toast.LENGTH_SHORT).show();
//                            }
//                        })
                        .show();

            }
            else {
                mEditTextTitle.setText(mEditTextTitlePrefix);
                mEditTextTitle.setSelection(mEditTextTitle.getText().length());
            }
        }
        else if (v == mBtnOk)
        {

            Thread threadSendEvent = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"Start processEventQueue");
                    boolean res = processEventQueue(mDataTomatoDateStart,mDataTomatoDateEnd,mSelectedCalendarID,mEditTextTitle.getText().toString(),mEditTextDetails.getText().toString(),"");
                    Log.d(TAG,"End processEventQueue " + res);
                }
            });
            Log.d(TAG,"Start to wait processEventQueue");
            threadSendEvent.start();

            try {
                // Thread B 加入 Thread A
                threadSendEvent.join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG,"End waiting processEventQueue");
            setResult(RESULT_OK);
            finish();
        }

    }
    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        super.onBackPressed();
        setResult(RESULT_OK);
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        //setResult(RESULT_OK);
    }
    private ArrayList<MessageListenerService.EventItem> getEvents(long calendarID, long timeStart, long timeEnd)
    {

        String[] projection =
                new String[]{
                        CalendarContract.Events.CALENDAR_ID,//0
                        CalendarContract.Events.TITLE,//1
                        CalendarContract.Events.DESCRIPTION,//2
                        CalendarContract.Events.DTSTART,//3
                        CalendarContract.Events.DTEND//4
                };

        String permission;
        permission = "android.permission.READ_CALENDAR";
        int res = TomatoBuilderActivity.this.checkCallingOrSelfPermission(permission);
        if (res == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"getEvents calendar_id " + calendarID + " dtend" + timeEnd + " tdstart" + timeStart);
            String selectionClause =
                    "(" + CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTEND + " <= ?)" + " OR " +
                            "(" + CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.ALL_DAY + " = ?)";

            selectionClause = " (" + selectionClause + ") AND (" + CalendarContract.Events.CALENDAR_ID + " = ? )";

            String[] selectionsArgs = new String[]{"" + timeStart, "" + timeEnd, "" + timeStart, "1" , ""+calendarID };

            Cursor eventCursor = getContentResolver()
                    .query(
                            CalendarContract.Events.CONTENT_URI,
                            projection,
                            selectionClause,
                            selectionsArgs, null);
            if (eventCursor.moveToFirst()) {

                ArrayList<MessageListenerService.EventItem> resList = new ArrayList<MessageListenerService.EventItem>();
                //Calendar cal = Calendar.getInstance();
                do {
                    long id = eventCursor.getLong(0);//CALENDAR_ID
                    String title = eventCursor.getString(1);//TITLE
                    String description = eventCursor.getString(2);//DESCRIPTION
                    long eventStart = eventCursor.getLong(3);
                    long eventEnd = eventCursor.getLong(4);

                    resList.add(new MessageListenerService().new EventItem(eventStart,eventEnd,title));

//                    cal.setTimeInMillis(eventStart);
//                    String strEventStart = DateFormat.format("YY MM dd HH:mm:ss", cal).toString();
//
//                    cal.setTimeInMillis(eventEnd);
//                    String strEventEnd = DateFormat.format("YY MM dd HH:mm:ss", cal).toString();

                    Log.i(TAG,"event calid:" + id + "  title :" + title + "  des:" + description + "  time:" + eventStart + " -> " + eventEnd);

                } while (eventCursor.moveToNext());

                return resList;
            }
            else
                Log.d(TAG,"eventCursor.moveToFirst() == null");


        }
        else {
            Log.d(TAG,"res != PackageManager.PERMISSION_GRANTED " + res);
            return null;
        }
        return null;
    }

    private boolean processEventQueue(long dtstart,long dtend,long calID,String strTitle,String strDescription,String strTimeZone)
    {

        if (calID == -1)
        {
            Log.d(TAG,"calID == -1, skip. ");
            return false;
        }

        String permission;
        permission = "android.permission.READ_CALENDAR";
        int res = TomatoBuilderActivity.this.checkCallingOrSelfPermission(permission);
        if (res == PackageManager.PERMISSION_GRANTED) {

            ContentResolver cr = getContentResolver();

            dtstart-= dtstart % 1000;
            dtend -= dtend % 1000;
            if (dtstart == dtend) dtend+=1000;



            long timeRange = 2000; //5sec
            ArrayList<MessageListenerService.EventItem> existList = getEvents(calID,dtstart-timeRange,dtend+timeRange);
            if (existList != null && existList.size()!=0)
            {
                for (MessageListenerService.EventItem item : existList)
                {
                    if (Math.abs(item.start - dtstart) < timeRange &&
                            Math.abs(item.end - dtend) < timeRange &&
                            item.title.equals(strTitle))
                    {
                        Log.d(TAG,"event has already created. ");
                        return false;
                    }
                }

            }


            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, dtstart);
            values.put(CalendarContract.Events.DTEND, dtend);
            values.put(CalendarContract.Events.TITLE, strTitle);
            values.put(CalendarContract.Events.DESCRIPTION, strDescription);
            values.put(CalendarContract.Events.CALENDAR_ID, calID );
            values.put(CalendarContract.Events.EVENT_TIMEZONE, strTimeZone.length()==0? TimeZone.getDefault().getID():strTimeZone );

            final Uri uriEventRes = cr.insert(CalendarContract.Events.CONTENT_URI, values);

            boolean isSuccess = true;
            try
            {
                int id = Integer.parseInt(uriEventRes.getLastPathSegment());
                Log.d(TAG,"event " + id + " is created.  url: " + uriEventRes.toString());
            }catch (NumberFormatException nfe)
            {
                Log.d(TAG,"event can't be created. " + nfe.toString() + " url: " + uriEventRes.toString());
                isSuccess = false;
            }
            return isSuccess;
        }
        return false;
    }

}
