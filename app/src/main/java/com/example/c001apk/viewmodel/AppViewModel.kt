package com.example.c001apk.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.database.BlackListDatabase
import com.example.c001apk.logic.database.BrowseHistoryDatabase
import com.example.c001apk.logic.database.FeedFavoriteDatabase
import com.example.c001apk.logic.database.SearchHistoryDatabase
import com.example.c001apk.logic.database.TopicBlackListDatabase
import com.example.c001apk.logic.model.AppItem
import com.example.c001apk.logic.model.FeedContentResponse
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.logic.model.HomeMenu
import com.example.c001apk.logic.model.MessageResponse
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.logic.model.UpdateCheckResponse
import com.example.c001apk.logic.network.Repository
import com.example.c001apk.util.Utils
import com.example.c001apk.util.Utils.Companion.getBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import rikka.core.content.pm.longVersionCodeCompat


class AppViewModel : ViewModel() {

    var isFollow = false
    var isTop = false
    var isEnable = false
    var listSize = -1
    var feedTypeName: String? = null
    var topReplyId: String? = null
    var commentStatusText: String? = null
    var errorMessage: String? = null
    val bHistoryList: ArrayList<Any> = ArrayList()
    var requestHash = ""
    var changeFirstItem = false
    var isRequestValidate = false
    var isGetCaptcha = false
    var isCreateFeed = false
    var isGetCheckLoginInfo = false
    var level = ""
    var bio = ""
    var loginTime = ""
    var like = ""
    var follow = ""
    var fans = ""
    var version = ""
    var logo = ""
    var size = ""
    var lastupdate = ""
    var dateLine = 0L
    var followType = false
    var isViewReply = false
    var isShowMoreReply = false
    var replyCount = ""
    var isRefreshReply = false
    var device = ""
    var avatar = ""
    var cover = ""
    var isNew = true
    var isPostLikeFeed = false
    var isPostUnLikeFeed = false
    var isPostLikeReply = false
    var isPostUnLikeReply = false
    var isPostReply = false
    var postFollowUnFollow = false

    var fuid = "" // feed user id

    //feed data
    var id = "" // feed id
    var uid = "" // feed user id
    var funame = ""
    var uname = "" // feed username //被回复用户name
    var rid = "" // 回复feed/reply id
    var ruid = "" // 被回复用户id
    var type = "" //feed reply

    var rPosition: Int? = null
    var firstVisibleItemPosition = -1
    var firstCompletelyVisibleItemPosition = -1
    var lastVisibleItemPosition = -1
    var likeReplyPosition = -1

    val feedContentList = ArrayList<FeedContentResponse>()
    val feedReplyList = ArrayList<TotalReplyResponse.Data>()
    val feedTopReplyList = ArrayList<TotalReplyResponse.Data>()

    var isRefreshing = true
    var isLoadMore = false
    var isEnd = false

    private val getFeedData = MutableLiveData<String>()
    var frid: String? = null
    val feedData = getFeedData.switchMap {
        Repository.getFeedContent(id, frid)
    }

    fun getFeed() {
        getFeedData.value = getFeedData.value
    }

    var page = 1
    private var discussMode = 1
    var listType = "lastupdate_desc"
    private var blockStatus = 0
    var fromFeedAuthor = 0

    private val getFeedReplyData = MutableLiveData<String>()

    val feedReplyData = getFeedReplyData.switchMap {
        Repository.getFeedContentReply(
            id,
            listType,
            page,
            firstItem,
            lastItem,
            discussMode,
            "feed",
            blockStatus,
            fromFeedAuthor
        )
    }

    fun getFeedReply() {
        getFeedReplyData.value = getFeedReplyData.value
    }


    //like reply
    var likeReplyId = ""
    private val postLikeReplyData = MutableLiveData<String>()
    val likeReplyData = postLikeReplyData.switchMap {
        Repository.postLikeReply(likeReplyId)
    }

    fun postLikeReply() {
        postLikeReplyData.value = postLikeReplyData.value
    }

    //unlike reply
    private val postUnLikeReplyData = MutableLiveData<String>()
    val unLikeReplyData = postUnLikeReplyData.switchMap {
        Repository.postUnLikeReply(likeReplyId)
    }

    fun postUnLikeReply() {
        postUnLikeReplyData.value = postUnLikeReplyData.value
    }

    //like feed
    var likeFeedId = ""
    private val postLikeFeedData = MutableLiveData<String>()
    val likeFeedData = postLikeFeedData.switchMap {
        Repository.postLikeFeed(likeFeedId)
    }

