package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.BasePresenter
import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.model.TwList
import net.yusukezzz.ssmtc.data.api.model.Tweet

interface TimelineContract {
    interface Presenter: BasePresenter {
        fun setTimeline(timeline: Timeline)
        fun setTokens(credentials: Credentials)
        fun loadTweets(maxId: Long? = null)
        fun loadLists(userId: Long)
        fun like(tweet: Tweet)
        fun retweet(tweet: Tweet)
        fun resetIgnoreIds()
    }

    interface View {
        fun setLastTweetId(id: Long?)
        fun setTweets(tweets: List<Tweet>)
        fun addTweets(tweets: List<Tweet>)
        fun showListsSelector(lists: List<TwList>)
        fun showListsLoading()
        fun dismissListsLoading()
        fun timelineEdgeReached()
        fun stopLoading()
        fun updateReactedTweet()
        fun handleError(error: Throwable)
    }
}
