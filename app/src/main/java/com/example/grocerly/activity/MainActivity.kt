package com.example.grocerly.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.grocerly.R
import com.example.grocerly.databinding.ActivityMainBinding
import com.example.grocerly.fragments.Home
import com.example.grocerly.fragments.Payments
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.utils.LocaleUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PaymentResultListener {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var db: FirebaseFirestore

    @Inject
    lateinit var grocerlyDataStore: GrocerlyDataStore

    private var lastHomeClickTime: Long = 0
    private val DOUBLE_CLICK_TIME_DELTA: Long = 300

    private lateinit var navController: NavController

    override fun attachBaseContext(newBase: Context) {
        val updatedContext = runBlocking { LocaleUtil.applyLocale(newBase) }
        super.attachBaseContext(updatedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.cart, R.id.splash, R.id.login, R.id.signUp, R.id.checkout, R.id.payments, R.id.orderPlaced -> {
                    setTabLayoutVisibility(false)
                }
                else -> {
                    setTabLayoutVisibility(true)
                }
            }
        }

        // Initialize graph. The listeners are now safely attached inside this function.
        setNavigationGraph()
        subscribeToProductUpdates()
    }

    private fun setupBottomNavigation() {
        binding.tabLayoutmain.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.home) {
                val clickTime = System.currentTimeMillis()

                if (clickTime - lastHomeClickTime < DOUBLE_CLICK_TIME_DELTA) {

                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
                    val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

                    if (currentFragment is Home) {
                        currentFragment.resetAndScrollToTop()
                    }
                }
                lastHomeClickTime = clickTime
            }
        }
    }

    fun setTabLayoutVisibility(visible: Boolean) {
        binding.tabLayoutmain.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setNavigationGraph() {
        lifecycleScope.launch {
            val currentUser = auth.currentUser?.uid.toString().isEmpty()
            val isLoggedIn = grocerlyDataStore.getLoginState().first()

            val graphId = if (!currentUser && isLoggedIn) {
                R.navigation.home_nav
            } else {
                R.navigation.grocerly_auth_nav
            }

            val orderId = intent.getStringExtra("orderId")
            val productId = intent.getStringExtra("productId")

            val bundle = if (orderId != null && productId != null) {
                Bundle().apply {
                    putString("notification_orderId", orderId)
                    putString("notification_productId", productId)
                }
            } else null

            // 1. Set the Graph first
            navController.setGraph(graphId, bundle)

            // 2. Setup standard NavigationUI (This natively handles multiple backstacks and state saving!)
            binding.tabLayoutmain.setupWithNavController(navController)

            // 3. Setup the double-tap to scroll top
            setupBottomNavigation()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val orderId = intent.getStringExtra("orderId")
        val productId = intent.getStringExtra("productId")

        if (orderId != null && productId != null) {
            val bundle = Bundle().apply {
                putString("notification_orderId", orderId)
                putString("notification_productId", productId)
            }

            navController.navigate(
                R.id.home,
                bundle,
                NavOptions.Builder()
                    .setPopUpTo(navController.graph.startDestinationId, false)
                    .setLaunchSingleTop(true)
                    .build()
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onPaymentSuccess(p0: String?) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
        if (currentFragment is Payments) {
            currentFragment.onPaymentSuccess(p0)
        }
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
        if (currentFragment is Payments) {
            currentFragment.onPaymentError(p0, p1)
        }
    }

    private fun subscribeToProductUpdates() {
        val topic = "product_updates"
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                var msg = "Subscribed to product updates!"
                if (!task.isSuccessful) {
                    msg = "Failed to subscribe to product updates."
                }
                Log.d("messagefromfcm", msg)
            }
    }
}