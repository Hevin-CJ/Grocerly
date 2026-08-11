package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.R
import com.example.grocerly.databinding.MenuAccountLayoutBinding
import com.example.grocerly.databinding.MenuitemlayoutBinding
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product
import com.example.grocerly.ui.uistate.MenuItemUiState
import com.example.grocerly.utils.MenuAction

class MenuItemAdaptor(private val onMenuActionClicked:(MenuAction) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<MenuItemUiState> = emptyList()
    private var favouriteList: List<FavouriteItem> = emptyList()
    private var cartList: List<CartProduct> = emptyList()



    companion object{
        const val TYPE_PRODUCT= 0
        const val TYPE_ACCOUNT=1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]){
            is MenuItemUiState.ProductItem -> TYPE_PRODUCT
            is MenuItemUiState.AccountItem -> TYPE_ACCOUNT
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PRODUCT -> {
                MenuItemViewHolder(MenuitemlayoutBinding.inflate(inflater, parent, false))
            }
            TYPE_ACCOUNT -> {
                AccountViewHolder(MenuAccountLayoutBinding.inflate(inflater, parent, false))
            }
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }



    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
       when(val item = items[position]){
           is MenuItemUiState.ProductItem -> (holder as MenuItemViewHolder).bindProduct(item.product)
           is MenuItemUiState.AccountItem -> (holder as AccountViewHolder).bind()
       }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class MenuItemViewHolder(private val binding: MenuitemlayoutBinding): RecyclerView.ViewHolder(binding.root){

        fun bindProduct(product: Product){
            binding.categoryItem = product

            if (cartList.any {it.product.productId == product.productId }){
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

            binding.addtocartbtn.setOnClickListener {
                onMenuActionClicked(MenuAction.addToCart(CartProduct(product,1)))
            }

            binding.addtofavouritesbtn.setOnClickListener {
                onMenuActionClicked(MenuAction.addToFavourites(FavouriteItem(product = product)))
            }

            binding.executePendingBindings()
        }
    }

    inner class AccountViewHolder(private val binding: MenuAccountLayoutBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(){

            binding.cardorders.setOnClickListener {
                onMenuActionClicked(MenuAction.ORDERS)
            }
            binding.cardsignout.setOnClickListener {
                onMenuActionClicked(MenuAction.SIGN_OUT)
            }
            binding.cardassistant.setOnClickListener {
                onMenuActionClicked(MenuAction.ASSISTANT)
            }


        }
    }

    fun setFavouritesList(favourites: List<FavouriteItem>){
        this.favouriteList = favourites
        notifyDataSetChanged()
    }

    fun setCartList(cartList: List<CartProduct>){
        this.cartList = cartList
        notifyDataSetChanged()
    }

    fun setProductList(products: List<Product>){
        this.items = products.map { MenuItemUiState.ProductItem(it) }
        notifyDataSetChanged()
    }

    fun showAccountView(){
        this.items = listOf(MenuItemUiState.AccountItem)
        notifyDataSetChanged()
    }
}