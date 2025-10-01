package com.example.grocerly.fragments


import android.graphics.Color
import android.graphics.Typeface
import com.example.grocerly.CheckoutListener
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.adapters.CheckoutAdaptor
import com.example.grocerly.databinding.FragmentCheckoutBinding
import com.example.grocerly.interfaces.AddressActionListener
import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.DeliveryCharge
import com.example.grocerly.model.Order
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.CheckoutViewModel
import com.example.grocerly.viewmodel.OrderSharedViewModel
import com.google.api.Context
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.text.get

@AndroidEntryPoint
class Checkout : Fragment() {
    private var checkout: FragmentCheckoutBinding?=null
    private val binding get() = checkout!!

    private val checkoutViewModel: CheckoutViewModel by activityViewModels()

    private val orderSharedViewModel: OrderSharedViewModel by activityViewModels()

    private lateinit var loadingDialogue: LoadingDialogue

    private  lateinit var checkoutAdaptor: CheckoutAdaptor

    private var changeAddressSheet: ChangeAddress? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkout = FragmentCheckoutBinding.inflate(inflater,container,false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setCheckoutAdaptor()
        observeDefaultAddress()
        setToolbarCheckout()
        observeCartItems()
        observeQuantityChange()
        setScrollView()
        setActionToChangeAddress()
        observeNoDefaultState()
        actionToAddDefaultAddress()
        setActionToPayment()
    }

    private fun setActionToPayment() {
        binding.includeBottomBar.btnCheckout.setOnClickListener {
            actionToPaymentFragment()
        }
    }

