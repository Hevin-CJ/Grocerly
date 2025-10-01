    package com.example.grocerly.fragments

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
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavOptions
    import androidx.navigation.fragment.findNavController
    import androidx.recyclerview.widget.LinearLayoutManager
    import com.example.grocerly.R
    import com.example.grocerly.activity.MainActivity
    import com.example.grocerly.adapters.PaymentAdaptor
    import com.example.grocerly.databinding.FragmentPaymentsBinding
    import com.example.grocerly.interfaces.PaymentListener
    import com.example.grocerly.model.Order
    import com.example.grocerly.utils.LoadingDialogue
    import com.example.grocerly.utils.NetworkResult
    import com.example.grocerly.utils.PaymentMethodItem
    import com.example.grocerly.viewmodel.OrderSharedViewModel
    import com.example.grocerly.viewmodel.PaymentViewModel
    import com.razorpay.Checkout
    import com.razorpay.Checkout.preload
    import com.razorpay.ExternalWalletListener
    import com.razorpay.PaymentData
    import com.razorpay.PaymentResultListener
    import com.razorpay.PaymentResultWithDataListener
    import dagger.hilt.android.AndroidEntryPoint
    import kotlinx.coroutines.flow.collectLatest
    import kotlinx.coroutines.launch
    import okhttp3.internal.wait
    import org.json.JSONObject

    @AndroidEntryPoint
    class Payments : Fragment(), PaymentResultListener{

        private var payments: FragmentPaymentsBinding?=null
        private val binding get() = payments!!

        private val orderSharedViewModel: OrderSharedViewModel by activityViewModels()

        private val paymentViewModel: PaymentViewModel by viewModels()

        private lateinit var adaptor: PaymentAdaptor

        private lateinit var loadingDialogue: LoadingDialogue

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            Checkout.preload(requireContext().applicationContext)

        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
           payments = FragmentPaymentsBinding.inflate(inflater,container,false)
            loadingDialogue = LoadingDialogue(requireContext())
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setPaymentToolbar()
            fetchTotalAmountToTextView()
            setPaymentAdaptor()
            loadPaymentHeaders()
            observeSavedCards()
            observeCardPayment()
            observePaymentConfirmation()
        }


        private fun initPayment(order: Order){
            val activity = requireActivity()
            val co = Checkout()
            co.setKeyID("rzp_test_RLLJ7soNtz9APa")

            try {
                val options = JSONObject()
                options.put("name","Grocerly")
                options.put("description","Delivering The Best Groceries")
                options.put("image","http://example.com/image/rzp.jpg")
                options.put("theme.color", "#0CA201");
                options.put("currency","INR");
                options.put("amount",(order.totalOrderPrice * 100).toInt())

                val retryObj =  JSONObject();
                retryObj.put("enabled", true);
                retryObj.put("max_count", 4);
                options.put("retry", retryObj);



                val prefill = JSONObject()
                prefill.put("contact",order.address.phoneNumber)
                options.put("prefill",prefill)
                co.open(activity,options)
            }catch (e: Exception){
                Toast.makeText(activity,"Error in payment: "+ e.message,Toast.LENGTH_LONG).show()
                e.printStackTrace()
                Log.d("paymentissue",e.message.toString())
            }

        }

        private fun observePaymentConfirmation() {
            viewLifecycleOwner.lifecycleScope.launch {
                paymentViewModel.confirmOrderState.collectLatest {
                    when(it){
                        is NetworkResult.Error<*> -> {
                            if (!it.message.isNullOrEmpty()){
                                Toast.makeText(requireContext(),it.message, Toast.LENGTH_SHORT).show()
                            }
                            loadingDialogue.dismiss()
                        }
                        is NetworkResult.Loading<*> -> {
                            loadingDialogue.show()
                            loadingDialogue.setText("This may take little longer, please wait...")
                        }
                        is NetworkResult.Success<*> ->{
                            loadingDialogue.dismiss()
                            orderSharedViewModel.clearOrder()
                            findNavController().navigate(R.id.action_payments_to_orderPlaced)
                        }
                        is NetworkResult.UnSpecified<*> -> {
                            loadingDialogue.dismiss()
                        }
                    }
                }
            }
        }

        private fun observeCardPayment() {
            viewLifecycleOwner.lifecycleScope.launch {
                paymentViewModel.cvvState.collectLatest {
                    if (it is NetworkResult.Success){
                        confirmOrder("Card")
                    }
                }
            }
        }

        private fun confirmOrder(paymentType: String) {
            viewLifecycleOwner.lifecycleScope.launch {
                orderSharedViewModel.currentOrder.collectLatest {
                   it?.let { order ->
                       paymentViewModel.setOrderInDb(paymentType,order)
                   }
                }
            }
        }

        private fun observeSavedCards() {
           viewLifecycleOwner.lifecycleScope.launch {
               paymentViewModel.savedCards.collectLatest {
                   when(it){
                       is NetworkResult.Error<*> -> {
                           Toast.makeText(requireContext(),it.message, Toast.LENGTH_SHORT).show()
                           loadingDialogue.dismiss()
                       }
                       is NetworkResult.Loading<*> -> {
                           loadingDialogue.show()
                       }
                       is NetworkResult.Success<*> -> {
                           it.data?.let {cards->
                               adaptor.setCard(cards)
                           }
                          loadingDialogue.dismiss()
                       }
                       is NetworkResult.UnSpecified<*> ->{
                           loadingDialogue.dismiss()
                       }
                   }
               }
           }
        }

        override fun onResume() {
            super.onResume()
            paymentViewModel.fetchSavedCards()
            paymentViewModel.fetchHeaders()
        }

        private fun loadPaymentHeaders() {
            viewLifecycleOwner.lifecycleScope.launch {
                paymentViewModel.savedPaymentHeader.collectLatest {
                    if (it is NetworkResult.Success || it is NetworkResult.Error){
                        it.data?.let {headers ->
                            Log.d("headers",headers.toString())
                            adaptor.setPaymentMethod(headers)
                        }

                        it.message?.let {
                            Toast.makeText(requireContext(),it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        }

        private fun setPaymentAdaptor() {
            adaptor = PaymentAdaptor( object : PaymentListener{
                override fun onCvvCheckListener(
                    cardId: String,
                    cvv: String,
                    onResult: (String) -> Unit
                ) {
                    paymentViewModel.checkCvvForPaymentDb(cardId,cvv)
                    lifecycleScope.launch {
                        paymentViewModel.cvvState.collectLatest {
                            if (it is NetworkResult.Error){
                                onResult(it.message.toString())
                            }else{
                                onResult("")
                            }
                        }
                    }
                }


                override fun onUpiListener(upi: String) {
                    if (upi.isNotBlank()){
                        val order = orderSharedViewModel.currentOrder.value
                        order?.let {
                            initPayment(it)
                        }
                    }
                }

                override fun onCodListener(isSet: Boolean) {

                }

            })

            binding.rcviewpayments.adapter = adaptor
            binding.rcviewpayments.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
        }

        private fun fetchTotalAmountToTextView() {
            binding.txtviewtotal.text = orderSharedViewModel.currentOrder.value?.totalOrderPrice.toString()
        }

        private fun setPaymentToolbar() {
            binding.paymenttoolbar.apply {
               setTitle("Payments")
                setNavigationIcon(R.drawable.backarrow)
                setNavigationOnClickListener {
                    findNavController().navigate(R.id.action_payments_to_checkout,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout,false).build())
                }
            }
        }


        override fun onDestroyView() {
            super.onDestroyView()
            payments = null
        }


        override fun onPaymentSuccess(p0: String?) {
            Toast.makeText(requireContext(),"Payment Success",Toast.LENGTH_SHORT).show()
            confirmOrder("Upi")
        }

        override fun onPaymentError(p0: Int, p1: String?) {
            Toast.makeText(requireContext(),"Payment Failed:${p1.toString()}",Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }


    }