package com.example.grocerly.adapters.viewholder

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.example.grocerly.databinding.PaymentHeaderLayoutBinding
import com.example.grocerly.utils.PaymentMethodItem

class HeaderViewHolder(private val binding: PaymentHeaderLayoutBinding, private val onExpandClick:(Int, PaymentMethodItem.Header) -> Unit): RecyclerView.ViewHolder(binding.root) {

    fun bindHeader(header: PaymentMethodItem.Header,position:Int){
        binding.apply {
            txtviewpaymenttype.text = header.title

            Glide.with(binding.root.context)
                .load(header.icon)
                .priority(Priority.IMMEDIATE)
                .into(binding.imageView15)

            Log.d("headericon",header.icon)

            creditcardlayout.setOnClickListener {
                onExpandClick(position,header)
            }
        }
    }
}