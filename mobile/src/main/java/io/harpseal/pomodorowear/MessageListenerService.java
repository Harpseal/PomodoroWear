package io.harpseal.pomodorowear;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class MessageListenerService extends WearableListenerService {
    private final String TAG = "MessageListenerService";
    private int mNumberOfMessage = 0;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(WatchFaceUtil.PATH_WITH_MESSAGE)) {
            mNumberOfMessage ++ ;
            //intent.putExtra("Message", new String(messageEvent.getData()));
            byte[] rawData = messageEvent.getData();
            DataMap messageMap = DataMap.fromByteArray(rawData);
            Log.d(TAG, "Msg:" + messageMap.toString());
            if (messageMap.containsKey(WatchFaceUtil.MSG_TYPE_KEY))
            {
                int key = messageMap.getInt(WatchFaceUtil.MSG_TYPE_KEY);
                if (key == WatchFaceUtil.MSG_TYPE_CREATE_EVENT)
                {
                    Log.d(TAG,"MSG_TYPE_CREATE_EVENT(NEW)");
                    new Thread(){
                        @Override
                        public void run()
                        {
                            processEventQueue();
                        }
                    }.start();
                }
                else if (key == WatchFaceUtil.MSG_TYPE_BATTERY)
                {
                    Log.d(TAG,"MSG_TYPE_BATTERY");
                    new Thread(){
                        @Override
                        public void run()
                        {
                            uploadPhoneBattery(MessageListenerService.this);
                        }
                    }.start();
                }
                else if (key == WatchFaceUtil.MSG_TYPE_UPDATE_CALENDAR_LIST)
                {
                    new Thread(){
                        @Override
                        public void run()
                        {
                            uploadCalendarList(MessageListenerService.this);
                        }
                    }.start();
                }
            }
            return;
        }

    }

    private void showToast(String message) {
        Toast.makeText(this, "" + mNumberOfMessage + " : " + message, Toast.LENGTH_LONG).show();
    }

    private class EventItem
    {
        public long start;
        public long end;
        public String title;

        public EventItem(long s,long e,String t)
        {
            start = s;
            end = e;
            title = t;
        }

        public boolean equal(long s,long e,String t)
        {
            return start == s && end == e && title.equalsIgnoreCase(t);
        }
    }
