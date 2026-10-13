package com.example.coursemanager

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coursemanager.data.ApiClient
import com.example.coursemanager.data.Category
import com.example.coursemanager.data.Course
import com.example.coursemanager.data.categoriesOrEmpty
import com.example.coursemanager.data.coursesOrEmpty
import com.example.coursemanager.databinding.ActivityMainBinding
import com.example.coursemanager.util.safeCall
import com.example.coursemanager.util.toUserMessage
import kotlinx.coroutines.launch

/**1*/
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CourseAdapter

    private var allCourses = emptyList<Course>()
    private var categories = emptyList<Category>()
    private var selectedCategoryId: String? = null   // null = tất cả danh mục
    private var loaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Khóa học"

        adapter = CourseAdapter { course ->
            startActivity(
                Intent(this, CourseDetailActivity::class.java)
                    .putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.id)
            )
        }
        binding.rvCourses.layoutManager = LinearLayoutManager(this)
        binding.rvCourses.adapter = adapter

        binding.etSearch.doAfterTextChanged { applyFilter() }
        binding.spCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = if (position == 0) null else categories.getOrNull(position - 1)?.id
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, CourseFormActivity::class.java))
        }
        binding.btnRetry.setOnClickListener { loadData() }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        binding.progress.visibility = View.VISIBLE
        binding.layoutMessage.visibility = View.GONE
        lifecycleScope.launch {
            val result = safeCall {
                Pair(ApiClient.api.categoriesOrEmpty(), ApiClient.api.coursesOrEmpty())
            }
            binding.progress.visibility = View.GONE
            result.onSuccess { (cats, courses) ->
                categories = cats
                allCourses = courses
                loaded = true
                setupCategorySpinner()
                applyFilter()
            }.onFailure { e ->
                loaded = false
                adapter.submit(emptyList(), emptyMap())
                showMessage(e.toUserMessage(), showRetry = true)
            }
        }
    }

    private fun setupCategorySpinner() {
        val names = listOf("Tất cả danh mục") + categories.map { it.name }
        binding.spCategoryFilter.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, names).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        val idx = categories.indexOfFirst { it.id == selectedCategoryId }
        if (idx >= 0) binding.spCategoryFilter.setSelection(idx + 1) else selectedCategoryId = null
    }

    private fun applyFilter() {
        if (!loaded) return
        val keyword = binding.etSearch.text.toString().trim().lowercase()
        val filtered = allCourses.filter { c ->
            (selectedCategoryId == null || c.categoryId == selectedCategoryId) &&
                c.title.lowercase().contains(keyword)
        }
        adapter.submit(filtered, categories.associate { it.id.orEmpty() to it.name })

        when {
            allCourses.isEmpty() -> showMessage("Chưa có khóa học nào.\nNhấn nút + để thêm.", false)
            filtered.isEmpty() -> showMessage("Không tìm thấy khóa học phù hợp.", false)
            else -> binding.layoutMessage.visibility = View.GONE
        }
    }

    private fun showMessage(text: String, showRetry: Boolean) {
        binding.tvMessage.text = text
        binding.btnRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
        binding.layoutMessage.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == R.id.action_categories) {
            startActivity(Intent(this, CategoryActivity::class.java))
            true
        } else super.onOptionsItemSelected(item)
}
