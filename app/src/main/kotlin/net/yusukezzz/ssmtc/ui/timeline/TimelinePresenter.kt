package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.services.TimelineParameter
import net.yusukezzz.ssmtc.services.Twitter
import nl.komponents.kovenant.task

class TimelinePresenter(val view: TimelineContract.View, val twitter: Twitter): TimelineContract.Presenter {
    private lateinit var param: TimelineParameter
    private var latestTweetId: Long? = null
    private var lastTweetId: Long? = null

    override fun setParameter(param: TimelineParameter) {
        this.param = param
        view.initialize()
    }

    override fun loadNewerTweets() {
        task {
            twitter.timeline(param.next(latestTweetId))
        } doneUi {
            latestTweetId = it.first()?.id
            view.addHeadTweets(it)
        }
    }

    override fun loadOlderTweets() {
        task {
            twitter.timeline(param.previous(lastTweetId))
        } doneUi {
            // save last tweet id before filtering
            lastTweetId = it.last()?.id
            view.addTailTweets(param.filter.shorten(it))
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
