package com.example.mladen.wear.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.mladen.wear.util.DigitalWatchFaceUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
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
import com.mladenbabic.utils.Constants;

import java.util.Date;


public class ConfigDataListenerService extends WearableListenerService implements
        com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks,
        com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "CDListenerService";

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, " onPeerConnected ");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

//        if (PreferencesUtil.getPrefs(this, Constants.KEY_STOP_WATCH_FACE, false)) {
//            sendMessageToDevice(Constants.PATH_STOP_WATCH_FACE);
//            Log.d(TAG, "Watch face already destroyed.");
//            return;
//        }
//
//        String path = messageEvent.getPath();
//        Log.d(TAG, "Message received on path " + path);
//        if (mGoogleApiClient == null) {
//            mGoogleApiClient = (new com.google.android.gms.common.api.GoogleApiClient.Builder(this)).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(Wearable.API).build();
//        }
//        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.blockingConnect(30L, TimeUnit.SECONDS).isSuccess()) {
//            Log.e(TAG, "Failed connect to Google Api client.");
//            return;
//        }
//        if (Constants.PATH_CONFIG_DATA.equals(path)) {
//            DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
//            Log.d(TAG, "Message has data " + dataMap);
//            DigitalWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, dataMap, path);
//            return;
//        } else {
//            super.onMessageReceived(messageEvent);
//        }
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
//        try {
//            for (DataEvent dataEvent : dataEvents) {
//                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
//                    continue;
//                }
//
//                DataItem dataItem = dataEvent.getDataItem();
//
//                Log.d(TAG, "path: " + dataItem.getUri().getPath());
//
//                if (!dataItem.getUri().getPath().equals(Constants.PATH_BATTERY_DATA)) {
//                    continue;
//                }
//
//                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
//                DataMap config = dataMapItem.getDataMap();
//                if (Log.isLoggable(TAG, Log.DEBUG)) {
//                    Log.d(TAG, "Config DataItem updated:" + config);
//                }
//                DigitalWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, config, Constants.PATH_CONFIG_DATA);
//            }
//        } finally {
//            dataEvents.close();
//        }
    }


    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, (new StringBuilder()).append("onConnected: ").append(bundle).toString());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, (new StringBuilder()).append("onConnectionSuspended: ").append(i).toString());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, (new StringBuilder()).append("onConnectionFailed: ").append(connectionResult).toString());
    }


    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
