package uzmany.bmonitor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Usman on 2016-11-01.
 */

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback
{
    public static final int WIDTH = 320;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -20;
    private MainThread thread;
    private Background bg;
    private Player player;
    private ArrayList<LeftBorder> Leftborder;
    private ArrayList<RightBorder> Rightborder;

    BLEservice myServiceBinder;

    MediaPlayer mediaPlayer, mediaPlayer2, mediaPlayer3;


    public GamePanel(Context context, BLEservice getServiceBinder)
    {

        super(context);
        myServiceBinder = getServiceBinder;
        if (myServiceBinder != null) {
            Log.d("GamePanel:", "Receiving ServiceBineder %f" + myServiceBinder.get3DStability().x);
        }
        //add the callback to the surfaceholder
        getHolder().addCallback(this);
        thread = new MainThread(getHolder(),this , myServiceBinder);
        //make gamePanel focusable so it an handle events
        setFocusable(true);

        // GAME STATES
        playerInside = true;
        totScore =0;
        justStarted = false;
        firstUpdate = true;
        zBench = 0;
        startTime = 0;
        currTime = 0;

        mediaPlayer = MediaPlayer.create(context, R.raw.heartbeatfast);
        mediaPlayer2 = MediaPlayer.create(context, R.raw.heartbeatfast);
        mediaPlayer3 = MediaPlayer.create(context, R.raw.heartbeatslow);

        centreWidth = 350;
        gameState = gS_INTRO;

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}


    @Override
    public void surfaceDestroyed(SurfaceHolder holder){

        boolean retry = true;
        while (retry)
        {
            try {
                thread.setRunning(false);
                thread.join();

            }catch (InterruptedException e){e.printStackTrace();}
            retry = false;

        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder){

        Bitmap back = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        Bitmap backScaled = Bitmap.createScaledBitmap( back, getWidth(), getHeight(), false );
        Bitmap playerB = BitmapFactory.decodeResource(getResources(), R.drawable.ninja);

        float playWidth = (float) 0.13125 * (float) getWidth();
        float playHeight =(float) 0.0875 * (float) getHeight();
        int playWidthI = (int) playWidth;
        int playHeightI = (int) playHeight;

        Bitmap playerBScaled = Bitmap.createScaledBitmap( playerB, playWidthI, playHeightI, false );


        //Bitmap.createScaledBitmap(target, scaledWidth, scaledHeight, false);
        bg = new Background( backScaled  );
       // bg     = new Background (BitmapFactory.decodeResource(getResources(),R.drawable.background));
        player = new Player( playerBScaled ,playWidthI,playHeightI,1); //bitmap for the ninja with 42x42
        Leftborder = new ArrayList<LeftBorder>();
        Rightborder = new ArrayList<RightBorder>();

        //start the game loop
        thread.setRunning(true);
        thread.start();

    }
    public class gameClickBox {
        float startX;
        float endX;
        float startY;
        float endY;
    }

    public boolean checkwithinBox( gameClickBox thisBox, float xPos, float yPos ) {

        xPos  = xPos / scaleFactorXU;
        yPos  = yPos / scaleFactorYU;

        Log.d("gs_INTRO_checkBox", "Outside Box X:" + Float.toString(thisBox.startX) + " Y:" + Float.toString(thisBox.startY) );

        if ( xPos <= thisBox.endX && xPos >= thisBox.startX  && yPos <= thisBox.endY && yPos >= thisBox.startY )
            return true;
        else
            return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction()== MotionEvent.ACTION_DOWN){

            float xTouch = event.getX();
            float yTouch = event.getY();


            switch ( gameState) {
                case gS_INTRO:
                        /*
                        if ( checkwithinBox( startBox, xTouch, yTouch ) ) {
                            gameState = gS_PLAYING;
                            Log.d("gs_INTRO", "Inside Box");
                        }
                        else Log.d("gs_INTRO", "Outside Box X:" + Float.toString(xTouch) + " Y:" + Float.toString(yTouch) );
                        */
                        if ( checkwithinBox(startBox, xTouch, yTouch) ) {
                            gameState = gS_PLAYING;
                        }
                        else if ( checkwithinBox( eyesBox, xTouch, yTouch ) ) {
                            gameEyes++;
                            gameEyes = gameEyes % 2;
                        }
                        else if ( checkwithinBox(testBox, xTouch, yTouch) ) {
                            gameTest++;
                            gameTest = gameTest % 2;
                        }
                        else if ( checkwithinBox(levelBox, xTouch, yTouch) ) {
                            gameLevel++;
                            gameLevel = gameLevel % 3;
                        }

                    return true;
                case gS_PLAYING:
                    if (!player.startCalibrating) {
                        player.startCalibrating = true;
                    } else if (!player.calibrated) {

                    } else if (!player.getPlaying()) {
                        player.setPlaying(true);
                        justStarted = true;
                    } else {
                        player.setUp(true);
                    }
                    return true;
                case gS_END:
                    return true;
                default:
                    return true;

            }
        }

        if (event.getAction()==MotionEvent.ACTION_UP)
        {
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }
    public static double zBench;
    public static int zBenchCounter;
    public static double startTime;
    public static double currTime;

    public static Boolean firstUpdate;
    public void update (Point3D stable3D)
    {
        if ( player.startCalibrating && !player.calibrated ) {
            currTime = System.currentTimeMillis();

            if (startTime == 0) {
                startTime = currTime;
            }
            if (currTime == startTime) {
                zBench += stable3D.z;
                zBenchCounter = 1;
            }
            else if (  (currTime-startTime) >  3000 ) {
                zBench += stable3D.z;
                player.calibrated = true;
            }
            else if ( (currTime-startTime) > 1000  && zBenchCounter == 1 ) {
                zBench += stable3D.z;
                zBenchCounter = 2;
            }

            //player.calibrated = !player.calibrated;

        }

        if (player.getPlaying())
        {
            if (firstUpdate) {
               // zBench = stable3D.z;
                firstUpdate = !firstUpdate;
            }
            bg.update();
            player.update(stable3D, zBench);


            //check Leftborder collision
            for(int i = 0; i<Leftborder.size(); i++)
            {
             //   if(collision(Leftborder.get(i), player))
               //     player.setPlaying(false);
            }

            //check Rightborder collision
            for(int i = 0; i <Rightborder.size(); i++)
            {
              //  if(collision(Rightborder.get(i),player))
               //     player.setPlaying(false);
            }

            //update left border
            this.updateLeftBorder();
            //udpate right border
            this.updateRightBorder();

        }

    }

    public boolean collision(GameObject a, GameObject b)
    {
        if(Rect.intersects(a.getRectangle(), b.getRectangle()))
        {
            return true;
        }
        return false;
    }

    public static Boolean playerInside;
    public static int totScore;
    public static boolean justStarted;
    public static int centreWidth;

    /*GAME VARIABLES*/
    public static int gameState;
    private static final int  gS_INTRO = 000;
    private static final int  gS_PLAYING = 111;
    private static final int  gS_END = 222;

    private static final int  EYES_ON = 0;
    private static final int  EYES_OFF = 1;

    private static final int TEST_ON = 0;
    private static final int TEST_OFF = 1;

    private static final int LEVEL_ONE = 0;
    private static final int LEVEL_TWO = 1;
    private static final int LEVEL_THREE = 2;

    public int gameEyes, gameTest, gameLevel;

    public gameClickBox startBox, eyesBox, levelBox, testBox;

    public float scaleFactorXU, scaleFactorYU;

    public void draw (Canvas canvas) {
        final float scaleFactorX = (float) getWidth() /   canvas.getWidth(); // (WIDTH * 1.f);
        final float scaleFactorY = (float) getHeight() / canvas.getHeight();//(HEIGHT * 1.f);
        scaleFactorXU= scaleFactorX;
        scaleFactorYU= scaleFactorY;
        Log.d("Width and Height" , "H: " + getHeight() + " W: " + getWidth() + "cH" + canvas.getHeight() + "cW" + canvas.getWidth());

        if (canvas != null) {

            final int savedState = canvas.save();
            canvas.scale((scaleFactorX), (scaleFactorY));
            bg.draw(canvas);
            player.draw(canvas);
            canvas.restoreToCount(savedState);

            Paint paint = new Paint();

            //gameState = gS_INTRO;
            switch (gameState) {
                case gS_INTRO:

                    paint.setColor(Color.BLACK);
                    canvas.drawRect(100, 100, getWidth()-100, getHeight()-100,paint);
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(100);
                    paint.setStrokeWidth(3);


                    paint.setColor(Color.RED);
                    canvas.drawRect( getWidth() / 2 - 250, getHeight() - 450, getWidth() / 2 +250, getHeight() - 250,paint); // START BOX
                    canvas.drawRect( getWidth() / 2 + 250  , 400, getWidth() / 2 +550, 600 ,paint); // EYES BOX
                    canvas.drawRect( getWidth() / 2 + 250  , 700, getWidth() / 2 +550, 900,paint); // LEVEL BOX
                    canvas.drawRect( getWidth() / 2 + 250  , 1000 , getWidth() / 2 +550, 1200,paint); // TEST BOX

                    paint.setColor(Color.WHITE);
                    canvas.drawText("MENU", getWidth() / 2 - 150, 300 , paint);

                    canvas.drawText("EYES", getWidth() / 2 - 450, 500 , paint);
                    Log.d("game_", Integer.toString( gameEyes ));

                    canvas.drawText("LEVEL", getWidth() / 2 - 450, 800 , paint);
                    Log.d("game_", Integer.toString( gameLevel ));
                    canvas.drawText(Integer.toString(gameLevel) , getWidth() / 2 + 350, 800 , paint);


                    canvas.drawText("TEST", getWidth() / 2 - 450, 1100 , paint);
                    Log.d("game_", Integer.toString( gameTest ));
                    canvas.drawText("START", getWidth() / 2 - 175, getHeight() - 300 , paint);


                    startBox = new gameClickBox();
                    startBox.startX   = getWidth() / 2 - 250;
                    startBox.startY   = getHeight() - 450;
                    startBox.endX     = getWidth() / 2 +250; // Check this
                    startBox.endY     = getHeight() - 250;
                    eyesBox = new gameClickBox();
                    eyesBox.startX   = getWidth() / 2 + 250;
                    eyesBox.startY   = 400;
                    eyesBox.endX     = getWidth() / 2 +550; // Check this
                    eyesBox.endY     = 600;
                    levelBox = new gameClickBox();
                    levelBox.startX   = getWidth() / 2 + 250;
                    levelBox.startY   = 700;
                    levelBox.endX     = getWidth() / 2 +550; // Check this
                    levelBox.endY     = 900;
                    testBox = new gameClickBox();
                    testBox.startX   = getWidth() / 2 + 250;
                    testBox.startY   = 1000;
                    testBox.endX     = getWidth() / 2 +550; // Check this
                    testBox.endY     = 1200;


                    break;

                case gS_PLAYING:
                    int yOff = 1;
                    int totHeight = (int) (getHeight() / yOff);
                    int totWidth = (int) getWidth();
                    int xStart;
                    int xEnd;

                    Random randomX = new Random();
                    int randomNum = randomX.nextInt(20);

                    xStart = randomNum;
                    xEnd = xStart + 10;
                    int k = 0;
                    int prevStart = 100;

                    for (int i = 0; i < totHeight; i++) {

                        //Paint paint = new Paint();
                        if (playerInside == true)
                            paint.setColor(Color.GREEN);
                        else
                            paint.setColor(Color.RED);
                        canvas.drawCircle(450, (i * yOff), 6, paint); //left side
                        canvas.drawCircle(getWidth() - 450, (i * yOff), 6, paint); //right sid
                    }
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(100);
                    paint.setStrokeWidth(3);

                    canvas.drawText("" + totScore, getWidth() / 2 - 40, 100, paint);
                    if (totScore < 75) mediaPlayer2.start();
                    if (totScore > 140) mediaPlayer3.start();
                    else if (totScore > 75) mediaPlayer.start();

                    //Count down timer for calibration!
                    if (player.startCalibrating && !player.calibrated) {
                        paint.setTextSize(200);
                        String temp = Double.toString(currTime / 1000);
                        canvas.drawText(temp, getWidth() / 2 - 40, getHeight() / 2, paint);

                    }
                break;
                case gS_END:

                    break;
            }

        }

    }



    public void updateLeftBorder()
    {

        if(player.getScore()%50 ==0) {
            Leftborder.add(new LeftBorder( BitmapFactory.decodeResource(getResources(), R.drawable.left),0,2560));
            ;
        }

        for(int i = 0; i<Leftborder.size(); i++) {
            Leftborder.get(i).update();
        }
    }



    public void updateRightBorder()
    {

        if(player.getScore()%50 ==0)
        {
            Rightborder.add(new RightBorder(BitmapFactory.decodeResource(getResources(),R.drawable.right),1237,2560));
        }



        for(int i = 0; i<Rightborder.size(); i++) {
            Rightborder.get(i).update();
        }


    }

}