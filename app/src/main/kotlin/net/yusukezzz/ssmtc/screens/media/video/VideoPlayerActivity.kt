package net.yusukezzz.ssmtc.screens.media.video

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import kotlinx.android.synthetic.main.video_player.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.json.VideoInfo
import net.yusukezzz.ssmtc.data.json.VideoInfoParcel
import net.yusukezzz.ssmtc.screens.media.MediaBaseActivity
import net.yusukezzz.ssmtc.util.TextUtil

class VideoPlayerActivity: MediaBaseActivity(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener {

    companion object {
        val ARG_VIDEO_INFO = "video_info"

        fun newIntent(context: Context, video: VideoInfo): Intent =
            Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(ARG_VIDEO_INFO, VideoInfoParcel(video))
            }
    }

    private val handler = Handler()
    private val updateTask: Runnable by lazy {
        Runnable {
            updateProgress(media_video.currentPosition)
            handler.postDelayed(updateTask, 100)
        }
    }
    private var totalDurationMilliSec: Int = 0
    private var totalDurationTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_player)

        val info = intent.getParcelableExtra<VideoInfoParcel>(ARG_VIDEO_INFO).data

        media_video.setOnPreparedListener(this)
        media_video.setOnCompletionListener(this)
        media_video.setVideoURI(Uri.parse(info.mp4Mid.url))
    }

    override fun onPrepared(mp: MediaPlayer?) {
        video_loading_bar.visibility = View.GONE
        video_time.visibility = View.VISIBLE

        totalDurationMilliSec = media_video.duration
        totalDurationTime = TextUtil.milliSecToTime(totalDurationMilliSec)
        handler.post(updateTask)

        media_video.start()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        handler.removeCallbacks(updateTask)
        updateProgress(totalDurationMilliSec)
    }

    override fun onStop() {
        handler.removeCallbacks(updateTask)
        super.onStop()
    }

    private fun updateProgress(currentMilliSec: Int) {
        val currentTime = TextUtil.milliSecToTime(currentMilliSec)
        video_time.text = "$currentTime/$totalDurationTime"
    }
}
