package tech.depthcore.ongoplayer;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;


public class SettingsGeneralFragment extends SettingsActivity.SettingsFragment {

    private String[] userTypeDesc;
    private String   versionString;

    public SettingsGeneralFragment() {
    }

    //clean history
    public int getSubjectId() {
        return SettingsActivity.SUBJECT_GENERAL;
    }

    private boolean initByBundle( Bundle bundle ) {
        if( bundle == null ) {
            return false;
        }

        return true;
    }

    private String getUserTypeDesc( int type ) {
        if( type < 0 || type >= userTypeDesc.length ) {
            return "" + type;
        }

        return userTypeDesc[ type ];
    }

    @Override
    public void adjustFocuseNavigation( View subjectList, View backBtn, View tplayBtn ) {
    }



    @Override
    public void onResume( ) {
        super.onResume();
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

        userTypeDesc = Utils.getContext().getResources().getStringArray( R.array.baidu_auth_usertype_desc );
        versionString = Utils.getContext().getString( R.string.ongo_settings_general_version, Utils.getAppVersion() );

    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle inState ) {

        if( !initByBundle( inState ) ) {
            initByBundle( getArguments() );
        }

        View cntView = inflater.inflate( R.layout.settings_general_fragment, container, false );
        setFocusable( cntView );


        TextView text = cntView.findViewById( R.id.settings_about_version );
        text.setText( versionString );

        return cntView;
    }

    @Override
    public void onSaveInstanceState( Bundle outState ){
        super.onSaveInstanceState( outState );
    }

}