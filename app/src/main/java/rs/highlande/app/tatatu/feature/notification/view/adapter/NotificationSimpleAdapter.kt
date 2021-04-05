package rs.highlande.app.tatatu.feature.notification.view.adapter

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.util.NOTIFICATION_TYPE_PROFILE
import rs.highlande.app.tatatu.core.util.formatDateToAge
import rs.highlande.app.tatatu.core.util.resolveColorAttribute
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.NotificationSimpleViewModelBinding
import rs.highlande.app.tatatu.feature.notification.view.fragment.NotificationFragment
import rs.highlande.app.tatatu.model.UserNotification

/**
 * Created by Abhin.
 */

class NotificationSimpleAdapter(listener: NotificationFragment.NotificationItemClickListener) :
    BaseRecViewAdapter<UserNotification, NotificationFragment.NotificationItemClickListener, NotificationSimpleAdapter.NotificationSimpleAdapterViewHolder>(listener) {

    private var mNotificationSimpleViewModelBinding: NotificationSimpleViewModelBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationSimpleAdapterViewHolder {
        mNotificationSimpleViewModelBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_notification_simple, parent, false)
        return NotificationSimpleAdapterViewHolder(mNotificationSimpleViewModelBinding!!)
    }
    inner class NotificationSimpleAdapterViewHolder(var mNotificationSimpleViewModelBinding: NotificationSimpleViewModelBinding) :
        BaseViewHolder<UserNotification, NotificationFragment.NotificationItemClickListener>(mNotificationSimpleViewModelBinding.root) {

        init {
            itemView.setOnClickListener {
                // no ops
            }
        }

        override fun onBind(
            item: UserNotification,
            listener: NotificationFragment.NotificationItemClickListener?
        ) {
            mNotificationSimpleViewModelBinding.apply {
                data = item
                executePendingBindings()
                listener?.let { listener ->
                    item.userInfo?.let { userInfo ->
                        roundAvatarImageView.picture.setProfilePicture(userInfo.picture)
                        roundAvatarImageView.setOnClickListener {
                            item.userInfo?.let {
                                listener.onItemClick(NOTIFICATION_TYPE_PROFILE, it.uid) }
                            }
                        if (!item.notificationType.isNullOrEmpty() && !item.referenceID.isNullOrEmpty())
                            root.setOnClickListener { listener.onItemClick(item.notificationType!!, item.referenceID!!) }
                    }
                }

                val formattedDate = formatDateToAge(mNotificationSimpleViewModelBinding.root.context, item.notificationDate!!)
                val spannableText = SpannableString(StringBuilder(item.notificationText!!).append(". ").append(formattedDate))
                val foregroundColorSpan = ForegroundColorSpan(resolveColorAttribute(root.context, R.attr.textColorSecondary))

                spannableText.setSpan(foregroundColorSpan, (spannableText.length - formattedDate.length), spannableText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                txtNotificationBody.text = spannableText
            }
        }

        override fun getImageViewsToRecycle() = emptySet<ImageView>()
    }

}