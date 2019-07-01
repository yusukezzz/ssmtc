package net.yusukezzz.ssmtc.ui.timeline

import kotlinx.coroutines.Job
import net.yusukezzz.ssmtc.BasePresenter
import net.yusukezzz.ssmtc.BaseView
import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.model.TwList
import net.yusukezzz.ssmtc.data.api.model.Tweet

interface TimelineContract {
    interface Presenter : BasePresenter {
        fun setTimeline(timeline: Timeline)
        fun setTokens(credentials: Credentials)
        fun loadTweets(maxId: Long? = null): Job
        fun loadLists(userId: Long): Job
        fun like(tweet: Tweet)
        fun retweet(tweet: Tweet)
        fun resetIgnoreIds()
    }

    interface View : BaseView {
        fun setLastTweetId(id: Long?)
        fun setTweets(tweets: List<Tweet>)
        fun addTweets(tweets: List<Tweet>)
        fun showListsSelector(lists: List<TwList>)
        fun showListsLoading()
        fun dismissListsLoading()
        fun timelineEdgeReached()
        fun stopLoading()
        fun rateLimitExceeded()
        fun updateReactedTweet()
    }
}
