package com.example.grocerly.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.databinding.FragmentProfileBinding
import com.example.grocerly.model.Account
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.Mappers.toAccount
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.ProfileViewModel
import com.google.android.play.integrity.internal.ac
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Profile : Fragment() {
    private var profile:FragmentProfileBinding?=null
    private val binding get() = profile!!

    private val profileViewModel:ProfileViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        profile = FragmentProfileBinding.inflate(inflater,container,false)
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpLoginOutButton()
        observeLogoutState()
        observeFetchingAccountDetails()
        actionToEditProfile()
        actionToSavedAddress()
        actionToEditLanguage()
        actionToSavedCards()
        actionToOrders()
        observeAccountErrorDetails()
        setActionToHelpCenter()
    }

    private fun setActionToHelpCenter() {
        binding.Helpcenterbtn.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_helpCenter,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.profile,false).build())
        }
    }

    private fun actionToOrders() {
        binding.apply {
            ordersbtn.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_orders,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.profile,false).build())
            }
        }
    }

    private fun actionToSavedCards() {
        binding.savedcardslayout.apply {
            setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_savedCards,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.profile,false).build())
            }
        }
    }


    private fun actionToEditLanguage() {
        binding.apply {
            editlanguagelayout.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_editLanguage,null,NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.profile , false).build())
            }
        }
    }

    private fun actionToSavedAddress() {
        binding.apply {
            savedaddresslayout.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_savedAddress,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.profile,false).build())
            }
        }
    }

    private fun actionToEditProfile() {
        binding.apply {
            editProfilelayout.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_editProfile)
            }
        }
    }

    private fun observeAccountErrorDetails() {
      viewLifecycleOwner.lifecycleScope.launch {
          profileViewModel.accountDetails.collectLatest { account->
              if(account is NetworkResult.Error){
                  Toast.makeText(requireContext(),account.message.toString(),Toast.LENGTH_SHORT).show()
              }
          }
      }
    }


    private fun observeFetchingAccountDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.getProfileData.collectLatest { account->
              account?.let {
                  showAccountDetails(account.toAccount())
              }
            }
        }
    }

    private fun showAccountDetails(account: Account) {
       binding.apply {
           Glide.with(requireContext()).load(account.imageUrl).placeholder(R.drawable.profileimg).into(imgiviewpicture)
           txtviewusername.text = buildString {
               append("Hello , ")
               append(account.firstName)}
           txtviewemail.text = account.email
       }
    }

    private fun observeLogoutState() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.logoutState.collect{result->
                when(result){
                    is NetworkResult.Error -> {
                        Toast.makeText(requireContext(),result.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
                        Toast.makeText(requireContext(),"Loading.. Please wait",Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Success -> {
                        Toast.makeText(requireContext(),result.data,Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        }

                    }
                    is NetworkResult.UnSpecified -> {

                    }
                }
            }
        }
    }

    private fun setUpLoginOutButton() {
        binding.apply {
            logoutbtn.setOnClickListener{
                profileViewModel.LogOutUserFromFirebase()
            }
        }
    }

}