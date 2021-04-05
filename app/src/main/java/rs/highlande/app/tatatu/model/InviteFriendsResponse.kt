package rs.highlande.app.tatatu.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import rs.highlande.app.tatatu.core.util.getDateDifferenceFromNowInDay


/**
 * Created by Abhin.
 */

data class InviteFriendsResponse(@SerializedName("pendingCount") @Expose var pendingCount: Int = 0, @SerializedName("signedCount") @Expose var signedCount: Int? = null, @SerializedName("users") @Expose var users: ArrayList<Users>? = null) {
    fun isCelebrity(accountType: String?): Boolean {
        return accountType == AccountType.CELEBRITY.value
    }
}

data class FriendsResponse(@SerializedName("pendingCount") @Expose var pendingCount: Int? = null, @SerializedName("signedCount") @Expose var signedCount: Int? = null, @SerializedName("users") @Expose var user: ArrayList<FriendsUser>? = null)

data class Users(@SerializedName("name") @Expose var name: String? = null, @SerializedName("indentifier") @Expose var indentifier: String? = null, @SerializedName("picture") @Expose var picture: String? = null, @SerializedName("invitationType") @Expose var invitationType: String? = null, @SerializedName("lastInvitedDate") @Expose var lastInvitedDate: String? = null, var ifMoreThanSevenDays: Boolean? = false) {

    fun canResend(): Boolean {
        return lastInvitedDate?.let {
            getDateDifferenceFromNowInDay(lastInvitedDate!!) >= 7
        } ?: false
    }

}


data class MainUser(@SerializedName("accountType") @Expose var accountType: String? = null,@SerializedName("TTUId") @Expose var tTUId: String? = null, @SerializedName("uid") @Expose var uid: String? = null, @SerializedName("name") @Expose var name: String? = null, @SerializedName("username") @Expose var username: String? = null, @SerializedName("picture") @Expose var picture: String? = null, @SerializedName("verified") @Expose var verified: Boolean? = null, @SerializedName("isPublic") @Expose var isPublic: Boolean? = null)

data class BalanceUser(@SerializedName("balance") @Expose var balance: Double? = null, @SerializedName("friendsCount") @Expose var friendsCount: Int? = null)

data class FriendsUser(@SerializedName("MainUserInfo") @Expose var mainUserInfo: MainUser? = null, @SerializedName("BalanceUserInfo") @Expose var balanceUserInfo: BalanceUser? = null){
    fun isCelebrity(): Boolean {
        return mainUserInfo!!.accountType == AccountType.CELEBRITY.value
    }

}