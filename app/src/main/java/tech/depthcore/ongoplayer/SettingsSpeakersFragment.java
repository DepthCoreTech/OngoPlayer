package tech.depthcore.ongoplayer;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.View.NO_ID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tech.depthcore.atphub.ATPhub;


public class SettingsSpeakersFragment extends SettingsActivity.SettingsFragment implements AdapterView.OnItemClickListener {

    public final SpeakerItem Empty_Speaker_Item = new SpeakerItem( "Empty", false );
    public final int[][] Position_Image_Ids = {
            { R.drawable.atp_speaker_l,  R.drawable.atp_speaker_l_selected  },
            { R.drawable.atp_speaker_r,  R.drawable.atp_speaker_r_selected  },
            { R.drawable.atp_speaker_sl, R.drawable.atp_speaker_sl_selected },
            { R.drawable.atp_speaker_sr, R.drawable.atp_speaker_sr_selected }
    };

    private SpeakerLayout  speakerLayout;
    private SpeakerAdapter speakerListAdapter;
    private ListView       speakerListView;
    private TextView       speakerArrayInfoText;
    private Timer          syncSpeakerTimer = null;
    private String         cachePath;
    private View           fragmentView;


    public SettingsSpeakersFragment() {
    }

    public int getSubjectId() {
        return SettingsActivity.SUBJECT_SPEAKERS;
    }

    private boolean initByBundle( Bundle bundle ) {
        if( bundle == null ) {
            return false;
        }

        return true;
    }

    @Override
    public void adjustFocuseNavigation( View subjectList, View backBtn, View tplayBtn ) {

        subjectList.setNextFocusRightId( speakerLayout.getSpeakerButton( SpeakerLayout.ID_SPEAKER_L ).getId() );
        tplayBtn.setNextFocusDownId( speakerListView.getId() );

        speakerListView.setNextFocusLeftId( speakerLayout.getSpeakerButton( SpeakerLayout.ID_SPEAKER_R  ).getId() );
        speakerListView.setNextFocusDownId( speakerListView.getId() );
        speakerListView.setNextFocusRightId( speakerListView.getId() );

        speakerLayout.getSpeakerButton( SpeakerLayout.ID_SPEAKER_L ).setOnKeyListener( new View.OnKeyListener() {
            @Override
            public boolean onKey( View view, int i, KeyEvent event ) {
                if( event.getAction() == ACTION_DOWN && event.getKeyCode() == KEYCODE_DPAD_UP ) {
                    backBtn.requestFocus();
                }
                return false;
            }
        });

        speakerLayout.getSpeakerButton( SpeakerLayout.ID_SPEAKER_R ).setOnKeyListener( new View.OnKeyListener() {
            @Override
            public boolean onKey( View view, int i, KeyEvent event ) {
                if( event.getAction() == ACTION_DOWN && event.getKeyCode() == KEYCODE_DPAD_UP ) {
                    backBtn.requestFocus();
                }
                return false;
            }
        });

        speakerListView.setOnKeyListener( new View.OnKeyListener() {
            @Override
            public boolean onKey( View view, int i, KeyEvent event ) {
                if( event.getAction() == ACTION_DOWN && event.getKeyCode() == KEYCODE_DPAD_UP ) {
                    if( speakerListView.getSelectedItemPosition() == 0 ) {
                        tplayBtn.requestFocus();
                    }
                }
                return false;
            }
        });
    }

    private boolean checkTPlayState() {
        if( !isTPlayActive() ) {
            speakerArrayInfoText.setText( R.string.atp_tplay_inactive_alert );
            speakerArrayInfoText.setTextColor( Utils.getResourceColor( R.color.atp_alert_info ) );
            return false;
        }
        return true;
    }

    @Override
    public void onResume( ) {
        super.onResume();
        fragmentView.requestFocus();

        String msg = getString( R.string.atp_tplay_getting_start );
        speakerArrayInfoText.setText( Html.fromHtml( msg ) );
        speakerArrayInfoText.setTextColor( Utils.getResourceColor( R.color.atp_guide_info ) );
    }

    @Override
    public void onPause( ) {
        super.onPause();
    }

