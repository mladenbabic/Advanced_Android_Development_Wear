package com.example.android.sunshine.app.wearable;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.mladenbabic.utils.Constants;


public class DataListenerService extends WearableListenerService {

    private static final String TAG = "DataListenerService";

    @Override
    public void onCreate() {
        Log.d(TAG, " onCreate ");
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, " onDestroy ");
        super.onDestroy();
    }

    @Override
    public void onPeerConnected(Node node) {
        super.onPeerConnected(node);
        Log.d(TAG, " onPeerConnected " + node);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, " onPeerDisconnected " + peer);
        if (!peer.getId().equals("cloud")) {
            Log.d(TAG, "onPeerDisconnected: ");
        }
        super.onPeerDisconnected(peer);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, " onMessageReceived - received msg from wearable " + messageEvent.getPath());
        if (messageEvent.getPath().equals(Constants.PATH_START_WATCH_FACE)) {
            Intent startWeatherService = new Intent(this, WearableWeatherService.class);
            startService(startWeatherService);
        }
    }

}
