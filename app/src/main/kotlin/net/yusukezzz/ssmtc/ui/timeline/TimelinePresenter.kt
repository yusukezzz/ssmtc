package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.api.model.Tweet
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.alwaysUi

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

    override fun loadLists(userId: Long) {
        view.showListsLoading()

        task {
            twitter.ownedLists(userId)
        } and task {
            twitter.subscribedLists(userId)
        } doneUi {
            view.showListsSelector(it.first + it.second)
        } alwaysUi {
            view.dismissListsLoading()
        }
    }

    override fun like(tweet: Tweet) {
        if (tweet.favorited) {
            unlike(tweet)
        } else {
            task {
                twitter.like(tweet.id)
            } doneUi {
                tweet.favorite_count++
                tweet.favorited = true
                view.updateReactedTweet()
            }
        }
    }

    private fun unlike(tweet: Tweet) {
        task {
            twitter.unlike(tweet.id)
        } doneUi {
            tweet.favorite_count--
            tweet.favorited = false
            view.updateReactedTweet()
        }
    }

    override fun retweet(tweet: Tweet) {
        if (tweet.retweeted) {
            unretweet(tweet)
        } else {
            task {
                twitter.retweet(tweet.id)
            } doneUi {
                tweet.retweet_count++
                tweet.retweeted = true
                view.updateReactedTweet()
            }
        }
    }

    private fun unretweet(tweet: Tweet) {
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
