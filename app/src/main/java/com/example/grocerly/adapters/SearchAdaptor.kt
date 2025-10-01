package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.AsyncListUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.grocerly.R
import com.example.grocerly.databinding.SearchItemLayoutBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product
import com.example.grocerly.viewmodel.CartViewModel

class SearchAdaptor(private val childCategoryListener: ChildCategoryListener): RecyclerView.Adapter<SearchAdaptor.SearchViewHolder>() {

    private var cartList: List<CartProduct> = emptyList()
    private var favouriteList: List<FavouriteItem> = emptyList()

    private val diffUtil = object : DiffUtil.ItemCallback<Product>(){
        override fun areItemsTheSame(
            oldItem: Product,
            newItem: Product
        ): Boolean {
          return  oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(
            oldItem: Product,
            newItem: Product
        ): Boolean {
         return  oldItem == newItem
        }

    }

    val products = AsyncListDiffer(this,diffUtil)

    inner class SearchViewHolder(private val binding: SearchItemLayoutBinding): RecyclerView.ViewHolder(binding.root){

        fun setProduct(product: Product){
            binding.apply {
                categoryItem = product
                binding.executePendingBindings()

                addtocartbtn.setOnClickListener {
                   childCategoryListener.addProductToCart(CartProduct(product,1))
                }

                addtofavouritesbtn.setOnClickListener {
                    childCategoryListener.addProductToFavourites(FavouriteItem(product = product))
                }

                if (cartList.any { it.product.productId == product.productId }){
                    binding.addtocartbtn.setImageDrawable(ContextCompat.getDrawable(binding.root.context,R.drawable.checkcircleadded))
                }else{
                    binding.addtocartbtn.setImageDrawable(ContextCompat.getDrawable(binding.root.context,R.drawable.carthome))
                }

                if (favouriteList.any { it.product.productId == product.productId }) {
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
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchViewHolder {
       return SearchViewHolder(SearchItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: SearchViewHolder,
        position: Int
    ) {
        val product = products.currentList[position]
        holder.setProduct(product)
    }

    override fun getItemCount(): Int {
       return products.currentList.size
    }

    fun setProducts(values: List<Product>?){
        products.submitList(values ?: emptyList())
    }

    fun setCartItems(items: List<CartProduct>){
        this.cartList = items
        notifyDataSetChanged()
    }

    fun setFavouriteItems(items: List<FavouriteItem>){
        this.favouriteList = items
        notifyDataSetChanged()
    }
}