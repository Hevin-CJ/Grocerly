package com.example.grocerly.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.adapters.CouponAdaptor
import com.example.grocerly.databinding.FragmentCouponsBinding
import com.example.grocerly.model.uievents.CouponUiEvent
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.viewmodel.CouponViewModel
import kotlinx.coroutines.launch


class Coupons : Fragment(R.layout.fragment_coupons) {

    private var coupons: FragmentCouponsBinding?=null
    private val binding get() = coupons!!

    private val couponViewModel by activityViewModels<CouponViewModel>()

    private lateinit var loadingDialogue: LoadingDialogue

    private val couponAdaptor by lazy { CouponAdaptor() }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coupons = FragmentCouponsBinding.bind(view)
        loadingDialogue = LoadingDialogue(requireContext())

        setCouponToolbar()
        setCouponAdaptor()
        observeCouponUiAndEventState()

    }

    private fun setCouponAdaptor() {
        binding.rcviewcoupons.apply {
            adapter = couponAdaptor
            layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
        }
    }

    private fun observeCouponUiAndEventState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){

                launch {
                    couponViewModel.couponUiState.collect {result->

                       if (result.isLoading){
                           loadingDialogue.show()
                       }else{
                           loadingDialogue.dismiss()
                       }

                        Log.d("couponlistgot",result.couponList.toString())
                        if (result.couponList.isEmpty()) {
                            binding.txtviewNoCoupons.visibility = View.VISIBLE
                            binding.rcviewcoupons.visibility = View.GONE
                        } else {
                            binding.txtviewNoCoupons.visibility = View.GONE
                            binding.rcviewcoupons.visibility = View.VISIBLE
                            couponAdaptor.submitList(result.couponList)
                        }

                    }

                }

                launch {
                    couponViewModel.couponUiEvent.collect {result->
                       when(result){
                           is CouponUiEvent.ShowMessage -> {
                               Toast.makeText(requireContext(),result.message,Toast.LENGTH_SHORT).show()
                           }
                       }
                    }

                }

            }
        }
    }

    private fun setCouponToolbar() {
        binding.coupontoolbar.setNavigationOnClickListener {
            findNavController().popBackStack(R.id.profile,false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coupons = null
    }

}