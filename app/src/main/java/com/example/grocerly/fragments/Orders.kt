package com.example.grocerly.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.adapters.OrderAdaptor
import com.example.grocerly.databinding.FragmentOrdersBinding
import com.example.grocerly.interfaces.OrderActionListener
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.OrdersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Orders : Fragment() {

    private var orders: FragmentOrdersBinding?=null
    private val binding get() = orders!!

    private val ordersViewModel: OrdersViewModel by activityViewModels()

    private lateinit var loadingDialogue: LoadingDialogue

    private lateinit var orderAdaptor: OrderAdaptor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        orders = FragmentOrdersBinding.inflate(inflater,container,false)
        loadingDialogue = LoadingDialogue(requireContext())
        (requireActivity() as MainActivity).setTabLayoutVisibility(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        initializeOrderAdaptorAndListener()
        observeOrders()
        observeOrders()
    }

    private fun initializeOrderAdaptorAndListener() {
         orderAdaptor = OrderAdaptor(object : OrderActionListener{
             override fun onItemTouchListener(
                 cartProduct: CartProduct,
                 order: Order
             ) {
                 val action = OrdersDirections.actionOrdersToOrderDetails(cartProduct,order)
                 findNavController().navigate(action)
             }

         })
        binding.rcvieworders.adapter = orderAdaptor
        binding.rcvieworders.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
    }

    override fun onResume() {
        super.onResume()
        ordersViewModel.fetchAllOrders()
    }

    private fun observeOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            ordersViewModel.orders.collectLatest {
                when(it){
                    is NetworkResult.Loading<*> -> {
                        loadingDialogue.show()
                        binding.txtviewerror.isVisible = false
                        binding.imgviewnoItems.isVisible = false
                    }
                    is NetworkResult.Success<*> -> {
                        loadingDialogue.dismiss()
                        val orders = it.data
                        if (orders.isNullOrEmpty()) {
                            binding.txtviewerror.text = "There is no orders yet"
                            binding.txtviewerror.isVisible = true
                            binding.imgviewnoItems.isVisible = true
                            orderAdaptor.setOrders(emptyList())
                        } else {
                            binding.txtviewerror.isVisible = false
                            binding.imgviewnoItems.isVisible = false
                            orderAdaptor.setOrders(orders)
                        }
                    }
                    is NetworkResult.UnSpecified<*> -> {
                        loadingDialogue.dismiss()
                        binding.txtviewerror.isVisible = false
                        binding.imgviewnoItems.isVisible = false
                    }

                    is NetworkResult.Error<*> ->{
                        loadingDialogue.dismiss()
                        binding.txtviewerror.text = it.message
                        binding.txtviewerror.isVisible = true
                        binding.imgviewnoItems.isVisible = true
                        orderAdaptor.setOrders(emptyList())
                    }
                }

            }
        }
    }

    private fun setToolbar() {
        binding.orderstoolbar.apply {
            setTitle("Orders")
            setNavigationIcon(R.drawable.backarrow)
            setNavigationIconTint(ContextCompat.getColor(requireContext(),R.color.black))
            setNavigationOnClickListener {
               findNavController().navigate( R.id.action_orders_to_profile,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.profile,false).build())
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        orders=null
    }

}