    @Override
    public void onCreate( Bundle inState ) {
        super.onCreate( inState );

        if( !initByBundle( inState ) ) {
            initByBundle( getArguments() );
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if( syncSpeakerTimer != null ) {
            ATPhub.switchOffSpeakerPositioningMode();

            syncSpeakerTimer.cancel();
            syncSpeakerTimer.purge();
            syncSpeakerTimer = null;

            String spkL  = speakerLayout.getPositioningSpeakerName( SpeakerLayout.ID_SPEAKER_L  );
            String spkR  = speakerLayout.getPositioningSpeakerName( SpeakerLayout.ID_SPEAKER_R  );
            String spkSL = speakerLayout.getPositioningSpeakerName( SpeakerLayout.ID_SPEAKER_SL );
            String spkSR = speakerLayout.getPositioningSpeakerName( SpeakerLayout.ID_SPEAKER_SR );

            ATPhub.setSpeakerPositions( spkL, spkR, spkSL, spkSR );

            ATPhub.saveSpeakerProfileToFile( Utils.getContext() );
        }


    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle inState ) {

        if( !initByBundle( inState ) ) {
            initByBundle( getArguments() );
        }

        fragmentView = inflater.inflate( R.layout.settings_speakers_fragment, container, false );
        setFocusable( fragmentView );

        float density = Utils.getContext().getResources().getDisplayMetrics().density;
        if( density >= 2.5f ) {
            View view = fragmentView.findViewById( R.id.atp_speaker_l_layout );
            ConstraintLayout.LayoutParams layoutParams = ( ConstraintLayout.LayoutParams )view.getLayoutParams();
            layoutParams.circleRadius = Utils.dpToPx( 100 );
            view.setLayoutParams( layoutParams );

            view = fragmentView.findViewById( R.id.atp_speaker_r_layout );
            layoutParams = ( ConstraintLayout.LayoutParams )view.getLayoutParams();
            layoutParams.circleRadius = Utils.dpToPx( 100 );
            view.setLayoutParams( layoutParams );

            view = fragmentView.findViewById( R.id.atp_speaker_sl_layout );
            layoutParams = ( ConstraintLayout.LayoutParams )view.getLayoutParams();
            layoutParams.circleRadius = Utils.dpToPx( 100 );
            view.setLayoutParams( layoutParams );

            view = fragmentView.findViewById( R.id.atp_speaker_sr_layout );
            layoutParams = ( ConstraintLayout.LayoutParams )view.getLayoutParams();
            layoutParams.circleRadius = Utils.dpToPx( 100 );
            view.setLayoutParams( layoutParams );

            view = fragmentView.findViewById( R.id.atp_audience_imageView );
            layoutParams = ( ConstraintLayout.LayoutParams )view.getLayoutParams();
            layoutParams.verticalBias = 0.6f;
            view.setLayoutParams( layoutParams );
        }


        speakerLayout = new SpeakerLayout(
                fragmentView.findViewById( R.id.atp_speaker_l_layout ),  fragmentView.findViewById( R.id.atp_speaker_l_imageBtn ),  fragmentView.findViewById( R.id.atp_speaker_l_title  ),
                fragmentView.findViewById( R.id.atp_speaker_r_layout ),  fragmentView.findViewById( R.id.atp_speaker_r_imageBtn ),  fragmentView.findViewById( R.id.atp_speaker_r_title  ),
                fragmentView.findViewById( R.id.atp_speaker_sl_layout ), fragmentView.findViewById( R.id.atp_speaker_sl_imageBtn ), fragmentView.findViewById( R.id.atp_speaker_sl_title ),
                fragmentView.findViewById( R.id.atp_speaker_sr_layout ), fragmentView.findViewById( R.id.atp_speaker_sr_imageBtn ), fragmentView.findViewById( R.id.atp_speaker_sr_title )  );

        ImageView audienceView = fragmentView.findViewById( R.id.atp_audience_imageView );
        audienceView.setImageResource( R.drawable.atp_audience );

        speakerArrayInfoText = fragmentView.findViewById( R.id.atp_speaker_array_info );

        speakerListAdapter = new SpeakerAdapter( Utils.getContext(), R.layout.speaker_list_item );
        speakerListAdapter.setNotifyOnChange( false );
        speakerListAdapter.add( Empty_Speaker_Item ); //default

        List<ATPhub.SpeakerProfile> speakerProfileList = new ArrayList<>();
        ATPhub.reloadSpeakerProfileList( speakerProfileList );
        
        for( ATPhub.SpeakerProfile profile : speakerProfileList ) {
            SpeakerItem item = new SpeakerItem( profile.speakerName, profile.speakerStatus == 1 );
            speakerListAdapter.add( item );
            SpeakerPosition position = speakerLayout.getPosition( profile.positionId );
            if( position != null ) {
                position.bindWith( item );
            }
        }
        speakerListAdapter.sort();

        speakerListView = fragmentView.findViewById( R.id.atp_speaker_listView );
        speakerListView.setAdapter( speakerListAdapter );
        speakerListView.setOnItemClickListener( this );

        if( syncSpeakerTimer != null ) {
            syncSpeakerTimer.cancel();
            syncSpeakerTimer.purge();
        }

        syncSpeakerTimer = new Timer();
        syncSpeakerTimer.schedule( new TimerTask() {
            @Override
            public void run() {
                try {
                    List<ATPhub.SpeakerProfile> speakerProfileList = new ArrayList<>();
                    ATPhub.reloadSpeakerProfileList( speakerProfileList );
                    speakerListAdapter.setNotifyOnChange( false );
                    if( speakerListAdapter.updateSpeakerProfile( speakerProfileList ) ) {
                        Utils.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                 speakerListAdapter.sort();
                                 speakerListAdapter.notifyDataSetChanged( );
                                 speakerLayout.updateView();
                            }
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 3000, 3000 );

        cachePath = Utils.getActivity().getFilesDir().getAbsolutePath();

        ATPhub.switchOnSpeakerPositioningMode();


        return fragmentView;
    }

    @Override
    public void onSaveInstanceState( Bundle outState ){
        super.onSaveInstanceState( outState );
    }

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int pos, long id ) {
        if( !checkTPlayState() ) {
            return;
        }

        SpeakerItem item = speakerListAdapter.getItem( pos );
        SpeakerPosition position = speakerLayout.getEditingPosition();
        if( position != null ) {
            position.bindWith( item );
            speakerListAdapter.notifyDataSetChanged();
            String msg;
            if( !item.isEmptyItem() ) {
                msg = getString( R.string.atp_tplay_assigned_speaker, item.name, position.name );
                speakerArrayInfoText.setText( Html.fromHtml( msg ) );
                speakerArrayInfoText.setTextColor( Utils.getResourceColor( R.color.atp_guide_info ) );
                ATPhub.voicePositioning( position.id, cachePath, item.name );
                return;
            }
            else {
                msg = getString( R.string.atp_tplay_position_free, position.name );
                speakerArrayInfoText.setText( Html.fromHtml( msg ) );
                speakerArrayInfoText.setTextColor( Utils.getResourceColor( R.color.atp_guide_info ) );
            }
            
        }
        ATPhub.voicePositioning( -1, null, null );
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    public class SpeakerAdapter extends ArrayAdapter<SpeakerItem> {
        private int resourceId;
        private SpeakerComparator compareor;

        public SpeakerAdapter( Context context, int resourceId ) {
            super( context, resourceId );
            this.resourceId = resourceId;

            compareor = new SpeakerComparator();
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            SpeakerItem spk = getItem( position );

            View view = convertView;
            if( view == null ) {
                view = getLayoutInflater().inflate( resourceId, parent, false );
            }

            return spk.updateView( view );

        }

        public boolean updateSpeakerProfile( List< ATPhub.SpeakerProfile> profileList ) {
            int count = getCount();
            boolean isUpdated = false;
            SpeakerItem item;

            for(  ATPhub.SpeakerProfile profile : profileList ) {
                boolean found = false;
                for( int i = 1; i < count; i++ ) {
                    item = getItem( i );
                    if( item.name.equals( profile.speakerName ) ) {
                        if( item.setActive( profile.speakerStatus == 1 ) ) {
                            isUpdated = true;
                        }
                        found = true;
                        break;
                    }
                }

                if( !found ) {
                    add( new SpeakerItem( profile.speakerName, profile.speakerStatus == 1 ) );
                    isUpdated = true;
                }
            }

            return isUpdated;
        }

        public void sort() {
            sort( compareor );
        }
    }

    private final class SpeakerComparator implements Comparator<SpeakerItem> {
        @Override
        public int compare( SpeakerItem spk1, SpeakerItem spk2 ) {
            return spk1.compareTo( spk2 );
        }
    }

    private final class SpeakerLayout {

        public static final int ID_SPEAKER_L  = 0;
        public static final int ID_SPEAKER_R  = 1;
        public static final int ID_SPEAKER_SL = 2;
        public static final int ID_SPEAKER_SR = 3;

        public static final String STR_SPEAKER_L  = "L";
        public static final String STR_SPEAKER_R  = "R";
        public static final String STR_SPEAKER_SL = "SL";
        public static final String STR_SPEAKER_SR = "SR";

        private SpeakerPosition[] positions;

        public SpeakerLayout( LinearLayout positionL,  ImageButton btnL,  TextView titleL,
                              LinearLayout positionR,  ImageButton btnR,  TextView titleR,
                              LinearLayout positionSL, ImageButton btnSL, TextView titleSL,
                              LinearLayout positionSR, ImageButton btnSR, TextView titleSR  ) {

            positions = new SpeakerPosition[] {
                    new SpeakerPosition( ID_SPEAKER_L,  STR_SPEAKER_L,  positionL,  btnL,  titleL  ),
                    new SpeakerPosition( ID_SPEAKER_R,  STR_SPEAKER_R,  positionR,  btnR,  titleR  ),
                    new SpeakerPosition( ID_SPEAKER_SL, STR_SPEAKER_SL, positionSL, btnSL, titleSL ),
                    new SpeakerPosition( ID_SPEAKER_SR, STR_SPEAKER_SR, positionSR, btnSR, titleSR )
            };

        }

        public ImageButton getSpeakerButton( int id ) {
            return positions[ id ].getSpeakerButton();
        }

        public SpeakerPosition getPosition( int id ) {
            if( !( id >= 0 && id <= ID_SPEAKER_SR) ) {
                return null;
            }

            return positions[ id ];
        }

        public String getPositioningSpeakerName( int positionId ) {
            SpeakerPosition position = getPosition( positionId );
            if( !position.bindingSpeakerItem.isEmptyItem() ) {
                return position.bindingSpeakerItem.name;
            }
            return null;
        }

        public SpeakerPosition getEditingPosition() {
            for( int i = 0; i <= ID_SPEAKER_SR; i++ ) {
                if( positions[i].isSelected ) {
                    return positions[i];
                }
            }
            return null;
        }

        public void updateView() {
            for( int i = 0; i <= ID_SPEAKER_SR; i++ ) {
                positions[i].updateTitleTextView( );
            }
        }

        public SpeakerPosition getBindingPositionBy( SpeakerItem item ) {
            for( int i = 0; i <= ID_SPEAKER_SR; i++ ) {
                if( positions[i].isBindingWith( item ) ) {
                    return positions[i];
                }
            }
            return null;
        }

        public void resetSelectedAll( int excludeId ) {
            for( int i = 0; i <= ID_SPEAKER_SR; i++ ) {
                if( positions[i].id != excludeId ) {
                    positions[i].setSelected( false );
                }
            }
        }

        public void resetFocusAll( int excludeId ) {
            for( int i = 0; i <= ID_SPEAKER_SR; i++ ) {
                if( positions[i].id != excludeId ) {
                    positions[i].setFocus( false );
                }
            }
        }

        public void unbindAll( SpeakerItem item ) {
            for( int i = 0; i <= ID_SPEAKER_SR; i++ ) {
                positions[i].unbind( item );
            }
        }
    }

    private class SpeakerPosition {

        public final int id;
        public final String name;

        private boolean isSelected;
        private boolean hasFocus;
        private LinearLayout positionLayout;
        private ImageButton  spkButton;
        private TextView     titleText;
        private SpeakerItem bindingSpeakerItem;

        public SpeakerPosition( int id, String name, LinearLayout positionLayout, ImageButton spkButton, TextView titleText ) {
            this.id = id;
            this.name = name;
            this.positionLayout = positionLayout;
            this.spkButton = spkButton;
            this.titleText = titleText;

            isSelected = false;
            hasFocus = false;
            bindingSpeakerItem = Empty_Speaker_Item;

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape( GradientDrawable.RECTANGLE );
            drawable.setStroke( 1, Utils.getResourceColor( R.color.atp_transparent ) );
            drawable.setCornerRadius( 4 );
            this.positionLayout.setBackground( drawable );

            this.spkButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    
                    onButtonClick();
                }
            } );
            this.spkButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    onButtonFocusChange();
                }
            });

