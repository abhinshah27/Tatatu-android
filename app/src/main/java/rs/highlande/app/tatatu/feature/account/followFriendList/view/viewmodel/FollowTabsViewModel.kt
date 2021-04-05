package rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel

import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.feature.account.common.BaseAccountViewModel
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class FollowTabsViewModel(usersRepository: UsersRepository, relationshipManager: RelationshipManager): BaseAccountViewModel(usersRepository, relationshipManager) {

    override fun performSearch(query: String) {}
}