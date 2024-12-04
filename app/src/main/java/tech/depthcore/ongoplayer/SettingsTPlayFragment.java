package tech.depthcore.ongoplayer;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import androidx.fragment.app.Fragment;


public class SettingsTPlayFragment extends SettingsActivity.SettingsFragment {

    RadioButton device2chsDeny;
    RadioButton device2chsPass;

    RadioButton device6chsDeny;
    RadioButton device6chsPassCLFE;

    RadioButton external6chsNone;
    RadioButton external6chsCLFEflfr;
    RadioButton external6chsCLFEflfrLFEslsr;
    RadioButton external6chsLFEflfrslsr;

    private int device2CHS;
    private int device6CHS;
    private int external4SPK;

    public SettingsTPlayFragment() {
    }

    public int getSubjectId() {
        return SettingsActivity.SUBJECT_TPLAY;
    }

    private boolean initByBundle( Bundle bundle ) {
        if( bundle == null ) {
            return false;
        }

        return true;
    }

    @Override
    public void onResume( ) {
        super.onResume();
        device2CHS   = OngoProfile.tplay.getDevice2CHS();
        device6CHS   = OngoProfile.tplay.getDevice6CHS();
        external4SPK = OngoProfile.tplay.getExternal4SPK();

        if( device2CHS == OngoProfile.TplayProfile.DEVICE_2CHS_PASS_ALL ) {
            device2chsPass.setChecked( true );
        }
        else {
            device2chsDeny.setChecked( true );
        }

        if( device6CHS == OngoProfile.TplayProfile.DEVICE_6CHS_PASS_C_LFE ) {
            device6chsPassCLFE.setChecked( true );
        }
        else {
            device6chsDeny.setChecked( true );
        }

        if( external4SPK == OngoProfile.TplayProfile.EXTERNAL_4SPK_MIXING_6CHS_CLFE_FLFR ) {
            external6chsCLFEflfr.setChecked( true );
        }
        else if( external4SPK == OngoProfile.TplayProfile.EXTERNAL_4SPK_MIXING_6CHS_CLFE_FLFR_LFE_SLSR ) {
            external6chsCLFEflfrLFEslsr.setChecked( true );
        }
        else if( external4SPK == OngoProfile.TplayProfile.EXTERNAL_4SPK_MIXING_6CHS_LFE_FLFRSLSR ) {
            external6chsLFEflfrslsr.setChecked( true );
        }
        else {
            external6chsNone.setChecked( true );
        }

    }

    @Override
    public void onPause( ) {
        super.onPause();

        boolean updated = OngoProfile.tplay.saveMixingProfile( device2CHS, device6CHS, external4SPK );
        if( updated && tech.depthcore.atphub.ATPhub.isServiceRunning() ) {
            TPlayButton.restartTPLAY();
        }
    }

    @Override
    public void adjustFocuseNavigation( View subjectList, View backBtn, View tplayBtn ) {

        subjectList.setNextFocusRightId( device2chsDeny.getId() );
        tplayBtn.setNextFocusDownId    ( device2chsDeny.getId() );

        external6chsLFEflfrslsr.setNextFocusDownId( device2chsDeny.getId() );

        device2chsDeny.setOnKeyListener( new View.OnKeyListener() {
            @Override
            public boolean onKey( View view, int i, KeyEvent event ) {
                if( event.getAction() == ACTION_DOWN && event.getKeyCode() == KEYCODE_DPAD_UP ) {
                    backBtn.requestFocus();
                }
                return false;
            }
        });
    }


    @Override
    public void onCreate( Bundle inState ) {
        super.onCreate( inState );

        if( !initByBundle( inState ) ) {
            initByBundle( getArguments() );
        }
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle inState ) {

        if( !initByBundle( inState ) ) {
            initByBundle( getArguments() );
        }

        View cntView = inflater.inflate( R.layout.settings_tplay_fragment, container, false );
        setFocusable( cntView );

        device2chsDeny = cntView.findViewById( R.id.settings_tplay_device_2chs_deny_radio );
        device2chsPass = cntView.findViewById( R.id.settings_tplay_device_2chs_pass_all_radio );

        device6chsDeny = cntView.findViewById( R.id.settings_tplay_device_6chs_deny_radio );
        device6chsPassCLFE = cntView.findViewById( R.id.settings_tplay_device_6chs_pass_c_lfe_radio );

        external6chsNone = cntView.findViewById( R.id.settings_tplay_external_4spks_6chs_mixing_none );
        external6chsCLFEflfr = cntView.findViewById( R.id.settings_tplay_external_4spks_6chs_mixing_clef_flfr );
        external6chsCLFEflfrLFEslsr = cntView.findViewById( R.id.settings_tplay_external_4spks_6chs_mixing_clef_flfr_lfe_slsr );
        external6chsLFEflfrslsr = cntView.findViewById( R.id.settings_tplay_external_4spks_6chs_mixing_lfe_flfrslsr );


        device2chsDeny.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton btn, boolean isChecked ) {
                if( isChecked ) {
                    device2CHS = OngoProfile.TplayProfile.DEVICE_2CHS_DENY;

                }
            }
        } );
        device2chsPass.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton btn, boolean isChecked ) {
                if( isChecked ) {
                    device2CHS = OngoProfile.TplayProfile.DEVICE_2CHS_PASS_ALL;
                }
            }
        } );

        device6chsDeny.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton btn, boolean isChecked ) {
                if( isChecked ) {
                    device6CHS = OngoProfile.TplayProfile.DEVICE_6CHS_DENY;
                }
            }
        } );
        device6chsPassCLFE.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton btn, boolean isChecked ) {
                if( isChecked ) {
                    device6CHS = OngoProfile.TplayProfile.DEVICE_6CHS_PASS_C_LFE;
                }
            }
        } );

        external6chsNone.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton btn, boolean isChecked ) {
                if( isChecked ) {
                    external4SPK = OngoProfile.TplayProfile.EXTERNAL_4SPK_MIXING_6CHS_NONE;
                }
            }
        } );
        external6chsCLFEflfr.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton btn, boolean isChecked ) {
                if( isChecked ) {
                    external4SPK = OngoProfile.TplayProfile.EXTERNAL_4SPK_MIXING_6CHS_CLFE_FLFR;
                }
            }
        } );
        external6chsCLFEflfrLFEslsr.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton btn, boolean isChecked ) {
                if( isChecked ) {
                    external4SPK = OngoProfile.TplayProfile.EXTERNAL_4SPK_MIXING_6CHS_CLFE_FLFR_LFE_SLSR;
                }
            }
        } );
        external6chsLFEflfrslsr.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton btn, boolean isChecked ) {
                if( isChecked ) {
                    external4SPK = OngoProfile.TplayProfile.EXTERNAL_4SPK_MIXING_6CHS_LFE_FLFRSLSR;
                }
            }
        } );

        return cntView;
    }

    @Override
    public void onSaveInstanceState( Bundle outState ){
        super.onSaveInstanceState( outState );
    }

}