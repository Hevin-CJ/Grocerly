package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.R
import com.example.grocerly.databinding.FragmentWishListBinding
import com.example.grocerly.databinding.WishlistItemLayoutBinding
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.WishItem
import com.example.grocerly.utils.WishListAction

class WishListAdaptor(
    private val onWishListAction: (WishListAction) -> Unit
): ListAdapter<WishItem,WishListAdaptor.wishListViewHolder>(DiffCallback) {


    private var cartItemIds: Set<String> = emptySet()

    fun setCartItems(cartItems: List<CartProduct>) {
        this.cartItemIds = cartItems.map { it.product.productId }.toSet()
        notifyDataSetChanged()
    }

    companion object DiffCallback : DiffUtil.ItemCallback<WishItem>() {
        override fun areItemsTheSame(oldItem: WishItem, newItem: WishItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WishItem, newItem: WishItem) = oldItem == newItem
    }



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): wishListViewHolder {
        return wishListViewHolder(WishlistItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: wishListViewHolder,
        position: Int
    ) {
      holder.bindWishItem(getItem(position))
    }


    inner class wishListViewHolder(private val binding: WishlistItemLayoutBinding): RecyclerView.ViewHolder(binding.root){

        fun bindWishItem(wishItem: WishItem){
            binding.categoryItem = wishItem.item
            binding.executePendingBindings()

            val isInCart = cartItemIds.contains(wishItem.item.productId)

            binding.addcartbtn.setOnClickListener {
                if (!isInCart) {
                    onWishListAction.invoke(WishListAction.AddItemToCart(wishItem))
                }
            }

            binding.deletebtnwishlist.setOnClickListener {
                onWishListAction.invoke(WishListAction.DeleteItemFromWishList(wishItem))
            }


            if(isInCart) {
                binding.addcartbtn.text = "Added"
                binding.addcartbtn.backgroundTintList = ContextCompat.getColorStateList(
                    binding.root.context,
                    R.color.lime_green
                )
            } else {
                binding.addcartbtn.text = "Add to Cart"
                binding.addcartbtn.backgroundTintList = ContextCompat.getColorStateList(
                    binding.root.context,
                    R.color.green
                )
            }
        }
    }
}