//
//    private Map<Long,Map<Date,ArrayList<EventItem>>> sEventCache = new Map<Long, Map<Date, ArrayList<EventItem>>>();

    private ArrayList<EventItem> getEvents(long calendarID,long timeStart,long timeEnd)
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
        int res = MessageListenerService.this.checkCallingOrSelfPermission(permission);
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

                ArrayList<EventItem> resList = new ArrayList<EventItem>();
                //Calendar cal = Calendar.getInstance();
                do {
                    long id = eventCursor.getLong(0);//CALENDAR_ID
                    String title = eventCursor.getString(1);//TITLE
                    String description = eventCursor.getString(2);//DESCRIPTION
                    long eventStart = eventCursor.getLong(3);
                    long eventEnd = eventCursor.getLong(4);

                    resList.add(new EventItem(eventStart,eventEnd,title));

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

    private void processEventQueue()
    {
        String nodeId;
        GoogleApiClient client;

        String permission;
        permission = "android.permission.READ_CALENDAR";
        int res = MessageListenerService.this.checkCallingOrSelfPermission(permission);
        if (res == PackageManager.PERMISSION_GRANTED) {
            client = new GoogleApiClient.Builder(MessageListenerService.this)
                    //.addConnectionCallbacks(this)
                    //.addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            if (!client.isConnected()) {
                ConnectionResult connRes = client.blockingConnect(1000, TimeUnit.MILLISECONDS);
                if (!connRes.isSuccess())
                {
                    Log.e(TAG,"Can't connect to GoogleApiClient...");
                    return;
                }

            }
            NodeApi.GetConnectedNodesResult result =
                    Wearable.NodeApi.getConnectedNodes(client).await();

            List<Node> nodes = result.getNodes();
            //for (Node n : nodes)
            //    Log.d(TAG,"Node " + n.getId() + "  " + n.getDisplayName());
            for (Node n : nodes) {
            //for (int idx = 0 ;idx<nodes.size();idx++){
                nodeId = n.getId();

                Log.d(TAG,"Create event for Node " + nodeId + "  " + n.getDisplayName() + " 666");

                Uri.Builder builder = new Uri.Builder();
                Uri uri = builder.scheme("wear").path(WatchFaceUtil.PATH_WITH_FEATURE).authority(nodeId).build();
                Log.d(TAG, "onConnected url: " + uri.toString());
                DataApi.DataItemResult dataItemResult = Wearable.DataApi.getDataItem(client, uri).await();//.setResultCallback(MainActivity.this);

                if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();

                    if (config.containsKey(WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE))
                    {
                        ContentResolver cr = getContentResolver();
                        ArrayList<DataMap> eventQueue = config.getDataMapArrayList(WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE);

                        if (eventQueue == null)
                            Log.d(TAG,"ERROR : eventQueue == null");
                        else if (eventQueue.size() == 0)
                            Log.d(TAG,"No event need to create.");

                        for (int e=0;eventQueue!=null && e<eventQueue.size();e++)
                        {
                            DataMap event = eventQueue.get(e);

                            long dtstart = event.getLong(CalendarContract.Events.DTSTART, 0);
                            long dtend = event.getLong(CalendarContract.Events.DTEND,0);
                            long calID = event.getLong(CalendarContract.Events.CALENDAR_ID, 0);

                            String strTitle = event.getString(CalendarContract.Events.TITLE, "");
                            String strDescription = event.getString(CalendarContract.Events.DESCRIPTION,"");
                            String strTimeZone = event.getString(CalendarContract.Events.EVENT_TIMEZONE,"");

                            if (calID == -1)
                            {
                                Log.d(TAG,"calID == -1, skip. " + event.toString());
                                eventQueue.remove(e);
                                e--;
                                continue;
                            }

                            long timeRange = 5000; //5sec
                            ArrayList<EventItem> existList = getEvents(calID,dtstart-timeRange,dtend+timeRange);
                            if (existList != null && existList.size()!=0)
                            {
                                Log.d(TAG,"event has already created. " + event.toString());
                                eventQueue.remove(e);
                                e--;
                                continue;
                            }


                            ContentValues values = new ContentValues();
                            values.put(CalendarContract.Events.DTSTART, dtstart);
                            values.put(CalendarContract.Events.DTEND, dtend);
                            values.put(CalendarContract.Events.TITLE, strTitle);
                            values.put(CalendarContract.Events.DESCRIPTION, strDescription);
                            values.put(CalendarContract.Events.CALENDAR_ID, calID );
                            values.put(CalendarContract.Events.EVENT_TIMEZONE, strTimeZone.length()==0?TimeZone.getDefault().getID():strTimeZone );

                            final Uri uriEventRes = cr.insert(CalendarContract.Events.CONTENT_URI, values);

                            boolean isSuccess = true;
                            try
                            {
                                int id = Integer.parseInt(uriEventRes.getLastPathSegment());
                                Log.d(TAG,"event " + id + " is created. " + event.toString() + " url: " + uriEventRes.toString());
                            }catch (NumberFormatException nfe)
                            {
                                Log.d(TAG,"event can't be created. " + nfe.toString() + " " + event.toString() + " url: " + uriEventRes.toString());
                                isSuccess = false;
                            }
                            if (isSuccess)
                            {
                                eventQueue.remove(e);
                                e--;
                            }
                            //return
                        }

                        if (eventQueue != null)
                        {
                            DataMap configReturn = new DataMap();
                            configReturn.putDataMapArrayList(WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE, eventQueue);
                            //PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.getDataItem(client, configReturn.as);//.setResultCallback(MainActivity.this);

//                            PutDataMapRequest dataMap = PutDataMapRequest.create(WatchFaceUtil.PATH_WITH_FEATURE);
//                            dataMap.getDataMap().putDataMapArrayList(WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE, eventQueue);
//                            PutDataRequest request = dataMap.asPutDataRequest();
//                            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(client, request);
//                            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
//                                @Override
//                                public void onResult(DataApi.DataItemResult dataItemResult) {
//                                    Log.d(TAG, "PutDataRequest Sent: " + dataItemResult.toString());
//                                    //mGoogleAppiClient.disconnect();
//                                    stopSelf();
//                                }
//                            });
                            //pendingResult.await();

                            byte[] rawData = configReturn.toByteArray();
                            Wearable.MessageApi.sendMessage(client, nodeId, WatchFaceUtil.PATH_WITH_FEATURE, rawData);
                        }

                    }
                    else
                        Log.d(TAG,"ERROR : config is not contains Key : WatchFaceUtil.KEY_TOMATO_EVENT_QUEUE");

                } else {
                    Log.d(TAG,"ERROR : dataItemResult.getStatus " + dataItemResult.getStatus().isSuccess() + "" + (dataItemResult.getDataItem() != null?"":"null"));

                }

            }

            if (nodes.size() == 0)
                Log.v(TAG, "Wear ERROR: no node to send....");

            client.disconnect();
        }
    }


    public static void uploadCalendarList(Context context)
    {
        //String nodeId;
        GoogleApiClient client;

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
        int res = context.checkCallingOrSelfPermission(permission);
        if (res == PackageManager.PERMISSION_GRANTED) {

            client = new GoogleApiClient.Builder(context)
                    //.addConnectionCallbacks(this)
                    //.addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            if (!client.isConnected()) {
                ConnectionResult connRes = client.blockingConnect(1000, TimeUnit.MILLISECONDS);
                if (!connRes.isSuccess())
                {
                    Log.e("uploadCalendarList","Can't connect to GoogleApiClient...");
                    return;
                }

            }
            NodeApi.GetConnectedNodesResult result =
                    Wearable.NodeApi.getConnectedNodes(client).await();

            List<Node> nodes = result.getNodes();
            //for (Node n : nodes)
            //    Log.d(TAG,"Node " + n.getId() + "  " + n.getDisplayName());
            if (nodes.size() > 0) {

                //nodeId = nodes.get(0).getId();

                Cursor calCursor =
                        context.getContentResolver().
                                query(CalendarContract.Calendars.CONTENT_URI,
                                        projection,
                                        CalendarContract.Calendars.VISIBLE + " = 1 " + "AND " +
                                                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " + CalendarContract.Calendars.CAL_ACCESS_OVERRIDE,
                                        null,
                                        CalendarContract.Calendars._ID + " ASC");
                if (calCursor.moveToFirst()) {
                    ArrayList<DataMap> calMapList = new ArrayList<DataMap>();
                    do {
                        long id = calCursor.getLong(0);
                        String displayName = calCursor.getString(1);
                        String accName = calCursor.getString(2);
                        //String accType = calCursor.getString(3);
                        String accLevel = calCursor.getString(4);
                        int color = calCursor.getInt(5);
                        Log.i("MainActivity", "id :" + id + "  name :" + displayName + "  accLevel:" + accLevel + "  color:" + Integer.toHexString(color));


                        DataMap map = new DataMap();
                        map.putLong(CalendarContract.Calendars._ID,id);
                        map.putString(CalendarContract.Calendars.NAME, displayName);
                        map.putString(CalendarContract.Calendars.ACCOUNT_NAME, accName);
                        map.putInt(CalendarContract.Calendars.CALENDAR_COLOR, color);

                        calMapList.add(map);

                    } while (calCursor.moveToNext());
                    DataMap calMap = new DataMap();
                    calMap.putDataMapArrayList(WatchFaceUtil.KEY_TOMATO_CALENDAR_LIST, calMapList);
                    byte[] rawData = calMap.toByteArray();
                    for (Node n : nodes)
                        Wearable.MessageApi.sendMessage(client, n.getId(), WatchFaceUtil.PATH_WITH_FEATURE, rawData);

                    Log.d("uploadCalendarList", "Sent watch face config message: " + WatchFaceUtil.KEY_TOMATO_CALENDAR_LIST);
                    for (DataMap map : calMapList)
                    {
                        Log.d("uploadCalendarList", "[" + map.toString() + "]");
                    }

                }
            }

        }
    }


    public static void uploadPhoneBattery(Context context)
    {

        //String nodeId;
        GoogleApiClient client;
        client = new GoogleApiClient.Builder(context)
                //.addConnectionCallbacks(this)
                //.addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        if (!client.isConnected()) {
            ConnectionResult connRes = client.blockingConnect(1000, TimeUnit.MILLISECONDS);
            if (!connRes.isSuccess())
            {
                Log.e("uploadPhoneBattery","Can't connect to GoogleApiClient...");
                return;
            }

        }
        NodeApi.GetConnectedNodesResult result =
                Wearable.NodeApi.getConnectedNodes(client).await();

        List<Node> nodes = result.getNodes();
        //for (Node n : nodes)
        //    Log.d(TAG,"Node " + n.getId() + "  " + n.getDisplayName());
        if (nodes.size() > 0) {

            //nodeId = nodes.get(0).getId();

            Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryStatus != null) {
                int batteryLevel = (int) ((float) batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) /
                        (float) batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) * 100.f);

                DataMap calMap = new DataMap();
                calMap.putInt(WatchFaceUtil.KEY_TOMATO_PHONE_BATTERY, batteryLevel);
                byte[] rawData = calMap.toByteArray();
                for (Node n : nodes)
                    Wearable.MessageApi.sendMessage(client, n.getId(), WatchFaceUtil.PATH_WITH_MESSAGE, rawData);

                Log.d("uploadCalendarList", "Sent watch face config message: " + WatchFaceUtil.KEY_TOMATO_PHONE_BATTERY + " Battery : " + batteryLevel);
            }

        }
    }
}
