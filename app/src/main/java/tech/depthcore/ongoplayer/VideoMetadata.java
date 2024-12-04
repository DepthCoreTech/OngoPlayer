package tech.depthcore.ongoplayer;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;

public class VideoMetadata {
    public static final String PLAYER_ACTION_VIEW = "tech.depthcore.ongoplayer.action.VIEW";

    public Bitmap thumbnail;
    public long   duration;
    public long   fileSize;
    public int    audioTracks;
    public String mimeType;
    public String thumbUrl;
    public Uri    uri;
    public String title;
    public int    startItemIndex;
    public long   startPosition;

    public VideoMetadata() {
        thumbnail      = null;
        duration       = 0L;
        fileSize       = 0L;
        audioTracks    = 0;
        mimeType       = null;
        thumbUrl       = null;
        uri            = null;
        title          = null;
        startItemIndex = C.INDEX_UNSET;
        startPosition  = C.TIME_UNSET;
    }

    @Override
    protected void finalize() throws Throwable {
        release();
    }

    @Override
    public String toString() { 
        return "duration=" + duration + ", size=" + fileSize + ", mime=" + mimeType
                + ", thumbUrl=" + thumbUrl + ", title=" + title + ", url=" + uri;
    }

    public int getPositionPrecent() {
        int precent = 0;
        if( startItemIndex > C.INDEX_UNSET && startPosition > C.TIME_UNSET && duration > 0 ) {
            float pos = ( float )startPosition / duration;
            if( pos > 1 ) {
                pos = 1;
            }
            precent = ( int )( pos * 100 + 0.5f );
        }
        return precent;
    }

    public void release() {
        if( thumbnail != null ) {
            thumbnail.recycle();
            thumbnail = null;
        }
    }


}
