package tech.depthcore.ongoplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import tech.depthcore.ongoplayer.MediaFilesFragment;
import android.util.Log;

public class MediaFilesAdapter extends FragmentStateAdapter {

    public MediaFilesAdapter( FragmentActivity fa ) {
        super( fa );
    }

    @Override
    public Fragment createFragment( int index ) {
        MediaSource source = MediaSource.getInstanceByIndex( index );
        if( source == null ) {
            Log.e( Utils.TAG, "Error!! MediaFilesAdapter.createFragment: Invalid source index[index=" + index + "]" );
            return null;
        }

        MediaFilesFragment fragment = new MediaFilesFragment();
        Bundle bundle = new Bundle();
        bundle.putInt( MediaFilesFragment.KEY_MEDIA_SOURCE, source.sourceId + 1 );
        fragment.setArguments( bundle );
        return fragment;
    }

    @Override
    public int getItemCount() {
        return MediaSource.getSourceCount();
    }
}