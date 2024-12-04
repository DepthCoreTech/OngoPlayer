package tech.depthcore.ongoplayer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.Lifecycle;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.RenderersFactory;

public class Utils  {
    public static final String TAG = "[OnGo]";
    public static final int Video_Thumbnail_Width = 144;

    public static final int PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE = 100;

    public static final int ACTIVITY_RESULT_REQUEST_CODE_BAIDU_SSO     = 1001;
    public static final int ACTIVITY_RESULT_REQUEST_CODE_PLAY_POSITION = 4001;


    public static final String KEY_PLAY_URI         = "play_uri";
    public static final String KEY_PLAY_MIME_TYPE   = "play_mime_type";
    public static final String KEY_PLAY_TITLE       = "play_title";
    public static final String KEY_PLAY_USER_AGENT  = "play_user_agent";
    public static final String KEY_PLAY_ITEM_INDEX  = "play_item_index";
    public static final String KEY_PLAY_POSITION    = "play_position";
    public static final String KEY_PLAY_AUTO_PLAY   = "play_auto_play";
    public static final String KEY_PLAY_SOURCE_TYPE = "play_source_type";
    public static final String KEY_PLAY_NODE_PATH   = "play_node_path";

    public static final int ERROR_NONE    = 0;
    public static final int ERROR_GENERIC = -1;
    public static final int ERROR_FATAL   = -9;

    public static final int MESSAGE_WHAT_TESTING   = 2000;
    public static final int MESSAGE_WHAT_FILES_LIST_REPAINT  = 1001;
    public static final int MESSAGE_WHAT_FILES_LIST_RELOAD   = 1002;
    public static final int MESSAGE_WHAT_FILES_LIST_LOADING  = 1003;
    public static final int MESSAGE_WHAT_LAUNCH_MEDIA_PLAYER = 1004;
    public static final int MESSAGE_WHAT_FILE_ITEM_REPAINT   = 1005;



    private static Utils internalInstance = null;

    private MediaChooserActivity mainActivity;
    private Context mainContext;
    private Handler mainHandler;

    public static void initiaized( Context context, Handler mainHandler ) {
        internalInstance = new Utils();
        internalInstance.mainContext = context;
        internalInstance.mainHandler = mainHandler;
        internalInstance.mainActivity = ( MediaChooserActivity )context;
    }

    public static Context getContext() {
        instance_validation();
        return internalInstance.mainContext;
    }

    public static Activity getActivity() {
        instance_validation();
        return ( Activity )internalInstance.mainActivity;
    }

    public static Handler getHandler() {
        instance_validation();
        return internalInstance.mainHandler;
    }

    public static int getResourceColor( Context context, int id ) {
        return ResourcesCompat.getColor( context.getResources(), id, null );
    }

    public static Drawable getResourceDrawable( Context context, int id ) {
        return ResourcesCompat.getDrawable( context.getResources(), id, null );
    }

    public static int getResourceColor( int id ) {
        return getResourceColor( getContext(), id );
    }

    public static Drawable getResourceDrawable( int id ) {
        return getResourceDrawable( getContext(), id );
    }

    public static void submit_MESSAGE_WHAT_FILE_ITEM_REPAINT( int sourceId, int nodeId ) {
        Log.i( Utils.TAG, "send MESSAGE_WHAT_FILE_ITEM_REPAINT [source=" + sourceId + ", node=" + nodeId + "]" );
        Message msg = new Message();
        msg.arg1 = sourceId;
        msg.arg2 = nodeId;
        msg.what = Utils.MESSAGE_WHAT_FILE_ITEM_REPAINT;
        Utils.getHandler().sendMessage( msg );
    }

    public static void submit_MESSAGE_WHAT_FILES_LIST_REPAINT( int sourceId, int nodeId ) {
        Log.i( Utils.TAG, "send MESSAGE_WHAT_FILES_LIST_REPAINT [source=" + sourceId + ", node=" + nodeId + "]" );
        Message msg = new Message();
        msg.arg1 = sourceId;
        msg.arg2 = nodeId;
        msg.what = Utils.MESSAGE_WHAT_FILES_LIST_REPAINT;
        Utils.getHandler().sendMessage( msg );
    }

