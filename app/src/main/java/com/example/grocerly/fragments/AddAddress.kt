package com.example.grocerly.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.grocerly.R
import com.example.grocerly.databinding.FragmentAddAddressBinding
import com.example.grocerly.model.Address
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.viewmodel.SaveAddressViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddAddress : Fragment() {
    private var addAddress: FragmentAddAddressBinding?=null
    private val binding get() = addAddress!!

    private val saveAddressViewModel: SaveAddressViewModel by activityViewModels()
    private lateinit var loadingDialogue: LoadingDialogue

    var caller: String?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        addAddress = FragmentAddAddressBinding.inflate(inflater,container,false)
        caller = arguments?.getString("bundlePass") ?: "saved_address"
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialogue = LoadingDialogue(requireContext())
        setToolbar()
        saveAddressToDb()
        observeAddressValidation()
        setAddressType()
        observeAddingAddressToDb()
        getCurrentLocationFromUseMyLocation()
    }

    override fun onResume() {
        super.onResume()
        saveAddressViewModel.getLocationDetails(requireContext())
    }

    private fun getCurrentLocationFromUseMyLocation() {
        binding.getlocationbtn.setOnClickListener {
            observeFusedLocationAddress()
        }
    }


    private fun observeFusedLocationAddress(){
        lifecycleScope.launch {
            saveAddressViewModel.locationAddress.collectLatest { location->
                when(location){
                    is NetworkResult.Error<*> -> {
                        Toast.makeText(requireContext(),location.message, Toast.LENGTH_SHORT).show()
                        loadingDialogue.dismiss()
                    }
                    is NetworkResult.Loading<*> -> {
                        loadingDialogue.show()
                    }
                    is NetworkResult.Success<*> -> {
                        loadingDialogue.dismiss()
                        binding.apply {
                            edttxtcity.setText(location.data?.city)
                            edttxtstate.setText(location.data?.state)
                            edttxtpincode.setText(location.data?.pinCode)
                        }
                    }
                    is NetworkResult.UnSpecified<*> -> {
                        loadingDialogue.dismiss()
                    }
                }
            }
        }
    }




    private fun observeAddingAddressToDb() {
        viewLifecycleOwner.lifecycleScope.launch {
            saveAddressViewModel.savedAddressState.collectLatest {state->
                when(state){
                    is NetworkResult.Error<*> ->{
                        loadingDialogue.dismiss()
                        Toast.makeText(requireContext(),state.message.toString(), Toast.LENGTH_SHORT).show()


                    }
                    is NetworkResult.Loading<*> -> {

                        loadingDialogue.show()
                        clearValidationErrors()
                    }
                    is NetworkResult.Success<*> ->{

                        loadingDialogue.dismiss()

                        when(caller){
                            "checkout" ->{
                                findNavController().navigate(R.id.action_addAddress_to_checkout,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout,true).build())
                            }

                            "saved_address" ->{
                                findNavController().navigate(R.id.action_addAddress_to_savedAddress,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.savedAddress,false).build())
                            }
                        }

                    }
                    is NetworkResult.UnSpecified<*> -> {
                        loadingDialogue.dismiss()
                    }
                }
            }
        }
    }

    private fun observeAddressValidation() {

      viewLifecycleOwner.lifecycleScope.launch {


          saveAddressViewModel.addressValidateState.collectLatest { state->

            if (state.firstName is RegisterValidation.Failed){
                binding.txtinputlayoutfirstname.apply {
                    requestFocus()
                    helperText = state.firstName.message
                }
            }else{
                binding.txtinputlayoutfirstname.apply {
                    helperText=null
                }
            }

              if (state.phoneNo is RegisterValidation.Failed){
                  binding.txtinputlayoutphoneno.apply {
                      requestFocus()
                      helperText = state.phoneNo.message
                  }
              }else{
                  binding.txtinputlayoutphoneno.apply {
                      helperText=null
                  }
              }

              if (state.alternatePhNo is RegisterValidation.Failed){
                  binding.txinputlayoutalternateno.apply {
                      requestFocus()
                      helperText = state.alternatePhNo.message
                  }
              }else{
                  binding.txinputlayoutalternateno.apply {
                      helperText=null
                  }
              }

              if (state.pincode is RegisterValidation.Failed){
                  binding.txtinputlayoutpincode.apply {
                      requestFocus()
                      helperText = state.pincode.message
                  }
              }else{
                  binding.txtinputlayoutpincode.apply {
                      helperText=null
                  }
              }

              if (state.state is RegisterValidation.Failed){
                  binding.txtinputstate.apply {
                      requestFocus()
                      helperText = state.state.message
                  }
              }else{
                  binding.txtinputstate.apply {
                      helperText=null
                  }
              }

              if (state.city is RegisterValidation.Failed){
                  binding.txtinputcity.apply {
                      requestFocus()
                      helperText = state.city.message
                  }
              }else{
                  binding.txtinputcity.apply {
                      helperText=null
                  }
              }

              if (state.deliveryAddress is RegisterValidation.Failed){
                  binding.txtinputfulladdress.apply {
                      requestFocus()
                      helperText = state.deliveryAddress.message
                  }
              }else{
                  binding.txtinputfulladdress.apply {
                      helperText=null
                  }
              }

              if (state.landMark is RegisterValidation.Failed){
                  binding.txtinputlandmark.apply {
                      requestFocus()
                      helperText = state.landMark.message
                  }
              }else{
                  binding.txtinputlandmark.apply {
                      helperText=null
                  }
              }
          }
      }
    }

    private fun clearValidationErrors() {
        val inputLayouts = listOf(
            binding.txtinputlayoutfirstname,
            binding.txtinputlayoutphoneno,
            binding.txinputlayoutalternateno,
            binding.txtinputlayoutpincode,
            binding.txtinputstate,
            binding.txtinputcity,
            binding.txtinputfulladdress,
            binding.txtinputlandmark
        )

        inputLayouts.forEach { it.helperText = null }
    }


    private fun setToolbar() {
        binding.apply {
            toolbardeliveryAddress.setTitle("Add Address")
            toolbardeliveryAddress.setNavigationIcon(R.drawable.backarrow)
            toolbardeliveryAddress.setNavigationIconTint(ContextCompat.getColor(requireContext(),R.color.black))
            toolbardeliveryAddress.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            toolbardeliveryAddress.setNavigationOnClickListener {

                when(caller){
                    "checkout" ->{
                        findNavController().navigate(R.id.action_addAddress_to_checkout,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout,true).build())
                    }

                    "saved_address" ->{
                        findNavController().navigate(R.id.action_addAddress_to_savedAddress,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.savedAddress,false).build())
                    }
                }


            }
        }
    }

    private fun saveAddressToDb() {
        binding.apply {
            saveaddressbtn.setOnClickListener {
                if (NetworkUtils.isNetworkAvailable(requireContext())){
                    val firstName = edttxtfirstname.text.toString().trim().lowercase()
                    val phoneNo = edttxtphoneno.text.toString().trim()
                    val alternateNo = edttxtalternatephoneno.text.toString().trim()
                    val pinCode = edttxtpincode.text.toString().trim()
                    val state = edttxtstate.text.toString().trim()
                    val city  = edttxtcity.text.toString().trim()
                    val address = edttxtfulladdress.text.toString().trim()
                    val landMark = edttxtlandmark.text.toString().trim()
                    val addressType = getAddressType(buttongroup.checkedButtonId)
                    Log.d("addressTypegot",addressType.toString())

                    val getAddress = Address("",firstName,phoneNo,alternateNo,state,city,address,landMark,pinCode,addressType)
                    saveAddressViewModel.insertAddressIntoDb(getAddress)

                }else{
                    Toast.makeText(requireContext(),"Enable Wifi or Mobile Data", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun getAddressType(id:Int): String {
      return when (id) {
            R.id.btnhome -> "Home"
            R.id.btnwork -> "Work"
            R.id.btnother -> "Other"
            else -> "Home"
        }
    }


    private fun setAddressType(){
        binding.buttongroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked){

                resetButtonStyles()
                when (checkedId) {
                    R.id.btnhome -> {
                        binding.btnhome.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                        binding.btnhome.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    }
                    R.id.btnwork -> {
                        binding.btnwork.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                        binding.btnwork.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    }
                    R.id.btnother -> {
                        binding.btnother.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                        binding.btnother.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    }
                }
            }
        }
    }


    private fun resetButtonStyles() {
        binding.btnhome.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.btnhome.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        binding.btnwork.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.btnwork.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        binding.btnother.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.btnother.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
    }


}