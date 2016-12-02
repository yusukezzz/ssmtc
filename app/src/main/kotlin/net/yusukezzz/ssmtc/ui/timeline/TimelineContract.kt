package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.BasePresenter
import net.yusukezzz.ssmtc.data.json.Tweet

interface TimelineContract {
    interface Presenter: BasePresenter {
        fun loadTweets(maxId: Long? = null)
        fun like(tweet: Tweet)
        fun unlike(tweet: Tweet)
        fun retweet(tweet: Tweet)
        fun unretweet(tweet: Tweet)
    }

    interface View {
        fun setLastTweetId(id: Long?)
        fun setTweets(tweets: List<Tweet>)
        fun addTweets(tweets: List<Tweet>)
        fun stopLoading()
        fun updateReactedTweet()
        fun initialize()
        fun handleError(error: Throwable)
    }
}
