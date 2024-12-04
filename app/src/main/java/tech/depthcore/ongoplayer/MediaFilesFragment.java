package tech.depthcore.ongoplayer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.util.Log;


/**
 * A fragment representing a list of Items.
 */
public class MediaFilesFragment extends Fragment {

    public static final String KEY_MEDIA_SOURCE = "media_source_id";

    private int mediaSourceId;

    public MediaFilesFragment() {
        mediaSourceId = -1;
    }

    private boolean initByBundle( Bundle bundle ) {
        if( mediaSourceId < 0  ) {
            if( bundle == null ) {
                return false;
            }
            
            int sid = bundle.getInt( KEY_MEDIA_SOURCE );
            if( sid > 0 ) {
                MediaSource source = MediaSource.getInstanceById( sid - 1 );
                if( source != null ) {
                    mediaSourceId = source.sourceId;
                }
            }
        }
        return true;
    }

    @Override
    public void onResume( ) {
        super.onResume();
        MediaSource mediaSource = MediaSource.getInstanceById( mediaSourceId );
        mediaSource.setFragmentResumed( true );
        mediaSource.setChooserViewFocusable( true );
        mediaSource.launchCurrentNodeLoader();
    }

    @Override
    public void onPause( ) {
        super.onPause();
        MediaSource mediaSource = MediaSource.getInstanceById( mediaSourceId );
        mediaSource.setFragmentResumed( false );
        mediaSource.displayFileListLoading( false );
        mediaSource.setChooserViewFocusable( false );
    }

    @Override
    public void onCreate( Bundle inState ) {
        super.onCreate( inState );
        
        if( !initByBundle( inState ) ) {
            initByBundle( getArguments() );
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {

        if( !initByBundle( inState ) ) {
            initByBundle( getArguments() );
        }

        MediaSource mediaSource = MediaSource.getInstanceById( mediaSourceId );

        if( mediaSource == null ) {
            Log.e(Utils.TAG, "Error!! MediaFilesFragment.onCreateView: mediaSource=null" );
            return inflater.inflate(R.layout.media_chooser_fragment, container, false);
        }

        View view = inflater.inflate(R.layout.media_chooser_fragment, container, false);

        return mediaSource.createFileChooserView( view );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ){
        super.onSaveInstanceState( outState );

        outState.putInt( KEY_MEDIA_SOURCE, mediaSourceId + 1 );
    }
}