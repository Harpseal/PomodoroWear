/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.harpseal.pomodorowear;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A {@link WearableListenerService} listening for {@link MainWatchFace} config messages
 * and updating the config {@link com.google.android.gms.wearable.DataItem} accordingly.
 */
public class MainWatchFaceConfigListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DigitalListenerService";

    private GoogleApiClient mGoogleApiClient;

    private Intent intent = new Intent(WatchFaceUtil.RECEIVER_ACTION_WATCH_FACE);
    
    @Override // WearableListenerService
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(WatchFaceUtil.PATH_WITH_MESSAGE)) {
            //intent.putExtra("Message", new String(messageEvent.getData()));
            intent.putExtra("MessageDataMap", messageEvent.getData());
            sendBroadcast(intent);
            Log.d(TAG,"onMessageReceived + sendBroadcast");
            return;
        }
        else if (!messageEvent.getPath().equals(WatchFaceUtil.PATH_WITH_FEATURE)) {
            Log.d(TAG,"onMessageReceived + PATH_WITH_FEATURE");
            return;
        }

        byte[] rawData = messageEvent.getData();
        // It's allowed that the message carries only some of the keys used in the config DataItem
        // and skips the ones that we don't want to change.
        DataMap configKeysToOverwrite = DataMap.fromByteArray(rawData);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Received watch face config message: " + configKeysToOverwrite);
        }

        for (String configKey : configKeysToOverwrite.keySet()) {
            if (configKey.equals(WatchFaceUtil.KEY_TOMATO_PHONE_BATTERY)) {
                //int newValue  = configKeysToOverwrite.getInt(configKey);
                intent.putExtra("MessageDataMap", messageEvent.getData());
                sendBroadcast(intent);
                Log.d(TAG,"onMessageReceived + overwriteKeysInConfigDataMap force MessageDataMap");
                return;
                //return;
            }
        }

        Log.d(TAG,"onMessageReceived + overwriteKeysInConfigDataMap :" +configKeysToOverwrite.toString());
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).addApi(Wearable.API).build();
        }
        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult =
                    mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Failed to connect to GoogleApiClient.");
                return;
            }
        }
        WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        //if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "WearableListenerService onConnected: " + connectionHint);
        //}
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        //if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "WearableListenerService onConnectionSuspended: " + cause);
        //}
    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        //if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "WearableListenerService onConnectionFailed: " + result);
        //}
    }

}
