package rs.highlande.app.tatatu.feature.commonView.webView

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.widget.PopupMenu
import kotlinx.android.synthetic.main.common_toolbar_web_view.*
import kotlinx.android.synthetic.main.fragment_webview.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.applyFont
import rs.highlande.app.tatatu.core.util.fireOpenBrowserIntent


/**
 * Created by Abhin.
 */
class WebViewFragment : BaseFragment() {

    enum class WVType { PRIVACY, TOS, NEWS, CHAT }
    private var wvType: WVType? = null

    private val mWebViewModel: WebViewModel by sharedViewModel()
    var popup: PopupMenu? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        img_back_arrow?.setOnClickListener {
            if (webView?.canGoBack()!!) {
                webView?.goBack()
                if (webView?.goBack() != null) {
                    progressbar?.visibility = View.VISIBLE
                }
            } else {
                activity?.supportFragmentManager?.popBackStack()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        wvType = arguments?.getSerializable(BUNDLE_KEY_WEV_TYPE) as? WVType

        txt_title?.text = mWebViewModel.mToolbarName
        webLoad(mWebViewModel.mWebUrl)
        img_options.setOnClickListener {
            showPopup(it!!)
        }

        srl?.setOnRefreshListener {
            webView.loadUrl(mWebViewModel.mWebUrl)
        }
    }

    override fun onStart() {
        super.onStart()

        wvType?.let {
            AnalyticsUtils.trackScreen(
                logTag,
                when (wvType) {
                    WVType.PRIVACY -> "PrivacyPolicy"
                    WVType.TOS -> "TermsOfUse"
                    WVType.NEWS -> "NewsPost"
                    WVType.CHAT -> "ChatMessage"
                    else -> ""
                }
            )
        }
    }

    private fun showPopup(v: View) {

        if (popup == null) {
            popup = PopupMenu(context!!, v)
            val inflater = popup!!.menuInflater
            inflater.inflate(R.menu.menu_webview, popup!!.menu)
            popup!!.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem?): Boolean {
                    when (item!!.itemId) {
                        R.id.menu_open_in_browser -> {
                            fireOpenBrowserIntent(activity!! as BaseActivity, mWebViewModel.mWebUrl)
                            return true
                        }
//                        R.id.menu_cancel -> {
//                            popup!!.dismiss()
//                            return true
//                        }
                        else -> return false
                    }
                }
            })

            popup!!.menu.findItem(R.id.menu_open_in_browser).applyFont(v.context, R.font.lato)
        }

        popup!!.show()
    }


    // TODO: 2019-08-08    keep share action for post
    fun openShareIntent() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
        }.putExtra(Intent.EXTRA_TEXT, Uri.parse(mWebViewModel.mWebUrl))
        startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.choose_to)))
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun webLoad(Url: String?) {
        try {
            val webSettings = webView!!.settings
            webSettings.javaScriptEnabled = true
            webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            webSettings.setAppCacheEnabled(true)
            webSettings.domStorageEnabled = true
            webSettings.setSupportZoom(true)
            webSettings.loadWithOverviewMode =true
            webSettings.builtInZoomControls = true
            webSettings.displayZoomControls = false

            // INFO: 2019-11-14    TRIED and NOT OK
//            if (hasQ()) {
//                webSettings.forceDark =
//                    if (sharedPrefs.isDarkTheme()) WebSettings.FORCE_DARK_ON
//                    else WebSettings.FORCE_DARK_OFF
//            }

            webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
            webSettings.useWideViewPort = true
            webView!!.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            webView!!.webChromeClient = MyChromeClient()
            webView!!.webViewClient = MyWebViewClient()
            webView!!.loadUrl(Url)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class MyChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            progressbar?.progress = newProgress
            if (newProgress == 100) {
                progressbar?.visibility = View.GONE
            }
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            progressbar?.visibility = View.VISIBLE
            view.loadUrl(url)
            return true
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            LogUtils.d("onLoadResource", "onLoadResource --> $url")
            super.onLoadResource(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            LogUtils.d("onPageStarted", "onPageStarted --> $url")
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            srl?.isRefreshing = false
        }
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}

    companion object {

        private const val BUNDLE_KEY_WEV_TYPE = "bundle_key_wv_type"

        val logTag = WebViewFragment::class.java.simpleName

        fun newInstance(wvType: WVType): WebViewFragment {

            return WebViewFragment().apply {

                arguments = Bundle().apply {
                    putSerializable(BUNDLE_KEY_WEV_TYPE, wvType)
                }
            }

        }

    }

}