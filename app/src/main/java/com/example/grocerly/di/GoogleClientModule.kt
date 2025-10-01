package com.example.grocerly.di

import android.content.Context
import com.example.grocerly.googleclient.GoogleSignInClientRepoImpl
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object GoogleClientModule {

    @Provides
    @ActivityRetainedScoped
    fun provideGoogleSignInClient(@ApplicationContext context: Context, auth: FirebaseAuth): GoogleSignInClientRepoImpl {
        return GoogleSignInClientRepoImpl(context, auth)
    }

}