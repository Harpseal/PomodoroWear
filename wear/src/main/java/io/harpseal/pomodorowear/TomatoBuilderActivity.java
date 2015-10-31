package io.harpseal.pomodorowear;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.speech.RecognizerIntent;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
import java.util.Calendar;
import java.util.TimeZone;

public class TomatoBuilderActivity extends Activity implements
        WearableListView.ClickListener, WearableListView.OnScrollListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "TomatoBuilderActivity";

    private TextView mHeader;
    private android.support.wearable.view.WearableListView mTagListView;
    private android.support.wearable.view.CircledImageView mBtnOK;
    private android.support.wearable.view.CircledImageView mBtnMic;

    private TextView mEventTitle;
    private TextView mEventDescription;

    private BoxInsetLayout mBuilderContent;

    private GoogleApiClient mGoogleApiClient;

    private WatchFaceUtil.PomodoroTagList mPomodoraTagList = new WatchFaceUtil.PomodoroTagList();

    private TagListAdapter mListAdapter;
    private final int RESULT_RECOGNIZE_SPEECH = 100;

    private long mEventDTStart = 0;
    private long mEventDTEnd = 0;
    private long mEventCalendarID = 0;

    private ArrayList<DataMap> mEventQueue = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tomato_builder);

        mPomodoraTagList.setByStringArray(WatchFaceUtil.DEFAULT_TOMATO_TAGS);

        mHeader = (TextView) findViewById(R.id.tomato_builder_header);
        mTagListView = (android.support.wearable.view.WearableListView) findViewById(R.id.tomato_builder_tag_list);

        mEventTitle = (TextView) findViewById(R.id.tomato_builder_text_preview_title);
        mEventDescription = (TextView) findViewById(R.id.tomato_builder_text_preview_description);

        mBuilderContent = (BoxInsetLayout) findViewById(R.id.tomato_builder_content);
        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        mBuilderContent.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            (int) getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                Log.d(TAG, "onApplyWindowInsets con " + v.getPaddingTop() + " " + v.getPaddingEnd() + " " + v.getPaddingBottom());
                return v.onApplyWindowInsets(insets);
            }
        });


        mTagListView.setHasFixedSize(true);
        mTagListView.setClickListener(this);
        mTagListView.addOnScrollListener(this);

        mListAdapter = new TagListAdapter(mPomodoraTagList);
        mTagListView.setAdapter(mListAdapter);


        mBtnMic = (android.support.wearable.view.CircledImageView)findViewById(R.id.tomato_builder_btn_mic);
        mBtnOK = (android.support.wearable.view.CircledImageView)findViewById(R.id.tomato_builder_btn_ok);


        mBtnMic.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {

                try {
                    // Intent作成
                    Intent intent = new Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // ACTION_WEB_SEARCH
                    intent.putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            "VoiceRecognitionTest"); // 可以替换成您喜欢的文字

                    // Intent发行
                    startActivityForResult(intent, RESULT_RECOGNIZE_SPEECH);
                } catch (ActivityNotFoundException e) {
                    // 如果没有安装可以应答这个Intent的Activity的时候，显示一条消息
                    Log.d(TAG, "ActivityNotFoundException");
                }

