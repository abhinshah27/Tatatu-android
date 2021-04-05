package rs.highlande.app.tatatu.model

import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import rs.highlande.app.tatatu.core.cache.AudioVideoCache
import rs.highlande.app.tatatu.core.cache.PicturesCache
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseDiffUtilCallback
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoObject
import rs.highlande.app.tatatu.core.util.JsonHelper
import rs.highlande.app.tatatu.core.util.JsonToSerialize
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import java.io.Serializable

/**
 * Enum indicating the kind of [PostMediaItem].
 * @author mbaldrighi on 2019-06-27.
 */
enum class MediaType(val value: String) {
    @SerializedName("photo")
    IMAGE("photo"),
    @SerializedName("video")
    VIDEO("video");

    companion object {

        fun toEnum(value: String?): MediaType {
            return value?.let {
                when (value) {
                    VIDEO.value -> VIDEO
                    else -> IMAGE
                }
            } ?: IMAGE
        }
    }
}

/**
 * Class holding the information of a media file present in a [Post] object.
 * @author mbaldrighi on 2019-06-27.
 */
// TODO: 2019-07-09    reconsider Parcelable BUT ISSUE with InstanceCreator
@Parcelize
class PostMediaItem(
    @Expose
    var mediaType: MediaType = MediaType.IMAGE,
    @Expose
    var mediaEndpoint: String = "",
    @Expose
    var size: Size = Size(),
    @Expose
    var scale: String = "1"
) : Serializable, Parcelable {

    /**
     * Holds the reference to the current selected media [Uri]. Annotated transient because excluded from the
     * de-/serialization process.
     */
    @Transient @IgnoredOnParcel
    var tmpUri: Uri? = null


    constructor() : this(mediaEndpoint = "")


    fun isPicture(): Boolean {
        return mediaType == MediaType.IMAGE
    }

    fun isVideo(): Boolean {
        return mediaType == MediaType.VIDEO
    }
}

/**
 * Holds the reference of the to the current selected media [Uri]. Annotated transient because excluded from the
 * de-/serialization process.
 */
@Parcelize
class Size(
    @Expose
    var width: Int = 0,
    @Expose
    var height: Int = 0
) : Serializable, Parcelable {
    constructor() : this(0, 0)
}


/**
 * Enum indicating the kind of [Post].
 * @author mbaldrighi on 2019-07-08.
 */
enum class PostType(val value: String) {
    @SerializedName("")
    NEW("new"),
    @SerializedName("image")
    IMAGE("image"),
    @SerializedName("video")
    VIDEO("video"),
    @SerializedName("multimedia")
    MULTIMEDIA("multimedia"),
    @SerializedName("news")
    NEWS("news")
}

/**
 * Class holding the information of a TTU post.
 * @author mbaldrighi on 2019-06-27.
 */
