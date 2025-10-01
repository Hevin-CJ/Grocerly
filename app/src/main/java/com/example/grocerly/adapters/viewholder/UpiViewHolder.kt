package com.example.grocerly.adapters.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.databinding.UpiLayoutBinding
import com.example.grocerly.interfaces.PaymentListener

class UpiViewHolder(private val binding: UpiLayoutBinding,private val listener: PaymentListener): RecyclerView.ViewHolder(binding.root) {

    fun bindUpi(){
        binding.apply {
            verifybtn.setOnClickListener {
                listener.onUpiListener(edttxtupiid.text.toString())
            }
        }
    }
}