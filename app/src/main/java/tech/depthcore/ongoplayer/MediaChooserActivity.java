/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.depthcore.ongoplayer;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import tech.depthcore.atphub.ATPhub;

public class MediaChooserActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener {

    private ImageButton settingsButton;

    private ViewPager2         filesViewPager;
    private MediaFilesAdapter  filesAdapter;
    private MediaSourceAdapter sourceAdapter;
    private ListView           sourceListView;
    private TPlayButton        tplayButton;
    private Timer              routineTimer;
    private AlertDialog        exitDialog;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.media_chooser_activity );

        Log.i( Utils.TAG, "OngoPlayer.onCreate: System SDK=" + Build.VERSION.SDK_INT + ", App Version=" + Utils.getAppVersion() + "..................." );

        OngoProfile.init( this );
        PlaybackHistory.init( this );

        Handler handler = new Handler( Looper.myLooper() ) {
            @Override
            public void handleMessage( @NonNull Message msg ) {
                super.handleMessage( msg );

                MediaSource source;
                switch( msg.what ) {
                    case Utils.MESSAGE_WHAT_FILE_ITEM_REPAINT:
                        source = MediaSource.getInstanceById( msg.arg1 );
                        source.triggerRepaintFileItem( msg.arg2 );
                        break;
                    case Utils.MESSAGE_WHAT_FILES_LIST_REPAINT:
                        source = MediaSource.getInstanceById( msg.arg1 );
                        source.triggerRepaintFileList( msg.arg2 );
                        break;
                    case Utils.MESSAGE_WHAT_FILES_LIST_RELOAD:
                        source = MediaSource.getInstanceById( msg.arg1 );
                        source.launchCurrentNodeLoader();
                        break;
                    case Utils.MESSAGE_WHAT_FILES_LIST_LOADING:
                        source = MediaSource.getInstanceById( msg.arg1 );
                        source.triggerDisplayFileListLoading( msg.arg2 );
                        break;
                    case Utils.MESSAGE_WHAT_LAUNCH_MEDIA_PLAYER:
                        source = MediaSource.getInstanceById( msg.arg1 );
                        source.triggerLaunchMediaPlayer( msg.arg2 );
                        break;
                }
            }
        };
        Utils.initiaized( this, handler );

        ImageButton backButton = findViewById( R.id.title_back_button );
        backButton.setLayoutParams( new LinearLayout.LayoutParams( 0, 0 ) );

        sourceAdapter = new MediaSourceAdapter( R.layout.media_chooser_source_item );
        sourceAdapter.setNotifyOnChange( false );
        sourceListView = findViewById( R.id.mediar_sources_listView );
        sourceListView.setAdapter( sourceAdapter );
        sourceListView.setOnItemClickListener( this );
        sourceListView.setFocusable( true );
        sourceListView.setFocusableInTouchMode( true );
        
        filesAdapter = new MediaFilesAdapter( this );
        filesViewPager = findViewById( R.id.media_files_viewPager );
        filesViewPager.setUserInputEnabled( false );
        filesViewPager.setAdapter( filesAdapter );
        filesViewPager.setOrientation( ORIENTATION_VERTICAL );

        tplayButton = findViewById( R.id.tplay_button );

        settingsButton = findViewById( R.id.settings_button );
        settingsButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                Intent intent = new Intent( MediaChooserActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        } );


        settingsButton.setNextFocusLeftId( sourceListView.getId() );
        settingsButton.setNextFocusRightId( tplayButton.getId() );
        tplayButton.setNextFocusLeftId( settingsButton.getId() );
        sourceListView.setNextFocusUpId( settingsButton.getId() );

        MediaSource source;
        for( int i = 0; i < MediaSource.Type_Size; i++ ) {
            source = MediaSource.getInstanceByIndex( i );
            if( source == null ) {
                source = MediaSource.createInstance( i );
            }
            sourceAdapter.addSource( source );
        }

        sourceListView.setItemChecked( 0, true );
        sourceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if( exitDialog != null ) {
            return;
        }

        if( MediaSource.returnToParentNodeOfCurrentSource() ) {
            return;
        }

        exitDialog = new AlertDialog.Builder( this )
                    .setMessage( Utils.getContext().getString( R.string.ongo_exit_confirmation ) )
                    .setCancelable( true )
                    .setPositiveButton( Utils.getContext().getString( R.string.ongo_dialog_positive_btn_text ), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton( Utils.getContext().getString( R.string.ongo_dialog_Negative_btn_text ), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            exitDialog.dismiss();
                            exitDialog = null;
                        }
                    }).setOnCancelListener( new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel( DialogInterface dialogInterface ) {
                            if( exitDialog != null ) {
                                exitDialog.dismiss();
                                exitDialog = null;
                            }
                        }
                    }).setOnDismissListener( new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss( DialogInterface dialogInterface ) {
                            if( exitDialog != null ) {
                                exitDialog = null;
                            }
                        }
                    } ).create();

        exitDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sourceListView.requestFocus();

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
            routineTimer.schedule( task, 1000, 1000 );
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

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i( Utils.TAG, "MediaChooserActivity.onActivityResult [ request=" + requestCode + ", result=" + resultCode + ", " + data + "]" );
        if( requestCode == Utils.ACTIVITY_RESULT_REQUEST_CODE_BAIDU_SSO || requestCode == Utils.ACTIVITY_RESULT_REQUEST_CODE_PLAY_POSITION ) {
            MediaSource.onActivityResultForEachSource( requestCode, resultCode, data );
        }
    }

    @Override
    public void onSaveInstanceState( Bundle inBundle ) {
        super.onSaveInstanceState( inBundle );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ////ATPHUB: make sure AndroTPlay is stopped
        tech.depthcore.atphub.ATPhub.stopService( Utils.getContext() );
        ////////////////////////////////////////////////////////////////////////

        MediaSource.clearCacheAll();

        OngoProfile.releaseAll();
    }

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int pos, long id ) {
        sourceAdapter.setSourceSelected( pos );
        filesViewPager.setCurrentItem( pos, false );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

        if( requestCode == Utils.PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE ) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i( Utils.TAG, "READ_EXTERNAL_STORAGE......passed" );
            } else {
                Log.i( Utils.TAG, "READ_EXTERNAL_STORAGE......nopass" );
            }
        }
    }

    public boolean isResumed2() {
        return routineTimer != null;
    }

    public void adjustFocuseNavigation( MediaSource source ) {
        ListView filesListView = source.getFilesListView();
        ImageButton returnButton = source.getReturnButton();

        if( filesListView == null || returnButton == null ) {
            return;
        }

        sourceListView.setNextFocusRightId( filesListView.getId() );
        settingsButton.setNextFocusDownId( filesListView.getId() );
        tplayButton.setNextFocusDownId( filesListView.getId() );

        if( returnButton.isEnabled() ) {
            filesListView.setNextFocusUpId( returnButton.getId() );
        }

        if( filesListView.getAdapter().getCount() > 0 ) {
            sourceListView.setNextFocusRightId( filesListView.getId() );
            settingsButton.setNextFocusDownId( filesListView.getId() );
            tplayButton.setNextFocusDownId( filesListView.getId() );

            filesListView.requestFocus();
        }
        else {
            returnButton.requestFocus();

            if( returnButton.isEnabled() ) {
                sourceListView.setNextFocusRightId( returnButton.getId() );
                settingsButton.setNextFocusDownId( returnButton.getId() );
                tplayButton.setNextFocusDownId( returnButton.getId() );
            }
            else {
                sourceListView.setNextFocusRightId( sourceListView.getId() );
                settingsButton.setNextFocusDownId( settingsButton.getId() );
                tplayButton.setNextFocusDownId( tplayButton.getId() );
            }
        }

        filesListView.setOnKeyListener( new View.OnKeyListener() {
            @Override
            public boolean onKey( View view, int i, KeyEvent event ) {
                if( event.getAction() == ACTION_DOWN ) {
                    if( event.getKeyCode() == KEYCODE_DPAD_UP ) {
                        if( filesListView.getSelectedItemPosition() == 0 ) {
                            if( !returnButton.isEnabled( ) ) {
                                settingsButton.requestFocus( );
                            }
                        }
                    }
                    else if( event.getKeyCode() == KEYCODE_DPAD_LEFT ) {
                        sourceListView.requestFocus();
                    }
                }
                return false;
            }
        });

        if( returnButton.isEnabled() ) {
            returnButton.setOnKeyListener( new View.OnKeyListener() {
             @Override
                public boolean onKey( View view, int i, KeyEvent event ) {
                    if( event.getAction() == ACTION_DOWN ) {
                        if( event.getKeyCode() == KEYCODE_DPAD_UP ) {
                            settingsButton.requestFocus( );
                        }
                        else if( event.getKeyCode() == KEYCODE_DPAD_LEFT ) {
                            sourceListView.requestFocus( );
                        }
                    }
                    return false;
                }
            });
        }
    }

    private class MediaSourceAdapter extends ArrayAdapter<MediaSource> {
        private int resourceId;
        
        public MediaSourceAdapter( int resourceId ) {
            super( Utils.getContext(), resourceId );
            this.resourceId = resourceId;
        }

        public void addSource( MediaSource source ) {
            this.add( source );
        }

        @Override
        public View getView( int index, View convertView, ViewGroup parent ) {
            MediaSource source = getItem( index );

            View view = convertView;
            if( view == null ) {
                view = getLayoutInflater().inflate( resourceId, parent, false );

                ImageView img = view.findViewById( R.id.media_source_imageView );
                TextView  txt = view.findViewById( R.id.media_source_textView );

                img.setImageResource( source.getSourceImageId() );
                txt.setText( source.getSourceNameId() );
            }
            
            return view;

        }

        public void setSourceSelected( int index ) {
            MediaSource source = getItem( index );
            source.selected();
        }
    }

}