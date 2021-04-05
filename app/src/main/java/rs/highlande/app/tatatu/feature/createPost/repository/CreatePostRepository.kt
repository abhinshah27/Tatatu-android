package rs.highlande.app.tatatu.feature.createPost.repository

import android.os.Bundle
import io.reactivex.Observable
import okhttp3.MultipartBody
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.feature.createPost.api.CreatePostApi
import rs.highlande.app.tatatu.model.createPost.uploadImage.UploadMediaResponse

/**
 * Created by Abhin.
 */
class CreatePostRepository : BaseRepository() {
    private val createPostApi: CreatePostApi by inject()

    //upload media
    fun getMediaUpload(xID: String, mediaType: String, uploadType: String, files: MultipartBody.Part): Observable<UploadMediaResponse> {
        return createPostApi.getMediaUpload(xID, mediaType, uploadType, files)
    }
    //create post
    fun getCreatePost(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return createPostApi.getCreatePost(caller, bundle)
    }
    //Edit post
    fun getEditPost(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return createPostApi.getEditPost(caller, bundle)
    }
}