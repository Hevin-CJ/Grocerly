package com.example.grocerly.adapters.viewholder

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.example.grocerly.R
import com.example.grocerly.databinding.PaymentHeaderLayoutBinding
import com.example.grocerly.utils.PaymentMethodItem

class HeaderViewHolder(private val binding: PaymentHeaderLayoutBinding, private val onExpandClick:(Int, PaymentMethodItem.Header) -> Unit): RecyclerView.ViewHolder(binding.root) {

    fun bindHeader(header: PaymentMethodItem.Header,position:Int){
        binding.apply {
            txtviewpaymenttype.text = header.title
            if (header.icon.isEmpty()) {
                imageView15.setImageResource(R.drawable.placeholderimage)
                return
            }

            imageView15.load(header.icon) {
                error(R.drawable.noimage)
                crossfade(200)
            }

            creditcardlayout.setOnClickListener {
                onExpandClick(position,header)
            }
        }
    }
}