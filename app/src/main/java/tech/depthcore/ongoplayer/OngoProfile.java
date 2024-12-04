package tech.depthcore.ongoplayer;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;

import com.google.android.exoplayer2.C;

import io.reactivex.Flowable;
import io.reactivex.Single;

public class OngoProfile {

    private static RxDataStore<Preferences> dataStore = null;

    public static GeneralProfile general = null;
    public static TplayProfile tplay = null;

    public static void releaseAll() {
        if( dataStore != null ) {
            dataStore.dispose();
            dataStore = null;
        }

        general = null;
        tplay = null;
    }

    private static void instance_validation() {
        if( dataStore == null ) {
            throw new RuntimeException( "!!!not initialized!!!" );
        }
    }

    private static String getStringValue( String key ) {
        instance_validation();

        Preferences.Key<String> keyId = PreferencesKeys.stringKey( key );
        Flowable<String> flow = dataStore.data().map( prefs -> prefs.get( keyId ) );
        try {
            return flow.blockingFirst();
        } catch( Exception e ) {
            return null;
        }
    }

    private static int getIntValue( String key, int defaultValue ) {
        String value = getStringValue( key );

        try {
            return Integer.parseInt( value );
        } catch( Exception e ) {
            return defaultValue;
        }
    }

    private static long getLongValue( String key, long defaultValue ) {
        String value = getStringValue( key );

        try {
            return Long.parseLong( value );
        } catch( Exception e ) {
            return defaultValue;
        }
    }

    private static void setStringValue( String key, String value ) {
        instance_validation();

        Preferences.Key<String> keyId = PreferencesKeys.stringKey( key );
        Single<Preferences> result = dataStore.updateDataAsync( prefs->{
            MutablePreferences mutablePrefs = prefs.toMutablePreferences();
            mutablePrefs.set( keyId, value );
            return Single.just( mutablePrefs );
        });
    }

    private static void setIntValue( String key, int value ) {
        setStringValue( key, "" + value );
    }

    private static void setLongValue( String key, long value ) {
        setStringValue( key, "" + value );
    }

    public static void init( Context context ) {
        dataStore = new RxPreferenceDataStoreBuilder( context, "OngoProfile" ).build();
        general = new GeneralProfile();
        tplay   = new TplayProfile();
    }

    ////////////////////////////////////////////////////////////////////////////////////
    public static class GeneralProfile {
        public final static int HISTORY_NUMBER_DEFAULT = 500;

        public final static String KEY_GENERAL_HISTORY_NUMBER   = new String( "HISTORY_NUM" );

        private int historyNumber;

        private GeneralProfile() {
            historyNumber = getIntValue( KEY_GENERAL_HISTORY_NUMBER, HISTORY_NUMBER_DEFAULT );
        }

        public int getHistoryNumber() {
            return historyNumber;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////
    public static class TplayProfile {

        //The stereo signals(2 channels) are not transmitted to the sink. Device speakers are silent when TPLAY is working.
        public final static int DEVICE_2CHS_DENY        = 0;

        //When TPLAY is working, the device speakers and external speakers play stereo signals synchronously.
        public final static int DEVICE_2CHS_PASS_ALL    = 1;

        //The 5.1 surround sound signals(6 channels) are not transmitted to the sink. Device speakers are silent when TPLAY is working.
        public final static int DEVICE_6CHS_DENY        = 0;

        //When TPLAY is working,  signals for all 5.1 surround sound channels are transmitted to the sink.
        public final static int DEVICE_6CHS_PASS_ALL    = 1;

        //When TPLAY is working,  signals for Center and LFE channel of 5.1 surround sound are transmitted to the sink.
        public final static int DEVICE_6CHS_PASS_C_LFE  = 2;

        //No mixing, the signals for FL/FR/SL/SR channels of 5.1 surround sound are directly transmitted to respective external speakers.
        public final static int EXTERNAL_4SPK_MIXING_6CHS_NONE               = 1;

        //Mixing FL=FL+C+LFE and FR=FR+C+LEF, then transmit FL / FR / SL / SR channels to respective external speakers.
        public final static int EXTERNAL_4SPK_MIXING_6CHS_CLFE_FLFR          = 2;

        //Mixing FL=FL+C+LFE and FR=FR+C+LEF and SL=SL+LFE and SR=SR+LFE, then transmit FL / FR / SL / SR channels to respective external speakers.
        public final static int EXTERNAL_4SPK_MIXING_6CHS_CLFE_FLFR_LFE_SLSR = 3;

        //Mixing FL=FL+LFE, FR=FR+LEF, SL=SL+LFE, SR=SR+LFE , then transmit FL / FR / SL / SR channels to respective external speakers.
        public final static int EXTERNAL_4SPK_MIXING_6CHS_LFE_FLFRSLSR       = 4;

        public final static String KEY_TPLAY_DEVICE_2CHS   = new String( "TPLY_DEV_2CHS" );
        public final static String KEY_TPLAY_DEVICE_6CHS   = new String( "TPLY_DEV_6CHS" );
        public final static String KEY_TPLAY_EXTER_4SPKS   = new String( "TPLY_EXT_SPKS" );

        private int device2CHS   = DEVICE_2CHS_DENY;
        private int device6CHS   = DEVICE_6CHS_DENY;
        private int external4SPK = EXTERNAL_4SPK_MIXING_6CHS_NONE;

        private TplayProfile() {
            device2CHS   = getIntValue( KEY_TPLAY_DEVICE_2CHS, DEVICE_2CHS_DENY );
            device6CHS   = getIntValue( KEY_TPLAY_DEVICE_6CHS, DEVICE_6CHS_PASS_C_LFE );
            external4SPK = getIntValue( KEY_TPLAY_EXTER_4SPKS, EXTERNAL_4SPK_MIXING_6CHS_NONE );
        }

        public int getDevice2CHS() {
            return device2CHS;
        }

        public int getDevice6CHS() {
            return device6CHS;
        }

        public int getExternal4SPK() {
            return external4SPK;
        }

        public boolean saveMixingProfile( int dev2CHS, int dev6CHS, int ext4SPK ) {
            boolean updated = false;

            if( ( dev2CHS == DEVICE_2CHS_DENY || dev2CHS == DEVICE_2CHS_PASS_ALL ) && device2CHS != dev2CHS ) {
                device2CHS = dev2CHS;
                setIntValue( KEY_TPLAY_DEVICE_2CHS, device2CHS );
                updated = true;
            }

            if( ( dev6CHS == DEVICE_6CHS_DENY || dev6CHS == DEVICE_6CHS_PASS_ALL ) && device6CHS != dev6CHS ){
                device6CHS = dev6CHS;
                setIntValue( KEY_TPLAY_DEVICE_6CHS, device6CHS );
                updated = true;
            }

            if( ext4SPK >= EXTERNAL_4SPK_MIXING_6CHS_NONE && ext4SPK <= EXTERNAL_4SPK_MIXING_6CHS_LFE_FLFRSLSR && external4SPK != ext4SPK ) {
                external4SPK = ext4SPK;
                setIntValue( KEY_TPLAY_EXTER_4SPKS, external4SPK );
                updated = true;
            }

            return updated;
        }
    }


}
