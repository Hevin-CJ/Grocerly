package com.example.grocerly.adapters.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.databinding.CashondeliveryLayoutBinding
import com.example.grocerly.interfaces.PaymentListener

class CashOnDeliveryViewHolder(private val binding:CashondeliveryLayoutBinding,private val listener: PaymentListener):RecyclerView.ViewHolder(binding.root) {


    fun bindCod(){
        binding.apply {
            radiobtncod.setOnCheckedChangeListener{buttonView,isChecked ->
                if (isChecked){
                    listener.onCodListener(isChecked)
                }
            }
        }
    }
}