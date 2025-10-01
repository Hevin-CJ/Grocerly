package com.example.grocerly.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.databinding.FragmentLoginBinding
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.viewmodel.LoginViewModel
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.LoginStatusCallback
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.OAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class Login : Fragment() {
    private var login:FragmentLoginBinding?=null
    private val binding get() = login!!

    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var callbackManager: CallbackManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       login = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginToFirebase()
        observeLoginState()
        observeValidationState()
        actionLoginToSignUp()
        loginUsingGoogleSignIn()
        setUpFacebookLogin()
        setUpTwitterLogin()
    }

    private fun setUpTwitterLogin() {
        binding.xbtn.setOnClickListener {
            loginViewModel.signInWithX(requireActivity())
        }
    }

    private fun setUpFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()

        binding.facebookbtn.setOnClickListener {

            binding.facebookbtn.setOnClickListener {
                val currentToken = AccessToken.getCurrentAccessToken()

                if (currentToken != null && !currentToken.isExpired) {
                    Log.d("FacebookLogin", "Token found, attempting express login.")
                    loginViewModel.signInWithFacebook(currentToken)
                } else {
                    Log.d("FacebookLogin", "No valid token, starting normal login flow.")
                    LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
                }
            }

        }

        LoginManager.getInstance().registerCallback(callbackManager,object : FacebookCallback<LoginResult>{
            override fun onCancel() {
                Toast.makeText(requireContext(),"Login Cancelled",Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(requireContext(),error.message.toString(),Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(result: LoginResult) {
                Log.d("accesstokengot",result.accessToken.token)
                loginViewModel.signInWithFacebook(result.accessToken)
            }

        })


    }

    private fun loginUsingGoogleSignIn() {
        binding.apply {
            googlebtn.setOnClickListener{
                loginViewModel.signInWithGoogle()
            }
        }
    }

    private fun actionLoginToSignUp() {
        binding.apply {
            actiontosignuptxtview.setOnClickListener{
                findNavController().navigate(R.id.action_login_to_signUp)
            }
        }
    }

    private fun observeValidationState() {
        lifecycleScope.launch {
            loginViewModel.validationState.collect{state->
                if (state.email is RegisterValidation.Failed){
                    binding.edttxtemail.apply {
                        requestFocus()
                        error = state.email.message
                    }
                }

                if (state.password is RegisterValidation.Failed){
                    binding.edttxtpassword.apply {
                        requestFocus()
                        error = state.password.message
                    }
                }
            }
        }
    }

    private fun observeLoginState() {
      lifecycleScope.launch{
          loginViewModel.loginstate.collect{ result->
              when(result){
                  is NetworkResult.Error -> {
                      Toast.makeText(requireContext(),result.message,Toast.LENGTH_SHORT).show()
                  }
                  is NetworkResult.Loading -> {
                      Toast.makeText(requireContext(),"Loading,Please wait...",Toast.LENGTH_SHORT).show()
                  }
                  is NetworkResult.Success -> {
                      Log.d("issuccess",result.data.toString())
                    setPopUpToHomeFragment()
                  }
                else -> {

                }
              }

          }
      }
    }

    private fun loginToFirebase() {
        binding.apply {
          loginbtn.setOnClickListener{
              if (NetworkUtils.isNetworkAvailable(requireContext())){
                  val email = edttxtemail.text.toString().trim()
                  val password = edttxtpassword.text.toString().trim()
                  loginViewModel.loginUserIntoFirebase(email,password)
              }else{
                  Toast.makeText(requireContext(),"Enable Wifi or Mobile data",Toast.LENGTH_SHORT).show()
              }
          }
        }
    }

    private fun setPopUpToHomeFragment(){
       lifecycleScope.launch {
           val intent = Intent(requireContext(), MainActivity::class.java).apply {
               flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
           }
           startActivity(intent)
       }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        login = null
    }

}