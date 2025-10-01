package com.example.grocerly.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.grocerly.databinding.RecycOffersBinding
import com.example.grocerly.model.OfferItem

class OffersAdaptor():RecyclerView.Adapter<OffersAdaptor.OfferViewHolder>() {

    private var offerItems: List<OfferItem> = emptyList()

    inner  class OfferViewHolder(private val binding: RecycOffersBinding):ViewHolder(binding.root){

        fun bindOffer(offerItem: OfferItem){
            binding.apply {
                txtviewofferText.text = offerItem.descriptionText
                txtviewofferText.setTextColor(offerItem.descriptionTextColor.toColorInt())
                shopnowbtn.text = offerItem.buttonText
                shopnowbtn.setBackgroundColor(offerItem.buttonBgColor.toColorInt())
                shopnowbtn.setTextColor(offerItem.buttonTxtColor.toColorInt())
                cardviewoffer.setCardBackgroundColor(offerItem.offerBgColor.toColorInt())

                Glide.with(binding.root.context)
                    .load(offerItem.offerImage)
                    .into(binding.imgviewItem)
            }
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OfferViewHolder {

        return OfferViewHolder(RecycOffersBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: OfferViewHolder,
        position: Int
    ) {

        val currentOffer = offerItems[position]
        holder.bindOffer(currentOffer)
    }

    override fun getItemCount(): Int {
        return offerItems.size
    }


    fun setOffers(offers:List<OfferItem>){
        this.offerItems = offers
        notifyDataSetChanged()
    }


}