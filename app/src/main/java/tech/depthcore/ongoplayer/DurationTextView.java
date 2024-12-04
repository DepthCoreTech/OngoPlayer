package tech.depthcore.ongoplayer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class DurationTextView extends androidx.appcompat.widget.AppCompatTextView {

    private final float PositionHeightDP     = 2.0f;
    private final float TimelinePaddingTopDP = 2.0f;

    private Paint timelinePaint;
    private int   positionPercent;
    private float timelineHeight;
    private float positionHeight;
    private float timelinePaddingTop;



    public DurationTextView( Context context ) {
        super( context );

        init( null, 0 );
    }

    public DurationTextView( Context context, AttributeSet attrs ) {
        super( context, attrs );

        init( attrs, 0 );
    }

    public DurationTextView( Context context, AttributeSet attrs, int defStyle ) {
        super( context, attrs, defStyle );

        init( attrs, defStyle );
    }

    public void setPositionPrecent( int precent ) {
        positionPercent = precent;
    }

    private void init( AttributeSet attrs, int defStyle ) {
        positionHeight     = Utils.dpToPx( PositionHeightDP );
        timelinePaddingTop = Utils.dpToPx( TimelinePaddingTopDP );
        timelineHeight     = positionHeight * 3;
        positionPercent    = 0;
        timelinePaint      = new Paint();

        timelinePaint.setStyle( Paint.Style.FILL );
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure( widthMeasureSpec, heightMeasureSpec );

    }

    @Override
    protected void onDraw( Canvas canvas ) {
        onDrawTimeline( canvas );
        super.onDraw( canvas );
    }

    private void onDrawTimeline( Canvas canvas ) {

        if( positionPercent <= 0 ) {
            return;
        }

        int baseline = getBaseline();
        float left, top, right, bottom;
        float paddingHorizontal = getWidth( ) / 3;
        float length = getWidth( ) - paddingHorizontal;

        left = paddingHorizontal / 2;
        top = baseline + timelinePaddingTop;
        right = left + length;
        bottom = top + timelineHeight;
        timelinePaint.setColor( Utils.getResourceColor( R.color.ongo_timeline_background ) );
        canvas.drawRect( left, top, right, bottom, timelinePaint );

        top = top + positionHeight;
        bottom = top + positionHeight;
        right = left + length * positionPercent / 100 + 0.5f;
        timelinePaint.setColor( Utils.getResourceColor( R.color.ongo_timeline_position ) );
        canvas.drawRect( left, top, right, bottom, timelinePaint );
    }

}