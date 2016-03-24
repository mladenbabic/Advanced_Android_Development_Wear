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

package com.example.mladen.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.mladen.wear.util.BitmapUtil;
import com.example.mladen.wear.util.ColorPreset;
import com.example.mladen.wear.util.DateUtil;
import com.example.mladen.wear.util.DigitalWatchFaceUtil;
import com.example.mladen.wear.util.PreferencesUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.mladenbabic.utils.Constants;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int TOUCH_CIRCLE_RADIUS = 40;
    private static final float BORDER_WIDTH_PX = 3.0f;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    private static final String TAG = "SunshineWatchFace";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }


        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;

        Calendar calendar;

        Paint mBackgroundPaint;
        Paint mBottomBackgroundPaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mAmPmPaint;
        Paint mDatePaint;
        Paint mWeatherMaxTempPaint;
        Paint mWeatherMinTempPaint;
        Paint mTouchTapPaint;
        Paint mAmbientPeekCardBorderPaint;
        Paint mBatteryPaint;

        Rect mCardBounds = new Rect();
        Bitmap mWeatherIconBitmap;
        Bitmap mBatteryWatchBitmap;
        boolean mAmbient;
        private boolean showTouchCircle;
        private float touchX;
        private float touchY;
        private int mWidth = 400;
        private int mHeight = 400;
        private float mScale;
        private int mCenterX;
        private int mCenterY;
        private boolean mIsRound;
        private int mChinSize;

        boolean mLowBitAmbient;

        private float mScaledHourXOffset;
        private float mScaledHourYOffset;
        private float mScaledDateYOffset;

        private float mScaledHourFormatXOffset;
        private float mScaledHourFormatYOffset;

        private float mScaledWeatherIconXOffset;
        private float mScaledWeatherIconYOffset;

        private float mScaledWeatherTempXOffset;
        private float mScaledWeatherTempYOffset;

        private float mScaledBatteryYOffset;

        private int mMinTemp = 0;
        private int mMaxTemp = 0;
        private int mWeatherIconId = 0;
        private String mWeatherUnit = Constants.DEFAULT_UNIT;
        int mBattery;

        SimpleDateFormat mDayOfWeekFormat;
        java.text.DateFormat mDateFormat;
        String dateValue;


        Typeface typeface2 = Typeface.createFromAsset(getAssets(), "fonts/digital2.ttf");

        SparseArray<Bitmap> weatherIcons = new SparseArray();
        SparseArray<ColorPreset> colorPresets = new SparseArray();

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
            }
        };

        final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                mBattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            }
        };


        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFace.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        Resources resources = SunshineWatchFace.this.getResources();


        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();
            mChinSize = insets.getSystemWindowInsetBottom();
        }


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            Log.d(TAG, "onCreate: ");

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            calendar = Calendar.getInstance();
            initColorPresets();
            initFormats();

            mAmbientPeekCardBorderPaint = new Paint();
            mAmbientPeekCardBorderPaint.setColor(Color.BLACK);
            mAmbientPeekCardBorderPaint.setStrokeWidth(BORDER_WIDTH_PX);

            mBatteryWatchBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.battery_watch);

            mTouchTapPaint = new Paint();
            mTouchTapPaint.setAntiAlias(true);
            mTouchTapPaint.setColor(0x80FEFEFE);

            ColorPreset colorPreset = getColorPreset();
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(colorPreset.getBgColor());

            mBottomBackgroundPaint = new Paint();
            mBottomBackgroundPaint.setColor(Color.WHITE);

            mDatePaint = createTextPaint(colorPreset.getTextColor(), typeface2);
            mDatePaint.setTextSize(20);

            mHourPaint = createTextPaint(colorPreset.getTextColor(), typeface2);
            mHourPaint.setTextSize(80);

            mMinutePaint = createTextPaint(colorPreset.getTextColor(), typeface2);
            mMinutePaint.setTextSize(80);

            mSecondPaint = createTextPaint(colorPreset.getTextColor(), typeface2);
            mSecondPaint.setTextSize(50);

            mAmPmPaint = createTextPaint(colorPreset.getTextColor(), typeface2);
            mAmPmPaint.setTextSize(30);

            mWeatherMaxTempPaint = createTextPaint(colorPreset.getBgColor(), typeface2);
            mWeatherMaxTempPaint.setTextSize(40);

            mWeatherMinTempPaint = createTextPaint(colorPreset.getBgColor(), typeface2);
            mWeatherMinTempPaint.setTextSize(25);

            mBatteryPaint = createTextPaint(colorPreset.getTextColor());
            setColorToBitmap(mBatteryPaint, colorPreset.getTextColor());

        }

        private Paint createTextPaint(int color, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        private Paint createTextPaint(int color) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setAntiAlias(true);
            return paint;
        }


        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            Log.d(TAG, "onSurfaceChanged: ");

            initImages();

            mWidth = width;
            mHeight = height;

            mCenterX = mWidth / 2;
            mCenterY = mHeight / 2;

            mScale = ((float) mWidth) / 320;

            mScaledHourXOffset = resources.getDimension(R.dimen.hour_x_offset) * mScale;
            mScaledHourYOffset = resources.getDimension(R.dimen.hour_y_offset) * mScale;
            mScaledDateYOffset = resources.getDimension(R.dimen.date_y_offset) * mScale;
            mScaledHourFormatXOffset = resources.getDimension(R.dimen.hour_format_x_offset) * mScale;
            mScaledHourFormatYOffset = resources.getDimension(R.dimen.hour_format_y_offset) * mScale;
            mScaledWeatherIconXOffset = resources.getDimension(R.dimen.weather_icon_x_offset) * mScale;
            mScaledWeatherIconYOffset = resources.getDimension(R.dimen.weather_icon_y_offset) * mScale;
            mScaledWeatherTempXOffset = resources.getDimension(R.dimen.weather_temp_x_offset) * mScale;
            mScaledWeatherTempYOffset = resources.getDimension(R.dimen.weather_temp_y_offset) * mScale;
            mScaledBatteryYOffset = resources.getDimension(R.dimen.battery_y_offset) * mScale;

            BitmapUtil.scaleBitmaps(weatherIcons, mScale);

            setTextSizes(mScale);

        }

        private void setTextSizes(float mScale) {

            
        }

        private void initImages() {

            Bitmap[] weatherIconBitmaps = BitmapUtil.loadBitmaps(R.array.weatherImagesIds, resources);
            for (int i = 0; i < weatherIconBitmaps.length; i++) {
                weatherIcons.put(i, weatherIconBitmaps[i]);
            }
        }

        private void initColorPresets() {
            colorPresets.put(0, new ColorPreset(Color.parseColor("#03A9F4"), Color.parseColor("#FFFFFF")));
            colorPresets.put(0, new ColorPreset(Color.parseColor("#000000"), Color.parseColor("#FFFFFF")));
            colorPresets.put(1, new ColorPreset(Color.parseColor("#4DD2FF"), Color.parseColor("#FFFFFF")));
            colorPresets.put(2, new ColorPreset(Color.parseColor("#ACBA96"), Color.parseColor("#FFFFFF")));
            colorPresets.put(3, new ColorPreset(Color.parseColor("#99F2DB"), Color.parseColor("#000000")));
            colorPresets.put(4, new ColorPreset(Color.parseColor("#DB8180"), Color.parseColor("#FFFFFF")));
            colorPresets.put(5, new ColorPreset(Color.parseColor("#3F51B5"), Color.parseColor("#FFFFFF")));
            colorPresets.put(6, new ColorPreset(Color.parseColor("#5D4037"), Color.parseColor("#FFFFFF")));
            colorPresets.put(7, new ColorPreset(Color.parseColor("#607D8B"), Color.parseColor("#FFFFFF")));
            colorPresets.put(7, new ColorPreset(Color.parseColor("#607D8B"), Color.parseColor("#FFFFFF")));
            colorPresets.put(7, new ColorPreset(Color.parseColor("#FFC107"), Color.parseColor("#212121")));
        }

        private void initFormats() {
            mDayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            mDayOfWeekFormat.setCalendar(calendar);
            mDateFormat = DateFormat.getDateFormat(SunshineWatchFace.this);
            mDateFormat.setCalendar(calendar);
        }


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
            BitmapUtil.recycleBitmaps(weatherIcons);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    boolean antiAlias = !inAmbientMode;
                    mDatePaint.setAntiAlias(antiAlias);
                    mHourPaint.setAntiAlias(antiAlias);
                    mMinutePaint.setAntiAlias(antiAlias);
                    mSecondPaint.setAntiAlias(antiAlias);
                    mAmPmPaint.setAntiAlias(antiAlias);
                    mBatteryPaint.setAntiAlias(antiAlias);
                    mBackgroundPaint.setAntiAlias(antiAlias);
                    mWeatherMaxTempPaint.setAntiAlias(antiAlias);
                    mWeatherMinTempPaint.setAntiAlias(antiAlias);
                }
                setColors();
                invalidate();
            }
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {

            hideTapHighlight();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    startTapHighlight(x, y);
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    hideTapHighlight();
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    hideTapHighlight();
                    setNextColorPresets();
                    break;
            }
            invalidate();
        }

        @Override
        public void onPeekCardPositionUpdate(Rect rect) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPeekCardPositionUpdate: " + rect);
            }
            super.onPeekCardPositionUpdate(rect);
            if (!rect.equals(mCardBounds)) {
                mCardBounds.set(rect);
                invalidate();
            }
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            calendar.setTimeInMillis(System.currentTimeMillis());

            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
                canvas.drawRect(mCardBounds, mAmbientPeekCardBorderPaint);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
                canvas.drawRect(0, 200 * mScale, canvas.getWidth(), canvas.getHeight(), mBottomBackgroundPaint);
                drawWeather(canvas);
                drawBatteryWatch(canvas);
            }

            drawDigitalClock(canvas, calendar, bounds);
            drawDate(canvas, calendar, bounds);

            if (showTouchCircle) {
                canvas.drawCircle(touchX, touchY, TOUCH_CIRCLE_RADIUS, mTouchTapPaint);
            }

        }


        private void startTapHighlight(int x, int y) {
            touchX = x;
            touchY = y;
            showTouchCircle = true;
            invalidate();
            updateTimer();
        }

        private void hideTapHighlight() {
            showTouchCircle = false;
            invalidate();
            updateTimer();
        }

        private void drawDate(Canvas canvas, Calendar calendar, Rect bounds) {
            String dayOfWeekString = mDayOfWeekFormat.format(calendar.getTime());
            String dateString = mDateFormat.format(calendar.getTime());
            dateValue = dayOfWeekString + ", " + dateString;
            float halfDateLenght = mDatePaint.measureText(dateValue) / 2;
            canvas.drawText(dateValue, mCenterX - halfDateLenght, mScaledDateYOffset, mDatePaint);
        }


        private void drawDigitalClock(Canvas canvas, Calendar calendar, Rect bounds) {

            int hour = DateUtil.getFormattedCurrentHours(SunshineWatchFace.this, calendar);
            int minutes = DateUtil.getCurrentMinutes(calendar);
            int seconds = DateUtil.getCurrentSeconds(calendar);

            float startXOffset = mScaledHourXOffset;
            String hourString = String.valueOf(hour);
            String minuteString = Constants.COLON_STRING + DateUtil.formatTwoDigitNumber(minutes);
            String secondsString = DateUtil.formatTwoDigitNumber(seconds);

            canvas.drawText(hourString, startXOffset, mScaledHourYOffset, mHourPaint);
            startXOffset += mHourPaint.measureText(hourString);
            canvas.drawText(minuteString, startXOffset, mScaledHourYOffset, mMinutePaint);
            startXOffset += mMinutePaint.measureText(minuteString);

            String hourFormat = DateUtil.getHourFormat(SunshineWatchFace.this, calendar);
            canvas.drawText(hourFormat, mScaledHourFormatXOffset, mScaledHourFormatYOffset, mAmPmPaint);

            if (!isInAmbientMode()) {
                canvas.drawText(Constants.COLON_STRING + secondsString, startXOffset, mScaledHourYOffset, mSecondPaint);
            }
        }

        private void drawBatteryWatch(Canvas canvas) {
            canvas.drawBitmap(mBatteryWatchBitmap, mCenterX - 25 * mScale, mScaledBatteryYOffset - 20 * mScale, mBatteryPaint);
            canvas.drawText(String.valueOf(mBattery) + "%", mCenterX, mScaledBatteryYOffset, mDatePaint);
        }

        private void drawWeather(Canvas canvas) {
            mWeatherIconBitmap = weatherIcons.get(mWeatherIconId);
            canvas.drawBitmap(mWeatherIconBitmap, mScaledWeatherIconXOffset, mScaledWeatherIconYOffset, null);

            String maxTempString = String.valueOf(mMaxTemp) + mWeatherUnit;
            String minTempString = String.valueOf(mMinTemp) + mWeatherUnit;
            float startXOffset = mScaledWeatherTempXOffset + mWeatherMinTempPaint.measureText(maxTempString);
            canvas.drawText(maxTempString, mScaledWeatherTempXOffset, mScaledWeatherTempYOffset, mWeatherMaxTempPaint);
            startXOffset += mWeatherMinTempPaint.measureText(minTempString);
            canvas.drawText(maxTempString, startXOffset, mScaledWeatherTempYOffset, mWeatherMinTempPaint);

        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                mGoogleApiClient.connect();
                // Update time zone in case it changed while we weren't visible.
                calendar.setTimeZone(TimeZone.getDefault());
                calendar.setTimeInMillis(System.currentTimeMillis());
            } else {
                Wearable.DataApi.removeListener(mGoogleApiClient, Engine.this);
                mGoogleApiClient.disconnect();
                unregisterReceiver();
            }

            showTouchCircle = false;
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);

            IntentFilter batfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            SunshineWatchFace.this.registerReceiver(mBatInfoReceiver, batfilter);

        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            SunshineWatchFace.this.unregisterReceiver(mBatInfoReceiver);
        }


        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode() && !showTouchCircle;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + connectionHint);
            }
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            updateConfigDataItemAndUiOnStartup(Constants.PATH_CONFIG_DATA);
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        }


        @Override // DataApi.DataListener
        public void onDataChanged(DataEventBuffer dataEvents) {
            try {
                for (DataEvent dataEvent : dataEvents) {
                    if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                        continue;
                    }

                    DataItem dataItem = dataEvent.getDataItem();
                    Log.d(TAG, "onDataChanged: Path" + dataItem.getUri().getPath());
                    if (!dataItem.getUri().getPath().equals(
                            Constants.PATH_CONFIG_DATA)) {
                        continue;
                    }

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Config DataItem updated:" + config);
                    }
                    updateUiForConfigDataMap(config);
                }
            } finally {
                dataEvents.close();
            }

        }

        @Override  // GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(ConnectionResult result) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + result);
            }
        }

        private void updateUiForConfigDataMap(final DataMap config) {

            for (String configKey : config.keySet()) {

                if (!config.containsKey(configKey)) {
                    continue;
                }

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Found watch face config key: " + configKey);
                }

                if (Constants.KEY_WEATHER_ICON.equals(configKey)) {
                    int weatherIconId = config.getInt(configKey);
                    PreferencesUtil.savePrefs(SunshineWatchFace.this, Constants.KEY_WEATHER_ICON, weatherIconId);
                }

                if (Constants.KEY_WEATHER_ID.equals(configKey)) {
                    int weatherId = config.getInt(configKey);
                    PreferencesUtil.savePrefs(SunshineWatchFace.this, Constants.KEY_WEATHER_ID, weatherId);
                }

                if (Constants.KEY_WEATHER_TEMP_MAX.equals(configKey)) {
                    int weatherTempMax = config.getInt(configKey);
                    PreferencesUtil.savePrefs(SunshineWatchFace.this, Constants.KEY_WEATHER_TEMP_MAX, weatherTempMax);
                }

                if (Constants.KEY_WEATHER_TEMP_MIN.equals(configKey)) {
                    int weatherTempMin = config.getInt(configKey);
                    PreferencesUtil.savePrefs(SunshineWatchFace.this, Constants.KEY_WEATHER_TEMP_MIN, weatherTempMin);
                }

                if (Constants.KEY_WEATHER_UNIT.equals(configKey)) {
                    String weatherUnit = config.getString(configKey);
                    PreferencesUtil.savePrefs(SunshineWatchFace.this, Constants.KEY_WEATHER_UNIT, weatherUnit);
                }
            }
        }


        private void updateConfigDataItemAndUiOnStartup(final String path) {

            DigitalWatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                    new DigitalWatchFaceUtil.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // If the DataItem hasn't been created yet or some keys are missing,
                            // use the default values.
                            setDefaultValuesForMissingConfigKeys(startupConfig);
                            DigitalWatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig, path);

                            updateUiForConfigDataMap(startupConfig);
                        }
                    }
                    , path);
        }

        private void setDefaultValuesForMissingConfigKeys(DataMap config) {
            addIntKeyIfMissing(config, Constants.KEY_WEATHER_ICON, 0);
            addIntKeyIfMissing(config, Constants.KEY_WEATHER_ID, 0);
            addIntKeyIfMissing(config, Constants.KEY_WEATHER_TEMP_MIN, 0);
            addIntKeyIfMissing(config, Constants.KEY_WEATHER_TEMP_MAX, 0);
            addStringKeyIfMissing(config, Constants.KEY_WEATHER_UNIT, "C");
        }


        private void addIntKeyIfMissing(DataMap config, String key, int color) {
            if (!config.containsKey(key)) {
                config.putInt(key, color);
            }
        }

        private void addBooleanKeyIfMissing(DataMap config, String key, boolean value) {
            if (!config.containsKey(key)) {
                config.putBoolean(key, value);
            }
        }

        private void addStringKeyIfMissing(DataMap config, String key, String value) {
            if (!config.containsKey(key)) {
                config.putString(key, value);
            }
        }

        public void setColors() {
            if (!isInAmbientMode()) {
                ColorPreset colorPreset = getColorPreset();
                mBackgroundPaint.setColor(colorPreset.getBgColor());
                mHourPaint.setColor(colorPreset.getTextColor());
                mMinutePaint.setColor(colorPreset.getTextColor());
                mSecondPaint.setColor(colorPreset.getTextColor());
            } else {
                mBackgroundPaint.setColor(Color.BLACK);
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
            }
        }

        public void setNextColorPresets() {
            int colorPresetPosition = PreferencesUtil.getPrefs(SunshineWatchFace.this, Constants.KEY_PRESET_COLOR_POSITION, 0);
            if (colorPresetPosition >= colorPresets.size() - 1) {
                colorPresetPosition = 0;
            } else {
                colorPresetPosition++;
            }

            ColorPreset colorPreset = colorPresets.get(colorPresetPosition);
            mBackgroundPaint.setColor(colorPreset.getBgColor());
            mHourPaint.setColor(colorPreset.getTextColor());
            mMinutePaint.setColor(colorPreset.getTextColor());
            mSecondPaint.setColor(colorPreset.getTextColor());
            mDatePaint.setColor(colorPreset.getTextColor());
            mAmPmPaint.setColor(colorPreset.getTextColor());

            mWeatherMaxTempPaint.setColor(colorPreset.getBgColor());
            mWeatherMinTempPaint.setColor(colorPreset.getBgColor());

            setColorToBitmap(mBatteryPaint, colorPreset.getTextColor());
            PreferencesUtil.savePrefs(SunshineWatchFace.this, Constants.KEY_PRESET_COLOR_POSITION, colorPresetPosition);
        }

        public ColorPreset getColorPreset() {
            int colorPresetPosition = PreferencesUtil.getPrefs(SunshineWatchFace.this, Constants.KEY_PRESET_COLOR_POSITION, 0);
            return colorPresets.get(colorPresetPosition);
        }

        private void setColorToBitmap(Paint colorPaint, int color) {
            int mul = 0x00000000; //remove BLACK component
            LightingColorFilter lightingColorFilter = new LightingColorFilter(mul, color);
            colorPaint.setColorFilter(lightingColorFilter);
        }

    }


}
