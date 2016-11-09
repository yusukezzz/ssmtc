package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.BasePresenter
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.services.TimelineParameter

interface TimelineContract {
    interface Presenter: BasePresenter {
        fun setParameter(param: TimelineParameter)
        fun loadNewerTweets()
        fun loadOlderTweets()
        fun like(tweet: Tweet)
        fun unlike(tweet: Tweet)
        fun retweet(tweet: Tweet)
        fun unretweet(tweet: Tweet)
    }

    interface View {
        fun addHeadTweets(tweets: List<Tweet>)
        fun addTailTweets(tweets: List<Tweet>)
        fun updateReactedTweet()
        fun initialize()
        fun handleError(error: Throwable)
    }
}
