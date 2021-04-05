package rs.highlande.app.tatatu.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created by Abhin.
 */
data class SendInvitationResponse(@SerializedName("_id") @Expose var id: String? = null, @SerializedName("responseStatus") @Expose var responseStatus: Int? = null, @SerializedName("error") @Expose var error: String? = null, @SerializedName("description") @Expose var description: String? = null)