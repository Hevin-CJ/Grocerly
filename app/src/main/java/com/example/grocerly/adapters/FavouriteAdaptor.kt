package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.grocerly.databinding.FavouriteItemLayoutBinding
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.viewmodel.CartViewModel
import com.example.grocerly.viewmodel.FavouriteViewModel

class FavouriteAdaptor(private val favouriteViewModel: FavouriteViewModel,private val cartViewModel: CartViewModel): RecyclerView.Adapter<FavouriteAdaptor.FavouriteViewHolder>() {

    private val diffUtil = object : DiffUtil.ItemCallback<FavouriteItem>(){

        override fun areItemsTheSame(
            oldItem: FavouriteItem,
            newItem: FavouriteItem
        ): Boolean {
            return oldItem.favouriteId == newItem.favouriteId
        }

        override fun areContentsTheSame(
            oldItem: FavouriteItem,
            newItem: FavouriteItem
        ): Boolean {
            return oldItem == newItem
        }

    }

    private val asyncDiffer = AsyncListDiffer(this,diffUtil)

    inner class FavouriteViewHolder(private val binding: FavouriteItemLayoutBinding):ViewHolder(binding.root){

        fun bindFavourite(favouriteItem: FavouriteItem){

            binding.apply {
                txtviewproductname.text = favouriteItem.product.itemName
                txtviewPrice.text = favouriteItem.product.itemPrice.toString()
                Glide.with(binding.root.context).load(favouriteItem.product.image).into(binding.imgviewcartitem)

                deletebtn.setOnClickListener {
                    favouriteViewModel.deleteFavouriteFromFirebase(favouriteItem)
                }

                addfavouritetocartbtn.setOnClickListener {
                    cartViewModel.addProductIntoCartFirebase(CartProduct(favouriteItem.product,1))
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavouriteViewHolder {
        return FavouriteViewHolder(FavouriteItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: FavouriteViewHolder,
        position: Int
    ) {
       val favourite = asyncDiffer.currentList[position]
        holder.bindFavourite(favourite)
    }

    override fun getItemCount(): Int {
        return asyncDiffer.currentList.size
    }


    fun saveFavourites(favourites: List<FavouriteItem>){
        asyncDiffer.submitList(favourites)
    }

}