    public static void submit_MESSAGE_WHAT_FILES_LIST_LOADING( int sourceId, boolean isLoading ) {
        Log.i( Utils.TAG, "send MESSAGE_WHAT_FILES_LIST_LOADING [source=" + sourceId + ", isLoading=" + isLoading + "]" );
        Message msg = new Message();
        msg.arg1 = sourceId;
        msg.arg2 = isLoading ? 1 : 0;
        msg.what = Utils.MESSAGE_WHAT_FILES_LIST_LOADING;
        Utils.getHandler().sendMessage( msg );
    }

    public static void submit_MESSAGE_WHAT_LAUNCH_MEDIA_PLAYER( int sourceId, int nodeId ) {
        Log.i( Utils.TAG, "send MESSAGE_WHAT_LAUNCH_MEDIA_PLAYER [source=" + sourceId + ", node=" + nodeId + "]" );
        Message msg = new Message();
        msg.arg1 = sourceId;
        msg.arg2 = nodeId;
        msg.what = Utils.MESSAGE_WHAT_LAUNCH_MEDIA_PLAYER;
        Utils.getHandler().sendMessage( msg );
    }

    public static void submit_MESSAGE_WHAT_FILES_LIST_RELOAD( int sourceId ) {
        Log.i( Utils.TAG, "send MESSAGE_WHAT_FILES_LIST_RELOAD [source=" + sourceId + "]" );
        Message msg = new Message();
        msg.arg1 = sourceId;
        msg.what = Utils.MESSAGE_WHAT_FILES_LIST_RELOAD;
        Utils.getHandler().sendMessage( msg );
    }

    private static void instance_validation() {
        if( internalInstance == null ) {
            throw new RuntimeException( "!!!Utils is not initialized!!!" );
        }
    }

    public static float dpToPxFloat( float dp ) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    public static float pxToDpFloat( float px ) {
        return px / getContext().getResources().getDisplayMetrics().density;
    }

    public static int dpToPx( float dp ) {
        return Math.round( dpToPxFloat( dp ) );
    }

    public static int pxToDp( float px ) {
        return Math.round( pxToDpFloat( px ) );
    }


    private static Bitmap scaleBitmap(Bitmap bitmap, int scaledSize ) {

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if( w >= h ) {
            h = h * scaledSize / w;
            w = scaledSize;
        }
        else {
            w = w * scaledSize / h;
            h = scaledSize;
        }

        Bitmap bmp = Bitmap.createScaledBitmap( bitmap, w, h, true );

        return bmp;
    }

    private static VideoMetadata retriveVideoMetadata( MediaMetadataRetriever retriever, VideoMetadata metadata ) {
        String result;
        boolean isValid = false;
        try {
            Bitmap bitmap = retriever.getFrameAtTime(1000 * 1000L, MediaMetadataRetriever.OPTION_CLOSEST);
            if (bitmap != null) {
                metadata.thumbnail = scaleBitmap( bitmap, Utils.Video_Thumbnail_Width );
                isValid = true;
                bitmap.recycle();
                bitmap = null;
            }
        }catch( Exception e ) {
            Log.e( Utils.TAG, "Error!! retriveVideoMetadata.thumbnail: " + e.toString() );
            e.printStackTrace();
        }

        try {
            result = retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION );
            metadata.duration = Long.valueOf( result );
            isValid = true;
        }catch( Exception e ) {
            Log.e( Utils.TAG, "Error!! retriveVideoMetadata.duration: " + e.toString() );
            e.printStackTrace();
        }

