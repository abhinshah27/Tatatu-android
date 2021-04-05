/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.model.chat

import com.google.gson.JsonElement
import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.json.JSONArray
import org.json.JSONObject
import rs.highlande.app.tatatu.core.util.JsonHelper
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.core.util.getDateFromDB
import rs.highlande.app.tatatu.core.util.nowToDBDate
import rs.highlande.app.tatatu.feature.chat.api.ChatApi
import rs.highlande.app.tatatu.model.RealmModelListener
import java.io.Serializable
import java.util.*

/**
 * Enum class representing all the potential statuses of a chat recipient.
 * @author mbaldrighi on 10/26/2018.
 */
enum class ChatRecipientStatus(val value: Int) { OFFLINE(0), ONLINE(1) }

/**
 * Class representing the instance of a chat room.
 * @author mbaldrighi on 10/26/2018.
 */
@RealmClass
open class ChatRoom(var ownerID: String? = null,
                    var participantIDs: RealmList<String> = RealmList(),
                    @PrimaryKey var chatRoomID: String? = null):
    RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer, Serializable, RealmChangeListener<ChatRoom> {

    companion object {
        @JvmStatic fun getRoom(json: JSONObject): ChatRoom? {
            val room = JsonHelper.deserialize(json) as? ChatRoom
            if (room != null) {
                room.recipientStatus = room.participants[0]?.chatStatus ?: 0
                for (it in room.participants)
                    room.participantIDs.add(it.id)

                room.dateObj = room.date.getDateFromDB()
                room.roomName = room.getRoomName()
            }

            return room
        }

        fun getRightTimeStamp(chatRoomId: String, realm: Realm?, direction: ChatApi.FetchDirection): Long? {
            if (realm == null) return 0

            val messages = RealmUtils.readFromRealmWithIdSorted(realm, ChatMessage::class.java,
                "chatRoomID", chatRoomId, "creationDateObj", Sort.ASCENDING) as RealmResults<ChatMessage>

            return if (direction == ChatApi.FetchDirection.BEFORE) {
                messages[0]?.unixtimestamp
            }
            else {
                return when {
                    messages.isEmpty() -> 0

                    !messages.isEmpty() -> {
                        var result = 0L
                        // from the newest message down to the oldest store [ChatMessage.unixtimestamp] if message isn't READ
                        for (it in (messages.size - 1) downTo 0) {
                            val message = messages[it]
                            if (message?.isRead() == false && !message.isError) {
                                if (message.unixtimestamp != null) result = message.unixtimestamp!!
                            }
                            else break
                        }

//                        if (result == 0L) {
//                            result = (messages[0] as ChatMessage).unixtimestamp!!; result
//                        } else
                        result
                    }

                    else -> 0
                }
            }
        }

        fun areThereUnreadMessages(userId: String, chatRoomId: String? = null, realm: Realm): Boolean {
            return if (!chatRoomId.isNullOrBlank()) {
                return RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", chatRoomId).let {
                    val incoming = it?.filter { (it as ChatMessage).getDirectionType(userId) == ChatMessage.DirectionType.INCOMING }
                    val filtered = incoming?.filter { (it as ChatMessage).getStatusEnum() == ChatMessage.Status.READ }
                    incoming?.let { inc ->
                        filtered?.let { filt ->
                            inc.size > filt.size
                        } ?: false
                    } ?: false
                }
            }
            else {
                var thereAreMessages = false
                RealmUtils.readFromRealm(realm, ChatRoom::class.java)?.forEach {
                    if (it is ChatRoom) {
                        if (it.toRead > 0) { thereAreMessages = true; return@forEach  }
                    }
                }

                thereAreMessages
            }
        }

        /**
         * Checks whether there are some rooms that need to be deleted from the local DB.
         * To be used already inside a Realm transaction.
         */
        fun handleDeletedRooms(realm: Realm, jsonArray: JSONArray?) {
            when {
                jsonArray == null -> return
                jsonArray.length() == 0 -> {
                    RealmUtils.deleteTable(realm, ChatRoom::class.java)
                    RealmUtils.deleteTable(realm, ChatMessage::class.java)
                    return
                }
                else -> {
                    val rooms = RealmUtils.readFromRealm(realm, ChatRoom::class.java)
                    if (rooms != null && !rooms.isEmpty()) {
                        rooms.forEach { room ->
                            val id = (room as? ChatRoom)?.chatRoomID
                            if (!id.isNullOrBlank()) {
                                var delete = true
                                for (i in 0 until jsonArray.length()) {
                                    val jID = jsonArray.optJSONObject(i)?.optString("chatRoomID", "")
                                    if (id == jID) { delete = false; break }
                                }

                                if (delete) {
                                    RealmObject.deleteFromRealm(room)
                                    RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", id)
                                        ?.deleteAllFromRealm()
                                }
                            }
                        }
                    }
                }
            }
        }

        fun checkRoom(chatRoomID: String?, realm: Realm?): Pair<Boolean, ChatRoom?> {
            if (!chatRoomID.isNullOrBlank() && RealmUtils.isValid(realm)) {
                // check if I have the same room
                val room = RealmUtils.readFirstFromRealmWithId(realm!!,
                    ChatRoom::class.java, "chatRoomID", chatRoomID) as? ChatRoom

                return (room?.isValid() == true) to room
            }

            return false to null
        }

        fun checkRoomByParticipant(participantID: String?, realm: Realm?): Pair<Boolean, ChatRoom?> {
            if (!participantID.isNullOrBlank() && RealmUtils.isValid(realm)) {
                // check if I have the same room
                val room =
                    RealmUtils.readFromRealm(realm!!, ChatRoom::class.java)?.firstOrNull {
                        (it as ChatRoom).getParticipant(participantID) != null
                    } as? ChatRoom

                return (room?.isValid() == true) to room
            }

            return false to null
        }

    }

    var date: String = nowToDBDate()
    var dateObj: Date? = null
    var text: String = ""
    var recipientStatus: Int = 0
    var participants = RealmList<Participant>()
    var roomName: String? = null
    var toRead: Int = 0
    var earnings: Double = 0.0
