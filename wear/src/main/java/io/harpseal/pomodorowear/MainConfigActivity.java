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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainConfigActivity extends Activity implements
        WearableListView.ClickListener, WearableListView.OnScrollListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "MainConfigActivity";

    private boolean mIsInititalized = false;
    private GoogleApiClient mGoogleApiClient;

    private TextView mHeader;
    private WearableListView mConfigListView;
    private BoxInsetLayout mConfigContent;

    private ArrayList<DataMap> mCalendarList = null;
    public enum ConfigType
    {
        CT_Lv1,
        CT_Calendar,
        CT_Timer,
        CT_Tomato,

        CT_Unknown
    }
    private ConfigType mConfigType = ConfigType.CT_Lv1;

    private int mDataTomatoWork = WatchFaceUtil.DEFAULT_TOMATO_WORK;
    private int mDataTomatoRelax = WatchFaceUtil.DEFAULT_TOMATO_RELAX;
    private int mDataTomatoRelaxLong = WatchFaceUtil.DEFAULT_TOMATO_RELAX_LONG;

    private int mDataTimer1 = WatchFaceUtil.DEFAULT_TIMER1;
    private int mDataTimer2 = WatchFaceUtil.DEFAULT_TIMER2;
    private int mDataTimer3 = WatchFaceUtil.DEFAULT_TIMER3;
    private int mDataTimer4 = WatchFaceUtil.DEFAULT_TIMER4;

    private long mCalendarID = WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ID;
    private String mCalendarName = WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_NAME;
    private int mCalendarColor = WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_COLOR;
    private String mCalendarAccountName = WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ACCOUNT_NAME;

    private ArrayList<DataMap> mTomatoEventQueue = null;

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

        Log.d(TAG,"Padding L:" + mConfigContent.getPaddingLeft() + " R:" + mConfigContent.getPaddingRight());
        mConfigListView.setHasFixedSize(true);
        mConfigListView.setClickListener(this);
        mConfigListView.addOnScrollListener(this);


        String strConfigType = getIntent().getExtras().getString("ConfigType","");
        if (strConfigType.equals(ConfigType.CT_Calendar.toString())) {
            mConfigType = ConfigType.CT_Calendar;
            mHeader.setText(getResources().getString(R.string.config_item_lv1_calendar));
        }
        if (strConfigType.equals(ConfigType.CT_Tomato.toString())) {
            mConfigType = ConfigType.CT_Tomato;
            mHeader.setText(getResources().getString(R.string.config_item_lv1_tomato));
        }
        if (strConfigType.equals(ConfigType.CT_Timer.toString())) {
            mConfigType = ConfigType.CT_Timer;
            mHeader.setText(getResources().getString(R.string.config_item_lv1_timer));
        }

        String[] items;
        if (mConfigType == ConfigType.CT_Lv1)
            items = getResources().getStringArray(R.array.config_array_lv1);
        else if (mConfigType == ConfigType.CT_Tomato)
            items = getResources().getStringArray(R.array.config_array_lv2_tomato);
        else if (mConfigType == ConfigType.CT_Timer)
            items = getResources().getStringArray(R.array.config_array_lv2_timer);
        else if (mConfigType == ConfigType.CT_Calendar)
            items = getResources().getStringArray(R.array.config_array_lv2_calendar);
        else {
            items = new String[1];
            items[0] = "Empty @ "+mConfigType.toString();
        }
        mListAdapter = new ConfigItemListAdapter(items);
        mConfigListView.setAdapter(mListAdapter);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
