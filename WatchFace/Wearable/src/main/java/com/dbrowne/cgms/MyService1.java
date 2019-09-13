package com.dbrowne.cgms;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
//import android.os.Vibrator;
import android.os.Vibrator;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;


//import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.example.android.wearable.watchface.R;
import com.example.android.wearable.watchface.provider.UpdateComplicationDataService;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.BleServices;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Uuids;

import static com.idevicesinc.sweetblue.utils.Uuids.GATTDisplayType.Hex;

/**
 * Main screen of the application. This is where all the magic happens!
 * Usually, to keep your code clean you'd want to move all the logic concerning bluetooth to a
 * separate controller, a {@link Service service}, a utility class or a combination of those.
 */
public class MyService1 extends Service {//implements LeScanCallback {

    Readings reading = new Readings();
    Calibrations calibration = new Calibrations();
    Alerts alerts = new Alerts();
    SharedPreferences mSharedPref;
    public int slope;
    public long intercept;

    public boolean newCal = false;
    public long calTime = 0;
    public int alertSleep = 0;
    public int lastGlucose = 0;
    public int noSignalCount =0;
    long lastTime=0;
    Vibrator vibrator;

    private static final String MY_DEVICE_NAME = "cgms";
    //private static final UUID MY_UUID = UUID.fromString("00002222-0000-1000-8000-00805f9b34fb");	// NOTE: Replace with your actual UUID.
    private static final byte[] MY_DATA = {(byte) 0x0F};		//  NOTE: Replace with your actual data, not 0xC0FFEE
    private final static int INTERVAL = 1000 * 60 * 2; //2 minutes

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onStart(Intent intent, int startid) {
       // Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreate() {
        Log.d("cgms:ble", "onCreate");

        mSharedPref = this.getApplicationContext().getSharedPreferences(
                getString(R.string.analog_complication_preference_file_key),
                Context.MODE_PRIVATE);

        reading = new Readings(mSharedPref);
        reading.sharedPref = mSharedPref;
        reading.retrieveReadings();

        calibration = new Calibrations(mSharedPref);
        calibration.sharedPref = mSharedPref;
        calibration.retrieveCal();

        Vibrator vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        alerts = new Alerts(vibrator);

        final BleManager mngr = BleManager.get(this);
        final BleServer server = mngr.getServer();

        BluetoothEnabler.start(this.getApplicationContext());

        // Set a listener so we know when the server has finished connecting.
        server.setListener_State(new BleServer.StateListener() {
            @Override
            public void onEvent(BleServer.StateListener.StateEvent e) {
                if (e.didEnter(BleServerState.CONNECTED)) {
                    Log.d("cgms:ble", "Server connected!");


                }
            }
        });


        // Kick things off...from here it's a flow of a bunch of async callbacks...obviously you may want to structure this differently for your actual app.
        //shouldn't be using currentTime, but it works ...

        server.addService(BleServices.currentTime(), new BleServer.ServiceAddListener() {
            @Override
            public void onEvent(final BleServer.ServiceAddListener.ServiceAddEvent e) {
                if (e.wasSuccess()) {
                    mngr.startScan(new BleManagerConfig.ScanFilter() {
                                       @Override
                                       public Please onEvent(final ScanEvent e) {
                                           return Please.acknowledgeIf(e.name_normalized().contains(MY_DEVICE_NAME)).thenStopScan();
                                       }
                                   },

                            new BleManager.DiscoveryListener() {
                                @Override
                                public void onEvent(DiscoveryEvent e) {
                                    if (e.was(LifeCycle.DISCOVERED)) {
                                        Log.d("cgms:ble", "Discovered");

                                        e.device().connect(new BleDevice.StateListener() {
                                            @Override
                                            public void onEvent(final StateEvent e) {
                                                if (e.didEnter(BleDeviceState.INITIALIZED)) {
                                                    Log.d("cgms:ble", "Initialized");
                                                    server.connect(e.device().getMacAddress());

                                                    final Handler handler = new Handler();
                                                    handler.postDelayed(new Runnable() {
                                                        public void run() {
                                                            Log.d("cgms:ble","Launching ble write");
                                                            e.device().write(Uuids.fromShort("2222"), MY_DATA, new BleDevice.ReadWriteListener()
                                                            {
                                                                @Override public void onEvent(ReadWriteEvent event)
                                                                {
                                                                    if( event.wasSuccess() )
                                                                    {
                                                                        Log.i("", "Write successful");
                                                                        try {
                                                                            Log.d("cgms:ble","MyService1 Length "+event.data().length);
                                                                            if (event.data().length > 4) {


                                                                                byte b1 = (event.data()[2]);
                                                                                byte b2 = (event.data()[3]);
                                                                                byte b3 = (event.data()[4]);
                                                                                byte b4 = (event.data()[5]);

                                                                                byte[] bytes = {b1, b2, b3, b4};

                                                                                long value = 0;
                                                                                for (int i = 0; i < bytes.length; i++) {
                                                                                    value = (value << 8) + (bytes[i] & 0xff);
                                                                                }
                                                                                Log.d("cgms:ble", "Value " + value);
                                                                                if (!Singleton.getInstance().getLastISIG().equals(Singleton.getInstance().getISIG())) {
                                                                                    Singleton.getInstance().setISIG(value);
                                                                                    Singleton.getInstance().setISIGTime(System.currentTimeMillis() / 1000 / 60);
                                                                                }
                                                                                try {
                                                                                    showGlucose();
                                                                                } catch (Exception err) {
                                                                                    Log.d("cgms:ble", "showGlucose exception " + err.toString());
                                                                                }
                                                                            }else{
                                                                               for (int i=0;i<event.data().length;i++){
                                                                                   byte a1 = (event.data()[i]);
                                                                                   String hexString = Integer.toHexString(a1);
                                                                                   Log.d("cgms:ble",hexString);
                                                                               }
                                                                            }
                                                                        } catch (Exception err) {
                                                                            Log.d("cgms:ble", "BLE Handled error " + err.toString());
                                                                        }                                                                 }
                                                                    else
                                                                    {
                                                                        Log.e("", event.status().toString()); // Logs the reason why it failed.
                                                                    }
                                                                }
                                                            });
                                                            handler.postDelayed(this, 60000);
                                                        }
                                                    }, 60000); //Every 60000 ms (minute)



                                                    e.device().enableNotify(Uuids.fromShort("2221"), new BleDevice.ReadWriteListener() {
                                                        @Override
                                                        public void onEvent(ReadWriteEvent e) {
                                                            if (e.wasSuccess() && !e.isNull()) {
                                                                try {
                                                                    Log.d("cgms:ble","MyService1 Length "+e.data().length);
                                                                    if (e.data().length > 4) {
                                                                        byte b1 = (e.data()[2]);
                                                                        byte b2 = (e.data()[3]);
                                                                        byte b3 = (e.data()[4]);
                                                                        byte b4 = (e.data()[5]);

                                                                        byte[] bytes = {b1, b2, b3, b4};

                                                                        long value = 0;
                                                                        for (int i = 0; i < bytes.length; i++) {
                                                                            value = (value << 8) + (bytes[i] & 0xff);
                                                                        }
                                                                        Log.d("cgms:ble", "Value " + value);
                                                                        Singleton.getInstance().setISIG(value);
                                                                        Singleton.getInstance().setISIGTime(System.currentTimeMillis() / 1000 / 60);
                                                                        showGlucose();
                                                                    }else{
                                                                        for (int i=0;i<e.data().length;i++){
                                                                            byte a1 = (e.data()[i]);
                                                                            String hexString = Integer.toHexString(a1);
                                                                            Log.d("cgms:ble",hexString);
                                                                        }
                                                                    }
                                                                } catch (Exception err) {
                                                                    Log.d("cgms:ble", "BLE Handled error " + err.toString());
                                                                }
                                                            }
                                                        }
                                                    });

                                                }
                                            }
                                        });
                                    }
                                }
                            });
                }
            }
        });


    }



