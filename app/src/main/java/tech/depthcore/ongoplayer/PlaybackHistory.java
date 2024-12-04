package tech.depthcore.ongoplayer;

import android.content.Context;
import android.util.Log;

import com.google.android.exoplayer2.C;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class PlaybackHistory {
    public static final String VERSION = "PH.001";

    private static final String HistoryFileName = new String( "ongo_history.dat" );

    private static PlaybackHistory instance = null;

    private List<HistoryRecord> historyList = new Vector<HistoryRecord>( OngoProfile.GeneralProfile.HISTORY_NUMBER_DEFAULT );
    private Context context;

    public static PlaybackHistory getInstance() {
        return instance;
    }

    public static void init( Context context ) {
        if( instance == null ) {
            instance = new PlaybackHistory( context );
        }
    }

    public static String descriptionOfDuration( long timestamp ) {
        long curtime = System.currentTimeMillis() / 1000;

        if( timestamp > curtime ) {
            return "null";
        }

        Calendar curCalendar = Calendar.getInstance();
        curCalendar.setTime( new Date( curtime * 1000 ) );

        Calendar befCalender = Calendar.getInstance();
        befCalender.setTime( new Date( timestamp * 1000 ) );

        int dtYear    = curCalendar.get( Calendar.YEAR ) - befCalender.get( Calendar.YEAR );
        int dtMonth   = curCalendar.get( Calendar.MONTH ) - befCalender.get( Calendar.MONTH );
        int dtDay     = curCalendar.get( Calendar.DAY_OF_YEAR ) - befCalender.get( Calendar.DAY_OF_YEAR );
        int dtWeek    = curCalendar.get( Calendar.WEEK_OF_YEAR ) - befCalender.get( Calendar.WEEK_OF_YEAR );
        int dtMinute  = (int)( curtime - timestamp ) / 60;
        int dtHour    = dtMinute / 60;

        if( dtMinute < 1 ) {
            return Utils.getContext().getString( R.string.ongo_history_just_now );
        }
        else if( dtMinute < 60 ) {
            return "" + dtMinute + Utils.getContext().getString( R.string.ongo_history_mintues_ago );
        }
        else if( dtHour < 6 ) {
            return "" + dtHour + Utils.getContext().getString( R.string.ongo_history_hours_ago );
        }
        else if( dtDay < 1 ) {
            return "" + dtHour + Utils.getContext().getString( R.string.ongo_history_hours_ago );
        }
        else if( dtDay == 1 ) {
            return Utils.getContext().getString( R.string.ongo_history_yesterday );
        }
        else if( dtWeek < 1 ) {
            return "" + dtDay + Utils.getContext().getString( R.string.ongo_history_days_ago );
        }
        else if( dtMonth < 1 ) {
            return "" + dtWeek + Utils.getContext().getString( R.string.ongo_history_weeks_ago );
        }
        else if( dtYear < 1 ) {
            return "" + dtMonth + Utils.getContext().getString( R.string.ongo_history_months_ago );
        }
        else if( dtYear == 1 ) {
            return dtMonth + Utils.getContext().getString( R.string.ongo_history_last_year );
        }
        else {
            return "" + dtYear + Utils.getContext().getString( R.string.ongo_history_years_ago );
        }

    }

    private PlaybackHistory( Context context ) {
        this.context = context;
        loadFromFile( );
    }

    public void pairHistoryForNode( MediaSource.FileNode node ) {

        HistoryRecord history = getHistoryByPath( node.sourceType, node.absolutePath, node.fsID );
        if( history == null ) {
            return;
        }

        history.pairNode( node );
    }

    public MediaSource.FileNode getLastViewedNodeOfParent( int sourceType, String path ) {

        for( HistoryRecord record : historyList ) {
            if( record.sourceType == sourceType && record.isParentPath( path ) ) {
                return record.nodePairing;
            }
        }

        return null;
    }

    public long getLastViewedTimeInPath( int sourceType, String path ) {

        for( HistoryRecord record : historyList ) {
            if( record.sourceType == sourceType && record.nodePath.startsWith( path ) ) {
                return record.lastViewedTime;
            }
        }

        return -1;
    }
    
    
    public HistoryRecord addLatestRecord( int type, String path, MediaSource.FileNode cache, int item, long position ) {

        int index = indexOf( type, path );

        HistoryRecord record = null;
        if( index >= 0 ) {
            record = historyList.remove( index );
        }
        else {
            record = new HistoryRecord( type, path, cache );
        }

        record.updatePosition( item, position );

        historyList.add( 0, record );

        if( historyList.size() > OngoProfile.general.getHistoryNumber() ) {
            historyList.remove( historyList.size() - 1 );
        }

        saveToFile();

        return record;
    }

    private HistoryRecord getHistoryByPath( int type, String nodePath, long fsid ) {
        int index;
        if( fsid > MediaSource.FSID_UNDEF ) {
            index = indexOf( type, fsid );
        }
        else {
            index = indexOf( type, nodePath );
        }
        if( index < 0 ) {
            return null;
        }

        return historyList.get( index );
    }

    private int indexOf( int type, String nodePath ) {
        int index = 0;
        for( HistoryRecord record : historyList ) {
            if( record.sourceType == type && record.nodePath.equals( nodePath ) ) {
                return index;
            }
            index++;
        }

        return -1;
    }

    private int indexOf( int type, long fsid ) {
        int index = 0;
        for( HistoryRecord record : historyList ) {
            if( record.sourceType == type && record.fsID == fsid ) {
                return index;
            }
            index++;
        }

        return -1;
    }

    private void saveToFile( ) {
        try {
            DataOutputStream out = new DataOutputStream( context.openFileOutput( HistoryFileName, context.MODE_PRIVATE ) );

            out.writeUTF( VERSION );
            out.writeInt( historyList.size() );
            for( HistoryRecord record : historyList ) {
                record.writeTo( out );
            }

            Log.i( Utils.TAG, "PlaybackHistory saved records: " + historyList.size() );
            out.close( );
        } catch( Exception e ) {
            Log.e( Utils.TAG, "Error!!! saveHistoryToFile failed.", e );
        }
    }

    private void loadFromFile() {
        DataInputStream in = null;
        try {
            FileInputStream fin = context.openFileInput( HistoryFileName );
            if( fin == null ) {
                return;
            }

            in = new DataInputStream( fin );

            String version = in.readUTF();
            if( !VERSION.equals( version ) ) {
                Log.w( Utils.TAG, "PlaybackHistory abort loading old version data: " + version );
                return;
            }

            int size = in.readInt();
            size = Math.min( size, OngoProfile.general.getHistoryNumber() );

            for( int i = 0; i < size; i++ ) {
                HistoryRecord record = new HistoryRecord( in );
                historyList.add( record );
            }
            Log.i( Utils.TAG, "PlaybackHistory loaded records: " + size );

        } catch( Exception e ) {
            Log.e( Utils.TAG, "Error!!! loadHistoryFromFile failed.", e );
        }
        finally {
            try {
                in.close( );
            } catch( Exception e ) {}
        }
    }

    public class HistoryRecord {
        private int    sourceType;
        private String nodePath;
        private long   fsID;
        private int    startItemIndex;
        private long   startPosition;
        private long   lastViewedTime;

        private MediaSource.FileNode nodePairing;

        private void pairNode( MediaSource.FileNode node ) {
            nodePairing = node;
            nodePairing.metadata.startItemIndex = startItemIndex;
            nodePairing.metadata.startPosition  = startPosition;
            nodePairing.lastViewedTime = lastViewedTime;

            if( node.fsID > MediaSource.FSID_UNDEF && !node.absolutePath.equals( nodePath ) ) {
                nodePath = node.absolutePath;
            }
        }

        public long getLastViewedTime() {
            return lastViewedTime;
        }

        public boolean isParentPath( String path ) {
            if( nodePairing != null && nodePairing.parent != null ) {
                return nodePairing.parent.absolutePath.equals( path );
            }

            if( nodePath.startsWith( path ) ) {
                if( nodePath.lastIndexOf( '/' ) == path.length() + 1 ) {
                    return true;
                }
            }

            return false;
        }

        public String toString() {
            return "[type=" + sourceType + ", path=" + nodePath + ", fsid=" + fsID + ", item=" + startItemIndex + ", pos=" + startPosition + "pair=" + nodePairing + "]";
        }

        public HistoryRecord( int type, String path, MediaSource.FileNode cache ) {
            sourceType = type;
            nodePath   = path;
            if( cache != null ) {
                fsID = cache.fsID;
            }
            else {
                fsID = MediaSource.FSID_UNDEF;
            }
            nodePairing = cache;

            startItemIndex = C.INDEX_UNSET;
            startPosition  = C.TIME_UNSET;

            lastViewedTime = System.currentTimeMillis() / 1000;
        }

        public void updatePosition( int item, long position ) {
            startItemIndex = item;
            startPosition  = position;
            lastViewedTime = System.currentTimeMillis() / 1000;

            if( nodePairing != null ) {
                nodePairing.metadata.startItemIndex = item;
                nodePairing.metadata.startPosition  = position;
                nodePairing.lastViewedTime = lastViewedTime;
            }
        }

        private void writeTo( DataOutputStream out ) throws IOException {
            out.writeInt ( sourceType     );
            out.writeUTF ( nodePath       );
            out.writeLong( fsID           );
            out.writeInt ( startItemIndex );
            out.writeLong( startPosition  );
            out.writeLong( lastViewedTime );
        }

        private HistoryRecord( DataInputStream in ) throws IOException {
            sourceType     = in.readInt();
            nodePath       = in.readUTF();
            fsID           = in.readLong();
            startItemIndex = in.readInt();
            startPosition  = in.readLong();
            lastViewedTime = in.readLong();
        }
    }

}