            updatePositionView();
            updateTitleTextView();
        }

        public ImageButton getSpeakerButton() {
            return spkButton;
        }

        public boolean isBindingWith( SpeakerItem item ) {
            return bindingSpeakerItem.isSameItemAs( item );
        }

        public void unbind( SpeakerItem item ) {
            if( bindingSpeakerItem.isEmptyItem() || item.isEmptyItem() ) {
                return;
            }

            if( isBindingWith( item ) ) {
                bindingSpeakerItem = Empty_Speaker_Item;
                updateTitleTextView();
            }
        }

        public void bindWith( SpeakerItem item ) {
            if( isBindingWith( item ) ) {
                return;
            }

            speakerLayout.unbindAll( item );

            bindingSpeakerItem = item;
            updateTitleTextView();
        }


        public void onButtonClick() {
            String msg = getString( R.string.atp_tplay_position_speaker, name );
            speakerArrayInfoText.setText( Html.fromHtml( msg ) );
            speakerArrayInfoText.setTextColor( Utils.getResourceColor( R.color.atp_guide_info ) );

            if( !isSelected ) {
                speakerLayout.resetSelectedAll( id );
            }
            setSelected( !isSelected );
        }

        public void onButtonFocusChange() {
            if( !hasFocus ) {
                speakerLayout.resetFocusAll( id );
            }
            setFocus( !hasFocus );
        }

        private void setSelected( boolean sel ) {
            if( isSelected != sel ) {
                isSelected = sel;
                speakerListAdapter.notifyDataSetChanged( );
                updatePositionView();
            }
        }

        private void setFocus( boolean focus ) {
            if( hasFocus != focus ) {
                hasFocus = focus;
                updatePositionView();
            }
        }

        private void updatePositionView() {

            GradientDrawable drawable = (GradientDrawable)positionLayout.getBackground();
            if( hasFocus ) {
                drawable.setColor( Utils.getResourceColor( R.color.atp_focused_background ) );
                drawable.setStroke( 1, Utils.getResourceColor( R.color.atp_focused_border ) );
            }
            else {
                drawable.setColor( Utils.getResourceColor( R.color.atp_transparent ) );
                drawable.setStroke( 1, Utils.getResourceColor( R.color.atp_transparent ) );
            }

            spkButton.setImageResource( Position_Image_Ids[ id ][ isSelected ? 1 : 0 ] );
        }

        private void updateTitleTextView() {

            if( bindingSpeakerItem.isEmptyItem() ) {
                titleText.setText( "-" + bindingSpeakerItem.name + "-" );
                titleText.setTextColor( Utils.getResourceColor( R.color.atp_empty_title ) );
            }
            else {
                titleText.setText( bindingSpeakerItem.name );
                titleText.setTextColor( bindingSpeakerItem.isActive()
                                        ? Utils.getResourceColor( R.color.atp_active_title )
                                        : Utils.getResourceColor( R.color.atp_normal_title ) );
            }
        }

    }

    private final class SpeakerItem {

        public final String name;

        private boolean isActive;
        private boolean isSelected;

        public SpeakerItem( String name, boolean isActive ) {
            this.name = name;
            this.isActive   = isActive;
            this.isSelected = false;
        }

        public boolean isSameItemAs( SpeakerItem item ) {
            if( item == null ) {
                return false;
            }

            return this.name.compareTo( ( item.name ) ) == 0;
        }

        public boolean isEmptyItem() {
            return isSameItemAs( Empty_Speaker_Item );
        }

        public boolean setActive( boolean isActive ) {
            if( this.isActive != isActive ) {
                this.isActive = isActive;
                return true;
            }
            return false;
        }
        public boolean isActive() {
            return this.isActive;
        }

        public boolean setSelected( boolean sel ) {
            isSelected = sel;
            return isSelected;
        }

        public int compareTo( SpeakerItem item ) {

            if( this.isEmptyItem() ) {
                return -1;
            }

            if( item.isEmptyItem() ) {
                return 1;
            }

            if( this.isActive && !item.isActive ) {
                return -1;
            }
            else if( !this.isActive && item.isActive ) {
                return 1;
            }

            return this.name.compareTo( item.name );
        }

        public View updateView( View view ) {

            String position = null;

            SpeakerPosition selectedPosition = speakerLayout.getEditingPosition();
            SpeakerPosition bindingPosition = speakerLayout.getBindingPositionBy( this );

            if( bindingPosition != null ) {
                position = bindingPosition.name;
            }

            TextView nameView = view.findViewById( R.id.atp_speaker_name );
            nameView.setText( name );
            nameView.setTextColor( isActive
                                    ? Utils.getResourceColor( R.color.atp_active_title )
                                    : Utils.getResourceColor( R.color.atp_normal_title ) );

            boolean binding = false;
            if( selectedPosition != null ) {
                binding = selectedPosition.isBindingWith( this );
            }
            ImageView cursorView = view.findViewById( R.id.atp_cursor_image );
            cursorView.setImageResource( binding ? R.drawable.atp_cursor : R.drawable.atp_no_cursor );

            if( this.isEmptyItem() ) {
                position = null;
            }
            TextView posView = view.findViewById( R.id.atp_speaker_positon );
            posView.setText( position == null ? "" : position );
            posView.setTextColor( isActive
                                    ? Utils.getResourceColor( R.color.atp_active_title )
                                    : Utils.getResourceColor( R.color.atp_normal_title ) );

            return view;
        }
    }

}