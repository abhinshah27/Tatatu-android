package rs.highlande.app.tatatu.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.ActivityAnimationHolder
import rs.highlande.app.tatatu.core.ui.BaseActivity


/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-08.
 */



//region = Fragments =

const val FRAGMENT_KEY_CODE = "fragment_code"
const val FRAGMENT_INVALID = -1

// Create Post
const val FRAGMENT_CREATE_POST = 0
const val FRAGMENT_EDIT_POST = 1

const val FRAGMENT_PROFILE = 2
const val FRAGMENT_ACCOUNT_FOLLOWERS = 3
const val FRAGMENT_ACCOUNT_FOLLOWING = 4
const val FRAGMENT_FOLLOW_REQUESTS = 5
const val FRAGMENT_SENT_FOLLOW_REQUESTS = 6
const val FRAGMENT_FRIENDS = 7
const val FRAGMENT_INVITES = 8


const val FRAGMENT_INVITE = 3

const val FRAGMENT_MM_PLAYLIST = 9
const val FRAGMENT_MM_VIDEO = 10

const val FRAGMENT_LOGIN = 11

const val FRAGMENT_POST_TIMELINE = 12
const val FRAGMENT_POST_DETAIL = 13
const val FRAGMENT_POST_LIKES = 14
const val FRAGMENT_MY_POST_DETAIL = 15
const val FRAGMENT_NEWS_POST_DETAIL = 16

const val FRAGMENT_WEBVIEW = 17

const val FRAGMENT_SETTINGS = 18
const val FRAGMENT_EDIT_PROFILE = 19
const val FRAGMENT_WALLET= 20

const val FRAGMENT_SIGNUP = 21

const val FRAGMENT_CHAT_MESSAGES = 23
const val FRAGMENT_CHAT_ROOMS = 24
const val FRAGMENT_CHAT_CREATION = 25

//endregion




fun addFragmentNull(
    fragmentTransaction: FragmentTransaction, @IdRes containerResId: Int,
    fragment: Fragment, logTag: String
) {
    addFragmentNull(fragmentTransaction, containerResId, fragment, logTag, null, 0)
}

fun addFragmentNull(
    fragmentTransaction: FragmentTransaction, @IdRes containerResId: Int,
    fragment: Fragment, logTag: String, target: Fragment?, requestCode: Int
) {
    addFragmentNull(fragmentTransaction, containerResId, fragment, logTag, target, requestCode, null)
}

fun addFragmentNull(
    fragmentTransaction: FragmentTransaction, @IdRes containerResId: Int,
    fragment: Fragment, logTag: String?, target: Fragment?, requestCode: Int,
    @Nullable tagName: String?
) {
    if (target != null)
        fragment.setTargetFragment(target, requestCode)
    fragment.retainInstance = true
    fragmentTransaction.replace(containerResId, fragment, logTag)
    if (!tagName.isNullOrBlank())
        fragmentTransaction.addToBackStack(tagName)
    else
        fragmentTransaction.addToBackStack(null)
}


fun addFragmentNotNull(fragmentTransaction: FragmentTransaction, fragment: Fragment) {
    addFragmentNotNull(fragmentTransaction, fragment, null, 0)
}

fun addFragmentNotNull(
    fragmentTransaction: FragmentTransaction,
    fragment: Fragment,
    target: Fragment?,
    requestCode: Int
) {
    addFragmentNotNull(fragmentTransaction, fragment, target, requestCode, null)
}

fun addFragmentNotNull(
    fragmentTransaction: FragmentTransaction, fragment: Fragment,
    target: Fragment?, requestCode: Int, @Nullable tagName: String?
) {
    if (target != null)
        fragment.setTargetFragment(target, requestCode)
    if (!tagName.isNullOrBlank())
        fragmentTransaction.addToBackStack(tagName)
    else
        fragmentTransaction.addToBackStack(null)
    fragmentTransaction.detach(fragment)
    fragmentTransaction.attach(fragment)
}



inline fun <reified T : BaseActivity>openFragment(
    context: Context,
    fragmentCode: Int,
    bundle: Bundle? = null,
    requestCode: Int? = null,
    finish: Boolean = false,
    animations: ActivityAnimationHolder = ActivityAnimationHolder(R.anim.no_animation, R.anim.no_animation),
    flags: Int? = null
) {
    val intent = Intent(context, T::class.java)

    // INFO: 2019-11-25    solves navigation issue
//    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

    intent.putExtra(FRAGMENT_KEY_CODE, fragmentCode)

    flags?.let { intent.addFlags(it) }

    if (bundle != null)
        intent.putExtras(bundle)
    if (requestCode != null)
        (context as Activity).startActivityForResult(intent, requestCode)
    else
        context.startActivity(intent)

    if (finish) (context as Activity).finish()

    (context as Activity).overridePendingTransition(animations.enterAnim, animations.exitAnim)
}