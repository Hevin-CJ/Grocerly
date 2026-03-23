package com.example.grocerly.fragments

import android.Manifest
import android.os.Build
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.adapters.CategoryAdaptor
import com.example.grocerly.adapters.OffersAdaptor
import com.example.grocerly.adapters.ParentCategoryAdaptor
import com.example.grocerly.databinding.CartActionLayoutBinding
import com.example.grocerly.databinding.FragmentHomeBinding
import com.example.grocerly.interfaces.AddressActionListener
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.interfaces.SearchViewListener
import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Category
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.WishItem
import com.example.grocerly.model.uievents.HomeUiEvents
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.Mappers
import com.example.grocerly.utils.Mappers.toCategory
import com.example.grocerly.utils.Mappers.toOfferItemList
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.PermissionManager
import com.example.grocerly.utils.ProductCategory
import com.example.grocerly.viewmodel.CartViewModel
import com.example.grocerly.viewmodel.CheckoutViewModel
import com.example.grocerly.viewmodel.FavouriteViewModel
import com.example.grocerly.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Home : Fragment() {

    private var home: FragmentHomeBinding? = null
    private val binding get() = home!!

    private lateinit var loadingDialogue: LoadingDialogue

    private var cartActionBinding: CartActionLayoutBinding? = null

    private lateinit var offersAdaptor: OffersAdaptor
    private lateinit var categoryAdaptor: CategoryAdaptor

    private val homeViewModel: HomeViewModel by viewModels()

   private lateinit var parentCategoryAdaptor: ParentCategoryAdaptor

   private var isAutoScrolling = false
    private var currentScrollPosition = 0

    private val handler = Handler(Looper.getMainLooper())

    private  var changeAddress: ChangeAddress?=null


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

    private val requiredPermissions by lazy {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissions.toTypedArray()
    }


    private val permissionManager = PermissionManager(this) { permissionsMap ->
        val allPermissionsGranted = permissionsMap.values.all { it }
        if (allPermissionsGranted) {
            Log.d("PERMISSION_CHECK", "All initial permissions granted.")
        } else {
            Log.w("PERMISSION_CHECK", "Some permissions were denied.")

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        home = FragmentHomeBinding.inflate(inflater, container, false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionManager.requestPermissions(requiredPermissions)

        setRcOfferAdapter()
        setToolBar()
        setRcViewParentCategoryAdaptor()
        setRcViewCategoryItem()
        actionToSearch()
        setChangeAddress()

        observeUiStateAndEvents()
        checkArgumentsForNotification()
    }

    private fun observeUiStateAndEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){

                launch {
                    homeViewModel.uiState.collect { state ->

                        if (state.isLoading && state.products.isEmpty()) {
                            binding.shimmerlayouthome.startShimmer()
                            binding.shimmerlayouthome.visibility = View.VISIBLE
                            binding.addresstoolbar.visibility = View.INVISIBLE
                            binding.scrollviewhome.visibility = View.INVISIBLE
                        } else {
                            binding.shimmerlayouthome.stopShimmer()
                            binding.shimmerlayouthome.visibility = View.INVISIBLE
                            binding.addresstoolbar.visibility = View.VISIBLE
                            binding.scrollviewhome.visibility = View.VISIBLE
                        }

                        if (state.isLoading){
                            loadingDialogue.show()
                        }else{
                            loadingDialogue.dismiss()
                        }

                        binding.txtviewaddress.text = state.homeAddress

                        parentCategoryAdaptor.setFavouriteItems(state.favouriteItems)

                        parentCategoryAdaptor.setCartItems(state.cartItems)
                        updateCardBadge(state.cartItems.size)


                        parentCategoryAdaptor.setParentCategoryItems(state.products)

                        parentCategoryAdaptor.setWishlistItems(state.wishListItems)

                        if (state.localOffers.isNotEmpty()) {
                            offersAdaptor.setOffers(state.localOffers)
                        }

                        if (state.localCategories.isNotEmpty()) {
                            categoryAdaptor.setItem(state.localCategories.map { it.toCategory() })
                        }
                    }
                }


                launch {
                    homeViewModel.uiEvents.collect { event ->
                        when (event) {
                            is HomeUiEvents.ShowMessage -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }

                            is HomeUiEvents.ActionToOrderDetails -> {
                                val bundle = Bundle().apply {
                                    putParcelable("cartItem", event.cartProduct)
                                    putParcelable("order", event.order)
                                }

                                findNavController().navigate(
                                    R.id.orderDetails,
                                    bundle,
                                    NavOptions.Builder()
                                        .setPopUpTo(R.id.home, false)
                                        .setLaunchSingleTop(true)
                                        .build()
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun checkArgumentsForNotification() {
        val orderId = arguments?.getString("notification_orderId")
        val productId = arguments?.getString("notification_productId")

        if (orderId != null && productId != null) {
            arguments?.remove("notification_orderId")
            arguments?.remove("notification_productId")

            homeViewModel.fetchOrderForNotification(orderId, productId)
        }
    }


    private fun setChangeAddress() {
        binding.lnrlayoutaddress.setOnClickListener {
            changeAddress = ChangeAddress(object : AddressActionListener{
                override fun onAddressActionRequested() {
                    val bundle = Bundle().apply {
                        putString("bundlePass","home")
                    }

                    findNavController().navigate(R.id.action_home_to_addAddress,bundle, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.home,false).build())
                    changeAddress?.dismiss()
                }

                override fun onEditRequested(address: Address) {
                    val action = HomeDirections.actionHomeToUpdateAddress(address,"updateAddress")
                    findNavController().navigate(action)
                    changeAddress?.dismiss()
                }

                override fun onDeleteRequested(address: Address) {
                    homeViewModel.deleteAddress(address)
                    changeAddress?.dismiss()
                }

                override fun onClickLayoutToMakeDefault(address: Address) {
                    homeViewModel.setAsDefaultAddress(address)
                    changeAddress?.dismiss()
                }

            })

            changeAddress?.show(childFragmentManager,"ChangeAddressSheet")
        }
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
        startAutoScroll()
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




    private fun setRcOfferAdapter() {
        offersAdaptor = OffersAdaptor{productId,partnerId->
            Log.d("OfferViewHolder", "bindOffer: ${productId}, ${partnerId}")
            homeViewModel.addOfferToCart(productId,partnerId)
        }

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
                    homeViewModel.addProductToCart(cartProduct)
                }

                override fun addProductToFavourites(favouriteItem: FavouriteItem) {
                    homeViewModel.addProductToFavourites(favouriteItem)
                }

                override fun addProductToWishList(wishItem: WishItem) {
                    homeViewModel.addProductToWishlist(wishItem)
                }


            },object : SearchViewListener{
                override fun onItemClicked(category: ProductCategory) {
                    val action = HomeDirections.actionHomeToCustomSearchView(category)
                    findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.home,false).setLaunchSingleTop(true).build())
                }

            })

            nestedrcview.adapter = parentCategoryAdaptor
            nestedrcview.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
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
            rcviewCategory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)


        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
        if (::loadingDialogue.isInitialized) {
            loadingDialogue.dismiss()
        }

        home = null
        cartActionBinding = null
    }


}

