package uzmany.bmonitor;

/**
 * Created by Usman on 2016-11-01.
 */

import android.graphics.Canvas;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * Created by David on 06/08/2016.
 */
public class MainThread extends Thread
{
    private int FPS = 30;
    private double averageFPS;
    private SurfaceHolder surfaceHolder;
    private GamePanel gamePanel;
    private boolean running;
    public static Canvas canvas;
    BLEservice myServiceBinder;

    public MainThread (SurfaceHolder surfaceHolder, GamePanel gamePanel, BLEservice getServiceBinder )
    {
        super();
        this.myServiceBinder = getServiceBinder;
        this.surfaceHolder = surfaceHolder;
        this.gamePanel = gamePanel;
    }

    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;
        long totalTime = 0;
        int frameCount = 0;
        //target Time in millseconds
        long targetTime = 1000 / FPS;
        boolean justStarted = true;
        long currStartGameTime =0 ;// = System.currentTimeMillis();

        while (running) {
           if (gamePanel.justStarted) {
                currStartGameTime = System.currentTimeMillis();
                gamePanel.justStarted  = false;
                justStarted = false;
           }
            if (!justStarted) {
                long currGameTime = System.currentTimeMillis();
                if ((currGameTime - currStartGameTime) > 10000)
                    break;
            }

                Log.d("GamePanel:", "Receiving ServiceBineder X" +PlayActivity.getPointPlay().x + "    Y"  +  PlayActivity.getPointPlay().y + "   Z" + PlayActivity.getPointPlay().z);

            startTime = System.nanoTime();
            canvas = null;
            //try locking the canvas for pixel editing
            try {
                canvas = this.surfaceHolder.lockCanvas();
                synchronized (surfaceHolder)

                {
                    this.gamePanel.update(PlayActivity.getPointPlay());
                    this.gamePanel.draw(canvas);

                }
            } catch (Exception e) {
            }

            finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            timeMillis = (System.nanoTime() - startTime) / 1000000;
            waitTime = targetTime - timeMillis;
            try {
                this.sleep(waitTime);
            } catch (Exception e) {
            }

            totalTime += System.nanoTime() - startTime;
            frameCount++;
            if (frameCount == FPS) {
                averageFPS = 1000 / ((totalTime / frameCount) / 1000000);
                frameCount = 0;
                totalTime = 0;
                System.out.print(averageFPS);

            }


        }
        justStarted = true;

    }
    public void setRunning (boolean setrun)
    {
        running = setrun;
    }



}

