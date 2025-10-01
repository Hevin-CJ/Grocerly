package com.example.grocerly.Repository.local

import com.example.grocerly.room.dao.ProfileDao
import com.example.grocerly.room.entity.ProfileEntity
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class ProfileLocalRepoImpl @Inject constructor(private val profileDao: ProfileDao) {

    suspend fun upsertProfile(profileEntity: ProfileEntity) = profileDao.upsertProfile(profileEntity)

    fun getProfile() = profileDao.getProfile()

}