    fun postLikeFeed() {
        postLikeFeedData.value = postLikeFeedData.value
    }

    //unlike feed
    private val postUnLikeFeedData = MutableLiveData<String>()
    val unLikeFeedData = postUnLikeFeedData.switchMap {
        Repository.postUnLikeFeed(likeFeedId)
    }

    fun postUnLikeFeed() {
        postUnLikeFeedData.value = postUnLikeFeedData.value
    }

    var replyData = HashMap<String, String>()

    private val postReplyLiveData = MutableLiveData<String>()

    val postReplyData = postReplyLiveData.switchMap {
        Repository.postReply(replyData, rid, type)
    }

    fun postReply() {
        postReplyLiveData.value = postReplyLiveData.value
    }

    private val getAppInfoData = MutableLiveData<String>()

    val appInfoData = getAppInfoData.switchMap {
        Repository.getAppInfo(id)
    }

    fun getAppInfo() {
        getAppInfoData.value = getAppInfoData.value
    }

    var packageName = ""
    var versionCode = ""

    private val getDownloadLinkData = MutableLiveData<String>()

    val downloadLinkData = getDownloadLinkData.switchMap {
        Repository.getAppDownloadLink(packageName, appId, versionCode)
    }

    fun getDownloadLink() {
        getDownloadLinkData.value = getDownloadLinkData.value
    }


    var likePosition = -1
    private val baseURL = "/page?url=/feed/apkCommentList?id="
    var appId = ""
    var isInit = true
    val appCommentList = ArrayList<HomeFeedResponse.Data>()
    var appCommentTitle = "最近回复"
    var appCommentSort = ""

    private val getAppCommentData = MutableLiveData<String>()

    val appCommentData = getAppCommentData.switchMap {
        Repository.getDataList(
            baseURL + appId + appCommentSort,
            appCommentTitle,
            subtitle,
            lastItem,
            page
        )
    }

    fun getAppComment() {
        getAppCommentData.value = getAppCommentData.value
    }

    val menuList = ArrayList<HomeMenu>()
    val tabList = ArrayList<String>()
    val fragmentList = ArrayList<Fragment>()

    var isResume = true

    var barTitle = ""
    var url = ""
    var title = ""
    var productTitle = "最近回复"

    val carouselList = ArrayList<HomeFeedResponse.Data>()

    private val getCarouselData = MutableLiveData<String>()

    val carouselData = getCarouselData.switchMap {
        Repository.getDataList(url, title, subtitle, lastItem, page)
    }

    fun getCarouselList() {
        getCarouselData.value = getCarouselData.value
    }

    val dataList = ArrayList<HomeFeedResponse.Data>()

    private val getDataListData = MutableLiveData<String>()

    val listData = getDataListData.switchMap {
        url = when (type) {
            "feed" -> "/v6/user/feedList?showAnonymous=0&isIncludeTop=1"
            "follow" -> "/v6/user/followList"
            "fans" -> "/v6/user/fansList"
            "apk" -> {
                uid = ""
                "/v6/user/apkFollowList"
            }

            "forum" -> {
                uid = ""
                "/v6/user/forumFollowList"
            }

            "like" -> "/v6/user/likeList"

            "reply" -> "/v6/user/replyList"

            "replyToMe" -> "/v6/user/replyToMeList"

            "recentHistory" -> "/v6/user/recentHistoryList"

            else -> throw IllegalArgumentException("invalid type: $type")
        }
        Repository.getFollowList(url, uid, page)
    }

    fun getFeedList() {
        getDataListData.value = getDataListData.value
    }

    val messageList = ArrayList<MessageResponse.Data>()

    private val getMessageListData = MutableLiveData<String>()

    val messageData = getMessageListData.switchMap {
        Repository.getMessage(url, page)
    }

    fun getMessage() {
        getMessageListData.value = getMessageListData.value
    }

    private val preGetLoginParamLiveData = MutableLiveData<String>()

    val preGetLoginParamData = preGetLoginParamLiveData.switchMap {
        Repository.preGetLoginParam()
    }

    fun preGetLoginParam() {
        preGetLoginParamLiveData.value = preGetLoginParamLiveData.value
    }

    private val getLoginParamData = MutableLiveData<String>()

    val loginParamData = getLoginParamData.switchMap {
        Repository.getLoginParam()
    }

    fun getLoginParam() {
        getLoginParamData.value = getLoginParamData.value
    }

    var loginData = HashMap<String, String?>()

    private val getTryLoginData = MutableLiveData<String>()

    val tryLoginData = getTryLoginData.switchMap {
        Repository.tryLogin(loginData)
    }

