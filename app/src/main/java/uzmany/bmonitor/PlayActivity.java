package uzmany.bmonitor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class PlayActivity extends AppCompatActivity {

    private final Handler mHandler = new Handler();
    private TextView test_text;
    private double stability;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        int mUIFlag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        getWindow().getDecorView().setSystemUiVisibility(mUIFlag);

        doBindService();

        startTestThread();

        test_text = (TextView) findViewById(R.id.test_text);
    }

    private Runnable updatedraw = new Runnable() {
        @Override
        public void run() {
            Log.d("PLAY GETTING IT::", Double.toString(myServiceBinder.get_sindex()));
            stability = myServiceBinder.get_sindex();
            test_text.setText(Double.toString(stability));
            mHandler.postDelayed(this, 200);
        }
    };

    protected void startTestThread() {
        Thread t = new Thread() {
            public void run() {

                mHandler.post(updatedraw);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        test_text.setText(Double.toString(stability));
                    }
                });
            }
        };
        t.start();
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

}


