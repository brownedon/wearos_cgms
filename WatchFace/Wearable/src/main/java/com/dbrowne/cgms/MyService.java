package com.dbrowne.cgms;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import java.util.UUID;

/**
 * Main screen of the application. This is where all the magic happens!
 * Usually, to keep your code clean you'd want to move all the logic concerning bluetooth to a
 * separate controller, a {@link Service service}, a utility class or a combination of those.
 */
public class MyService extends Service {//implements LeScanCallback {

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    /**
     * The GATT standard defines this UUID as the identifier of the update notification descriptor,
     * i.e. the descriptor of a characteristic that defines if you will receive updates on the
     * value of the characteristic.
     */
    private static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    // TODO Change this value to the UUID of the service you want to communicate with
    private static final UUID SERVICE_UUID = UUID.fromString("00002220-0000-1000-8000-00805f9b34fb");

    // TODO change this value to the UUID of the characteristic you want to receive updates from
    private static final UUID CHARACTERISTIC_B_READ = UUID.fromString("00002221-0000-1000-8000-00805f9b34fb");

    private Handler mHandler;
    /**
     * Request code used when starting the bluetooth settings Activity
     */
    //private static final int REQUEST_ENABLE_BT = RESULT_FIRST_USER;

    private static final String TAG = "CGMS:BLE";

    // Bluetooth objects
    private BluetoothAdapter mAdapter;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mWriteCharacteristic;
private boolean deviceFound=false;
    private boolean mIsScanning = false;
    private boolean mSearchIsUUID;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 20000;

    @Override
        public void onStart(Intent intent, int startid) {
            Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCreate() {
            Log.d(TAG, "onCreate");
        //super.onCreate(savedInstanceState);
      //  setContentView(R.layout.activity_main);
        //super.onStart(intent, startId);
       // mHandler = new Handler();
        // Initialize the adapter
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = btManager.getAdapter();

        //mAdapter.disable();
        //while(!mAdapter.isEnabled()) {
        //    mAdapter.enable();
        //}

       // mAdapter.disable();
       // while(!mAdapter.isEnabled()) {
       //     mAdapter.enable();
       // }
        if (!isBluetoothEnabled()) {
            // If bluetooth is not enabled, we ask the user to do so
            //Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            mAdapter.enable();
            // If the permission android.permission.BLUETOOTH_ADMIN is included in the manifest,
            // then we could also enable bluetooth without requiring a user action by doing:
            // mAdapter.enable();
            // ...But that is not nice towards the user.
        }
        if(isBluetoothEnabled()) {
            startScan();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,  "onDestroy");
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mGatt != null) {
            mGatt.disconnect();
            try {
                mGatt.close();
            } catch (Exception e) {
                Log.d(TAG,  "mgatt close exception");
            }
            mGatt = null;
        }
        mAdapter=null;

        //mAdapter.disable();
        //while(!mAdapter.isEnabled()) {
        //    mAdapter.enable();
       // }
    }


    /**
     * Checks if bluetooth is enabled in the system settings
     *
     * @return {@code true} if bluetooth is available and enabled, {@code false} otherwise
     */
    public boolean isBluetoothEnabled() {
        return mAdapter != null && mAdapter.isEnabled();
    }

    /**
     * Start scanning for nearby devices. If a UUID is entered in the EditText, it will scan
     * for that specific service. If a MAC address or nothing is entered it will scan for all
     * devices.
     *
     * @see #onLeScan(BluetoothDevice, int, byte[])
     */
    private void startScan() {
        Log.d(TAG, "startscan enter");



        //deviceFound=false;
        //if(mGatt != null) mGatt.close();
       // mSearchIsUUID = false;
         //  Log.d(TAG, "startscan loop");
          // mIsScanning = true;
          // mAdapter.startLeScan(this);




            // Stops scanning after a pre-defined scan period.
          //  mHandler.postDelayed(new Runnable() {
          //      @Override
          //      public void run() {
          //          mIsScanning = false;
          //          mAdapter.stopLeScan(mLeScanCallback);
          //      }
          //  }, SCAN_PERIOD);

            mIsScanning = true;
              mAdapter.startLeScan(mLeScanCallback);

    }



    /**
     * Closes the GATT connection (if any) and updates the UI to show
     * the 'disconnected' state. Can be called from background threads.
     */
    private void cleanUp() {
        Log.d(TAG, "cleanUp");
        if (mGatt != null) {
            mGatt.disconnect();
            try {
                mGatt.close();
            } catch (Exception e) {
                Log.d(TAG,  "mgatt close exception");
            }
            mGatt = null;
        }

        startScan();
    }


