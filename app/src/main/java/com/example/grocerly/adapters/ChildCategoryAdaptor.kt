package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.R
import com.example.grocerly.databinding.ChildcategoryLayoutBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product
import com.example.grocerly.model.WishItem

class ChildCategoryAdaptor(
    private val listener: ChildCategoryListener
) : ListAdapter<Product, ChildCategoryAdaptor.ChildCategoryViewHolder>(DiffCallback) {

    private var favoritesList: List<FavouriteItem> = emptyList()
    private var cartItems: List<CartProduct> = emptyList()
    private var wishItems: List<WishItem> = emptyList()

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    inner class ChildCategoryViewHolder(private val binding: ChildcategoryLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                addtocartbtn.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        listener.addProductToCart(CartProduct(product = getItem(position), 1))
                    }
                }
                addtofavouritesbtn.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        listener.addProductToFavourites(FavouriteItem(item.productId, product = item))
                    }
                }
                addtowishlistButton.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        listener.addProductToWishList(WishItem(id = item.productId, item = item))
                    }
                }
            }
        }

        fun bindFullData(childCategoryItem: Product) {
            binding.categoryItem = childCategoryItem
            binding.executePendingBindings()
            updateInteractiveStates(childCategoryItem)
        }


        fun updateInteractiveStates(childCategoryItem: Product) {
            val context = binding.root.context

            if (cartItems.any { it.product.productId == childCategoryItem.productId }) {
                binding.addtocartbtn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.checkcircleadded))
            } else {
                binding.addtocartbtn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.carthome))
            }

            if (favoritesList.any { it.product.productId == childCategoryItem.productId }) {
                binding.addtofavouritesbtn.setColorFilter(ContextCompat.getColor(context, R.color.red))
            } else {
                binding.addtofavouritesbtn.clearColorFilter()
            }

            if (wishItems.any { it.item.productId == childCategoryItem.productId }) {
                binding.addtowishlistButton.setImageResource(R.drawable.wishlist_done)
            } else {
                binding.addtowishlistButton.setImageResource(R.drawable.wishlist)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildCategoryViewHolder {
        return ChildCategoryViewHolder(
            ChildcategoryLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ChildCategoryViewHolder, position: Int) {
        holder.bindFullData(getItem(position))
    }


    override fun onBindViewHolder(holder: ChildCategoryViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            holder.updateInteractiveStates(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun setFavouriteItems(favourites: List<FavouriteItem>) {
        if (this.favoritesList == favourites) return
        this.favoritesList = favourites
        notifyItemRangeChanged(0, itemCount, "UPDATE_ICONS")
    }

    fun setCartItems(items: List<CartProduct>) {
        if (this.cartItems == items) return
        this.cartItems = items
        notifyItemRangeChanged(0, itemCount, "UPDATE_ICONS")
    }

    fun setWishItems(newWishItems: List<WishItem>) {
        if (this.wishItems == newWishItems) return
        this.wishItems = newWishItems
        notifyItemRangeChanged(0, itemCount, "UPDATE_ICONS")
    }

    fun setProducts(products: List<Product>) {
        submitList(products)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem.productId == newItem.productId
            }

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem == newItem
            }
        }
    }
}