//                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                        RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
//                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Command me");
//                startActivityForResult(intent, RESULT_RECOGNIZE_SPEECH);

            }
        });

        mBtnOK.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {

                Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                DataMap eventDataMap = new DataMap();
                //eventDataMap.putLong(WatchFaceUtil.MSG_ID_KEY, cal.getTimeInMillis());
                //eventDataMap.putString(WatchFaceUtil.MSG_SENDER_KEY, WatchFaceUtil.MSG_SENDER_EVENT_BUILDER);
                //eventDataMap.putInt(WatchFaceUtil.MSG_TYPE_KEY, WatchFaceUtil.MSG_TYPE_CREATE_EVENT);

                eventDataMap.putLong(CalendarContract.Events.DTSTART, mEventDTStart);
                eventDataMap.putLong(CalendarContract.Events.DTEND, mEventDTEnd);
                eventDataMap.putLong(CalendarContract.Events.CALENDAR_ID, mEventCalendarID);

                eventDataMap.putString(CalendarContract.Events.TITLE, mEventTitle.getText().toString());
                if (mEventDescription.getVisibility() == View.VISIBLE && mEventDescription.getText().length()!=0)
                    eventDataMap.putString(CalendarContract.Events.DESCRIPTION, mEventDescription.getText().toString());
                else
                    eventDataMap.putString(CalendarContract.Events.DESCRIPTION, "");

                if (mEventQueue == null)
                    mEventQueue = new ArrayList<DataMap>();

                mEventQueue.add(eventDataMap);

                DataMap configKeysToOverwrite = new DataMap();
                configKeysToOverwrite.putDataMapArrayList(WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE, mEventQueue);
                WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);

                finish();
            }
        });


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        Intent intent = getIntent();
        mEventDTStart = intent.getLongExtra(CalendarContract.Events.DTSTART,0);
        mEventDTEnd = intent.getLongExtra(CalendarContract.Events.DTEND,0);
        mEventCalendarID = intent.getLongExtra(CalendarContract.Events.CALENDAR_ID,0);


        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        String infoFromExtra = "CalID " + mEventCalendarID + " ";
        cal.setTimeInMillis(mEventDTStart);
        infoFromExtra += DateFormat.format("yyyy/MM/dd HH:mm:ss", cal).toString();

        cal.setTimeInMillis(mEventDTEnd);
        infoFromExtra += " -> " + DateFormat.format("yyyy/MM/dd HH:mm:ss", cal).toString();
        Log.d(TAG,infoFromExtra);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_RECOGNIZE_SPEECH: {
                if (resultCode == RESULT_OK)
                {
                    String resultsString = "";

                    // 结果文字列数组
                    ArrayList<String> results = data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS);

                    for (int i = 0; i< results.size(); i++) {
                        // 结合复数个文字列
                        resultsString += results.get(i);
                    }

                    Log.d(TAG, "resultsString:[" + resultsString + "]");
                    mEventDescription.setText(resultsString);
                    mEventDescription.setVisibility(View.VISIBLE);

                }
                else
                {
                    Log.d(TAG, "resultsString RESULT_CANCEL");
                    mEventDescription.setText("");
                    mEventDescription.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
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


    @Override // WearableListView.ClickListener
    public void onClick(WearableListView.ViewHolder viewHolder) {
        TagViewHolder tagViewHolder = (TagViewHolder) viewHolder;

        int itemId = (int)tagViewHolder.getAdapterPosition();

        Log.d(TAG, " WearableListView.ClickListener " + itemId + " :[" + tagViewHolder.mTagItem.getLebelName() + "]");

        mListAdapter.setSelected(itemId, !mListAdapter.getSelected(itemId));
        mListAdapter.notifyDataSetChanged();

        String eventTitle = "[30]";
        if (mEventDescription.getVisibility() == View.VISIBLE && mEventDescription.getText().length()!=0)
            eventTitle+="[N]";

        int nTags = mListAdapter.getItemCount();
        for (int t=0;t<nTags;t++)
        {
            if (mListAdapter.getSelected(t))
            {
                eventTitle+=mListAdapter.getTag(t);
            }
        }
        mEventTitle.setText(eventTitle);

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

    private class TagListAdapter extends WearableListView.Adapter {
        private WatchFaceUtil.PomodoroTagList mTagList;
        private boolean[] mSelected;
        private final float mCircleRadius = 16;

        public TagListAdapter(WatchFaceUtil.PomodoroTagList taglist) {
            setTags(taglist);
        }

        public void setTags(WatchFaceUtil.PomodoroTagList taglist)
        {
            mTagList = taglist;
            mSelected = new boolean[taglist.size()];
            for (int i=0;i<mSelected.length;i++) {
                mSelected[i] = taglist.get(i).getIsEnableDefault();
            }
        }

        public void setSelected(int pos,boolean isSelected)
        {
            if (pos>=0 && pos<mSelected.length)
                mSelected[pos] = isSelected;
        }
        public boolean getSelected(int pos)
        {
            if (pos>=0 && pos<mSelected.length)
                return mSelected[pos];
            return false;
        }

        public String getTag(int pos)
        {
            if (pos>=0 && pos<mSelected.length)
                return mTagList.get(pos).getName();
            return "";
        }
        @Override
        public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TagViewHolder(new ConfigItem(parent.getContext(),mCircleRadius));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            TagViewHolder configItemViewHolder = (TagViewHolder) holder;
            String itemName = mTagList.get(position).getName();
            if (mSelected[position])
                configItemViewHolder.mTagItem.setColor(Color.WHITE);
            else
                configItemViewHolder.mTagItem.setColor(Color.TRANSPARENT);

            configItemViewHolder.mTagItem.setItemName(itemName);

//            if (mConfigType == ConfigType.CT_Lv1) {
//                configItemViewHolder.mConfigItem.setItemName(itemName);
//                if (position == 0 && mCalendarName.length()!=0) {
//                    configItemViewHolder.mConfigItem.setSubLebelName(mCalendarName);
//                    configItemViewHolder.mConfigItem.setColor(mCalendarColor);
//                }
//            }
//            else if (mConfigType == ConfigType.CT_Calendar) {
//                configItemViewHolder.mConfigItem.setItemName(itemName);
//            }
//            else if (mConfigType == ConfigType.CT_Timer) {
//                if (!mIsInititalized)
//                    configItemViewHolder.mConfigItem.setItemName("");
//                else if (position == 0)
//                    configItemViewHolder.mConfigItem.setItemName("" + mDataTimer1/60 + " min");
//                else if (position == 1)
//                    configItemViewHolder.mConfigItem.setItemName("" + mDataTimer2/60 + " min");
//                else if (position == 2)
//                    configItemViewHolder.mConfigItem.setItemName("" + mDataTimer3/60 + " min");
//                else if (position == 3)
//                    configItemViewHolder.mConfigItem.setItemName("" + mDataTimer4/60 + " min");
//                configItemViewHolder.mConfigItem.setSubLebelName(itemName);
//            }
//            else if (mConfigType == ConfigType.CT_Tomato) {
//                if (!mIsInititalized)
//                    configItemViewHolder.mConfigItem.setItemName("");
//                else if (position == 0)
//                    configItemViewHolder.mConfigItem.setItemName("" + mDataTomatoWork/60 + " min");
//                else if (position == 1)
//                    configItemViewHolder.mConfigItem.setItemName("" + mDataTomatoRelax/60 + " min");
//                else if (position == 2)
//                    configItemViewHolder.mConfigItem.setItemName("" + mDataTomatoRelaxLong/60 + " min");
//                configItemViewHolder.mConfigItem.setSubLebelName(itemName);
//            }



            RecyclerView.LayoutParams layoutParams =
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            int colorPickerItemMargin = (int) getResources()
                    .getDimension(R.dimen.digital_config_item_margin);
            // Add margins to first and last item to make it possible for user to tap on them.
            if (position == 0) {
                layoutParams.setMargins(0, colorPickerItemMargin, 0, 0);
            } else if (position == mTagList.size() - 1) {
                layoutParams.setMargins(0, 0, 0, colorPickerItemMargin);
            } else {
                layoutParams.setMargins(0, 0, 0, 0);
            }
            configItemViewHolder.itemView.setLayoutParams(layoutParams);
        }

        @Override
        public int getItemCount() {
            return mTagList.size();
        }
    }


    private static class TagViewHolder extends WearableListView.ViewHolder {
        public final ConfigItem mTagItem;

        public TagViewHolder(ConfigItem colorItem) {
            super(colorItem);
            mTagItem = colorItem;
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
                    }
                }
        );
    }

    private void setDefaultValuesForMissingConfigKeys(DataMap config) {
        //addIntKeyIfMissingStringArray(config,WatchFaceUtil.KEY_TOMATO_TAGS,WatchFaceUtil.DEFAULT_TOMATO_TAGS);
        addIntKeyIfMissingDataMapArray(config, WatchFaceUtil.KEY_TOMATO_TAG_LIST,mPomodoraTagList.toDataMapArray());
        addIntKeyIfMissingDataMapArray(config, WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE, null);
    }

    private void addIntKeyIfMissingStringArray(DataMap config, String key, String[] array) {
        if (!config.containsKey(key)) {
            Log.d(TAG,"addIntKeyIfMissing  key: " + key);
            config.putStringArray(key, array);
        }
    }
    private void addIntKeyIfMissingDataMapArray(DataMap config, String key, ArrayList<DataMap> array) {
        if (!config.containsKey(key)) {
            Log.d(TAG, "addIntKeyIfMissing  key: " + key);
            if (array == null)
                array = new ArrayList<DataMap>();
            config.putDataMapArrayList(key, array);
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

            if (configKey.equals(WatchFaceUtil.KEY_TOMATO_TAG_LIST)) {
                ArrayList<DataMap> arrayMap = config.getDataMapArrayList(WatchFaceUtil.KEY_TOMATO_TAG_LIST);
                mPomodoraTagList.setByDataMapArray(arrayMap);
                uiUpdated = true;

            }else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE)) {
                mEventQueue = config.getDataMapArrayList(configKey);
                uiUpdated = true;
                for (DataMap map : mEventQueue)
                {
                    Log.d(TAG,map.toString());
                }
            }else {
                Log.w(TAG, "Ignoring unknown config key: " + configKey);
            }


        }

        if (uiUpdated)
        {
            mListAdapter.setTags(mPomodoraTagList);
            mListAdapter.notifyDataSetChanged();
        }

    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
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
