package com.example.coursemanager

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coursemanager.data.ApiClient
import com.example.coursemanager.data.Category
import com.example.coursemanager.data.categoriesOrEmpty
import com.example.coursemanager.data.coursesOrEmpty
import com.example.coursemanager.databinding.ActivityCategoryBinding
import com.example.coursemanager.databinding.DialogCategoryBinding
import com.example.coursemanager.util.safeCall
import com.example.coursemanager.util.toUserMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 4*/
class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Quản lý danh mục"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = CategoryAdapter(onEdit = { showEditDialog(it) }, onDelete = { tryDelete(it) })
        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter

        binding.fabAdd.setOnClickListener { showEditDialog(null) }
        binding.btnRetry.setOnClickListener { load() }
        load()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun load() {
        binding.progress.visibility = View.VISIBLE
        binding.layoutMessage.visibility = View.GONE
        lifecycleScope.launch {
            val result = safeCall { ApiClient.api.categoriesOrEmpty() }
            binding.progress.visibility = View.GONE
            result.onSuccess { list ->
                adapter.submit(list)
                if (list.isEmpty()) showMessage("Chưa có danh mục nào.\nNhấn nút + để thêm.", false)
            }.onFailure { e ->
                adapter.submit(emptyList())
                showMessage(e.toUserMessage(), true)
            }
        }
    }

    private fun showMessage(text: String, retry: Boolean) {
        binding.tvMessage.text = text
        binding.btnRetry.visibility = if (retry) View.VISIBLE else View.GONE
        binding.layoutMessage.visibility = View.VISIBLE
    }

    /** category == null -> thêm mới; ngược lại -> sửa */
    private fun showEditDialog(category: Category?) {
        val d = DialogCategoryBinding.inflate(layoutInflater)
        category?.let {
            d.etName.setText(it.name)
            d.etDescription.setText(it.description)
        }
        val dialog: AlertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (category == null) "Thêm danh mục" else "Sửa danh mục")
            .setView(d.root)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu", null)   // gán listener bên dưới để không tự đóng khi dữ liệu sai
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = d.etName.text.toString().trim()
            if (name.isEmpty()) {
                d.tilName.error = "Tên danh mục không được để trống"
                return@setOnClickListener
            }
            dialog.dismiss()
            saveCategory(category, Category(name = name, description = d.etDescription.text.toString().trim()))
        }
    }

    private fun saveCategory(old: Category?, data: Category) {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = safeCall {
                val id = old?.id
                if (id == null) ApiClient.api.createCategory(data)          // POST /categories
                else ApiClient.api.updateCategory(id, data)                 // PUT /categories/{id}
            }
            binding.progress.visibility = View.GONE
            result.onSuccess { load() }.onFailure { toast(it.toUserMessage()) }
        }
    }

    /** Ràng buộc: không cho xóa danh mục đang có khóa học (kiểm tra phía app). */
    private fun tryDelete(category: Category) {
        val id = category.id ?: return
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = safeCall { ApiClient.api.coursesOrEmpty() }   // lấy dữ liệu mới nhất từ server
            binding.progress.visibility = View.GONE
            result.onSuccess { courses ->
                val used = courses.count { it.categoryId == id }
                if (used > 0) {
                    MaterialAlertDialogBuilder(this@CategoryActivity)
                        .setTitle("Không thể xóa")
                        .setMessage("Danh mục \"${category.name}\" đang có $used khóa học. Hãy xóa hoặc chuyển các khóa học sang danh mục khác trước.")
                        .setPositiveButton("Đã hiểu", null)
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this@CategoryActivity)
                        .setTitle("Xóa danh mục")
                        .setMessage("Bạn có chắc muốn xóa \"${category.name}\"?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa") { _, _ -> deleteCategory(id) }
                        .show()
                }
            }.onFailure { toast(it.toUserMessage()) }
        }
    }

    private fun deleteCategory(id: String) {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = safeCall { ApiClient.api.deleteCategory(id) }   // DELETE /categories/{id}
            binding.progress.visibility = View.GONE
            result.onSuccess { load() }.onFailure { toast(it.toUserMessage()) }
        }
    }
}
