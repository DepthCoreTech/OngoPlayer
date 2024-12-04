
package tech.depthcore.ongoplayer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.C;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.RepeatModeUtil;
import com.google.android.exoplayer2.util.Util;


public class PlayerActivity extends AppCompatActivity 
                            implements OnClickListener, StyledPlayerView.ControllerVisibilityListener {

    protected static final String PREFER_EXTENSION_DECODERS_EXTRA = "prefer_extension_decoders";

    protected StyledPlayerView playerView;
    protected @Nullable ExoPlayer player;

    private MediaItem playingMediaItem;
    private Tracks    lastSeenTracks;
    private boolean   startAutoPlay;
    private int       startItemIndex;
    private long      startPosition;
    private int       sourceType;
    private String    nodePath;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView();

        playingMediaItem = null;
        playerView = findViewById(R.id.player_view);

        playerView.setShowRewindButton( false );
        playerView.setShowFastForwardButton( false );
        playerView.setShowNextButton( false );
        playerView.setShowPreviousButton( false );
        playerView.setShowShuffleButton( false );
        playerView.setRepeatToggleModes( RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE );

        playerView.setControllerVisibilityListener(this);
        playerView.setErrorMessageProvider(new PlayerErrorMessageProvider());
        playerView.requestFocus();

        if (savedInstanceState != null) {
            restoreStartPosition( savedInstanceState );
        } else {
            resetStartPosition( getIntent() );
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();
        resetStartPosition( intent );
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (playerView != null) {
            playerView.onPause();
        }
        releasePlayer();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, String[] permissions, int[] grantResults ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer();
        } else {
            showToast( R.string.storage_permission_denied );
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateStartPosition();
        outState.putBoolean( Utils.KEY_PLAY_AUTO_PLAY, startAutoPlay );
        outState.putInt    ( Utils.KEY_PLAY_ITEM_INDEX, startItemIndex );
        outState.putLong   ( Utils.KEY_PLAY_POSITION, startPosition );
    }

    
    @Override
    public boolean dispatchKeyEvent( KeyEvent event ) {
        // See whether the player view wants to handle media or DPAD keys events.
        return playerView.dispatchKeyEvent( event ) || super.dispatchKeyEvent( event );
    }


    @Override
    public void onClick(View view) {
    }

    @Override
    public void onBackPressed() {
        if( player != null ) {
            updateStartPosition();
            setActivityResult();
        }

        super.onBackPressed();
    }

    @Override
    public void onVisibilityChanged( int visibility ) {

    }


    protected void setContentView() {
        setContentView( R.layout.player_activity );
    }

    /**
     * @return Whether initialization was successful.
     */
    protected boolean initializePlayer() {
        if( player != null ) {
            return true;
        }
        
        Intent intent = getIntent();

        nodePath = intent.getStringExtra( Utils.KEY_PLAY_NODE_PATH );
        sourceType = intent.getIntExtra( Utils.KEY_PLAY_SOURCE_TYPE, -1 );
        String mime = intent.getStringExtra( Utils.KEY_PLAY_MIME_TYPE );
        String userAgent = intent.getStringExtra( Utils.KEY_PLAY_USER_AGENT );

        playingMediaItem = loadMediaItemFrom( intent );
        if( playingMediaItem == null ) {
            return false;
        }

        lastSeenTracks = Tracks.EMPTY;
        ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder( this );
        setRenderersFactory( playerBuilder, intent.getBooleanExtra( PREFER_EXTENSION_DECODERS_EXTRA, false ) );
        player = playerBuilder.build();
        player.addListener( new PlayerEventListener() );
        player.addAnalyticsListener( new EventLogger() );
        player.setAudioAttributes( AudioAttributes.DEFAULT, /* handleAudioFocus= */ true );
        player.setPlayWhenReady( startAutoPlay );
        playerView.setPlayer( player );


        ////ATPHUB:bind player
        Log.i( tech.depthcore.atphub.ATPhub.TAG, "initializePlayer.bind...." );
        tech.depthcore.atphub.ATPhub.setExoPlayer( player );
        ////////////////////////////////////////////////////////////////////////////

        boolean haveStartPosition = startItemIndex != C.INDEX_UNSET;
        if( haveStartPosition ) {
            player.seekTo( startItemIndex, startPosition );
        }

        if( MimeTypes.APPLICATION_M3U8.equals( mime ) ) {
            if( userAgent == null ) {
                userAgent = Util.getUserAgent( this, getString( R.string.application_name ) );
            }
            Log.i( Utils.TAG, "initializePlayer BAIDU_M3U8 mode: [mode=" + mime + ", userAgent=" + userAgent + "]" );

            DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent( userAgent );
            HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory( dataSourceFactory ).createMediaSource( playingMediaItem );
            player.setMediaSource( hlsMediaSource, !haveStartPosition );
        }
        else {
            Log.i( Utils.TAG, "initializePlayer generic mode: " + mime );
            player.setMediaItem( playingMediaItem, !haveStartPosition );
        }

        player.prepare();

        return true;
    }

    private MediaItem loadMediaItemFrom( Intent intent ) {
        if( !VideoMetadata.PLAYER_ACTION_VIEW.equals( intent.getAction() ) ) {
            return null;
        }

        Uri uri = Uri.parse( intent.getStringExtra( Utils.KEY_PLAY_URI ) );
        String mime = intent.getStringExtra( Utils.KEY_PLAY_MIME_TYPE );

        MediaItem.Builder builder = new MediaItem.Builder()
                                        .setUri( uri )
                                        .setMediaMetadata( new MediaMetadata.Builder().setTitle( intent.getStringExtra( Utils.KEY_PLAY_TITLE ) ).build());

        if( mime != null ) {
            builder.setMimeType( mime );
        }

        return builder.build();
    }


    private void setRenderersFactory( ExoPlayer.Builder playerBuilder, boolean preferExtensionDecoders ) {
        RenderersFactory renderersFactory = Utils.buildRenderersFactory( /* context= */ this, preferExtensionDecoders );
        playerBuilder.setRenderersFactory( renderersFactory );
    }

    protected void releasePlayer() {
        if( player != null ) {
            ////ATPHUB:unbind player
            Log.i( tech.depthcore.atphub.ATPhub.TAG, "releasePlayer.unbind...." );
            tech.depthcore.atphub.ATPhub.setExoPlayer( null );
            ///////////////////

            updateStartPosition();
            player.release();
            player = null;
            playerView.setPlayer( /* player= */ null );
        }
    }

    private void setActivityResult( ) {
        Intent intent = new Intent(  );
        intent.putExtra( Utils.KEY_PLAY_SOURCE_TYPE, sourceType );
        intent.putExtra( Utils.KEY_PLAY_NODE_PATH, nodePath );

        int result = 0;
        if( startItemIndex != C.INDEX_UNSET ) {
            intent.putExtra( Utils.KEY_PLAY_ITEM_INDEX, startItemIndex );
            intent.putExtra( Utils.KEY_PLAY_POSITION,   startPosition  );
            result = 1;
        }

        setResult( result, intent );
    }

    private void updateStartPosition() {
        if( player != null ) {
            startAutoPlay  = player.getPlayWhenReady();
            startItemIndex = player.getCurrentMediaItemIndex();
            startPosition  = Math.max( 0, player.getContentPosition() );
        }
    }

    private void resetStartPosition( Intent intent ) {
        startAutoPlay = true;

        startItemIndex = intent.getIntExtra ( Utils.KEY_PLAY_ITEM_INDEX, C.INDEX_UNSET );
        startPosition  = intent.getLongExtra( Utils.KEY_PLAY_POSITION,   C.TIME_UNSET );
        Log.i( Utils.TAG, "PlayerActivity resetStartPosition [autoplay=" + startAutoPlay + ", item=" + startItemIndex + ", position=" + startPosition + "]" );
    }

    private void restoreStartPosition( Bundle bundle ) {
        startAutoPlay  = bundle.getBoolean( Utils.KEY_PLAY_AUTO_PLAY );
        startItemIndex = bundle.getInt    ( Utils.KEY_PLAY_ITEM_INDEX );
        startPosition  = bundle.getLong   ( Utils.KEY_PLAY_POSITION );
        Log.i( Utils.TAG, "PlayerActivity restoreStartPosition [autoplay=" + startAutoPlay + ", item=" + startItemIndex + ", position=" + startPosition + "]" );
    }

    private void showControls() {
    }

    private void showToast(int messageId) {
        showToast( getString(messageId) );
    }

    private void showToast(String message) {
        Toast.makeText( getApplicationContext(), message, Toast.LENGTH_LONG ).show();
    }

    private class PlayerEventListener implements Player.Listener {
        @Override
        public void onPlaybackStateChanged( @Player.State int playbackState ) {
            if( playbackState == Player.STATE_ENDED ) {
                showControls();
            }
        }

        @Override
        public void onPlayerError( PlaybackException error ) {
            if( error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ) {
                player.seekToDefaultPosition();
                player.prepare();
            } else {
                showControls();
            }
        }

        @Override
        @SuppressWarnings( "ReferenceEquality" )
        public void onTracksChanged( Tracks tracks ) {
            if( tracks == lastSeenTracks ) {
             return;
            }
            if( tracks.containsType( C.TRACK_TYPE_VIDEO ) && !tracks.isTypeSupported( C.TRACK_TYPE_VIDEO, /* allowExceedsCapabilities= */ true ) ) {
                showToast( R.string.error_unsupported_video );
            }
            if( tracks.containsType( C.TRACK_TYPE_AUDIO ) && !tracks.isTypeSupported( C.TRACK_TYPE_AUDIO, /* allowExceedsCapabilities= */ true ) ) {
                showToast( R.string.error_unsupported_audio );
            }
            lastSeenTracks = tracks;
        }

        ////ATPHUB: pause, resume and rebuffering
        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            if( !playWhenReady ) {
                tech.depthcore.atphub.ATPhub.pausePipe();
            }
            else {
                tech.depthcore.atphub.ATPhub.resumePipe();
            }
        }
        ////////////////////////////////////////////////////////////////////////////////
    }

    private class PlayerErrorMessageProvider implements ErrorMessageProvider<PlaybackException> {
        @Override
        public Pair<Integer, String> getErrorMessage( PlaybackException e ) {
            String errorString = getString( R.string.error_generic );
            Throwable cause = e.getCause();
            if( cause instanceof DecoderInitializationException ) {
                // Special case for decoder initialization failures.
                DecoderInitializationException decoderInitializationException = ( DecoderInitializationException )cause;
                if( decoderInitializationException.codecInfo == null ) {
                    if( decoderInitializationException.getCause() instanceof DecoderQueryException ) {
                        errorString = getString(R.string.error_querying_decoders);
                    } else if( decoderInitializationException.secureDecoderRequired ) {
                        errorString = getString( R.string.error_no_secure_decoder, decoderInitializationException.mimeType );
                    } else {
                        errorString = getString( R.string.error_no_decoder, decoderInitializationException.mimeType );
                    }
                } else {
                    errorString = getString( R.string.error_instantiating_decoder, decoderInitializationException.codecInfo.name );
                }
            }
            return Pair.create( 0, errorString );
        }
    }
}
