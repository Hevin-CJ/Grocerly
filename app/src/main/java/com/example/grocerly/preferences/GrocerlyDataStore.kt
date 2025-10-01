package com.example.grocerly.preferences


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map


private val Context.loginDataStore: DataStore<Preferences> by preferencesDataStore(name = "GROCERLY")
private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore("language_prefs")
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore("session_prefs")

class GrocerlyDataStore(context: Context) {

    private val loginDataStore = context.loginDataStore
    private val languageDataStore = context.languageDataStore
    private val sessionDataStore = context.sessionDataStore


    companion object {
        val isLoggedInKey = booleanPreferencesKey("IS_LOGGED")
        val savedLanguage = stringPreferencesKey("SAVED_LANGUAGE")
        val sessionKey = stringPreferencesKey("session_token_key")
    }

    suspend fun setLoginState(isLoggedIn:Boolean){
        loginDataStore.edit {pref->
            pref[isLoggedInKey] = isLoggedIn
        }
    }

    suspend fun setLanguage(languageCode: String){
        languageDataStore.edit { pref->
         pref[savedLanguage] = languageCode
        }
    }

    suspend fun setSessionToken(token: String){
        sessionDataStore.edit {pref->
            pref[sessionKey] = token
        }
    }

    fun getSessionToken(): Flow<String>{
        return sessionDataStore.data
            .catch { exception->
                if (exception is IOException){
                    emit(emptyPreferences())
                }else{
                    throw exception
                }
            }
            .map {preferences ->
                val token =  preferences[sessionKey] ?: ""
                token
            }
    }

     fun getLanguage(): Flow<String>{
        return languageDataStore.data
            .catch { exception->
                if (exception is IOException){
                    emit(emptyPreferences())
                }else{
                    throw exception
                }
            }
            .map { prefs->
               val savedLang = prefs[savedLanguage] ?: "en"
                savedLang
            }
    }

    fun getLoginState():Flow<Boolean>{
        return loginDataStore.data
            .catch {exception->
                if (exception is IOException){
                    emit(emptyPreferences())
                }else{
                    throw exception
                }
            }

            .map {pref->
                val loginState= pref[isLoggedInKey] ?: false
                loginState
            }
    }

    suspend fun clearAll() {
        loginDataStore.edit { it.clear() }
        languageDataStore.edit { it.clear() }
        sessionDataStore.edit { it.clear() }
    }


}