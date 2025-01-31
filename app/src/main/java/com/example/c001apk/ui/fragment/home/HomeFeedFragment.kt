package com.example.c001apk.ui.fragment.home

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.ThemeUtils
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libraries.utils.extensions.dp
import com.example.c001apk.R
import com.example.c001apk.adapter.AppAdapter
import com.example.c001apk.constant.RecyclerView.checkForGaps
import com.example.c001apk.constant.RecyclerView.markItemDecorInsetsDirty
import com.example.c001apk.databinding.FragmentHomeFeedBinding
import com.example.c001apk.ui.fragment.ReplyBottomSheetDialog
import com.example.c001apk.ui.fragment.minterface.AppListener
import com.example.c001apk.ui.fragment.minterface.INavViewContainer
import com.example.c001apk.ui.fragment.minterface.IOnPublishClickListener
import com.example.c001apk.ui.fragment.minterface.IOnTabClickContainer
import com.example.c001apk.ui.fragment.minterface.IOnTabClickListener
import com.example.c001apk.util.BlackListUtil
import com.example.c001apk.util.DensityTool
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.TokenDeviceUtils
import com.example.c001apk.util.TopicBlackListUtil
import com.example.c001apk.view.LinearItemDecoration
import com.example.c001apk.view.StaggerItemDecoration
import com.example.c001apk.viewmodel.AppViewModel
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.lang.reflect.Method


class HomeFeedFragment : Fragment(), AppListener, IOnTabClickListener, IOnPublishClickListener {