    fun tryLogin() {
        getTryLoginData.value = getTryLoginData.value
    }

    private val getCaptchaData = MutableLiveData<String>()

    var timeStamp = 0L
    private val baseUrl = "/auth/showCaptchaImage?"
    val captchaData = getCaptchaData.switchMap {
        Repository.getCaptcha("$baseUrl$timeStamp")
    }

    fun getCaptcha() {
        getCaptchaData.value = getCaptchaData.value
    }

    private val getValidateCaptchaLiveData = MutableLiveData<String>()

    private val validateCaptchaBaseUrl = "/v6/account/captchaImage?"
    val validateCaptchaData = getValidateCaptchaLiveData.switchMap {
        Repository.getValidateCaptcha("$validateCaptchaBaseUrl${timeStamp}&w=270=&h=113")
    }

    fun getValidateCaptcha() {
        getValidateCaptchaLiveData.value = getValidateCaptchaLiveData.value
    }

    private val getProfileDataLiveData = MutableLiveData<String>()

    val profileDataLiveData = getProfileDataLiveData.switchMap {
        Repository.getProfile(uid)
    }

    fun getProfile() {
        getProfileDataLiveData.value = getProfileDataLiveData.value
    }

    var key = ""
    private val getSmsLoginParamLiveData = MutableLiveData<String>()

    val smsLoginParamData = getSmsLoginParamLiveData.switchMap {
        Repository.getSmsLoginParam("mobile")
    }

    fun getSmsLoginParam() {
        getSmsLoginParamLiveData.value = getSmsLoginParamLiveData.value
    }

    var getSmsData = HashMap<String, String?>()

    private val getSmsDataLiveData = MutableLiveData<String>()

    val getSmsTokenData = getSmsDataLiveData.switchMap {
        Repository.getSmsToken("mobile", getSmsData)
    }

    fun getSmsToken() {
        getSmsDataLiveData.value = getSmsDataLiveData.value
    }

    var badge = 0

    private val getCheckLoginInfoData = MutableLiveData<String>()

    val checkLoginInfoData = getCheckLoginInfoData.switchMap {
        Repository.checkLoginInfo()
    }

    fun getCheckLoginInfo() {
        getCheckLoginInfoData.value = getCheckLoginInfoData.value
    }

    private val getUserData = MutableLiveData<String>()

    val userData = getUserData.switchMap {
        Repository.getUserSpace(id)
    }

    fun getUser() {
        getUserData.value = getUserData.value
    }


    val feedList = ArrayList<HomeFeedResponse.Data>()

    private val getUserFeedData = MutableLiveData<String>()

    val userFeedData = getUserFeedData.switchMap {
        Repository.getUserFeed(uid, page)
    }

    fun getUserFeed() {
        getUserFeedData.value = getUserFeedData.value
    }

    val dyhDataList = ArrayList<HomeFeedResponse.Data>()

    var dyhId = ""

    private val getDyhDetailLiveData = MutableLiveData<String>()

    val dyhDetailLiveData = getDyhDetailLiveData.switchMap {
        Repository.getDyhDetail(dyhId, type, page)
    }

    fun getDyhDetail() {
        getDyhDetailLiveData.value = getDyhDetailLiveData.value
    }

    var position: Int = 0
    var r2rPosition = 0

    val replyTotalList = ArrayList<TotalReplyResponse.Data>()

    private val getReplyTotalLiveData = MutableLiveData<String>()

    val replyTotalLiveData = getReplyTotalLiveData.switchMap {
        Repository.getReply2Reply(id, page)
    }

    fun getReplyTotal() {
        getReplyTotalLiveData.value = getReplyTotalLiveData.value
    }

    val appList = ArrayList<AppItem>()
    val items: MutableLiveData<ArrayList<AppItem>> = MutableLiveData()
    val updateCheckEncoded: MutableLiveData<String> = MutableLiveData()

    fun getItems(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val appList = context.packageManager
                .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
            val newItems = ArrayList<AppItem>()
            val updateCheckJsonObject = JSONObject()

            for (info in appList) {
                if (((info.flags and ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM)) {
                    val packageInfo = context.packageManager.getPackageInfo(info.packageName, 0)

                    val appItem = AppItem().apply {
//                        icon = info.loadIcon(context.packageManager)
//                        appName = info.loadLabel(context.packageManager).toString()
                        packageName = info.packageName
                        versionName =
                            "${packageInfo.versionName}(${packageInfo.longVersionCodeCompat})"
                        lastUpdateTime = packageInfo.lastUpdateTime
                    }

                    if (appItem.packageName != "com.example.c001apk")
                        newItems.add(appItem)

                    if (info.packageName != "com.example.c001apk")
                        updateCheckJsonObject.put(
                            info.packageName,
                            "0,${packageInfo.longVersionCodeCompat},${Utils.getInstalledAppMd5(info)}"
                        )
                }
            }

            withContext(Dispatchers.Main) {
                items.value =
                    newItems.sortedByDescending { it.lastUpdateTime }.toCollection(ArrayList())
                updateCheckEncoded.value = updateCheckJsonObject.toString().getBase64(false)
            }
        }
    }

