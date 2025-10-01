package com.example.grocerly.activity

import android.content.Context
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
import com.example.grocerly.fragments.Payments
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.LocaleUtil
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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



    private lateinit var navController: NavController


    override fun attachBaseContext(newBase: Context) {
        val updatedContext = runBlocking {  LocaleUtil.applyLocale(newBase) }
        super.attachBaseContext(updatedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        binding.tabLayoutmain.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _,destination,_->
            when(destination.id){
                R.id.cart,R.id.splash,R.id.login,R.id.signUp,R.id.checkout,R.id.payments,R.id.orderPlaced ->{
                  setTabLayoutVisibility(false)
                }
                else -> {
                   setTabLayoutVisibility(true)
                }
            }
        }

        setNavigationGraph()
        setBottomNavigationListener()
    }

    private fun setBottomNavigationListener() {
        binding.tabLayoutmain.setOnItemSelectedListener { item ->
            val destinationId = item.itemId
            val currentDest = navController.currentDestination?.id

            if (currentDest != destinationId) {
                navController.navigate(
                    destinationId,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(navController.graph.startDestinationId, false)
                        .setLaunchSingleTop(true)
                        .build()
                )
            }
            true
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

          navController.setGraph(graphId)
      }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() ||  super.onSupportNavigateUp()
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
            currentFragment.onPaymentError(p0,p1)
        }
    }


}


