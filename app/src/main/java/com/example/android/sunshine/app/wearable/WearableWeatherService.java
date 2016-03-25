package com.example.android.sunshine.app.wearable;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.util.DataManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.mladenbabic.utils.Constants;

import java.math.BigDecimal;
import java.util.Date;

public class WearableWeatherService extends Service implements
        GoogleApiClient.ConnectionCallbacks, ResultCallback<DataApi.DataItemResult> {


    private static final String TAG = "WWeatherService";
    private GoogleApiClient mGoogleApiClient;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;


    public WearableWeatherService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mGoogleApiClient = (new GoogleApiClient.Builder(WearableWeatherService.this))
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected ");
        sendDataToWear();
    }

    private void sendDataToWear() {

        Log.d(TAG, "sendDataToWear: ");

        String locationQuery = Utility.getPreferredLocation(this);

        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

        Cursor cursor = getBaseContext().getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);
        DataMap dataMap = new DataMap();

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);

            high  = Utility.getFormattedTemperature(this, high);
            low = Utility.getFormattedTemperature(this, low);

            Log.d(TAG, "sendDataToWear: high temp: " + high);
            Log.d(TAG, "sendDataToWear: low temp: " + low);

            BigDecimal highBD = new BigDecimal(high);
            highBD = highBD.setScale(2, BigDecimal.ROUND_UP);
            BigDecimal lowBD = new BigDecimal(low);
            lowBD = lowBD.setScale(2, BigDecimal.ROUND_UP);

            dataMap.putLong("time", new Date().getTime());
            dataMap.putInt(Constants.KEY_WEATHER_TEMP_MAX, highBD.intValue());
            dataMap.putInt(Constants.KEY_WEATHER_TEMP_MIN, lowBD.intValue());
            dataMap.putInt(Constants.KEY_WEATHER_ID, weatherId);
            dataMap.putString(Constants.KEY_WEATHER_UNIT, Utility.isMetric(this) ? "C" : "F");
        }

        DataManager.getInstance().syncDataMap(mGoogleApiClient, dataMap, Constants.PATH_WEATHER_DATA, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, " onConnectionSuspended ");
    }

    private void disconnectClient() {
        if (mGoogleApiClient != null && (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting())) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        disconnectClient();
        super.onDestroy();
    }

    @Override
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess()) {
            WearableWeatherService.this.stopSelf();
        }
    }
}
