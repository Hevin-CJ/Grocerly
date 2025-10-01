package com.example.grocerly.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedDispatcher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.databinding.FragmentEditLanguageBinding
import com.example.grocerly.preferences.GrocerlyDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditLanguage : Fragment() {
    private var editLanguage: FragmentEditLanguageBinding?=null
    private val binding get() = editLanguage!!


    private lateinit var grocerlyDataStore: GrocerlyDataStore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        editLanguage = FragmentEditLanguageBinding.inflate(inflater,container,false)
        grocerlyDataStore  = GrocerlyDataStore(requireContext())
        (requireActivity() as MainActivity).setTabLayoutVisibility(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbarNavigation()
        getLanguageFromDataStore()
        saveLanguageFromRadioBtn()
    }

    private fun getLanguageFromDataStore() {
        viewLifecycleOwner.lifecycleScope.launch {
            grocerlyDataStore.getLanguage().collectLatest { lang->
                when (lang) {
                    "en" -> binding.langradiobtn.check(R.id.radioenglish)
                    "hi" -> binding.langradiobtn.check(R.id.radioHindi)
                    "ml" -> binding.langradiobtn.check(R.id.radiomalayalam)
                    "ta" -> binding.langradiobtn.check(R.id.radiotamil)
                    "te" -> binding.langradiobtn.check(R.id.radiotelugu)
                    "kn" -> binding.langradiobtn.check(R.id.radioKannada)
                }
            }
        }
    }

    private fun setToolbarNavigation() {
        binding.languagetoolbar.setNavigationIcon(R.drawable.backarrow)
        binding.languagetoolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(),R.color.white))
        binding.languagetoolbar.setNavigationOnClickListener {
           findNavController().navigate(R.id.action_editLanguage_to_profile,null,NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.editLanguage,true).build())
        }
    }


    private fun saveLanguageFromRadioBtn(){
        binding.applybtn.setOnClickListener {
            val selectedLang = when(binding.langradiobtn.checkedRadioButtonId){
                R.id.radioenglish -> "en"
                R.id.radiomalayalam -> "ml"
                R.id.radiotamil -> "ta"
                R.id.radiotelugu ->"te"
                R.id.radioKannada ->"kn"
                R.id.radioHindi ->"hi"
                else -> {"en"}
            }

            lifecycleScope.launch {
                grocerlyDataStore.setLanguage(selectedLang)
                requireActivity().recreate()
            }
        }
    }



}