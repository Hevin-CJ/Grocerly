package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.grocerly.databinding.CategoriesItemsLayoutBinding
import com.example.grocerly.interfaces.SearchViewListener
import com.example.grocerly.model.Category

class CategoryAdaptor(private val searchViewListener: SearchViewListener): RecyclerView.Adapter<CategoryAdaptor.CategoryViewHolder>() {

    private var categoryList: List<Category> = emptyList()

    inner class CategoryViewHolder(private val binding: CategoriesItemsLayoutBinding):ViewHolder(binding.root){

        fun bindItem(category: Category){
            binding.categoryItem = category
            binding.executePendingBindings()

            binding.root.setOnClickListener {
                searchViewListener.onItemClicked(category.category)
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {
       return CategoryViewHolder(CategoriesItemsLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {


       val currentItem = categoryList[position]
        holder.bindItem(currentItem)
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    fun setItem(category:List<Category>){
        this.categoryList = category
        notifyDataSetChanged()
    }

}