    public void showGlucose() {
        if (Singleton.getInstance().getLastISIG()==null){
            Singleton.getInstance().setLastISIG(0L);
        }
        Log.d("cgms", "showGlucose ISIG:" + Singleton.getInstance().getLastISIG() +":"+ Singleton.getInstance().getISIG());
        //isig should always be different ...

        if (!Singleton.getInstance().getLastISIG().equals(Singleton.getInstance().getISIG()) ) {

            if ((Singleton.getInstance().getActionTime() != null) && (Singleton.getInstance().getActionTime() != 0)) {

                Log.d("cgms", "Got an action " + Singleton.getInstance().getActionTime());
                Log.d("cgms", "showGlucose ISIG:" + Singleton.getInstance().getLastISIG() +":"+ Singleton.getInstance().getISIG());
                if (Singleton.getInstance().getAction().equals("Reset")) {
                    //calibration.retrieveCal();
                    calibration.initCalibrations();
                    calibration.addCalibration(30000, 0);

                    SharedPreferences.Editor editor = mSharedPref.edit();
                    slope = 700;
                    intercept = 30000;
                    editor.putInt("slope", 700);
                    editor.putLong("intercept", 30000);
                    //editor.commit();
                    editor.apply();
                    calibration.persistCalibration();
                    Singleton.getInstance().setActionTime(0L);
                    Singleton.getInstance().setAction("none");
                    Singleton.getInstance().setActionInt(0);
                }

                if (Singleton.getInstance().getAction().equals("zzz")) {
                    //
                    //do something in alerts
                    alertSleep = 24;
                    Singleton.getInstance().setActionTime(0L);
                    Singleton.getInstance().setAction("none");
                    Singleton.getInstance().setActionInt(0);
                }

                //calibration
                //2-14 just represent items in the menu 80-200
                if ((Singleton.getInstance().getActionInt() >= 2) && (Singleton.getInstance().getActionInt() <= 14)) {
                    if (!newCal) {  //debounce, user can click multiple times
                        int newGlucose = Integer.valueOf(Singleton.getInstance().getAction());
                        Log.d("cgms", "Calibration point at:" + newGlucose + ":" + reading.readings_arr[0].isig);
                        calibration.addCalibration(reading.readings_arr[0].isig, newGlucose);
                        //calibration.persistCalibration();
                        newCal = true;
                        calTime = Long.valueOf(Singleton.getInstance().getActionTime());
                        calibration.calcSlopeandInt();

                        Singleton.getInstance().setActionTime(0L);
                        Singleton.getInstance().setAction("none");
                        Singleton.getInstance().setActionInt(0);
                    }
                }


            }

            if ((System.currentTimeMillis() / 1000 / 60) - (calTime ) > 20 && newCal) { //16 minutes missed readings, clear it
                newCal = false;
            }

            if ((System.currentTimeMillis() / 1000 / 60) - (calTime ) > 11 && newCal) { //11 minutes
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

           // Log.d("cgms", "showGlucose ISIG:" + Singleton.getInstance().getLastISIG() +":"+ Singleton.getInstance().getISIG());
            //if ( Singleton.getInstance().getLastISIG() != Singleton.getInstance().getISIG()) {
                if (alertSleep == 0) {
                    alerts.doAlerts((int) gluc, reading.getTimeToCriticalInt());
                } else {
                    alertSleep--;
                }
           // }
            lastGlucose = (int) gluc;


            ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setShortText(ComplicationText.plainText(glucoseString))
                    .build();

            Singleton.getInstance().setLastISIG(Singleton.getInstance().getISIG());
            //ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
            //ad        .setLongText(ComplicationText.plainText(glucoseString))
            //        .build();

            //onComplicationDataUpdate(0, complicationData);

        }
    }
}