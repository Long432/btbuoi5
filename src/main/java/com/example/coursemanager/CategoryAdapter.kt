package com.example.coursemanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coursemanager.data.Category
import com.example.coursemanager.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var items = listOf<Category>()

    fun submit(list: List<Category>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        with(holder.b) {
            tvName.text = c.name
            tvDescription.text = c.description
            btnEdit.setOnClickListener { onEdit(c) }
            btnDelete.setOnClickListener { onDelete(c) }
        }
    }
}
