package uzmany.bmonitor;

import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

        import android.app.Activity;
        import android.app.ProgressDialog;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothGatt;
        import android.bluetooth.BluetoothGattCallback;
        import android.bluetooth.BluetoothGattCharacteristic;
        import android.bluetooth.BluetoothGattDescriptor;
        import android.bluetooth.BluetoothManager;
        import android.bluetooth.BluetoothProfile;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Message;
        import android.util.Log;
        import android.util.SparseArray;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.view.Window;
        import android.widget.TextView;
        import android.widget.Toast;


        import java.util.UUID;

/**
 * Created by Dave Smith
 * Double Encore, Inc.
 * MainActivity
 */
public class MainActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "BluetoothGattActivity";

    private static final String DEVICE_NAME = "CC2650 SensorTag";


    /* ACC Service */

    private static final UUID MAG_SERVICE =     UUID.fromString("f000aa30-0451-4000-b000-000000000000");
    private static final UUID MAG_DATA_CHAR =   UUID.fromString("f000aa31-0451-4000-b000-000000000000");
    private static final UUID MAG_CONFIG_CHAR = UUID.fromString("f000aa32-0451-4000-b000-000000000000");

    private static final UUID GYR_SERVICE =     UUID.fromString("f000aa50-0451-4000-b000-000000000000");
    private static final UUID GYR_DATA_CHAR =   UUID.fromString("f000aa51-0451-4000-b000-000000000000");
    private static final UUID GYR_CONFIG_CHAR = UUID.fromString("f000aa52-0451-4000-b000-000000000000");

    private static final UUID ACC_SERVICE =     UUID.fromString("f000aa10-0451-4000-b000-000000000000");
    private static final UUID ACC_DATA_CHAR =   UUID.fromString("f000aa11-0451-4000-b000-000000000000");
    private static final UUID ACC_CONFIG_CHAR = UUID.fromString("f000aa12-0451-4000-b000-000000000000");

    private static final UUID MOV_SERVICE =     UUID.fromString("f000aa80-0451-4000-b000-000000000000");
    private static final UUID MOV_DATA_CHAR =   UUID.fromString("f000aa81-0451-4000-b000-000000000000");
    private static final UUID MOV_CONFIG_CHAR = UUID.fromString("f000aa82-0451-4000-b000-000000000000");




