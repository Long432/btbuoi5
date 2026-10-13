package com.example.coursemanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coursemanager.data.Course
import com.example.coursemanager.databinding.ItemCourseBinding
import com.example.coursemanager.util.loadUrl
import com.example.coursemanager.util.toVnd

class CourseAdapter(private val onClick: (Course) -> Unit) :
    RecyclerView.Adapter<CourseAdapter.VH>() {

    private var items = listOf<Course>()
    private var categoryNames = mapOf<String, String>()   // categoryId -> tên danh mục (lấy từ API)

    fun submit(list: List<Course>, names: Map<String, String>) {
        items = list
        categoryNames = names
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemCourseBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        with(holder.b) {
            ivThumb.loadUrl(c.thumbnail)
            tvTitle.text = c.title
            tvInstructor.text = "Giảng viên: ${c.instructor}"
            tvCategory.text = "Danh mục: ${categoryNames[c.categoryId] ?: "Không rõ"}"
            tvPrice.text = c.price.toVnd()
            root.setOnClickListener { onClick(c) }
        }
    }
}
