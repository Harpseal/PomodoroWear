package io.harpseal.pomodorowear;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DataItemResult> {

    private static final String TAG = "MainActivity";

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
    private int mSelectedCalendarListIdx = -1;
    private long mSelectedCalendarID = -1;

    private FloatingActionButton mFAB;
    private android.support.design.widget.AppBarLayout mAppbar;
    private TextView mTextTitleDay;
    private TextView mTextTitleDate;
    private TextView mTextTitleCalendar;
    private Spinner mCalendarSpinner;

    private AlertDialog mCalendarAlertDialog;
    private View mCCView;
    private CompactCalendarView mCCV;
    private Date mCurrentDate;
    private Date mTempDate;
    //private Calendar mCurrentCalendar = Calendar.getInstance(Locale.getDefault());
    private SimpleDateFormat dateFormatForMonth = new SimpleDateFormat("MMM - yyyy", Locale.getDefault());

    private AlertDialog mTagAlertDialog;
    private DynamicListView mTagDynListView;

    private WatchFaceUtil.PomodoroTagList mPromodoroTagList;
    private DynamicArrayAdapter mTagDynAdapter;

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFAB = (FloatingActionButton) findViewById(R.id.fab);
        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                //sendMessageToWear("MsgString","string~~");
                //sendMessageToWear("PhoneBattery",98);

//                new SendActivityPhoneMessage("PhoneBattery", 96).start();
//                sendMessageToWear("PhoneBatteryPeer", 98);
                long curTimeMS = System.currentTimeMillis();

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(curTimeMS);
                updateEventListByDay(cal.getTime());

            }
        });

        mAppbar = (android.support.design.widget.AppBarLayout)findViewById(R.id.appbar);

        mTextTitleDay = (TextView) findViewById(R.id.toolbar_day);
        mTextTitleDate = (TextView) findViewById(R.id.toolbar_date);
        mTextTitleCalendar = (TextView) findViewById(R.id.toolbar_calendar);

        mCalendarSpinner = (Spinner) findViewById(R.id.toolbar_calendar_spinner);
        //mCalendarSpinner.setVisibility();
        mCalendarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (position >= 0 && position < mCalendarList.size()) {

                    Log.d(TAG,"onItemSelected mSelectedCalendarID: " + position + " -> " + mSelectedCalendarID);
                    if (mSelectedCalendarID != -1) {
                        mSelectedCalendarListIdx = position;
                        if (mSelectedCalendarID != mCalendarList.get(mSelectedCalendarListIdx).id)
                            sendCalendarConfigUpdateMessage();
                        mSelectedCalendarID = mCalendarList.get(mSelectedCalendarListIdx).id;
                        mTextTitleCalendar.setText(mCalendarList.get(mSelectedCalendarListIdx).name);

                    }
                }
                //Toast.makeText(mContext, "你選的是"+list[position], Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });

        LayoutInflater inflater = getLayoutInflater();
        mCCView = inflater.inflate(R.layout.compact_calendar_dialog_layout, null);
        mCCV = (CompactCalendarView)mCCView.findViewById(R.id.compactcalendar_view);

        mCurrentDate = new Date();
        mTempDate = new Date();
        updateTitleDate();

        mCCV.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                //mCurrentCalendar.setTime(dateClicked);
                mTempDate = dateClicked;
                Log.d("MainActivity", "inside onclick " + dateClicked);

                updateEventListByDay(dateClicked);
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                mCalendarAlertDialog.setTitle(dateFormatForMonth.format(firstDayOfNewMonth));
            }
        });
        mCCV.drawSmallIndicatorForEvents(true);
        //mCCV.setLocale(Locale.CHINESE);
        mCCV.setUseThreeLetterAbbreviation(true);


        AlertDialog.Builder adBuilder = new AlertDialog.Builder(MainActivity.this)
                .setTitle(dateFormatForMonth.format(mCCV.getFirstDayOfCurrentMonth()))
                        //.setMessage("Click to schedule or view events.")
                .setView(mCCView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //do nothing...yet
                        //mCalendarAlertDialog
                        mCurrentDate = mTempDate;
                        updateTitleDate();
                        Log.d("MainActivity", "OK at time: " + mCurrentDate.toString());
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                                Log.d("MainActivity", "Cancel at time: " + mTempDate.toString());
                                dialog.dismiss();

                            }
                        }
                );

        mCalendarAlertDialog = adBuilder.create();
        //mCalendarAlertDialog.getWindow().setLayout(RadioGroup.LayoutParams.WRAP_CONTENT,600);
        mCalendarAlertDialog.getWindow().setLayout(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);


        mTagDynListView = new DynamicListView(this);
        mTagDynListView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        mPromodoroTagList = new WatchFaceUtil.PomodoroTagList();
        mPromodoroTagList.setByStringArray(WatchFaceUtil.DEFAULT_TOMATO_TAGS);


        mTagDynAdapter = new DynamicArrayAdapter(this, R.layout.dyn_text_view, mPromodoroTagList);

        mTagDynListView.setDynamicArrayList(mPromodoroTagList);
        mTagDynListView.setAdapter(mTagDynAdapter);
        mTagDynListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mTagDynListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Tag Editing");

