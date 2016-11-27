package uzmany.bmonitor;

/**
 * Created by Usman on 2016-11-01.
 */


        import android.graphics.Bitmap;
        import android.graphics.Canvas;
        import android.media.MediaPlayer;
        import android.provider.Settings;
        import android.util.Log;


/**
 * Created by David on 04/09/2016.
 */
public class Player extends GameObject{

    private Bitmap spritesheet;
    private int score;
    private long startTime;
    private double dxa;
    private boolean left;
    private boolean playing = false;
    public boolean calibrated = false;
    public boolean startCalibrating = false;
    private Animation animation = new Animation();

    public Player (Bitmap res, int w, int h, int numFrames)
    {
        x = 1440/2 -  w/2; //Need to fix this.
        y = 2560   - (2*h); //Need to fix this for screen size.
        Log.d("HX", getHeight() +" " + getWidth());
       // x = 60;
       // y = GamePanel.HEIGHT - 150;
        dx = 0;
        score = 0;
        height = h;
        width = w;

        Bitmap[] image = new Bitmap[numFrames];
        spritesheet = res;

        for (int i = 0; i < image.length; i++)
        {

            image[i] = Bitmap.createBitmap(spritesheet, i*width, 0 , width, height);
        }

        animation.setFrames(image);
        animation.setDelay(10);
        startTime = System.nanoTime();

    }

    public void setUp (boolean b)
    {
        left = b;
    }

    public void update(Point3D stable3D, double zBench)
    {
        double zMov = (double) stable3D.z;
        double zReduce = (double) zBench /3;

        zMov = (zMov -zReduce)* 150;
        long elapsed = (System.nanoTime () - startTime)/1000000;
        if (elapsed > 10)
        {
            score ++;
            startTime = System.nanoTime();
        }

        animation.update();
        if (left)
        {
            dx = (int)(dxa+=1.05);
        }

     /*   else
        {
           // dx = (int)(dxa+=1.02);

        }
*/
        if (dx >3) dx =  3;
        if (dx<-3) dx = -3;

        //x += dx*2;
        x += zMov;
        GamePanel.playerInside = true;
        GamePanel.totScore += 1;
        if ( x > (1440 - 450 - 80) )   // FIX THIS LATER
        {


            GamePanel.totScore -= 2;
            GamePanel.playerInside = false;
            x -= 20;
        }
        if ( x < (450-80) )   // FIX THIS LATER
        {
            GamePanel.totScore -= 2;
            GamePanel.playerInside = false;
            x += 20;
        }
        //x += ( 720 - x)/10;
        dx = 0;
    }

    public void draw (Canvas canvas)
    {
        canvas.drawBitmap (animation.getImage(),x,y,null);
    }

    public int getScore(){return score;}
    public boolean getPlaying(){return playing;}
    public void setPlaying(boolean b) {playing = b;}
    public void resetDYA(){dxa = 0;}
    public void resetScore () {score = 0;}

}
