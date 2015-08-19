package radiancetops.com.resistora;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by neerajen on 18/08/15.
 */
public class MarkerView extends View {
    private Paint paint;

    private float width = -1;
    private float height = -1;
    private boolean firstTime = true;
    private ArrayList<Integer> bandLocation;


    public MarkerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();

        firstTime = true;
        width = -1;
        height = -1;

        bandLocation = new ArrayList<Integer>();

    }


    public void setup() {
        //Only ran once when the view is first created
        if (!firstTime)
            return;

        firstTime = false;

        //Sets up the width and height of the gameControl on the screen
        //The gameControl is centered in the screen with a possible border around them
        width = getWidth();
        height = getHeight();

    }//initialisation of the gameboard

    public void setBandLocation (int [] bandLocation){
        this.bandLocation = new ArrayList<Integer>();
        for (int i = 0; i < bandLocation.length; i++){
            this.bandLocation.add(bandLocation[i]);
        }
        Log.v("band size",""+bandLocation.length);
        invalidate();
    }
    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        setup();
        paint.setStrokeWidth(4);
        for (int i = 0; i < bandLocation.size(); i++){
            paint.setColor(Color.BLACK);
            canvas.drawLine(bandLocation.get(i),0 , bandLocation.get(i), height, paint);
            Log.v("band location",""+bandLocation.get(i));
        }



        canvas.drawLine(0, 0, width, 0, paint);
        canvas.drawLine(0, height, width, height, paint);
        Log.v("band dims", "" + width + "  " + height);



    }
}