/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.onHold

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.cache.PicturesCache
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import java.io.File

/**
 * @author mbaldrighi on 4/25/2018.
 */
class PhotoViewActivity : BaseActivity(),
    ShareHelper.ShareableProvider {

    private val picturesCache: PicturesCache by inject()

    override fun configureLayout() {}

    override fun bindLayout() {}

    override fun getUserID() = getUser()?.uid

    override fun getPostOrMessageID() = postOrMessageID

    private var urlToLoad: String? = null
    private var transitionName: String? = null
    private var fileToLoad: File? = null
    private var postOrMessageID: String? = null
    private var fromChat: Boolean = false

    private var mShareHelper: ShareHelper? = null


    //region == SHARE ==

    //val progressView: View? = null

//    val userID: String
//        get() = mUser.getId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view_constr)
//        setRootContent(R.id.root_content)
//        setProgressIndicator(R.id.progress)

        val decorView = window.decorView
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE)
        decorView.systemUiVisibility = uiOptions
        isImmersiveActivity = true

        supportPostponeEnterTransition()


        //		configureToolbar(findViewById(R.id.toolbar), "", false);


        manageIntent()

        findViewById<View>(R.id.close_btn).setOnClickListener { v -> onBackPressed() }
        findViewById<View>(R.id.share_btn).setOnClickListener { v -> mShareHelper!!.initOps(fromChat) }

        val mPhotoView = findViewById<ImageView>(R.id.photo_view)
        if (areStringsValid(urlToLoad, transitionName)) {
            if (hasLollipop())
                mPhotoView.transitionName = transitionName

            GlideApp.with(this)
                .asDrawable()
                .fitCenter()
                .load(if (fileToLoad != null) fileToLoad else urlToLoad)
                .dontAnimate()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        supportStartPostponedEnterTransition()
                        if (e != null)
                            LogUtils.e(LOG_TAG, e.message, e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        supportStartPostponedEnterTransition()
                        return false
                    }
                })
                .into(mPhotoView)
        }

        mShareHelper =
            ShareHelper(
                this,
                this
            )

    }

    override fun onResume() {
        super.onResume()

//        AnalyticstrackScreen(this, AnalyticsFEED_IMAGE_VIEWER)

        mShareHelper!!.onResume()
    }

    override fun onStop() {

        mShareHelper!!.onStop()

        super.onStop()
    }

    //	@Override
    //	public boolean onCreateOptionsMenu(Menu menu) {
    //		getMenuInflater().inflate(R.menu.menu_viewer_sharing, menu);
    //
    //		// Locate MenuItem with ShareActionProvider
    //		MenuItem item = menu.findItem(R.id.share);
    //
    //		// Fetch and store ShareActionProvider
    //		mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
    //        Intent shareIntent = new Intent(Intent.ACTION_SEND);
    //        shareIntent.setType("text/plain");
    //        shareIntent.putExtra(Intent.EXTRA_TEXT, transitionName);
    //        mShareActionProvider.setShareIntent(shareIntent);
    //
    //		return true;
    //	}
    //
    //	@Override
    //	public boolean onOptionsItemSelected(MenuItem item) {
    //		return super.onOptionsItemSelected(item);
    //	}


    protected fun configureResponseReceiver() {}

    override fun manageIntent() {
        val intent = intent
        if (intent != null) {
            if (intent.hasExtra(EXTRA_PARAM_1))
                urlToLoad = intent.getStringExtra(EXTRA_PARAM_1)
            if (intent.hasExtra(EXTRA_PARAM_2))
                transitionName = intent.getStringExtra(EXTRA_PARAM_2)
            if (intent.hasExtra(EXTRA_PARAM_3))
                postOrMessageID = intent.getStringExtra(EXTRA_PARAM_3)
            if (intent.hasExtra(EXTRA_PARAM_4))
                fromChat = intent.getBooleanExtra(EXTRA_PARAM_4, false)

            val objToLoad =
                picturesCache.getMedia(urlToLoad, HLMediaType.PHOTO)
            if (objToLoad is Uri) {
                val path = objToLoad.path
                fileToLoad = if (isStringValid(path)) File(path!!) else null
            } else if (objToLoad is File) fileToLoad = objToLoad
        }
    }

    override fun afterOps() {}

    companion object {

        val LOG_TAG = PhotoViewActivity::class.java.canonicalName
    }

    //endregion

}