//        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//            mGoogleApiClient.disconnect();
//        }
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (WatchFaceUtil.TIME_PICKER_REQUEST_CODE_TIMER) : {
                if (resultCode == Activity.RESULT_OK) {
                    int sec = data.getIntExtra("TimeSec",Integer.MAX_VALUE);
                    int idx = data.getIntExtra("TimeIdx",Integer.MAX_VALUE);
                    Log.d(TAG,"onActivityResult Timer Idx: "+idx + " Sec: " + sec);
                    if (sec != Integer.MAX_VALUE && idx != Integer.MAX_VALUE)
                    {
                        if (idx == 0) {
                            mDataTimer1 = sec;
                            DataMap configKeysToOverwrite = new DataMap();
                            configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TIMER1, sec);
                            WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
                        }
                        else if (idx == 1) {
                            mDataTimer2 = sec;
                            DataMap configKeysToOverwrite = new DataMap();
                            configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TIMER2, sec);
                            WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
                        }
                        else if (idx == 2) {
                            mDataTimer3 = sec;
                            DataMap configKeysToOverwrite = new DataMap();
                            configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TIMER3, sec);
                            WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
                        }
                        else if (idx == 3) {
                            mDataTimer4 = sec;
                            DataMap configKeysToOverwrite = new DataMap();
                            configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TIMER4, sec);
                            WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
                        }
                        //mListAdapter.notifyDataSetChanged();
                    }
                    // TODO Update your TextView.
                }
                break;
            }
            case (WatchFaceUtil.TIME_PICKER_REQUEST_CODE_TOMATO) : {
                if (resultCode == Activity.RESULT_OK) {
                    int sec = data.getIntExtra("TimeSec",Integer.MAX_VALUE);
                    int idx = data.getIntExtra("TimeIdx",Integer.MAX_VALUE);
                    Log.d(TAG,"onActivityResult Tomato Idx: "+idx + " Sec: " + sec);
                    if (sec != Integer.MAX_VALUE && idx != Integer.MAX_VALUE)
                    {
                        if (idx == 0) {
                            mDataTomatoWork = sec;
                            DataMap configKeysToOverwrite = new DataMap();
                            configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TOMATO_WORK, sec);
                            WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
                        }
                        else if (idx == 1) {
                            mDataTomatoRelax = sec;
                            DataMap configKeysToOverwrite = new DataMap();
                            configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TOMATO_RELAX, sec);
                            WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
                        }
                        else if (idx == 2) {
                            mDataTomatoRelaxLong = sec;
                            DataMap configKeysToOverwrite = new DataMap();
                            configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TOMATO_RELAX_LONG, sec);
                            WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
                        }
                        //mListAdapter.notifyDataSetChanged();
                    }
                    // TODO Update your TextView.
                }
                break;
            }
        }
    }


    @Override // WearableListView.ClickListener
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ConfigItemViewHolder configItemViewHolder = (ConfigItemViewHolder) viewHolder;
        Log.d(TAG," WearableListView.ClickListener :[" + configItemViewHolder.mConfigItem.getLebelName() + "]");

        if (mConfigType == ConfigType.CT_Lv1)
        {
            String strCur = configItemViewHolder.mConfigItem.getLebelName();
            String strCal = getResources().getString(R.string.config_item_lv1_calendar);
            String strTomato = getResources().getString(R.string.config_item_lv1_tomato);
            String strTimer = getResources().getString(R.string.config_item_lv1_timer);
            String strClearEventQueue = getResources().getString(R.string.config_item_lv1_clear_event_queue);

            ConfigType ctype = ConfigType.CT_Unknown;


//            if (strCur.equals(strCal)) {
//                Intent i = new Intent(getApplicationContext(), CalendarPickerActivity.class);
//                startActivity(i);
//            }
//            else
            if (strCur.equals(strClearEventQueue)) {
                if (mTomatoEventQueue != null && mTomatoEventQueue.size()!=0)
                {
                    DataMap emtpyQueue = new DataMap();
                    emtpyQueue.putDataMapArrayList(WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE,new ArrayList<DataMap>());
                    WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient,emtpyQueue);
                }
            }
            else {
                if (strCur.equals(strTomato))
                    ctype = ConfigType.CT_Tomato;
                else if (strCur.equals(strTimer))
                    ctype = ConfigType.CT_Timer;
                else if (strCur.equals(strCal))
                    ctype = ConfigType.CT_Calendar;


                if (ctype != ConfigType.CT_Unknown) {
                    Intent i = new Intent(getApplicationContext(), MainConfigActivity.class);
                    i.putExtra("ConfigType", ctype.toString());
                    startActivity(i);
                }
            }

        }
        else if (mConfigType == ConfigType.CT_Calendar)
        {
            int idx = configItemViewHolder.getAdapterPosition();
            if (mCalendarList!=null && idx>=0 && idx<mCalendarList.size())
            {
                DataMap calMap = mCalendarList.get(idx);
                DataMap config = new DataMap();

                mCalendarID = calMap.getLong(CalendarContract.Calendars._ID, 0);
                mCalendarName = calMap.getString(CalendarContract.Calendars.NAME, "");
                mCalendarAccountName = calMap.getString(CalendarContract.Calendars.ACCOUNT_NAME, WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ACCOUNT_NAME);
                mCalendarColor = calMap.getInt(CalendarContract.Calendars.CALENDAR_COLOR, WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_COLOR);

                config.putLong(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID, mCalendarID);
                config.putString(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME, mCalendarName);
                config.putString(WatchFaceUtil.KEY_TOMATO_CALENDAR_ACCOUNT_NAME, mCalendarAccountName);
                config.putInt(WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR, mCalendarColor);

                WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, config);
                finish();
            }
        }
        else if (mConfigType == ConfigType.CT_Timer)
        {
            Intent i = new Intent(getApplicationContext(), WatchTimePickerActivity.class);
            i.putExtra("TimePickerName",configItemViewHolder.mConfigItem.getSubLebelName());
            int idx = configItemViewHolder.getAdapterPosition();
            i.putExtra("TimeIdx",idx);
            switch (idx)
            {
                case 0:
                    i.putExtra("TimeSec",mDataTimer1);
                    break;
                case 1:
                    i.putExtra("TimeSec",mDataTimer2);
                    break;
                case 2:
                    i.putExtra("TimeSec",mDataTimer3);
                    break;
                case 3:
                    i.putExtra("TimeSec",mDataTimer4);
                    break;
                default:
                    i.putExtra("TimeSec",0);
                    break;

            }
            startActivityForResult(i, WatchFaceUtil.TIME_PICKER_REQUEST_CODE_TIMER);
        }
        else if (mConfigType == ConfigType.CT_Tomato)
        {
            Intent i = new Intent(getApplicationContext(), WatchTimePickerActivity.class);
            i.putExtra("TimePickerName",configItemViewHolder.mConfigItem.getSubLebelName());
            int idx = configItemViewHolder.getAdapterPosition();
            i.putExtra("TimeIdx",idx);
            switch (idx)
            {
                case 0:
                    i.putExtra("TimeSec",mDataTomatoWork);
                    break;
                case 1:
                    i.putExtra("TimeSec",mDataTomatoRelax);
                    break;
                case 2:
                    i.putExtra("TimeSec",mDataTomatoRelaxLong);
                    break;
                default:
                    i.putExtra("TimeSec",0);
                    break;

            }
            startActivityForResult(i, WatchFaceUtil.TIME_PICKER_REQUEST_CODE_TOMATO);
        }
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