// Set up the input
                final android.widget.EditText input = new android.widget.EditText(MainActivity.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);// | InputType.TYPE_TEXT_VARIATION_PASSWORD
                if (position>=0 && position<mPromodoroTagList.size())
                    input.setText(mPromodoroTagList.get(position).getName());
                builder.setView(input);

// Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //m_Text = input.getText().toString();
                        Log.v(TAG, "Text input :[" + input.getText().toString() + "]");
                        mPromodoroTagList.get(position).setName(input.getText().toString());
                        mTagDynAdapter.setList(mPromodoroTagList);
                        mTagDynAdapter.notifyDataSetChanged();
                        mTagAlertDialog.show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        mTagAlertDialog.show();
                    }
                });
                builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                        //dialog.dismiss();
                        mPromodoroTagList.remove(position);
                        mTagDynAdapter.setList(mPromodoroTagList);
                        mTagDynAdapter.notifyDataSetChanged();
                        mTagAlertDialog.show();


                    }
                });

                builder.show();
            }
        });

        adBuilder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Tags")
                        //.setMessage("Click to schedule or view events.")
                .setView(mTagDynListView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendConfigUpdateMessage(WatchFaceUtil.KEY_TOMATO_TAG_LIST, mPromodoroTagList.toDataMapArray());
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                            dialog.dismiss();

                        }
                    }
                ).setNeutralButton("Add", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                                //dialog.dismiss();

                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setTitle("Create a new Tag");

// Set up the input
                                final android.widget.EditText input = new android.widget.EditText(MainActivity.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                                input.setInputType(InputType.TYPE_CLASS_TEXT);// | InputType.TYPE_TEXT_VARIATION_PASSWORD
                                builder.setView(input);

// Set up the buttons
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //m_Text = input.getText().toString();
                                        Log.v(TAG, "Text input :[" + input.getText().toString() + "]");
                                        mPromodoroTagList.add(new WatchFaceUtil.PomodoroTag(input.getText().toString(),0));
                                        mTagDynAdapter.setList(mPromodoroTagList);

                                        mTagAlertDialog.show();
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });

                                builder.show();

                            }
                        }
                );
        mTagAlertDialog = adBuilder.create();



        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();



        ComponentName name = getIntent().getParcelableExtra(
                WatchFaceCompanion.EXTRA_WATCH_FACE_COMPONENT);
        if (name !=null)
            Log.d(TAG,"ComponentName:" + name == null? "null" : name.getClassName() );

        //label.setText(label.getText() + " (" + name.getClassName() + ")");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    final static int MSG_SHOW_NO_PEER_DIALOG = 100;
    final static int MSG_SHOW_CONNECT_NODES = 101;
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_NO_PEER_DIALOG:
                    displayNoConnectedDeviceDialog();
                    break;
                case MSG_SHOW_CONNECT_NODES:
                    String MsgString = (String)msg.obj;
                    Toast.makeText(MainActivity.this,MsgString , Toast.LENGTH_LONG).show();
                    break;

            }
            super.handleMessage(msg);
        }
    };

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }

        if (mPeerId != null) {
            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.scheme("wear").path(WatchFaceUtil.PATH_WITH_FEATURE).authority(mPeerId).build();
            Log.d(TAG,"onConnected url: "+uri.toString());
            Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);

        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //mGoogleApiClient.blockingConnect(100, TimeUnit.MILLISECONDS);
                    NodeApi.GetConnectedNodesResult result =
                            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    List<Node> nodes = result.getNodes();
                    for (Node n : nodes)
                        Log.d(TAG,"Node " + n.getId() + "  " + n.getDisplayName());
                    if (nodes.size() > 0) {
                        mPeerId = nodes.get(0).getId();

                        Message message;
                        String obj;
                        if (nodes.size() > 1) {
                            obj = "Warning! There are " + nodes.size() + " nodes\nConnected to " + nodes.get(0).getDisplayName();
                        }
                        else
                        {
                            obj = "Connected to " + nodes.get(0).getDisplayName();
                        }
                        message = mHandler.obtainMessage(MSG_SHOW_CONNECT_NODES,obj);
                        mHandler.sendMessage(message);
                        //else
                        //    Toast.makeText(MainActivity.this, "Connected to " + nodes.get(0).getDisplayName(), Toast.LENGTH_LONG).show();
                        Uri.Builder builder = new Uri.Builder();
                        Uri uri = builder.scheme("wear").path(WatchFaceUtil.PATH_WITH_FEATURE).authority(mPeerId).build();
                        Log.d(TAG, "onConnected url: " + uri.toString());
                        Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(MainActivity.this);
                    }
                    else {
                        mHandler.sendEmptyMessage(MSG_SHOW_NO_PEER_DIALOG);
                        //displayNoConnectedDeviceDialog();
                    }
                }
            }).start();

        }
    }

