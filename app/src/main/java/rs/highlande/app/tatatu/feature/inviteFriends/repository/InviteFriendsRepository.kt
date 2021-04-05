package rs.highlande.app.tatatu.feature.inviteFriends.repository

import android.os.Bundle
import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.feature.inviteFriends.api.InviteFriendsApi

/**
 * Created by Abhin.
 */
class InviteFriendsRepository : BaseRepository() {
    private val mInviteFriendsApi: InviteFriendsApi by inject()

    //get Invitation Link
    fun getInvitationLink(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return mInviteFriendsApi.getInvitationLink(caller, bundle)
    }

    //do Send Invitation
    fun doSendInvitation(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return mInviteFriendsApi.doSendInvitation(caller, bundle)
    }
}