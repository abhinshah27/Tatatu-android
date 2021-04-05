package rs.highlande.app.tatatu.model

import rs.highlande.app.tatatu.core.util.getReadableVideoDuration

/**
 * Created by Abhin.
 */
open class DataList(
    val id: String = "",
    val imagePath: String = "",
    val fileName: String? = null,
    val fileSize: Int? = 0,
    val imageWidth: Int? = 0,
    val imageHeight: Int? = 0
) {
    var isSelected = false
}

class DataListVideo(
    id: String = "",
    imagePath: String = "",
    fileName: String? = null,
    fileSize: Int? = 0,
    imageWidth: Int? = 0,
    imageHeight: Int? = 0,
    videoDuration: Int? = 0
) : DataList(id, imagePath, fileName, fileSize, imageWidth, imageHeight) {
    val durationToString = if (videoDuration != null) getReadableVideoDuration(videoDuration.toLong()) else ""
}

class DataListDisplay : DataList()
