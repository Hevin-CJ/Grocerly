package com.example.grocerly.Repository.local

import com.example.grocerly.room.dao.OfferDao
import com.example.grocerly.room.entity.OfferEntity
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject


@ActivityRetainedScoped
class OfferLocalRepoImpl  @Inject constructor(private val offerDao: OfferDao) {


    suspend fun upsertOffer(offerEntity: List<OfferEntity>) = offerDao.upsertOffer(offerEntity)

    fun getOffers() = offerDao.getOffers()


}