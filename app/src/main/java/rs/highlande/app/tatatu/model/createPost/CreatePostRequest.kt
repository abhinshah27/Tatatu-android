package rs.highlande.app.tatatu.model.createPost

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created by Abhin.
 */

class CreatePostRequest{
    @SerializedName("date")
    @Expose
    var date: String? = null
    @SerializedName("preview")
    @Expose
    var preview: String? = null
    @SerializedName("caption")
    @Expose
    var caption: String? = null
    @SerializedName("type") //post type can be: image, video, news
    @Expose
    var type: String? = null
    @SerializedName("mediaItems")
    @Expose
    var mediaItems= CreatePostMediaItemsRequest()
}

class CreatePostMediaItemsRequest {
    @SerializedName("mediaType")
    @Expose
    var mediaType: String? = null
    @SerializedName("mediaEndpoint")
    @Expose
    var mediaEndpoint: String? = null
    @SerializedName("size")
    @Expose
    var size= Size()
    @SerializedName("scale")
    @Expose
    var scale: String? = null
}

class Size {
    @SerializedName("width")
    @Expose
    var width: Int? = null
    @SerializedName("height")
    @Expose
    var height: Int? = null
}