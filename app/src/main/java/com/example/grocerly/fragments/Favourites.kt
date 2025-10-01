package com.example.grocerly.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.adapters.FavouriteAdaptor
import com.example.grocerly.databinding.FragmentFavouritesBinding
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.CartViewModel
import com.example.grocerly.viewmodel.FavouriteViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Favourites : Fragment() {
   private var favourites: FragmentFavouritesBinding?=null
    private val binding get() = favourites!!

    private val favouriteViewModel  by activityViewModels<FavouriteViewModel>()
    private val cartViewModel  by activityViewModels<CartViewModel>()

    private val favouriteAdaptor by lazy { FavouriteAdaptor(favouriteViewModel,cartViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favouriteViewModel.getAllFavouritesFromFirebase()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        favourites = FragmentFavouritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeFavouriteListState()
        setRcViewFavourites()
        collectAddToCartState()
    }

    private fun collectAddToCartState() {
        viewLifecycleOwner.lifecycleScope.launch {
            cartViewModel.addedCartItems.collectLatest { state->
                when(state){
                    is NetworkResult.Error<*> -> {
                        Toast.makeText(requireContext(),state.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading<*> -> {

                    }
                    is NetworkResult.Success<*> -> {
                        Toast.makeText(requireContext(),"Added To Cart", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.UnSpecified<*> -> {

                    }
                }
            }
        }
    }

    private fun setRcViewFavourites() {
        binding.apply {
            rcviewfavourites.adapter = favouriteAdaptor
            rcviewfavourites.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
        }
    }

    private fun observeFavouriteListState() {
        viewLifecycleOwner.lifecycleScope.launch {
            favouriteViewModel.favouritesList.collectLatest { favourites->
                when(favourites){
                    is NetworkResult.Error<*> ->{
                        Toast.makeText(requireContext(),favourites.message, Toast.LENGTH_SHORT).show()
                        showNoFavourites(false)
                    }
                    is NetworkResult.Loading<*> -> {

                    }
                    is NetworkResult.Success<*> -> {
                        favourites.data?.let {
                            favouriteAdaptor.saveFavourites(it)
                            showNoFavourites(it.isNotEmpty())
                            Log.d("favouritevalues",it.toString())
                        }


                    }
                    is NetworkResult.UnSpecified<*> -> {

                    }
                }
            }
        }
    }

    private fun showNoFavourites(isTrue: Boolean) {
        if (isTrue){
            binding.txtviewnofavourites.visibility = View.INVISIBLE
            binding.imgviewnofavourites.visibility = View.INVISIBLE
        }else{
            binding.txtviewnofavourites.visibility = View.VISIBLE
            binding.imgviewnofavourites.visibility = View.VISIBLE
        }
    }


}