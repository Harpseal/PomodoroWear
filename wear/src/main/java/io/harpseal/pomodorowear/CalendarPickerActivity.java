package io.harpseal.pomodorowear;

import android.app.Activity;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CalendarPickerActivity extends Activity implements
        WearableListView.ClickListener, WearableListView.OnScrollListener{

    private static final String TAG = "CalendarPickerActivity";

    private TextView mHeader;
    private WearableListView mConfigListView;
    private BoxInsetLayout mConfigContent;

    private long mCalendarID = WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ID;
    private String mCalendarName = WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_NAME;
    private int mCalendarColor = WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_COLOR;
    private String mCalendarAccountName = WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ACCOUNT_NAME;

    private ConfigItemListAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_config);

        mHeader = (TextView) findViewById(R.id.config_header);
        mConfigListView = (WearableListView) findViewById(R.id.config_list);

        mConfigContent = (BoxInsetLayout) findViewById(R.id.config_content);
        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        mConfigContent.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            (int) getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }

                return v.onApplyWindowInsets(insets);
            }
        });

        mConfigListView.setHasFixedSize(true);
        mConfigListView.setClickListener(this);
        mConfigListView.addOnScrollListener(this);

        mHeader.setText(getResources().getString(R.string.config_item_lv1_calendar));

        String[] items;
        items = getResources().getStringArray(R.array.config_array_lv2_calendar);
        mListAdapter = new ConfigItemListAdapter(items);
        mConfigListView.setAdapter(mListAdapter);

        updateCalendarList();
    }

    @Override // WearableListView.ClickListener
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ConfigItemViewHolder configItemViewHolder = (ConfigItemViewHolder) viewHolder;
        Log.d(TAG, " WearableListView.ClickListener :[" + configItemViewHolder.mConfigItem.getLebelName() + "]");


        //updateConfigDataItem(colorItemViewHolder.mColorItem.getColor());
        //finish();
    }


    @Override // WearableListView.ClickListener
    public void onTopEmptyRegionClick() {}

    @Override // WearableListView.OnScrollListener
    public void onScroll(int scroll) {}

    @Override // WearableListView.OnScrollListener
    public void onAbsoluteScrollChange(int scroll) {
        float newTranslation = Math.min(-scroll, 0);
        mHeader.setTranslationY(newTranslation);
    }

    @Override // WearableListView.OnScrollListener
    public void onScrollStateChanged(int scrollState) {}

    @Override // WearableListView.OnScrollListener
    public void onCentralPositionChanged(int centralPosition) {}

    private class ConfigItemListAdapter extends WearableListView.Adapter {
        private String[] mItems;
        private String[] mSubItems = null;

        public ConfigItemListAdapter(String[] items) {
            setItems(items);
        }

        public ConfigItemListAdapter(String[] items,String[] subItems) {
            setItems(items,subItems);
        }

        public void setItems(String[] items) {
            mItems = items;
        }
        public void setItems(String[] items,String[] subItems) {
            mItems = items;
            mSubItems = subItems;
        }

        @Override
        public ConfigItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ConfigItemViewHolder(new ConfigItem(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            ConfigItemViewHolder configItemViewHolder = (ConfigItemViewHolder) holder;
            String itemName = mItems[position];
            configItemViewHolder.mConfigItem.setItemName(itemName);

            RecyclerView.LayoutParams layoutParams =
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            int colorPickerItemMargin = (int) getResources()
                    .getDimension(R.dimen.digital_config_item_margin);
            // Add margins to first and last item to make it possible for user to tap on them.
            if (position == 0) {
                layoutParams.setMargins(0, colorPickerItemMargin, 0, 0);
            } else if (position == mItems.length - 1) {
                layoutParams.setMargins(0, 0, 0, colorPickerItemMargin);
            } else {
                layoutParams.setMargins(0, 0, 0, 0);
            }
            configItemViewHolder.itemView.setLayoutParams(layoutParams);
        }

        @Override
        public int getItemCount() {
            return mItems.length;
        }
    }


    private static class ConfigItemViewHolder extends WearableListView.ViewHolder {
        public final ConfigItem mConfigItem;

        public ConfigItemViewHolder(ConfigItem colorItem) {
            super(colorItem);
            mConfigItem = colorItem;
        }
    }

    private class CalendarItem
    {
        public long id = 0;
        public String name = "";
        public String accountName = "";
        public int color = 0;
        public CalendarItem(long _id,String _name,String accName,int _color)
        {
            id = _id;
            name = _name;
            accountName = accName;
            color = _color;
        }
        @Override
        public String toString()
        {
            return name;
        }

    }
    private static final ArrayList<CalendarItem> mCalendarList = new ArrayList<CalendarItem>();

    private boolean updateCalendarList()
    {
        mCalendarList.clear();

        String[] projection =
                new String[]{
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.NAME,
                        CalendarContract.Calendars.ACCOUNT_NAME,
                        CalendarContract.Calendars.ACCOUNT_TYPE,
                        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                        CalendarContract.Calendars.CALENDAR_COLOR};

        String permission;
        permission = "android.permission.READ_CALENDAR";
        int res = this.checkCallingOrSelfPermission(permission);
        if (res == PackageManager.PERMISSION_GRANTED) {
            Cursor calCursor =
                    getContentResolver().
                            query(CalendarContract.Calendars.CONTENT_URI,
                                    projection,
                                    CalendarContract.Calendars.VISIBLE + " = 1 " + "AND " +
                                            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " + CalendarContract.Calendars.CAL_ACCESS_OVERRIDE,
                                    null,
                                    CalendarContract.Calendars._ID + " ASC");
            if (calCursor.moveToFirst()) {
                do {
                    long id = calCursor.getLong(0);
                    String displayName = calCursor.getString(1);
                    String accName = calCursor.getString(2);
                    //String accType = calCursor.getString(3);
                    String accLevel = calCursor.getString(4);
                    int color = calCursor.getInt(5);
                    Log.i(TAG,"id :" + id + "  name :" + displayName + "  accLevel:" + accLevel + "  color:" + Integer.toHexString(color));

                    mCalendarList.add(new CalendarItem(id,displayName,accName,color));
                    // …
                } while (calCursor.moveToNext());
            }
            else
                Log.i(TAG,"calCursor :" + calCursor.getCount());



            return (mCalendarList.size() != 0);
        }
        else
            return  false;

    }

//    private boolean updateCalendarList()
//    {
//        //List<CalendarEvent> events=new ArrayList<CalendarEvent>();
//
//        long dtstart;
//
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(new Date());
//        cal.set(Calendar.HOUR_OF_DAY, 0);
//        cal.set(Calendar.MINUTE, 0);
//        cal.set(Calendar.SECOND, 0);
//        cal.set(Calendar.MILLISECOND, 0);
//
//        dtstart = cal.getTimeInMillis();
//
//        Uri.Builder builder =
//                WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
//        ContentUris.appendId(builder, dtstart);
//        ContentUris.appendId(builder, dtstart + DateUtils.DAY_IN_MILLIS);
//        final Cursor cursor = getContentResolver().query(builder.build(),
//                null, null, null, null);
//        int numMeetings = cursor.getCount();
//        Log.v(TAG, "Num meetings: " + numMeetings);
//
//        while (cursor.moveToNext()) {
//            long beginVal=cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));
//            long endVal=cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.END));
//            String title=cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
//            String description = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION));
//            Boolean isAllDay=!cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)).equals("0");
//            String eventColor=cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR));
//            Log.v(TAG,"Event " + title + " eventColor " + eventColor + " allDay " + isAllDay.toString());
//        }
//
//        return false;
//    }
    private class LoadCalendarsTask extends AsyncTask<Void, Void, Integer> {
        private PowerManager.WakeLock mWakeLock;

        @Override
        protected Integer doInBackground(Void... voids) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "CalendarWatchFaceWakeLock");
            mWakeLock.acquire();

            String[] projection =
                    new String[]{
                            CalendarContract.Calendars._ID,
                            CalendarContract.Calendars.NAME,
                            CalendarContract.Calendars.ACCOUNT_NAME,
                            CalendarContract.Calendars.ACCOUNT_TYPE,
                            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                            CalendarContract.Calendars.CALENDAR_COLOR};


            Cursor calCursor =
                    getContentResolver().
                            query(WearableCalendarContract.Instances.CONTENT_URI,
                                    projection,
                                    CalendarContract.Calendars.VISIBLE + " = 1 " + "AND " +
                                            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " + CalendarContract.Calendars.CAL_ACCESS_OVERRIDE,
                                    null,
                                    CalendarContract.Calendars._ID + " ASC");

            long begin = System.currentTimeMillis();
            Uri.Builder builder =
                    WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, begin);
            ContentUris.appendId(builder, begin + DateUtils.DAY_IN_MILLIS);
            final Cursor cursor = getContentResolver().query(builder.build(),
                    null, null, null, null);
            int numMeetings = cursor.getCount();
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Num meetings: " + numMeetings);
            }
            return numMeetings;
        }

        @Override
        protected void onPostExecute(Integer result) {
            releaseWakeLock();
            //onMeetingsLoaded(result);
        }

        @Override
        protected void onCancelled() {
            releaseWakeLock();
        }

        private void releaseWakeLock() {
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
        }
    }
}
