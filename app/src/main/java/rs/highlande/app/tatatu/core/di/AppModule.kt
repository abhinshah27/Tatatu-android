package rs.highlande.app.tatatu.core.di

import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import rs.highlande.app.tatatu.connection.webSocket.WebSocketConnection
import rs.highlande.app.tatatu.connection.webSocket.WebSocketTracker
import rs.highlande.app.tatatu.connection.webSocket.realTime.RTCommHelper
import rs.highlande.app.tatatu.core.cache.AudioVideoCache
import rs.highlande.app.tatatu.core.cache.DownloadReceiver
import rs.highlande.app.tatatu.core.cache.PicturesCache
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.util.ForegroundManager
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.core.util.ResourcesHelper
import rs.highlande.app.tatatu.feature.account.followFriendList.api.FriendsApi
import rs.highlande.app.tatatu.feature.account.followFriendList.repository.FriendRepository
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.*
import rs.highlande.app.tatatu.feature.account.profile.api.ProfileApi
import rs.highlande.app.tatatu.feature.account.profile.repository.ProfileRepository
import rs.highlande.app.tatatu.feature.account.profile.view.ProfileViewModel
import rs.highlande.app.tatatu.feature.account.profile.view.viewmodel.ProfileEditViewModel
import rs.highlande.app.tatatu.feature.account.settings.api.SettingsApi
import rs.highlande.app.tatatu.feature.account.settings.repository.SettingsRepository
import rs.highlande.app.tatatu.feature.account.settings.view.viewModel.*
import rs.highlande.app.tatatu.feature.authentication.AuthViewModel
import rs.highlande.app.tatatu.feature.authentication.api.AuthApi
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.authentication.repository.AuthRepository
import rs.highlande.app.tatatu.feature.authentication.view.forgot.ForgotViewModel
import rs.highlande.app.tatatu.feature.authentication.view.login.LoginViewModel
import rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels.SignUpAddUserViewModel
import rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels.SignUpViewModel
import rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels.SignUpWelcomeViewModel
import rs.highlande.app.tatatu.feature.chat.WebLinkRecognizer
import rs.highlande.app.tatatu.feature.chat.api.ChatApi
import rs.highlande.app.tatatu.feature.chat.chatRoomList.ChatRoomsViewModel
import rs.highlande.app.tatatu.feature.chat.onHold.VideoViewVModel
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.feature.chat.view.viewModel.ChatMessagesViewModel
import rs.highlande.app.tatatu.feature.chat.view.viewModel.ChatOrCallCreationViewModel
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.commonApi.UsersApi
import rs.highlande.app.tatatu.feature.commonRepository.PostRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonView.UsersViewModel
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewModel
import rs.highlande.app.tatatu.feature.createPost.api.CreatePostApi
import rs.highlande.app.tatatu.feature.createPost.repository.CreatePostRepository
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostViewModel
import rs.highlande.app.tatatu.feature.home.api.HomeApi
import rs.highlande.app.tatatu.feature.home.repository.HomeRepository
import rs.highlande.app.tatatu.feature.home.view.HomeViewModel
import rs.highlande.app.tatatu.feature.inviteFriends.api.InviteFriendsApi
import rs.highlande.app.tatatu.feature.inviteFriends.repository.InviteFriendsRepository
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.ContactViewModel
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.EmailBottomSheetViewModel
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.InviteFriendsViewModel
import rs.highlande.app.tatatu.feature.multimediaContent.api.MultimediaApi
import rs.highlande.app.tatatu.feature.multimediaContent.repository.MultimediaRepository
import rs.highlande.app.tatatu.feature.multimediaContent.view.PlaylistDetailViewModel
import rs.highlande.app.tatatu.feature.multimediaContent.view.PlaylistViewModel
import rs.highlande.app.tatatu.feature.notification.api.NotificationApi
import rs.highlande.app.tatatu.feature.notification.repository.NotificationRepository
import rs.highlande.app.tatatu.feature.notification.view.viewModel.NotificationViewModel
import rs.highlande.app.tatatu.feature.post.api.PostApi
import rs.highlande.app.tatatu.feature.post.detail.viewModel.PostDetailViewModel
import rs.highlande.app.tatatu.feature.post.like.viewmodel.PostLikeViewModel
import rs.highlande.app.tatatu.feature.post.timeline.viewmodel.PostTimelineViewModel
import rs.highlande.app.tatatu.feature.search.repository.SearchRepository
import rs.highlande.app.tatatu.feature.search.view.viewModel.SearchViewModel
import rs.highlande.app.tatatu.feature.suggested.api.SuggestedApi
import rs.highlande.app.tatatu.feature.suggested.repository.SuggestedRepository
import rs.highlande.app.tatatu.feature.suggested.view.SuggestedViewModel
import rs.highlande.app.tatatu.feature.voiceVideoCalls.view.CallViewModel
import rs.highlande.app.tatatu.feature.wallet.api.WalletApi
import rs.highlande.app.tatatu.feature.wallet.repository.WalletRepository
import rs.highlande.app.tatatu.feature.wallet.view.viewModel.WalletViewModel

