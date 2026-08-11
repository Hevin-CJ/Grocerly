package com.example.grocerly.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.ui.navigation.AppNavigation
import com.example.grocerly.utils.LocaleUtil
import com.example.grocerly.utils.RazorpayResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

object RazorpayResultManager {
    private val _paymentResult = MutableSharedFlow<RazorpayResult>(extraBufferCapacity = 1)
    val paymentResult = _paymentResult.asSharedFlow()

    fun sendSuccess(paymentId: String?) {
        _paymentResult.tryEmit(RazorpayResult.Success(paymentId))
    }

    fun sendError(code: Int, description: String?) {
        _paymentResult.tryEmit(RazorpayResult.Error(code, description))
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultListener {

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var grocerlyDataStore: GrocerlyDataStore

    private var notificationOrderId by mutableStateOf<String?>(null)
    private var notificationProductId by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        val updatedContext = runBlocking { LocaleUtil.applyLocale(newBase) }
        super.attachBaseContext(updatedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val isSkipped = intent?.getBooleanExtra("skip_splash", false) ?: false
        notificationOrderId = intent?.getStringExtra("orderId")
        notificationProductId = intent?.getStringExtra("productId")

        subscribeToProductUpdates()

        setContent {
                    AppNavigation(
                        isSkippedSplash = isSkipped,
                        notificationOrderId = notificationOrderId,
                        notificationProductId = notificationProductId
                    )

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)


        notificationOrderId = intent.getStringExtra("orderId")
        notificationProductId = intent.getStringExtra("productId")
    }


    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        RazorpayResultManager.sendSuccess(razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, description: String?) {
        RazorpayResultManager.sendError(code, description)
    }

    private fun subscribeToProductUpdates() {
        val topic = "product_updates"
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    "Subscribed to product updates!"
                } else {
                    "Failed to subscribe to product updates."
                }
                Log.d("messagefromfcm", msg)
            }
    }
}