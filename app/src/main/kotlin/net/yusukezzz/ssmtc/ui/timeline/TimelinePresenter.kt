package net.yusukezzz.ssmtc.ui.timeline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.TwitterApiException
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.util.async
import org.threeten.bp.OffsetDateTime

class TimelinePresenter(private val view: TimelineContract.View,
                        private val twitter: TwitterService) : TimelineContract.Presenter {
    companion object {
        // block & mute ids API rate limit is 15req/15min
        const val IGNORE_IDS_CACHE_SECONDS: Long = 60L
    }
    private var ignoreUserIds: List<Long> = listOf()
    private var ignoreIdsLastUpdatedAt: OffsetDateTime = OffsetDateTime.now().minusSeconds(IGNORE_IDS_CACHE_SECONDS)
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
    override fun loadTweets(maxId: Long?): Job = execOnUi({
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
        val shouldRetweetIgnore = tw.retweeted_status?.let{ containsIgnoreUser(it) } ?: false
        val shouldQuotedIgnore = tw.quoted_status?.let { containsIgnoreUser(it) } ?: false

        return shouldTweetIgnore || shouldRetweetIgnore || shouldQuotedIgnore
    }

    private fun CoroutineScope.fetchTweetsAndUpdateIgnoreIds(maxId: Long?,
                                                             now: OffsetDateTime = OffsetDateTime.now()): Deferred<List<Tweet>> {
        val fetchTweetsTask = async { twitter.statuses(timeline, maxId) }
        return async {
            if (shouldIgnoreIdsUpdate(now)) {
                updateIgnoreIdsTask().await()
                fetchTweetsTask.await()
            } else {
                // use cached ignoreUserIds
                fetchTweetsTask.await()
            }
        }
    }

    private fun shouldIgnoreIdsUpdate(now: OffsetDateTime): Boolean =
        now.isAfter(ignoreIdsLastUpdatedAt.plusSeconds(IGNORE_IDS_CACHE_SECONDS))

    private fun CoroutineScope.updateIgnoreIdsTask(): Deferred<Unit> = async {
        val blockIds = async { twitter.blockedIds().ids }
        val muteIds = async { twitter.mutedIds().ids }
        // save blocked and muted user ids
        ignoreIdsLastUpdatedAt = OffsetDateTime.now()
        ignoreUserIds = (blockIds.await() + muteIds.await()).distinct()
    }

    override fun loadLists(userId: Long): Job = execOnUi({
        view.showListsLoading()
        val owned = async { twitter.ownedLists(userId) }
        val subscribed = async { twitter.subscribedLists(userId) }
        view.showListsSelector(owned.await() + subscribed.await())
    }, view::dismissListsLoading)

    override fun like(tweet: Tweet) {
        if (tweet.favorited) {
            unlike(tweet)
        } else {
            execOnUi({
                async { twitter.like(tweet.id) }.await()
                tweet.favorite_count++
                tweet.favorited = true
                view.updateReactedTweet()
            })
        }
    }

    private fun unlike(tweet: Tweet) = execOnUi({
        async { twitter.unlike(tweet.id) }.await()
        tweet.favorite_count--
        tweet.favorited = false
        view.updateReactedTweet()
    })

    override fun retweet(tweet: Tweet) {
        if (tweet.retweeted) {
            unretweet(tweet)
        } else {
            execOnUi({
                async { twitter.retweet(tweet.id) }.await()
                tweet.retweet_count++
                tweet.retweeted = true
                view.updateReactedTweet()
            })
        }
    }

    private fun unretweet(tweet: Tweet) = execOnUi({
        async { twitter.unretweet(tweet.id) }.await()
        tweet.retweet_count--
        tweet.retweeted = false
        view.updateReactedTweet()
    })

    override fun handleError(error: Throwable) {
        if (error is TwitterApiException && error.isRateLimitExceeded()) {
            view.rateLimitExceeded()
        } else {
            view.handleError(error)
        }
    }

}
