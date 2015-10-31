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

}