    private lateinit var binding: FragmentHomeFeedBinding
    private val viewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }
    private lateinit var mAdapter: AppAdapter
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var sLayoutManager: StaggeredGridLayoutManager
    private lateinit var bottomSheetDialog: ReplyBottomSheetDialog
    private val fabViewBehavior by lazy { HideBottomViewOnScrollBehavior<FloatingActionButton>() }
    private lateinit var mCheckForGapMethod: Method
    private lateinit var mMarkItemDecorInsetsDirtyMethod: Method

    companion object {
        @JvmStatic
        fun newInstance(type: String) =
            HomeFeedFragment().apply {
                arguments = Bundle().apply {
                    putString("TYPE", type)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.type = it.getString("TYPE")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged", "RestrictedApi", "InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!viewModel.isInit) {
            initPublish()
            initView()
            initData()
            initRefresh()
            initScroll()
        }

        viewModel.homeFeedData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val feed = result.getOrNull()
                if (!feed.isNullOrEmpty()) {
                    if (viewModel.isRefreshing) {
                        if (feed.size <= 4 && feed.last().entityTemplate == "refreshCard") {
                            if (viewModel.homeFeedList.size >= 4) {
                                if (viewModel.homeFeedList[3].entityTemplate != "refreshCard") {
                                    viewModel.homeFeedList.add(3, feed.last())
                                    mAdapter.notifyItemInserted(3)
                                }
                            }
                            mAdapter.setLoadState(mAdapter.LOADING_COMPLETE, null)
                            viewModel.isLoadMore = false
                            viewModel.isRefreshing = false
                            binding.swipeRefresh.isRefreshing = false
                            binding.indicator.isIndeterminate = false
                            binding.indicator.visibility = View.GONE
                            return@observe
                        } else {
                            viewModel.homeFeedList.clear()
                        }
                    }
                    if (viewModel.isRefreshing || viewModel.isLoadMore) {
                        viewModel.listSize = viewModel.homeFeedList.size
                        for (element in feed) {
                            if (element.entityType == "feed"
                                || element.entityTemplate == "iconMiniScrollCard"
                                || element.entityTemplate == "iconLinkGridCard"
                                || element.entityTemplate == "imageCarouselCard_1"
                                || element.entityTemplate == "imageTextScrollCard"
                                || element.entityTemplate == "refreshCard"
                            ) {
                                if (element.entityType == "feed" && viewModel.changeFirstItem) {
                                    viewModel.changeFirstItem = false
                                    viewModel.firstItem = element.id
                                }
                                if (!PrefManager.isIconMiniCard && element.entityTemplate == "iconMiniScrollCard")
                                    continue
                                else if (!BlackListUtil.checkUid(element.userInfo?.uid.toString()) && !TopicBlackListUtil.checkTopic(
                                        element.tags + element.ttitle
                                    )
                                )
                                    viewModel.homeFeedList.add(element)
                            }
                        }
                        if (viewModel.homeFeedList.last().entityTemplate == "refreshCard") {
                            viewModel.lastItem =
                                viewModel.homeFeedList[viewModel.homeFeedList.size - 2].entityId
                        } else {
                            viewModel.lastItem =
                                viewModel.homeFeedList.last().entityId
                        }
                    }
                    mAdapter.setLoadState(mAdapter.LOADING_COMPLETE, null)
                } else {
                    mAdapter.setLoadState(mAdapter.LOADING_END, null)
                    viewModel.isEnd = true
                    result.exceptionOrNull()?.printStackTrace()
                }
                if (viewModel.isLoadMore)
                    if (viewModel.isEnd)
                        mAdapter.notifyItemChanged(viewModel.homeFeedList.size)
                    else
                        mAdapter.notifyItemRangeChanged(
                            viewModel.listSize,
                            viewModel.homeFeedList.size - viewModel.listSize + 1
                        )
                else
                    mAdapter.notifyDataSetChanged()
                viewModel.isLoadMore = false
                viewModel.isRefreshing = false
                binding.swipeRefresh.isRefreshing = false
                binding.indicator.isIndeterminate = false
                binding.indicator.visibility = View.GONE
            }
        }

        viewModel.dataListData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val feed = result.getOrNull()
                if (!feed.isNullOrEmpty()) {
                    if (viewModel.isRefreshing)
                        viewModel.homeFeedList.clear()
                    if (viewModel.isRefreshing || viewModel.isLoadMore) {
                        viewModel.listSize = viewModel.homeFeedList.size
                        for (element in feed) {
                            if (element.entityType == "feed"
                                || element.entityTemplate == "iconMiniGridCard"
                                || element.entityTemplate == "iconLinkGridCard"
                                || element.entityTemplate == "imageSquareScrollCard"
                            )
                                if (!BlackListUtil.checkUid(element.userInfo?.uid.toString()) && !TopicBlackListUtil.checkTopic(
                                        element.tags + element.ttitle
                                    )
                                )
                                    viewModel.homeFeedList.add(element)
                            //viewModel.lastItem = feed[feed.size - 1].entityId
                        }
                    }
                    mAdapter.setLoadState(mAdapter.LOADING_COMPLETE, null)
                } else {
                    mAdapter.setLoadState(mAdapter.LOADING_END, null)
                    viewModel.isEnd = true
                    result.exceptionOrNull()?.printStackTrace()
                }
                if (viewModel.isLoadMore)
                    if (viewModel.isEnd)
                        mAdapter.notifyItemChanged(viewModel.homeFeedList.size)
                    else
                        mAdapter.notifyItemRangeChanged(
                            viewModel.listSize,
                            viewModel.homeFeedList.size - viewModel.listSize + 1
                        )
                else
                    mAdapter.notifyDataSetChanged()
                viewModel.isLoadMore = false
                viewModel.isRefreshing = false
                binding.swipeRefresh.isRefreshing = false
                binding.indicator.isIndeterminate = false
                binding.indicator.visibility = View.GONE
            }
        }

        viewModel.likeFeedData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isPostLikeFeed) {
                viewModel.isPostLikeFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.homeFeedList[viewModel.likePosition].likenum =
                            response.data.count
                        viewModel.homeFeedList[viewModel.likePosition].userAction?.like = 1
                        mAdapter.notifyItemChanged(viewModel.likePosition, "like")
                    } else
                        Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.unLikeFeedData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isPostUnLikeFeed) {
                viewModel.isPostUnLikeFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.homeFeedList[viewModel.likePosition].likenum =
                            response.data.count
                        viewModel.homeFeedList[viewModel.likePosition].userAction?.like = 0
                        mAdapter.notifyItemChanged(viewModel.likePosition, "like")
                    } else
                        Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.postCreateFeedData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isCreateFeed) {
                viewModel.isCreateFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.id != null) {
                        Toast.makeText(activity, "发布成功", Toast.LENGTH_SHORT).show()
                        bottomSheetDialog.editText.text = null
                        bottomSheetDialog.dismiss()
                    } else {
                        response.message?.let {
                            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
                        }
                        if (response.messageStatus == "err_request_captcha") {
                            viewModel.isGetCaptcha = true
                            viewModel.timeStamp = System.currentTimeMillis() / 1000
                            viewModel.getValidateCaptcha()
                        }
                    }
                } else {
                    Toast.makeText(activity, "response is null", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.validateCaptchaData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isGetCaptcha) {
                viewModel.isGetCaptcha = false

                val response = result.getOrNull()
                response?.let {
                    val responseBody = response.body()
                    val bitmap = BitmapFactory.decodeStream(responseBody!!.byteStream())
                    val captchaView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_captcha, null, false)
                    val captchaImg: ImageView = captchaView.findViewById(R.id.captchaImg)
                    captchaImg.setImageBitmap(bitmap)
                    val captchaText: TextInputEditText = captchaView.findViewById(R.id.captchaText)
                    captchaText.highlightColor = ColorUtils.setAlphaComponent(
                        ThemeUtils.getThemeAttrColor(
                            requireContext(),
                            rikka.preference.simplemenu.R.attr.colorPrimaryDark
                        ), 128
                    )
                    MaterialAlertDialogBuilder(requireContext()).apply {
                        setView(captchaView)
                        setTitle("captcha")
                        setNegativeButton(android.R.string.cancel, null)
                        setPositiveButton("验证并继续") { _, _ ->
                            viewModel.requestValidateData["type"] = "err_request_captcha"
                            viewModel.requestValidateData["code"] = captchaText.text.toString()
                            viewModel.requestValidateData["mobile"] = ""
                            viewModel.requestValidateData["idcard"] = ""
                            viewModel.requestValidateData["name"] = ""
                            viewModel.isRequestValidate = true
                            viewModel.postRequestValidate()
                        }
                        show()
                    }
                }
            }
        }

        viewModel.postRequestValidateData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isRequestValidate) {
                viewModel.isRequestValidate = false

                val response = result.getOrNull()
                response?.let {
                    if (response.data != null) {
                        Toast.makeText(activity, response.data, Toast.LENGTH_SHORT).show()
                        if (response.data == "验证通过") {
                            viewModel.isCreateFeed = true
                            bottomSheetDialog.editText.text = null
                            bottomSheetDialog.dismiss()
                            //viewModel.postCreateFeed()
                        }
                    } else if (response.message != null) {
                        Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
                        if (response.message == "请输入正确的图形验证码") {
                            viewModel.isGetCaptcha = true
                            viewModel.timeStamp = System.currentTimeMillis() / 1000
                            viewModel.getValidateCaptcha()
                        }
                    }
                }
            }
        }

        viewModel.postDeleteData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data == "删除成功") {
                        Toast.makeText(requireContext(), response.data, Toast.LENGTH_SHORT).show()
                        viewModel.homeFeedList.removeAt(viewModel.position)
                        mAdapter.notifyItemRemoved(viewModel.position)
                    } else if (!response.message.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

    }

    @SuppressLint("InflateParams")
    private fun initPublish() {
        if (viewModel.type == "feed" && PrefManager.isLogin) {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_reply_bottom_sheet, null, false)
            val lp = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(
                0,
                0,
                25.dp,
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    DensityTool.getNavigationBarHeight(requireContext())
                            + 105.dp
                else
                    25.dp
            )
            lp.gravity = Gravity.BOTTOM or Gravity.END
            binding.fab.layoutParams = lp
            (binding.fab.layoutParams as CoordinatorLayout.LayoutParams).behavior = fabViewBehavior
            binding.fab.visibility = View.VISIBLE
            bottomSheetDialog = ReplyBottomSheetDialog(requireContext(), view)
            bottomSheetDialog.setIOnPublishClickListener(this)
            bottomSheetDialog.apply {
                setContentView(view)
                setCancelable(false)
                setCanceledOnTouchOutside(true)
                window?.apply {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
                type = "publish"
            }
            binding.fab.setOnClickListener {
                if (PrefManager.SZLMID == "") {
                    Toast.makeText(activity, "数字联盟ID不能为空", Toast.LENGTH_SHORT).show()
                } else {
                    bottomSheetDialog.show()
                }
            }
        } else
            binding.fab.visibility = View.GONE
    }

    private fun initScroll() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (viewModel.lastVisibleItemPosition == viewModel.homeFeedList.size
                        && !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                    ) {
                        mAdapter.setLoadState(mAdapter.LOADING, null)
                        mAdapter.notifyItemChanged(viewModel.homeFeedList.size)
                        viewModel.isLoadMore = true
                        //viewModel.firstItem = null
                        viewModel.page++
                        viewModel.firstLaunch = 0
                        viewModel.isNew = true
                        when (viewModel.type) {
                            "feed" -> viewModel.getHomeFeed()
                            "rank" -> viewModel.getDataList()
                            "follow" -> viewModel.getDataList()
                            "coolPic" -> viewModel.getDataList()
                        }

                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (viewModel.homeFeedList.isNotEmpty()) {
                    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        viewModel.lastVisibleItemPosition =
                            mLayoutManager.findLastVisibleItemPosition()
                        viewModel.firstCompletelyVisibleItemPosition =
                            mLayoutManager.findFirstCompletelyVisibleItemPosition()
                    } else {
                        val result =
                            mCheckForGapMethod.invoke(binding.recyclerView.layoutManager) as Boolean
                        if (result)
                            mMarkItemDecorInsetsDirtyMethod.invoke(binding.recyclerView)

                        val positions = sLayoutManager.findLastVisibleItemPositions(null)
                        for (pos in positions) {
                            if (pos > viewModel.lastVisibleItemPosition) {
                                viewModel.lastVisibleItemPosition = pos
                            }
                        }
                    }
                    if (dy > 0) {
                        (activity as INavViewContainer).hideNavigationView()
                    } else if (dy < 0) {
                        (activity as INavViewContainer).showNavigationView()
                    }

                }
            }
        })
    }

    @SuppressLint("RestrictedApi")
    private fun initRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ThemeUtils.getThemeAttrColor(
                requireContext(),
                rikka.preference.simplemenu.R.attr.colorPrimary
            )
        )
        binding.swipeRefresh.setOnRefreshListener {
            binding.indicator.isIndeterminate = false
            binding.indicator.visibility = View.GONE
            refreshData()
        }
    }

    private fun initData() {
        if (viewModel.homeFeedList.isEmpty()) {
            binding.indicator.isIndeterminate = true
            binding.indicator.visibility = View.VISIBLE
            refreshData()
        }
    }

    private fun refreshData() {
        //viewModel.lastItem = null
        viewModel.firstVisibleItemPosition = -1
        viewModel.lastVisibleItemPosition = -1
        viewModel.changeFirstItem = true
        viewModel.isEnd = false
        viewModel.page = 1
        viewModel.firstLaunch = 0
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.installTime = TokenDeviceUtils.getLastingInstallTime(requireContext())
        viewModel.isNew = true
        when (viewModel.type) {
            "feed" -> viewModel.getHomeFeed()
            "rank" -> {
                viewModel.url = "/page?url=V9_HOME_TAB_RANKING"
                viewModel.title = "热榜"
                viewModel.getDataList()
            }

            "follow" -> {
                if (viewModel.url == "") {
                    viewModel.url = "/page?url=V9_HOME_TAB_FOLLOW"
                    viewModel.title = "全部关注"
                }
                viewModel.getDataList()
            }

            "coolPic" -> {
                viewModel.url = "/page?url=V11_FIND_COOLPIC"
                viewModel.title = "酷图"
                viewModel.getDataList()
            }
        }
    }

    private fun initView() {
        val space = resources.getDimensionPixelSize(R.dimen.normal_space)
        mAdapter = AppAdapter(
            requireContext(), viewModel.homeFeedList
        )
        mLayoutManager = LinearLayoutManager(activity)
        sLayoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // https://codeantenna.com/a/2NDTnG37Vg
            mCheckForGapMethod = checkForGaps
            mCheckForGapMethod.isAccessible = true
            mMarkItemDecorInsetsDirtyMethod = markItemDecorInsetsDirty
            mMarkItemDecorInsetsDirtyMethod.isAccessible = true
        }

        mAdapter.setAppListener(this)
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    mLayoutManager
                else sLayoutManager
            if (itemDecorationCount == 0) {
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    addItemDecoration(LinearItemDecoration(space))
                else
                    addItemDecoration(StaggerItemDecoration(space))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        (requireParentFragment() as IOnTabClickContainer).tabController = this

        if (viewModel.isInit) {
            viewModel.isInit = false
            initPublish()
            initView()
            initData()
            initRefresh()
            initScroll()
        }
    }

    override fun onShowTotalReply(position: Int, uid: String, id: String, rPosition: Int?) {}

    override fun onPostFollow(isFollow: Boolean, uid: String, position: Int) {}

    override fun onReply2Reply(
        rPosition: Int,
        r2rPosition: Int?,
        id: String,
        uid: String,
        uname: String,
        type: String
    ) {
    }

    override fun onPostLike(type: String?, isLike: Boolean, id: String, position: Int?) {
        viewModel.likeFeedId = id
        viewModel.likePosition = position!!
        if (isLike) {
            viewModel.isPostUnLikeFeed = true
            viewModel.postUnLikeFeed()
        } else {
            viewModel.isPostLikeFeed = true
            viewModel.postLikeFeed()
        }

    }

    override fun onRefreshReply(listType: String) {}

    override fun onDeleteFeedReply(id: String, position: Int, rPosition: Int?) {
        viewModel.isNew = true
        viewModel.position = position
        viewModel.url = "/v6/feed/deleteFeed"
        viewModel.deleteId = id
        viewModel.postDelete()
    }

    override fun onShowCollection(id: String, title: String) {}

    @SuppressLint("NotifyDataSetChanged")
    override fun onReturnTop(isRefresh: Boolean?) {
        if (isRefresh!!) {
            if (viewModel.firstCompletelyVisibleItemPosition == 0) {
                binding.swipeRefresh.isRefreshing = true
                refreshData()
            } else {
                binding.recyclerView.scrollToPosition(0)
                binding.swipeRefresh.isRefreshing = true
                refreshData()
            }
        } else if (viewModel.type == "follow") {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("关注分组")
                val items = arrayOf("全部关注", "好友关注", "话题关注", "数码关注", "应用关注")
                setSingleChoiceItems(
                    items,
                    viewModel.position
                ) { dialog: DialogInterface, position: Int ->
                    when (position) {
                        0 -> {
                            viewModel.position = 0
                            viewModel.url = "/page?url=V9_HOME_TAB_FOLLOW"
                            viewModel.title = "全部关注"
                        }

                        1 -> {
                            viewModel.position = 1
                            viewModel.url = "/page?url=V9_HOME_TAB_FOLLOW&type=circle"
                            viewModel.title = "好友关注"
                        }

                        2 -> {
                            viewModel.position = 2
                            viewModel.url = "/page?url=V9_HOME_TAB_FOLLOW&type=topic"
                            viewModel.title = "话题关注"
                        }

                        3 -> {
                            viewModel.position = 3
                            viewModel.url = "/page?url=V9_HOME_TAB_FOLLOW&type=product"
                            viewModel.title = "数码关注"
                        }

                        4 -> {
                            viewModel.position = 4
                            viewModel.url = "/page?url=V9_HOME_TAB_FOLLOW&type=apk"
                            viewModel.title = "应用关注"
                        }
                    }
                    viewModel.homeFeedList.clear()
                    mAdapter.notifyDataSetChanged()
                    binding.indicator.visibility = View.VISIBLE
                    binding.indicator.isIndeterminate = true
                    refreshData()
                    dialog.dismiss()
                }
                show()
            }
        }
    }

    override fun onPublish(message: String, replyAndForward: String) {
        viewModel.createFeedData["id"] = ""
        viewModel.createFeedData["message"] = message
        viewModel.createFeedData["type"] = "feed"
        viewModel.createFeedData["is_html_artical"] = "0"
        viewModel.createFeedData["pic"] = ""
        viewModel.createFeedData["status"] = "-1"
        viewModel.createFeedData["location"] = ""
        viewModel.createFeedData["long_location"] = ""
        viewModel.createFeedData["latitude"] = "0.0"
        viewModel.createFeedData["longitude"] = "0.0"
        viewModel.createFeedData["media_url"] = ""
        viewModel.createFeedData["media_type"] = "0"
        viewModel.createFeedData["media_pic"] = ""
        viewModel.createFeedData["message_title"] = ""
        viewModel.createFeedData["message_brief"] = ""
        viewModel.createFeedData["extra_title"] = ""
        viewModel.createFeedData["extra_url"] = ""
        viewModel.createFeedData["extra_key"] = ""
        viewModel.createFeedData["extra_pic"] = ""
        viewModel.createFeedData["extra_info"] = ""
        viewModel.createFeedData["message_cover"] = ""
        viewModel.createFeedData["disallow_repost"] = "0"
        viewModel.createFeedData["is_anonymous"] = "0"
        viewModel.createFeedData["is_editInDyh"] = "0"
        viewModel.createFeedData["forwardid"] = ""
        viewModel.createFeedData["fid"] = ""
        viewModel.createFeedData["dyhId"] = ""
        viewModel.createFeedData["targetType"] = ""
        viewModel.createFeedData["productId"] = ""
        viewModel.createFeedData["province"] = ""
        viewModel.createFeedData["city_code"] = ""
        viewModel.createFeedData["province"] = ""
        viewModel.createFeedData["city_code"] = ""
        viewModel.createFeedData["targetId"] = ""
        viewModel.createFeedData["location_city"] = ""
        viewModel.createFeedData["location_country"] = ""
        viewModel.createFeedData["disallow_reply"] = "0"
        viewModel.createFeedData["vote_score"] = "0"
        viewModel.createFeedData["replyWithForward"] = "0"
        viewModel.createFeedData["media_info"] = ""
        viewModel.createFeedData["insert_product_media"] = "0"
        viewModel.createFeedData["is_ks_doc"] = "0"
        viewModel.createFeedData["goods_list_id"] = ""
        viewModel.isCreateFeed = true
        viewModel.postCreateFeed()
    }

}