package rs.highlande.app.tatatu.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.JsonHelper
import rs.highlande.app.tatatu.model.User.UserDeserializer
import java.io.Serializable
import java.lang.reflect.Type


/**
 * Enum representing all possible values of a TTU [Relationship].
 */
enum class Relationship(val value: String) {

    @SerializedName(value = "NA") NA("NA"),
    @SerializedName(value = "following") FOLLOWING("following"),
    @SerializedName(value = "follower") FOLLOWER("follower"),
    @SerializedName(value = "friends") FRIENDS("friends"),
    @SerializedName(value = "pendingFollow") PENDING_FOLLOW("pendingFollow"),
    @SerializedName(value = "declined") DECLINED("declined"),
    @SerializedName(value = "myself") MYSELF("myself");

    @StringRes fun toStringForButton(): Int {
        return when (value) {
            FOLLOWING.value, FRIENDS.value -> R.string.profile_message
            FOLLOWER.value -> R.string.profile_followback
            PENDING_FOLLOW.value -> R.string.profile_requested
            else -> R.string.profile_follow
        }
    }

    companion object {

        fun toEnum(value: String?): Relationship {
            return value?.let {
                when (value) {
                    FOLLOWING.value -> FOLLOWING
                    FOLLOWER.value -> FOLLOWER
                    FRIENDS.value -> FRIENDS
                    PENDING_FOLLOW.value -> PENDING_FOLLOW
                    MYSELF.value -> MYSELF
                    else -> NA
                }
            } ?: NA
        }
    }
}

/**
 * Enum representing all possible values of an [AccountType].
 */
enum class AccountType(val value: String) {
    @SerializedName(value = "normal") NORMAL("normal"),
    @SerializedName(value = "publicFigure") PUBLIC_FIGURE("publicFigure"),
    @SerializedName(value = "celebrity") CELEBRITY("celebrity"),
    @SerializedName(value = "charity") CHARITY("charity"),
    @SerializedName(value = "channel") CHANNEL("channel");

    fun toString(context: Context): String {
        return when (value) {
            PUBLIC_FIGURE.value -> context.getString(R.string.type_account_public_figure)
            CELEBRITY.value -> context.getString(R.string.type_account_celebrity)
            CHARITY.value -> context.getString(R.string.type_account_charity)

            // INFO: 2019-09-05    CHANNEL behaves like NORMAL type
//            CHANNEL.value -> context.getString(R.string.type_account_channel)
            
            else -> ""
        }
    }

    companion object {

        fun toEnum(value: String?): AccountType {
            return value?.let {
                when (value) {
                    PUBLIC_FIGURE.value -> PUBLIC_FIGURE
                    CELEBRITY.value -> CELEBRITY
                    CHARITY.value -> CHARITY
                    CHANNEL.value -> CHANNEL
                    else -> NORMAL
                }
            } ?: NORMAL
        }
    }

}

// TODO: 2019-07-30    consider if it still stands
@Parcelize
data class AccountDetails(
    var bio: String? = null,
    var website: String? = null,
    var email: String? = null,
    var phoneNo: String? = null,
    var password: String = "",
    var country: String? = null,
    var gender: String? = null,
    var dateOfBirth: String? = null
) : Parcelable {

    companion object {
        fun get(json: JSONObject): AccountDetails? {
            return JsonHelper.deserialize(json) as? AccountDetails
        }
    }

}


/**
 * Class holding reference for all base properties of a [User] like ID, username, etc.
 * It serves as a parent of [User] class.
 */
@Parcelize
open class MainUserInfo(
    var uid: String = "",
    var username: String = "",
    var name: String = "",
    var picture: String = "",
    var verified: Boolean = false,
    var isPublic: Boolean = false,
    var accountType: AccountType = AccountType.NORMAL,
    var TTUId: String = ""
) : Serializable, Parcelable, BaseDiffutilImpl<MainUserInfo> {

    constructor() : this("")

    // INFO: 2019-09-04    removes temporarily username as constrain for valid user until further info are provided
    fun isValid() = !uid.isBlank() && /*!username.isBlank() && */!name.isBlank()

    //TODO: 2019-07-18 define where this is going to come from
    var hasNewMoment: Boolean = false

    fun isCelebrity() = accountType == AccountType.CELEBRITY

    fun isPrivate() = !isPublic

    companion object {
        fun get(json: JSONObject): MainUserInfo? {
            return JsonHelper.deserialize(json) as? MainUserInfo
        }
    }


    override fun areItemsTheSame(other: MainUserInfo): Boolean {
        return uid == other.uid
    }

    override fun areContentsTheSame(other: MainUserInfo): Boolean {
        return username == other.username &&
                name == other.name &&
                picture == other.picture &&
                verified == other.verified &&
                isPublic == other.isPublic &&
                accountType == other.accountType &&
                TTUId == other.TTUId
    }
}


/**
 * Main class holding all references of a TTU [User].
 * The user is composed by two objects: [DetailUserInfo] and [BalanceUserInfo].
 * Being child class of [MainUserInfo], it already has all inherited properties.
 *
 * The call to get a [User] is composable with 3 elements: "MainUserInfo", "DetailsUserInfo", and "BalanceUserInfo".
 * JSON results for [User] can be delivered with or without [MainUserInfo] object, depending on the context of the call.
 * For this reason a [UserDeserializer] class is provided to handle all possible cases and deserialize object accordingly.
 */
