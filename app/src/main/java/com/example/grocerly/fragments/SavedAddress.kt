package com.example.grocerly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.adapters.AddressAdaptor
import com.example.grocerly.databinding.FragmentSavedAddressBinding
import com.example.grocerly.model.Address
import com.example.grocerly.onAddressMenuClickListener
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.SaveAddressViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SavedAddress : Fragment() {
    private var savedAddress: FragmentSavedAddressBinding?=null
    private val binding get() = savedAddress!!

    private lateinit var addressAdaptor: AddressAdaptor
    private lateinit var loadingDialogue: LoadingDialogue

    private val saveAddressViewModel: SaveAddressViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       savedAddress = FragmentSavedAddressBinding.inflate(inflater,container,false)
        (requireActivity() as MainActivity).setTabLayoutVisibility(false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        actionToAddAddress()
        setAddressAdaptor()
        observeFetchedAddress()
        observeDeleteAddress()
    }

    private fun setToolbar() {
        binding.apply {
            toolbar.setTitle("Address")
            toolbar.setNavigationIcon(R.drawable.backarrow)
            toolbar.setNavigationIconTint(ContextCompat.getColor(requireContext(),R.color.black))
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun observeDeleteAddress() {
        viewLifecycleOwner.lifecycleScope.launch {
            saveAddressViewModel.deleteAddress.collectLatest { deleted->
                when(deleted){
                    is NetworkResult.Error<*> -> {
                        loadingDialogue.dismiss()
                        Toast.makeText(requireContext(),deleted.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading<*> -> {
                        loadingDialogue.show()
                    }
                    is NetworkResult.Success<*> -> {
                        loadingDialogue.dismiss()
                        Toast.makeText(requireContext(),deleted.data.toString(), Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.UnSpecified<*> ->{
                        loadingDialogue.dismiss()
                    }
                }
            }
        }
    }


    private fun observeFetchedAddress() {
        viewLifecycleOwner.lifecycleScope.launch {
            saveAddressViewModel.fetchedAddress.collectLatest {address->
                when(address){
                    is NetworkResult.Error<*> -> {
                       showShimmer(false)
                        Toast.makeText(requireContext(),address.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading<*> -> {
                        showShimmer(true)
                    }
                    is NetworkResult.Success<*> -> {
                        address.data?.let {
                            addressAdaptor.setAddress(it)
                            showShimmer(false)
                            showAddressNotFound(it.isEmpty())
                        }

                    }
                    is NetworkResult.UnSpecified<*> -> {
                       showShimmer(false)
                    }
                }
            }
        }
    }

    private fun showAddressNotFound(isTrue: Boolean) {
        if (isTrue){
            binding.imgnoaddressfound.visibility = View.VISIBLE
            binding.txtviewaddress.visibility = View.VISIBLE
        }else{
            binding.imgnoaddressfound.visibility = View.INVISIBLE
            binding.txtviewaddress.visibility = View.INVISIBLE
        }
    }


    override fun onResume() {
        super.onResume()
        saveAddressViewModel.getAllAddressFromDatabase()
    }

    private fun showShimmer(isTrue: Boolean){
        if (isTrue){
            binding.shimmeraddress.visibility = View.VISIBLE
            binding.shimmeraddress.startShimmer()
        }else{
            binding.shimmeraddress.stopShimmer()
            binding.shimmeraddress.visibility = View.INVISIBLE
        }
    }


    private fun setAddressAdaptor() {

        addressAdaptor = AddressAdaptor( object : onAddressMenuClickListener{
            override fun onEditClicked(address: Address) {
                val action = SavedAddressDirections.actionSavedAddressToUpdateAddress(address,"savedAddress")
                findNavController().navigate(action)
            }

            override fun onsetDefaultClicked(address: Address) {
                saveAddressViewModel.setAddressAsDefault(address)
            }

            override fun onDeleteClicked(address: Address) {
                 AlertDialog.Builder(requireContext())
                    .setTitle("Delete Address?")
                    .setMessage("Are you sure to delete the address")
                    .setPositiveButton("Yes") {dialogue,_ ->
                        saveAddressViewModel.deleteAddress(address)
                    }.setNegativeButton("No") { dialogue,_ ->
                        dialogue.cancel()
                     }
                     .show()
            }

        })

        binding.rcviewaddress.adapter = addressAdaptor
        binding.rcviewaddress.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
    }


    private fun actionToAddAddress() {
        binding.apply {
            actintoaddaddress.setOnClickListener {
                findNavController().navigate(R.id.action_savedAddress_to_addAddress,null,NavOptions.Builder().setPopUpTo(R.id.savedAddress,false).setLaunchSingleTop(true).build())

            }
        }
    }


}