    val appsUpdate = ArrayList<UpdateCheckResponse.Data>()
    private val getAppsUpdateData = MutableLiveData<String>()

    val appsUpdateData = getAppsUpdateData.switchMap {
        updateCheckEncoded.value?.let { it1 -> Repository.getAppsUpdate(it1) }
    }

    fun getAppsUpdate() {
        getAppsUpdateData.value = getAppsUpdateData.value
    }

    val homeFeedList = ArrayList<HomeFeedResponse.Data>()
    var firstLaunch = 1
    var installTime = ""
    var firstItem: String? = null
    var lastItem: String? = null

    private val getHomeFeedData = MutableLiveData<String>()

    val homeFeedData = getHomeFeedData.switchMap {
        if (isRefreshing)
            Repository.getHomeFeed(page, firstLaunch, installTime, firstItem, null)
        else //if (isLoadMore)
            Repository.getHomeFeed(page, firstLaunch, installTime, null, lastItem)
    }

    fun getHomeFeed() {
        getHomeFeedData.value = getHomeFeedData.value
    }

    private val getDataListLiveData = MutableLiveData<String>()

    val dataListData = getDataListLiveData.switchMap {
        Repository.getDataList(url, title, subtitle, lastItem, page)
    }

    fun getDataList() {
        getDataListLiveData.value = getDataListLiveData.value
    }

    var isInitial = true


    var titleList = ArrayList<String>()

    val homeTopicTitleList = ArrayList<HomeFeedResponse.Entities>()

    val topicDataList = ArrayList<HomeFeedResponse.Data>()

    private val getTopicDataLiveData = MutableLiveData<String>()

    val topicDataLiveData = getTopicDataLiveData.switchMap {
        Repository.getDataList(url, title, subtitle, lastItem, page)
    }

    fun getTopicData() {
        getTopicDataLiveData.value = getTopicDataLiveData.value
    }


    val countList = ArrayList<String>()
    val messCountList = ArrayList<Int>()

    var historyList = ArrayList<String>()

    val searchList = ArrayList<HomeFeedResponse.Data>()

    var feedType: String = "all"
    var sort: String = "default" //hot // reply
    var keyWord: String = ""
    var pageType = ""  //"tag"
    var pageParam = "" //topic title
    private var showAnonymous = -1

    private val getSearchData = MutableLiveData<String>()

    val searchData = getSearchData.switchMap {
        Repository.getSearch(
            type,
            feedType,
            sort,
            keyWord,
            pageType,
            pageParam,
            page,
            showAnonymous
        )
    }

    fun getSearch() {
        getSearchData.value = getSearchData.value
    }

    var subtitle: String? = null

    private val getTopicLayoutLiveData = MutableLiveData<String>()

    val topicLayoutLiveData = getTopicLayoutLiveData.switchMap {
        Repository.getTopicLayout(url)
    }

    fun getTopicLayout() {
        getTopicLayoutLiveData.value = getTopicLayoutLiveData.value
    }

    private val getProductLayoutLiveData = MutableLiveData<String>()

    val productLayoutLiveData = getProductLayoutLiveData.switchMap {
        Repository.getProductLayout(id)
    }

    fun getProductLayout() {
        getProductLayoutLiveData.value = getProductLayoutLiveData.value
    }

    var searchTabList = emptyArray<String>()
    var searchFragmentList = ArrayList<Fragment>()


    private val postFollowUnFollowLiveData = MutableLiveData<String>()

    val postFollowUnFollowData = postFollowUnFollowLiveData.switchMap {
        Repository.postFollowUnFollow(url, uid)
    }

    var createFeedData = HashMap<String, String?>()

    fun postFollowUnFollow() {
        postFollowUnFollowLiveData.value = postFollowUnFollowLiveData.value
    }

    private val postCreateFeedLiveData = MutableLiveData<String>()

    val postCreateFeedData = postCreateFeedLiveData.switchMap {
        Repository.postCreateFeed(createFeedData)
    }

