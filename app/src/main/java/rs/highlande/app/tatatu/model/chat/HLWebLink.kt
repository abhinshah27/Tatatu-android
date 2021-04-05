package rs.highlande.app.tatatu.model.chat

import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import io.realm.Realm
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.json.JSONObject
import rs.highlande.app.tatatu.core.util.JsonHelper
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.model.RealmModelListener


@RealmClass
open class HLWebLink : RealmModel,
    RealmModelListener, JsonHelper.JsonDeSerializer {

    companion object {
        fun get(json: JSONObject): HLWebLink? {
            return JsonHelper.deserialize(json) as? HLWebLink
        }
    }


    //region == Getters and setters ==

    @Expose
    var ogDescription: String? = null
    @Expose
    var ogImage: String? = null
    @Expose
    var title: String? = null
    @Expose
    var mediaURL: String? = null
    @Expose
    var source: String? = null
    @PrimaryKey @Expose
    var link: String? = null
    @Expose
    var messageID: String? = null

    override val selfObject: Any
        get() = this

    fun deserializeToClass(json: JSONObject) = deserialize(json) as HLWebLink


    //region == Realm model methods ==

    override fun reset() {}

    override fun read(realm: Realm?): Any? = read(realm, HLWebLink::class.java)

    override fun read(realm: Realm?, model: Class<out RealmModel>?): RealmModel? =
        RealmUtils.readFirstFromRealm(realm, this.javaClass)

    override fun deserializeStringListFromRealm() {}

    override fun serializeStringListForRealm() {}

    override fun write(realm: Realm?) {
        RealmUtils.writeToRealm(realm, this)
    }

    override fun write(json: JSONObject?) {}

    override fun write(obj: Any?) {}

    override fun write(realm: Realm?, model: RealmModel?) {}

    override fun update() {}

    override fun update(obj: Any?) {}

    override fun update(json: JSONObject?) {}

    override fun updateWithReturn(): RealmModelListener? = null

    override fun updateWithReturn(obj: Any?): RealmModelListener? = null

    override fun updateWithReturn(json: JSONObject?): RealmModelListener? = null

    //endregion


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
        return JsonHelper.deserialize(json)
    }

    override fun deserialize(jsonString: String): JsonHelper.JsonDeSerializer? {
        return JsonHelper.deserialize(jsonString)
    }

    //endregion

}

