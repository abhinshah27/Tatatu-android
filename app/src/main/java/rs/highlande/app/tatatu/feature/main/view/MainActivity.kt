package rs.highlande.app.tatatu.feature.main.view

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.common_activity_view_pager.view.*
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.ActivityMainBinding
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonView.BottomNavigationActivity
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.CallsSDKTokenUtils

class MainActivity: BottomNavigationActivity() {

    private val usersRepository by inject<UsersRepository>()

    companion object {
        val logTag = MainActivity::class.java.simpleName
    }

    private lateinit var mainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        configureLayout()

        if (CallsSDKTokenUtils.callToken.isNullOrBlank()) {
            //call for token generate
            CallsSDKTokenUtils.init(usersRepository.fetchCachedMyUserId())
        }
    }

    override fun onStart() {
        super.onStart()

        fetchUser(true)
    }

    override fun configureLayout() {
        setupBottomNavigation(mainBinding.container.bottomBar as BottomNavigationView, mainBinding.container.pager)
    }
    override fun bindLayout() {}
    override fun manageIntent() {}

}