package uzmany.bmonitor;

/**
 * Created by Usman on 2016-11-01.
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by David on 31/10/2016.
 */
public class LeftBorder extends GameObject{
    private Bitmap image;

    public LeftBorder(Bitmap res, int x, int y)
    {
        height = 824;
        width = 103;

        this.x = x;
        this.y = y;

        dy = GamePanel.MOVESPEED;
        image = Bitmap.createBitmap(res, 0, 0, width, height);
    }
    public void update()
    {
        y+=dy;
    }
    public void draw(Canvas canvas)
    {
        try{canvas.drawBitmap(image,x,y,null);}catch(Exception e){};
    }

}