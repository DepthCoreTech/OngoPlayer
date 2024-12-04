package tech.depthcore.ongoplayer;

public class MediaSourceAPK extends MediaSource {
    protected MediaSourceAPK( ) {
        super( MediaSource.Type_APK );
    }

    @Override
    protected int loadChildrenOfNode( FileNode node ) {
        if( node.children.getCount() > 0 ) {
            return 0;
        }

        node.children.add( new FileNode(
                FileNode.TYPE_FILE,
                Utils.getContext().getString( R.string.ongo_demos_in_apk_1 ),
                "file:///android_asset/movies/1.mp4",
                node,
                sourceType, 
                FSID_UNDEF )
        );
        node.children.add( new FileNode(
                FileNode.TYPE_FILE,
                Utils.getContext().getString( R.string.ongo_demos_in_apk_2 ),
                "file:///android_asset/movies/2.mp4",
                node,
                sourceType, 
                FSID_UNDEF )
        );
        node.children.add( new FileNode(
                FileNode.TYPE_FILE,
                Utils.getContext().getString( R.string.ongo_demos_in_apk_3 ),
                "file:///android_asset/movies/3.mp4",
                node,
                sourceType, 
                FSID_UNDEF )
        );
        node.children.add( new FileNode(
                FileNode.TYPE_FILE,
                Utils.getContext().getString( R.string.ongo_demos_in_apk_4 ),
                "file:///android_asset/movies/4.mp4",
                node,
                sourceType, 
                FSID_UNDEF )
        );

        return 1;
    }

    @Override
    protected int loadVideoMetadataOfNode( FileNode node ) throws Utils.ErrorException {
        if( node.metadata.thumbnail == null ) {
            if( Utils.retriveVideoMetadataFromAssetFile( node.absolutePath.substring( node.absolutePath.indexOf( "movies" ) ), node.metadata ) >= 0 ) {
                return 1;
            }
        }
        return 0;
    }
}
