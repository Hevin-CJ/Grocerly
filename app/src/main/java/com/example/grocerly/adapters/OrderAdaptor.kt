package com.example.grocerly.adapters

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grocerly.R
import com.example.grocerly.databinding.OrderitemsRclayoutBinding
import com.example.grocerly.fragments.Orders
import com.example.grocerly.interfaces.OrderActionListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order
import com.example.grocerly.utils.CancellationStatus
import com.example.grocerly.utils.OrderStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OrderAdaptor(private val listener: OrderActionListener): RecyclerView.Adapter<OrderAdaptor.OrderViewHolder>() {

    private var orderList: List<Pair<Order, CartProduct>> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {
        return OrderViewHolder(OrderitemsRclayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: OrderViewHolder,
        position: Int
    ) {

        val (order, cartProduct) = orderList[position]
        holder.bindOrder(order,cartProduct)

    }

    override fun getItemCount(): Int {
        return orderList.size
    }


    inner class OrderViewHolder(private val binding: OrderitemsRclayoutBinding): RecyclerView.ViewHolder(binding.root){

        fun bindOrder(order:Order,cartProduct: CartProduct){


            binding.apply {
                Glide.with(binding.root.context)
                    .load(cartProduct.product.image)
                    .into(imageView17)

                txtviewItemName.text = cartProduct.product.itemName

                setOrderStatus(cartProduct)

                orderlayout.setOnClickListener {
                    listener.onItemTouchListener(cartProduct,order)
                }
            }
        }

        private fun setOrderStatus(cartProduct: CartProduct) {

            if (cartProduct.cancellationInfo.cancellationStatus == CancellationStatus.Cancelled){
                binding.apply {
                    val deliveryText =  SpannableString("Cancelled on ${convertDate(cartProduct.cancellationInfo.cancelledAt)}").apply {

                        setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(binding.root.context,R.color.red)
                            ), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

                        )
                    }

                    txtviewdeliverydata.text = deliveryText
                }
            }else{
                when(cartProduct.orderStatus){
                    OrderStatus.PENDING -> {
                        binding.apply {
                            val deliveryText =  SpannableString("Delivery by ${cartProduct.deliveryDate}").apply {

                                setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(binding.root.context,R.color.green)
                                    ), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

                                )
                            }

                            txtviewdeliverydata.text = deliveryText
                        }
                    }
                    OrderStatus.ACCEPTED -> {
                        binding.apply {
                            val deliveryText =  SpannableString("Order Accepted , Expected Delivery by \n${cartProduct.deliveryDate}").apply {

                                setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(binding.root.context,R.color.green)
                                    ), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

                                )
                            }

                            txtviewdeliverydata.text = deliveryText
                        }
                    }
                    OrderStatus.READY -> {
                        binding.apply {
                            val deliveryText =  SpannableString("Order Ready , Expected Delivery by \n${cartProduct.deliveryDate}").apply {

                                setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(binding.root.context,R.color.green)
                                    ), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

                                )
                            }

                            txtviewdeliverydata.text = deliveryText
                        }
                    }
                    OrderStatus.SHIPPED -> {
                        binding.apply {
                            val deliveryText =  SpannableString("Order Shipped , Expected Delivery by \n${cartProduct.deliveryDate}").apply {

                                setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(binding.root.context,R.color.green)
                                    ), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

                                )
                            }

                            txtviewdeliverydata.text = deliveryText
                        }
                    }
                    OrderStatus.OUTFORDELIVERY -> {
                        binding.apply {
                            val deliveryText =  SpannableString("Out for Delivery").apply {

                                setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(binding.root.context,R.color.light_green)
                                    ), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

                                )

                                setSpan(StyleSpan(Typeface.BOLD),
                                    0,
                                    length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }

                            txtviewdeliverydata.text = deliveryText
                        }
                    }
                    OrderStatus.DELIVERED ->{
                        binding.apply {
                            val deliveryText =  SpannableString("Delivered on ${convertDate(cartProduct.deliveredDate)}").apply {

                                setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(binding.root.context,R.color.green)
                                    ), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

                                )

                                setSpan(StyleSpan(Typeface.BOLD),
                                    0,
                                    length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }

                            txtviewdeliverydata.text = deliveryText
                        }
                    }
                }
            }


        }

    }

    fun convertDate(date: Long, format: String = "dd MMMM, E"): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(date))
    }

    fun setOrders(orders: List<Order>){
        this.orderList = orders.flatMap { order ->
            order.items.map { cartProduct -> order to cartProduct }
        }
        notifyDataSetChanged()
    }
}