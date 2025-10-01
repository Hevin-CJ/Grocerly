package com.example.grocerly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.adapters.SearchAdaptor
import com.example.grocerly.databinding.FragmentCustomSearchViewBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.utils.GridSpacingItemDecoration
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.CustomSearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CustomSearchView : Fragment() {

    private var customSearchView: FragmentCustomSearchViewBinding?=null
    private val binding get() = customSearchView!!

    private val categoryArgs by navArgs<CustomSearchViewArgs>()

    private lateinit var searchAdaptor: SearchAdaptor

    private val customSearchViewModel: CustomSearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        customSearchView = FragmentCustomSearchViewBinding.inflate(inflater,container,false)
        (requireActivity() as MainActivity).setTabLayoutVisibility(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showSearchedData(categoryArgs)
        setupCustomSearchAdaptor()
        observeSearchedCategoryData()
        observeCustomSearchData()
        setCartItems()
        setFavouriteItems()
        setSearchListener()
        setUpBackBtn()
    }

    private fun setUpBackBtn() {
        binding.backbtncustomsrchview.setOnClickListener {
            findNavController().popBackStack(destinationId = R.id.home, inclusive = false)
        }
    }

    private fun observeCustomSearchData() {
        viewLifecycleOwner.lifecycleScope.launch {
            customSearchViewModel.searchItem.collectLatest {
                if (it is NetworkResult.Success){
                    searchAdaptor.setProducts(it.data ?: emptyList())
                }
            }
        }
    }

    private fun setFavouriteItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            customSearchViewModel.favouriteItems.collectLatest {
                if (it is NetworkResult.Success){
                    searchAdaptor.setFavouriteItems(it.data ?: emptyList())
                }

            }
        }
    }

    private fun setCartItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            customSearchViewModel.cartItems.collectLatest {
                if (it is NetworkResult.Success){
                    searchAdaptor.setCartItems(it.data ?: emptyList())
                }

            }
        }
    }

    private fun setSearchListener() {
        binding.srchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                customSearchViewModel.searchItemsInFirebase(newText)
                return true
            }

        })
    }

    private fun observeSearchedCategoryData() {
        viewLifecycleOwner.lifecycleScope.launch {
            customSearchViewModel.searchedCategories.collectLatest {
                if (it is NetworkResult.Success){
                    searchAdaptor.setProducts(it.data ?: emptyList())
                }
            }
        }
    }

    private fun setupCustomSearchAdaptor() {
        binding.apply {
            val spanCount = 3
            val spacingInDp = 12
            val spacingInPixels = (spacingInDp * resources.displayMetrics.density).toInt()

            searchAdaptor = SearchAdaptor(object : ChildCategoryListener {
                override fun addProductToCart(cartProduct: CartProduct) {
                    customSearchViewModel.addProductIntoCartFirebase(cartProduct)
                }

                override fun addProductToFavourites(favouriteItem: FavouriteItem) {
                    customSearchViewModel.addFavouriteIntoCartFirebase(favouriteItem)
                }

            })

            rcviewcategoryItems.adapter = searchAdaptor
            rcviewcategoryItems.layoutManager = GridLayoutManager(requireContext(), spanCount)

            if (rcviewcategoryItems.itemDecorationCount == 0) {
                rcviewcategoryItems.addItemDecoration(
                    GridSpacingItemDecoration(spanCount, spacingInPixels, true)
                )
            }
        }
    }

    private fun showSearchedData(category: CustomSearchViewArgs) {
        val categoryToBeSearched =  category.category
        customSearchViewModel.searchCategory(categoryToBeSearched)

    }


}