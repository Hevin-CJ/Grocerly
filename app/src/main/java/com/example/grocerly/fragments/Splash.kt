package com.example.grocerly.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.window.SplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.databinding.FragmentSplashScreenBinding
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.viewmodel.HomeViewModel
import com.example.grocerly.viewmodel.LoginViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Splash (): Fragment() {

    private var splash:FragmentSplashScreenBinding?=null
    private val binding  get() =  splash!!

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var grocerlyDataStore: GrocerlyDataStore

   private  val loginViewModel: LoginViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       splash = FragmentSplashScreenBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isskipped = requireActivity().intent.getBooleanExtra("skip_splash",false)

        if (isskipped){
           navigateToNextScreen()
        }else{
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToNextScreen()
            },3000)
        }

    }

    private fun navigateToNextScreen() {
        val navOptions = NavOptions.Builder().setPopUpTo(R.id.splash,true).build()
        lifecycleScope.launch {
            grocerlyDataStore.getLoginState().collectLatest{state->
                when(state){
                    true -> {
                        val intent = Intent(requireContext(), MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                    false -> {
                        findNavController().navigate(R.id.action_splash_to_login,null,navOptions)
                    }
                }
            }
        }
    }

}