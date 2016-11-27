package uzmany.bmonitor;

import android.app.Service;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.IBinder;


import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class BLEservice extends Service {


    private boolean CONNECT_COMPLETE;
    private BluetoothGatt mConnectedGatt;
    private SparseArray<BluetoothDevice> mDevices;

    private static final String TAG = "BluetoothGattActivity";
    private static final String DEVICE_NAME = "CC2650 SensorTag";

    private static final UUID MOV_SERVICE =     UUID.fromString("f000aa80-0451-4000-b000-000000000000");
    private static final UUID MOV_DATA_CHAR =   UUID.fromString("f000aa81-0451-4000-b000-000000000000");
    private static final UUID MOV_CONFIG_CHAR = UUID.fromString("f000aa82-0451-4000-b000-000000000000");
    private static final UUID MOV_CONFIG_PERIOD= UUID.fromString("f000aa83-0451-4000-b000-000000000000");

    private static final UUID HUM_SERV = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID HUM_DATA = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID HUM_CONF = UUID.fromString("f000aa22-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    private static final UUID HUM_PERI = UUID.fromString("f000aa23-0451-4000-b000-000000000000"); // Period in tens of milliseconds

    private static final UUID LUX_SERV = UUID.fromString("f000aa70-0451-4000-b000-000000000000");
    private static final UUID LUX_DATA = UUID.fromString("f000aa71-0451-4000-b000-000000000000");
    private static final UUID LUX_CONF = UUID.fromString("f000aa72-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    private static final UUID LUX_PERI = UUID.fromString("f000aa73-0451-4000-b000-000000000000"); // Period in tens of milliseconds

    private static final int MSG_MOV = 105;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;

    private static final int MSG_HUM = 666;
    private static final int MSG_LUX = 777;

    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private ProgressDialog mProgress;

    private TextView DeviceSelection;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        CONNECT_COMPLETE = false;

        mProgress = new ProgressDialog(this);

        Bundle extras = intent.getExtras();
        BluetoothDevice device = (BluetoothDevice) extras.get("BLDevice");
        Log.i(TAG, "Connecting to "+ device.getName());

        mConnectedGatt = device.connectGatt(this, false, mGattCallback);
        //Display progress UI
        mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to "+device.getName()+"..."));
        return super.onStartCommand(intent,flags,startId);
    }

    public BLEservice() {
    }


    private final IBinder mBinder = new MyBinder();
    private Messenger outMessenger;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        Bundle extras = intent.getExtras();
        Log.d("service","onBind");
        // Get messager from the Activity
        if (extras != null) {
            Log.d("service","onBind with extra");
            outMessenger = (Messenger) extras.get("MESSENGER");
        }
        return mBinder;
    }


    public class MyBinder extends Binder {
        BLEservice getService() {
            return BLEservice.this;
        }
    }


    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }

    /*HANDLING GATT CALL BACKS*/
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /* State Machine Tracking */
        private int mState = 0;
        private void reset()   { mState = 0; }
        private void advance() { mState++; }

        /*IF THE CONNECTION IS SUCCESSFUL ONCONNECTIONSTATECHANGE WILL BE CALLED*/
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

        /*WHEN THE CONNECTION IS SUCCESSFUL THIS IS CALLEDBACK*/
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            int i =0;

            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                Log.d("Services Values: ", String.valueOf(characteristics));

            }

            Log.d(TAG, "Services Discovered: "+status);
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
            /*
             * With services discovered, we are going to reset our state machine and start
             * working through the sensors we need to enable
             */
            reset();
            BluetoothGattCharacteristic characteristic, characteristic2;
            switch (mState) {
                //case 3:
                   // Log.d(TAG, "Setting mov period");

                    /*characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_CONFIG_CHAR);
                    characteristic.setValue(new byte[] {0x7f, 0x00});
                    */
                    /*characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_CONFIG_PERIOD);
                    characteristic.setValue(new byte[] {0x0A});
                    */
                   // break;
                case 0:
                    Log.d(TAG, "Enabling mov");

                    characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_CONFIG_CHAR);
                    characteristic.setValue(new byte[] {0x7f, 0x00});

            /*        characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_CONFIG_PERIOD);
                    characteristic.setValue(new byte[] {0x0A});
*/
                    break;

                case 1:
                    Log.d(TAG, "Enabling HUM");
