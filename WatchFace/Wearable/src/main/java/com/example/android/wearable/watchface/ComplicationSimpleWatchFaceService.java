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

package com.example.android.wearable.watchface;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v7.graphics.Palette;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.Formatter;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.dbrowne.cgms.Alerts;
import com.dbrowne.cgms.MyService;
import com.dbrowne.cgms.MyService1;
import com.dbrowne.cgms.Readings;
import com.dbrowne.cgms.Calibrations;
import com.dbrowne.cgms.Singleton;

//import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.example.android.wearable.watchface.provider.UpdateComplicationDataService;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.BleServices;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Uuids;

import static com.idevicesinc.sweetblue.utils.Uuids.GATTDisplayType.Hex;


/**
 * Demonstrates two simple complications in a watch face.
 */
public class ComplicationSimpleWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "SimpleComplicationWF";
    private static final String MY_DEVICE_NAME = "test";
    Intent myIntent;

    // Unique IDs for each complication.
    private static final int LEFT_DIAL_COMPLICATION = 0;
    private static final int RIGHT_DIAL_COMPLICATION = 1;
    public int alertSleep = 0;
    public int lastGlucose = 0;
    public int noSignalCount = 0;
    long lastTime = 0;
    long lastBounce = System.currentTimeMillis();

    // Left and right complication IDs as array for Complication API.
    public static final int[] COMPLICATION_IDS = {LEFT_DIAL_COMPLICATION, RIGHT_DIAL_COMPLICATION, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};

    // Left and right dial supported types.
    public static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SHORT_TEXT}
    };

    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        //
        //
        public int slope;
        public long intercept;

        public boolean newCal = false;
        public long calTime = 0;

        //Readings reading = new Readings();
        //Calibrations calibration = new Calibrations();
        Alerts alerts = new Alerts();

        SharedPreferences mSharedPref;
        //
        //
        private static final int MSG_UPDATE_TIME = 0;

        private static final float COMPLICATION_TEXT_SIZE = 32f;
        private static final int COMPLICATION_TAP_BUFFER = 40;

        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 6;

        private Calendar mCalendar;
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;

        private int mWidth;
        private int mHeight;
        private float mCenterX;
        private float mCenterY;

        private float mSecondHandLength;
        private float mMinuteHandLength;
        private float mHourHandLength;

        // Colors for all hands (hour, minute, seconds, ticks) based on photo loaded.
        private int mWatchHandColor;
        private int mWatchHandHighlightColor;
        private int mWatchHandShadowColor;

        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mTickAndCirclePaint;

        private Paint mBackgroundPaint;
        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;

        // Variables for painting Complications
        private Paint mComplicationPaint;

        /* To properly place each complication, we need their x and y coordinates. While the width
         * may change from moment to moment based on the time, the height will not change, so we
         * store it as a local variable and only calculate it only when the surface changes
         * (onSurfaceChanged()).
         */
        private int mComplicationsY;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        private Rect mPeekCardBounds = new Rect();

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        // Handler to update the time once a second in interactive mode.
        private final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "updating time");
                }
                invalidate();
                if (shouldTimerBeRunning()) {
                    long timeMs = System.currentTimeMillis();
                    long delayMs = INTERACTIVE_UPDATE_RATE_MS
                            - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                    mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                }

            }
        };

        public String byteToHex(final byte[] hash) {
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String result = formatter.toString();
            formatter.close();
            return result;
        }

        private boolean isMyServiceRunning(Class<?> serviceClass) {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d("cgms", "onCreate");
            super.onCreate(holder);

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyWakelockTag");
            wakeLock.acquire();

            mCalendar = Calendar.getInstance();

            setWatchFaceStyle(new WatchFaceStyle.Builder(ComplicationSimpleWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAcceptsTapEvents(true)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            //
            //
            //
            Context context = getApplicationContext();
            mSharedPref = context.getSharedPreferences(
                    getString(R.string.analog_complication_preference_file_key),
                    Context.MODE_PRIVATE);

            //reading = new Readings(mSharedPref);
            //reading.sharedPref = mSharedPref;
            //reading.retrieveReadings();

            //calibration = new Calibrations(mSharedPref);
            //calibration.sharedPref = mSharedPref;
            //calibration.retrieveCal();

            Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            //
            //
            //if(! isMyServiceRunning(MyService.class)) {
            //    startService(new Intent(ComplicationSimpleWatchFaceService.this, MyService.class));
            // }

            // if(! isMyServiceRunning(MyService.class)) {
            //     startService(new Intent(ComplicationSimpleWatchFaceService.this, BluetoothLeService.class));
            // }


            //   if(! isMyServiceRunning(MyService1.class)) {
            //       Log.d(TAG,"starting service");
            //       myIntent=new Intent(ComplicationSimpleWatchFaceService.this, MyService1.class);
            //       startService(myIntent);
            //   }


            alerts = new Alerts(vibrator);

            initializeBackground();
            initializeComplication();
            initializeWatchFace();
        }


        public void showGlucose() {
            Log.d("cgms", "showGlucose ISIG:" + Singleton.getInstance().getLastISIG() + ":" + Singleton.getInstance().getISIG());
            //isig should always be different ...

            /*if (Singleton.getInstance().getLastISIG() != Singleton.getInstance().getISIG() &&(Singleton.getInstance().getISIG()!=0)) {
                Singleton.getInstance().setLastISIG(Singleton.getInstance().getISIG());
                if ((Singleton.getInstance().getActionTime() != null) && (Singleton.getInstance().getActionTime() != 0)) {

                    Log.d("cgms", "Got an action " + Singleton.getInstance().getActionTime());

                    if (Singleton.getInstance().getAction().equals("Reset")) {
                        //calibration.retrieveCal();
                        calibration.initCalibrations();
                        calibration.addCalibration(30000, 0);

                        SharedPreferences.Editor editor = mSharedPref.edit();
                        slope = 700;
                        intercept = 30000;
                        editor.putInt("slope", 700);
                        editor.putLong("intercept", 30000);
                        editor.commit();
                        calibration.persistCalibration();
                        Singleton.getInstance().setActionTime(0l);
                        Singleton.getInstance().setAction("none");
                        Singleton.getInstance().setActionInt(0);
                    }

                    if (Singleton.getInstance().getAction().equals("zzz")) {
                        //
                        //do something in alerts
                        alertSleep = 24;
                        Singleton.getInstance().setActionTime(0l);
                        Singleton.getInstance().setAction("none");
                        Singleton.getInstance().setActionInt(0);
                    }

                    //calibration
                    //2-14 just represent items in the menu 80-200
                    if ((Singleton.getInstance().getActionInt() >= 2) && (Singleton.getInstance().getActionInt() <= 14)) {
                        int newGlucose = Integer.valueOf(Singleton.getInstance().getAction());
                        Log.d("cgms", "Calibration point at:" + newGlucose + ":" + reading.readings_arr[0].isig);
                        calibration.addCalibration(reading.readings_arr[0].isig, newGlucose);
                        //calibration.persistCalibration();
                        newCal = true;
                        calTime = Long.valueOf(Singleton.getInstance().getActionTime());
                        calibration.calcSlopeandInt();

                        Singleton.getInstance().setActionTime(0l);
                        Singleton.getInstance().setAction("none");
                        Singleton.getInstance().setActionInt(0);
                    }


                }

                if ((System.currentTimeMillis() / 1000 / 60) - (calTime / 60) > 20 && newCal) { //16 minutes missed readings, clear it
                    newCal = false;
                }

                if ((System.currentTimeMillis() / 1000 / 60) - (calTime / 60) > 11 && newCal) { //11 minutes
                    newCal = false;
                    calibration.updateIsig(Singleton.getInstance().getISIG());
                    calibration.calcSlopeandInt();
                }

                slope = mSharedPref.getInt("slope", 700);
                intercept = mSharedPref.getLong("intercept", 30000);
                Log.d("cgms", "showGlucose " + slope + ":" + intercept);
                long gluc = (Singleton.getInstance().getISIG() - intercept) / slope;
                String glucoseString = String.valueOf(gluc);
                reading.addReading((int) gluc, Singleton.getInstance().getISIG());

                Log.d("cgms", "Slope: " + slope + " intercept:" + intercept);

                Double glucoseSlope = reading.getSlopeGlucose();
                Log.d("cgms", "glucose slope " + glucoseSlope);
                String timeToCritical = reading.getTimeToCritical();
                Log.d("cgms", "TimetoCritical is:" + timeToCritical);

                glucoseString = "  "+glucoseString + System.getProperty("line.separator") + timeToCritical;
                if (Math.abs(glucoseSlope) > 0) {
                    String sign = "-";
                    if (glucoseSlope > 0) {
                        sign = "+";
                    }
                    if (lastGlucose != 0 && Math.abs(lastGlucose - (int) gluc) > 25) {
                        glucoseString = glucoseString + System.getProperty("line.separator") + "?";
                    } else {
                        if (Math.abs(glucoseSlope) > 0.1) {
                            glucoseString = glucoseString + System.getProperty("line.separator") + sign + Double.toString(Math.abs(glucoseSlope)).substring(0, 3);
                        }
                    }
                }
                glucoseString = glucoseString + System.getProperty("line.separator");
                Log.d("cgms", glucoseString);
                Singleton.getInstance().setGlucose(glucoseString);
                Log.d("cgms", "Alerts:" + gluc + ":" + reading.getTimeToCriticalInt());
                if (alertSleep == 0) {
                    alerts.doAlerts((int) gluc, reading.getTimeToCriticalInt());
                } else {
                    alertSleep--;
                }

                lastGlucose = (int) gluc;


                ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText(glucoseString))
                        .build();

                //ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                //ad        .setLongText(ComplicationText.plainText(glucoseString))
                //        .build();

                onComplicationDataUpdate(0, complicationData);

            }*/
            String glucoseString = Singleton.getInstance().getGlucose();
            ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setShortText(ComplicationText.plainText(glucoseString))
                    .build();

            //ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
            //ad        .setLongText(ComplicationText.plainText(glucoseString))
            //        .build();

            onComplicationDataUpdate(0, complicationData);
        }

        private void initializeBackground() {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);
            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        }

        private void initializeComplication() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "initializeComplications()");
            }
            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            mComplicationPaint = new Paint();
            mComplicationPaint.setColor(Color.WHITE);
            mComplicationPaint.setTextSize(COMPLICATION_TEXT_SIZE);
            mComplicationPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            mComplicationPaint.setAntiAlias(true);

            setActiveComplications(COMPLICATION_IDS);
        }

        private void initializeWatchFace() {
            /* Set defaults for colors */
            mWatchHandColor = Color.WHITE;
            mWatchHandHighlightColor = Color.RED;
            mWatchHandShadowColor = Color.BLACK;

            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            // Asynchronous call extract colors from background image to improve watch face style.
            Palette.from(mBackgroundBitmap).generate(
                    new Palette.PaletteAsyncListener() {
                        public void onGenerated(Palette palette) {
                            /*
                             * Sometimes, palette is unable to generate a color palette
                             * so we need to check that we have one.
                             */
                            if (palette != null) {
                                Log.d("onGenerated", palette.toString());
                                mWatchHandColor = palette.getVibrantColor(Color.WHITE);
                                mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
                                updateWatchHandStyle();
                            }
                        }
                    });
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        /*
         * Called when there is updated data for a complication id.
         */
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "ComplicationSimpleWatchFaceService onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);
            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()" + x + ":" + y + ":" + eventTime);
            switch (tapType) {
                case TAP_TYPE_TAP:
                    int tappedComplicationId = getTappedComplicationId(x, y);
                    if (tappedComplicationId != -1) {
                        onComplicationTap(tappedComplicationId);
                    }
                    break;
            }
        }

        /*
         * Determines if tap inside a complication area or returns -1.
         */
        private int getTappedComplicationId(int x, int y) {
            Log.d(TAG, "getTappedComplicationId");
            ComplicationData complicationData;
            long currentTimeMillis = System.currentTimeMillis();

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationData = mActiveComplicationDataSparseArray.get(COMPLICATION_IDS[i]);

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))
                        && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                        && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                    Rect complicationBoundingRect = new Rect(0, 0, 0, 0);

                    switch (COMPLICATION_IDS[i]) {
                        case LEFT_DIAL_COMPLICATION:
                            complicationBoundingRect.set(
                                    0,                                          // left
                                    mComplicationsY - COMPLICATION_TAP_BUFFER,  // top
                                    (mWidth / 2),                               // right
                                    ((int) COMPLICATION_TEXT_SIZE               // bottom
                                            + mComplicationsY
                                            + COMPLICATION_TAP_BUFFER));
                            break;

                        case RIGHT_DIAL_COMPLICATION:
                            complicationBoundingRect.set(
                                    (mWidth / 2),                               // left
                                    mComplicationsY - COMPLICATION_TAP_BUFFER,  // top
                                    mWidth,                                     // right
                                    ((int) COMPLICATION_TEXT_SIZE               // bottom
                                            + mComplicationsY
                                            + COMPLICATION_TAP_BUFFER));
                            break;
                    }

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return COMPLICATION_IDS[i];
                        }
                    } else {
                        Log.e(TAG, "Not a recognized complication id.");
                    }
                }
            }
            return -1;
        }

        /*
         * Fires PendingIntent associated with complication (if it has one).
         */
        private void onComplicationTap(int complicationId) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onComplicationTap()");
            }

            ComplicationData complicationData =
                    mActiveComplicationDataSparseArray.get(complicationId);

            if (complicationData != null) {

                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "On complication tap action error " + e);
                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                    // Watch face does not have permission to receive complication data, so launch
                    // permission request.
                    ComponentName componentName = new ComponentName(
                            getApplicationContext(),
                            ComplicationSimpleWatchFaceService.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }

            } else {
                Log.d(TAG, "No PendingIntent for complication " + complicationId + ".");
            }
        }

        @Override
        public void onTimeTick() {
            Log.d("cgms", "onTimeTick");
            super.onTimeTick();

            //adb  Log.d(TAG,"Elapsed "+(System.currentTimeMillis() / 1000 / 60) - (Singleton.getInstance().getISIGTime() / 60));
            //if ((System.currentTimeMillis() / 1000 / 60) - (Singleton.getInstance().getISIGTime() ) > 12) {
            if (!isMyServiceRunning(MyService1.class)) {
                Log.d(TAG, "starting service");
                myIntent = new Intent(ComplicationSimpleWatchFaceService.this, MyService1.class);
                startService(myIntent);
            }

            if ((System.currentTimeMillis() / 1000 / 60) - (lastBounce / 1000 / 60) > 10) {  //10 used to work
                lastBounce = System.currentTimeMillis();
                if (myIntent != null) {
                    Log.d(TAG, "Shutting it down");
                    stopService(myIntent);
                }
            }

            showGlucose();
            if ((System.currentTimeMillis() / 1000 / 60) - (Singleton.getInstance().getISIGTime()) > 15) {
                if (noSignalCount == 0) {
                    alerts.doNoSignal();
                    noSignalCount = 10;
                    //if (myIntent != null) {
                    //    stopService(myIntent);
                    //}

                    if (!isMyServiceRunning(MyService1.class)) {
                        //     BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        //     mBluetoothAdapter.disable();
                        //     while(!mBluetoothAdapter.isEnabled()) {
                        //        mBluetoothAdapter.enable();
                        //    }
                        Log.d(TAG, "Restart service");
                        myIntent = new Intent(ComplicationSimpleWatchFaceService.this, MyService1.class);
                        startService(myIntent);
                    }
                }
                noSignalCount--;
            }


            lastTime = System.currentTimeMillis();
            invalidate();

        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            mAmbient = inAmbientMode;

            updateWatchHandStyle();

            // Updates complication style
            mComplicationPaint.setAntiAlias(!inAmbientMode);

            // Check and trigger whether or not timer should be running (only in active mode).
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
                mTickAndCirclePaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
                mTickAndCirclePaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondPaint.clearShadowLayer();
                mTickAndCirclePaint.clearShadowLayer();

            } else {
                mHourPaint.setColor(mWatchHandColor);
                mMinutePaint.setColor(mWatchHandColor);
                mSecondPaint.setColor(mWatchHandHighlightColor);
                mTickAndCirclePaint.setColor(mWatchHandColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mTickAndCirclePaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            // Used for complications
            mWidth = width;
            mHeight = height;

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;

            /*
             * Since the height of the complications text does not change, we only have to
             * recalculate when the surface changes.
             */
            mComplicationsY = (int) ((mHeight / 2) + (mComplicationPaint.getTextSize() / 2));

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (float) (mCenterX * 0.875);
            mMinuteHandLength = (float) (mCenterX * 0.75);
            mHourHandLength = (float) (mCenterX * 0.5);


            /* Scale loaded background image (more efficient) if surface dimensions change. */
            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!mBurnInProtection && !mLowBitAmbient) {
                initGrayBackgroundBitmap();
            }
        }

        private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                    mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onDraw");
            }
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawComplications(canvas, now);
            drawWatchFace(canvas);


        }

        private void drawBackground(Canvas canvas) {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            Log.d(TAG, "drawComplications");
            ComplicationData complicationData;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {

                complicationData = mActiveComplicationDataSparseArray.get(COMPLICATION_IDS[i]);

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))) {

                    // Both Short Text and No Permission Types can be rendered with the same code.
                    // No Permission will display "--" with an Intent to launch a permission prompt.
                    // If you want to support more types, just add a "else if" below with your
                    // rendering code inside.

                    if (complicationData.getType() == ComplicationData.TYPE_SHORT_TEXT
                            || complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                        ComplicationText mainText = complicationData.getShortText();
                        ComplicationText subText = complicationData.getShortTitle();

                        CharSequence complicationMessage =
                                mainText.getText(getApplicationContext(), currentTimeMillis);

                        /* In most cases you would want the subText (Title) under the
                         * mainText (Text), but to keep it simple for the code lab, we are
                         * concatenating them all on one line.
                         */
                        if (subText != null) {
                            complicationMessage = TextUtils.concat(
                                    complicationMessage,
                                    " ",
                                    subText.getText(getApplicationContext(), currentTimeMillis));
                        }

                        //Log.d(TAG, "Com id: " + COMPLICATION_IDS[i] + "\t" + complicationMessage);
                        double textWidth =
                                mComplicationPaint.measureText(
                                        complicationMessage,
                                        0,
                                        complicationMessage.length());

                        int complicationsX;

                        if (COMPLICATION_IDS[i] == LEFT_DIAL_COMPLICATION) {
                            complicationsX = (int) ((mWidth / 2) - textWidth) / 2;
                        } else {
                            // RIGHT_DIAL_COMPLICATION calculations
                            int offset = (int) ((mWidth / 2) - textWidth) / 2;
                            complicationsX = (mWidth / 2) + offset;
                        }

                        canvas.drawText(
                                complicationMessage,
                                0,
                                complicationMessage.length(),
                                complicationsX,
                                mComplicationsY,
                                mComplicationPaint);
                    }

                    if (complicationData.getType() == ComplicationData.TYPE_LONG_TEXT
                            || complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                        ComplicationText mainText = complicationData.getShortText();
                        ComplicationText subText = complicationData.getShortTitle();

                        CharSequence complicationMessage =
                                mainText.getText(getApplicationContext(), currentTimeMillis);

                        /* In most cases you would want the subText (Title) under the
                         * mainText (Text), but to keep it simple for the code lab, we are
                         * concatenating them all on one line.
                         */
                        if (subText != null) {
                            complicationMessage = TextUtils.concat(
                                    complicationMessage,
                                    " ",
                                    subText.getText(getApplicationContext(), currentTimeMillis));
                        }

                        //Log.d(TAG, "Com id: " + COMPLICATION_IDS[i] + "\t" + complicationMessage);
                        double textWidth =
                                mComplicationPaint.measureText(
                                        complicationMessage,
                                        0,
                                        complicationMessage.length());

                        int complicationsX;

                        if (COMPLICATION_IDS[i] == LEFT_DIAL_COMPLICATION) {
                            complicationsX = (int) ((mWidth / 2) - textWidth) / 2;
                        } else {
                            // RIGHT_DIAL_COMPLICATION calculations
                            int offset = (int) ((mWidth / 2) - textWidth) / 2;
                            complicationsX = (mWidth / 2) + offset;
                        }

                        canvas.drawText(
                                complicationMessage,
                                0,
                                complicationMessage.length(),
                                complicationsX,
                                mComplicationsY,
                                mComplicationPaint);
                    }

                }
            }
        }

        private void drawWatchFace(Canvas canvas) {
            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);

            }
            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mTickAndCirclePaint);

            /* Restore the canvas' original orientation. */
            canvas.restore();

            /* Draw rectangle behind peek card in ambient mode to improve readability. */
            if (mAmbient) {
                canvas.drawRect(mPeekCardBounds, mBackgroundPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onPeekCardPositionUpdate(Rect rect) {
            super.onPeekCardPositionUpdate(rect);
            mPeekCardBounds.set(rect);
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ComplicationSimpleWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            ComplicationSimpleWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }
    }


}