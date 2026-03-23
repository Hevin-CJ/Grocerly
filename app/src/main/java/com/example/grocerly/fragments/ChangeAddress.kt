package com.example.grocerly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.adapters.ChangeAddressAdaptor
import com.example.grocerly.databinding.FragmentChangeAddressBinding
import com.example.grocerly.interfaces.AddressActionListener
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.viewmodel.CheckoutViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChangeAddress(private val listener: AddressActionListener) : BottomSheetDialogFragment() {

    private var changeAddress: FragmentChangeAddressBinding? = null
    private val binding get() = changeAddress!!


    private val checkoutViewModel: CheckoutViewModel by activityViewModels()

    private lateinit var loadingDialogue: LoadingDialogue
    private val changeAddressAdaptor: ChangeAddressAdaptor by lazy { ChangeAddressAdaptor(listener) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        changeAddress = FragmentChangeAddressBinding.inflate(inflater, container, false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAddressAdaptor()
        setActionToAddAddress()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()

        checkoutViewModel.fetchAddress()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                checkoutViewModel.uiState.collectLatest { state ->


                    if (state.isLoading) loadingDialogue.show() else loadingDialogue.dismiss()


                    if (state.savedAddresses.isEmpty() && !state.isLoading) {
                        binding.txtviewerrortype.visibility = View.VISIBLE
                        binding.txtviewerrortype.text = "No Address Found"
                        changeAddressAdaptor.setAddresses(emptyList())
                    } else {
                        binding.txtviewerrortype.visibility = View.GONE
                        changeAddressAdaptor.setAddresses(state.savedAddresses)
                    }
                }
            }
        }
    }

    private fun setAddressAdaptor() {
        binding.recyclerView2.apply {
            adapter = changeAddressAdaptor
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun setActionToAddAddress() {
        binding.txtviewaddnewaddress.setOnClickListener {
            listener.onAddressActionRequested()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        changeAddress = null
    }
}