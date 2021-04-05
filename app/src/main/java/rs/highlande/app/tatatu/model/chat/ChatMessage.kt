package rs.highlande.app.tatatu.model.chat

import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.json.JSONObject
import rs.highlande.app.tatatu.core.cache.AudioVideoCache
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoObject
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import rs.highlande.app.tatatu.model.RealmModelListener
import java.util.*

/**
 * Enum class representing all the potential types of a [ChatMessage] object.
 * @author mbaldrighi on 10/26/2018.
 */
enum class ChatMessageType(val value: Int) {
    TEXT(0),
    VIDEO(1),
    PICTURE(2),
    LOCATION(3),
    AUDIO(4),
    SYSTEM(5),
    DOCUMENT(6),

    MISSED_VOICE(7),
    MISSED_VIDEO(8);


    fun toHLMediaTypeEnum(): HLMediaType? {
        return when (this) {
            VIDEO -> HLMediaType.VIDEO
            PICTURE -> HLMediaType.PHOTO
            AUDIO -> HLMediaType.AUDIO
            DOCUMENT -> HLMediaType.DOCUMENT
            else -> null
        }
    }
}

/**
 * Class representing the instance of a chat message.
 * @author mbaldrighi on 10/15/2018.
 */
@RealmClass
open class ChatMessage: RealmModel, RealmChangeListener<ChatMessage>,
    RealmModelListener, JsonHelper.JsonDeSerializer,
    JsonToSerialize, VideoObject {

    enum class DirectionType { INCOMING, OUTGOING }
    enum class Status { SENDING, SENT, DELIVERED, READ, ERROR, OPENED }

    companion object {
        fun getMessage(json: JSONObject?): ChatMessage? {

            if (json == null) return null

            val message = JsonHelper.deserialize(json) as? ChatMessage

            return message?.let {
                //TODO: Is there a case when creationDate can be null?
                it.creationDateObj = it.creationDate.getDateFromDB()!!
                it.sentDateObj = it.sentDate?.getDateFromDB()
                it.deliveryDateObj = it.deliveryDate?.getDateFromDB()
                it.readDateObj = it.readDate?.getDateFromDB()
                it.openedDateObj = it.openedDate?.getDateFromDB()
                it
            } ?: run {
                null
            }
        }

        fun handleUnsentMessages(realm: Realm): Boolean {
            val nullLong: Long? = null
            val unsent = realm.where(ChatMessage::class.java)
                .equalTo("unixtimestamp", nullLong)
                .or()
                .equalTo("unixtimestamp", 0L)
                .findAll()

            return if (unsent.isNotEmpty()) {
                realm.executeTransaction {
                    for (i in unsent) i.isError = true
                }
                true
            }
            else false
        }
    }



    /* IDs */
    @Expose @PrimaryKey var messageID: String = UUID.randomUUID().toString()
    @Expose @Index var senderID: String? = null
    @Expose var recipientID: String? = null
    @Expose @Index var chatRoomID: String? = null
    @Index var unixtimestamp: Long? = null

    @Expose var messageType: Int = 0

    var isError: Boolean = false

    /* Dates */
    @Expose var creationDate: String = nowToDBDate()
    @Index var creationDateObj: Date = Date()
        set(value) {
            field = value
            if (creationDate.isBlank()) creationDate = value.toDBDate()
        }
    var sentDate: String? = null
    var sentDateObj: Date? = null
        set(value) {
            field = value
            if (sentDate.isNullOrBlank()) sentDate = value?.toDBDate()
        }
    var deliveryDate: String? = null
    var deliveryDateObj: Date? = null
        set(value) {
            field = value
            if (deliveryDate.isNullOrBlank() && value != null) deliveryDate = value.toDBDate()
        }
    var readDate: String? = null
    var readDateObj: Date? = null
        set(value) {
            field = value
            if (readDate.isNullOrBlank()) readDate = value?.toDBDate()
        }
    var openedDate: String? = null
    var openedDateObj: Date? = null
        set(value) {
            field = value
            if (openedDate.isNullOrBlank()) openedDate = value?.toDBDate()
        }

    /* Content */
    @Expose var text: String = ""
    @Expose var mediaURL: String = ""
    var videoThumbnail: String? = null
    @Expose var location: String = ""
    @Expose var sharedDocumentFileName: String = ""
    var webLink: HLWebLink? = null


    //region == Class methods ==

    fun isSameDateAs(message: ChatMessage?): Boolean {
        val cal = Calendar.getInstance()
        val calOther = Calendar.getInstance()

        cal.time = creationDateObj
        if (message?.creationDateObj == null) return false
        calOther.time = message.creationDateObj

        return cal[Calendar.YEAR] == calOther[Calendar.YEAR] &&
                cal[Calendar.MONTH] == calOther[Calendar.MONTH] &&
                cal[Calendar.DAY_OF_MONTH] == calOther[Calendar.DAY_OF_MONTH]
    }

    fun isSameType(message: ChatMessage?, currentUserId: String): Boolean {
        return getDirectionType(currentUserId) == message?.getDirectionType(currentUserId)
    }

    fun hasMedia(): Boolean {
        return !mediaURL.isBlank()
    }

    fun hasPicture(): Boolean {
        return getMessageType() == ChatMessageType.PICTURE
    }

    fun hasVideo(): Boolean {
        return getMessageType() == ChatMessageType.VIDEO
    }

    fun hasAudio(): Boolean {
        return getMessageType() == ChatMessageType.AUDIO
    }

    fun hasLocation(): Boolean {
        return getMessageType() == ChatMessageType.LOCATION && location.isNotBlank()
    }

    fun hasDocument(): Boolean {
        return getMessageType() == ChatMessageType.DOCUMENT && sharedDocumentFileName.isNotBlank()
    }

    fun hasWebLink(): Boolean {
        return webLink != null
    }

    fun isMissedVoiceCall(): Boolean {
        return getMessageType() == ChatMessageType.MISSED_VOICE
    }

    fun isMissedVideoCall(): Boolean {
        return getMessageType() == ChatMessageType.MISSED_VIDEO
    }

    fun isSystem(): Boolean {
        return getMessageType() == ChatMessageType.SYSTEM
    }


    fun getMessageType(): ChatMessageType {
        return when (messageType) {
            0 -> ChatMessageType.TEXT
            1 -> ChatMessageType.VIDEO
            2 -> ChatMessageType.PICTURE
            3 -> ChatMessageType.LOCATION
            4 -> ChatMessageType.AUDIO
            5 -> ChatMessageType.SYSTEM
            6 -> ChatMessageType.DOCUMENT
            7 -> ChatMessageType.MISSED_VOICE
            8 -> ChatMessageType.MISSED_VIDEO
            else -> ChatMessageType.TEXT
        }
    }

    fun getDirectionType(currentUserId: String): DirectionType {
        return if (currentUserId == senderID) DirectionType.OUTGOING else DirectionType.INCOMING
    }

    fun getStatusEnum(): Status {
        return when {
            !openedDate.isNullOrBlank() -> Status.OPENED
            !readDate.isNullOrBlank() -> Status.READ
            !deliveryDate.isNullOrBlank() -> Status.DELIVERED
            unixtimestamp != null && unixtimestamp!! > 0 -> Status.SENT
            (unixtimestamp == null || unixtimestamp == 0L) && isError -> Status.ERROR
            else -> Status.SENDING
        }
    }

    fun isRead(): Boolean {
        return getStatusEnum() == Status.READ
    }

    fun isDelivered(): Boolean {
        return getStatusEnum() == Status.DELIVERED
    }

    fun isSent(): Boolean {
        return getStatusEnum() == Status.SENT
    }

    fun isOpened(): Boolean {
        return getStatusEnum() == Status.OPENED
    }

    fun isIncoming(myUserId: String?): Boolean {
        return myUserId?.let {
            senderID == myUserId
        } ?: false
    }

    fun getNewOutgoingMessage(senderId: String? = null,
                              recipientId: String? = null,
                              chatRoomId: String? = null,
                              text: String = "",
                              type: Int = 0,
                              mediaPayload: String = "",
                              initialized: Boolean = false,
                              fileName: String = ""): ChatMessage {

        return this.apply {

            if (!initialized) {
                this.senderID = senderId
                this.recipientID = recipientId
                this.chatRoomID = chatRoomId
            }
            this.messageType = type
            this.text = text

            if (mediaPayload.isNotBlank()) {
                when (type) {
                    ChatMessageType.LOCATION.value -> this.location = mediaPayload
                    ChatMessageType.DOCUMENT.value -> {
                        this.mediaURL = mediaPayload
                        this.sharedDocumentFileName = fileName
                    }
                    else -> this.mediaURL = mediaPayload
                }
            }
        }
    }

    //endregion


    //region == Json De-Serialization ==

    override val selfObject: Any
        get() = this

    override fun serializeWithExpose(): JsonElement {
        return JsonHelper.serializeWithExpose(this)
    }

    override fun serializeToStringWithExpose(): String {
        return JsonHelper.serializeToStringWithExpose(this)
    }

    override fun serialize(): JsonElement {
        return JsonHelper.serialize(this)
    }

    override fun serializeToString(): String {
        return JsonHelper.serializeToString(this)
    }

    override fun deserialize(json: JSONObject): JsonHelper.JsonDeSerializer? {
        return JsonHelper.deserialize(json)
    }

    override fun deserialize(json: JsonElement): JsonHelper.JsonDeSerializer? {
        return JsonHelper.deserialize(json)
    }

    override fun deserialize(jsonString: String): JsonHelper.JsonDeSerializer? {
        return JsonHelper.deserialize(jsonString)
    }

    //endregion


    //region == Realm listener ==

    override fun onChange(t: ChatMessage) {
        update(t)
    }

    override fun reset() {}

    override fun read(realm: Realm?): Any? {
        return null
    }

    override fun read(realm: Realm?, model: Class<out RealmModel>?): RealmModel? {
        return null
    }

    override fun deserializeStringListFromRealm() {}

    override fun serializeStringListForRealm() {}

    override fun write(realm: Realm?) {}

    override fun write(obj: Any?) {}

    override fun write(json: JSONObject?) {
        update(json)
    }

    override fun write(realm: Realm?, model: RealmModel?) {}

    override fun update() {}

    override fun update(obj: Any?) {
        if (obj is ChatMessage) {
            messageID = obj.messageID
            senderID = obj.senderID
            recipientID = obj.recipientID
            chatRoomID = obj.chatRoomID
            unixtimestamp = obj.unixtimestamp

            messageType = obj.messageType

            creationDate = obj.creationDate
            creationDateObj = obj.creationDateObj
            sentDate = obj.sentDate
            sentDateObj = if (obj.sentDateObj != null) obj.sentDateObj else obj.sentDate?.getDateFromDB()
            deliveryDate = obj.deliveryDate
            deliveryDateObj = if (obj.deliveryDateObj != null) obj.deliveryDateObj else obj.deliveryDate?.getDateFromDB()
            readDate = obj.readDate
            readDateObj = if (obj.readDateObj != null) obj.readDateObj else obj.readDate?.getDateFromDB()
            openedDate = obj.openedDate
            openedDateObj = if (obj.openedDateObj != null) obj.openedDateObj else obj.openedDate?.getDateFromDB()

            text = obj.text
            mediaURL = obj.mediaURL
            videoThumbnail = obj.videoThumbnail
            location = obj.location
            sharedDocumentFileName = obj.sharedDocumentFileName
            webLink = obj.webLink
        }
    }

    override fun update(json: JSONObject?) {
        if (json != null) deserialize(json)
    }

    override fun updateWithReturn(): RealmModelListener? = null

    override fun updateWithReturn(obj: Any?): RealmModelListener {
        update(obj)
        return this
    }

    override fun updateWithReturn(json: JSONObject?): RealmModelListener {
        return deserialize(json.toString()) as RealmModelListener
    }


    //endregion



    override fun serializeToJsonObject(): JSONObject {
        return JSONObject(JsonHelper.serializeToStringWithExpose(this))
    }

    override fun getUniqueID() = messageID

    override fun getVideoUrls() = listOf(mediaURL)

    override fun getVideoObject(videoCache: AudioVideoCache): List<Any>? {
        return if (getMessageType() == ChatMessageType.VIDEO) {
            val media = videoCache.getMedia(mediaURL, HLMediaType.VIDEO)
             listOf(media ?: mediaURL)
        } else null
    }

}