/*
                    characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_CONFIG_CHAR);
                    characteristic.setValue(new byte[] {0x7f, 0x00});
  */
                    characteristic = gatt.getService(HUM_SERV)
                            .getCharacteristic(HUM_CONF);
                    characteristic.setValue(new byte[] {0x01});

                    break;
                case 2:
                    Log.d(TAG, "Enabling LUX");

                    characteristic = gatt.getService(LUX_SERV)
                            .getCharacteristic(LUX_CONF);
                    characteristic.setValue(new byte[] {0x01});
                    break;

                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled2");
                    return;
            }
            gatt.writeCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*
             * After notifications are enabled, all updates from the device on characteristic
             * value changes will be posted here.  Similar to read, we hand these up to the
             * UI thread to update the display.
             */
            int whaterverbitches= 0;
            if (HUM_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HUM, characteristic));
            }
            if (LUX_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_LUX, characteristic));
            }
            if (MOV_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_MOV, characteristic));
            }
        }


        /*
         * Send an enable command to each sensor by writing a configuration
         * characteristic.  This is specific to the SensorTag to keep power
         * low by disabling sensors you aren't using.
         */
        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic, characteristic2;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Enabling mov");

                    characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_CONFIG_CHAR);
                    characteristic.setValue(new byte[] {0x7f, 0x00});

                    /*characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_CONFIG_PERIOD);
                    characteristic.setValue(new byte[] {0x64});
                    */
                    //characteristic.setValue(new byte[] {0x00, 0x7f});
                    break;
                case 1:
                    Log.d(TAG, "Enabling HUM");

                    characteristic = gatt.getService(HUM_SERV)
                            .getCharacteristic(HUM_CONF);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                case 2:
                    Log.d(TAG, "Enabling LUX");

                    characteristic = gatt.getService(LUX_SERV)
                            .getCharacteristic(LUX_CONF);
                    characteristic.setValue(new byte[] {0x01});
                    break;

                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled2");
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
                case 0:
                    Log.d(TAG, "Reading mov");
                    characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_DATA_CHAR);
                    break;
                case 1:
                    Log.d(TAG, "Reading hum");
                    characteristic = gatt.getService(HUM_SERV)
                            .getCharacteristic(HUM_DATA);
                    break;
                case 2:
                    Log.d(TAG, "Reading LUX");
                    characteristic = gatt.getService(LUX_SERV)
                            .getCharacteristic(LUX_DATA);
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
                case 0:
                    Log.d(TAG, "Set notify mov");
                    characteristic = gatt.getService(MOV_SERVICE)
                            .getCharacteristic(MOV_DATA_CHAR);
                    break;
                case 1:
                    Log.d(TAG, "Set notify hum");
                    characteristic = gatt.getService(HUM_SERV)
                            .getCharacteristic(HUM_DATA);
                    break;
                case 2:
                    Log.d(TAG, "Set notify LUX");
                    characteristic = gatt.getService(LUX_SERV)
                            .getCharacteristic(LUX_DATA);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);

                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            /*for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                //find descriptor UUID that matches Client Characteristic Configuration (0x2902)
                // and then call setValue on that descriptor

                gatt.setCharacteristicNotification(characteristic, true);

                descriptor.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }*/

            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }





        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //For each read, pass the data up to the UI thread to update the display
            int hi=0;
            if (HUM_DATA.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_HUM, characteristic));
            }
            if (MOV_DATA_CHAR.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_MOV, characteristic));
            }
            if (LUX_DATA.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_LUX, characteristic));
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
                    Log.d("UpdateWorking:" , "YES");
                    updateMovValues2(characteristic);
                    break;
                case MSG_HUM:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining hum value");
                        return;
                    }
                    Log.d("HUMUPDATE:" , "YES");
                    updateMovValues(characteristic);
                    break;
                case MSG_LUX:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining lux value");
                        return;
                    }
                    Log.d("LUXUPDATE:" , "YES");
                    updateLuxValues(characteristic);
                    break;
                case MSG_PROGRESS:
                    mProgress.setMessage((String) msg.obj);
                    if (!mProgress.isShowing()) {
                        // mProgress.show();
                    }
                    break;
                case MSG_DISMISS:
                    // mProgress.hide();
                    break;
                case MSG_CLEAR:
                    break;
            }
        }
    };


    double stablelevel =0;
    private void updateLuxValues (BluetoothGattCharacteristic characteristic) {

        double stablelevel2 = SensorTagData.extractLux(characteristic);
        Log.d(TAG, "updateLUXValues: "+stablelevel2);
    }
    private void updateMovValues(BluetoothGattCharacteristic characteristic) {

        byte [] value = characteristic.getValue();
        stablelevel = SensorTagData.extractHumidity(characteristic);

        /*
        byte[] value = characteristic.getValue();

        Point3D v, v2,v3;

        v = SensorTagData.extractMov_Acc(value);
        //mMov.setText(String.format("X:%.2fG, Y:%.2fG, Z:%.2fG", v.x,v.y,v.z));

        v2 = SensorTagData.extractMov_Gyro(value);
        //mMov2.setText(String.format("X:%.2f'/s, Y:%.2f'/s, Z:%.2f'/s", v2.x,v2.y,v2.z));

        //MAG - PRETTY MUCH USELESS. ABSOLUTE POSITIONING IN NSEW PLANE
        v3 = SensorTagData.extractMov_Mag(value);
        //mMov3.setText(String.format("X:%.2fuT, Y:%.2fuT, Z:%.2fuT", v3.x,v3.y,v3.z));


        stablelevel = Math.abs(v2.x) + Math.abs(v2.y) +Math.abs(v2.z) ;*/
        Log.d(TAG, "updateHUMValues: "+stablelevel);
    }

    double stablelevel3;
    Point3D point3DStable;
    private void updateMovValues2(BluetoothGattCharacteristic characteristic) {



        byte[] value = characteristic.getValue();

        Point3D v, v2,v3;

        v = SensorTagData.extractMov_Acc(value);
        //mMov.setText(String.format("X:%.2fG, Y:%.2fG, Z:%.2fG", v.x,v.y,v.z));

        v2 = SensorTagData.extractMov_Gyro(value);
        //mMov2.setText(String.format("X:%.2f'/s, Y:%.2f'/s, Z:%.2f'/s", v2.x,v2.y,v2.z));

        //MAG - PRETTY MUCH USELESS. ABSOLUTE POSITIONING IN NSEW PLANE
        v3 = SensorTagData.extractMov_Mag(value);
        //mMov3.setText(String.format("X:%.2fuT, Y:%.2fuT, Z:%.2fuT", v3.x,v3.y,v3.z));

        v2=v;
        stablelevel3 = 10*Math.abs(v2.x) + 10*Math.abs(v2.y) + 10*Math.abs(v2.z) ;
        point3DStable = v2;
        point3DStable.x = (point3DStable.x);
        point3DStable.y = (point3DStable.y);
        point3DStable.z = (point3DStable.z);
        Log.d(TAG, "updateMOVINGGGValues: "+stablelevel3);
    }

    public double   get_sindex()       { return stablelevel3;    }
    public Point3D  get3DStability()   { return point3DStable;   }
    public boolean get_bind_complete() { return CONNECT_COMPLETE;}


}


