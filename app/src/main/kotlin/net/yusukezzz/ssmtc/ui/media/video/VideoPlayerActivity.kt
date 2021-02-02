package net.yusukezzz.ssmtc.ui.media.video

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import net.yusukezzz.ssmtc.data.api.model.VideoInfo
import net.yusukezzz.ssmtc.databinding.VideoPlayerBinding
import net.yusukezzz.ssmtc.ui.media.MediaBaseActivity
import net.yusukezzz.ssmtc.util.TextUtil
import net.yusukezzz.ssmtc.util.gone
import net.yusukezzz.ssmtc.util.visible

class VideoPlayerActivity : MediaBaseActivity(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener {

    companion object {
        const val ARG_VIDEO_INFO = "video_info"

        fun newIntent(context: Context, video: VideoInfo): Intent =
            Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(ARG_VIDEO_INFO, video)
            }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateTask: Runnable by lazy {
        Runnable {
            updateProgress(binding.mediaVideo.currentPosition)
            handler.postDelayed(updateTask, 100)
        }
    }
    private var totalDurationMilliSec: Int = 0
    private var totalDurationTime: String = ""

    private lateinit var binding: VideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getParcelableExtra<VideoInfo>(ARG_VIDEO_INFO)?.let {
            binding.mediaVideo.setOnPreparedListener(this)
            binding.mediaVideo.setOnCompletionListener(this)
            binding.mediaVideo.setVideoURI(Uri.parse(it.mp4Mid.url))
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        binding.videoLoadingBar.gone()
        binding.videoTime.visible()

        totalDurationMilliSec = binding.mediaVideo.duration
        totalDurationTime = TextUtil.milliSecToTime(totalDurationMilliSec)
        handler.post(updateTask)

        binding.mediaVideo.start()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        handler.removeCallbacks(updateTask)
        updateProgress(totalDurationMilliSec)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateTask)
        binding.mediaVideo.stopPlayback()
        super.onDestroy()
    }

    private fun updateProgress(currentMilliSec: Int) {
        val currentTime = TextUtil.milliSecToTime(currentMilliSec)
        binding.videoTime.text = "$currentTime/$totalDurationTime"
    }
}
