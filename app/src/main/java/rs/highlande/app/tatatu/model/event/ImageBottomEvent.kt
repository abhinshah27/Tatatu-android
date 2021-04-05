package rs.highlande.app.tatatu.model.event

/**
 * Created by Abhin.
 */
data class ImageBottomEvent(
    var mImageClick: Boolean = false,
    var mGalleryClick: Boolean = false,
    var mVideoClick: Boolean = false)