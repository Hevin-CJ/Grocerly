package com.example.grocerly.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.grocerly.R
import com.example.grocerly.databinding.FragmentOrderPlacedBinding


class OrderPlaced : Fragment() {

    private var orderPlaced: FragmentOrderPlacedBinding?=null
    private val binding get() = orderPlaced!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        orderPlaced = FragmentOrderPlacedBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionToHome()
    }

    private fun actionToHome() {
        binding.backToHomeBtn.setOnClickListener {
            findNavController().navigate(R.id.action_orderPlaced_to_home,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.home,true).build())
        }
    }

}