/*
    UUID_MAG_SERV = fromString("f000aa30-0451-4000-b000-000000000000"),
    UUID_MAG_DATA = fromString("f000aa31-0451-4000-b000-000000000000"),
    UUID_MAG_CONF = fromString("f000aa32-0451-4000-b000-000000000000"), // 0: disable, 1: enable
    UUID_MAG_PERI = fromString("f000aa33-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_GYR_SERV = fromString("f000aa50-0451-4000-b000-000000000000"),
    UUID_GYR_DATA = fromString("f000aa51-0451-4000-b000-000000000000"),
    UUID_GYR_CONF = fromString("f000aa52-0451-4000-b000-000000000000"), // 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z
    UUID_GYR_PERI = fromString("f000aa53-0451-4000-b000-000000000000"), // Period in tens of milliseconds



    UUID_ACC_SERV = fromString("f000aa10-0451-4000-b000-000000000000"),
    UUID_ACC_DATA = fromString("f000aa11-0451-4000-b000-000000000000"),
    UUID_ACC_CONF = fromString("f000aa12-0451-4000-b000-000000000000"), // 0: disable, 1: enable
    UUID_ACC_PERI = fromString("f000aa13-0451-4000-b000-000000000000"), // Period in tens of milliseconds
*/

    /* LUX Service */
    private static final UUID LUX_SERVICE = UUID.fromString("f000aa70-0451-4000-b000-000000000000");
    private static final UUID LUX_DATA_CHAR = UUID.fromString("f000aa71-0451-4000-b000-000000000000");
    private static final UUID LUX_CONFIG_CHAR = UUID.fromString("f000aa72-0451-4000-b000-000000000000");
    //private static final UUID LUX_PER_CHAR = UUID.fromString("f000aa73-0451-4000-b000-000000000000");



    /* Humidity Service */
    private static final UUID HUMIDITY_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_DATA_CHAR = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_CONFIG_CHAR = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
    /* Barometric Pressure Service */
    private static final UUID PRESSURE_SERVICE = UUID.fromString("f000aa40-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_DATA_CHAR = UUID.fromString("f000aa41-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_CONFIG_CHAR = UUID.fromString("f000aa42-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_CAL_CHAR = UUID.fromString("f000aa44-0451-4000-b000-000000000000");
    //ACTUALLY AA43 NOT AA44
    /* Client Configuration Descriptor */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevices;

    private BluetoothGatt mConnectedGatt;

    private TextView mTemperature, mHumidity, mPressure, mLux, mMov,mMov2,mMov3;

    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
        setContentView(R.layout.activity_main);
        setProgressBarIndeterminate(true);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
         * We are going to display the results in some text fields
         */
        mMov = (TextView) findViewById(R.id.text_mov);
        mMov2 = (TextView) findViewById(R.id.text_mov2);
        mMov3 = (TextView) findViewById(R.id.text_mov3);
        mTemperature = (TextView) findViewById(R.id.text_temperature);
        mHumidity = (TextView) findViewById(R.id.text_humidity);
        mPressure = (TextView) findViewById(R.id.text_pressure);
        mLux = (TextView) findViewById(R.id.text_lux);

        /*
         * Bluetooth in Android 4.3 is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        mDevices = new SparseArray<BluetoothDevice>();

        /*
         * A progress dialog will be needed while the connection process is
         * taking place
         */
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        clearDisplayValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Make sure dialog is hidden
        mProgress.dismiss();
        //Cancel any scans in progress
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        mBluetoothAdapter.stopLeScan(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Disconnect from any active tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add the "scan" option to the menu
        getMenuInflater().inflate(R.menu.main, menu);
        //Add any device elements we've discovered to the overflow menu
        for (int i=0; i < mDevices.size(); i++) {
            BluetoothDevice device = mDevices.valueAt(i);
            menu.add(0, mDevices.keyAt(i), 0, device.getName());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                mDevices.clear();
                startScan();
                return true;
            default:
                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevices.get(item.getItemId());
                Log.i(TAG, "Connecting to "+device.getName());
                /*
                 * Make a connection with the device using the special LE-specific
                 * connectGatt() method, passing in a callback for GATT events
                 */
                mConnectedGatt = device.connectGatt(this, false, mGattCallback);
                //Display progress UI
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to "+device.getName()+"..."));
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearDisplayValues() {
        mTemperature.setText("---");
        mHumidity.setText("---");
        mPressure.setText("---");
        mLux.setText("---");
        mMov.setText("---");
        mMov2.setText("---");
        mMov3.setText("---");
    }


    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };
    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

    private void startScan() {
        mBluetoothAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);

        mHandler.postDelayed(mStopRunnable, 2500);
    }

    private void stopScan() {
        mBluetoothAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
    }

    /* BluetoothAdapter.LeScanCallback */

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New LE Device: " + device.getName() + " @ " + rssi);
        /*
         * We are looking for SensorTag devices only, so validate the name
         * that each device reports before adding it to our collection
         */
        if (DEVICE_NAME.equals(device.getName())) {
            mDevices.put(device.hashCode(), device);
            //Update the overflow menu
            invalidateOptionsMenu();
        }
    }

    /*
     * In this callback, we've created a bit of a state machine to enforce that only
     * one characteristic be read or written at a time until all of our sensors
     * are enabled and we are registered to get notifications.
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /* State Machine Tracking */
        private int mState = 0;

        private void reset() { mState = 0; }

        private void advance() { mState++; }

        /*
         * Send an enable command to each sensor by writing a configuration
         * characteristic.  This is specific to the SensorTag to keep power
         * low by disabling sensors you aren't using.
         */
        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                /*case 2:
                    Log.d(TAG, "Enabling pressure cal");
                    characteristic = gatt.getService(PRESSURE_SERVICE)
                            .getCharacteristic(PRESSURE_CAL_CHAR);
                    characteristic.setValue(new byte[] {0x02});
                    break;*/
                case 0:
                    Log.d(TAG, "Enabling pressure");
                    characteristic = gatt.getService(PRESSURE_SERVICE)
                            .getCharacteristic(PRESSURE_CONFIG_CHAR);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                case 1:
                    Log.d(TAG, "Enabling humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_CONFIG_CHAR);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                case 2:
                    Log.d(TAG, "Enabling lux");
                    characteristic = gatt.getService(LUX_SERVICE)
                            .getCharacteristic(LUX_CONFIG_CHAR);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                case 3:
                    Log.d(TAG, "Enabling mov");
                    characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_CONFIG_CHAR);
                    characteristic.setValue(new byte[] {0x7f, 0x00});
                    //characteristic.setValue(new byte[] {0x00, 0x7f});
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            gatt.writeCharacteristic(characteristic);
        }

        /*
         * Read the data characteristic's value for each sensor explicitly
         */
        private void readNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                /*case 2:
                    Log.d(TAG, "Reading pressure cal");
                    characteristic = gatt.getService(PRESSURE_SERVICE)
                            .getCharacteristic(PRESSURE_CAL_CHAR);
                    break;*/
                case 0:
                    Log.d(TAG, "Reading pressure");
                    characteristic = gatt.getService(PRESSURE_SERVICE)
                            .getCharacteristic(PRESSURE_DATA_CHAR);
                    break;
                case 1:
                    Log.d(TAG, "Reading humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_DATA_CHAR);
                    break;
                case 2:
                    Log.d(TAG, "Reading lux");
                    characteristic = gatt.getService(LUX_SERVICE)
                            .getCharacteristic(LUX_DATA_CHAR);
                    break;
                case 3:
                    Log.d(TAG, "Reading mov");
                    characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_DATA_CHAR);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            gatt.readCharacteristic(characteristic);
        }

        /*
         * Enable notification of changes on the data characteristic for each sensor
         * by writing the ENABLE_NOTIFICATION_VALUE flag to that characteristic's
         * configuration descriptor.
         */
        private void setNotifyNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                /*case 2:
                    Log.d(TAG, "Set notify pressure cal");
                    characteristic = gatt.getService(PRESSURE_SERVICE)
                            .getCharacteristic(PRESSURE_CAL_CHAR);
                    break;*/
                case 0:
                    Log.d(TAG, "Set notify pressure");
                    characteristic = gatt.getService(PRESSURE_SERVICE)
                            .getCharacteristic(PRESSURE_DATA_CHAR);
                    break;
                case 1:
                    Log.d(TAG, "Set notify humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_DATA_CHAR);
                    break;
                case 2:
                    Log.d(TAG, "Set notify lux");
                    characteristic = gatt.getService(LUX_SERVICE)
                            .getCharacteristic(LUX_DATA_CHAR);
                    break;
                case 3:
                    Log.d(TAG, "Set notify mov");
                    characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_DATA_CHAR);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            //Enable local notifications
            gatt.setCharacteristicNotification(characteristic, true);
            //Enabled remote notifications
            BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: "+status+" -> "+connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 * Once successfully connected, we must next discover all the services on the
                 * device before we can read and write their characteristics.
                 */
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering Services..."));
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered: "+status);
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
            /*
             * With services discovered, we are going to reset our state machine and start
             * working through the sensors we need to enable
             */
            reset();
            enableNextSensor(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //For each read, pass the data up to the UI thread to update the display
            Log.d(TAG, "READUUIDDDDDDD: " +characteristic.getUuid());
            if (MOV_DATA_CHAR.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_MOV, characteristic));
            }
            if (LUX_DATA_CHAR.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_LUX, characteristic));
            }
            if (HUMIDITY_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HUMIDITY, characteristic));
            }
            if (PRESSURE_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE, characteristic));
            }
            if (PRESSURE_CAL_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE_CAL, characteristic));
            }

            //After reading the initial value, next we enable notifications
            setNotifyNextSensor(gatt);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //After writing the enable flag, next we read the initial value
            readNextSensor(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*
             * After notifications are enabled, all updates from the device on characteristic
             * value changes will be posted here.  Similar to read, we hand these up to the
             * UI thread to update the display.
             */
            Log.d(TAG, "CHANGEEEEUUIDDDDDDD: " +characteristic.getUuid());
            if (MOV_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_MOV, characteristic));
            }
            if (LUX_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_LUX, characteristic));
            }
            if (HUMIDITY_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HUMIDITY, characteristic));
            }
            if (PRESSURE_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE, characteristic));
            }
            if (PRESSURE_CAL_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE_CAL, characteristic));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Once notifications are enabled, we move to the next sensor and start over with enable
            advance();
            enableNextSensor(gatt);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: "+rssi);
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };

    /*
     * We have a Handler to process event results on the main thread
     */
    private static final int MSG_LUX = 104;
    private static final int MSG_MOV = 105;
    private static final int MSG_HUMIDITY = 101;
    private static final int MSG_PRESSURE = 102;
    private static final int MSG_PRESSURE_CAL = 103;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_MOV:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining mov value");
                        return;
                    }
                    updateMovValues(characteristic);
                    break;
                case MSG_LUX:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining lux value");
                        return;
                    }
                    updateLuxValues(characteristic);
                    break;
                case MSG_HUMIDITY:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining humidity value");
                        return;
                    }
                    updateHumidityValues(characteristic);
                    break;
                case MSG_PRESSURE:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining pressure value");
                        return;
                    }
                    updatePressureValue(characteristic);
                    break;
                case MSG_PRESSURE_CAL:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining cal value");
                        return;
                    }
                    updatePressureCals(characteristic);
                    break;
                case MSG_PROGRESS:
                    mProgress.setMessage((String) msg.obj);
                    if (!mProgress.isShowing()) {
                        mProgress.show();
                    }
                    break;
                case MSG_DISMISS:
                    mProgress.hide();
                    break;
                case MSG_CLEAR:
                    clearDisplayValues();
                    break;
            }
        }
    };

    /* Methods to extract sensor data and update the UI */
    private void updateMovValues(BluetoothGattCharacteristic characteristic) {
        //double humidity = SensorTagData.extractLux(characteristic);
        //mMov.setText(String.format("%.2f", humidity));

        byte[] value = characteristic.getValue();
        Point3D v;

        v = SensorTagData.extractMov_Acc(value);
        mMov.setText(String.format("X:%.2fG, Y:%.2fG, Z:%.2fG", v.x,v.y,v.z));

        v = SensorTagData.extractMov_Gyro(value);
        mMov2.setText(String.format("X:%.2f'/s, Y:%.2f'/s, Z:%.2f'/s", v.x,v.y,v.z));

        //MAG - PRETTY MUCH USELESS. ABSOLUTE POSITIONING IN NSEW PLANE
        v = SensorTagData.extractMov_Mag(value);
        mMov3.setText(String.format("X:%.2fuT, Y:%.2fuT, Z:%.2fuT", v.x,v.y,v.z));
    }



    private void updateLuxValues(BluetoothGattCharacteristic characteristic) {
        double humidity = SensorTagData.extractLux(characteristic);
        mLux.setText(String.format("%.2f", humidity));
    }
    private void updateHumidityValues(BluetoothGattCharacteristic characteristic) {
        double humidity = SensorTagData.extractHumidity(characteristic);

        mHumidity.setText(String.format("%.02f%%", humidity));
    }

    private int[] mPressureCals;
    private void updatePressureCals(BluetoothGattCharacteristic characteristic) {
        mPressureCals = SensorTagData.extractCalibrationCoefficients(characteristic);
    }

    private void updatePressureValue(BluetoothGattCharacteristic characteristic) {
        if (mPressureCals == null) return;
        double pressure = SensorTagData.extractBarometer(characteristic, mPressureCals);
        double temp = SensorTagData.extractBarTemperature(characteristic, mPressureCals);

        mTemperature.setText(String.format("%.1f\u00B0C", temp));
        mPressure.setText(String.format("%.2f", pressure));
    }
}
