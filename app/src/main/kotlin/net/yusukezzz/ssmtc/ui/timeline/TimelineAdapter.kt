package net.yusukezzz.ssmtc.ui.timeline

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.ViewGroup
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.og.OpenGraphService
import net.yusukezzz.ssmtc.ui.timeline.TweetItemView.TweetItemListener
import net.yusukezzz.ssmtc.util.inflate

class TimelineAdapter(val listener: TweetItemListener,
                      private val ogClient: OpenGraphService) : RecyclerView.Adapter<ViewHolder>() {
    companion object {
        private class TweetViewHolder(val view: TweetItemView) : ViewHolder(view) {
            fun bind(tweet: Tweet) = view.bind(tweet)
            fun cleanup() = view.cleanup()
        }
    }

    private var timeline: List<Tweet> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tweetView = parent.inflate(R.layout.tweet_item) as TweetItemView
        tweetView.setTweetListener(listener)
        tweetView.setOpenGraphClient(ogClient)

        return TweetViewHolder(tweetView)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) = (holder as TweetViewHolder).bind(timeline[pos])

    override fun onViewRecycled(holder: ViewHolder) = (holder as TweetViewHolder).cleanup()

    override fun getItemCount(): Int = timeline.size
    override fun getItemId(pos: Int): Long = timeline[pos].id

    fun getAll(): List<Tweet> = timeline.toList()
    fun clear() {
        timeline = listOf()
    }

    fun set(tweets: List<Tweet>) {
        clear()
        add(tweets)
    }

    fun add(tweets: List<Tweet>) {
        timeline += tweets
        notifyDataSetChanged()
    }
}