//    private void updateConfigDataItem(final int backgroundColor) {
//        DataMap configKeysToOverwrite = new DataMap();
//        configKeysToOverwrite.putInt(DigitalWatchFaceUtil.KEY_BACKGROUND_COLOR,
//                backgroundColor);
//        DigitalWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
//    }


    private class ConfigItemListAdapter extends WearableListView.Adapter {
        private String[] mItems;
        private int[] mColors = null;

        public ConfigItemListAdapter(String[] items) {
            mItems = items;
        }

        public void setItems(String[] items)
        {
            mItems = items;
        }

        public void setColors(int[] colors)
        {
            mColors = colors;
        }


        @Override
        public ConfigItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ConfigItemViewHolder(new ConfigItem(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            ConfigItemViewHolder configItemViewHolder = (ConfigItemViewHolder) holder;
            String itemName = mItems[position];
            if (mColors != null && position < mColors.length)
            {
                configItemViewHolder.mConfigItem.setColor(mColors[position]);
                configItemViewHolder.mConfigItem.setCircleBorderColor(0);
            }

            if (mConfigType == ConfigType.CT_Lv1) {
                configItemViewHolder.mConfigItem.setItemName(itemName);
                if (mCalendarName.length()!=0 && itemName.equals(getResources().getString(R.string.config_item_lv1_calendar))){
                    configItemViewHolder.mConfigItem.setSubLebelName(mCalendarName);
                    configItemViewHolder.mConfigItem.setColor(mCalendarColor);
                }

                if (itemName.equals(getResources().getString(R.string.config_item_lv1_clear_event_queue)))
                {
                    String subTitle = "";
                    if (mTomatoEventQueue==null || mTomatoEventQueue.size()==0)
                        subTitle = "No event in queue";
                    else if (mTomatoEventQueue.size() == 1)
                        subTitle = "Only one event in queue";
                    else
                        subTitle = "" + mTomatoEventQueue.size() + " events in queue";
                    configItemViewHolder.mConfigItem.setSubLebelName(subTitle);
                }
            }
            else if (mConfigType == ConfigType.CT_Calendar) {
                configItemViewHolder.mConfigItem.setItemName(itemName);
            }
            else if (mConfigType == ConfigType.CT_Timer) {
                if (!mIsInititalized)
                    configItemViewHolder.mConfigItem.setItemName("");
                else if (position == 0)
                    configItemViewHolder.mConfigItem.setItemName("" + mDataTimer1/60 + " min");
                else if (position == 1)
                    configItemViewHolder.mConfigItem.setItemName("" + mDataTimer2/60 + " min");
                else if (position == 2)
                    configItemViewHolder.mConfigItem.setItemName("" + mDataTimer3/60 + " min");
                else if (position == 3)
                    configItemViewHolder.mConfigItem.setItemName("" + mDataTimer4/60 + " min");
                configItemViewHolder.mConfigItem.setSubLebelName(itemName);
            }
            else if (mConfigType == ConfigType.CT_Tomato) {
                if (!mIsInititalized)
                    configItemViewHolder.mConfigItem.setItemName("");
                else if (position == 0)
                    configItemViewHolder.mConfigItem.setItemName("" + mDataTomatoWork/60 + " min");
                else if (position == 1)
                    configItemViewHolder.mConfigItem.setItemName("" + mDataTomatoRelax/60 + " min");
                else if (position == 2)
                    configItemViewHolder.mConfigItem.setItemName("" + mDataTomatoRelaxLong/60 + " min");
                configItemViewHolder.mConfigItem.setSubLebelName(itemName);
            }



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


    private void updateConfigDataItemAndUiOnStartup() {
        WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                new WatchFaceUtil.FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap startupConfig) {
                        // If the DataItem hasn't been created yet or some keys are missing,
                        // use the default values.
                        setDefaultValuesForMissingConfigKeys(startupConfig);
                        WatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);

                        Log.d(TAG, "onConfigDataMapFetched");
                        updateUiForConfigDataMap(startupConfig);
                        mIsInititalized = true;
                    }
                }
        );
    }

    private void setDefaultValuesForMissingConfigKeys(DataMap config) {
        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TIMER1,
                WatchFaceUtil.DEFAULT_TIMER1);
        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TIMER2,
                WatchFaceUtil.DEFAULT_TIMER2);
        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TIMER3,
                WatchFaceUtil.DEFAULT_TIMER3);
        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TIMER4,
                WatchFaceUtil.DEFAULT_TIMER4);

        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TOMATO_WORK,
                WatchFaceUtil.DEFAULT_TOMATO_WORK);
        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TOMATO_RELAX,
                WatchFaceUtil.DEFAULT_TOMATO_RELAX);
        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TOMATO_RELAX_LONG,
                WatchFaceUtil.DEFAULT_TOMATO_RELAX_LONG);

        addIntKeyIfMissingLong(config, WatchFaceUtil.KEY_TOMATO_CALENDAR_ID,
                WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ID);
        addIntKeyIfMissingString(config, WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME,
                WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_NAME);
        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR,
                WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_COLOR);
        addIntKeyIfMissingString(config, WatchFaceUtil.KEY_TOMATO_CALENDAR_ACCOUNT_NAME,
                WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ACCOUNT_NAME);
    }

    private void addIntKeyIfMissingInt(DataMap config, String key, int sec) {
        if (!config.containsKey(key)) {
            Log.d(TAG,"addIntKeyIfMissing  key: " + key + "  sec: " + sec);
            config.putInt(key, sec);
        }
    }

    private void addIntKeyIfMissingLong(DataMap config, String key, long value) {
        if (!config.containsKey(key)) {
            Log.d(TAG,"addIntKeyIfMissing  key: " + key + "  value: " + value);
            config.putLong(key, value);
        }
    }

    private void addIntKeyIfMissingString(DataMap config, String key, String str) {
        if (!config.containsKey(key)) {
            Log.d(TAG,"addIntKeyIfMissing  key: " + key + "  str: " + str);
            config.putString(key, str);
        }
    }

    @Override // DataApi.DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }

            DataItem dataItem = dataEvent.getDataItem();
            if (!dataItem.getUri().getPath().equals(
                    WatchFaceUtil.PATH_WITH_FEATURE)) {
                continue;
            }

            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            DataMap config = dataMapItem.getDataMap();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Config DataItem updated:" + config);
            }
            Log.d(TAG,"onDataChanged");
            updateUiForConfigDataMap(config);
        }
    }

    private void updateUiForConfigDataMap(final DataMap config) {
        boolean uiUpdated = false;
        for (String configKey : config.keySet()) {
            if (!config.containsKey(configKey)) {
                continue;
            }

            int newTime = -1;

            if (configKey.equals(WatchFaceUtil.KEY_TIMER1)) {
                newTime = mDataTimer1 = config.getInt(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TIMER2)) {
                newTime =  mDataTimer2 = config.getInt(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TIMER3)) {
                newTime = mDataTimer3 = config.getInt(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TIMER4)) {
                newTime = mDataTimer4 = config.getInt(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_WORK)) {
                newTime =  mDataTomatoWork = config.getInt(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_RELAX)) {
                newTime =  mDataTomatoRelax = config.getInt(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_RELAX_LONG)) {
                newTime = mDataTomatoRelaxLong = config.getInt(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID)) {
                mCalendarID = config.getLong(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR)) {
                mCalendarColor = config.getInt(configKey);uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME)) {
                mCalendarName = config.getString(configKey);uiUpdated = true;
            }else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE)) {
                mTomatoEventQueue = config.getDataMapArrayList(configKey);
                uiUpdated = true;
                for (DataMap map : mTomatoEventQueue)
                {
                    Log.d(TAG,map.toString());
                }
                Log.d(TAG,WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE + " size is " + mTomatoEventQueue.size());
            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_LIST)) {
                mCalendarList = config.getDataMapArrayList(configKey);
                if (mConfigType == ConfigType.CT_Calendar && mCalendarList.size()!=0)
                {
                    String[] calNameArray = new String[mCalendarList.size()];
                    int[] calColorArray = new int[mCalendarList.size()];
                    for (int i=0;i<mCalendarList.size();i++)
                    {
                        calNameArray[i] = mCalendarList.get(i).getString(CalendarContract.Calendars.NAME,"");
                        calColorArray[i] = mCalendarList.get(i).getInt(CalendarContract.Calendars.CALENDAR_COLOR, 0);
                    }
                    mListAdapter.setItems(calNameArray);
                    mListAdapter.setColors(calColorArray);
                    mListAdapter.notifyDataSetChanged();
                    //mConfigListView.setAdapter(mListAdapter);
                    //mC
                }
            } else {
                Log.w(TAG, "Ignoring unknown config key: " + configKey);
            }

            Log.d(TAG, "updateUiForConfigDataMap configKey:" + configKey + " sec: " + newTime);

        }

        if (uiUpdated)
        {
            mListAdapter.notifyDataSetChanged();
        }

    }


    /**
     * Updates the color of a UI item according to the given {@code configKey}. Does nothing if
     * {@code configKey} isn't recognized.
     *
     * @return whether UI has been updated
     */
//    private boolean updateUiForKey(String configKey, int sec) {
//        if (configKey.equals(WatchFaceUtil.KEY_TIMER1)) {
//            mDataTimer1 = sec;
//        } else if (configKey.equals(WatchFaceUtil.KEY_TIMER2)) {
//            mDataTimer2 = sec;
//        } else if (configKey.equals(WatchFaceUtil.KEY_TIMER3)) {
//            mDataTimer3 = sec;
//        } else if (configKey.equals(WatchFaceUtil.KEY_TIMER4)) {
//            mDataTimer4 = sec;
//        } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_WORK)) {
//            mDataTomatoWork = sec;
//        } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_RELAX)) {
//            mDataTomatoRelax = sec;
//        } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_RELAX_LONG)) {
//            mDataTomatoRelaxLong = sec;
//        } else {
//            Log.w(TAG, "Ignoring unknown config key: " + configKey);
//            return false;
//        }
//        return true;
//    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        if (!mIsInititalized)
            updateConfigDataItemAndUiOnStartup();
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }
    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }
}