    /**
     * Called for every device that is found during the scan
     *
     * @param device     The device that is found
     * @param rssi       The received signal strength indication
     * @param scanRecord Extra data concerning the scanned device
     */
    //this is the call back
   // @Override
  //  public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
  //      Log.d(TAG,  "onLeScan");
      //  String search = mEtUUID.getText().toString();

      //  if (device.getAddress().equals("ED:C7:CE:47:C2:C4")) {  //test device
    //    if (device.getAddress().equals("D9:5F:44:7E:22:F4")) {
            // Does the device address match our search term?
      //      deviceFound=true;
        //    connectDevice(device);
        //}
   //}


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d(TAG,"onLeScan");
                    if (device.getAddress().equals("D9:5F:44:7E:22:F4")) {
                        // Does the device address match our search term?
                        deviceFound=true;
                        connectDevice(device);
                    }
                }
            };



    /**
     * Starts connecting to a device. The device is obtained a scan.
     *
     * @param device The device to connect with
     */
    private void connectDevice(final BluetoothDevice device) {
        Log.d(TAG, "Connecting");
        // We've found the device we want, so stop scanning.
        // This is important, because scanning is the most battery intensive part of the process.
        if(mLeScanCallback !=null) {
            mAdapter.stopLeScan(mLeScanCallback);
        }
        mIsScanning = false;

        // Connect to the device
        mGatt = device.connectGatt(this, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.d(TAG, "Connection state change:"+status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED:
                            Log.d(TAG, "Connected");
                            Singleton.getInstance().setBluetooth("UP");
                            // showStatus(R.string.status_discovering);
                            // Start discovering services.
                            // Once done, the onServicesDiscovered callback will be called.
                            if (!gatt.discoverServices()) {
                                Log.d(TAG, "Errored at gatt.discoverservices");
                                // If it fails, clean up
                                deviceFound=false;
                                cleanUp();
                            }
                            break;
                        case BluetoothProfile.STATE_DISCONNECTED:
                            Log.d(TAG, "Disconnected");
                            //cleanUp();
                           // mGatt=null;
                            //gatt = null;
                            startScan();
                            break;
                    }
                } else {
                    // Connection failed, clean up
                    Log.d(TAG, "Connection failed cleanup");
                    deviceFound=false;
                    cleanUp();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.d(TAG, "onServiceDiscovered");
                // Find the service we want to use
                BluetoothGattService service = gatt.getService(SERVICE_UUID);

                // Find the characteristic that we want to read from
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_B_READ);
                // Enable notifications of that characteristic
                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    Log.d(TAG,  "Unable to get notifications for characteristic " + CHARACTERISTIC_B_READ);
                    return;
                }
                // Enable notifications even further by enabling it in the characteristic's descriptors
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);

                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(descriptor)) {
                    Log.d(TAG, "Unable to write to descriptor of characteristic " + characteristic.getUuid());
                    cleanUp();
                }
            }

            /**
             * Once notifications are enabled, this method is called every time the value of the characteristic changes.
             *
             * @param gatt The GATT connection
             * @param characteristic The characteristic that has changed.
             */
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "Characteristic changed: " + characteristic.getUuid() + " = " + characteristic.getValue()[0]);

                long value = 0;
                try {
                    Log.d("cgms:ble","In Loop");
                    byte b1 = (characteristic.getValue()[2]);
                    byte b2 = (characteristic.getValue()[3]);
                    byte b3 = (characteristic.getValue()[4]);
                    byte b4 = (characteristic.getValue()[5]);

                    byte[] bytes = {b1, b2, b3, b4};

                    Log.d("cgms:ble","In Loop1");
                    for (int i = 0; i < bytes.length; i++) {
                        value = (value << 8) + (bytes[i] & 0xff);
                    }
                    Log.d(TAG,  "Value " + value);
                   // Singleton.getInstance().setISIG(value);
                    Log.d("cgms:ble","In Loop2");
                    Singleton.getInstance().setISIG(value);
                    Singleton.getInstance().setISIGTime(System.currentTimeMillis() / 1000/60);
                    //showGlucose(value);
                } catch (Exception err) {
                    Log.d(TAG, "BLE Handled error " + err.toString());
                }
            }
        });

    }

    public void close() {
        Log.d(TAG, "Ble Closing");
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }
}
