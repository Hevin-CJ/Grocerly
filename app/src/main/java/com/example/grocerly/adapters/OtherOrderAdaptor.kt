package com.example.grocerly.adapters

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grocerly.databinding.OtherorderItemLayoutBinding
import com.example.grocerly.model.CartProduct

class OtherOrderAdaptor(): RecyclerView.Adapter<OtherOrderAdaptor.OtherOrderViewHolder>() {

    private var cartProducts: List<CartProduct> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OtherOrderViewHolder {
        return OtherOrderViewHolder(OtherorderItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: OtherOrderViewHolder,
        position: Int
    ) {
        val currentItem = cartProducts[position]
        holder.bindCartProduct(currentItem)

    }

    override fun getItemCount(): Int {
        return cartProducts.size
    }

    inner class OtherOrderViewHolder(private val binding: OtherorderItemLayoutBinding): RecyclerView.ViewHolder(binding.root){

        fun bindCartProduct(cartProduct: CartProduct){
            binding.apply {
                txtviewproductname.text = cartProduct.product.itemName
                txtviewPrice.text = cartProduct.product.itemPrice?.toString()

                val deliveryText = SpannableStringBuilder().apply {
                    append("Delivery by ")

                    val start = length
                    append(cartProduct.deliveryDate)
                    val end = length

                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )


                }
                txtviewexpecteddelivery.text = deliveryText



                Glide.with(binding.root.context)
                    .load(cartProduct.product.image)
                    .into(imgviewcartitem)

            }
        }
    }

    fun setCartProducts(product: List<CartProduct>){
        this.cartProducts = product
        notifyDataSetChanged()
    }
}