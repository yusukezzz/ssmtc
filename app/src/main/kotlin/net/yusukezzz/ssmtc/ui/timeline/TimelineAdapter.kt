package net.yusukezzz.ssmtc.ui.timeline

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.og.OpenGraphService
import net.yusukezzz.ssmtc.ui.timeline.TweetItemView.TweetItemListener
import net.yusukezzz.ssmtc.util.inflate

class TimelineAdapter(
    val listener: TweetItemListener,
    private val ogClient: OpenGraphService
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private class TweetViewHolder(val view: TweetItemView) : RecyclerView.ViewHolder(view) {
            fun bind(tweet: Tweet) = view.bind(tweet)
            fun cleanup() = view.cleanup()
        }
    }

    private var timeline: List<Tweet> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val tweetView = parent.inflate(R.layout.tweet_item) as TweetItemView
        tweetView.setTweetListener(listener)
        tweetView.setOpenGraphClient(ogClient)

        return TweetViewHolder(tweetView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) =
        (holder as TweetViewHolder).bind(timeline[pos])

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) =
        (holder as TweetViewHolder).cleanup()

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
        timeline = timeline + tweets
        notifyDataSetChanged()
    }
}
