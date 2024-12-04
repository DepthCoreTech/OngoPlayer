package tech.depthcore.ongoplayer;

import static android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS;
import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Timer;
import java.util.TimerTask;

import tech.depthcore.atphub.ATPhub;

public class SettingsActivity extends AppCompatActivity
                              implements AdapterView.OnItemClickListener {

    public final static int SUBJECT_GENERAL  = 0;
    public final static int SUBJECT_TPLAY    = 1;
    public final static int SUBJECT_SPEAKERS = 2;
    public final static int SUBJECT_NUMBER   = 3;

    private static boolean isTPlayActive = false;
    private static SettingsActivity settingsActivityInstance = null;


    private final static int subjectNameIds[] = new int[]{
            R.string.ongo_settings_general,
            R.string.ongo_settings_tplay,
            R.string.ongo_settings_speakers
    };

    private ViewPager2     cntViewPager;
    private ListView       subjectListView;
    private ContentAdapter cntAdapter;
    private TPlayButton    tplayButton;
    private ImageButton    backButton;
    private Timer          routineTimer;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        settingsActivityInstance = this;

        setContentView( R.layout.settings_activity );

        backButton = findViewById( R.id.title_back_button );
        backButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        } );

        TextView titleText = findViewById( R.id.title_name_textView );
        titleText.setText( "" );

        ImageButton settingsBtn = findViewById( R.id.settings_button );
        settingsBtn.setVisibility( View.INVISIBLE );
        settingsBtn.setFocusable( false );

        tplayButton = findViewById( R.id.tplay_button );
        tplayButton.addOnStateListener( new TPlayButton.OnStateListener( ) {
            @Override
            public void onStateChange( boolean active, int number ) {
                isTPlayActive = active;
            }
        } );

        subjectListView = findViewById( R.id.subject_listView );
        subjectListView.setAdapter( new SubjectAdapter() );
        subjectListView.setOnItemClickListener( this );

        cntAdapter = new ContentAdapter( this );
        cntViewPager = findViewById( R.id.content_viewPager );
        cntViewPager.setUserInputEnabled( false );
        cntViewPager.setAdapter( cntAdapter );
        cntViewPager.setOrientation( ORIENTATION_VERTICAL );
        cntViewPager.setDescendantFocusability( FOCUS_AFTER_DESCENDANTS );

        subjectListView.setSelection( 0 );
        subjectListView.setItemChecked( 0, true );

        backButton.setNextFocusDownId( subjectListView.getId() );
        subjectListView.setNextFocusUpId( backButton.getId() );
        subjectListView.setNextFocusDownId( subjectListView.getId() );

    }


    @Override
    public void onItemClick( AdapterView< ? > adapterView, View view, int i, long l ) {
        cntViewPager.setCurrentItem( i, false );
    }


    @Override
    public void onDestroy( ) {
        super.onDestroy( );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( routineTimer == null ) {
            routineTimer = new Timer( );
            TimerTask task = new TimerTask( ) {
                @Override
                public void run( ) {
                    try {
                        if( tplayButton.updateState( ATPhub.isServiceRunning(), ATPhub.getConnectingSpeakersNumber() ) ) {
                            runOnUiThread( new Runnable() {
                                @Override
                                public void run() {
                                    tplayButton.invalidate();
                                }
                            });
                        }
                    } catch( Exception e ) {
                        e.printStackTrace( );
                    }
                }
            };
            routineTimer.schedule( task, 3000, 3000 );
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        if( routineTimer != null ) {
            routineTimer.cancel( );
            routineTimer.purge();
            routineTimer = null;
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////

    private class SubjectAdapter extends ArrayAdapter<String> {

        public SubjectAdapter( ) {
            super( SettingsActivity.this, R.layout.settings_subject_item );

            for( int i = 0; i < SUBJECT_NUMBER; i++ ) {
                add( SettingsActivity.this.getString( subjectNameIds[ i ] ) );
            }
        }


        @Override
        public View getView( int index, View convertView, ViewGroup parent ) {

            String subjectName = getItem( index );

            View view = convertView;
            if( view == null ) {
                view = getLayoutInflater().inflate( R.layout.settings_subject_item, parent, false );

                TextView  txt = view.findViewById( R.id.subject_name_textView );

                txt.setText( subjectName );
            }

            return view;

        }

    }

    public class ContentAdapter extends FragmentStateAdapter {

        private SettingsFragment fragments[] = new SettingsFragment[ SUBJECT_NUMBER ];

        public ContentAdapter( FragmentActivity fa ) {
            super( fa );
        }

        @Override
        public Fragment createFragment( int index ) {
            switch( index ) {
                case SUBJECT_GENERAL:
                    fragments[ index ] = new SettingsGeneralFragment();
                    break;
                case SUBJECT_TPLAY:
                    fragments[ index ] = new SettingsTPlayFragment();
                    break;
                case SUBJECT_SPEAKERS:
                    fragments[ index ] = new SettingsSpeakersFragment();
                    break;
            }
            return fragments[ index ];
        }

        @Override
        public int getItemCount() {
            return SUBJECT_NUMBER;
        }

    }

    static public abstract class SettingsFragment extends Fragment {

        public SettingsFragment() {
        }

        @Override
        public void onResume( ) {
            super.onResume( );

            adjustFocuseNavigation( settingsActivityInstance.subjectListView, settingsActivityInstance.backButton, settingsActivityInstance.tplayButton );
        }

        public void adjustFocuseNavigation( View subjectList, View backBtn, View tplayBtn ) {
        }

        protected boolean isTPlayActive() {
            return isTPlayActive;
        }



        protected void setFocusable( View view ) {
        }

        public abstract int getSubjectId();
    }

}