val appModule = module {

    //Helpers
    single { PreferenceHelper(androidContext()) }
    single { ResourcesHelper(androidContext()) }

    // ForegroundManager
    single { ForegroundManager() }

    // Repositories
    single { UsersRepository() }
    single { PostRepository() }
    single { FriendRepository() }
    single { ChatRepository() }

    // APIs
    single { CommonApi() }
    single { PostApi() }
    single { UsersApi() }
    single { ChatApi() }

    //ViewModels
    viewModel { UsersViewModel() }
    viewModel { WebViewModel() }

    //Managers
    single { RelationshipManager(get()) }

}

val homeModule = module {

    //ViewModels
    viewModel { HomeViewModel(get(), get(), get(), get()) }

    // Repositories
    single { HomeRepository() }
    single { HomeApi() }


}

val createPostModule = module {
    //ViewModels
    viewModel { CreatePostViewModel(androidApplication()) }

    // Repositories
    single { CreatePostRepository() }
    single { CreatePostApi() }
}

val inviteModule = module {
    //Api
    single { InviteFriendsApi() }

    //Repository
    single { InviteFriendsRepository() }

    //ViewModels
    viewModel { ContactViewModel() }
    viewModel { EmailBottomSheetViewModel() }
    viewModel { InviteFriendsViewModel(androidApplication()) }
}


val postModule = module {
    //ViewModels
    viewModel { PostTimelineViewModel(androidApplication(), get(), get(), get()) }
    viewModel { PostDetailViewModel(androidApplication(), get(), get(), get()) }
    viewModel { PostLikeViewModel(get(), get()) }
}

val profileModule = module {

    //Repository
    single { ProfileRepository() }
    single { ProfileApi() }
    single { FriendsApi() }

    //ViewModels
    viewModel { ProfileViewModel(get(), get(), get(), get()) }

    viewModel { FollowingViewModel(get(), get()) }
    viewModel { FollowersViewModel(get(), get()) }
    viewModel { FollowRequestsViewModel(get(), get()) }
    viewModel { SentFollowRequestsViewModel(get(), get()) }

    viewModel { FriendsViewModel(get(), get()) }
    viewModel { InvitesViewModel(get(), get()) }
    viewModel { FollowTabsViewModel(get(), get()) }

    viewModel { ProfileEditViewModel(androidApplication()) }


}

val loginSignupModule = module {

    //ViewModels
    viewModel { LoginViewModel(androidContext()) }
    viewModel { ForgotViewModel(androidApplication()) }
    viewModel { SignUpViewModel(androidContext()) }
    viewModel { SignUpAddUserViewModel(androidApplication()) }
    viewModel { SignUpWelcomeViewModel() }
    viewModel { AuthViewModel() }

    // Repositories
    single { AuthRepository(androidContext()) }
    single { AuthApi() }

}


val suggestedModule = module {

    //ViewModels
    viewModel { SuggestedViewModel(get()) }

    // Repositories
    single { SuggestedRepository() }
    single { SuggestedApi() }
}


val connectionModule = module {

    single { WebSocketConnection(androidContext()) }
    single { WebSocketTracker() }
    single { RTCommHelper(androidContext()) }

    // Auth
    single { AuthManager(androidContext()) }

}


val multimediaModule = module {

    //ViewModels
    viewModel { PlaylistViewModel() }
    viewModel { PlaylistDetailViewModel(get()) }
    viewModel { SearchViewModel(get()) }

    // Repositories
    single { SearchRepository(get(), get()) }
    single { MultimediaRepository() }
    single { MultimediaApi() }
}


val settingsModule = module {

    //Api
    single { SettingsApi() }

    //Repository
    single { SettingsRepository() }

    //ViewModels
    viewModel { SettingsViewModel() }
    viewModel { SettingsBlockAccountViewModel() }
    viewModel { SettingsThemePrivacyViewModel() }
    viewModel { SettingsNotificationsViewModel() }
    viewModel { SettingsUpgradeAccountViewModel() }
    viewModel { SettingsUpgradeAccountVerifyNowViewModel() }
    viewModel { SettingsUpgradeAccountVerifyNowEmailViewModel() }
    viewModel { SettingsCompleteYourProfileViewModel() }
}

val walletModule = module {

    //Api
    single { WalletApi() }

    //Repository
    single { WalletRepository() }

    //ViewModels
    viewModel { WalletViewModel() }
}


val notificationModule = module {
    //ViewModels
    viewModel { NotificationViewModel() }

    // Repositories
    single { NotificationApi() }
    single { NotificationRepository() }
}

val callModule = module {
    //ViewModels
    viewModel { CallViewModel(androidApplication()) }
}

val chatModule = module {

    //ViewModel
    viewModel { ChatMessagesViewModel(androidApplication(), get(), get(), get()) }
    viewModel { ChatRoomsViewModel(get(), get()) }
    viewModel { ChatOrCallCreationViewModel(get(), get()) }
    viewModel {
        VideoViewVModel(
            androidApplication()
        )
    }

    single {
        WebLinkRecognizer(
            androidContext()
        )
    }

}

val cacheModule = module {

    single { AudioVideoCache(androidContext()) }
    single { PicturesCache(androidContext()) }
    single { DownloadReceiver() }

}
