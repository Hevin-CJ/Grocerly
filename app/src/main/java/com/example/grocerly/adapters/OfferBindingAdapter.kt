package com.example.grocerly.adapters

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import java.util.Locale
import androidx.core.graphics.toColorInt
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.grocerly.R
import com.example.grocerly.utils.QuantityType

object OfferBindingAdapter {

    @BindingAdapter("setOfferImage")
    @JvmStatic
    fun setOfferImage(imageView: ImageView,savedFile: String){

        try {
            Glide.with(imageView.context)
                .load(savedFile)
                .into(imageView)

        }catch (e:Exception){
            e.printStackTrace()
        }

    }


    @BindingAdapter("setCategoryImage")
    @JvmStatic
    fun setCategoryImage(imageView: ImageView,url: String){
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.placeholderimage)
            .error(R.drawable.noimage)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)

        Glide.with(imageView.context)
            .applyDefaultRequestOptions(requestOptions)
            .load(url)
            .thumbnail(Glide.with(imageView.context)
                .load(url)
                .sizeMultiplier(0.25f) )
            .into(imageView)
    }

    @JvmStatic
    @BindingAdapter("setFormattedRating")
    fun setFormattedRating(view: TextView, value: Double) {
        view.text = String.format(Locale.getDefault(), "%.1f", value)
    }


    @JvmStatic
    @BindingAdapter("formattedTotalRating")
    fun setFormattedTotalRating(view: TextView, totalRating: Int) {
       view.text = buildString {
        append("(")
        append(totalRating)
        append(")")
    }
    }

    @JvmStatic
    @BindingAdapter("buttonTextColor")
    fun setButtonTextColor(button: Button, colorString: String?) {
        if (!colorString.isNullOrEmpty()) {
            try {
                button.setTextColor(colorString.toColorInt())
            } catch (e: IllegalArgumentException) {
                button.setTextColor(Color.BLACK)
                e.printStackTrace()
            }
        }
    }


    @BindingAdapter("android:setOfferBackgroundColor")
    @JvmStatic
    fun setOfferBackgroundColor(view: ConstraintLayout, colorString: Int) {
        colorString.let {
            view.setBackgroundColor(it)
        }
    }

    @BindingAdapter("strikeThrough")
    @JvmStatic
    fun setStrikeThrough(textView: TextView, strikeThrough: Boolean) {
        if (strikeThrough) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    @BindingAdapter("convertQuantityIntoString")
    @JvmStatic
    fun convertQuantityIntoString(textView: TextView,quantityType: QuantityType){
        val text =  when(quantityType){
            QuantityType.selectQuantity -> "/ Quantity"
            QuantityType.perKilogram -> "/ Kg"
            QuantityType.perLiter -> "/ L"
            QuantityType.perPiece -> "/ Piece"
            QuantityType.perPacket -> "/ Packet"
        }
        textView.text = text
    }

}