        try {
            metadata.mimeType = retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_MIMETYPE );
            isValid = true;
        }catch( Exception e ) {
            Log.e( Utils.TAG, "Error!! retriveVideoMetadata.mimeType: " + e.toString() );
            e.printStackTrace();
        }

        if( isValid ) {
            return metadata;
        }

        return null;
    }

    public static int retriveVideoMetadataFromAssetFile( String assetFilePath, VideoMetadata metadata ) {
        instance_validation();

        AssetFileDescriptor afd = null;
        MediaMetadataRetriever retriever = null;

        try {
            afd = internalInstance.mainContext.getAssets().openFd(assetFilePath);
            metadata.fileSize = afd.getLength();

            retriever = new MediaMetadataRetriever();
            retriever.setDataSource( afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength() );

            retriveVideoMetadata( retriever, metadata );
        }
        catch( Exception e ) {
            Log.e( Utils.TAG, "Error!! retriveVideoMetadataFromAssetFile(" + assetFilePath + "): " + e.toString() );
            e.printStackTrace();
            return -1;
        }
        finally {
            try {
                afd.close();
            } catch( Exception e1 ) {
            };

            try {
                retriever.release();
            } catch( Exception e1 ) {
            };
        }

        return 0;
    }

    public static int retriveVideoMetadataFromDeviceFile( String deviceFilePath, VideoMetadata metadata ) {
        instance_validation();

        MediaMetadataRetriever retriever = null;

        try {
            File file = new File(deviceFilePath);
            metadata.fileSize = file.length();

            retriever = new MediaMetadataRetriever();
            retriever.setDataSource( deviceFilePath );

            retriveVideoMetadata( retriever, metadata );

        } catch( Exception e ) {
            Log.e( Utils.TAG, "Error!! retriveVideoMetadataFromDeviceFile(" + deviceFilePath + "): " + e.toString() );
            e.printStackTrace();
            return -1;
        }

        try {
            retriever.release();
        } catch( Exception ee ){};

        return 0;
    }

    public static String formetDuration( long duration ) {
        DecimalFormat df = new DecimalFormat("00");

        duration = duration / 1000;

        if( duration <= 0 ) {
            return "--:--";
        }

        long min = duration / 60;
        long sec = duration % 60;
        long hour = min / 60;
        min = min % 60;

        if( hour > 0 ) {
            return df.format( (int)hour ) + ":" + df.format( (int)min ) + ":" + df.format( (int)sec );
        }
        else {
            return df.format( (int)min ) + ":" + df.format( (int)sec );
        }

    }

    public static String formetFileSize( long fileSize ) {
        double result;
        if( fileSize < 0 ) {
            return "0B";
        } else if( fileSize < 1024 ) {
            return "" + fileSize + "B";
        } else if( fileSize < 1048576 ) {
            result = (double)fileSize / 1024 + 0.5;
            return "" + (int)result + "KB";
        } else if( fileSize < 1073741824 ) {
            result = (double)fileSize / 1048576 + 0.5;
            return "" + (int)result + "MB";
        } else {
            result = (double)fileSize / 1073741824 + 0.5;
            return "" + (int)result + "GB";
        }
    }

    public class ErrorException extends Exception {
        public final int errorCode;
        public ErrorException( String message ) {
            super( message );
            errorCode = 0;
        }

        public ErrorException( int error, String message ) {
            super( message );
            errorCode = error;
        }
    }

    /** Returns whether extension renderers should be used. */
    public static boolean useExtensionRenderers() {
        return BuildConfig.USE_DECODER_EXTENSIONS;
    }

    public static RenderersFactory buildRenderersFactory( Context playerContext, boolean preferExtensionRenderer ) {
        @DefaultRenderersFactory.ExtensionRendererMode
        int extensionRendererMode = useExtensionRenderers() ? ( preferExtensionRenderer ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                                                                                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON )
                                                            : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        return new DefaultRenderersFactory( playerContext.getApplicationContext() ).setExtensionRendererMode( extensionRendererMode );
    }


    public static String getSystemVersion() {
        int version = Build.VERSION.SDK_INT;
        return "Android" + version;
    }

    public static String getSystemName() {
        return "Linux/Android";
    }

    public static String getDeviceName() {
        return Build.DEVICE;
    }

    public static String getAppName() {
        return getContext().getString( R.string.application_name );
    }

    public static String getAppVersion() {
        return "" + OngoVersion.major + "." + OngoVersion.minor + "." + OngoVersion.build;
    }

}
