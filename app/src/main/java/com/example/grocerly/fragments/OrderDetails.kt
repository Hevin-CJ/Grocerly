package com.example.grocerly.fragments


import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.adapters.ChildCategoryAdaptor
import com.example.grocerly.adapters.OtherOrderAdaptor
import com.example.grocerly.databinding.FragmentOrderDetailsBinding
import com.example.grocerly.databinding.ReasonSpinnerBinding
import com.example.grocerly.interfaces.ChildCategoryListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Order
import com.example.grocerly.utils.CancellationStatus
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.OrderStatus
import com.example.grocerly.utils.ProductCategory
import com.example.grocerly.utils.Reason
import com.example.grocerly.viewmodel.OrdersViewModel
import com.loukwn.stagestepbar.StageStepBar.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrderDetails : Fragment() {
    private var orderDetails: FragmentOrderDetailsBinding?=null
    private val binding get() = orderDetails!!

    private val ordersViewModel: OrdersViewModel by viewModels()

    private val orderDetailsArgs: OrderDetailsArgs by navArgs()

    private lateinit var cartProduct: CartProduct
    private lateinit var order: Order

    private val otherOrderAdaptor: OtherOrderAdaptor by lazy { OtherOrderAdaptor() }

    private lateinit var childCategoryAdaptor: ChildCategoryAdaptor


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        orderDetails = FragmentOrderDetailsBinding.inflate(inflater, container, false)
        (requireActivity() as MainActivity).setTabLayoutVisibility(false)
        cartProduct = orderDetailsArgs.cartItem
        order = orderDetailsArgs.order
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbarOrderDetails()
        setOrderDetails(cartProduct,order)
        observeOrderStatus()
    }



    private fun observeOrderStatus() {
       viewLifecycleOwner.lifecycleScope.launch {
           ordersViewModel.orderStatus.collectLatest {
               if(it is NetworkResult.Success){
                   it.data?.let { cartProduct ->
                       setProgressBar(cartProduct)
                   }
               }
           }
       }
    }

    private fun observeCartItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            ordersViewModel.cartItems.collectLatest {
                if (it is NetworkResult.Success) {
                    it.data?.let { cartProducts ->
                        childCategoryAdaptor.setCartItems(cartProducts)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ordersViewModel.fetchFavourites()
        ordersViewModel.fetchOrderStatus(cartProduct,order)
    }

    private fun observeCategoryItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            ordersViewModel.Products.collectLatest {
                if (it is NetworkResult.Success){
                    it.data?.let { products ->
                        childCategoryAdaptor.setProducts(products)
                    }
                }
            }

        }
    }

    private fun setToolbarOrderDetails() {
        binding.toolbardetails.apply {
            setNavigationIcon(R.drawable.backarrow)
            setTitle("Order Details")
            setNavigationIconTint(ContextCompat.getColor(requireContext(),R.color.black))
            setNavigationOnClickListener {
                findNavController().popBackStack(R.id.orders,false)
            }
        }
    }

    private fun setOrderDetails(cartProduct: CartProduct,order: Order) {
        binding.apply {
            txtvieworderId.text = order.orderId
            txtiviewitemname.text = cartProduct.product.itemName
            txtviewquantity.text = cartProduct.quantity.toString()
            txtviewprice.text = cartProduct.product.itemPrice.toString()
            Glide.with(requireContext())
                .load(cartProduct.product.image)
                .into(binding.imageView19)
            setAddressDetailsAndPayment(order)
            ordersViewModel.fetchOrderStatus(cartProduct,order)
            setCancelOrder(cartProduct,order)
            setAdaptiveAdaptor(order)
            setDeliveredState(cartProduct,order)
        }
    }

    private fun setDeliveredState(
        cartProduct: CartProduct,
        order: Order
    ) {
       binding.setdeliveredbtn.setOnClickListener {
         val dialgoueDelivered =   AlertDialog.Builder(requireContext())
               .setTitle("Delivered")
               .setMessage("Are you sure to set as Delivered?")
               .setPositiveButton("Yes"){dialogue,which->
                   ordersViewModel.setOrderStatus(cartProduct,order, OrderStatus.DELIVERED)
                   dialogue.cancel()
               }
               .setNegativeButton("No"){dialogue,which->
                   dialogue.cancel()
               }.create()

           dialgoueDelivered.show()
       }
    }

    private fun setAdaptiveAdaptor(order: Order) {
        if (order.items.size>1){
            val filteredItems = order.items.filterNot{ it.product.productId == cartProduct.product.productId}
            setOrderAdaptor(filteredItems)
        }else{
            NewItemsAdaptor(cartProduct.product.category)
        }
    }

    private fun setCancelOrder(cartProduct: CartProduct, order: Order) {
        binding.cancelButton.setOnClickListener {
           AlertDialog.Builder(requireContext()).apply {
                setTitle("Cancel Order?")
                setMessage("Are you sure to cancel this item?")
                setPositiveButton("Yes"){dialogue,which ->

                    val spinnerBinding = ReasonSpinnerBinding.inflate(layoutInflater)
                    val reasons = Reason.entries.map { it.displayName }

                    val adapter = ArrayAdapter(requireContext(),android.R.layout.simple_spinner_item,reasons)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                    spinnerBinding.spinnerreason.adapter = adapter

                    val reasonDialogue = AlertDialog.Builder(requireContext())
                        .setView(spinnerBinding.root)
                        .create()
                    spinnerBinding.dismissbtn.setOnClickListener {
                        reasonDialogue.dismiss()
                    }

                    spinnerBinding.canceorderbtn.setOnClickListener {
                        val reason = spinnerBinding.spinnerreason.selectedItem.toString()

                        Log.d("cancellationgot",cartProduct.cancellationInfo.cancellationStatus.toString())
                        ordersViewModel.setCancelOrder(cartProduct,order,reason)
                        reasonDialogue.dismiss()
                    }

                    reasonDialogue.show()

                }
                setNegativeButton("No"){dialogue,which ->
                    dialogue.cancel()
                }
            }.show()
        }
    }

    private fun setProgressBar(cartProduct: CartProduct) {
        if(cartProduct.cancellationInfo.cancellationStatus == CancellationStatus.Cancelled) {
            binding.apply {
                stageStepBar.setStageStepConfig(listOf(1,1,1))
                stageStepBar.setCurrentState(State(3,0))

                stageStepBar.setFilledThumbToCustomDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.checkred)!!)
                stageStepBar.setActiveThumbToCustomDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.checkred)!!)
                stageStepBar.setFilledTrackToNormalShape(ContextCompat.getColor(requireContext(), R.color.red))

                cancelButton.visibility =  if ( cartProduct.cancellationInfo.cancellationStatus == CancellationStatus.Cancelled) View.GONE else View.VISIBLE
                txtviewdeliveryupdate.text = "The Item has been cancelled by ${cartProduct.cancellationInfo?.cancelledBy?.displayName} because \n ${cartProduct.cancellationInfo.reason}"
            }
            return
        }

        when(cartProduct.orderStatus){
            OrderStatus.PENDING -> {
                binding.apply {
                    stageStepBar.setCurrentState(State(0,0))
                    cancelButton.visibility =  if (cartProduct.orderStatus == OrderStatus.PENDING) View.VISIBLE else View.INVISIBLE
                    txtviewdeliveryupdate.text = "Order has placed and awaiting confirmation from Seller"
                }
            }
            OrderStatus.ACCEPTED -> {
                binding.apply {
                    stageStepBar.setCurrentState(State(0,1))
                    cancelButton.visibility =  if (cartProduct.orderStatus == OrderStatus.ACCEPTED) View.VISIBLE else View.GONE
                    txtviewdeliveryupdate.text = "Order has been accepted by Seller"
                }
            }
            OrderStatus.READY -> {
                binding.apply {
                    stageStepBar.setCurrentState(State(0,2))
                    cancelButton.visibility =  if (cartProduct.orderStatus == OrderStatus.READY) View.VISIBLE else View.GONE
                    txtviewdeliveryupdate.text = "Order is prepared and ready to ship "
                }
            }
            OrderStatus.SHIPPED -> {
                binding.apply {
                    stageStepBar.setCurrentState(State(1,1))
                    cancelButton.visibility =  if (cartProduct.orderStatus == OrderStatus.SHIPPED) View.GONE else View.VISIBLE
                    txtviewdeliveryupdate.text = "The Item has been shipped"
                }
            }

            OrderStatus.OUTFORDELIVERY ->{
                binding.apply {
                    stageStepBar.setCurrentState(State(2,1))
                    val isOutForDelivery = cartProduct.orderStatus == OrderStatus.OUTFORDELIVERY
                    cancelButton.visibility =  if (isOutForDelivery) View.GONE else View.VISIBLE
                    setdeliveredbtn.visibility =if (isOutForDelivery) View.VISIBLE else View.GONE
                    txtviewdeliveryupdate.text = "The item is out for delivery"

                }
            }
            OrderStatus.DELIVERED -> {
                binding.apply {
                    stageStepBar.setCurrentState(State(3,0))
                    cancelButton.visibility =  if (cartProduct.orderStatus == OrderStatus.DELIVERED) View.GONE else View.VISIBLE
                    txtviewdeliveryupdate.text = "Your Item has been delivered"
                    setdeliveredbtn.visibility = View.GONE
                }
            }

        }
    }

    private fun NewItemsAdaptor(category: ProductCategory) {
        childCategoryAdaptor = ChildCategoryAdaptor( object : ChildCategoryListener{
            override fun addProductToCart(cartProduct: CartProduct) {
                ordersViewModel.insertCartProduct(cartProduct)
            }

            override fun addProductToFavourites(favouriteItem: FavouriteItem) {
               ordersViewModel.insertProductIntoFavourites(favouriteItem)
            }

        })
        binding.rcviewotheritems.adapter = childCategoryAdaptor
        binding.rcviewotheritems.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
        viewLifecycleOwner.lifecycleScope.launch {
            ordersViewModel.fetchProductByCategory(category)

            ordersViewModel.favourites.collectLatest {
                if (it is NetworkResult.Success){
                    it.data?.let { items ->
                        childCategoryAdaptor.setFavouriteItems(items)
                    }
                }
            }
        }
        val alternativeText = SpannableString("Other Products You May Like...").apply {

            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.black)),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            setSpan(
                AbsoluteSizeSpan(16,true),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )


            setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

        }

        binding.txtviewsimilar.text = alternativeText
        observeCategoryItems()
        observeCartItems()
    }

    private fun setOrderAdaptor(orders: List<CartProduct>) {
        binding.rcviewotheritems.adapter = otherOrderAdaptor
        binding.rcviewotheritems.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
        otherOrderAdaptor.setCartProducts(orders)
    }

    private fun setAddressDetailsAndPayment(order: Order) {
        binding.apply {
            textView20.text = order.address.firstName
            txtviewaddress.text = buildString {
                append(order.address.deliveryAddress)
                append(" , ")
                append(order.address.city)
                append(order.address.state)
                append(order.address.pinCode)
            }
            txtviewphoneno.text = buildString {
                append(order.address.phoneNumber)
                append(" , ")
                append(order.address.alternateNumber)
            }
            txtviewpaymenttypeandamount.text = buildString {
                append(order.totalOrderPrice)
                append(" , ")
                append(order.paymentType)
            }
        }
    }

}