//    private void updateConfigDataItemAndUiOnStartup() {
//        WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
//                new WatchFaceUtil.FetchConfigDataMapCallback() {
//                    @Override
//                    public void onConfigDataMapFetched(DataMap startupConfig) {
//                        // If the DataItem hasn't been created yet or some keys are missing,
//                        // use the default values.
//                        setDefaultValuesForMissingConfigKeys(startupConfig);
//                        WatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);
//
//                        Log.d(TAG, "onConfigDataMapFetched");
//                        updateUiForConfigDataMap(startupConfig);
//                    }
//                }
//        );
//    }

//    private void setDefaultValuesForMissingConfigKeys(DataMap config) {
//        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TIMER1,
//                WatchFaceUtil.DEFAULT_TIMER1);
//        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TIMER2,
//                WatchFaceUtil.DEFAULT_TIMER2);
//        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TIMER3,
//                WatchFaceUtil.DEFAULT_TIMER3);
//        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TIMER4,
//                WatchFaceUtil.DEFAULT_TIMER4);
//
//        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TOMATO_WORK,
//                WatchFaceUtil.DEFAULT_TOMATO_WORK);
//        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TOMATO_RELAX,
//                WatchFaceUtil.DEFAULT_TOMATO_RELAX);
//        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TOMATO_RELAX_LONG,
//                WatchFaceUtil.DEFAULT_TOMATO_RELAX_LONG);

//        addIntKeyIfMissingLong(config, WatchFaceUtil.KEY_TOMATO_CALENDAR_ID,
//                WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ID);
//        addIntKeyIfMissingString(config, WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME,
//                WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_NAME);
//        addIntKeyIfMissingInt(config, WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR,
//                WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_COLOR);
//        addIntKeyIfMissingString(config, WatchFaceUtil.KEY_TOMATO_CALENDAR_ACCOUNT_NAME,
//                WatchFaceUtil.DEFAULT_TOMATO_CALENDAR_ACCOUNT_NAME);
//    }