    private fun actionToPaymentFragment() {
        val currentAddress = checkoutViewModel.defaultAddress.value.data
        val cartItems = checkoutViewModel.cartItems.value.data
        val totalPrice = checkoutViewModel.priceBreakdown.value.data?.get("Total Amount") ?: 0

        if (currentAddress != null && cartItems != null && totalPrice >0) {
            val order = Order(
                orderId = generatePrettyOrderId(),
                address = currentAddress,
                items = cartItems,
                timestamp = System.currentTimeMillis(),
                totalOrderPrice = totalPrice
            )

            orderSharedViewModel.setOrder(order)

            findNavController().navigate(R.id.action_checkout_to_payments,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout,false).build())
        }
    }


    fun generatePrettyOrderId(): String {
        val digits = ('0'..'9').toList()

        val mixedPool = List(100) { digits.random()  }

        fun randomSegment(length: Int): String {
            return (1..length)
                .map { mixedPool.random() }
                .joinToString("")
        }

        val part1 = randomSegment(3)
        val part2 = randomSegment(6)
        val part3 = randomSegment(6)

        return "#$part1-$part2-$part3"
    }

    private fun actionToAddDefaultAddress() {
        binding.addAddressButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("bundlePass","checkout")
            }

            findNavController().navigate(R.id.action_checkout_to_addAddress,bundle, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout,false).build())
        }
    }

    private fun observeNoDefaultState() {
       viewLifecycleOwner.lifecycleScope.launch {
           checkoutViewModel.emptyDefaultAddress.collectLatest {
               if (it){
                   binding.apply {

                       val invisibleList = listOf(
                           binding.txtviewDelivertxt,
                           binding.btnchangeaddress,
                           binding.btnchangeaddress,
                           binding.toolbarsavedaddress,
                           binding.txtviewfulladdress,
                           binding.txtviewphoneno
                       )

                       invisibleList.forEach {
                           it.visibility = View.INVISIBLE
                       }
                       addAddressButton.visibility = View.VISIBLE
                   }
               }else{
                   binding.apply {
                       val invisibleList = listOf(
                           binding.txtviewDelivertxt,
                           binding.btnchangeaddress,
                           binding.btnchangeaddress,
                           binding.toolbarsavedaddress,
                           binding.txtviewfulladdress,
                           binding.txtviewphoneno
                       )

                       invisibleList.forEach {
                           it.visibility = View.VISIBLE
                       }
                       addAddressButton.visibility = View.INVISIBLE

                   }
               }
           }
       }
    }

    private fun setActionToChangeAddress() {
       binding.btnchangeaddress.setOnClickListener {
           changeAddressSheet =  ChangeAddress(object : AddressActionListener{
               override fun onAddressActionRequested() {
                   val bundle = Bundle().apply {
                       putString("bundlePass","checkout")
                   }

                   findNavController().navigate(R.id.action_checkout_to_addAddress,bundle, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout,false).build())
                   changeAddressSheet?.dismiss()
               }

               override fun onDeleteRequested(address: Address) {
                   checkoutViewModel.deleteAddress(address)
                   changeAddressSheet?.dismiss()
               }

               override fun onClickLayoutToMakeDefault(address: Address) {
                   checkoutViewModel.setAsDefault(address)
                   changeAddressSheet?.dismiss()
               }

               override fun onEditRequested(address: Address) {

                   val action = CheckoutDirections.actionCheckoutToUpdateAddress(address,"updateAddress")
                   findNavController().navigate(action)
                   changeAddressSheet?.dismiss()
               }

           })
           changeAddressSheet?.show(parentFragmentManager,"ChangeAddressSheet")
       }
    }

    private fun setScrollView() {
        binding.includeBottomBar.txtviewpricebreak.setOnClickListener {
                   binding.scrollView2.smoothScrollTo(0,binding.scrollView2.getChildAt(0).height)
        }
    }

    private fun observeTotalPriceCalculation() {
        viewLifecycleOwner.lifecycleScope.launch {
            checkoutViewModel.priceBreakdown.collectLatest { map->

                map.data?.let {
                    val totalAmount = map.data.get("Total Amount") ?: 0
                    if (totalAmount>0){
                        setPriceDetailsVisibility(it)
                        setPriceDetailsToUi(it)
                        setPriceToBottomBar(it)
                    }else{
                        findNavController().popBackStack(R.id.cart,false)
                    }
                }

            }
        }
    }

    private fun setPriceToBottomBar(map: Map<String, Int>) {
        binding.includeBottomBar.textTotalPrice.text= map["Total Amount"].toString()
    }

    private fun setPriceDetailsToUi(map: Map<String, Int>) {
        binding.apply {
            map.keys.withIndex().forEach { (index, key) ->
                when(index){
                    0 -> txtviewnormalprice.text = key
                    1 -> txtviewdiscountedprice.text = key
                    2 -> txtviewplatform.text = key
                    3-> txtviewdelivery.text= key
                    4 -> txtviewcoupondiscount.text = key
                    5 -> txtviewtotalpricetxt.text = key
                }
            }

            map.values.withIndex().forEach { (index, key) ->
                when(index){
                    0 -> totalitemprice.text = key.toString()
                    1 -> discountpriceamount.text = key.toString()
                    2 -> platformfeeprice.text = key.toString()
                    3-> convertKeyAndGetDeliveryAmount(key)
                    4 -> appliedcoupons.text = key.toString()
                    5 -> totalamount.text = key.toString()
                }
            }

        }
    }

    private fun convertKeyAndGetDeliveryAmount(key: Int) {

        if (key == 0) {
            binding.deliveryamount.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
         val styled =    SpannableString("Free Delivery").apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(requireContext(),R.color.green)),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            }

            binding.deliveryamount.text = styled
        } else {
            val rupeeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.rupee_indian_wrapped)
            rupeeIcon?.setBounds(0, 0, rupeeIcon.intrinsicWidth, rupeeIcon.intrinsicHeight)
            binding.deliveryamount.setCompoundDrawablesWithIntrinsicBounds(rupeeIcon, null, null, null)
            val styled = SpannableString(key.toString())
            binding.deliveryamount.text = styled
        }
    }

    private fun setPriceDetailsVisibility(map: Map<String, Int>) {
        binding.apply {
          val productDiscountVisibility  = if ((map["Product Discount"] ?: 0) > 0) View.VISIBLE else View.GONE
            val couponVisibility =   if ((map["Applied Coupons"] ?: 0) > 0) View.VISIBLE else View.GONE

            txtviewdiscountedprice.visibility = productDiscountVisibility
            txtviewcoupondiscount.visibility = couponVisibility
            discountpriceamount.visibility = productDiscountVisibility
            appliedcoupons.visibility = couponVisibility

        }
    }


    private fun observeQuantityChange() {
       lifecycleScope.launch {
           checkoutViewModel.updateQuantityState.collectLatest {
               if (it is NetworkResult.Error){
                   Toast.makeText(requireContext(),it.message.toString(), Toast.LENGTH_SHORT).show()
               }
           }
       }
    }

    private fun setCheckoutAdaptor() {

        checkoutAdaptor = CheckoutAdaptor(object : CheckoutListener{
            override fun onQuantityChanged(
                cartProduct: CartProduct
            ) {
                checkoutViewModel.updateQuantity(cartProduct)
            }

            override fun onItemDeleted(cartProduct: CartProduct) {
                checkoutViewModel.deleteCartItem(cartProduct)
            }


        })
        binding.rcviewcartcheckout.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
            adapter = checkoutAdaptor
        }
    }

    private fun observeCartItems() {
       lifecycleScope.launch {
           checkoutViewModel.cartItems.collectLatest {
               when(it){
                   is NetworkResult.Error<*> -> {
                       Toast.makeText(requireContext(),it.message.toString(), Toast.LENGTH_SHORT).show()
                       loadingDialogue.dismiss()
                       findNavController().popBackStack(R.id.cart,true)
                   }
                   is NetworkResult.Loading<*> -> {
                       loadingDialogue.show()
                   }
                   is NetworkResult.Success<*> -> {
                       loadingDialogue.dismiss()
                       it.data?.let {data->
                           checkoutAdaptor.setCartItems(data)
                           checkoutViewModel.calculateTotalPrice(data)
                       }
                   }
                   is NetworkResult.UnSpecified<*> -> {
                       loadingDialogue.dismiss()
                   }
               }
           }
       }
    }

    private fun setToolbarCheckout() {
        binding.toolbarcheckout.apply {
            setTitle(context.getString(R.string.order_summary))
            setNavigationIcon(R.drawable.backarrow)
            setNavigationIconTint(ContextCompat.getColor(requireContext(),R.color.black))
            setNavigationOnClickListener {
                findNavController().navigate(R.id.action_checkout_to_cart,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.cart,true).build())
            }
        }
    }

    private fun observeDefaultAddress() {
        viewLifecycleOwner.lifecycleScope.launch {
            checkoutViewModel.defaultAddress.collectLatest {
                when(it){
                    is NetworkResult.Error<*> -> {
                        Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        loadingDialogue.dismiss()
                        findNavController().navigate(R.id.action_checkout_to_cart, null,NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.cart,true).build())
                    }
                    is NetworkResult.Loading<*> -> {
                        loadingDialogue.show()
                    }
                    is NetworkResult.Success<*> -> {
                        it.data?.let { address->
                            setDefaultAddress(address)
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

    private fun setDefaultAddress(result: Address) {
        binding.apply {
            toolbarsavedaddress.text = result.firstName.uppercase()
            txtviewfulladdress.text = buildString {
                append(result.deliveryAddress)
                append(" , ")
                append(result.city)
                append(" , ")
                append(result.state)
                append(" , ")
                append(result.pinCode)
            }
            txtviewphoneno.text = result.phoneNumber
        }
    }


    override fun onResume() {
        super.onResume()
        checkoutViewModel.getDefaultAddress()
        checkoutViewModel.fetchCartItems()
        observeTotalPriceCalculation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        checkout = null
    }


}