package net.yusukezzz.ssmtc.ui.media.video

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import kotlinx.android.synthetic.main.video_player.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.VideoInfo
import net.yusukezzz.ssmtc.ui.media.MediaBaseActivity
import net.yusukezzz.ssmtc.util.TextUtil
import net.yusukezzz.ssmtc.util.gone
import net.yusukezzz.ssmtc.util.visible

class VideoPlayerActivity: MediaBaseActivity(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener {

    companion object {
        val ARG_VIDEO_INFO = "video_info"

        fun newIntent(context: Context, video: VideoInfo): Intent =
            Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(ARG_VIDEO_INFO, video)
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

        val info: VideoInfo = intent.getParcelableExtra(ARG_VIDEO_INFO)

        media_video.setOnPreparedListener(this)
        media_video.setOnCompletionListener(this)
        media_video.setVideoURI(Uri.parse(info.mp4Mid.url))
    }

    override fun onPrepared(mp: MediaPlayer?) {
        video_loading_bar.gone()
        video_time.visible()

        totalDurationMilliSec = media_video.duration
        totalDurationTime = TextUtil.milliSecToTime(totalDurationMilliSec)
        handler.post(updateTask)

        media_video.start()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        handler.removeCallbacks(updateTask)
        updateProgress(totalDurationMilliSec)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateTask)
        media_video.setOnPreparedListener(null)
        media_video.setOnCompletionListener(null)
        super.onDestroy()
    }

    private fun updateProgress(currentMilliSec: Int) {
        val currentTime = TextUtil.milliSecToTime(currentMilliSec)
        video_time.text = "$currentTime/$totalDurationTime"
    }
}
