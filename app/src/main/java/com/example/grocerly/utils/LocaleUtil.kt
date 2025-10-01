package com.example.grocerly.utils

import android.content.Context
import android.content.res.Configuration
import com.example.grocerly.preferences.GrocerlyDataStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

object LocaleUtil {

    fun setLocale(context: Context,languageCode: String): Context{
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }


     suspend fun applyLocale(context: Context): Context{
        val lang = GrocerlyDataStore(context).getLanguage().first()
         return setLocale(context,lang)
    }
}