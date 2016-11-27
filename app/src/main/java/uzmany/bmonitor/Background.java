package uzmany.bmonitor;

/**
 * Created by Usman on 2016-11-01.
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by David on 06/08/2016.
 */
public class Background {

    private Bitmap image;
    private int x, y,deltay;

    public Background (Bitmap res)
    {
        image = res;
        deltay = GamePanel.MOVESPEED;
    }

    public void update ()
    {
        y+= deltay;
        if (y < -GamePanel.HEIGHT)
        {
            y = 0;
        }
    }
    public void draw (Canvas canvas)
    {

        canvas.drawBitmap(image,x,y,null);
        if (y < 0)
        {
            canvas.drawBitmap(image,x, y+ GamePanel.HEIGHT, null);

        }
    }



}
