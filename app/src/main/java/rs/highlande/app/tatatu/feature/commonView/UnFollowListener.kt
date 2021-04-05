package rs.highlande.app.tatatu.feature.commonView

import android.view.View

/**
 * TODO - Interface description
 * @author mbaldrighi on 2019-07-03.
 */
interface UnFollowListener<in T : UnFollowable> {

    fun onFollowClickedListener(item: T, view: View, position: Int)
    fun onUnfollowClickedListener(item: T, view: View, position: Int)

}

interface UnFollowable