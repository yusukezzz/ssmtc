package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.services.TimelineParameter
import net.yusukezzz.ssmtc.services.Twitter
import nl.komponents.kovenant.task

class TimelinePresenter(val view: TimelineContract.View, val twitter: Twitter, private val param: TimelineParameter) : TimelineContract.Presenter {
    init {
        view.initialize()
    }

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
            if (maxId == null) {
                view.setTweets(applyFilter(tweets))
            } else {
                view.addTweets(applyFilter(tweets))
            }
        }
    }

    private fun applyFilter(tweets: List<Tweet>): List<Tweet> {
        return param.filter.shorten(tweets)
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
