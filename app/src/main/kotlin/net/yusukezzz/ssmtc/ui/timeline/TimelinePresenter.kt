package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.data.Credential
import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.api.model.Tweet
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.combine.Tuple2
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.alwaysUi

class TimelinePresenter(private val view: TimelineContract.View,
                        private val twitter: Twitter) : TimelineContract.Presenter {
    private var ignoreIds: List<Long> = listOf()
    private lateinit var param: TimelineParameter

    override fun setTimelineParameter(param: TimelineParameter) {
        this.param = param
    }

    override fun setTokens(credential: Credential) {
        twitter.setTokens(credential.token, credential.tokenSecret)
    }

    /**
     * Load tweet from timeline API
     *
     * @param maxId
     */
    override fun loadTweets(maxId: Long?) {
        fetchTweetsWithIgnoreIds(maxId) doneUi {
            val tweets = it.first
            val ignores = it.second
            // save last tweet id before filtering
            tweets.lastOrNull()?.let { tw -> view.setLastTweetId(tw.id) }
            val filtered = tweets.filter { tw -> param.filter.match(tw) && ignores.contains(tw.user.id).not() }
            if (maxId == null) {
                view.setTweets(filtered)
            } else {
                view.addTweets(filtered)
            }
        } alwaysUi {
            view.stopLoading()
        }
    }

    private fun fetchTweetsWithIgnoreIds(maxId: Long?): Promise<Tuple2<List<Tweet>, List<Long>>, Exception> = if (maxId == null) {
        task { twitter.timeline(param, maxId) } and updateIgnoreIdsTask()
    } else {
        // use cached ignoreIds
        task { Tuple2(twitter.timeline(param, maxId), ignoreIds) }
    }

    private fun updateIgnoreIdsTask(): Promise<List<Long>, Exception> = task {
        twitter.blockedIds().ids
    } and task {
        twitter.mutedIds().ids
    } then {
        // save blocked and muted user ids
        ignoreIds = (it.first + it.second).distinct()
        ignoreIds
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
