package rs.highlande.app.tatatu.feature.authentication

import android.os.Bundle
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.fireOpenEmailIntent

/**
 * Middle activity supporting Gmail intent. It simulates a redirection otherwise impossible without it.
 * @author mbaldrighi on 2020-01-10.
 */
class InvisibleGmailActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fireOpenEmailIntent(this)
    }

    override fun configureLayout() {}

    override fun bindLayout() { }

    override fun manageIntent() {}
}