package com.example.grocerly.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.NavOptions.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.adapters.MenuItemAdaptor
import com.example.grocerly.adapters.MenuSideAdaptor
import com.example.grocerly.databinding.FragmentMenuBinding
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.MenuAction
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.MenuViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Menu : Fragment() {
    private var menu: FragmentMenuBinding?=null
    private val binding get() = menu!!

    private val menuViewModel by viewModels<MenuViewModel>()

    private lateinit var menuSideAdaptor: MenuSideAdaptor

    private lateinit var menuItemAdaptor: MenuItemAdaptor

    private lateinit var loadingDialogue: LoadingDialogue

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        menu = FragmentMenuBinding.inflate(inflater,container,false)
        loadingDialogue  = LoadingDialogue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMenuAdaptors()
        observeProductViewModels()
        observeLogoutState()
    }

    private fun observeProductViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    menuViewModel.categories.collectLatest {
                        menuSideAdaptor.updateData(it.data ?: emptyList())

                        if (it.message != null){
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                launch {
                    menuViewModel.search_data.collectLatest { result ->
                        Log.d("products_menu", "Received ${result.data?.size} items")

                        if (result.data != null) {
                            menuItemAdaptor.setProductList(result.data)
                        } else if (result.message != null) {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                launch {
                    menuViewModel.favourites.collectLatest { result ->
                        if (result.data != null) {
                            menuItemAdaptor.setFavouritesList(result.data)
                        } else if (result.message != null) {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                launch {
                    menuViewModel.cartItems.collectLatest { result ->
                        if (result.data != null) {
                            menuItemAdaptor.setCartList(result.data)
                        } else if (result.message != null) {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


    private fun setMenuAdaptors() {

       menuItemAdaptor =  MenuItemAdaptor{action ->
            when(action){
                MenuAction.SIGN_OUT -> {
                    menuViewModel.signOut()
                }
                MenuAction.ORDERS -> {
                    findNavController().navigate(R.id.action_menu_to_orders,null, Builder().setPopUpTo(R.id.menu,false).setLaunchSingleTop(true).build())
                }
                MenuAction.ASSISTANT -> {
                    findNavController().navigate(R.id.action_menu_to_helpCenter,null, Builder().setPopUpTo(R.id.menu,false).setLaunchSingleTop(true).build())
                }

                is MenuAction.addToCart -> {
                    menuViewModel.addToCart(action.product)
                }
                is MenuAction.addToFavourites -> {
                    menuViewModel.addToFavourite(action.favouriteItem)
                }
            }
        }

        menuSideAdaptor = MenuSideAdaptor(){ selectedCategory->
            if (selectedCategory.categoryTitleForFirebase == "Account"){
                menuItemAdaptor.showAccountView()
            }else{
                menuViewModel.searchCategory(selectedCategory.category)
            }
        }

        binding.rcviewsidebar.apply {
            adapter = menuSideAdaptor
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        binding.apply {
            rcviewmenuitems.adapter = menuItemAdaptor
            val gridLayoutManager = GridLayoutManager(requireContext(),2)

            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (menuItemAdaptor.getItemViewType(position)) {
                        MenuItemAdaptor.TYPE_ACCOUNT -> 2
                        else -> 1
                    }
                }
            }

            rcviewmenuitems.layoutManager = gridLayoutManager
        }
    }


    private fun observeLogoutState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                menuViewModel.logoutstate.collect{result->
                    when(result){
                        is NetworkResult.Error -> {
                            loadingDialogue.dismiss()
                            Toast.makeText(requireContext(),result.message, Toast.LENGTH_SHORT).show()
                        }
                        is NetworkResult.Loading -> {
                            loadingDialogue.setText("Logging Out,Please wait....")
                           loadingDialogue.show()
                            Toast.makeText(requireContext(),"Loading.. Please wait",Toast.LENGTH_SHORT).show()
                        }
                        is NetworkResult.Success -> {
                           loadingDialogue.dismiss()
                            Toast.makeText(requireContext(),result.data,Toast.LENGTH_SHORT).show()
                            intentToMainActivity()
                        }
                        is NetworkResult.UnSpecified -> {
                            loadingDialogue.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun intentToMainActivity() {

        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        menu = null
    }

}