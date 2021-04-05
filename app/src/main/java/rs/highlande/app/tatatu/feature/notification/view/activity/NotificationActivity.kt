package rs.highlande.app.tatatu.feature.notification.view.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.chat.view.ChatActivity
import rs.highlande.app.tatatu.feature.main.view.MainActivity
import rs.highlande.app.tatatu.feature.post.PostActivity

class NotificationActivity: BaseActivity() {

    private val sharedPreferences: PreferenceHelper by inject()

    override fun configureLayout() {}

    override fun bindLayout() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.d("notificationTEST", "notificationONCREATE")
        manageIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_RESULT_NOTIFICATION && resultCode == Activity.RESULT_OK) finish()
    }

    override fun manageIntent() {
        intent.extras?.let { extras ->

            val id = extras.getString(BUNDLE_NOTIFICATION_ID)
            val type = extras.getString(BUNDLE_NOTIFICATION_TYPE)
            if (id.isNullOrBlank() || type.isNullOrBlank()) {
                finish()
            }
            LogUtils.d("notificationTYPE", type)
            LogUtils.d("notificationID", id)

            when(type) {
                NOTIFICATION_TYPE_PROFILE -> {
                    AccountActivity.openProfileFragment(
                        this,
                        id!!,
                        fromNotification = true,
                        appForeground = extras.getBoolean(BUNDLE_NOTIFICATION_APP_FOREGROUND),
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                NOTIFICATION_TYPE_FEED -> {
                    PostActivity.openPostDetailFragment(
                        this,
                        sharedPreferences.getUserId(),
                        id!!,
                        true,
                        extras.getBoolean(BUNDLE_NOTIFICATION_APP_FOREGROUND),
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                NOTIFICATION_TYPE_CHAT -> {
                    startActivity(Intent(this@NotificationActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        LogUtils.d("notificationTEST", "notificationNEWINTENT")
        manageIntent()
    }
}