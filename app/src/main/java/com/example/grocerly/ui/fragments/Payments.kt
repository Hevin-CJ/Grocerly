package com.example.grocerly.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.adapters.PaymentAdaptor
import com.example.grocerly.databinding.FragmentPaymentsBinding
import com.example.grocerly.interfaces.PaymentListener
import com.example.grocerly.model.uievents.PaymentUiEvent
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.OrderSharedViewModel
import com.example.grocerly.viewmodel.PaymentViewModel
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Payments : Fragment(), PaymentResultListener {

    private var payments: FragmentPaymentsBinding? = null
    private val binding get() = payments!!

    private val orderSharedViewModel: OrderSharedViewModel by activityViewModels()
    private val paymentViewModel: PaymentViewModel by viewModels()

    private lateinit var adaptor: PaymentAdaptor
    private lateinit var loadingDialogue: LoadingDialogue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checkout.preload(requireContext().applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        payments = FragmentPaymentsBinding.inflate(inflater, container, false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPaymentToolbar()
        fetchTotalAmountToTextView()
        setPaymentAdaptor()

        observeSavedCards()
        loadPaymentHeaders()
        observePaymentConfirmation()
        observeUiEvents()
    }

    override fun onResume() {
        super.onResume()
        paymentViewModel.fetchSavedCards()
        paymentViewModel.fetchHeaders()
    }

    private fun observeUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            paymentViewModel.paymentEvent.collectLatest { event ->
                when (event) {
                    is PaymentUiEvent.ShowMessage -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                    }
                    is PaymentUiEvent.LaunchRazorpay -> {
                        val co = Checkout()
                        co.setKeyID("rzp_test_RLLJ7soNtz9APa")
                        try {
                            co.open(requireActivity(), event.options)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    is PaymentUiEvent.NavigateToOrderPlaced -> {
                        orderSharedViewModel.clearOrder()
                        findNavController().navigate(R.id.action_payments_to_orderPlaced)
                    }
                }
            }
        }
    }

    private fun observePaymentConfirmation() {
        viewLifecycleOwner.lifecycleScope.launch {
            paymentViewModel.confirmOrderState.collectLatest { state ->
                when (state) {
                    is NetworkResult.Loading -> {
                        loadingDialogue.show()
                        loadingDialogue.setText("This may take a little longer, please wait...")
                    }
                    else -> {
                        loadingDialogue.dismiss()
                    }
                }
            }
        }
    }

    private fun observeSavedCards() {
        viewLifecycleOwner.lifecycleScope.launch {
            paymentViewModel.savedCards.collectLatest { result ->
                when (result) {
                    is NetworkResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        loadingDialogue.dismiss()
                    }
                    is NetworkResult.Loading -> loadingDialogue.show()
                    is NetworkResult.Success -> {
                        result.data?.let { cards -> adaptor.setCard(cards) }
                        loadingDialogue.dismiss()
                    }
                    else -> loadingDialogue.dismiss()
                }
            }
        }
    }

    private fun loadPaymentHeaders() {
        viewLifecycleOwner.lifecycleScope.launch {
            paymentViewModel.savedPaymentHeader.collectLatest { result ->
                if (result is NetworkResult.Success || result is NetworkResult.Error) {
                    result.data?.let { headers -> adaptor.setPaymentMethod(headers) }
                    result.message?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private fun setPaymentAdaptor() {
        adaptor = PaymentAdaptor(object : PaymentListener {
            override fun onCvvCheckListener(cardId: String, cvv: String, onResult: (String) -> Unit) {
                val currentOrder = orderSharedViewModel.currentOrder.value
                if (currentOrder != null) {
                    paymentViewModel.processCardPayment(cardId, cvv, currentOrder, onResult)
                } else {
                    onResult("Failed to retrieve order data")
                }
            }

            override fun onUpiListener(upi: String) {
                orderSharedViewModel.currentOrder.value?.let { order ->
                    paymentViewModel.prepareUpiPayment(order, upi)
                }
            }

            override fun onCodListener(isSet: Boolean) {
                if (isSet){
                    binding.confirmpaymentbtn.visibility = View.VISIBLE


                    binding.confirmpaymentbtn.setOnClickListener {

                        orderSharedViewModel.currentOrder.value?.let { order ->
                            paymentViewModel.confirmOrder("COD", order)
                        } ?: Toast.makeText(requireContext(), "Order data missing", Toast.LENGTH_SHORT).show()
                    }

                }else{
                    binding.confirmpaymentbtn.visibility = View.GONE
                }
            }

            override fun onAddCardClicked() {
                val action = PaymentsDirections.actionPaymentsToUpsertCard(null, false)
                findNavController().navigate(action)
            }
        })

        binding.rcviewpayments.adapter = adaptor
        binding.rcviewpayments.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    private fun fetchTotalAmountToTextView() {
        binding.txtviewtotal.text = orderSharedViewModel.currentOrder.value?.totalOrderPrice.toString()
    }

    private fun setPaymentToolbar() {
        binding.paymenttoolbar.apply {
            setTitle("Payments")
            setNavigationIcon(R.drawable.backarrow)
            setNavigationOnClickListener {
                findNavController().navigate(R.id.action_payments_to_checkout, null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout, false).build())
            }
        }
    }

    override fun onPaymentSuccess(p0: String?) {
        Toast.makeText(requireContext(), "Payment Success", Toast.LENGTH_SHORT).show()
        orderSharedViewModel.currentOrder.value?.let { order ->
            paymentViewModel.confirmOrder("UPI", order)
        }
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        Toast.makeText(requireContext(), "Payment Failed: $p1", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        payments = null
    }
}