package com.example.grocerly.adapters

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.databinding.ParentCategoryLayoutBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.interfaces.SearchViewListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.ParentCategoryItem
import com.example.grocerly.model.WishItem
import com.example.grocerly.utils.ProductCategory

class ParentCategoryAdaptor(
    private val listener: ChildCategoryListener,
    private val searchViewListener: SearchViewListener
) : ListAdapter<ParentCategoryItem, ParentCategoryAdaptor.ParentCategoryViewHolder>(DiffCallback) {

    private var favoritesList: List<FavouriteItem> = emptyList()
    private var cartList: List<CartProduct> = emptyList()
    private var wishItems: List<WishItem> = emptyList()

    private val horizontalScrollStates = mutableMapOf<String, Parcelable?>()

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    inner class ParentCategoryViewHolder(val binding: ParentCategoryLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val childAdapter = ChildCategoryAdaptor(listener)

        init {
            binding.apply {
                rcViewChildItems.adapter = childAdapter
                rcViewChildItems.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
            }
        }

        fun bindCategoryItem(parentCategoryItem: ParentCategoryItem) {
            binding.apply {
                txtviewCategoryItems.text = parentCategoryItem.categoryName

                childAdapter.setProducts(parentCategoryItem.childCategoryItems)
                updateInnerAdapters()

                seeallbtn.setOnClickListener {
                    searchViewListener.onItemClicked(ProductCategory.fromString(parentCategoryItem.categoryName))
                }

                val savedState = horizontalScrollStates[parentCategoryItem.categoryName]
                if (savedState != null) {
                    rcViewChildItems.layoutManager?.onRestoreInstanceState(savedState)
                } else {
                    rcViewChildItems.layoutManager?.scrollToPosition(0)
                }
            }
        }

        fun updateInnerAdapters() {
            childAdapter.setFavouriteItems(favoritesList)
            childAdapter.setCartItems(cartList)
            childAdapter.setWishItems(wishItems)
        }

        fun resetHorizontalScroll() {
            binding.rcViewChildItems.scrollToPosition(0)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentCategoryViewHolder {
        return ParentCategoryViewHolder(
            ParentCategoryLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ParentCategoryViewHolder, position: Int) {
        holder.bindCategoryItem(getItem(position))
    }

    override fun onBindViewHolder(
        holder: ParentCategoryViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            for (payload in payloads) {
                when (payload) {
                    "UPDATE_CHILDREN" -> holder.updateInnerAdapters()
                    "RESET_SCROLL" -> holder.resetHorizontalScroll()
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewRecycled(holder: ParentCategoryViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            val categoryName = getItem(position).categoryName
            val layoutManager = holder.binding.rcViewChildItems.layoutManager
            horizontalScrollStates[categoryName] = layoutManager?.onSaveInstanceState()
        }
    }

    fun setFavouriteItems(favourites: List<FavouriteItem>) {
        if (this.favoritesList == favourites) return
        this.favoritesList = favourites
        notifyItemRangeChanged(0, itemCount, "UPDATE_CHILDREN")
    }

    fun setCartItems(cartItems: List<CartProduct>) {
        if (this.cartList == cartItems) return
        this.cartList = cartItems
        notifyItemRangeChanged(0, itemCount, "UPDATE_CHILDREN")
    }

    fun setWishlistItems(wishlistItems: List<WishItem>) {
        if (this.wishItems == wishlistItems) return
        this.wishItems = wishlistItems
        notifyItemRangeChanged(0, itemCount, "UPDATE_CHILDREN")
    }

    fun setParentCategoryItems(parentCategoryItems: List<ParentCategoryItem>, onCommit: (() -> Unit)? = null) {
        submitList(parentCategoryItems) {
            onCommit?.invoke()
        }
    }

    fun resetScrollState() {
        horizontalScrollStates.clear()
        notifyItemRangeChanged(0, itemCount, "RESET_SCROLL")
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ParentCategoryItem>() {
            override fun areItemsTheSame(oldItem: ParentCategoryItem, newItem: ParentCategoryItem): Boolean {
                return oldItem.categoryName == newItem.categoryName
            }

            override fun areContentsTheSame(oldItem: ParentCategoryItem, newItem: ParentCategoryItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}