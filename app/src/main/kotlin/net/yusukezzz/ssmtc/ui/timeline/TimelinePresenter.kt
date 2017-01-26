package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.api.model.Tweet
import nl.komponents.kovenant.task

class TimelinePresenter(val view: TimelineContract.View, val twitter: Twitter, private val param: TimelineParameter) : TimelineContract.Presenter {
    /**
     * Load tweet from timeline API
     *
     * @param maxId
     */
    override fun loadTweets(maxId: Long?) {
        task {
            twitter.timeline(param, maxId)
        } doneUi { tweets ->
            // save last tweet id before filtering
            tweets.lastOrNull()?.let { view.setLastTweetId(it.id) }
            val filtered = tweets.filter { param.filter.match(it) }
            if (maxId == null) {
                view.setTweets(filtered)
            } else {
                view.addTweets(filtered)
            }
            view.stopLoading()
        }
    }

    override fun like(tweet: Tweet) {
        task {
            twitter.like(tweet.id)
        } doneUi {
            tweet.favorite_count++
            tweet.favorited = true
            view.updateReactedTweet()
        }
    }

    override fun unlike(tweet: Tweet) {
        task {
            twitter.unlike(tweet.id)
        } doneUi {
            tweet.favorite_count--
            tweet.favorited = false
            view.updateReactedTweet()
        }
    }

    override fun retweet(tweet: Tweet) {
        task {
            twitter.retweet(tweet.id)
        } doneUi {
            tweet.retweet_count++
            tweet.retweeted = true
            view.updateReactedTweet()
        }
    }

    override fun unretweet(tweet: Tweet) {
        task {
            twitter.unretweet(tweet.id)
        } doneUi {
            tweet.retweet_count--
            tweet.retweeted = false
            view.updateReactedTweet()
        }
    }

    override fun handleError(error: Throwable) {
        view.handleError(error)
    }

}
