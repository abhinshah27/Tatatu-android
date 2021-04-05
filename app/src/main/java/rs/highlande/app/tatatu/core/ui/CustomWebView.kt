package rs.highlande.app.tatatu.core.ui

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.webkit.WebView

/**
 * Created by Abhin.
 */
class CustomWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WebView(context.createConfigurationContext(Configuration()), attrs, defStyleAttr, defStyleRes)