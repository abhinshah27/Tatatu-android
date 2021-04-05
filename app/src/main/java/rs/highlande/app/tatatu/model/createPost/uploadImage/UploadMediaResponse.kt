package rs.highlande.app.tatatu.model.createPost.uploadImage

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created by Abhin.
 */
class UploadMediaResponse {
    @SerializedName("reponseCode")
    @Expose
    var responseCode: Int? = null
    @SerializedName("responseStatus")
    @Expose
    var responseStatus: String? = null
    @SerializedName("responseError")
    @Expose
    var responseError: String? = null
    @SerializedName("data")
    @Expose
    var mData: Data? = null

    @SerializedName("height")
    var height: Int? = null
    @SerializedName("width")
    var width: Int? = null
    @SerializedName("mediaType")
    var mediaType: String? = null
}

class Data {
    @SerializedName("preview")
    @Expose
    var preview: String? = null
    @SerializedName("original")
    @Expose
    var original: String? = null
}