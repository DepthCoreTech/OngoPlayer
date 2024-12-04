package tech.depthcore.ongoplayer;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Comparator;

public abstract class MediaSource implements AdapterView.OnItemClickListener {

    public static final int FSID_UNDEF  = -1;

    public static final int Type_APK    = 0;
    public static final int Type_Device = 1;
    public static final int Type_Size   = 2;

    public static final int[] MediaSource_Image_Ids = {
            R.drawable.ongo_source_apk,
            R.drawable.ongo_source_device
    };
    public static final int[] MediaSource_Name_Ids = {
            R.string.ongo_media_source_name_apk,
            R.string.ongo_media_source_name_devices,
    };

    private static ArrayList<MediaSource> sources = new ArrayList<>();
    private static int selectedSourceId     = 0;
    private static int sourceSequneceNumber = 0;
    private static int nodeSequneceNumber   = 0;

    protected final int sourceId;
    protected int       sourceType;
    protected FileNode  rootNode;
    protected boolean   isOnlyLoadNodesWhenDisplaying;
    protected boolean   isFragmentResumed = false;
    private FileNode    currentNode;
    private View        fragmentView;


    protected abstract int loadChildrenOfNode( FileNode node );
    protected abstract int loadVideoMetadataOfNode( FileNode node ) throws Utils.ErrorException;

    private synchronized static final int nextSourceSequenceNumber() {
        return sourceSequneceNumber++;
    }

    private synchronized static final int nextNodeSequenceNumber() {
        return nodeSequneceNumber++;
    }

    public static void clearCacheAll() {
        for( MediaSource source : sources ) {
            source.clearCache();
        }
        nodeSequneceNumber = 0;
    }

    public void clearCache() {
        if( rootNode != null ) {
            rootNode.release();
            rootNode = null;
        }

        this.rootNode = new FileNode(  FileNode.TYPE_FOLDER,
                "/" + Utils.getContext().getString( MediaSource_Name_Ids[ sourceType ] ),
                "/",
                null,
                sourceType, 
                FSID_UNDEF );

        this.currentNode = rootNode;

        this.fragmentView = null;

        this.isFragmentResumed = false;
    }

    public static final MediaSource createInstance( int type ) {
        MediaSource src;
        if( type == MediaSource.Type_APK ) {
            src = new MediaSourceAPK( );
        }
        else if( type == MediaSource.Type_Device ) {
            src = new MediaSourceDevice( );
        }
        else {
            throw new RuntimeException( "Undefined source type: " + type );
        }

        sources.add( src );
        return src;
    }

    public static final MediaSource getInstanceByIndex( int index ) {
        try {
            return sources.get( index );
        }
        catch( Exception e ) {
            return null;
        }

    }

    public static final MediaSource getInstanceById( int id ) {
        for( MediaSource source : sources ) {
            if( source.sourceId == id ) {
                return source;
            }
        }
        return null;
    }

    public ListView getFilesListView( ) {
        if( fragmentView != null ) {
            return fragmentView.findViewById( R.id.files_dirs_listView );
        }
        return null;
    }

    public ImageButton getReturnButton( ) {
        if( fragmentView != null ) {
            return fragmentView.findViewById( R.id.path_back_button );
        }
        return null;
    }

    private static void setSourceSelected( int id ) {
        selectedSourceId = id;
    }

    public static void onActivityResultForEachSource( int requestCode, int resultCode, Intent data ) {
        for( MediaSource source : sources ) {
            source.onActivityResult( requestCode, resultCode, data );
        }
    }

    public static int getSourceCount() {
        return sources.size();
    }

    protected MediaSource( int type ) {
        this.sourceId     = nextSourceSequenceNumber();
        this.sourceType   = type;
        this.rootNode     = new FileNode(  FileNode.TYPE_FOLDER,
                                            "/" + Utils.getContext().getString( MediaSource_Name_Ids[ type ] ),
                                            "/",
                                            null,
                                            sourceType,
                                            FSID_UNDEF );
        this.currentNode  = rootNode;
        this.fragmentView = null;

        this.isFragmentResumed = false;
        this.isOnlyLoadNodesWhenDisplaying = false;

    }

    public void triggerRepaintFileItem( int nodeId ) {
        if( currentNode != null ) {
            FileNode node = currentNode.getNodeById( nodeId );
            if( node != null ) {
                node.repaintItemView();
            }
        }
    }

