package com.example.grocerly.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.R
import com.example.grocerly.adapters.viewholder.CardViewHolder
import com.example.grocerly.adapters.viewholder.CashOnDeliveryViewHolder
import com.example.grocerly.adapters.viewholder.HeaderViewHolder
import com.example.grocerly.adapters.viewholder.UpiViewHolder
import com.example.grocerly.databinding.CardPaymentLayoutBinding
import com.example.grocerly.databinding.CashondeliveryLayoutBinding
import com.example.grocerly.databinding.PaymentHeaderLayoutBinding
import com.example.grocerly.databinding.UpiLayoutBinding
import com.example.grocerly.interfaces.PaymentListener
import com.example.grocerly.model.Card
import com.example.grocerly.utils.PaymentMethodItem

class PaymentAdaptor(private val listener: PaymentListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var paymentItems: MutableList<PaymentMethodItem> = mutableListOf()

    private var cardItems:List<Card> = emptyList()

    private var expandedIndex: Int? = null


    override fun getItemViewType(position: Int): Int {
        return when (val item = paymentItems[position]) {
            is PaymentMethodItem.Header -> 0
            is PaymentMethodItem.Content -> when (item.type) {
                PaymentMethodItem.Type.CARD -> 1
                PaymentMethodItem.Type.UPI -> 2
                PaymentMethodItem.Type.COD -> 3
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType){
            0 -> {
                val binding = PaymentHeaderLayoutBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding){ position,header ->
                    toggleContent(position,header)
                }
            }
            1 ->{
                val binding = CardPaymentLayoutBinding.inflate(inflater, parent, false)
                CardViewHolder(binding,listener)
            }
            2 ->{
                val binding = UpiLayoutBinding.inflate(inflater, parent, false)
                UpiViewHolder(binding,listener)
            }
            3 -> {
                val binding = CashondeliveryLayoutBinding.inflate(inflater, parent, false)
                CashOnDeliveryViewHolder(binding,listener)
            }
            else -> throw IllegalArgumentException("Unknown viewType")


        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when(val currentItem = paymentItems[position]){
            is PaymentMethodItem.Header -> (holder as HeaderViewHolder).bindHeader(currentItem,position)

            is PaymentMethodItem.Content ->{
                when(currentItem.type){
                    PaymentMethodItem.Type.CARD -> {
                        currentItem.card?.let { card ->
                            (holder as CardViewHolder).bindCard(card)
                        }

                    }
                    PaymentMethodItem.Type.UPI -> (holder as UpiViewHolder).bindUpi()
                    PaymentMethodItem.Type.COD -> (holder as CashOnDeliveryViewHolder).bindCod()
                }
            }
        }
    }

    override fun getItemCount(): Int {
       return paymentItems.size
    }

    private fun toggleContent(position: Int,header: PaymentMethodItem.Header){
        if (expandedIndex != null) {
            val removeIndex = expandedIndex!! + 1

            if (removeIndex < paymentItems.size && paymentItems[removeIndex] is PaymentMethodItem.Content) {
                paymentItems.removeAt(removeIndex)
                notifyItemRemoved(removeIndex)
            }


            if (expandedIndex == position) {
                expandedIndex = null
                return
            }
        }


        val newContent = when (header.id) {
            1 -> cardItems.map { card -> PaymentMethodItem.Content(PaymentMethodItem.Type.CARD, card) }
            2 -> listOf(PaymentMethodItem.Content(PaymentMethodItem.Type.UPI))
            3 -> listOf(PaymentMethodItem.Content(PaymentMethodItem.Type.COD))
            else -> emptyList()
        }

        paymentItems.addAll(position + 1, newContent)
        notifyItemRangeInserted(position + 1, newContent.size)
        expandedIndex = position
    }


    fun setCard(cards:List<Card>){
        this.cardItems = cards
        notifyDataSetChanged()
    }

    fun setPaymentMethod(paymentMethod:List<PaymentMethodItem>){
        this.paymentItems = paymentMethod.toMutableList()
        notifyDataSetChanged()
    }
}