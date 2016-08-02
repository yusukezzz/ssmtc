package net.yusukezzz.ssmtc.screens.timeline

import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.services.TimelineParameter
import net.yusukezzz.ssmtc.services.Twitter
import nl.komponents.kovenant.task

class TimelinePresenter(val view: TimelineContract.View, val twitter: Twitter): TimelineContract.Presenter {
    init {
        view.setPresenter(this)
    }

    private lateinit var param: TimelineParameter

    override fun setParameter(param: TimelineParameter) {
        this.param = param
        view.initialize()
    }

    override fun loadNewerTweets() {
        task {
            twitter.timeline(param.next(view.getLatestTweetId()))
        } doneUi {
            view.addHeadTweets(it)
        }
    }

    override fun loadGapTweets(gapPosition: Int, sinceId: Long, maxId: Long) {
        task {
            twitter.timeline(param.gap(sinceId, maxId))
        } doneUi {
            view.addGapTweets(gapPosition, it)
        }
    }

    override fun loadOlderTweets() {
        task {
            twitter.timeline(param.previous(view.getLastTweetId()))
        } doneUi {
            view.addTailTweets(it)
        }
    }

    override fun like(tweet: Tweet) {
        task { twitter.like(tweet.id) } doneUi {
            tweet.favorite_count++
            tweet.favorited = true
            view.updateReactedTweet()
        }
    }

    override fun unlike(tweet: Tweet) {
        task { twitter.unlike(tweet.id) } doneUi {
            tweet.favorite_count--
            tweet.favorited = false
            view.updateReactedTweet()
        }
    }

    override fun retweet(tweet: Tweet) {
        task { twitter.retweet(tweet.id) } doneUi {
            tweet.retweet_count++
            tweet.retweeted = true
            view.updateReactedTweet()
        }
    }

    override fun unretweet(tweet: Tweet) {
        task { twitter.unretweet(tweet.id) } doneUi {
            tweet.retweet_count--
            tweet.retweeted = false
            view.updateReactedTweet()
        }
    }

    override fun handleError(error: Throwable) {
        view.handleError(error)
    }

}
