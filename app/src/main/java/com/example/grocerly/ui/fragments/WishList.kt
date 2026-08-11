package com.example.grocerly.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.grocerly.R
import com.example.grocerly.adapters.WishListAdaptor
import com.example.grocerly.databinding.FragmentWishListBinding
import com.example.grocerly.ui.uievents.WishListUiEvents
import com.example.grocerly.utils.GridSpacingItemDecoration
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.Mappers.calculateDynamicSpanCount
import com.example.grocerly.utils.WishListAction
import com.example.grocerly.viewmodel.WishListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class WishList : Fragment(R.layout.fragment_wish_list) {
    private var wishList: FragmentWishListBinding?=null
    private val binding get() = wishList!!

    private val wishListViewModel: WishListViewModel by  viewModels<WishListViewModel>()

    private lateinit var loadingDialogue: LoadingDialogue
    private lateinit var wishListAdaptor: WishListAdaptor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wishList = FragmentWishListBinding.bind(view)

        loadingDialogue = LoadingDialogue(requireContext())

        setToolbar()
        setWishListRecyclerView()
        observeUiStateAndEventState()
    }

    private fun setWishListRecyclerView() {

       val dynamicSpanCount = calculateDynamicSpanCount(120,requireContext())

       binding.rcviewwishlist.apply {
           addItemDecoration(GridSpacingItemDecoration(dynamicSpanCount,30,true))
           wishListAdaptor= WishListAdaptor{ action->
               when(action){
                   is WishListAction.AddItemToCart -> {
                       wishListViewModel.addWishItemToCart(action.wishItem)
                   }
                   is WishListAction.DeleteItemFromWishList -> {
                       wishListViewModel.removeWishItemFromWishList(action.wishItem)
                   }
               }
           }
           adapter = wishListAdaptor
           layoutManager = GridLayoutManager(requireContext(),dynamicSpanCount)
       }
    }

    private fun observeUiStateAndEventState() {
        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    wishListViewModel.uiState.collect {state->

                       if ( state.isLoading){
                           loadingDialogue.show()
                       }else{
                           loadingDialogue.dismiss()
                       }
                        wishListAdaptor.submitList(state.wishList)

                        showEmptyStateWishList(state.wishList.isEmpty())

                        wishListAdaptor.setCartItems(state.cartItems)
                    }
                }

                launch {
                    wishListViewModel.uiEvents.collect { event ->
                        when(event){
                            WishListUiEvents.NavigateToLogin -> {

                            }
                            is WishListUiEvents.ShowMessage -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showEmptyStateWishList(isEmpty: Boolean) {
        if (isEmpty) {
            binding.apply {
                imgviewwishlist.visibility = View.VISIBLE
                txtviewnowishitems.visibility = View.VISIBLE
            }
        } else {
            binding.apply {
                imgviewwishlist.visibility = View.INVISIBLE
                txtviewnowishitems.visibility = View.INVISIBLE
            }
        }
    }

    private fun setToolbar() {
        binding.toolbarwishlist.apply {
            setNavigationIcon(R.drawable.backarrow)
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        wishList = null
    }

}