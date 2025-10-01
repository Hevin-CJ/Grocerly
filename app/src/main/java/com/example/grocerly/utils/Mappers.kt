package com.example.grocerly.utils

import com.example.grocerly.model.Account
import com.example.grocerly.model.Category
import com.example.grocerly.model.OfferItem
import com.example.grocerly.room.entity.CategoryEntity
import com.example.grocerly.room.entity.OfferEntity
import com.example.grocerly.room.entity.ProfileEntity

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
            descriptionTextColor = this.descriptionTextColor
        )
    }

    fun List<OfferEntity>.toOfferItemList(): List<OfferItem> {
        return this.map { it.toOfferItem() }
    }

    fun List<OfferItem>.toOfferEntityList(): List<OfferEntity> {
        return this.map { it.toOfferEntity() }
    }

}