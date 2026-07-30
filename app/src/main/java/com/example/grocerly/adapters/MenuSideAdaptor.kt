package com.example.grocerly.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.example.grocerly.R
import com.example.grocerly.databinding.ItemSidebarLayoutBinding
import com.example.grocerly.model.Category

class MenuSideAdaptor(private val onCategorySelected:(Category) -> Unit): RecyclerView.Adapter<MenuSideAdaptor.menuViewHolder>() {



    private var categories: List<Category> = emptyList()
    private var selectedPosition = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): menuViewHolder {
        return menuViewHolder(ItemSidebarLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: menuViewHolder,
        position: Int
    ) {
        holder.bindSideBar(categories[position],position)
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class menuViewHolder(private val binding: ItemSidebarLayoutBinding): RecyclerView.ViewHolder(binding.root) {

        fun bindSideBar(category: Category,position: Int){


            binding.apply {
                ivIcon.load(category.imageUrl){
                    crossfade(true)
                    crossfade(500)
                    placeholder(R.drawable.placeholderimage)
                    error(R.drawable.noimage)
                }
                tvName.text = category.category.displayName

                if (selectedPosition == position){
                    rootLayout.setBackgroundColor(Color.WHITE)
                    binding.selectionIndicator.visibility = View.VISIBLE
                    binding.tvName.setTypeface(null, Typeface.BOLD)
                }else{
                    rootLayout.setBackgroundColor(ContextCompat.getColor(root.context,R.color.light_white))
                    binding.selectionIndicator.visibility = View.INVISIBLE
                    binding.tvName.setTypeface(null, Typeface.NORMAL)
                }

                rootLayout.setOnClickListener {
                    val previousPosition = selectedPosition
                    selectedPosition = bindingAdapterPosition
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)

                    onCategorySelected(category)

                }
            }
        }
    }

    fun updateData(newCategories: List<Category>) {
        this.categories = newCategories
        notifyDataSetChanged()
    }
}