//    var participantAvatar: String? = null
//    var lastSeenDate: String? = null



    //region == Class custom methods ==

    fun getRecipientStatus(): ChatRecipientStatus {
        return when (recipientStatus) {
            0 -> ChatRecipientStatus.OFFLINE
            1 -> ChatRecipientStatus.ONLINE
            else -> ChatRecipientStatus.OFFLINE
        }
    }

    fun isValid(): Boolean {
        return !chatRoomID.isNullOrBlank()/* && !ownerID.isNullOrBlank() && participantIDs.isNotEmpty()*/
    }

    fun getRoomAvatar(id: String? = null): String? {
        return getParticipant(id)?.avatarURL
    }

    fun getRoomName(id: String? = null): String? {
        return getParticipant(id)?.name
    }

    fun getLastSeenDate(id: String? = null): Date? {
        return getParticipant(id)?.lastSeenDate?.getDateFromDB()
    }

    fun getParticipantId(only: Boolean = true): String? {
        return if (!participantIDs.isNullOrEmpty() && only) participantIDs[0] else null
    }

    fun canVoiceCallParticipant(id: String? = null): Boolean {
        return getParticipant(id)?.canAudiocall?: false
    }

    fun canVideoCallParticipant(id: String? = null): Boolean {
        return getParticipant(id)?.canVideocall?: false
    }

    fun getParticipant(id: String? = null): Participant? {
        return if (!participants.isNullOrEmpty()) {
            if (id == null) participants[0]
            else {
                val filtered = participants.filter { it.id == id }
                if (!filtered.isNullOrEmpty() && filtered.size == 1)
                    filtered[0]
                else null
            }
        } else null
    }

    fun getParticipantIDsList(): Array<String>? {
        return if (!participantIDs.isNullOrEmpty()) {
            participantIDs.toTypedArray()
        } else null
    }

    //endregion


    //region == Realm listener ==

    override fun onChange(t: ChatRoom) {
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

    override fun write(realm: Realm?) {
        RealmUtils.writeToRealm(realm, this)
    }

    override fun write(`object`: Any?) {}

    override fun write(json: JSONObject?) {
        update(json)
    }

    override fun write(realm: Realm?, model: RealmModel?) {}

    override fun update() {}

    override fun update(`object`: Any?) {
        if (`object` is ChatRoom) {
            chatRoomID = `object`.chatRoomID
            ownerID = `object`.ownerID
            participantIDs = `object`.participantIDs
            participants = `object`.participants
            date = `object`.date
            recipientStatus = `object`.recipientStatus
            toRead = `object`.toRead
            roomName = `object`.roomName
            toRead = `object`.toRead
        }
    }

    override fun update(json: JSONObject?) {
        if (json != null) deserialize(json)
    }

    override fun updateWithReturn(): RealmModelListener? {
        return null
    }

    override fun updateWithReturn(`object`: Any?): RealmModelListener {
        update(`object`)
        return this
    }

    override fun updateWithReturn(json: JSONObject?): RealmModelListener {
        return deserialize(json.toString()) as RealmModelListener
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

}