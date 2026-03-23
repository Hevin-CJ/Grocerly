package com.example.grocerly.utils

import android.content.Context
import com.example.grocerly.model.Account
import com.example.grocerly.model.Category
import com.example.grocerly.model.OfferItem
import com.example.grocerly.room.entity.CategoryEntity
import com.example.grocerly.room.entity.OfferEntity
import com.example.grocerly.room.entity.ProfileEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


object Mappers {
    fun Category.toCategoryEntity(): CategoryEntity {
        return CategoryEntity(
            id = this.id,
            category = this.category,
            imageUrl = this.imageUrl
        )
    }

    fun CategoryEntity.toCategory(): Category {
        return Category(
            id = this.id,
            imageUrl = this.imageUrl
        ).also {
            it.category = this.category
        }
    }

    fun ProfileEntity.toAccount(): Account {
        return Account(
            userId = this.userId,
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            imageUrl = this.imageUrl,
            countryCode = this.countryCode,
            phoneNumber = this.phoneNumber
        )
    }

    fun Account.toProfileEntity(): ProfileEntity {
        return ProfileEntity(
            userId = this.userId,
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            imageUrl = this.imageUrl,
            countryCode = this.countryCode,
            phoneNumber = this.phoneNumber
        )
    }

    fun OfferEntity.toOfferItem():OfferItem {
        return OfferItem(
            offerId = this.offerId,
            offerImage = this.offerImage,
            offerBgColor = this.offerBgColor,
            buttonText = this.buttonText,
            buttonBgColor = this.buttonBgColor,
            buttonTxtColor = this.buttonTxtColor,
            descriptionText = this.descriptionText,
            descriptionTextColor = this.descriptionTextColor
        )
    }

    fun OfferItem.toOfferEntity(): OfferEntity {
        return OfferEntity(
            offerId = this.offerId,
            offerImage = this.offerImage,
            offerBgColor = this.offerBgColor,
            buttonText = this.buttonText,
            buttonBgColor = this.buttonBgColor,
            buttonTxtColor = this.buttonTxtColor,
            descriptionText = this.descriptionText,
            descriptionTextColor = this.descriptionTextColor,
            productId = this.productId,
            partnerId = this.partnerId
        )
    }

    fun getFutureDateString(packUp: PackUp, format: String = "dd MMMM, E"): String {

        val zoneId = ZoneId.of("Asia/Kolkata")
        val now = ZonedDateTime.now(zoneId)
        val noonToday = now.withHour(12).withMinute(0).withSecond(0).withNano(0)


        val daysToAdd = when (packUp) {
            PackUp.selectTime -> 0
            PackUp.oneday -> 1
            PackUp.twoday -> 2
            PackUp.threeday -> 3
        }

        val adjustedDaysToAdd = if (now.isAfter(noonToday)) {
            daysToAdd + 1
        } else {
            daysToAdd
        }

        val futureDate = now.plusDays(adjustedDaysToAdd.toLong())

        val formatter = DateTimeFormatter.ofPattern(format, Locale.getDefault())
        return futureDate.format(formatter)

    }

    fun List<OfferEntity>.toOfferItemList(): List<OfferItem> {
        return this.map { it.toOfferItem() }
    }

    fun List<OfferItem>.toOfferEntityList(): List<OfferEntity> {
        return this.map { it.toOfferEntity() }
    }

     fun calculateDynamicSpanCount(desiredItemWidthInDp: Int,context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthInDp = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (screenWidthInDp / desiredItemWidthInDp).toInt()


        return if (spanCount > 0) spanCount else 1
    }


    fun Long.toFormattedDateString(): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

}