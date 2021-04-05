package rs.highlande.app.tatatu.model.chat

import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.realm.Realm
import io.realm.RealmModel
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.json.JSONObject
import rs.highlande.app.tatatu.core.util.JsonHelper
import rs.highlande.app.tatatu.core.util.areStringsValid
import rs.highlande.app.tatatu.core.util.isStringValid
import rs.highlande.app.tatatu.model.RealmModelListener
import rs.highlande.app.tatatu.model.User
import java.io.Serializable

@RealmClass
open class Participant : RealmModel,
    RealmModelListener, Serializable, Comparable<Participant>,
    JsonHelper.JsonDeSerializer {

    /* VARS NEEDED FOR SHARING */
    //endregion


    //region == Getters and setters

    @SerializedName(value = "userID", alternate = ["_id"])
    @Expose
    @PrimaryKey
    var id: String? = null
    @Expose
    var name: String? = null
    @Expose
    var nickname: String? = null
    @Expose
    var avatarURL: String? = null


    /* VARS NEEDED FOR CHAT */
    var chatRoomID: String? = null
    @SerializedName("pstatus")
    var chatStatus = ChatRecipientStatus.OFFLINE.value
    var lastSeenDate: String? = null

    @Index
    var canChat: Boolean = false
    var canVideocall: Boolean = false
    var canAudiocall: Boolean = false


    @Ignore
    var isSelected: Boolean = false



    override val selfObject: Any
        get() = this

    override fun hashCode(): Int {
        return if (isStringValid(id))
            id!!.hashCode()
        else
            super.hashCode()
    }

    override fun compareTo(o: Participant): Int {
        return if (areStringsValid(this.name, o.name)) this.name!!.compareTo(o.name!!) else 0

    }

    override fun equals(obj: Any?): Boolean {
        return if (obj is Participant && areStringsValid(
                id,
                obj.id
            )
        ) id == obj.id else super.equals(obj)
    }

    fun hasValidChatRoom(): Boolean {
        return isStringValid(chatRoomID)
    }



    fun convertToMainUser(): User {
        return User().apply {
            uid = id!!
            username = this@Participant.name!!
            name = this@Participant.name!!
            picture = avatarURL ?: ""
        }
    }


    //region == Serialization methods ==

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
        return null
    }

    override fun deserialize(jsonString: String): JsonHelper.JsonDeSerializer? {
        return JsonHelper.deserialize(jsonString)
    }

    //endregion


    //region == Realm model listener ==

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

    override fun update(obj: Any?) {}

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

}