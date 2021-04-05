package rs.highlande.app.tatatu.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created by Abhin.
 */
data class InvitationLinkResponse(@SerializedName("invitationLink") @Expose var invitationLink: String? = null)