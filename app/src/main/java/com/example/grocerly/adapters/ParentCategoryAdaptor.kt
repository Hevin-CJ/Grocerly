package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.grocerly.databinding.ParentCategoryLayoutBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.interfaces.SearchViewListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.ParentCategoryItem
import com.example.grocerly.model.Product
import com.example.grocerly.utils.ProductCategory

class ParentCategoryAdaptor(private val listener: ChildCategoryListener,private val searchViewListener: SearchViewListener): RecyclerView.Adapter<ParentCategoryAdaptor.ParentCategoryViewHolder>() {

    private var favoritesList: List<FavouriteItem> = emptyList()
    private var cartList: List<CartProduct> = emptyList()


    private val diffUtil = object : DiffUtil.ItemCallback<ParentCategoryItem>(){
        override fun areItemsTheSame(
            oldItem: ParentCategoryItem,
            newItem: ParentCategoryItem
        ): Boolean {
            return oldItem.categoryName == newItem.categoryName
        }

        override fun areContentsTheSame(
            oldItem: ParentCategoryItem,
            newItem: ParentCategoryItem
        ): Boolean {
            return oldItem == newItem
        }

    }

    private val asyncDiffer = AsyncListDiffer(this, diffUtil)

   inner  class ParentCategoryViewHolder( private val binding: ParentCategoryLayoutBinding):ViewHolder(binding.root){

       val childAdapter = ChildCategoryAdaptor(listener)

       init {
          binding.apply {
              rcViewChildItems.adapter = childAdapter
              rcViewChildItems.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)

          }
       }

         fun bindCategoryItem(parentCategoryItem: ParentCategoryItem){
             binding.apply {

                 txtviewCategoryItems.text = parentCategoryItem.categoryName

                 childAdapter.setProducts(parentCategoryItem.childCategoryItems)
                 childAdapter.setFavouriteItems(favoritesList)
                 childAdapter.setCartItems(cartList)

                 seeallbtn.setOnClickListener {
                     searchViewListener.onItemClicked(ProductCategory.fromString(parentCategoryItem.categoryName))
                 }
             }
         }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ParentCategoryViewHolder {
       return ParentCategoryViewHolder(ParentCategoryLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: ParentCategoryViewHolder,
        position: Int
    ) {

        val currentCategory = asyncDiffer.currentList[position]
        holder.bindCategoryItem(currentCategory)
    }

    override fun getItemCount(): Int {
        return asyncDiffer.currentList.size
    }


    fun setFavouriteItems(favourites: List<FavouriteItem>){
        this.favoritesList = favourites
        notifyDataSetChanged()
    }

    fun setCartItems(cartItems: List<CartProduct>){
        this.cartList = cartItems
        notifyDataSetChanged()
    }


    fun setParentCategoryItems(parentCategoryItems: List<ParentCategoryItem>){
        asyncDiffer.submitList(parentCategoryItems)
    }

}