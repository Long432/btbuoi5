package com.example.coursemanager

import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coursemanager.data.ApiClient
import com.example.coursemanager.data.Category
import com.example.coursemanager.data.Course
import com.example.coursemanager.data.categoriesOrEmpty
import com.example.coursemanager.databinding.ActivityCourseFormBinding
import com.example.coursemanager.util.clean
import com.example.coursemanager.util.loadUrl
import com.example.coursemanager.util.safeCall
import com.example.coursemanager.util.today
import com.example.coursemanager.util.toUserMessage
import kotlinx.coroutines.launch

/**3*/
class CourseFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COURSE_ID = "course_id"   // không có = chế độ thêm mới
    }

    private lateinit var binding: ActivityCourseFormBinding
    private var editingId: String? = null
    private var editingCreatedAt: String? = null
    private var categories = emptyList<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editingId = intent.getStringExtra(EXTRA_COURSE_ID)
        supportActionBar?.title = if (editingId == null) "Thêm khóa học" else "Sửa khóa học"

        binding.btnPreview.setOnClickListener { previewImage() }
        binding.btnSave.setOnClickListener { save() }

        loadInitialData()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }

    /** Lấy danh mục từ API cho Spinner; nếu đang sửa thì lấy thêm khóa học để điền form. */
    private fun loadInitialData() {
        setLoading(true)
        lifecycleScope.launch {
            val catResult = safeCall { ApiClient.api.categoriesOrEmpty() }
            val cats = catResult.getOrElse { e ->
                setLoading(false)
                Toast.makeText(this@CourseFormActivity, e.toUserMessage(), Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            if (cats.isEmpty()) {
                setLoading(false)
                Toast.makeText(this@CourseFormActivity, "Hãy tạo ít nhất 1 danh mục trước", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            categories = cats
            binding.spCategory.adapter =
                ArrayAdapter(this@CourseFormActivity, android.R.layout.simple_spinner_item, cats.map { it.name }).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

            val id = editingId
            if (id != null) {
                val courseResult = safeCall { ApiClient.api.getCourse(id) }
                val course = courseResult.getOrElse { e ->
                    setLoading(false)
                    Toast.makeText(this@CourseFormActivity, e.toUserMessage(), Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }
                fillForm(course)
            }
            setLoading(false)
        }
    }

    private fun fillForm(c: Course) {
        editingCreatedAt = c.createdAt
        binding.etTitle.setText(c.title)
        binding.etDescription.setText(c.description)
        binding.etInstructor.setText(c.instructor)
        binding.etPrice.setText(c.price.clean())
        binding.etDuration.setText(c.duration.clean())
        binding.etThumbnail.setText(c.thumbnail)
        binding.etTrailer.setText(c.trailerUrl)

        val idx = categories.indexOfFirst { it.id == c.categoryId }
        if (idx >= 0) binding.spCategory.setSelection(idx)
        else Toast.makeText(this, "Danh mục cũ không còn tồn tại, hãy chọn lại", Toast.LENGTH_LONG).show()

        if (c.thumbnail.isNotBlank()) previewImage()
    }

    private fun previewImage() {
        val url = binding.etThumbnail.text.toString().trim()
        if (!URLUtil.isHttpsUrl(url)) {
            binding.tilThumbnail.error = "URL ảnh phải bắt đầu bằng https://"
            return
        }
        binding.tilThumbnail.error = null
        binding.ivPreview.visibility = View.VISIBLE
        binding.ivPreview.loadUrl(url)
    }

    /** Kiểm tra dữ liệu. Trả về Course hợp lệ hoặc null (đã hiện lỗi trên từng ô). */
    private fun validate(): Course? {
        var ok = true
        val til = listOf(
            binding.tilTitle, binding.tilDescription, binding.tilInstructor,
            binding.tilPrice, binding.tilDuration, binding.tilThumbnail, binding.tilTrailer
        )
        til.forEach { it.error = null }

        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val instructor = binding.etInstructor.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val durationText = binding.etDuration.text.toString().trim()
        val thumbnail = binding.etThumbnail.text.toString().trim()
        val trailer = binding.etTrailer.text.toString().trim()

        if (title.isEmpty()) { binding.tilTitle.error = "Bắt buộc nhập"; ok = false }
        if (desc.isEmpty()) { binding.tilDescription.error = "Bắt buộc nhập"; ok = false }
        if (instructor.isEmpty()) { binding.tilInstructor.error = "Bắt buộc nhập"; ok = false }

        val price = priceText.toDoubleOrNull()
        when {
            priceText.isEmpty() -> { binding.tilPrice.error = "Bắt buộc nhập"; ok = false }
            price == null -> { binding.tilPrice.error = "Học phí không hợp lệ"; ok = false }
            price < 0 -> { binding.tilPrice.error = "Học phí không được âm"; ok = false }
        }

        val duration = durationText.toDoubleOrNull()
        when {
            durationText.isEmpty() -> { binding.tilDuration.error = "Bắt buộc nhập"; ok = false }
            duration == null || duration <= 0 -> { binding.tilDuration.error = "Thời lượng phải lớn hơn 0"; ok = false }
        }

        if (!URLUtil.isHttpsUrl(thumbnail)) { binding.tilThumbnail.error = "Nhập URL ảnh bắt đầu bằng https://"; ok = false }
        if (!URLUtil.isHttpsUrl(trailer)) { binding.tilTrailer.error = "Nhập URL trailer (MP4) bắt đầu bằng https://"; ok = false }

        val category = categories.getOrNull(binding.spCategory.selectedItemPosition)
        if (category?.id == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show()
            ok = false
        }

        if (!ok) return null
        return Course(
            id = editingId,
            title = title,
            description = desc,
            categoryId = category!!.id!!,
            instructor = instructor,
            price = price!!,
            duration = duration!!,
            thumbnail = thumbnail,
            trailerUrl = trailer,
            createdAt = editingCreatedAt ?: today()
        )
    }

    private fun save() {
        val course = validate() ?: return
        setLoading(true)
        lifecycleScope.launch {
            val result = safeCall {
                // Kiểm tra lại categoryId còn tồn tại trên server trước khi lưu
                val latest = ApiClient.api.categoriesOrEmpty()
                check(latest.any { it.id == course.categoryId }) { "Danh mục đã chọn không còn tồn tại" }

                val id = editingId
                if (id == null) ApiClient.api.createCourse(course)       // POST /courses
                else ApiClient.api.updateCourse(id, course)              // PUT /courses/{id}
            }
            setLoading(false)
            result.onSuccess {
                Toast.makeText(
                    this@CourseFormActivity,
                    if (editingId == null) "Đã thêm khóa học" else "Đã cập nhật khóa học",
                    Toast.LENGTH_SHORT
                ).show()
                finish()   // màn hình trước sẽ tự tải lại danh sách
            }.onFailure { e ->
                Toast.makeText(this@CourseFormActivity, e.toUserMessage(), Toast.LENGTH_LONG).show()
            }
        }
    }
}