//    private void addIntKeyIfMissingLong(DataMap config, String key, long value) {
//        if (!config.containsKey(key)) {
//            Log.d(TAG,"addIntKeyIfMissing  key: " + key + "  value: " + value);
//            config.putLong(key, value);
//        }
//    }

    private void updateUiForConfigDataMap(final DataMap config) {
        boolean uiUpdated = false;
        for (String configKey : config.keySet()) {
            if (!config.containsKey(configKey)) {
                continue;
            }

            int newTime = -1;

            if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID)) {
                mSelectedCalendarID = config.getLong(configKey);uiUpdated = true;
                Log.d(TAG,"updateUiForConfigDataMap mSelectedCalendarID :" + mSelectedCalendarID);
                newTime = (int)mSelectedCalendarID;
            }
            else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME)){
                Log.d(TAG,"updateUiForConfigDataMap KEY_TOMATO_CALENDAR_NAME :" + config.getString(configKey));
            }
            else if (configKey.equals(WatchFaceUtil.KEY_TIMER1)) {
                newTime = config.getInt(configKey);
                uiUpdated = true;
            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_TAG_LIST)) {
                mPromodoroTagList.setByDataMapArray(config.getDataMapArrayList(configKey));
                mTagDynAdapter.setList(mPromodoroTagList);
                mTagDynAdapter.notifyDataSetChanged();
                uiUpdated = true;
            }

