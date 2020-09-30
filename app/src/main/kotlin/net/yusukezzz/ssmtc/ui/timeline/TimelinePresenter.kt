package net.yusukezzz.ssmtc.ui.timeline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.TwitterApiError
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.util.async
import net.yusukezzz.ssmtc.util.withIO
import org.threeten.bp.OffsetDateTime

class TimelinePresenter(
    override val view: TimelineContract.View,
    private val twitter: TwitterService
) : TimelineContract.Presenter {
    companion object {
        // block & mute ids API rate limit is 15req/15min
        const val IGNORE_IDS_CACHE_SECONDS: Long = 60L
    }

    private var ignoreUserIds: List<Long> = listOf()
    private var ignoreIdsLastUpdatedAt: OffsetDateTime =
        OffsetDateTime.now().minusSeconds(IGNORE_IDS_CACHE_SECONDS)
    private lateinit var timeline: Timeline

    override fun setTimeline(timeline: Timeline) {
        this.timeline = timeline
    }

    override fun resetIgnoreIds() {
        ignoreUserIds = listOf()
        ignoreIdsLastUpdatedAt = OffsetDateTime.now().minusSeconds(IGNORE_IDS_CACHE_SECONDS)
    }

    override fun setTokens(credentials: Credentials) = twitter.setTokens(credentials)

    /**
     * Load tweet from timeline API
     *
     * @param maxId
     */
    override fun loadTweets(maxId: Long?): Job = task({
        val tweets = fetchTweetsAndUpdateIgnoreIds(maxId).await()
        if (tweets.isEmpty()) {
            view.timelineEdgeReached()
        } else {
            // save last tweet id before filtering
            tweets.lastOrNull()?.let { tw -> view.setLastTweetId(tw.id) }
            val filtered = tweets.filter { isVisible(it) }
            if (maxId == null) {
                view.setTweets(filtered)
            } else {
                view.addTweets(filtered)
            }
        }
    }, view::stopLoading)

    private fun isVisible(tw: Tweet): Boolean = timeline.filter.match(tw) && !isIgnoreUser(tw)
    private fun isIgnoreUser(tw: Tweet): Boolean {
        fun containsIgnoreUser(tweet: Tweet): Boolean = ignoreUserIds.contains(tweet.user.id)
        val shouldTweetIgnore = containsIgnoreUser(tw)
        val shouldRetweetIgnore = tw.retweeted_status?.let { containsIgnoreUser(it) } ?: false
        val shouldQuotedIgnore = tw.quoted_status?.let { containsIgnoreUser(it) } ?: false

        return shouldTweetIgnore || shouldRetweetIgnore || shouldQuotedIgnore
    }

    private suspend fun CoroutineScope.fetchTweetsAndUpdateIgnoreIds(
        maxId: Long?,
        now: OffsetDateTime = OffsetDateTime.now()
    ): Deferred<List<Tweet>> {
        val fetchTweetsTask = async { twitter.statuses(timeline, maxId) }
        return async {
            try {
                if (shouldIgnoreIdsUpdate(now)) {
                    updateIgnoreIdsTask()
                }
                fetchTweetsTask.await().getOrThrow()
            } catch (e: Throwable) {
                println(e)
                listOf()
            }
        }
    }

    private fun shouldIgnoreIdsUpdate(now: OffsetDateTime): Boolean =
        now.isAfter(ignoreIdsLastUpdatedAt.plusSeconds(IGNORE_IDS_CACHE_SECONDS))

    private suspend fun CoroutineScope.updateIgnoreIdsTask(): Unit = withIO {
        val blockIds = async { twitter.blockedIds().getOrNull()?.ids ?: listOf() }
        val muteIds = async { twitter.mutedIds().getOrNull()?.ids ?: listOf() }
        // save blocked and muted user ids
        ignoreIdsLastUpdatedAt = OffsetDateTime.now()
        ignoreUserIds = (blockIds.await() + muteIds.await()).distinct()
    }

    override fun loadLists(userId: Long): Job = task({
        view.showListsLoading()
        val owned = async { twitter.ownedLists(userId).getOrNull()?.lists ?: listOf() }
        val subscribed = async { twitter.subscribedLists(userId).getOrNull()?.lists ?: listOf() }
        view.showListsSelector(owned.await() + subscribed.await())
    }, view::dismissListsLoading)

    override fun like(tweet: Tweet) {
        if (tweet.favorited) {
            unlike(tweet)
        } else {
            task({
                twitter.like(tweet.id)
                tweet.favorite_count++
                tweet.favorited = true
                view.updateReactedTweet()
            })
        }
    }

    private fun unlike(tweet: Tweet) = task({
        twitter.unlike(tweet.id)
        tweet.favorite_count--
        tweet.favorited = false
        view.updateReactedTweet()
    })

    override fun retweet(tweet: Tweet) {
        if (tweet.retweeted) {
            unretweet(tweet)
        } else {
            task({
                twitter.retweet(tweet.id)
                tweet.retweet_count++
                tweet.retweeted = true
                view.updateReactedTweet()
            })
        }
    }

    private fun unretweet(tweet: Tweet) = task({
        twitter.unretweet(tweet.id)
        tweet.retweet_count--
        tweet.retweeted = false
        view.updateReactedTweet()
    })

    override fun handleError(error: Throwable) {
        if (error is TwitterApiError && error.isRateLimitExceeded()) {
            view.rateLimitExceeded()
        } else {
            view.handleError(error)
        }
    }
}
