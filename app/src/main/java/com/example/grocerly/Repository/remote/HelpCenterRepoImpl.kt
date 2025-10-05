package com.example.grocerly.Repository.remote

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.example.grocerly.model.ChatMessageModel
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.content
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@ActivityRetainedScoped
class HelpCenterRepoImpl @Inject constructor(private val generativeModel: GenerativeModel) {


    fun getQuestionAndSendMessage(
        question: String,
        history: List<ChatMessageModel>
    ): Flow<NetworkResult<List<ChatMessageModel>>> = flow {

        emit(NetworkResult.Loading())

        try {

            val chat = generativeModel.startChat(
                history = history.map {
                    val role = if (it.role == "loading") "model" else it.role
                    content(role) { text(it.message) }
                }
            )


            val response = chat.sendMessage(question)
            val modelResponse = ChatMessageModel(response.text ?: "Sorry, I couldn't process that.", "model")


            val newHistory = history + ChatMessageModel(question, "user") + modelResponse

            emit(NetworkResult.Success(newHistory))
            Log.d("GeminiResponse", "getQuestionAndSendMessage: ${response.text}")

        } catch (e: Exception) {
            val errorMessage = e.message ?: "An unknown error occurred"
            Log.e("GeminiError", "getQuestionAndSendMessage: $errorMessage", e)

            emit(NetworkResult.Error(errorMessage))
        }
    }

}