@Parcelize
class User(
    @SerializedName(value = "DetailsUserInfo") var detailsInfo: DetailUserInfo = DetailUserInfo(),
    @SerializedName(value = "BalanceUserInfo") var balanceInfo: BalanceUserInfo = BalanceUserInfo(),
    @SerializedName(value = "PrivateUserInfo") var privateInfo: PrivateUserInfo = PrivateUserInfo()
) : MainUserInfo() {

    constructor() : this(DetailUserInfo(), BalanceUserInfo(), PrivateUserInfo())

    // TODO: 2019-08-07    Reactivate PARCELER impl if needed
    companion object/* : Parceler<User>*/ {
        fun get(json: JSONObject): User? {
            return JsonHelper.deserialize(json) as? User
        }

        // TODO: 2019-08-07    Reactivate PARCELER impl if needed
//        override fun create(parcel: Parcel): User {
//            return User().apply {
//                parcel.apply {
//                    uid = readString() ?: ""
//                    username = readString() ?: ""
//                    name = readString() ?: ""
//                    picture = readString() ?: ""
//
//                    verified = if (hasQ()) readBoolean() else (readInt() == 1)
//                    isPublic = if (hasQ()) readBoolean() else (readInt() == 1)
//                    accountType = readSerializable() as AccountType
//
//                    detailsInfo = readParcelable<DetailUserInfo>(DetailUserInfo::class.java.classLoader) ?: DetailUserInfo()
//                    balanceInfo = readParcelable<BalanceUserInfo>(BalanceUserInfo::class.java.classLoader) ?: BalanceUserInfo()
//                }
//            }
//        }
//
//        override fun User.write(parcel: Parcel, flags: Int) {
//            parcel.writeString(uid)
//            parcel.writeString(username)
//            parcel.writeString(name)
//            parcel.writeString(picture)
//
//            if (hasQ()) parcel.writeBoolean(verified)
//            else parcel.writeInt(if (verified) 1 else 0)
//
//            if (hasQ()) parcel.writeBoolean(isPublic)
//            else parcel.writeInt(if (isPublic) 1 else 0)
//
//            parcel.writeSerializable(accountType)
//            parcel.writeParcelable(detailsInfo, flags)
//            parcel.writeParcelable(balanceInfo, flags)
//        }
    }

    override fun areContentsTheSame(other: MainUserInfo): Boolean {
        return super.areContentsTheSame(other) &&
                other is User &&
                detailsInfo.areContentsTheSame(other.detailsInfo) &&
                balanceInfo.areContentsTheSame(other.balanceInfo) &&
                privateInfo.areContentsTheSame(other.privateInfo)
    }


    /**
     * Class implementing [JsonDeserializer] of type [User] that deserializes the [MainUserInfo] interface.
     */
    class UserDeserializer : JsonDeserializer<User> {

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): User {
            if (json != null) {

                return Gson().fromJson(json, User::class.java).apply {
                    json.asJsonObject.apply {
                        if (has("MainUserInfo")) {
                            val mainInfo = get("MainUserInfo").asJsonObject
                            uid = mainInfo.get("uid").asString
                            username = mainInfo.get("username").asString
                            name = mainInfo. get("name").asString
                            picture = mainInfo.get("picture").asString
                            verified = mainInfo.get("verified").asBoolean
                            isPublic = mainInfo.get("isPublic").asBoolean
                            accountType = AccountType.toEnum(mainInfo.get("accountType").asString)
                            TTUId = mainInfo.get("TTUId").asString

                        }
                    }
                }

            }

            return User()
        }

    }

}


/**
 * Holds reference to properties related to TTU tokens balance and friends count.
 */
@Parcelize
data class BalanceUserInfo(
    var friendsCount: Int = 0,
    var balance: Double = 0.0
) : Parcelable, BaseDiffutilImpl<BalanceUserInfo> {
    constructor() : this(0, 0.0)

    override fun areItemsTheSame(other: BalanceUserInfo): Boolean {
        return true
    }

    override fun areContentsTheSame(other: BalanceUserInfo): Boolean {
        return friendsCount == other.friendsCount &&
                balance == other.balance
    }
}

/**
 * Holds reference to all properties related to the display in the profile section.
 */
@Parcelize
data class DetailUserInfo(
    var details: String = "",
    var website: String = "",
    var bio: String = "",
    var donatingUid: String = "",
    var donatingInfo: String = "",
    var followersCount: Int = 0,
    var followingCount: Int = 0,
    var requestID: String? = "",      // the current follow request id between the showed user and us
    var relationship: Relationship = Relationship.NA,
    var postsCount: Int = 0,
    var momentsCount: Int = 0,      // next release
    var tagsCount: Int = 0         // next release
) : Parcelable, BaseDiffutilImpl<DetailUserInfo> {
    constructor() : this("")

    override fun areItemsTheSame(other: DetailUserInfo): Boolean {
        return true
    }

    override fun areContentsTheSame(other: DetailUserInfo): Boolean {
        return details == other.details &&
                donatingUid == other.donatingUid &&
                donatingInfo == other.donatingInfo &&
                followersCount == other.followersCount &&
                followingCount == other.followingCount &&
                requestID == other.requestID &&
                relationship == other.relationship &&
                postsCount == other.postsCount
    }
}

/**
 * Holds reference to all properties related to the display in the edit profile seciton.
 */
@Parcelize
data class PrivateUserInfo(
    var email: String? = null,
    var password: String = "",
    var phoneNo: String? = null,
    var country: String? = null,
    var gender: String? = null,
    var dateOfBirth: String? = null
) : Parcelable, BaseDiffutilImpl<PrivateUserInfo> {
    constructor() : this("")

    override fun areItemsTheSame(other: PrivateUserInfo): Boolean {
        return true
    }

    override fun areContentsTheSame(other: PrivateUserInfo): Boolean {
        return email == other.email &&
                password == other.password &&
                phoneNo == other.phoneNo &&
                country == other.country &&
                gender == other.gender &&
                dateOfBirth == other.dateOfBirth
    }
}