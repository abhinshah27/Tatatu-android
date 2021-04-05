package rs.highlande.app.tatatu.feature.suggested.view

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.chat_bubble_with_badge.view.*
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseDividerDecoration
import rs.highlande.app.tatatu.core.ui.recyclerView.CustomSwipeToDismiss
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.databinding.ActivitySuggestedBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.chat.view.ChatActivity
import rs.highlande.app.tatatu.feature.commonView.BottomNavigationActivity
import rs.highlande.app.tatatu.feature.commonView.UnFollowListener
import rs.highlande.app.tatatu.model.SuggestedPerson

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-03.
 */
class SuggestedActivity : BottomNavigationActivity(),
    UnFollowListener<SuggestedPerson> {

    private val suggestedViewModel: SuggestedViewModel by viewModel()

    private lateinit var binding: ActivitySuggestedBinding
    private lateinit var sentFollowRequest: TextView
    private lateinit var adapter: SuggestedAdapter

    private val scrollListener = object : EndlessScrollListener() {
        override fun onLoadMore() {
            suggestedViewModel.getSuggested()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_suggested)
        binding.lifecycleOwner = this

        adapter = SuggestedAdapter(
            SuggestedPerson.SuggestedDiffCallback(),
            object : OnItemClickListener<SuggestedPerson> {
                override fun onItemClick(item: SuggestedPerson) {
                    AccountActivity.openProfileFragment(this@SuggestedActivity, item.uid)
                }
            }, this@SuggestedActivity
        ).also { it.setHasStableIds(true) }

        configureLayout()

        with(suggestedViewModel) {
            suggestedConnected.observe(this@SuggestedActivity, Observer {
                LogUtils.d("", if (it.first) "GET SUGGESTED Request successful" else "GET SUGGESTED Request failed")
            })
            suggestedReceived.observe(this@SuggestedActivity, Observer {})
            suggested.observe(this@SuggestedActivity, Observer {
                showData(it)
            })

            loadMoreCanFetch.observe(this@SuggestedActivity, Observer {
                scrollListener.canFetch = it
            })

            getSuggested()
        }

    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        bindLayout()
    }


    override fun configureLayout() {
        binding.suggestedList.apply {
            adapter = this@SuggestedActivity.adapter
            layoutManager = LinearLayoutManager(this@SuggestedActivity)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            addItemDecoration(
                // FIXME: 2019-07-05    ItemDecoration custom padding NOT WORKING
                BaseDividerDecoration(
                    this@SuggestedActivity,
                    padding = R.dimen.padding_margin_default
                )
            )
            addOnScrollListener(scrollListener)

            val swipe = object : CustomSwipeToDismiss() {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val adapter = this@apply.adapter as SuggestedAdapter
                    val item = adapter.getItem(viewHolder.adapterPosition)
                    adapter.removeAt(viewHolder.adapterPosition)
                    suggestedViewModel.removeSuggested(item)
                }
            }
            ItemTouchHelper(swipe).attachToRecyclerView(this)
        }

        sentFollowRequest = binding.include2.findViewById<TextView>(R.id.item).also {
            it.setOnClickListener {
                AccountActivity.openSentFollowerRequestsFragment(this)
            }
        }

        binding.srl.setOnRefreshListener { suggestedViewModel.getSuggested() }

        setSupportActionBar(binding.toolbar as Toolbar)
        binding.toolbar.backArrow.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_to_right)
        }
    }

    override fun bindLayout() {
        binding.toolbar.title.setText(R.string.home_title_suggested)

        sentFollowRequest.setText(R.string.follow_sent_requests_label)
    }

    override fun manageIntent() {}

    override fun onPause() {
        suggestedViewModel.clearObservers(this)
        super.onPause()
    }


    private fun showData(data: List<SuggestedPerson>) {
        (binding.srl as SwipeRefreshLayout).isRefreshing = false

        adapter.addAll(data)
    }

    override fun onFollowClickedListener(item: SuggestedPerson, view: View, position: Int) {
        suggestedViewModel.relationshipChanged.observe(this@SuggestedActivity, Observer {
            if (it) {
                adapter.remove(item)
                suggestedViewModel.removeSuggested(item)
            }
            view.isEnabled = true
            suggestedViewModel.relationshipChanged.removeObservers(this@SuggestedActivity)
        })
        view.isEnabled = false
        suggestedViewModel.followSuggested(item.uid)

    }
    override fun onUnfollowClickedListener(item: SuggestedPerson, view: View, position: Int) {}


    companion object {
        val logTag = SuggestedActivity::class.java.simpleName
    }
}