//            if (configKey.equals(WatchFaceUtil.KEY_TIMER1)) {
//                newTime = mDataTimer1 = config.getInt(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TIMER2)) {
//                newTime =  mDataTimer2 = config.getInt(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TIMER3)) {
//                newTime = mDataTimer3 = config.getInt(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TIMER4)) {
//                newTime = mDataTimer4 = config.getInt(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_WORK)) {
//                newTime =  mDataTomatoWork = config.getInt(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_RELAX)) {
//                newTime =  mDataTomatoRelax = config.getInt(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_RELAX_LONG)) {
//                newTime = mDataTomatoRelaxLong = config.getInt(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID)) {
//                mCalendarID = config.getLong(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR)) {
//                mCalendarColor = config.getInt(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME)) {
//                mCalendarName = config.getString(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_TAGS)) {
//                mTomatoTags = config.getStringArray(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_EVENTS)) {
//                mTomatoEvents = config.getStringArray(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_TYPE)) {
//                mTomatoType = config.getString(configKey);uiUpdated = true;
//            } else if (configKey.equals(WatchFaceUtil.KEY_TOMATO_DATE)) {
//                Long dateInMillis = config.getLong(configKey);uiUpdated = true;
//                Calendar cal = Calendar.getInstance();
//                cal.setTimeInMillis(dateInMillis);
//                mTomatoDate = cal.getTime();
//                uiUpdated = true;
//            } else {
//                Log.w(TAG, "Ignoring unknown config key: " + configKey);
//            }

            Log.d(TAG, "updateUiForConfigDataMap configKey:" + configKey + " sec: " + newTime);

        }


    }

    @Override // ResultCallback<DataApi.DataItemResult>
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap config = dataMapItem.getDataMap();

            Log.d(TAG, "onResult 0");
            updateUiForConfigDataMap(config);

            //updateConfigDataItemAndUiOnStartup();
            Log.d(TAG, "onResult 1");
            updateCalendarList();
            //setUpAllPickers(config);
        } else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.
            //setUpAllPickers(null);
            updateCalendarList();
        }
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }
    }

    @Override // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }

    private void displayNoConnectedDeviceDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        String messageText = getResources().getString(R.string.title_no_device_connected);
        String okText = getResources().getString(R.string.ok_no_device_connected);
        builder.setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        updateCalendarList();
                        //finish();
                    }
                });
        android.app.AlertDialog alert = builder.create();
        alert.show();
    }

    class SendActivityPhoneMessage extends Thread {
        String path;
        DataMap dataMap;

        // Constructor to send a message to the data layer
//            SendActivityPhoneMessage(String p, DataMap data) {
//                path = p;// WatchFaceUtil.PATH_WITH_FEATURE
//                dataMap = data;
//            }

        SendActivityPhoneMessage(DataMap data) {
            path = WatchFaceUtil.PATH_WITH_MESSAGE;
            dataMap = data;
        }

        SendActivityPhoneMessage(String key,int value) {
            path = WatchFaceUtil.PATH_WITH_MESSAGE;
            dataMap = new DataMap();
            dataMap.putInt(key, value);
        }

        SendActivityPhoneMessage(String key,String str) {
            path = WatchFaceUtil.PATH_WITH_MESSAGE;
            dataMap = new DataMap();
            dataMap.putString(key, str);
        }
/*
*             new Thread(new Runnable() {
                @Override
                public void run() {
                    //mGoogleApiClient.blockingConnect(100, TimeUnit.MILLISECONDS);
                    NodeApi.GetConnectedNodesResult result =
                            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    List<Node> nodes = result.getNodes();
                    for (Node n : nodes)
                        Log.d(TAG,"Node " + n.getId() + "  " + n.getDisplayName());
                    if (nodes.size() > 0) {
                        mPeerId = nodes.get(0).getId();

                        Uri.Builder builder = new Uri.Builder();
                        Uri uri = builder.scheme("wear").path(WatchFaceUtil.PATH_WITH_FEATURE).authority(mPeerId).build();
                        Log.d(TAG, "onConnected url: " + uri.toString());
                        Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(MainActivity.this);
                    }
                    else {
                        mHandler.sendEmptyMessage(MSG_SHOW_NO_PEER_DIALOG);
                        //displayNoConnectedDeviceDialog();
                    }
                }
            }).start();
*
*
* */
        public void run() {
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.blockingConnect(100, TimeUnit.MILLISECONDS);
            NodeApi.GetConnectedNodesResult result =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

            List<Node> nodes = result.getNodes();
            for (Node n : nodes)
                Log.d(TAG,"Node " + n.getId() + "  " + n.getDisplayName());
            if (nodes.size() > 0) {
                MessageApi.SendMessageResult sendResult = Wearable.MessageApi.sendMessage(mGoogleApiClient, nodes.get(0).getId(), path, dataMap.toByteArray()).await();
                if (sendResult.getStatus().isSuccess()) {
                    Log.v(TAG, "Phone: Activity Message: {" + dataMap.toString() + "} sent to: " + nodes.get(0).getId());
                } else {
                    // Log an error
                    Log.v(TAG, "Phone ERROR: failed to send Activity Message");
                }
            }
            else
                Log.v(TAG, "Phone ERROR: no node to send....");


        }
    }


    private void sendMessageToWear(String msgKey,int value)
    {
        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putInt(msgKey, value);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, WatchFaceUtil.PATH_WITH_MESSAGE, rawData);
        }
    }


    private void sendConfigUpdateMessage(String configKey, int value) {
        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putInt(configKey, value);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, WatchFaceUtil.PATH_WITH_FEATURE, rawData);

            // if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Sent watch face config message: " + configKey + " -> "
                        + Integer.toHexString(value));
           // }
        }
    }

    private void sendConfigUpdateMessage(String configKey, long value) {
        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putLong(configKey, value);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, WatchFaceUtil.PATH_WITH_FEATURE, rawData);

           // if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Sent watch face config message: " + configKey + " -> "
                        + value);
            //}
        }
    }

    private void sendConfigUpdateMessage(String configKey, String value) {
        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putString(configKey, value);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, WatchFaceUtil.PATH_WITH_FEATURE, rawData);


            //if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Sent watch face config message: " + configKey + " -> "
                        + value);
            //}
        }
    }


    private void sendConfigUpdateMessage(String configKey, String[] array) {
        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putStringArray(configKey, array);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, WatchFaceUtil.PATH_WITH_FEATURE, rawData);

//            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Sent watch face config message: " + configKey + " -> "
                        + array);
                for (String str : array)
                {
                    Log.d(TAG, "[" + str + "]");
                }
//            }
        }
    }

    private void sendConfigUpdateMessage(String configKey, ArrayList<DataMap> arrayMap) {
        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putDataMapArrayList(configKey, arrayMap);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, WatchFaceUtil.PATH_WITH_FEATURE, rawData);

//            if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Sent watch face config message: " + configKey);
            for (DataMap map : arrayMap)
            {
                Log.d(TAG, "[" + map.toString() + "]");
            }
