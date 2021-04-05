package rs.highlande.app.tatatu.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created by Abhin.
 */
data class BlockAccountDataList(@SerializedName("accountType") @Expose var accountType: String? = null, @SerializedName("uid") @Expose var uid: String? = null, @SerializedName("name") @Expose var name: String? = null, @SerializedName("username") @Expose var username: String? = null, @SerializedName("picture") @Expose var picture: String? = null, @SerializedName("verified") @Expose var verified: Boolean? = null, @SerializedName("isPublic") @Expose var isPublic: Boolean? = null)
