package tech.depthcore.ongoplayer;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MediaSourceDevice extends MediaSource {
    protected MediaSourceDevice( ) {
        super( MediaSource.Type_Device );
    }

    @Override
    protected int loadChildrenOfNode( FileNode node ) {
        if( node.children.getCount() > 0 ) {
            return 0;
        }

        File folder;

        if( node.parent == null ) {
            folder = Environment.getExternalStorageDirectory();
            node.children.add( new FileNode(
                    FileNode.TYPE_DEVICE,
                    Utils.getContext().getString( R.string.ongo_device_storage_folder_name ),
                    folder.getAbsolutePath() + "/Movies",
                    node,
                    sourceType, 
                    FSID_UNDEF )
            );
            node.children.add( new FileNode(
                    FileNode.TYPE_USB,
                    Utils.getContext().getString( R.string.ongo_usb_storage_folder_name ),
                    null,
                    node,
                    sourceType, 
                    FSID_UNDEF )
            );
        }
        else {
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission( Utils.getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions( Utils.getActivity(), new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE }, Utils.PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE );
            }

            folder = new File( node.absolutePath );
            if( !folder.exists() ) {
                Log.i( Utils.TAG, "Error!! the device's storage does not exist: " + node.absolutePath );
                return 0;
            }

            File[] allFiles = folder.listFiles();
            if( allFiles == null ) {
                return 0;
            }

            for( File oneFile : allFiles ) {
                String filename = oneFile.getName().toLowerCase();
                if( oneFile.isDirectory() || filename.endsWith( ".mp4" ) || filename.endsWith( ".mkv" ) ) {
                    node.children.add(new FileNode(
                            oneFile.isDirectory() ? FileNode.TYPE_FOLDER : FileNode.TYPE_FILE,
                            oneFile.getName(),
                            oneFile.getAbsolutePath(),
                            node,
                            sourceType, 
                            FSID_UNDEF )
                    );
                }
            }
            node.children.sort();
        }

        return 1;
    }

    @Override
    protected int loadVideoMetadataOfNode( FileNode node ) throws Utils.ErrorException {

        if( node.metadata.thumbnail == null ) {
            if( Utils.retriveVideoMetadataFromDeviceFile( node.absolutePath, node.metadata ) >= 0 ) {
                return 1;
            }
        }

        return 0;
    }

    @Override
    protected void setCurrentNodeAndCreateChooserView( FileNode node ) {

        if( node != null ) {
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission( Utils.getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions( Utils.getActivity(), new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE }, Utils.PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE );
                return;
            }
        }

        super.setCurrentNodeAndCreateChooserView( node );
    }


}
