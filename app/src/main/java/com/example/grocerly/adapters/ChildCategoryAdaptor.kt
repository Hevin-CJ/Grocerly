package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.grocerly.R
import com.example.grocerly.databinding.ChildcategoryLayoutBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product

class ChildCategoryAdaptor(private val listener: ChildCategoryListener) : RecyclerView.Adapter<ChildCategoryAdaptor.ChildCategoryViewHolder>() {

    private var favoritesList: List<FavouriteItem> = emptyList()
    private var cartItems: List<CartProduct> = emptyList()

    private val diffUtil = object : DiffUtil.ItemCallback<Product>(){
        override fun areItemsTheSame(
            oldItem: Product,
            newItem: Product
        ): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(
            oldItem: Product,
            newItem: Product
        ): Boolean {
            return oldItem == newItem
        }

    }

    private val asyncDiffer = AsyncListDiffer(this, diffUtil)



    inner class ChildCategoryViewHolder(private val binding: ChildcategoryLayoutBinding) :
        ViewHolder(binding.root) {


        fun setItem(childCategoryItem: Product) {
            binding.categoryItem = childCategoryItem
            binding.executePendingBindings()


            binding.addtocartbtn.setOnClickListener {
                listener.addProductToCart(CartProduct(product = childCategoryItem, 1))
            }
            binding.addtofavouritesbtn.setOnClickListener {
                listener.addProductToFavourites(
                    FavouriteItem(
                        childCategoryItem.productId,
                        product = childCategoryItem
                    )
                )
            }

            if (cartItems.any { it.product.productId == childCategoryItem.productId }){
                binding.addtocartbtn.setImageDrawable(ContextCompat.getDrawable(binding.root.context,R.drawable.checkcircleadded))
            }else{
                binding.addtocartbtn.setImageDrawable(ContextCompat.getDrawable(binding.root.context,R.drawable.carthome))
            }

            if (favoritesList.any { it.product.productId == childCategoryItem.productId }) {
                binding.addtofavouritesbtn.setColorFilter(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.red
                    )
                )
            } else {
                binding.addtofavouritesbtn.clearColorFilter()
            }

        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChildCategoryViewHolder {

        return ChildCategoryViewHolder(
            ChildcategoryLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ChildCategoryViewHolder,
        position: Int
    ) {
        val product = asyncDiffer.currentList[position]
        holder.setItem(product)


    }

    override fun getItemCount(): Int {
       return asyncDiffer.currentList.size
    }

    fun setFavouriteItems(favourites: List<FavouriteItem>) {
        favoritesList = favourites
        notifyDataSetChanged()
    }

    fun setCartItems(items: List<CartProduct>) {
        cartItems = items
        notifyDataSetChanged()
    }

    fun setProducts(products: List<Product>){
        asyncDiffer.submitList(products)
    }

}