    fun postCreateFeed() {
        postCreateFeedLiveData.value = postCreateFeedLiveData.value
    }

    var requestValidateData = HashMap<String, String?>()
    private val postRequestValidateLiveData = MutableLiveData<String>()

    val postRequestValidateData = postCreateFeedLiveData.switchMap {
        Repository.postRequestValidate(requestValidateData)
    }

    fun postRequestValidate() {
        postRequestValidateLiveData.value = postRequestValidateLiveData.value
    }


    val blackListLiveData: MutableLiveData<ArrayList<String>> = MutableLiveData()
    fun getBlackList(type: String, context: Context) {
        val searchHistoryDao = SearchHistoryDatabase.getDatabase(context).searchHistoryDao()
        val blackListDao = BlackListDatabase.getDatabase(context).blackListDao()
        val topicBlacklist = TopicBlackListDatabase.getDatabase(context).blackListDao()
        val newList = ArrayList<String>()
        viewModelScope.launch(Dispatchers.IO) {
            if (type == "history")
                for (element in searchHistoryDao.loadAllHistory()) {
                    newList.add(element.keyWord)
                }
            else if (type == "blacklist")
                for (element in blackListDao.loadAllList()) {
                    newList.add(element.keyWord)
                }
            else if (type == "topicBlacklist")
                for (element in topicBlacklist.loadAllList()) {
                    newList.add(element.keyWord)
                }
            withContext(Dispatchers.Main) {
                blackListLiveData.value = newList
            }
        }
    }

    val browseLiveData: MutableLiveData<ArrayList<Any>> = MutableLiveData()
    fun getBrowseList(type: String, context: Context) {
        val browseHistoryDao = BrowseHistoryDatabase.getDatabase(context).browseHistoryDao()
        val feedFavoriteDao = FeedFavoriteDatabase.getDatabase(context).feedFavoriteDao()
        val newList = ArrayList<Any>()
        viewModelScope.launch(Dispatchers.IO) {
            if (type == "browse")
                for (element in browseHistoryDao.loadAllHistory()) {
                    newList.add(element)
                }
            else
                for (element in feedFavoriteDao.loadAllHistory()) {
                    newList.add(element)
                }
            withContext(Dispatchers.Main) {
                browseLiveData.value = newList
            }
        }
    }

    var totalOptionNum = -1
    var currentOption = 0
    var extraKey = ""
    var leftEnd = false
    var rightEnd = false

    val leftVoteCommentList = ArrayList<TotalReplyResponse.Data>()
    val rightVoteCommentList = ArrayList<TotalReplyResponse.Data>()
    var voteCommentList = ArrayList<TotalReplyResponse.Data>()

    private val getVoteCommentListData = MutableLiveData<String>()

    val voteCommentData = getVoteCommentListData.switchMap {
        Repository.getVoteComment(id, extraKey, page, firstItem, lastItem)
    }

    fun getVoteComment() {
        getVoteCommentListData.value = getVoteCommentListData.value
    }

    private val getProductListData = MutableLiveData<String>()

    val productCategoryData = getProductListData.switchMap {
        Repository.getProductList()
    }

    fun getProductList() {
        getProductListData.value = getProductListData.value
    }

    var collectionUrl = ""
    var cId: String? = null
    var cUid: String? = null
    private val getCollectionListData = MutableLiveData<String>()

    val collectionListData = getCollectionListData.switchMap {
        Repository.getCollectionList(collectionUrl, cUid, cId, 0, page)
    }

    fun getCollectionList() {
        getCollectionListData.value = getCollectionListData.value
    }

    private val postDeleteLiveData = MutableLiveData<String>()

    val postDeleteData = postDeleteLiveData.switchMap {
        Repository.postDelete(url, deleteId)
    }

    var deleteId = ""

    fun postDelete() {
        postDeleteLiveData.value = postDeleteLiveData.value
    }

    var postFollow = HashMap<String, String>()

    private val postFollowLiveData = MutableLiveData<String>()

    val postFollowData = postFollowLiveData.switchMap {
        Repository.postFollow(postFollow)
    }

    fun postFollow() {
        postFollowLiveData.value = postFollowLiveData.value
    }

    var tag: String? = null
    var fid: String? = null
    var followUrl = ""
    private val getFollowLiveData = MutableLiveData<String>()

    val getFollowData = getFollowLiveData.switchMap {
        Repository.getFollow(followUrl, tag, fid)
    }

    fun getFollow() {
        getFollowLiveData.value = getFollowLiveData.value
    }

}