package com.example.grocerly.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.grocerly.adapters.SearchAdaptor
import com.example.grocerly.databinding.FragmentSearchBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.utils.GridSpacingItemDecoration
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.CartViewModel
import com.example.grocerly.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class Search : Fragment() {
    private var search: FragmentSearchBinding?=null
    private val binding get() = search!!

    private val searchViewModel by viewModels<SearchViewModel>()

    private val cartViewModel by activityViewModels<CartViewModel>()

    private lateinit var searchAdaptor: SearchAdaptor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        search = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDefaultView()
        enableSearchView()
        collectSearchFromFirebase()
        setSearchAdaptor()
        observeAddToCartItems()
        setCancelSearch()
        setupPredefinedSearchButtons()
        setCartItems()
        setFavouriteItems()
    }

    private fun setFavouriteItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.favouriteItems.collectLatest {
                if (it is NetworkResult.Success){
                    searchAdaptor.setFavouriteItems(it.data ?: emptyList())
                }

            }
        }
    }

    private fun setCartItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.cartItems.collectLatest {
                if (it is NetworkResult.Success){
                    searchAdaptor.setCartItems(it.data ?: emptyList())
                }

            }
        }
    }

    private fun setDefaultView() {
        binding.apply {
            rcviewsearchadaptor.visibility = View.INVISIBLE
            srchview.setQuery("",true)
            cancelbtn.visibility = View.INVISIBLE
        }
    }

    private fun setupPredefinedSearchButtons() {
        binding.apply {
            val buttons = listOf(
                binding.btnapple,
                binding.btnorange,
                binding.btnDetergent,
                binding.btnSoaps,
                binding.btnFruits,
                binding.btnBanana,
                binding.btnEgg,
                binding.btnGrapes
            )
            buttons.forEach { button ->

                button.setOnClickListener {
                    val query = button.text.toString()
                    binding.srchview.setQuery(query, true)
                    searchViewModel.searchItemsInFirebase(query.lowercase(Locale.ROOT))
                }
            }
        }
    }

    private fun setCancelSearch() {
        binding.apply {
            cancelbtn.setOnClickListener {
                rcviewsearchadaptor.visibility = View.INVISIBLE
                srchview.setQuery("",true)
                cancelbtn.visibility = View.INVISIBLE
            }
        }
    }

    private fun observeAddToCartItems() {
       viewLifecycleOwner.lifecycleScope.launch {
           cartViewModel.addedCartItems.collectLatest{ result ->
               if (result is NetworkResult.Error){
                   Toast.makeText(requireContext(),result.message.toString(), Toast.LENGTH_SHORT).show()
               }
           }
       }
    }

    private fun setSearchAdaptor() {
        binding.apply {
            val spanCount = 3

            val spacingInDp = 12
            val spacingInPixels = (spacingInDp * resources.displayMetrics.density).toInt()

            searchAdaptor = SearchAdaptor(object : ChildCategoryListener {
                override fun addProductToCart(cartProduct: CartProduct) {
                    cartViewModel.addProductIntoCartFirebase(cartProduct)
                }

                override fun addProductToFavourites(favouriteItem: FavouriteItem) {
                   searchViewModel.addFavouriteToFirebase(favouriteItem)
                }

            })


            rcviewsearchadaptor.adapter = searchAdaptor
            rcviewsearchadaptor.layoutManager = GridLayoutManager(requireContext(), spanCount)

            if (rcviewsearchadaptor.itemDecorationCount == 0) {
                rcviewsearchadaptor.addItemDecoration(
                    GridSpacingItemDecoration(spanCount, spacingInPixels, true)
                )
            }
        }
    }

    private fun collectSearchFromFirebase() {
        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.searchItem.collectLatest { values->
                when(values){
                    is NetworkResult.Error<*> -> {
                        Toast.makeText(requireContext(),values.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading<*> -> {

                    }
                    is NetworkResult.Success<*> -> {
                        val products = values.data ?: emptyList()
                        searchAdaptor.setProducts(products)
                        showNoOfferItemsFound(products.isNullOrEmpty())
                        Log.d("searchvalues",products.toString())
                    }
                    is NetworkResult.UnSpecified<*> -> {

                    }
                }
            }
        }
    }

    private fun showNoOfferItemsFound(bool: Boolean) {
        if (bool){
            binding.apply {
                txtviewnoitems.visibility = View.VISIBLE
                cancelbtn.visibility = View.GONE
                rcviewsearchadaptor.visibility = View.VISIBLE
            }
        }else{
            binding.apply {
                txtviewnoitems.visibility = View.INVISIBLE
                cancelbtn.visibility = View.VISIBLE
                rcviewsearchadaptor.visibility = View.VISIBLE
            }
        }
    }


    private fun enableSearchView() {
        binding.srchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {

                    searchViewModel.searchItemsInFirebase(newText)
                    return true
            }

        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        search=null
    }

}