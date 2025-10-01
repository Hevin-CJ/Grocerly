package com.example.grocerly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.grocerly.R
import com.example.grocerly.databinding.FragmentSignUpBinding
import com.example.grocerly.model.Account

import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.RegisterFieldState
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.viewmodel.SignUpViewmodel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUp : Fragment() {

    private var  signUp:FragmentSignUpBinding?=null
    private val binding get() = signUp!!

    private val signUpViewmodel by viewModels<SignUpViewmodel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        signUp = FragmentSignUpBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createUserWithPassword()
        observeSignUpData()
        observeValidationState()
        actionToLogin()
    }

    private fun actionToLogin() {
        binding.apply {
            actiontosignuptxtview.setOnClickListener {
                findNavController().navigate(R.id.action_signUp_to_login)
            }
        }
    }

    private fun observeSignUpData() {
       lifecycleScope.launch {
           signUpViewmodel.isSigned.collectLatest{result->
               when(result){
                   is NetworkResult.Error -> {
                       Toast.makeText(requireContext(),result.message,Toast.LENGTH_SHORT).show()
                       binding.apply {
                           progressbarsignin.visibility = View.INVISIBLE
                           signupbtn.visibility = View.VISIBLE
                       }
                   }
                   is NetworkResult.Loading -> {
                       binding.apply {
                           progressbarsignin.visibility = View.VISIBLE
                           signupbtn.visibility = View.INVISIBLE
                       }
                   }
                   is NetworkResult.Success -> {
                       Toast.makeText(requireContext(),"Signed In as ${result.data?.email}",Toast.LENGTH_SHORT).show()
                       findNavController().navigate(R.id.action_signUp_to_login)
                   }
                   else ->{

                   }
               }
           }
       }
    }

    private fun createUserWithPassword() {
        binding.apply {
         signupbtn.setOnClickListener {
             if (NetworkUtils.isNetworkAvailable(requireContext())){
                 val user = Account("",  edttxtname.text.toString().trim(),edttxtlastname.text.toString().trim(),edttxtemail.text.toString().trim(),"")
                 val password = edttxtpassword.text.toString().trim()
                 signUpViewmodel.createUser(user,password)
             }else{
                 Toast.makeText(requireContext(),"Enable Wifi or Mobile data",Toast.LENGTH_SHORT).show()
             }
         }
        }
    }


    private fun observeValidationState(){
        lifecycleScope.launch {
            signUpViewmodel.validationState.collect{state->
                if(state.email is RegisterValidation.Failed){
                    binding.edttxtemail.apply {
                        requestFocus()
                        error = state.email.message
                    }
                }

                if(state.password is RegisterValidation.Failed){
                    binding.edttxtpassword.apply {
                        requestFocus()
                        error = state.password.message
                    }
                }

                if(state.firstname is RegisterValidation.Failed){
                    binding.edttxtname.apply {
                        requestFocus()
                        error = state.firstname.message
                    }
                }

                if(state.lastname is RegisterValidation.Failed){
                    binding.edttxtlastname.apply {
                        requestFocus()
                        error = state.lastname.message
                    }
                }
            }
        }
    }
}