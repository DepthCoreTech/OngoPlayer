package tech.depthcore.ongoplayer;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import tech.depthcore.atphub.ATPhub;

public class TPlayButton extends View implements View.OnClickListener{

    private static final float ORIG_IMAGE_HEIGHT = 54f;
    private static final float ORIG_TEXT_AREA_X  = 112f;
    private static final float ORIG_TEXT_AREA_Y  = 28f;
    private static final float ORIG_TEXT_AREA_W  = 48f;
    private static final float ORIG_TEXT_AREA_H  = 22f;

    private boolean isActive;
    private int activeNumber;

    private Paint paint;
    private Bitmap onFocusedImage, offFocusedImage, onNormalImage, offNormalImage;
    private int imgWidth, imgHeight;
    private Rect imageArea;
    private RectF textArea, viewArea;
    private float imageScale;
    private int onColor, offColor;

    private OnStateListener stateListener = null;

    public TPlayButton( Context context ) {
        super( context );
    }

    public TPlayButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,-1);
    }

    public TPlayButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        activeNumber = 0;
        isActive = false;

        paint = new Paint( Paint.ANTI_ALIAS_FLAG );

        onColor  = Utils.getResourceColor( context, R.color.atp_tplay_text_on );
        offColor = Utils.getResourceColor( context, R.color.atp_tplay_text_off );

        Drawable drawable = Utils.getResourceDrawable( context, R.drawable.atp_tplay_on_normal );
        onNormalImage = ( ( BitmapDrawable ) drawable ).getBitmap( );

        drawable = Utils.getResourceDrawable( context, R.drawable.atp_tplay_on_focused );
        onFocusedImage = ( ( BitmapDrawable ) drawable ).getBitmap( );

        drawable = Utils.getResourceDrawable( context, R.drawable.atp_tplay_off_normal );
        offNormalImage = ( ( BitmapDrawable ) drawable ).getBitmap( );

        drawable = Utils.getResourceDrawable( context, R.drawable.atp_tplay_off_focused );
        offFocusedImage = ( ( BitmapDrawable ) drawable ).getBitmap( );

        imgWidth = offFocusedImage.getWidth();
        imgHeight = offFocusedImage.getHeight();

        imageScale = (float)imgHeight / ORIG_IMAGE_HEIGHT;

        imageArea = new Rect( 0, 0, imgWidth, imgHeight );

        textArea  = new RectF( 0, 0, 0, 0 );
        viewArea  = new RectF( 0, 0, 0, 0 );

        setOnClickListener( this );
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        float scale = 1.0f;
        int viewWidth, viewHeight;
        float textSize;

        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        if( viewHeight < imgHeight ) {
            scale = (float)viewHeight / imgHeight;
            viewWidth = (int)( (float)imgWidth * scale );
        }
        else {
            viewWidth  = imgWidth;
            viewHeight = imgHeight;
        }

        scale = scale * imageScale;

        textArea.left   = ORIG_TEXT_AREA_X * scale;
        textArea.top    = ORIG_TEXT_AREA_Y * scale;
        textArea.right  = ( ORIG_TEXT_AREA_X + ORIG_TEXT_AREA_W ) * scale;
        textArea.bottom = ( ORIG_TEXT_AREA_Y + ORIG_TEXT_AREA_H ) * scale;

        textSize = ORIG_TEXT_AREA_H * scale - 1;
        paint.setTypeface( Typeface.create( Typeface.SANS_SERIF, Typeface.BOLD ) );
        paint.setTextSize( textSize  );

        viewArea.right = viewWidth;
        viewArea.bottom = viewHeight;
        setMeasuredDimension( viewWidth, viewHeight );

    }

    @Override
    protected void onDraw( Canvas canvas ) {
        super.onDraw(canvas);

        Bitmap img;
        String text;

        if( isFocused() ) {
            img = isActive ? onFocusedImage : offFocusedImage;
        }
        else {
            img = isActive ? onNormalImage : offNormalImage;
        }
        canvas.drawBitmap( img, imageArea, viewArea, null );

        if( isActive ) {
            text = activeNumber > 0 ? "" + activeNumber : "ON";
            paint.setColor( onColor );
        }
        else {
            text = "OFF";
            paint.setColor( offColor );
        }

        float textWidth = paint.measureText( text );

        canvas.drawText( text,
                textArea.left + ( textArea.right - textArea.left - textWidth ) / 2,
                textArea.top + ( textArea.bottom - textArea.top - paint.descent() - paint.ascent() ) / 2,
                paint );

    }

    public boolean updateState( boolean active, int number ) {
        boolean result = false;

        if( this.isActive != active ) {
            this.isActive = active;
            result = true;
        }

        if( this.activeNumber != number ) {
            this.activeNumber = number;
            result = true;
        }

        if( result && stateListener != null ) {
            stateListener.onStateChange( active, number );
        }
        return result;
    }

    public boolean isActive() {
        return isActive;
    }

    public void addOnStateListener( OnStateListener listener ) {
        stateListener = listener;
    }

    public interface OnStateListener {
        public void onStateChange( boolean active, int number );
    }

    @Override
    public void onClick( View v ) {
        AnimatorSet set = new AnimatorSet();
        set.play( ObjectAnimator.ofFloat( v, "alpha", 1f, 0.5f ) )
                .before(  ObjectAnimator.ofFloat( v, "alpha", 0.5f, 1f ) );
        set.setDuration( 200 );
        set.start();

        if( !ATPhub.isServiceRunning() ) {
            Log.i( Utils.TAG, "MediaChooserActivity: tplayButton clicked: ATPhub.startService" );

            startTPLAY();
        }
        else {
            Log.i( Utils.TAG, "MediaChooserActivity: tplayButton clicked: ATPhub.stopService" );

            stopTPLAY();
        }

    }


    static void startTPLAY() {
        ////ATPHUB: start AndroTPlay:
        tech.depthcore.atphub.ATPhub.startService( Utils.getContext(),
                                                   OngoProfile.tplay.getDevice2CHS(),
                                                   OngoProfile.tplay.getDevice6CHS(),
                                                   OngoProfile.tplay.getExternal4SPK() );
    }

    static void stopTPLAY() {
        ////ATPHUB: stop AndroTPlay:
        tech.depthcore.atphub.ATPhub.stopService( Utils.getContext() );
    }


    static void restartTPLAY() {
        new Thread() {
            public void run() {
                stopTPLAY();

                do {
                    try {
                        Thread.sleep( 200 );
                    } catch( InterruptedException e ) {}
                } while( tech.depthcore.atphub.ATPhub.isServiceRunning() );

                startTPLAY();

            }
        }.start();
    }

}
