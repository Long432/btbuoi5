package com.example.coursemanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coursemanager.data.ApiClient
import com.example.coursemanager.data.Category
import com.example.coursemanager.data.Course
import com.example.coursemanager.data.categoriesOrEmpty
import com.example.coursemanager.databinding.ActivityCourseDetailBinding
import com.example.coursemanager.util.clean
import com.example.coursemanager.util.loadUrl
import com.example.coursemanager.util.safeCall
import com.example.coursemanager.util.toUserMessage
import com.example.coursemanager.util.toVnd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**2*/
class CourseDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COURSE_ID = "course_id"
    }

    private lateinit var binding: ActivityCourseDetailBinding
    private var courseId = ""
    private var course: Course? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Chi tiết khóa học"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        courseId = intent.getStringExtra(EXTRA_COURSE_ID).orEmpty()
        if (courseId.isEmpty()) {
            finish()
            return
        }

        binding.btnTrailer.setOnClickListener {
            val c = course ?: return@setOnClickListener
            if (c.trailerUrl.isBlank()) {
                Toast.makeText(this, "Khóa học chưa có trailer", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(
                    Intent(this, TrailerActivity::class.java)
                        .putExtra(TrailerActivity.EXTRA_URL, c.trailerUrl)
                        .putExtra(TrailerActivity.EXTRA_TITLE, c.title)
                )
            }
        }
        binding.btnEdit.setOnClickListener {
            startActivity(
                Intent(this, CourseFormActivity::class.java)
                    .putExtra(CourseFormActivity.EXTRA_COURSE_ID, courseId)
            )
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnRetry.setOnClickListener { load() }
    }

    // Sau khi sửa xong quay lại màn hình này -> tải lại dữ liệu mới
    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun load() {
        binding.progress.visibility = View.VISIBLE
        binding.layoutMessage.visibility = View.GONE
        lifecycleScope.launch {
            // GET /courses/{id} và GET /categories để lấy tên danh mục tương ứng categoryId
            val result = safeCall { Pair(ApiClient.api.getCourse(courseId), ApiClient.api.categoriesOrEmpty()) }
            binding.progress.visibility = View.GONE
            result.onSuccess { (c, cats) ->
                course = c
                show(c, cats)
            }.onFailure { e ->
                if (e is HttpException && e.code() == 404) {
                    Toast.makeText(this@CourseDetailActivity, "Khóa học không còn tồn tại", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.scrollContent.visibility = View.GONE
                    binding.tvMessage.text = e.toUserMessage()
                    binding.layoutMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun show(c: Course, cats: List<Category>) {
        binding.scrollContent.visibility = View.VISIBLE
        binding.ivThumb.loadUrl(c.thumbnail)
        binding.tvTitle.text = c.title
        binding.tvCategory.text = "Danh mục: ${cats.firstOrNull { it.id == c.categoryId }?.name ?: "Không rõ"}"
        binding.tvInstructor.text = "Giảng viên: ${c.instructor}"
        binding.tvPrice.text = "Học phí: ${c.price.toVnd()}"
        binding.tvDuration.text = "Thời lượng: ${c.duration.clean()} giờ"
        binding.tvCreatedAt.text = "Ngày tạo: ${c.createdAt}"
        binding.tvDescription.text = c.description
    }

    private fun confirmDelete() {
        val c = course ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa khóa học")
            .setMessage("Bạn có chắc muốn xóa \"${c.title}\"?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ -> deleteCourse() }
            .show()
    }

    private fun deleteCourse() {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = safeCall { ApiClient.api.deleteCourse(courseId) }
            binding.progress.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(this@CourseDetailActivity, "Đã xóa khóa học", Toast.LENGTH_SHORT).show()
                finish()   // MainActivity.onResume sẽ tải lại danh sách
            }.onFailure { e ->
                Toast.makeText(this@CourseDetailActivity, e.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }
}
