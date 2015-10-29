package io.harpseal.pomodorowear;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class MessageListenerService extends WearableListenerService {
    private final String TAG = "MessageListenerService";
    private int mNumberOfMessage = 0;
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        //showToast(messageEvent.getPath());
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
                }
            }
            return;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, "" + mNumberOfMessage + " : " + message, Toast.LENGTH_LONG).show();
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
            if (nodes.size() > 0) {

                nodeId = nodes.get(0).getId();

                Log.d(TAG,"Create event for Node " + nodeId + "  " + nodes.get(0).getDisplayName());

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
                        for (int e=0;eventQueue!=null && e<eventQueue.size();e++)
                        {
                            DataMap event = eventQueue.get(e);

                            long dtstart = event.getLong(CalendarContract.Events.DTSTART, 0);
                            long dtend = event.getLong(CalendarContract.Events.DTEND,0);
                            long calID = event.getLong(CalendarContract.Events.CALENDAR_ID, 0);
                            String strTitle = event.getString(CalendarContract.Events.TITLE, "");
                            String strDescription = event.getString(CalendarContract.Events.DESCRIPTION,"");
                            String strTimeZone = event.getString(CalendarContract.Events.EVENT_TIMEZONE,"");


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
                            byte[] rawData = config.toByteArray();
                            Wearable.MessageApi.sendMessage(client, nodeId, WatchFaceUtil.PATH_WITH_FEATURE, rawData);
                        }

                    }

                } else {
                    Log.d(TAG,"ERROR : dataItemResult.getStatus " + dataItemResult.getStatus().isSuccess() + "" + (dataItemResult.getDataItem() != null?"":"null"));

                }

            }
            else
                Log.v(TAG, "Wear ERROR: no node to send....");

            client.disconnect();
        }
    }
}