// TODO: 2019-07-09    reconsider Parcelable BUT ISSUE with InstanceCreator
@Parcelize
class Post(
    @Expose
    @SerializedName(value = "postID", alternate = ["uid"])
    var uid: String = "",
    @Expose
    var date: String = "",
    var title: String? = "",
    @Expose
    var preview: String = "",
    @Expose
    var type: PostType = PostType.NEW,
    @Expose
    var caption: String? = "",
    var likes: Int = 0,
    var commentsCount: Int = 0,
    var sharesCount: Int = 0,
    var liked: Boolean = false,
    var userData: MainUserInfo = MainUserInfo(),
    var link: String? = "",  // when is type=NEWS
    @Expose
    var mediaItems: MutableList<PostMediaItem> = mutableListOf(),
    var latestComments: MutableList<PostComment> = mutableListOf()
): HomeDataObject, ProfileListItem, PreviewPostDiffUtilImpl, Parcelable, JsonToSerialize, VideoObject {

    constructor() : this("")

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Post) && areItemsTheSame(other)
    }

    override fun hashCode(): Int {
        return if (!uid.isBlank()) uid.hashCode() else super.hashCode()
    }

    fun hasMultipleMedia(): Boolean {
        return type == PostType.MULTIMEDIA
    }

    fun isVideo(): Boolean {
        return type == PostType.VIDEO
    }

    fun isNews(): Boolean {
        return type == PostType.NEWS
    }

    fun isCreateNewPost(): Boolean {
        return type == PostType.NEW
    }

    fun isMainUserAuthor(userId: String?): Boolean {
        return if (!userId.isNullOrBlank()) userId == userData.uid else false
    }

    /**
     * This method is not really used: the post preview was never really implemented server side.
     */
    fun getPostPreview(): Any {
        return if (mediaItems.isNotEmpty()) mediaItems[0].mediaEndpoint else ""
    }

    // TODO: 2019-08-07    Reactivate PARCELER impl if needed

    companion object/* : Parceler<Post>*/ {
        fun get(json: JSONObject?): Post? {
            return if (json != null) JsonHelper.deserialize(json) else null
        }
        
        fun getIds(json: JSONArray?): List<String> {
            if (json == null) return emptyList()
            val idsList = ArrayList<String>(json.length())
            for(i in 0 until json.length()) {
                JsonHelper.deserialize<Map<String, String>>(json.getString(i))?.let {
                    it["_id"]?.let {
                        idsList.add(it)
                    }
                }
            }
            return idsList
        }

        // TODO: 2019-08-07    Reactivate PARCELER impl if needed

//        override fun create(parcel: Parcel): Post {
//            return Post(
//                parcel.readString() ?: "",                                  //uid
//                parcel.readString() ?: "",                                 //date
//                parcel.readString() ?: "",                                 //title
//                parcel.readString() ?: "",                              //preview
//                (parcel.readSerializable() as? PostType) ?: PostType.NEW,                        //type
//                parcel.readString() ?: "",                              //caption
//                parcel.readInt(),                                               //likes
//                parcel.readInt(),                                               //comments
//                parcel.readInt(),                                               //shares
//
//                if (hasQ()) parcel.readBoolean()                                //liked
//                else parcel.readInt() == 1,
//
//                parcel.readParcelable<MainUserInfo>(                   //userData
//                    MainUserInfo::class.java.classLoader
//                ) ?: MainUserInfo(),
//
//                parcel.readString() ?: "",                                 //link
//                parcel.readParcelableList(
//                    mutableListOf(),
//                    PostMediaItem::class.java.classLoader
//                ),                                                              //medias
//                parcel.readParcelableList(
//                    mutableListOf(),
//                    PostComment::class.java.classLoader
//                )                                                               //comments
//            )
//        }
//
//        override fun Post.write(parcel: Parcel, flags: Int) {
//            parcel.apply {
//                writeString(uid)
//                writeString(date)
//                writeString(title)
//                writeString(preview)
//                writeSerializable(type)
//                writeString(caption)
//                writeInt(likes)
//                writeInt(commentsCount)
//                writeInt(sharesCount)
//
//                if (hasQ()) writeBoolean(liked)
//                else writeInt(if (liked) 1 else 0)
//
//                writeParcelable(userData, flags)
//                writeString(link)
//                writeParcelableList(mediaItems, flags)
//                writeParcelableList(latestComments, flags)
//            }
//        }
    }

    override fun serializeToJsonObject(): JSONObject {
        return JSONObject(JsonHelper.serializeToStringWithExpose(this))
    }


    override fun areItemsTheSame(other: Post): Boolean {
        return uid == other.uid
    }

    override fun areContentsTheSame(other: Post): Boolean {
        return date == other.date &&
                title == other.title &&
                preview == other.preview &&
                type == other.type &&
                caption == other.caption &&
                likes == other.likes &&
                commentsCount == other.commentsCount &&
                sharesCount == other.sharesCount &&
                liked == other.liked &&
                userData.areItemsTheSame(other.userData) &&
                userData.areContentsTheSame(other.userData) &&
                link == other.link
    }

    override fun areContentsTheSameForPreview(other: Post): Boolean {
        return preview == other.preview && userData.username == other.userData.username && userData.name == other.userData.name
    }


    override fun getUniqueID() = uid

    override fun getVideoUrls(): List<String>? {
        return if (mediaItems.isNotEmpty()) {
            mutableListOf<String>().apply {
                mediaItems.forEach { add(it.mediaEndpoint) }
            }
        } else null
    }

    override fun getVideoObject(videoCache: AudioVideoCache): List<Any>? {
        return getMediaObjects(null, videoCache)
    }


    fun getMediaObjects(picturesCache: PicturesCache?, videoCache: AudioVideoCache?): List<Any>? {
        if (picturesCache == null && videoCache == null) return null

        return if (mediaItems.isNotEmpty()) {
            mutableListOf<Any>().apply {
                mediaItems.forEach {
                    if (it.isVideo() && videoCache != null)
                        add(videoCache.getMedia(it.mediaEndpoint, HLMediaType.VIDEO) ?: it.mediaEndpoint)
                    else if (it.isPicture() && picturesCache != null)
                        add(picturesCache.getMedia(it.mediaEndpoint, HLMediaType.PHOTO) ?: it.mediaEndpoint)
                }
            }
        } else null
    }

}


interface PreviewPostDiffUtilImpl : BaseDiffutilImpl<Post> {
    fun areContentsTheSameForPreview(other: Post): Boolean
}


/**
 * Class holding the information of a TTU comment on a [Post].
 * @author mbaldrighi on 2019-06-27.
 */
@Parcelize
open class PostComment(
    var id: String = "",
    var userData: MainUserInfo = MainUserInfo(),
    var date: String = "",
    var likesCount: Int = 0,
    var commentsCount: Int = 0,
    var liked: Boolean = false,
    var text: String = "",
    var level: Int = 0,
    var parentCommentID: String? = null
) : Serializable, Parcelable, BaseDiffutilImpl<PostComment> {

    constructor() : this("")

    companion object {
        fun get(json: JSONObject): PostComment? = JsonHelper.deserialize(json)

        fun getIds(json: JSONArray?): List<String> {
            if (json == null) return emptyList()
            val idsList = ArrayList<String>(json.length())
            for(i in 0 until json.length()) {
                JsonHelper.deserialize<Map<String, String>>(json.getString(i))?.let {
                    it["commentID"]?.let {
                        idsList.add(it)
                    }
                }
            }
            return idsList
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is PostComment && areItemsTheSame(other))
    }

    override fun hashCode(): Int {
        return if (!id.isBlank()) id.hashCode() else super.hashCode()
    }


    fun isSubComment() = level == 1


    override fun areItemsTheSame(other: PostComment) = id == other.id

    override fun areContentsTheSame(other: PostComment): Boolean {
        return userData.areItemsTheSame(other.userData) &&
                userData.areContentsTheSame(other.userData) &&
                likesCount == other.likesCount &&
                commentsCount == other.commentsCount &&
                liked == other.liked &&
                text == other.text &&
                level == other.level &&
                parentCommentID == other.parentCommentID
    }
}


class PostDiffCallback : BaseDiffUtilCallback<Post>() {

    override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.areItemsTheSame(newItem)

    override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem.areContentsTheSame(newItem)
}

class CommentDiffCallback : BaseDiffUtilCallback<PostComment>() {

    override fun areItemsTheSame(oldItem: PostComment, newItem: PostComment) = oldItem.areItemsTheSame(newItem)

    override fun areContentsTheSame(oldItem: PostComment, newItem: PostComment) = oldItem.areContentsTheSame(newItem)
}