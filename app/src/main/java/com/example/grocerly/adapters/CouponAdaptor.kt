package com.example.grocerly.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.databinding.CouponUiLayoutBinding
import com.example.grocerly.model.EarnedCoupon
import com.example.grocerly.model.WishItem
import com.example.grocerly.utils.Mappers.toFormattedDateString

class CouponAdaptor(): ListAdapter<EarnedCoupon, CouponAdaptor.CouponViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CouponViewHolder {
        return CouponViewHolder(CouponUiLayoutBinding.inflate(android.view.LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: CouponViewHolder,
        position: Int
    ) {
        holder.bindCoupon(getItem(position))

    }


    companion object DiffCallback : DiffUtil.ItemCallback<EarnedCoupon>() {
        override fun areItemsTheSame(
            oldItem: EarnedCoupon,
            newItem: EarnedCoupon
        ): Boolean {
            return oldItem.couponId == newItem.couponId
        }

        override fun areContentsTheSame(
            oldItem: EarnedCoupon,
            newItem: EarnedCoupon
        ): Boolean {
            return oldItem == newItem
        }

    }

    inner class CouponViewHolder(private val binding: CouponUiLayoutBinding): RecyclerView.ViewHolder(binding.root){

        fun bindCoupon(earnedCoupon: EarnedCoupon){
            binding.apply {
                couponId.text = "COUPON ID:${earnedCoupon.couponId}"
                tvDiscountValue.text="${earnedCoupon.discountAmount}% OFF"
                tvSubtitle.text="MIN. ORDER:${earnedCoupon.minOrderValue}"
                tvCode.text =  earnedCoupon.code
                tvDisclaimer.text = "*valid till: ${earnedCoupon.expiryTimestamp.toFormattedDateString()}"

                tvCode.setOnClickListener { view ->
                    copyToClipboard(view.context, earnedCoupon.code)
                }

            }
        }
    }

    private fun copyToClipboard(context: Context, code: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Coupon Code", code)
        clipboardManager.setPrimaryClip(clipData)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(context, "Coupon copied", Toast.LENGTH_SHORT).show()
        }
    }
}