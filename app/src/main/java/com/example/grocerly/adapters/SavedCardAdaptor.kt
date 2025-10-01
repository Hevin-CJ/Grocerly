package com.example.grocerly.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.SavedCardListener
import com.example.grocerly.databinding.SavedcardRclayoutBinding
import com.example.grocerly.fragments.SavedCardsDirections
import com.example.grocerly.model.Card
import com.example.grocerly.model.ExpiryDate

class SavedCardAdaptor(private val listener: SavedCardListener): RecyclerView.Adapter<SavedCardAdaptor.savedCardViewHolder>() {

    private val diffUtil = object : DiffUtil.ItemCallback<Card>() {
        override fun areItemsTheSame(
            oldItem: Card,
            newItem: Card
        ): Boolean {
          return oldItem.cardId == newItem.cardId
        }

        override fun areContentsTheSame(
            oldItem: Card,
            newItem: Card
        ): Boolean {
           return oldItem == newItem
        }

    }

    private val asyncDiffer = AsyncListDiffer(this@SavedCardAdaptor,diffUtil)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): savedCardViewHolder {
       return savedCardViewHolder(SavedcardRclayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: savedCardViewHolder,
        position: Int
    ) {
        val currentCard = asyncDiffer.currentList[position]
        holder.setCurrentCard(currentCard)
    }

    override fun getItemCount(): Int {
         return asyncDiffer.currentList.size
    }

    inner class savedCardViewHolder(private val binding: SavedcardRclayoutBinding): RecyclerView.ViewHolder(binding.root){

        fun setCurrentCard(card: Card){
               setEditCardAction(card)
               fetchSavedCardToUi(card)
        }

        private fun fetchSavedCardToUi(card: Card) {
            binding.apply {
                txtviewdatecard.text = formatCardExpiry(card.expiryDate)
                txtviewusernamecard.text = card.holderName
                txtviewcardNoblured.text = maskCardShort(card.cardNumber)
            }
        }

        private fun formatCardExpiry(expiry: ExpiryDate): String {
            val(month,year) = expiry
            val shortYear = year % 100
            return String.format("%02d/%02d",month,shortYear)
        }

        private fun maskCardShort(cardNo: String): String {
            val cleaned = cardNo.replace(" ","")
            val last4No = cleaned.takeLast(4)
            return "XXXX $last4No"
        }

        private fun setEditCardAction(card: Card) {
            binding.apply {
                editcardButton.setOnClickListener {
                    listener.onEditCardClicked(card)
                }
            }
        }

    }


    fun setCard(card: List<Card>){
        asyncDiffer.submitList(card)
    }


}