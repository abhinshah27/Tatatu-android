package rs.highlande.app.tatatu.feature.createPost.api

import android.os.Bundle
import io.reactivex.Observable
import okhttp3.MultipartBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.http.*
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CREATE_POST
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_EDIT_POST
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi
import rs.highlande.app.tatatu.model.createPost.uploadImage.UploadMediaResponse

/**
 * Created by Abhin.
 */
class CreatePostApi : BaseApi() {

    //Api call Interface
    interface UploadImagesRetrofitService {
        @Multipart
        @POST(TOKEN_UPLOADMEDIA)
        fun getUploadMedia(
            @Header(PARAM_X_ID) xID: String? = null,
            @Header(PARAM_X_MEDIA_TYPE) mediaType: String? = null,
            @Header(PARAM_X_UPLOAD_TYPE) uploadType: String? = null,
            @Part files: MultipartBody.Part
        ): Observable<UploadMediaResponse>
    }

    //Access the Interface
    fun getMediaUpload(xID: String, mediaType: String, uploadType: String, files: MultipartBody.Part): Observable<UploadMediaResponse> {
        return getRetrofitServiceTTU<UploadImagesRetrofitService>().getUploadMedia(xID, mediaType, uploadType, files)
    }

    //call the Create post
    fun getCreatePost(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_CREATE_POST, logTag = "Create New Post", caller = caller))
    }

    //call the Edit post
    fun getEditPost(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_EDIT_POST, logTag = "Edit Post", caller = caller))
    }
}