package com.example.grocerly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.adapters.ChangeAddressAdaptor
import com.example.grocerly.databinding.FragmentChangeAddressBinding
import com.example.grocerly.interfaces.AddressActionListener
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.CheckoutViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChangeAddress(private val listener: AddressActionListener): BottomSheetDialogFragment(R.layout.fragment_change_address) {

    private var changeAddress: FragmentChangeAddressBinding?=null
    private val binding get() = changeAddress!!

    private val checkoutViewModel:CheckoutViewModel by  activityViewModels()

    private lateinit var loadingDialogue: LoadingDialogue

    private val changeAddressAdaptor: ChangeAddressAdaptor by lazy { ChangeAddressAdaptor(listener) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        changeAddress = FragmentChangeAddressBinding.inflate(inflater,container,false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAddressAdaptor()
        setActionToAddAddress()
        observeSavedAddresses()
    }

    private fun setAddressAdaptor() {
        binding.recyclerView2.adapter = changeAddressAdaptor
        binding.recyclerView2.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
    }

    private fun observeSavedAddresses() {
       lifecycleScope.launch {
           checkoutViewModel.savedAddresses.collectLatest {address->
               when(address){
                   is NetworkResult.Error<*> -> {
                       Toast.makeText(requireContext(),address.message.toString(), Toast.LENGTH_SHORT).show()
                       loadingDialogue.dismiss()
                   }
                   is NetworkResult.Loading<*> -> {
                       loadingDialogue.show()
                   }
                   is NetworkResult.Success<*> ->{
                       address.data?.let {
                           changeAddressAdaptor.setAddresses(it)
                       }
                       loadingDialogue.dismiss()
                   }
                   is NetworkResult.UnSpecified<*> -> {
                       loadingDialogue.dismiss()
                   }
               }
           }
       }
    }

    private fun setActionToAddAddress() {
        binding.txtviewaddnewaddress.setOnClickListener {
            listener.onAddressActionRequested()
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        checkoutViewModel.fetchAddress()
    }

}