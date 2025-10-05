package com.example.grocerly.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grocerly.R
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.adapters.ChatAdaptor
import com.example.grocerly.databinding.FragmentHelpCenterBinding
import com.example.grocerly.model.ChatMessageModel
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.HelpCenterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HelpCenter : Fragment() {
  private var helpCenter: FragmentHelpCenterBinding?=null
    private val binding get() = helpCenter!!

    private val helpCenterViewModel: HelpCenterViewModel by viewModels()

    private val chatAdaptor by lazy { ChatAdaptor() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       helpCenter = FragmentHelpCenterBinding.inflate(inflater,container,false)
        (requireActivity() as MainActivity).setTabLayoutVisibility(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setChatRcView()
        setUpChatSystem()
        observeChatSystemData()
        setHelpCenterToolbar()
        startTypingSuggestionAnimation()
    }

    private fun startTypingSuggestionAnimation() {
        val suggestions = listOf(
            "Hello , I am your Grocerly AI Assistant\uD83D\uDE0A",
            "How can I help you today?",
            "I can give you the best choices for groceries\uD83D\uDED2",
        )

        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                for (suggestion in suggestions) {
                    typeText(binding.txtviewlivetext, suggestion)
                    delay(1500)

                    eraseText(binding.txtviewlivetext)
                    delay(500)
                }
            }
        }
    }

    private suspend fun typeText(textView: TextView, text: String) {
        text.forEach { char ->
            textView.append(char.toString())
            delay(100)
        }
        textView.append("|")
    }

    private suspend fun eraseText(textView: TextView) {
        val currentText = textView.text.toString()
        currentText.forEach { _ ->
            if (textView.text.isNotEmpty()) {
                textView.text = textView.text.subSequence(0, textView.text.length - 1)
                delay(50)
            }
        }
    }

    private fun setHelpCenterToolbar() {
        binding.toolbarhelpcenter.apply {
            title = "Help Center"
            setNavigationIcon(R.drawable.backarrow)
            setNavigationIconTint(ContextCompat.getColor(requireContext(),R.color.black))
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun setChatRcView() {
        binding.rcviewchat.apply {
            adapter = chatAdaptor
            layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
        }
    }

    private fun observeChatSystemData() {
        viewLifecycleOwner.lifecycleScope.launch {
            helpCenterViewModel.uiState.collectLatest { state ->
                chatAdaptor.setMessages(state.messages)
                setLiveTextVisibility(state.messages)

                if (state.messages.isNotEmpty()) {
                    binding.rcviewchat.smoothScrollToPosition(state.messages.size - 1)
                }
            }
        }
    }

    private fun setLiveTextVisibility(messages: List<ChatMessageModel>) {
        if (messages.isEmpty()){
            binding.txtviewlivetext.visibility = View.VISIBLE
            binding.imgiviewgrocerly.visibility = View.VISIBLE
        }else{
            binding.txtviewlivetext.visibility = View.GONE
            binding.imgiviewgrocerly.visibility = View.GONE
        }
    }

    private fun setUpChatSystem() {
        binding.apply {
            sendbtn.setOnClickListener {
                if (edttxtchat.toString().isNotBlank() && !edttxtchat.text.isNullOrEmpty()){
                    val question = edttxtchat.text.toString().trim()
                    helpCenterViewModel.sendMessage(question)
                    edttxtchat.text?.clear()
                }else{
                    txtinputlayoutchat.helperText = "query cannot be empty"
                }
            }
        }
    }


}