package com.example.grocerly.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.SavedCardListener
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.adapters.SavedCardAdaptor
import com.example.grocerly.databinding.FragmentSavedCardsBinding
import com.example.grocerly.model.Card
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.SavedCardsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SavedCards : Fragment() {
    private var savedCards: FragmentSavedCardsBinding?=null
    private val binding get() = savedCards!!

    private val savedCardsViewModel: SavedCardsViewModel by activityViewModels()

    private lateinit var loadingDialogue: LoadingDialogue

    private lateinit var savedCardAdaptor: SavedCardAdaptor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        savedCards = FragmentSavedCardsBinding.inflate(inflater,container,false)
        (requireActivity() as MainActivity).setTabLayoutVisibility(false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionToInsertNewCard()
        setToolbar()
        fetchAllSavedCards()
        setCardsAdaptor()
        observeEmptyCardState()
    }

    private fun observeEmptyCardState() {
        savedCardsViewModel.emptyCardState.observe(viewLifecycleOwner) {isEmpty->
            if (isEmpty){
                binding.txtviewNoSavedCard.visibility = View.VISIBLE
                binding.imgviewcardNotadded.visibility = View.VISIBLE
            }else{
                binding.txtviewNoSavedCard.visibility = View.INVISIBLE
                binding.imgviewcardNotadded.visibility = View.INVISIBLE
            }
        }
    }

    private fun setCardsAdaptor() {
        savedCardAdaptor = SavedCardAdaptor(object : SavedCardListener{
            override fun onEditCardClicked(card: Card) {
                val actionToEditCard = SavedCardsDirections.actionSavedCardsToUpsertCard(savedCard = card, isUpdate = true)

                findNavController().navigate(actionToEditCard)
            }

        })

        binding.recyclerView.adapter = savedCardAdaptor
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
    }

    private fun fetchAllSavedCards() {
       viewLifecycleOwner.lifecycleScope.launch {
           savedCardsViewModel.fetchedCards.collectLatest { fetchedAddress ->
               when(fetchedAddress){
                   is NetworkResult.Error<*> -> {
                       loadingDialogue.dismiss()
                       Toast.makeText(requireContext(),fetchedAddress.message, Toast.LENGTH_SHORT).show()

                   }
                   is NetworkResult.Loading<*> -> {
                       loadingDialogue.show()
                   }
                   is NetworkResult.Success<*> -> {
                       fetchedAddress.data?.let {
                           savedCardAdaptor.setCard(it)
                       }
                       loadingDialogue.dismiss()

                   }
                   is NetworkResult.UnSpecified<*> -> {
                       loadingDialogue.dismiss()
                   }
               }
           }
       }
    }

    private fun setToolbar() {
        binding.savedcardstoolbar.apply {
            setNavigationIcon(R.drawable.backarrow)
            navigationIcon?.setTint(ContextCompat.getColor(requireContext(),R.color.black))
            setNavigationOnClickListener {
                findNavController().navigate(R.id.action_savedCards_to_profile,null, NavOptions.Builder().setPopUpTo(R.id.profile,true).setLaunchSingleTop(true).build())
            }

            setTitle("Saved Debit/Credit Cards")
        }


    }

    private fun actionToInsertNewCard() {
        binding.btnaddnewcard.setOnClickListener {
           val card = SavedCardsDirections.actionSavedCardsToUpsertCard(isUpdate = false, savedCard = null)
            findNavController().navigate(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        savedCards = null
    }


}