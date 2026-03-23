package com.example.grocerly.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.CheckoutListener
import com.example.grocerly.R
import com.example.grocerly.adapters.CheckoutAdaptor
import com.example.grocerly.databinding.FragmentCheckoutBinding
import com.example.grocerly.interfaces.AddressActionListener
import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order
import com.example.grocerly.model.uievents.CheckoutUiEvent
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.viewmodel.CheckoutViewModel
import com.example.grocerly.viewmodel.OrderSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Checkout : Fragment() {
    private var checkout: FragmentCheckoutBinding? = null
    private val binding get() = checkout!!

    private val checkoutViewModel: CheckoutViewModel by activityViewModels()
    private val orderSharedViewModel: OrderSharedViewModel by activityViewModels()

    private lateinit var loadingDialogue: LoadingDialogue
    private lateinit var checkoutAdaptor: CheckoutAdaptor
    private var changeAddressSheet: ChangeAddress? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        checkout = FragmentCheckoutBinding.inflate(inflater, container, false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setToolbarCheckout()
        setCheckoutAdaptor()
        setScrollView()
        setActionToChangeAddress()
        actionToAddDefaultAddress()
        setActionToPayment()

        observeUiState()
        observeUiEvents()
        setValidateCoupon()
    }




    private fun setValidateCoupon() {
            binding.validatebtn.setOnClickListener {

                val isCouponApplied = checkoutViewModel.uiState.value.appliedCoupon != null

                if (isCouponApplied) {
                    checkoutViewModel.removeCoupon()
                    binding.edttxtcoupon.text?.clear()
                } else {

                    val couponCode = binding.edttxtcoupon.text.toString().trim()
                    if (couponCode.isNotEmpty()) {
                        checkoutViewModel.applyCoupon(couponCode)
                    } else {
                        binding.txtinputlyttxtcoupon.helperText = "Please enter a valid coupon code"
                    }
                }
            }

    }

    override fun onResume() {
        super.onResume()
        checkoutViewModel.fetchCartItems()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                checkoutViewModel.uiState.collectLatest { state ->


                    if (state.isLoading) loadingDialogue.show() else loadingDialogue.dismiss()

                    if (state.isCouponError){
                        binding.txtinputlyttxtcoupon.setHelperTextColor(ColorStateList.valueOf(Color.RED))
                        binding.txtinputlyttxtcoupon.helperText = state.couponMessage
                    }else{
                        binding.txtinputlyttxtcoupon.helperText = state.couponMessage
                        binding.txtinputlyttxtcoupon.setHelperTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.light_green)))
                    }

                    if (state.appliedCoupon != null) {
                        binding.apply {
                            validatebtn.text = "Remove"
                            validatebtn.setBackgroundColor(Color.RED)


                            edttxtcoupon.isFocusable = false
                            edttxtcoupon.isFocusableInTouchMode = false
                            edttxtcoupon.isCursorVisible = false
                        }
                    } else {
                        binding.apply {
                            validatebtn.text = "Apply"
                            validatebtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))


                            edttxtcoupon.isFocusable = true
                            edttxtcoupon.isFocusableInTouchMode = true
                            edttxtcoupon.isCursorVisible = true
                        }
                    }

                    if (state.isDefaultAddressEmpty) {
                        setNoAddressUiVisibility()
                    } else {
                        state.defaultAddress?.let {
                            setDefaultAddress(it)
                            setAddressUiVisibility()
                        }
                    }

                    checkoutAdaptor.setCartItems(state.cartItems)

                    if (state.priceBreakdown.isNotEmpty()) {
                        val totalAmount = state.priceBreakdown["Total Amount"] ?: 0
                        if (totalAmount > 0) {
                            setPriceDetailsVisibility(state.priceBreakdown)
                            setPriceDetailsToUi(state.priceBreakdown)
                            binding.includeBottomBar.textTotalPrice.text = totalAmount.toString()
                        } else {
                            findNavController().popBackStack(R.id.cart, false)
                        }
                    }


                    state.errorMessage?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun observeUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                checkoutViewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is CheckoutUiEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                        is CheckoutUiEvent.ItemDeletedSuccess -> {

                        }
                        is CheckoutUiEvent.AddressUpdatedSuccess -> {

                        }
                    }
                }
            }
        }
    }

    private fun setActionToPayment() {
        binding.includeBottomBar.btnCheckout.setOnClickListener {
            val state = checkoutViewModel.uiState.value
            val currentAddress = state.defaultAddress
            val cartItems = state.cartItems
            val totalPrice = state.priceBreakdown["Total Amount"] ?: 0

            if (currentAddress != null && cartItems.isNotEmpty() && totalPrice > 0) {
                val order = Order(
                    orderId = generatePrettyOrderId(),
                    address = currentAddress,
                    items = cartItems,
                    timestamp = System.currentTimeMillis(),
                    totalOrderPrice = totalPrice
                )

                orderSharedViewModel.setOrder(order)
                findNavController().navigate(
                    R.id.action_checkout_to_payments, null,
                    NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout, false).build()
                )
            } else {
                Toast.makeText(requireContext(), "Please enter details to continue", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setPriceDetailsToUi(map: Map<String, Int>) {

        val entries = map.entries.toList()

        binding.apply {
            if (entries.isNotEmpty()) { txtviewnormalprice.text = entries[0].key; totalitemprice.text = entries[0].value.toString() }
            if (entries.size > 1) { txtviewdiscountedprice.text = entries[1].key; discountpriceamount.text = entries[1].value.toString() }
            if (entries.size > 2) { txtviewplatform.text = entries[2].key; platformfeeprice.text = entries[2].value.toString() }
            if (entries.size > 3) { txtviewdelivery.text = entries[3].key; convertKeyAndGetDeliveryAmount(entries[3].value) }
            if (entries.size > 4) { txtviewcoupondiscount.text = entries[4].key; appliedcoupons.text = entries[4].value.toString() }
            if (entries.size > 5) { txtviewtotalpricetxt.text = entries[5].key; totalamount.text = entries[5].value.toString() }
        }
    }

    private fun setPriceDetailsVisibility(map: Map<String, Int>) {
        binding.apply {
            val productDiscountVisibility = if ((map["Product Discount"] ?: 0) > 0) View.VISIBLE else View.GONE
            val couponVisibility = if ((map["Applied Coupons"] ?: 0) > 0) View.VISIBLE else View.GONE

            txtviewdiscountedprice.visibility = productDiscountVisibility
            txtviewcoupondiscount.visibility = couponVisibility
            discountpriceamount.visibility = productDiscountVisibility
            appliedcoupons.visibility = couponVisibility
        }
    }

    private fun convertKeyAndGetDeliveryAmount(key: Int) {
        if (key == 0) {
            binding.deliveryamount.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            val styled = SpannableString("Free Delivery").apply {
                setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.green)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            binding.deliveryamount.text = styled
        } else {
            val rupeeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.rupee_indian_wrapped)
            rupeeIcon?.setBounds(0, 0, rupeeIcon.intrinsicWidth, rupeeIcon.intrinsicHeight)
            binding.deliveryamount.setCompoundDrawablesWithIntrinsicBounds(rupeeIcon, null, null, null)
            binding.deliveryamount.text = SpannableString(key.toString())
        }
    }

    private fun setNoAddressUiVisibility() {
        binding.apply {
            val invisibleList = listOf(
                txtviewDelivertxt, btnchangeaddress, toolbarsavedaddress,
                txtviewfulladdress, txtviewphoneno
            )
            invisibleList.forEach { it.visibility = View.INVISIBLE }
            addAddressButton.visibility = View.VISIBLE
        }
    }

    private fun setAddressUiVisibility() {
        binding.apply {
            val visibleList = listOf(
                txtviewDelivertxt, btnchangeaddress, toolbarsavedaddress,
                txtviewfulladdress, txtviewphoneno
            )
            visibleList.forEach { it.visibility = View.VISIBLE }
            addAddressButton.visibility = View.INVISIBLE
        }
    }

    private fun setDefaultAddress(result: Address) {
        binding.apply {
            toolbarsavedaddress.text = result.firstName.uppercase()
            txtviewfulladdress.text = buildString {
                append(result.deliveryAddress).append(" , ")
                append(result.city).append(" , ")
                append(result.state).append(" , ")
                append(result.pinCode)
            }
            txtviewphoneno.text = result.phoneNumber
        }
    }

    private fun setActionToChangeAddress() {
        binding.btnchangeaddress.setOnClickListener {
            changeAddressSheet = ChangeAddress(object : AddressActionListener {
                override fun onAddressActionRequested() {
                    val bundle = Bundle().apply { putString("bundlePass", "checkout") }
                    findNavController().navigate(
                        R.id.action_checkout_to_addAddress, bundle,
                        NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout, false).build()
                    )
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
                    val action = CheckoutDirections.actionCheckoutToUpdateAddress(address, "updateAddress")
                    findNavController().navigate(action)
                    changeAddressSheet?.dismiss()
                }
            })
            changeAddressSheet?.show(parentFragmentManager, "ChangeAddressSheet")
        }
    }

    private fun actionToAddDefaultAddress() {
        binding.addAddressButton.setOnClickListener {
            val bundle = Bundle().apply { putString("bundlePass", "checkout") }
            findNavController().navigate(
                R.id.action_checkout_to_addAddress, bundle,
                NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.checkout, false).build()
            )
        }
    }

    private fun setCheckoutAdaptor() {
        checkoutAdaptor = CheckoutAdaptor(object : CheckoutListener {
            override fun onQuantityChanged(cartProduct: CartProduct) = checkoutViewModel.updateQuantity(cartProduct)
            override fun onItemDeleted(cartProduct: CartProduct) = checkoutViewModel.deleteCartItem(cartProduct)
        })
        binding.rcviewcartcheckout.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = checkoutAdaptor
        }
    }

    private fun generatePrettyOrderId(): String {
        val digits = ('0'..'9').toList()
        val mixedPool = List(100) { digits.random() }
        fun randomSegment(length: Int) = (1..length).map { mixedPool.random() }.joinToString("")
        return "#${randomSegment(3)}-${randomSegment(6)}-${randomSegment(6)}"
    }

    private fun setScrollView() {
        binding.includeBottomBar.txtviewpricebreak.setOnClickListener {
            binding.scrollView2.smoothScrollTo(0, binding.scrollView2.getChildAt(0).height)
        }
    }

    private fun setToolbarCheckout() {
        binding.toolbarcheckout.apply {
            setTitle(context.getString(R.string.order_summary))
            setNavigationIcon(R.drawable.backarrow)
            setNavigationIconTint(ContextCompat.getColor(requireContext(), R.color.black))
            setNavigationOnClickListener {
                findNavController().navigate(
                    R.id.action_checkout_to_cart, null,
                    NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.cart, true).build()
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        checkout = null
        changeAddressSheet = null
    }
}