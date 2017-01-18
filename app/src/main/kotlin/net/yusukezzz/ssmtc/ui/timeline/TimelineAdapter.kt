package net.yusukezzz.ssmtc.ui.timeline

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.ViewGroup
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.ui.timeline.TweetItemView.TweetItemListener
import net.yusukezzz.ssmtc.util.inflate

class TimelineAdapter(val listener: TweetItemListener) : RecyclerView.Adapter<ViewHolder>() {
    companion object {
        private class TweetViewHolder(private val view: TweetItemView) : ViewHolder(view) {
            fun bindTo(tweet: Tweet) = {
                if (tweet.isRetweet) {
                    view.bindRetweeted(tweet)
                } else if (tweet.isRetweetWithQuoted) {
                    view.bindQuoted(tweet)
                } else {
                    view.bindTweet(tweet)
                }
            }

            fun cleanup() = view.cleanup()
        }
    }

    private val timeline: MutableList<Tweet> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tweetView = parent.inflate(R.layout.tweet_item) as TweetItemView
        tweetView.setTweetListener(listener)

        return TweetViewHolder(tweetView)
    }

    override fun onViewRecycled(holder: ViewHolder): Unit {
        (holder as TweetViewHolder).cleanup()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int): Unit {
        (holder as TweetViewHolder).bindTo(timeline[position])
    }

    override fun getItemCount(): Int = timeline.size

    fun get(pos: Int): Tweet = timeline[pos]
    fun getAll(): List<Tweet> = timeline.toList()
    fun clear() = timeline.clear()

    fun set(tweets: List<Tweet>) {
        timeline.clear()
        add(tweets)
    }

    fun add(tweets: List<Tweet>) {
        timeline.addAll(tweets)
        notifyDataSetChanged()
    }
}
