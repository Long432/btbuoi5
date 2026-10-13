package com.example.coursemanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.example.coursemanager.databinding.ActivityTrailerBinding
/*5*/
class TrailerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }

    private lateinit var binding: ActivityTrailerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrailerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = intent.getStringExtra(EXTRA_TITLE) ?: "Trailer"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()

        // Phương án tương thích cho YouTube: VideoView không phát được -> mở app YouTube / trình duyệt
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            showError("Link YouTube không phát trực tiếp được trong ứng dụng.", openUrl = url)
            return
        }

        binding.progress.visibility = View.VISIBLE
        val controller = MediaController(this)
        controller.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(controller)
        binding.videoView.setOnPreparedListener {
            binding.progress.visibility = View.GONE
            binding.videoView.start()
        }
        binding.videoView.setOnErrorListener { _, _, _ ->
            binding.progress.visibility = View.GONE
            showError("Không phát được video. Hãy kiểm tra URL (cần là link MP4 trực tiếp, bắt đầu bằng https://) và kết nối mạng.", null)
            true
        }
        binding.videoView.setVideoURI(Uri.parse(url))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showError(message: String, openUrl: String?) {
        binding.tvError.text = message
        binding.btnOpenExternal.visibility = if (openUrl != null) View.VISIBLE else View.GONE
        binding.btnOpenExternal.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(openUrl)))
        }
        binding.layoutError.visibility = View.VISIBLE
    }
}
