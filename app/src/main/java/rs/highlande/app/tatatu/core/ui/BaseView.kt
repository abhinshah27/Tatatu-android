package rs.highlande.app.tatatu.core.ui

import androidx.annotation.StringRes

interface BaseView {
    fun showMessage(message: String)
    fun showMessage(@StringRes message: Int)
    fun showError(error: String)

    fun showLoader(@StringRes message: Int? = null)
    fun showLoader(message: String? = null)
    fun hideLoader()

}