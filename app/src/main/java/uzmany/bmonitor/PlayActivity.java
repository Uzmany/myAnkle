package uzmany.bmonitor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class PlayActivity extends AppCompatActivity {

    private final Handler mHandler = new Handler();
    private TextView test_text;
    private double stability;
    Bitmap target, background, aim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        int mUIFlag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.STATUS_BAR_HIDDEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        getWindow().getDecorView().setSystemUiVisibility(mUIFlag);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        doBindService();

        //startTestThread();
/*
        test_text = (TextView) findViewById(R.id.test_text);

        //Drawing the game
        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();
        background = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        target = BitmapFactory.decodeResource(getResources(), R.drawable.target);
        aim = BitmapFactory.decodeResource(getResources(), R.drawable.aim);

        XVALS = new ArrayList<Double>();
        YVALS = new ArrayList<Double>();
        */

    }


    public void playGameOneLoop( Point3D stable3D){

        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();

        int scaledWidth = width;
        int scaledHeight= (int) (height * 0.6);

        final ImageView imgCircle= (ImageView) findViewById(R.id.GameArea);
        Bitmap bmp = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bmp);

        double XCOR = stable3D.x;
        double YCOR = stable3D.y;
        double Xaverage = 0;
        double Yaverage = 0;
        //Average:
        if ( XVALS.size() < 10 ) {

            XVALS.add(XCOR);
            YVALS.add(YCOR);
        }
        else {

            XVALS.remove(0);
            YVALS.remove(0);
            XVALS.add(XCOR);
            YVALS.add(YCOR);

            for (int i=0; i<10; i++) {

                Xaverage += XVALS.get(i);
                Yaverage += YVALS.get(i);
            }
        }

        Xaverage /= 10;
        Yaverage /= 10;

        //1. Draw target Layer

        Bitmap targetR = Bitmap.createScaledBitmap(target, scaledWidth, scaledHeight, false);
        canvas.drawBitmap(targetR, 0 , 0 , null);

        //2. Draw Aim Layer

        imgCircle.setImageBitmap(bmp);
        Bitmap aimR = Bitmap.createScaledBitmap(aim, scaledWidth/8, scaledHeight/8, false);




        int moveX = (int) (Xaverage/10 *10* ( scaledWidth/4));
        int moveY = (int) (Yaverage/10 *10* ( scaledHeight/4));

        int startXAim=0, startYAim=0;

        startXAim = (int) (startXAim  +  targetR.getWidth()/2   - aimR.getWidth()/2  );  // midX - (((int) stability)  );   // 300   -   ( 500%300 )
        startYAim = (int) (startYAim  +  targetR.getHeight()/2  - aimR.getHeight()/2  );



        startXAim += moveX;
        startYAim += moveY;

        Log.d("Details: ",  startXAim +" " + startYAim + " " + moveX +" " + moveY + " " + scaledWidth + " " + scaledHeight + " " + stable3D.x + "  " + stable3D.y);

        canvas.drawBitmap(aimR, startXAim , startYAim , null);

    }




    public void playGameTwoLoop(Point3D stable3D) {

    }


    ArrayList<Double> XVALS;
    ArrayList<Double> YVALS;

    private Runnable updatedraw = new Runnable() {
        @Override
        public void run() {

            Log.d("PLAY GETTING::", Double.toString(myServiceBinder.get_sindex()));
            Point3D stable3D = myServiceBinder.get3DStability();
            stability = myServiceBinder.get_sindex();
            test_text.setText(Double.toString(stability));


            Log.d("Play Loop" , "Working here");



            //playGameOneLoop(stable3D);

            playGameTwoLoop(stable3D);

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

    public static BLEservice myServiceBinder;

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
        setContentView(new GamePanel(this, myServiceBinder));
    }


    public static Point3D getPointPlay () {

        Point3D empty = new Point3D(0,0,0);

        if (myServiceBinder != null) {
            return myServiceBinder.get3DStability();
        }
        else  {
            return empty;
        }
    }
    /******************************************************************************/
    /******************************************************************************/
    /******************************************************************************/
    /***************************END OF CONNECTING BLE SERVICE**********************/
    /******************************************************************************/
    /******************************************************************************/
    /******************************************************************************/

}


