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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.CheckoutListener
import com.example.grocerly.R
import com.example.grocerly.adapters.CartAdaptor
import com.example.grocerly.databinding.FragmentCartBinding
import com.example.grocerly.model.CartProduct
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.CartViewModel
import com.example.grocerly.viewmodel.CheckoutViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class Cart : Fragment() {
    private var cart: FragmentCartBinding? = null
    private val binding get() = cart!!

    private val cartViewModel by activityViewModels<CartViewModel>()

    private val checkoutViewModel: CheckoutViewModel by activityViewModels()

    private lateinit var cartAdaptor: CartAdaptor

    private lateinit var loadingDialogue: LoadingDialogue


    override fun onResume() {
        super.onResume()
        cartViewModel.fetchCartItems()
        cartViewModel.fetchTotalAmountFromCart()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        cart = FragmentCartBinding.inflate(inflater, container, false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerviewCart()
        observeCartItems()
        actionToHome()
        observeDeleteItems()
        actionToCheckout()
        observeEmptyCart()
    }

    private fun observeEmptyCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            cartViewModel.cartUiState.collectLatest {
                if (!it.data?.cartItems.isNullOrEmpty()){
                    enableButton(true)
                    binding.apply {
                        imgviewnoitems.visibility = View.INVISIBLE
                        txtviewnoitems.visibility = View.INVISIBLE
                        materialCardView4.visibility = View.VISIBLE
                    }
                }else{
                    enableButton(false)
                    binding.apply {
                        imgviewnoitems.visibility = View.VISIBLE
                        txtviewnoitems.visibility = View.VISIBLE
                        materialCardView4.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    private fun actionToCheckout() {
        binding.checkoutbtn.setOnClickListener {
            checkoutViewModel.fetchCartItems()
            loadingDialogue.show()
           viewLifecycleOwner.lifecycleScope.launch {
               while (true){
                   val cartItems = checkoutViewModel.cartItems.value.data

                   if (!cartItems.isNullOrEmpty()){
                       loadingDialogue.dismiss()
                       findNavController().navigate(R.id.action_cart_to_checkout,null,NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.cart,false).build())
                       break
                   }
                   delay(1000)
               }

            }
        }
    }



    private fun showFreeDeliverymsg(amount: Float) {
        val minAmountForFreeDelivery = 500
        val remainingAmount = minAmountForFreeDelivery - amount

        if (remainingAmount <=0) {
            binding.txtviewfreedelivery.text = buildString {
                append("🎉 Free Delivery Available!")
            }
        } else {
            binding.txtviewfreedelivery.text = buildString {
                append("You are ")
                append(remainingAmount)
                append("Rs away from free delivery")
            }
        }

        val progress =
            if (amount > minAmountForFreeDelivery) minAmountForFreeDelivery else amount.toInt()
        binding.progressbardelivery.progress = progress
    }

    private fun enableButton(isenabled: Boolean) {
        if (isenabled) {
            binding.checkoutbtn.isEnabled = true
            binding.checkoutbtn.alpha = 1.0f
            binding. progressbardelivery.visibility = View.VISIBLE
            binding. txtviewfreedelivery.visibility = View.VISIBLE
        } else {

            binding.checkoutbtn.isEnabled = false
            binding.checkoutbtn.alpha = 0.5f
            binding. progressbardelivery.visibility = View.INVISIBLE
            binding. txtviewfreedelivery.visibility = View.INVISIBLE
        }
    }

    private fun observeDeleteItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            cartViewModel.deletedItems.collectLatest { deleted ->
                if (deleted is NetworkResult.Error){
                    Toast.makeText(requireContext(),deleted.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actionToHome() {
        binding.backbtn.setOnClickListener {
            findNavController().navigate(R.id.action_cart_to_home,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.home,false).build())
        }
    }

    private fun setRecyclerviewCart() {
        binding.apply {

            cartAdaptor = CartAdaptor(object : CheckoutListener{
                override fun onQuantityChanged(
                    cartProduct: CartProduct
                ) {
                    cartViewModel.updateQuantity(cartProduct)
                }



                override fun onItemDeleted(cartProduct: CartProduct) {
                   cartViewModel.deleteCartItem(cartProduct)
                }

            })
            rcviewcartitems.adapter = cartAdaptor
            rcviewcartitems.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL, false
            )
        }
    }

    private fun observeCartItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            cartViewModel.cartUiState.collectLatest { items ->

                when (items) {
                    is NetworkResult.Error<*> -> {
                        Toast.makeText(requireContext(), items.message, Toast.LENGTH_SHORT).show()
                        showShimmer(false)
                    }

                    is NetworkResult.Loading<*> -> {
                        showShimmer(true)
                    }

                    is NetworkResult.Success<*> -> {
                        showShimmer(false)
                        items.data?.cartItems?.let {
                            cartAdaptor.setCartItems(it)
                        }
                        val amount = items.data?.totalAmount ?: 0f

                        binding.checkoutbtn.text = buildString {
                            append("Go to Checkout(Rs. ")
                            append(amount)
                            append(")")
                        }
                        showFreeDeliverymsg(amount)
                    }

                    is NetworkResult.UnSpecified<*> -> {
                        showShimmer(false)
                    }
                }


            }
        }
    }




    fun showShimmer(istrue: Boolean) {
        if (istrue) {
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