    public void triggerRepaintFileList( int nodeId ) {
        displayFileListLoading( false );
        currentNode.sortChildrenInListView();
        if( isFragmentResumed ) {
            setChooserViewFocusable( isSelected( ) );
        }
    }

    public void triggerLaunchMediaPlayer( int nodeId ) {
        if( !isFragmentResumed ) {
            return;
        }

        Utils.getActivity().runOnUiThread( new Runnable() {
            @Override
            public void run() {
                FileNode node = getNodeById( nodeId );
                if( node != null ) {
                    launchMoviePlayer(node);
                }
                else {
                    Log.e( Utils.TAG, "Error!! triggerLaunchMediaPlayer: node is null [source=" + sourceId + ", node=" + nodeId + "]");
                }
            }
        });        
    }

    public void triggerDisplayFileListLoading( int loading ) {
        if( !isFragmentResumed ) {
            return;
        }

        displayFileListLoading( loading == 1 );
    }

    protected int prepareMovieNodeForPlaying( FileNode node ) {
        return 0;
    }

    protected void savePlaybackHistory( Intent data ) {
        int stype = data.getIntExtra( Utils.KEY_PLAY_SOURCE_TYPE, -1 );
        if( stype != this.sourceType ) {
            return;
        }

        String nodePath = data.getStringExtra( Utils.KEY_PLAY_NODE_PATH );
        FileNode node = rootNode.getNodeByPath( nodePath );

        if( node == null ) {
            Log.w( Utils.TAG, " WARNING!!! Fail to save playback history, node not found: [sourceType=" + stype + ", path='" + nodePath + "']" );
            return;
        }

        int item = data.getIntExtra( Utils.KEY_PLAY_ITEM_INDEX, -1 );
        long position = data.getLongExtra( Utils.KEY_PLAY_POSITION, 0L );

        if( item >= 0 ) {
            PlaybackHistory.HistoryRecord record = PlaybackHistory.getInstance().addLatestRecord( sourceType, nodePath, node, item, position );
            node = currentNode;
            while( node != null  ) {
                node.lastViewedTime = record.getLastViewedTime();
                node = node.parent;
            }
        }
    }

    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if( requestCode == Utils.ACTIVITY_RESULT_REQUEST_CODE_PLAY_POSITION && resultCode >= 1 ) {
            savePlaybackHistory( data );
        }
    }

    public boolean isReady() {
        return true;
    }

    public void setFragmentResumed( boolean resumed ) {
        isFragmentResumed = resumed;
    }

    public void launchCurrentNodeLoader() {

        if( isOnlyLoadNodesWhenDisplaying && !isFragmentResumed ) {
            return;
        }

        if( currentNode != null ) {
            if( !currentNode.launchFileNodeLoader() ) {
                currentNode.sortChildrenInListView();
            }
        }

    }


    public String toString() {
        return "MediaSource[" + sourceType + ", " + sourceId + "]";
    }


    public void selected() {
        setSourceSelected( sourceId );
    }



    public void setChooserViewFocusable( boolean enable ) {
        if( fragmentView == null ) {
            return;
        }
        
        ListView listView = getFilesListView();
        if( listView != null ) {
            listView.setFocusable( enable );
        }

        ImageButton button = fragmentView.findViewById( R.id.path_back_button );
        if( button != null ) {
            button.setFocusable( enable );
        }
        
        if( enable ) {
            ( ( MediaChooserActivity )Utils.getContext() ).adjustFocuseNavigation( this );
        }
    }

    public boolean isSelected() {
        return selectedSourceId == sourceId;
    }

    public int getSourceImageId() {
        return MediaSource_Image_Ids[ sourceType ];
    }

    public int getSourceNameId() {
        return MediaSource_Name_Ids[ sourceType ];
    }

    public void displayFileListLoading( boolean inLoading ) {
        ListView listView = fragmentView.findViewById( R.id.files_dirs_listView );
        ProgressBar progressBar = fragmentView.findViewById( R.id.waiting_processBar );
        TextView informText = fragmentView.findViewById( R.id.waiting_info_textView );

        if( inLoading ) {
            if( listView.getVisibility() == INVISIBLE ) {
                return;
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, 0 );
            listView.setLayoutParams( params );
            listView.setVisibility( INVISIBLE );

            informText.setText( R.string.ongo_information_loading );
            progressBar.setVisibility( VISIBLE );
            informText.setVisibility( VISIBLE );
        }
        else {
            if( progressBar.getVisibility() == INVISIBLE ) {
                return;
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT );
            listView.setLayoutParams( params );
            listView.setVisibility( VISIBLE );

            progressBar.setVisibility( INVISIBLE );
            informText.setVisibility( INVISIBLE );
        }
    }

    private void createListViewOfCurrentNode() {
        if( fragmentView == null ) {
            Log.e( Utils.TAG, "Error!! createListViewOfCurrentNode: fragmentView is null" );
            return;
        }

        ListView listView = fragmentView.findViewById( R.id.files_dirs_listView );

        listView.setAdapter( currentNode.children );
        listView.setOnItemClickListener( this );
        
        listView.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected( AdapterView< ? > adapterView, View view, int i, long l ) {
                currentNode.selectedChildIndex = i;
            }

            @Override
            public void onNothingSelected( AdapterView<?> parent ) {
            }
        });

        launchCurrentNodeLoader();
    } 

    public FileNode getNodeById( int nodeId ) {
        return rootNode.getNodeById( nodeId );
    }

    public static boolean returnToParentNodeOfCurrentSource() {
        MediaSource source = getInstanceById( selectedSourceId );
        if( source != null && source.currentNode != null && source.currentNode.parent != null ) {
            source.setCurrentNodeAndCreateChooserView( source.currentNode.parent );
            return true;
        }
        return false;
    }

    public View createFileChooserView ( View view ) {
        
        fragmentView = view;

        ImageButton backBtn = fragmentView.findViewById( R.id.path_back_button );
        backBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                setCurrentNodeAndCreateChooserView( currentNode.parent );
            }
        } );

        setCurrentNodeAndCreateChooserView( null );

        return view;
    }

    protected void setCurrentNodeAndCreateChooserView( FileNode node ) {
        if( node != null ) {
            currentNode = node;
        }

        TextView txt = fragmentView.findViewById( R.id.cur_path_name_textView );
        txt.setText( currentNode.getFullName() );

        ImageButton backBtn = fragmentView.findViewById( R.id.path_back_button );
        if( currentNode.parent != null ) {
            backBtn.setEnabled( true );
            backBtn.setVisibility( VISIBLE );
        }
        else {
            backBtn.setEnabled( false );
            backBtn.setVisibility( INVISIBLE );
        }

        createListViewOfCurrentNode();

    }

    protected int setIntentForPlayer( String nodePath, VideoMetadata metadata, Intent intent ) {

        intent.setAction( VideoMetadata.PLAYER_ACTION_VIEW );

        intent.putExtra( Utils.KEY_PLAY_SOURCE_TYPE, sourceId );
        intent.putExtra( Utils.KEY_PLAY_NODE_PATH,   nodePath );

        if( metadata.uri != null ) {
            intent.putExtra( Utils.KEY_PLAY_URI, metadata.uri.toString() );
        } else {
            return -1;
        }

        if( metadata.mimeType != null ) {
            intent.putExtra( Utils.KEY_PLAY_MIME_TYPE, metadata.mimeType );
        }

        if( metadata.title != null ) {
            intent.putExtra( Utils.KEY_PLAY_TITLE, metadata.title );
        } else {
            return -2;
        }

        if( metadata.startItemIndex != C.INDEX_UNSET ) {
            intent.putExtra( Utils.KEY_PLAY_ITEM_INDEX, metadata.startItemIndex );
        }

        if( metadata.startPosition != C.TIME_UNSET ) {
            intent.putExtra( Utils.KEY_PLAY_POSITION, metadata.startPosition );
        }

        return Utils.ERROR_NONE;
    }

    private void launchMoviePlayer( FileNode node ) {

        int rst = 0;
        Intent intent = new Intent( Utils.getContext(), PlayerActivity.class );
        if( ( rst = setIntentForPlayer( node.absolutePath, node.metadata, intent ) ) < Utils.ERROR_NONE ) {
            Log.e( Utils.TAG, "Error!! launchMoviePlayer setIntentForPlayer: " + rst );
            return;
        }

        Utils.getActivity().startActivityForResult( intent, Utils.ACTIVITY_RESULT_REQUEST_CODE_PLAY_POSITION );
    }


    @Override  //AdapterView.OnItemClickListener
    public void onItemClick( AdapterView<?> parent, View view, int pos, long id ) {
        
        FileNode node = currentNode.children.getItem( pos );
        if( node.type == FileNode.TYPE_FILE ) {
            if( prepareMovieNodeForPlaying( node ) == 0 ) {
                launchMoviePlayer( node );
            }
        }
        else {
            setCurrentNodeAndCreateChooserView( node );
        }
    }

    public class FileNode {
        public static final int TYPE_FOLDER = 0;
        public static final int TYPE_FILE   = 1;
        public static final int TYPE_DEVICE = 2; //only for MediaSourceDevice
        public static final int TYPE_USB    = 3; //only for MediaSourceDevice

        protected final int        id;
        protected final int        type;
        protected final String     name;
        protected final String     absolutePath;
        protected int              sourceType;
        protected FileNode         parent;
        protected FileNodesAdapter children;
        protected VideoMetadata    metadata;
        protected FileNodeLoader   nodeLoader;
        protected boolean          needToRepaint;
        protected int              selectedChildIndex;
        protected long             fsID;
        protected long             lastViewedTime;
        protected View             itemView;


        public FileNode( int type, String name, String path, FileNode parent, int sType, long fsid ) {
            this.id             = nextNodeSequenceNumber();
            this.type           = type;
            this.name           = name;
            this.absolutePath   = path;
            this.parent         = parent;
            this.sourceType     = sType;
            this.children       = new FileNodesAdapter( );
            this.nodeLoader     = null;
            this.needToRepaint  = true;
            this.selectedChildIndex = -1;
            this.fsID           = fsid;
            this.lastViewedTime = -1L;
            this.itemView       = null;

            if( type == TYPE_FILE ) {
                metadata = new VideoMetadata();
                metadata.title = name;
                metadata.uri = Uri.parse( absolutePath );
                int ctype = Util.inferContentType( metadata.uri );
                metadata.mimeType = Util.getAdaptiveMimeTypeForContentType( ctype );

                PlaybackHistory.getInstance().pairHistoryForNode( this );
            }
            else {
                if( parent != null ) {
                    lastViewedTime = PlaybackHistory.getInstance( ).getLastViewedTimeInPath( sourceType, absolutePath );
                }
                metadata = null;
            }

            this.children.setNotifyOnChange( false );
        }

        public void release() {

            if( nodeLoader != null ) {
                nodeLoader.release();
                nodeLoader = null;
            }

            if( children != null ) {
                children.release();
                children = null;
            }

            if( metadata != null ) {
                metadata.release();
                metadata = null;
            }

            itemView = null;
            parent   = null;
        }

        protected void repaintItemView() {
            if( itemView != null ) {
                updateItemView( itemView );
                itemView.invalidate();
            }
        }

        protected View updateItemView( View nodeView ) {

            itemView = nodeView;

            TextView txt = nodeView.findViewById( R.id.file_name_textView );
            txt.setText( name );

            DurationTextView txtDuration = nodeView.findViewById( R.id.file_duration_textView );
            TextView txtSize = nodeView.findViewById( R.id.file_size_textView );
            txtDuration.setText( "" );
            txtSize.setText( "" );
            txtDuration.setPositionPrecent( 0 );

            ImageView img = nodeView.findViewById( R.id.file_thumbnail_imageView );
            if( type == FileNode.TYPE_FOLDER ) {
                img.setImageResource( R.drawable.ongo_path_folder );
                if( lastViewedTime > 0 ) {
                    String desc = PlaybackHistory.descriptionOfDuration( lastViewedTime );
                    txtSize.setTextColor( Utils.getResourceColor( R.color.ongo_timeline_position ) );
                    txtSize.setText( desc );
                    txtDuration.setTextColor( Utils.getResourceColor( R.color.ongo_timeline_position ) );
                    txtDuration.setText( Utils.getContext().getString( R.string.ongo_history_viewed ) );
                }
            }
            else if( type == FileNode.TYPE_DEVICE ) {
                img.setImageResource( R.drawable.ongo_storage_device );
            }
            else if( type == FileNode.TYPE_USB ) {
                img.setImageResource( R.drawable.ongo_storage_usb );
            }
            else {
                if( metadata != null ) {
                    txtDuration.setTextColor( Utils.getResourceColor( R.color.ongo_media_files_listitem_duration ) );
                    txtDuration.setText( Utils.formetDuration( metadata.duration ) );
                    txtSize.setTextColor( Utils.getResourceColor( R.color.ongo_media_files_listitem_filesize ) );
                    txtSize.setText( Utils.formetFileSize( metadata.fileSize ) );
                    txtDuration.setPositionPrecent( metadata.getPositionPrecent() );
                }

                if( metadata != null && metadata.thumbnail != null ) {
                    img.setImageBitmap( metadata.thumbnail );
                }
                else {
                    img.setImageResource( R.drawable.ongo_loading );
                }
            }

            return itemView;
        }

        protected void sortChildrenInListView() {
            if( children.getCount() <= 0 ) {
                return;
            }

            FileNode selectedNode = null;
            if( selectedChildIndex >= 0 ) {
                selectedNode = children.getItem( selectedChildIndex );
            }

            children.sort();

            if( selectedNode == null ) {
                selectedNode = children.lastViewedItem;
            }

            int index = children.getPosition( selectedNode );
            if( index < 0 ) {
                index = 0;
            }

            children.setSelectionByIndex( index );

            children.notifyDataSetChanged();
        }

        protected boolean launchFileNodeLoader() {
            if( nodeLoader == null || nodeLoader.needToReload() ) {
                displayFileListLoading( true );
                nodeLoader = new FileNodeLoader( this );
                nodeLoader.start();
                return true;
            }
            return false;
        }

        public String getFullName() {
            if( parent != null ) {
                return parent.getFullName() + "/" + name;
            }
            return name;
        }

        @Override
        public boolean equals( Object obj ) {
            return ( obj instanceof FileNode ) && isSameAs( ( FileNode )obj );
        }

        public boolean isSameAs( FileNode node ) {
            if( node == null ) {
                return false;
            }

            return node.id == id;
        }

        public int compareTo( FileNode node ) {
            if( this.type == node.type ) {
                if( this.type == TYPE_FOLDER ) {
                    if( this.lastViewedTime > node.lastViewedTime ) {
                        return -2;
                    }
                    else if( this.lastViewedTime < node.lastViewedTime ) {
                        return 2;
                    }
                }
                return this.name.compareTo( node.name );
            }

            if( this.type == TYPE_FOLDER ) {
                return -1;
            }
            else {
                return 1;
            }
        }

        public FileNode getNodeById( int nodeId ) {
            if( this.type == FileNode.TYPE_FILE ) {
                return null;
            }
            return children.getNodeById( nodeId );
        }

        public FileNode getNodeByPath( String nodePath ) {
            if( this.absolutePath.equals( nodePath ) ) {
                return this;
            }
            return children.getNodeByPath( nodePath );
        }

        public String toString( ) {
            return "FileNode[ id=" + id + ", name='" + name + "', absolutePath='" + absolutePath + "', selectedChildIndex=" + selectedChildIndex + " ]";
        }
    }

    public class FileNodesAdapter extends ArrayAdapter<FileNode> {
        private FileNodeComparator compareor;
        private FileNode lastViewedItem = null;

        public FileNodesAdapter( ) {
            super( Utils.getContext(), R.layout.media_chooser_file_item );
            compareor = new FileNodeComparator();
        }

        public void release() {
            for( int i = 0; i < getCount(); i++ ) {
                FileNode node = getItem( i );
                node.release();
            }
            clear();
        }

        public void setSelectionByIndex( int index ) {
            ListView listView = fragmentView.findViewById( R.id.files_dirs_listView );

            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection( index );
                    View v = listView.getChildAt( index );
                    if (v != null) {
                        v.requestFocus();
                    }
                }
            });
        }

        @Override
        public View getView(int index, View nodeView, ViewGroup parent ) {
            FileNode node = getItem( index );

            if( nodeView == null ) {
                nodeView = LayoutInflater.from( Utils.getContext() ).inflate( R.layout.media_chooser_file_item, parent, false );
            }

            return node.updateItemView( nodeView );
        }

        public FileNode getNodeById( int nodeId ) {

            for( int i = 0; i < getCount(); i++ ) {
                FileNode node = getItem( i );
                if( node.id == nodeId ) {
                    return node;
                }
                FileNode target = node.getNodeById( nodeId );
                if( target != null ) {
                    return target;
                }
            }
            return null;
        }

        public FileNode getNodeByPath( String nodePath ) {
            for( int i = 0; i < getCount(); i++ ) {
                FileNode node = getItem( i );
                FileNode target = node.getNodeByPath( nodePath );
                if( target != null ) {
                    return target;
                }
            }
            return null;
        }

        public void sort() {
            lastViewedItem = null;
            sort( compareor );
        }

        private void searchLastViewedItem( FileNode node ) {
            if( node.lastViewedTime > 0 ) {
                if( lastViewedItem == null ) {
                    lastViewedItem = node;
                }
                else if( node.lastViewedTime > lastViewedItem.lastViewedTime ) {
                    lastViewedItem = node;
                }
            }
        }

        private final class FileNodeComparator implements Comparator<FileNode> {
            @Override
            public int compare( FileNode node1, FileNode node2 ) {
                searchLastViewedItem( node1 );
                searchLastViewedItem( node2 );
                return node1.compareTo( node2 );
            }
        }
    }


    private class FileNodeLoader extends Thread {

        private static final int STATUS_IDLE    = 0;
        private static final int STATUS_WORKING = 1;
        private static final int STATUS_DONE    = 2;
        private static final int STATUS_FAILED  = -1;

        private FileNode loadingNode;
        private int status;
        private boolean needToInterrupt = false;

        public FileNodeLoader( FileNode node ) {
            loadingNode = node;
            status = FileNodeLoader.STATUS_IDLE;
        }

        public boolean isDone() {
            return status == STATUS_DONE;
        }

        public boolean needToReload() {
            return status == FileNodeLoader.STATUS_FAILED;
        }

        public void release() {
            needToInterrupt = true;
        }

        @Override
        public void run() {
            if( status == FileNodeLoader.STATUS_WORKING ) {
                Log.w( Utils.TAG, "WARNING!!! FileNodeLoader is already work: source=" + sourceId );
            }
            status = FileNodeLoader.STATUS_WORKING;

            try {
                Thread.sleep(200);
            }catch( Exception e ){};

            if( needToInterrupt ) return;

            try {
                if( !isReady() ) {
                    status = FileNodeLoader.STATUS_FAILED;
                    return;
                }

                if( needToInterrupt ) return;

                int rst;
                if( ( rst = loadChildrenOfNode( loadingNode ) ) <= Utils.ERROR_GENERIC ) {
                    status = FileNodeLoader.STATUS_FAILED;
                    Log.e( Utils.TAG, "Error!! FileNodeLoader.loadChildrenOfNode(" +  loadingNode.name + "): " + rst );
                    return;
                }

                if( needToInterrupt ) return;

                if( rst > Utils.ERROR_NONE ) {
                    Utils.submit_MESSAGE_WHAT_FILES_LIST_REPAINT( sourceId, loadingNode.id );
                }

                FileNode node;
                for( int i = 0; i < loadingNode.children.getCount(); i++ ) {
                    if( needToInterrupt ) return;

                    node = loadingNode.children.getItem(i);
                    
                    if( node.type == FileNode.TYPE_FILE ) {
                        try {
                            rst = loadVideoMetadataOfNode(node);
                            if( rst > Utils.ERROR_NONE ) {
                                Utils.submit_MESSAGE_WHAT_FILE_ITEM_REPAINT( sourceId, node.id );
                            }
                        }catch( Utils.ErrorException fe ) {
                            fe.printStackTrace();
                            Log.e( Utils.TAG, "Error!!  FileNodeLoader.loadVideoMetadataOfNode(" + i + ":" +  node.id + " : " + node.absolutePath + "): " + fe.toString() );
                            status = FileNodeLoader.STATUS_FAILED;
                            break;
                        }
                    }
                }

                if( status == FileNodeLoader.STATUS_WORKING ) {
                    status = FileNodeLoader.STATUS_DONE;
                }
            }
            catch( Exception e ) {
            }
            finally {
                loadingNode = null;
            }
        }
    }

}
