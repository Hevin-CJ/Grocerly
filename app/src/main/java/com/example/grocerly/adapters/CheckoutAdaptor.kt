package com.example.grocerly.adapters


import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grocerly.CheckoutListener
import com.example.grocerly.databinding.CheckoutItemsLayoutBinding
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order
import com.example.grocerly.utils.QuantityType
import okhttp3.internal.wait

class CheckoutAdaptor(private val listener: CheckoutListener): RecyclerView.Adapter<CheckoutAdaptor.checkoutViewHolder>() {

  private var checkoutList: List<CartProduct> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): checkoutViewHolder {
        return checkoutViewHolder(CheckoutItemsLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: checkoutViewHolder,
        position: Int
    ) {
        val currentItem = checkoutList[position]
        holder.setItemData(currentItem)
    }

    override fun getItemCount(): Int {
       return checkoutList.size
    }

    inner class checkoutViewHolder(private val binding: CheckoutItemsLayoutBinding): RecyclerView.ViewHolder(binding.root){


        fun setItemData(item: CartProduct){
            binding.apply {
                txtviewproductname.text = item.product.itemName
                txtviewofferPrice.text = item.product.itemOriginalPrice.toString()
                txtvieworgPrice.apply {
                    text = item.product.itemPrice.toString()
                    this.paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }
                txtviewquantity.text = item.quantity.toString()
                txtviewquantitypetxt.text = convertQuantityIntoString(item.product.quantityType)
                val deliveryText = SpannableStringBuilder().apply {
                    append("Delivery by ")

                    val start = length
                    append(item.deliveryDate)
                    val end = length

                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )


                }
                txtviewexpecteddelivery.text = deliveryText

                loadItemImage(item)
                setQuantityClickListener(item)
            }
        }

        private fun setQuantityClickListener(product: CartProduct) {
            binding.apply {
                txviewaddequantity.setOnClickListener {
                    val newQuantity = product.quantity + 1
                    val newProduct = product.copy(quantity = newQuantity)
                    listener.onQuantityChanged(newProduct)

                }

                txtviewreducequantity.setOnClickListener {
                    val newQuantity = product.quantity - 1
                    val newProduct = product.copy(quantity = newQuantity)
                    if (newQuantity>=1){
                        listener.onQuantityChanged(newProduct)
                    }else{
                        txtviewquantity.text = 0.toString()
                        listener.onItemDeleted(newProduct)
                    }
                }
            }
        }

        private fun loadItemImage(product: CartProduct) {
            Glide.with(binding.root.context)
                .load(product.product.image)
                .into(binding.imgviewcartitem)
                .onStart()

        }

    }

    private fun convertQuantityIntoString(quantityType: QuantityType): String{
        return when(quantityType){
            QuantityType.selectQuantity -> "Per Quantity"
            QuantityType.perKilogram -> "Per Kilogram"
            QuantityType.perLiter -> "Per Liter"
            QuantityType.perPiece -> "Per Piece"
            QuantityType.perPacket -> "Per Packet"
        }
    }


    fun setCartItems(order: List<CartProduct>){
       checkoutList = order
        notifyDataSetChanged()
    }
}