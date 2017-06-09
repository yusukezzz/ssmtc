package net.yusukezzz.ssmtc.ui.timeline

import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.api.model.Tweet
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.alwaysUi
import org.threeten.bp.OffsetDateTime

class TimelinePresenter(private val view: TimelineContract.View,
                        private val twitter: Twitter) : TimelineContract.Presenter {
    companion object {
        // block & mute ids API rate limit is 15req/15min
        const val IGNORE_IDS_CACHE_SECONDS: Long = 60L
    }
    private var ignoreIds: List<Long> = listOf()
    private var ignoreIdsLastUpdatedAt: OffsetDateTime = OffsetDateTime.now().minusSeconds(IGNORE_IDS_CACHE_SECONDS)
    private lateinit var timeline: Timeline

    override fun setTimeline(timeline: Timeline) {
        this.timeline = timeline
    }

    override fun setTokens(credentials: Credentials): Unit = twitter.setTokens(credentials)

    /**
     * Load tweet from timeline API
     *
     * @param maxId
     */
    override fun loadTweets(maxId: Long?) {
        fetchTweetsAndUpdateIgnoreIds(maxId) doneUi { tweets ->
            // save last tweet id before filtering
            tweets.lastOrNull()?.let { tw -> view.setLastTweetId(tw.id) }
            val filtered = tweets.filter { tw -> timeline.filter.match(tw) && ignoreIds.contains(tw.user.id).not() }
            if (maxId == null) {
                view.setTweets(filtered)
            } else {
                view.addTweets(filtered)
            }
        } alwaysUi {
            view.stopLoading()
        }
    }

    private fun fetchTweetsAndUpdateIgnoreIds(maxId: Long?,
                                              now: OffsetDateTime = OffsetDateTime.now()): Promise<List<Tweet>, Exception> {
        val fetchTweetsTask = task { twitter.tweets(timeline, maxId) }
        return if (shouldIgnoreIdsUpdate(now)) {
            updateIgnoreIdsTask() and fetchTweetsTask then { it.second }
        } else {
            // use cached ignoreIds
            fetchTweetsTask
        }
    }

    private fun shouldIgnoreIdsUpdate(now: OffsetDateTime): Boolean =
        now.isAfter(ignoreIdsLastUpdatedAt.plusSeconds(IGNORE_IDS_CACHE_SECONDS))

    private fun updateIgnoreIdsTask(): Promise<Unit, Exception> = task {
        twitter.blockedIds().ids
    } and task {
        twitter.mutedIds().ids
    } then {
        // save blocked and muted user ids
        ignoreIdsLastUpdatedAt = OffsetDateTime.now()
        ignoreIds = (it.first + it.second).distinct()
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
