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
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.example.grocerly.interfaces.AddressActionListener
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.interfaces.SearchViewListener
import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.WishItem
import com.example.grocerly.model.uievents.HomeUiEvents
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.PermissionManager
import com.example.grocerly.utils.ProductCategory
import com.example.grocerly.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Home : Fragment() {

    private var home: FragmentHomeBinding? = null
    private val binding get() = home!!

    private lateinit var loadingDialogue: LoadingDialogue
    private var cartActionBinding: CartActionLayoutBinding? = null

    private var offersAdaptor: OffersAdaptor? = null
    private var categoryAdaptor: CategoryAdaptor? = null
    private var parentCategoryAdaptor: ParentCategoryAdaptor? = null

    private val homeViewModel: HomeViewModel by activityViewModels()

    private var isAutoScrolling = false
    private var currentScrollPosition = 0


    companion object {
        // Survives fragment recreation by Jetpack Navigation
        private var verticalScrollPosition = 0
    }
    private var isScrollRestored = false

    private val handler = Handler(Looper.getMainLooper())
    private var changeAddress: ChangeAddress? = null

    private val runnable = object : Runnable {
        override fun run() {
            try {
                if (offersAdaptor?.itemCount == 0) return

                currentScrollPosition = (currentScrollPosition + 1) % (offersAdaptor?.itemCount ?: 0)
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
        setupSwipeRefresh()


        binding.scrollviewhome.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (isScrollRestored) {
                verticalScrollPosition = scrollY
            }
        }

        observeUiStateAndEvents()
        checkArgumentsForNotification()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.refreshHomeData()
        }
    }

    private fun restoreScrollPosition() {
        if (verticalScrollPosition <= 0) {
            isScrollRestored = true
            return
        }

        binding.scrollviewhome.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (home == null) return true

                val child = binding.scrollviewhome.getChildAt(0)
                if (child != null && child.height > binding.scrollviewhome.height) {

                    binding.scrollviewhome.viewTreeObserver.removeOnPreDrawListener(this)

                    // Synchronous call. No .post block.
                    binding.scrollviewhome.scrollTo(0, verticalScrollPosition)
                    isScrollRestored = true

                    // Prevent this specific frame from drawing at Y=0.
                    return false
                }
                return true
            }
        })
    }

    private fun observeUiStateAndEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    homeViewModel.uiState
                        .map { Triple(it.isLoading, it.products, it.isRefreshing) }
                        .distinctUntilChanged()
                        .collect { (isLoading, products, isRefreshing) ->

                            binding.swipeRefreshLayout.isRefreshing = isRefreshing

                            val isInitialFeedLoad = isLoading && products.isEmpty() && !isRefreshing

                            if (isInitialFeedLoad) {
                                binding.shimmerlayouthome.visibility = View.VISIBLE
                                binding.addresstoolbar.visibility = View.INVISIBLE
                                binding.scrollviewhome.visibility = View.INVISIBLE
                            } else {
                                binding.scrollviewhome.post {
                                    binding.shimmerlayouthome.stopShimmer()
                                    binding.shimmerlayouthome.visibility = View.INVISIBLE
                                    binding.addresstoolbar.visibility = View.VISIBLE
                                    binding.scrollviewhome.visibility = View.VISIBLE
                                }
                            }
                        }
                }

                launch {
                    homeViewModel.uiState.map { it.products }.distinctUntilChanged().collect { products ->
                        parentCategoryAdaptor?.setParentCategoryItems(products) {
                            if (!isScrollRestored) {
                                restoreScrollPosition()
                            }
                        }
                    }
                }

                launch {
                    homeViewModel.uiState.map { it.isActionLoading }.distinctUntilChanged().collect { isActionLoading ->
                        if (isActionLoading) loadingDialogue.show() else loadingDialogue.dismiss()
                    }
                }

                launch {
                    homeViewModel.uiState.map { it.homeAddress }.distinctUntilChanged().collect { address ->
                        binding.txtviewaddress.text = address
                    }
                }

                launch {
                    homeViewModel.uiState.map { it.cartItems }.distinctUntilChanged().collect { cartItems ->
                        parentCategoryAdaptor?.setCartItems(cartItems)
                        updateCardBadge(cartItems.size)
                    }
                }

                launch {
                    homeViewModel.uiState.map { it.favouriteItems }.distinctUntilChanged().collect { favs ->
                        parentCategoryAdaptor?.setFavouriteItems(favs)
                    }
                }

                launch {
                    homeViewModel.uiState.map { it.wishListItems }.distinctUntilChanged().collect { wishList ->
                        parentCategoryAdaptor?.setWishlistItems(wishList)
                    }
                }

                launch {
                    homeViewModel.uiState.map { it.localOffers }.distinctUntilChanged().collect { offers ->
                        if (offers.isNotEmpty()) offersAdaptor?.setOffers(offers)
                    }
                }

                launch {
                    homeViewModel.uiState.map { it.categoryItems }.distinctUntilChanged().collect { categories ->
                        if (categories.isNotEmpty()) categoryAdaptor?.setItem(categories.map { it })
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
            changeAddress = ChangeAddress(object : AddressActionListener {
                override fun onAddressActionRequested() {
                    val bundle = Bundle().apply {
                        putString("bundlePass", "home")
                    }

                    findNavController().navigate(R.id.action_home_to_addAddress, bundle, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.home, false).build())
                    changeAddress?.dismiss()
                }

                override fun onEditRequested(address: Address) {
                    val action = HomeDirections.actionHomeToUpdateAddress(address, "updateAddress")
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

            changeAddress?.show(childFragmentManager, "ChangeAddressSheet")
        }
    }

    private fun actionToSearch() {
        binding.apply {
            txtviewSeeAll.setOnClickListener {
                val action = HomeDirections.actionHomeToCustomSearchView(ProductCategory.selectcatgory)
                findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.home, false).setLaunchSingleTop(true).build())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll()
        isScrollRestored = false
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
        if (offersAdaptor == null) {
            offersAdaptor = OffersAdaptor { productId, partnerId ->
                Log.d("OfferViewHolder", "bindOffer: ${productId}, ${partnerId}")
                homeViewModel.addOfferToCart(productId, partnerId)
            }
        }

        binding.apply {
            rcpageoffers.adapter = offersAdaptor
            rcpageoffers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            rcpageoffers.setHasFixedSize(true)
            rcpageoffers.isNestedScrollingEnabled = false

            rcpageoffers.onFlingListener = null
            LinearSnapHelper().attachToRecyclerView(rcpageoffers)
        }
    }

    private fun setRcViewParentCategoryAdaptor() {
        binding.apply {

            if (parentCategoryAdaptor == null) {
                parentCategoryAdaptor = ParentCategoryAdaptor(
                    object : ChildCategoryListener {
                        override fun addProductToCart(cartProduct: CartProduct) {
                            homeViewModel.addProductToCart(cartProduct)
                        }

                        override fun addProductToFavourites(favouriteItem: FavouriteItem) {
                            homeViewModel.addProductToFavourites(favouriteItem)
                        }

                        override fun addProductToWishList(wishItem: WishItem) {
                            homeViewModel.addProductToWishlist(wishItem)
                        }
                    },
                    object : SearchViewListener {
                        override fun onItemClicked(category: ProductCategory) {
                            val action = HomeDirections.actionHomeToCustomSearchView(category)
                            findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.home, false).setLaunchSingleTop(true).build())
                        }
                    }
                )
            }

            nestedrcview.adapter = parentCategoryAdaptor
            nestedrcview.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun setRcViewCategoryItem() {
        binding.apply {

            if (categoryAdaptor == null) {
                categoryAdaptor = CategoryAdaptor(object : SearchViewListener {
                    override fun onItemClicked(category: ProductCategory) {
                        val action = HomeDirections.actionHomeToCustomSearchView(category)
                        findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.home, false).setLaunchSingleTop(true).build())
                    }
                })
            }

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

        if (::loadingDialogue.isInitialized) {
            loadingDialogue.dismiss()
        }

        binding.rcpageoffers.adapter = null
        binding.nestedrcview.adapter = null
        binding.rcviewCategory.adapter = null

        home = null
        cartActionBinding = null
    }

    fun resetAndScrollToTop() {

        verticalScrollPosition = 0
        isScrollRestored = true
        binding.scrollviewhome.smoothScrollTo(0, 0)

        binding.rcviewCategory.scrollToPosition(0)
        currentScrollPosition = 0

        parentCategoryAdaptor?.resetScrollState()
    }
}