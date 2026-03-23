package com.example.grocerly.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import com.example.grocerly.adapters.CartAdaptor
import com.example.grocerly.databinding.FragmentCartBinding
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.uievents.CartUiEvents
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.CartViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Cart : Fragment(R.layout.fragment_cart) {

    private var cart: FragmentCartBinding? = null
    private val binding get() = cart!!

    private val cartViewModel by activityViewModels<CartViewModel>()
    private lateinit var cartAdaptor: CartAdaptor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cart = FragmentCartBinding.bind(view)

        setRecyclerviewCart()
        actionToHome()
        actionToCheckout()
        observeUiStateAndEvents()
    }

    private fun observeUiStateAndEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    cartViewModel.cartUiState.collectLatest { result ->
                        when (result) {
                            is NetworkResult.Loading -> {
                                showShimmer(true)
                            }
                            is NetworkResult.Error -> {
                                showShimmer(false)
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                            }
                            is NetworkResult.Success -> {
                                showShimmer(false)
                                val items = result.data?.cartItems ?: emptyList()

                                val amount = result.data?.totalAmount ?: 0f

                                cartAdaptor.setCartItems(items)
                                handleEmptyState(items.isEmpty())

                                binding.checkoutbtn.text = "Go to Checkout(Rs. $amount)"
                                showFreeDeliverymsg(amount)

                            }
                            else -> showShimmer(false)
                        }
                    }
                }

                launch {
                    cartViewModel.cartUiEvents.collectLatest { event ->
                        when (event) {
                            is CartUiEvents.ShowMessage -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            enableButton(false)
            binding.apply {
                imgviewnoitems.visibility = View.VISIBLE
                txtviewnoitems.visibility = View.VISIBLE
                materialCardView4.visibility = View.INVISIBLE
            }
        } else {
            enableButton(true)
            binding.apply {
                imgviewnoitems.visibility = View.INVISIBLE
                txtviewnoitems.visibility = View.INVISIBLE
                materialCardView4.visibility = View.VISIBLE
            }
        }
    }

    private fun actionToCheckout() {
        binding.checkoutbtn.setOnClickListener {
            val currentState = cartViewModel.cartUiState.value


            val hasItems = currentState is NetworkResult.Success && !currentState.data?.cartItems.isNullOrEmpty()

            if (hasItems) {

                findNavController().navigate(
                    R.id.action_cart_to_checkout,
                    null,
                    NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.cart, false)
                        .build()
                )
            } else {

                Toast.makeText(requireContext(), "Empty Cart", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFreeDeliverymsg(amount: Float) {
        val minAmountForFreeDelivery = 500
        val remainingAmount = minAmountForFreeDelivery - amount

        if (remainingAmount <= 0) {
            binding.txtviewfreedelivery.text = "🎉 Free Delivery Available!"
            binding.progressbardelivery.progress = minAmountForFreeDelivery
        } else {
            binding.txtviewfreedelivery.text = "You are ${remainingAmount.toInt()}Rs away from free delivery"
            binding.progressbardelivery.progress = amount.toInt()
        }
    }

    private fun enableButton(isEnabled: Boolean) {
        binding.checkoutbtn.isEnabled = isEnabled
        binding.checkoutbtn.alpha = if (isEnabled) 1.0f else 0.5f

        val visibility = if (isEnabled) View.VISIBLE else View.INVISIBLE
        binding.progressbardelivery.visibility = visibility
        binding.txtviewfreedelivery.visibility = visibility
    }

    private fun actionToHome() {
        binding.backbtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_cart_to_home,
                null,
                NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.home, false).build()
            )
        }
    }

    private fun setRecyclerviewCart() {
        cartAdaptor = CartAdaptor(object : CheckoutListener {
            override fun onQuantityChanged(cartProduct: CartProduct) {
                cartViewModel.updateQuantity(cartProduct)
            }
            override fun onItemDeleted(cartProduct: CartProduct) {
                cartViewModel.deleteCartItem(cartProduct)
            }
        })
        binding.rcviewcartitems.apply {
            adapter = cartAdaptor
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showShimmer(isVisible: Boolean) {
        if (isVisible) {
            binding.shimmerlayout.startShimmer()
            binding.shimmerlayout.visibility = View.VISIBLE
        } else {
            binding.shimmerlayout.stopShimmer()
            binding.shimmerlayout.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cart = null
    }
}