package com.example.grocerly.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.grocerly.R
import com.example.grocerly.adapters.CategoryAdaptor
import com.example.grocerly.adapters.OffersAdaptor
import com.example.grocerly.adapters.ParentCategoryAdaptor
import com.example.grocerly.databinding.CartActionLayoutBinding
import com.example.grocerly.databinding.FragmentHomeBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.interfaces.SearchViewListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Category
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.utils.Mappers
import com.example.grocerly.utils.Mappers.toCategory
import com.example.grocerly.utils.Mappers.toOfferItemList
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.ProductCategory
import com.example.grocerly.viewmodel.CartViewModel
import com.example.grocerly.viewmodel.FavouriteViewModel
import com.example.grocerly.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Home : Fragment() {

    private var home: FragmentHomeBinding? = null
    private val binding get() = home!!

    private var cartActionBinding: CartActionLayoutBinding? = null

    private val offersAdaptor: OffersAdaptor by lazy { OffersAdaptor() }
    private lateinit var categoryAdaptor: CategoryAdaptor

    private val cartViewModel by activityViewModels<CartViewModel>()

    private val favouriteViewModel by activityViewModels<FavouriteViewModel>()

    private val homeViewModel: HomeViewModel by viewModels()

   private lateinit var parentCategoryAdaptor: ParentCategoryAdaptor

   private var isAutoScrolling = false
    private var currentScrollPosition = 0

    private val handler = Handler(Looper.getMainLooper())

   private val runnable = object : Runnable {
        override fun run() {
            try {
                if (offersAdaptor.itemCount == 0) return

                currentScrollPosition = (currentScrollPosition + 1) % offersAdaptor.itemCount
                binding.rcpageoffers.smoothScrollToPosition(currentScrollPosition)

                startAutoScroll()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error in auto-scroll: ${e.message}")
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        home = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRcOfferAdapter()
        setToolBar()
        setRcViewParentCategoryAdaptor()
        setRcViewCategoryItem()
        observeGetAllItems()
        setCategoryItems()
        observeProductFromFirebase()
        observeOffersFromFirebase()
        observeAddProductInCart()
        observeAddedToFavouriteState()
        showShimmerLayout()
        observeCartItems()
        observeHomeAddress()
        actionToSearch()
        observeOffersErrorFromFirebase()
    }

    private fun actionToSearch() {
        binding.apply {
            txtviewSeeAll.setOnClickListener {
                val action = HomeDirections.actionHomeToCustomSearchView(ProductCategory.selectcatgory)
                findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.home,false).setLaunchSingleTop(true).build())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        favouriteViewModel.getAllFavouritesFromFirebase()
        startAutoScroll()
    }

    private fun observeHomeAddress() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.homeAddress.collectLatest {
                if (it is NetworkResult.Success || it is  NetworkResult.Error){
                    binding.txtviewaddress.text = it.data

                    if (!it.message.isNullOrEmpty()){
                       Toast.makeText(requireContext(), it.message.toString(), Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }

    private fun observeCartItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.cartItems.collectLatest {
                if (it is NetworkResult.Success || it is NetworkResult.Error){

                    it.data?.let { cartProducts ->
                        parentCategoryAdaptor.setCartItems(cartProducts)
                    }
                    updateCardBadge(it.data?.size ?: 0)
                    Log.d("datasizegot",it.data?.size.toString())

                    it.message?.mapNotNull {
                        Toast.makeText(requireContext(),it.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateCardBadge(size: Int) {
        cartActionBinding?.let { badgeBinding ->
            if (size > 0) {
                badgeBinding.cartBadgeTextView.text = size.toString()
                badgeBinding.cartBadgeTextView.visibility = View.VISIBLE
            } else {
                badgeBinding.cartBadgeTextView.visibility = View.GONE
            }
        }
    }

    private fun showShimmerLayout() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.products.collectLatest {
                if (it.data.isNullOrEmpty()){
                    binding.shimmerlayouthome.startShimmer()
                    binding.shimmerlayouthome.visibility = View.VISIBLE
                    binding.addresstoolbar.visibility = View.INVISIBLE
                    binding.scrollviewhome.visibility = View.INVISIBLE
                }else{
                    binding.shimmerlayouthome.stopShimmer()
                    binding.shimmerlayouthome.visibility = View.INVISIBLE
                    binding.addresstoolbar.visibility = View.VISIBLE
                    binding.scrollviewhome.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun observeGetAllItems() {
             viewLifecycleOwner.lifecycleScope.launch {
            favouriteViewModel.favouritesList.collectLatest { favourites->
                when(favourites){
                    is NetworkResult.Error<*> -> {

                    }
                    is NetworkResult.Loading<*> -> {

                    }
                    is NetworkResult.Success<*> -> {
                        favourites.data?.let {
                            parentCategoryAdaptor.setFavouriteItems(it)
                        }
                    }
                    is NetworkResult.UnSpecified<*> -> {

                    }
                }
            }
        }
    }

    private fun observeAddedToFavouriteState() {
       viewLifecycleOwner.lifecycleScope.launch {
           favouriteViewModel.favouritesState.collectLatest { favourites->
               when(favourites){
                   is NetworkResult.Error<*> -> {
                       Toast.makeText(requireContext(), favourites.message, Toast.LENGTH_SHORT).show()
                   }
                   is NetworkResult.Loading<*> -> {

                   }
                   is NetworkResult.Success<*> -> {
                      favourites.data?.let {
                          Toast.makeText(requireContext(), "Your Item (${it.product.itemName}) \nAdded to favourites", Toast.LENGTH_SHORT).show()
                      }
                   }
                   is NetworkResult.UnSpecified<*> -> {

                   }
               }
           }
       }
    }

    private fun observeAddProductInCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            cartViewModel.addedCartItems.collectLatest{ result ->
                if (result is NetworkResult.Error){
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setToolBar() {
        val menu = binding.addresstoolbar.menu
        val menuItem = menu.findItem(R.id.cartm)


        val actionView = LayoutInflater.from(requireContext()).inflate(R.layout.cart_action_layout, null)
        cartActionBinding = CartActionLayoutBinding.bind(actionView)

        menuItem.actionView = cartActionBinding?.root

        cartActionBinding?.root?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_cart)
        }
    }

    private fun stopAutoScroll() {
        isAutoScrolling = false
        handler.removeCallbacks(runnable)
    }


    private fun startAutoScroll() {
        isAutoScrolling = true
        handler.postDelayed(runnable, 3000)
    }



    private fun observeOffersErrorFromFirebase() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.offers.collectLatest { offers ->
                if (offers is NetworkResult.Error){
                    Toast.makeText(requireContext(),offers.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeOffersFromFirebase() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.getOffers.collectLatest { offers ->
                offersAdaptor.setOffers(offers.toOfferItemList())
            }
        }
    }


    private fun observeProductFromFirebase() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.products.collectLatest{ result ->

                when (result) {
                    is NetworkResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }

                    is NetworkResult.Loading -> {

                    }

                    is NetworkResult.Success -> {
                        result.data?.let {
                            parentCategoryAdaptor.setParentCategoryItems(it)
                        }
                    }

                    is NetworkResult.UnSpecified -> {

                    }
                }

            }

        }
    }

    private fun setCategoryItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.categories.collectLatest {
                if(it.isNotEmpty()) {
                    categoryAdaptor.setItem(it.map { it.toCategory() })
                }
            }
        }
    }


    private fun setRcOfferAdapter() {
        binding.apply {
            rcpageoffers.adapter = offersAdaptor
           rcpageoffers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            rcpageoffers.setHasFixedSize(true)
            rcpageoffers.isNestedScrollingEnabled = false

            LinearSnapHelper().attachToRecyclerView(binding.rcpageoffers)
        }

    }

    private fun setRcViewParentCategoryAdaptor() {
        binding.apply {

            parentCategoryAdaptor = ParentCategoryAdaptor( object : ChildCategoryListener{
                override fun addProductToCart(cartProduct: CartProduct) {
                    cartViewModel.addProductIntoCartFirebase(cartProduct)
                }

                override fun addProductToFavourites(favouriteItem: FavouriteItem) {
                    favouriteViewModel.addToFavourites(favouriteItem)
                }


            },object : SearchViewListener{
                override fun onItemClicked(category: ProductCategory) {
                    val action = HomeDirections.actionHomeToCustomSearchView(category)
                    findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.home,false).setLaunchSingleTop(true).build())
                }

            })

            nestedrcview.adapter = parentCategoryAdaptor
            nestedrcview.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }


    private fun setRcViewCategoryItem() {
        binding.apply {

            categoryAdaptor = CategoryAdaptor(object : SearchViewListener{
                override fun onItemClicked(category: ProductCategory) {
                    val action = HomeDirections.actionHomeToCustomSearchView(category)
                    findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.home,false).setLaunchSingleTop(true).build())
                }

            })

            rcviewCategory.adapter = categoryAdaptor
            rcviewCategory.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)


        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
        home = null
        cartActionBinding = null
    }


}