//            }
        }
    }

    private void sendCalendarConfigUpdateMessage()
    {
        if (mSelectedCalendarListIdx <0 ||mSelectedCalendarListIdx>=mCalendarList.size()) return;

        sendConfigUpdateMessage(WatchFaceUtil.KEY_TOMATO_CALENDAR_ID,  mCalendarList.get(mSelectedCalendarListIdx).id);
        sendConfigUpdateMessage(WatchFaceUtil.KEY_TOMATO_CALENDAR_NAME,  mCalendarList.get(mSelectedCalendarListIdx).name);
        sendConfigUpdateMessage(WatchFaceUtil.KEY_TOMATO_CALENDAR_COLOR,  mCalendarList.get(mSelectedCalendarListIdx).color);
        sendConfigUpdateMessage(WatchFaceUtil.KEY_TOMATO_CALENDAR_ACCOUNT_NAME,  mCalendarList.get(mSelectedCalendarListIdx).accountName);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, MainConfigActivity.class);
            startActivity(intent);
            return true;
        }
        else
        if (id == R.id.action_calendar)
        {
            mCalendarAlertDialog.show();

            return true;
        }
        else if (id == R.id.action_tags)
        {
            mTagAlertDialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void updateTitleDate()
    {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTime(mCurrentDate);

        Calendar now = Calendar.getInstance();
        //final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEEE, MMM d";
        final long HOURS = 60 * 60 * 60;
        if(now.get(Calendar.DAY_OF_YEAR) == smsTime.get(Calendar.DAY_OF_YEAR)){
            mTextTitleDay.setText("Today");
            mTextTitleDate.setText(DateFormat.format(dateTimeFormatString, smsTime).toString());
        }else if(now.get(Calendar.DAY_OF_YEAR) - smsTime.get(Calendar.DAY_OF_YEAR) == 1 ){
            mTextTitleDay.setText("Yesterday");
            mTextTitleDate.setText(DateFormat.format(dateTimeFormatString, smsTime).toString());
        }else if(now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)){
            mTextTitleDay.setText("");
            mTextTitleDate.setText(DateFormat.format(dateTimeFormatString, smsTime).toString());
        }else {
            mTextTitleDay.setText("");
            mTextTitleDate.setText(DateFormat.format("MMM dd yyyy", smsTime).toString());
        }

//        currentCalender.setTime(new Date());
//        setToMidnight(currentCalender);
//        long timeToday = currentCalender.getTimeInMillis();
//        currentCalender.setTime(mCurrentDate);
//        setToMidnight(currentCalender);
//        long timeCurrent = currentCalender.getTimeInMillis();
//        if (timeToday == timeCurrent)
//            mTextTitleDate.setText("Today");
//        else
//            mTextTitleDate.setText(dateFormatForMonth.format(mCurrentDate));
    }

    private boolean updateEventListByDay(Date date)
    {
        if (mSelectedCalendarListIdx < 0 || mSelectedCalendarListIdx>= mCalendarList.size()) return false;

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
        int res = this.checkCallingOrSelfPermission(permission);
        if (res == PackageManager.PERMISSION_GRANTED) {

            String selectionClause =
                    "(" + CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTEND + " <= ?)" + " OR " +
                            "(" + CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.ALL_DAY + " = ?)";

            selectionClause = " (" + selectionClause + ") AND (" + CalendarContract.Events.CALENDAR_ID + " = ? )";
            long dtstart,dtend;


            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            dtstart = cal.getTimeInMillis();

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);

            dtend = cal.getTimeInMillis();

            String[] selectionsArgs = new String[]{"" + dtstart, "" + dtend, "" + dtstart, "1" , ""+mCalendarList.get(mSelectedCalendarListIdx).id };

            Cursor eventCursor = getContentResolver()
                    .query(
                            CalendarContract.Events.CONTENT_URI,
                            projection,
                            selectionClause,
                            selectionsArgs, null);
            if (eventCursor.moveToFirst()) {
                do {
                    long id = eventCursor.getLong(0);//CALENDAR_ID
                    String title = eventCursor.getString(1);//TITLE
                    String description = eventCursor.getString(2);//DESCRIPTION
                    long eventStart = eventCursor.getLong(3);
                    long eventEnd = eventCursor.getLong(4);

                    cal.setTimeInMillis(eventStart);
                    String strEventStart = DateFormat.format("YY MM dd HH:mm:ss", cal).toString();

                    cal.setTimeInMillis(eventEnd);
                    String strEventEnd = DateFormat.format("YY MM dd HH:mm:ss", cal).toString();

                    Log.i("MainActivity","event calid:" + id + "  title :" + title + "  des:" + description + "  time:" + strEventStart + " -> " + strEventEnd);

                } while (eventCursor.moveToNext());
            }
            else
                Log.d("MainActivity","eventCursor.moveToFirst() == null");


        }
        else
            return false;

        return true;
    }

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
                    Log.i("MainActivity","id :" + id + "  name :" + displayName + "  accLevel:" + accLevel + "  color:" + Integer.toHexString(color));

                    mCalendarList.add(new CalendarItem(id,displayName,accName,color));
                    // …
                } while (calCursor.moveToNext());
            }

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            long calID = SP.getLong("calendarID", 0);
            String  calName = SP.getString("calendarName", "");
            String  calAccName = SP.getString("calendarAccountName", "");
            if (mCalendarList.size() !=0)
            {
                for (int c=0;c<mCalendarList.size();c++)
                {
                    if (mSelectedCalendarID == mCalendarList.get(c).id)
                    {
                        mSelectedCalendarListIdx = c;
                        break;
                    }
                }

                ArrayAdapter<CalendarItem> calAdapter = new ArrayAdapter<CalendarItem>(MainActivity.this,R.layout.spinner_item, mCalendarList);
                mCalendarSpinner.setAdapter(calAdapter);



                if (mSelectedCalendarListIdx != -1)
                    mCalendarSpinner.setSelection(mSelectedCalendarListIdx);
            }
            if (mCalendarList.size() != 0 && mSelectedCalendarListIdx == -1)
            {
                int c=0;
                for (CalendarItem item : mCalendarList)
                {
                    if (item.id == calID &&
                            item.name.equals(calName)&&
                            item.accountName.equals(calAccName))
                    {
                        mSelectedCalendarListIdx = c;
                        mCalendarSpinner.setSelection(mSelectedCalendarListIdx);
                        SharedPreferences.Editor editor = SP.edit();
                        editor.putLong("calendarID",mCalendarList.get(mSelectedCalendarListIdx).id);
                        editor.putString("calendarName", mCalendarList.get(mSelectedCalendarListIdx).name);
                        editor.putString("calendarAccountName", mCalendarList.get(mSelectedCalendarListIdx).accountName);

                        //mAppbar.setBackgroundColor(mCalendarList.get(mCalendarSelected).color);
                        break;
                    }
                }
                if (mSelectedCalendarListIdx == -1)
                {
                    if (mCalendarList.size() == 1) {
                        mSelectedCalendarListIdx = 0;
                        mTextTitleCalendar.setText(mCalendarList.get(mSelectedCalendarListIdx).name);
                    }
                    else
                    {
                        String[] calNameArray = new String[mCalendarList.size()];
                        c=0;
                        for (CalendarItem item : mCalendarList)
                        {
                            calNameArray[c] = item.name;
                            c++;
                        }

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Please select a calender")
                                .setItems(calNameArray, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mSelectedCalendarListIdx = which;
                                        mSelectedCalendarID = mCalendarList.get(mSelectedCalendarListIdx).id;
                                        mCalendarSpinner.setSelection(mSelectedCalendarListIdx);
                                        Log.d("MainActivity", "mCalendarSelected :" + mSelectedCalendarListIdx);
                                        mTextTitleCalendar.setText(mCalendarList.get(mSelectedCalendarListIdx).name);
                                        sendCalendarConfigUpdateMessage();

                                        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                                        SharedPreferences.Editor editor = SP.edit();
                                        editor.putLong("calendarID",mCalendarList.get(mSelectedCalendarListIdx).id);
                                        editor.putString("calendarName", mCalendarList.get(mSelectedCalendarListIdx).name);
                                        editor.putString("calendarAccountName", mCalendarList.get(mSelectedCalendarListIdx).accountName);


                                    }
                                }).show();

                    }
                }

            }

            return (mCalendarList.size() != 0);
        }
        else
            return  false;

    }

}
