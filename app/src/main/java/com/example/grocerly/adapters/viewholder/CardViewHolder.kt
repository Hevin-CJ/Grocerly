package com.example.grocerly.adapters.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.databinding.CardPaymentLayoutBinding
import com.example.grocerly.interfaces.PaymentListener
import com.example.grocerly.model.Card

 class CardViewHolder(private val binding: CardPaymentLayoutBinding,private val listener: PaymentListener): RecyclerView.ViewHolder(binding.root) {

    fun bindCard(card: Card){
        binding.apply {
            val last4No = card.cardNumber.takeLast(4).toString()
            txtviewcardNoblured.text = "XXXX $last4No"

            txtviewdatecard.text = buildString {
                append(card.expiryDate.month)
                append("/")
                append(card.expiryDate.year)
            }

             paymentbtn.setOnClickListener {
               listener.onCvvCheckListener(card.cardId,edttxtcvv.text.toString()){ msg->
                   binding.txtviewerror.text  = msg
               }
             }
        }
    }

}