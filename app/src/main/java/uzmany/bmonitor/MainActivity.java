package uzmany.bmonitor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v7.view.menu.MenuView;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
        import android.widget.Toast;
import android.widget.ToggleButton;


import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {

    private static final int STABILITY_TAB = 1;
    private static final int HISTORY_TAB =2;
    private static final int PLAY_TAB =3;
    private static int tab_view=STABILITY_TAB;

    private static final String TAG = "BluetoothGattActivity";
    private static final String DEVICE_NAME = "CC2650 SensorTag";

    /* ACC Service */

    private static final UUID MOV_SERVICE =     UUID.fromString("f000aa80-0451-4000-b000-000000000000");
    private static final UUID MOV_DATA_CHAR =   UUID.fromString("f000aa81-0451-4000-b000-000000000000");
    private static final UUID MOV_CONFIG_CHAR = UUID.fromString("f000aa82-0451-4000-b000-000000000000");

    /* Client Configuration Descriptor */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevices;

    private BluetoothGatt mConnectedGatt;

    private TextView DeviceSelection;
    private TextView DeviceDescription;
    private ProgressDialog mProgress;

    private static final int MSG_MOV = 105;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;

    public int whichmethod=1;
    Point3D ov=null, ov2=null, ov3=null;
    double st1=0,st2=0,st3=0;
    double st1s=0,st2s=0,st3s=0;
    double totalstable;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_main);
        setProgressBarIndeterminate(true);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Typeface custom_font_roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/blair_itc_bold1.ttf");
        Typeface custom_font_bold = Typeface.createFromAsset(getAssets(), "fonts/blair_itc_bold1.ttf");


        TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText("My Ankle");
        toolbarTitle.setTextSize(16);
        toolbarTitle.setTextColor(Color.parseColor("#ffffff"));

        toolbar.setBackgroundColor(Color.parseColor("#d15851"));

        LinearLayout devices_drop = (LinearLayout) findViewById(R.id.device_select_drop);
        devices_drop.setVisibility(View.GONE);
        devices_drop.setBackgroundColor(Color.parseColor("#d15851"));
        /*
         * We are going to display the results in some text fields
         */

        DeviceSelection = (TextView) findViewById(R.id.devices);
        DeviceSelection.setText("");
        DeviceDescription = (TextView) findViewById(R.id.devices_detail);

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


        onDraw(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
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
            // THE THREE DOTS MENU BUTTON
            //BluetoothDevice device = mDevices.valueAt(i);
           // menu.add(0, mDevices.keyAt(i), 0, device.getName());
        }

        for (int i=0; i < mDevices.size(); i++) {
            BluetoothDevice device = mDevices.valueAt(i);
            DeviceSelection.setText("CONNECT: " +device.getName() );
            DeviceDescription.setText(device.getAddress() );
        }
        return true;
    }

    private boolean show_device_drop = true;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                if (show_device_drop) {
                    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                    TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
                    toolbarTitle.setText("My Ankle");
                    toolbarTitle.setTextSize(18);
                    findViewById(R.id.device_select_drop).setVisibility(View.GONE);
                    show_device_drop = false;
                }
                else {
                    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                    TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
                    toolbarTitle.setText("Select Device");
                    findViewById(R.id.device_select_drop).setVisibility(View.VISIBLE);
                    show_device_drop=true;
                }
                //mDevices.clear();
                //startScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    /*
     * We have a Handler to process event results on the main thread
     */

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
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
                    break;
            }
        }
    };

    /* Methods to extract sensor data and update the UI */


    //BIND TO SERVICEE

    public void connect_device (View v) {
        Intent serviceIntent = new Intent(this, BLEservice.class);
        serviceIntent.putExtra("BLDevice", mDevices.valueAt(0) );
        startService(serviceIntent);
        doBindService();
        DeviceSelection.setText("Bluetooth Device Connected");
        DeviceDescription.setTextColor(Color.parseColor("#0000ff"));
        DeviceSelection.setTextColor(Color.parseColor("#0000ff"));



        LinearLayout devices_drop = (LinearLayout) findViewById(R.id.device_select_drop);
        devices_drop.setBackgroundColor(Color.parseColor("#65b2da"));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.parseColor("#65b2da"));



        startTestThread();




    }




    /*DRAW THE IMAGEVIEW TO DISPLAY STABILITY INDEX*/

    private int mode =0;
    int[] historyarray;

    public void onDraw(double totalstable){

        Typeface custom_font_roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/blair_itc_bold1.ttf");
        Typeface custom_font_bold = Typeface.createFromAsset(getAssets(), "fonts/blair_itc_bold1.ttf");

        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();

        int text_size=130;

        final ImageView imgCircle= (ImageView) findViewById(R.id.imgCircle);

        Paint paint = new Paint();

        if (totalstable>=100) {paint.setColor(Color.parseColor("#FF0000")); }
        if (totalstable<100) {paint.setColor(Color.parseColor("#FFD700"));}
        if (totalstable<50) {paint.setColor(Color.parseColor("#00FF00"));}
        if (totalstable<30) {paint.setColor(Color.parseColor("#0000FF"));}

        paint.setStyle(Paint.Style.STROKE);

        Bitmap bmp = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bmp);
        Paint paint2 = new Paint();



        if (mode == 0) {
            historyarray = new int[100];
            paint2.setTypeface(custom_font_bold);
            paint2.setTextSize(text_size);
            paint2.setTextAlign(Paint.Align.CENTER);

            paint2.setColor(Color.parseColor("#FF0000"));
            if (totalstable < 100) {
                paint2.setColor(Color.parseColor("#FFD700"));
            }
            if (totalstable < 50) {
                paint2.setColor(Color.parseColor("#00FF00"));
            }
            if (totalstable < 30) {
                paint2.setColor(Color.parseColor("#0000FF"));
            }

            canvas.drawText(String.format("%.0f", totalstable), bmp.getWidth() / 2, 5 * (bmp.getHeight() / 8) + (text_size / 4) - 30, paint2);

            paint2.setTypeface(custom_font);
            paint2.setTextSize(2 * text_size / 4);

            paint2.setColor(Color.parseColor("#65b2da"));
            canvas.drawText("Stability Index", bmp.getWidth() / 2, 5 * (bmp.getHeight() / 8) + (text_size) - 20, paint2);

            int radius;
            int circle_width = 30;
            radius = (bmp.getHeight() < bmp.getWidth()) ? bmp.getHeight() / 3 : bmp.getWidth() / 3;
            int i = 0;
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            for (i = 0; i < circle_width; i++)
                canvas.drawCircle(bmp.getWidth() / 2, 5 * (bmp.getHeight() / 8), radius + i, paint);

            imgCircle.setImageBitmap(bmp);

        }
        else if (mode == 1) {


            paint2.setColor(Color.parseColor("#FF0000"));
            if (totalstable < 100) {
                paint2.setColor(Color.parseColor("#FFD700"));
            }
            if (totalstable < 50) {
                paint2.setColor(Color.parseColor("#00FF00"));
            }
            if (totalstable < 30) {
                paint2.setColor(Color.parseColor("#0000FF"));
            }

            int radius;
            int circle_width = 1;
            radius = 1;
            int i = 0;
            int j =0;


                historyarray[bind_complete%100] = (int) totalstable;

            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            for (j=0;j<100;j++) {
                for (i = 0; i < circle_width; i++) {
                    canvas.drawCircle(j * (bmp.getWidth() / 100), (float) ( ((0.8)*bmp.getHeight() - historyarray[j] * (bmp.getHeight() / 1000))), radius + i, paint);
                    int k = j - 1;
                    if (k >= 0)
                        canvas.drawLine(k * (bmp.getWidth() / 100), (float) (i+((0.8)*bmp.getHeight() -  historyarray[k] * (bmp.getHeight() / 1000))), j * (bmp.getWidth() / 100), (float) (i+ ((0.8)*bmp.getHeight() - historyarray[j] * (bmp.getHeight() / 1000))), paint);

                }
            }
            imgCircle.setImageBitmap(bmp);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);

        //TextView devices_title = (TextView) findViewById(R.id.devices_title);
        TextView one_tab       = (TextView) findViewById(R.id.si_tab);
        TextView two_tab       = (TextView) findViewById(R.id.history_tab);
        TextView three_tab     = (TextView) findViewById(R.id.play_tab);

       //devices_title.setTypeface(custom_font_bold);
        toolbarTitle.setTypeface(custom_font_roboto);
        one_tab.setTypeface(custom_font);
        two_tab.setTypeface(custom_font);
        three_tab.setTypeface(custom_font);

        DeviceSelection.setTypeface(custom_font_bold);
    }

    public void stability_tab_press(View v) {
        TextView one_tab = (TextView) findViewById(R.id.si_tab);
        TextView two_tab = (TextView) findViewById(R.id.history_tab);
        TextView three_tab = (TextView) findViewById(R.id.play_tab);

        one_tab.setBackgroundResource(R.drawable.gradient_bg);
        two_tab.setBackgroundResource(0);
        three_tab.setBackgroundResource(0);


        mode = 0;



    }
    public void history_tab_press(View v) {
        TextView one_tab = (TextView) findViewById(R.id.si_tab);
        TextView two_tab = (TextView) findViewById(R.id.history_tab);
        TextView three_tab = (TextView) findViewById(R.id.play_tab);

        one_tab.setBackgroundResource(0);
        two_tab.setBackgroundResource(R.drawable.gradient_bg);
        three_tab.setBackgroundResource(0);

        mode = 1;


    }
    public void play_tab_press(View v) {
        TextView one_tab = (TextView) findViewById(R.id.si_tab);
        TextView two_tab = (TextView) findViewById(R.id.history_tab);
        TextView three_tab = (TextView) findViewById(R.id.play_tab);

        one_tab.setBackgroundResource(0);
        two_tab.setBackgroundResource(0);
        three_tab.setBackgroundResource(R.drawable.gradient_bg);

        Intent intent = new Intent(this, PlayActivity.class);
        startActivity(intent);
    }

    public void refresh_devices_Click(View v) {
        mDevices.clear();
        startScan();
    }



    /******************************************************************************/
    /******************************************************************************/
    /******************************************************************************/
    /******************************CONNECT TO THE BLE SERVICE**********************/
    /******************************************************************************/
    /******************************************************************************/
    /******************************************************************************/

    private BLEservice myServiceBinder;
    public ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            myServiceBinder = ((BLEservice.MyBinder) binder).getService();
            Log.d("ServiceConnection","connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("ServiceConnection","disconnected");

        }
    };

    public Handler myHandler = new Handler() {
        public void handleMessage(Message message) {
            Bundle data = message.getData();
        }
    };

    public void doBindService() {
        Intent intent = null;
        intent = new Intent(this, BLEservice.class);
        // Create a new Messenger for the communication back
        // From the Service to the Activity
        Messenger messenger = new Messenger(myHandler);
        intent.putExtra("MESSENGER", messenger);

        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
        Toast.makeText(this, "Bind Complete!", Toast.LENGTH_SHORT).show();


    }

    /******************************************************************************/
    /******************************************************************************/
    /******************************************************************************/
    /***************************END OF CONNECTING BLE SERVICE**********************/
    /******************************************************************************/
    /******************************************************************************/
    /******************************************************************************/


    private int bind_complete =0;

    private Runnable updatedraw = new Runnable() {
        @Override
        public void run() {

            //ADD A DELAYY
            if (bind_complete > 5) {
            onDraw(myServiceBinder.get_sindex());
                findViewById(R.id.device_select_drop).setVisibility(View.GONE);
            }
            bind_complete++;
            mHandler.postDelayed(this, 100);
        }
    };

    protected void startTestThread() {
        Thread t = new Thread() {
            public void run() {
                mHandler.post(updatedraw);
            }
        };
        t.start();
    }

}
