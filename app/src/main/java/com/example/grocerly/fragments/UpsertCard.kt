package com.example.grocerly.fragments

import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.grocerly.R
import com.example.grocerly.SavedCardListener
import com.example.grocerly.adapters.SavedCardAdaptor
import com.example.grocerly.databinding.FragmentUpsertCardBinding
import com.example.grocerly.model.Card
import com.example.grocerly.model.ExpiryDate
import com.example.grocerly.utils.CardValidation
import com.example.grocerly.utils.CustomDialogue
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.SavedCardsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class UpsertCard : Fragment() {
    private var upsertCard: FragmentUpsertCardBinding?=null
    private val binding get() = upsertCard!!

   private val cardArgs: UpsertCardArgs by navArgs()

    private val savedCardsViewModel: SavedCardsViewModel by activityViewModels()

    private lateinit var loadingDialogue: LoadingDialogue



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        upsertCard = FragmentUpsertCardBinding.inflate(inflater,container,false)
        loadingDialogue = LoadingDialogue(requireContext())
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkInsertOrUpdateData()
        setToolbar()
        setSaveOrUpdateClickListener()
        observeValidationErrors()
        observeSavedCardState()
        observeCardNumberAndSetCardType()
        formatCardNumber(binding.inputCardNumber.editText!!)
        formatExpiryDate(binding.inputExpiry.editText!!)
        observeDeletionState()
    }

    private fun observeDeletionState() {
       lifecycleScope.launch {
           savedCardsViewModel._cardDeleteState.collectLatest {deleteState->
               when(deleteState){
                   is NetworkResult.Error<*> -> {
                       loadingDialogue.dismiss()
                       Toast.makeText(requireContext(),deleteState.message, Toast.LENGTH_SHORT).show()
                       findNavController().navigate(R.id.action_upsertCard_to_savedCards)
                   }
                   is NetworkResult.Loading<*> -> {
                       loadingDialogue.show()
                   }
                   is NetworkResult.Success<*> -> {
                       loadingDialogue.dismiss()
                       CustomDialogue(requireContext())
                           .setMessage(deleteState.data.toString())
                           .setButtonClick("Go To Saved Cards") {
                               findNavController().navigate(R.id.action_upsertCard_to_savedCards)
                           }
                           .show()
                   }
                   is NetworkResult.UnSpecified<*> ->{
                       loadingDialogue.dismiss()
                   }
               }
           }
       }
    }

    private fun formatExpiryDate(ediText: EditText) {

        ediText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private var deletedSlash = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                deletedSlash = count == 1 && after == 0 && s?.getOrNull(start) == '/'
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return

                isFormatting = true

                val input = s.toString().replace("[^\\d]".toRegex(), "").take(6)
                val builder = StringBuilder()

                if (input.length >= 2) {
                    builder.append(input.substring(0, 2))
                    if (input.length > 2) {
                        builder.append('/')
                        builder.append(input.substring(2.coerceAtMost(6)))
                    } else if (!deletedSlash) {
                        builder.append('/')
                    }
                } else {
                    builder.append(input)
                }

                ediText.setText(builder.toString())
                ediText.setSelection(builder.length.coerceAtMost(ediText.text.length))

                isFormatting = false
            }
        })

    }

    private fun formatCardNumber(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher{

            private var isFormatting = false
            private var deletingSpace = false
            private var onTextChangedCalled = false

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                if (s != null && count > 0 && after == 0 && s[start] == ' ') {
                    deletingSpace = true
                } else {
                    deletingSpace = false
                }
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                onTextChangedCalled = true
            }

            override fun afterTextChanged(s: Editable?) {
                if (!onTextChangedCalled || isFormatting || s == null) return

                onTextChangedCalled = false
                isFormatting = true

                val digitsOnly = s.toString().replace(" ", "").take(19)
                val formatted = StringBuilder()

                for (i in digitsOnly.indices) {
                    formatted.append(digitsOnly[i])
                    if ((i + 1) % 4 == 0 && i + 1 != digitsOnly.length) {
                        formatted.append(" ")
                    }
                }


                val currentCursor = editText.selectionStart
                val formattedString = formatted.toString()

                editText.removeTextChangedListener(this)
                editText.setText(formattedString)

                val newCursorPosition = when {
                    deletingSpace -> currentCursor - 1
                    formattedString.length > s.length -> currentCursor + 1
                    else -> currentCursor
                }.coerceAtMost(formattedString.length)

                editText.setSelection(newCursorPosition)
                editText.addTextChangedListener(this)

                isFormatting = false
            }

        })
    }


    private fun observeCardNumberAndSetCardType() {

        binding.inputCardNumber.editText?.addTextChangedListener(object : TextWatcher{

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                setDrawableForCardType()
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                setDrawableForCardType()
            }

            override fun afterTextChanged(s: Editable?) {
                setDrawableForCardType()
            }

        })
    }

    private fun setDrawableForCardType() {
        binding.apply {
            val cardNumber = inputCardNumber.editText?.text.toString().trim()
            Log.d("cardNumber",cardNumber.toString())
            val cleanedNo = cardNumber.replace("\\s".toRegex(),"")

           val drawableRes =  when {
                cleanedNo.matches("^4[0-9]{12}(?:[0-9]{3})?$".toRegex()) -> R.drawable.visa
                cleanedNo.matches("^5[1-5][0-9]{14}$".toRegex()) -> R.drawable.mastercard
                cleanedNo.matches("^3[47][0-9]{13}$".toRegex()) -> R.drawable.americanexp
                cleanedNo.matches("^6(?:011|5[0-9]{2})[0-9]{12}$".toRegex()) -> R.drawable.discover
                else -> R.drawable.rejected
            }

            binding.imgviewcardtype.setImageDrawable(ContextCompat.getDrawable(requireContext(),drawableRes))
        }
    }

    private fun observeSavedCardState() {
      lifecycleScope.launch {
          savedCardsViewModel._cardSavedState.collectLatest { cardState->
              when(cardState){
                  is NetworkResult.Error<*> -> {
                      loadingDialogue.dismiss()
                      Toast.makeText(requireContext(),cardState.message, Toast.LENGTH_SHORT).show()
                      findNavController().navigate(R.id.action_upsertCard_to_savedCards)
                  }
                  is NetworkResult.Loading<*> -> {
                      loadingDialogue.show()
                  }
                  is NetworkResult.Success<*> -> {
                      loadingDialogue.dismiss()
                      findNavController().navigate(R.id.action_upsertCard_to_savedCards,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.upsertCard,true).build())
                  }
                  is NetworkResult.UnSpecified<*> -> {
                      loadingDialogue.dismiss()
                  }
              }
          }
      }
    }

    private fun observeValidationErrors() {
      lifecycleScope.launch {
           savedCardsViewModel._cardValidationState.collectLatest { savedCard ->
               if (savedCard.holderName is CardValidation.Failed){
                   binding.inputHolderName.apply {
                       requestFocus()
                       helperText = savedCard.holderName.message
                   }
               }else{
                  binding.inputHolderName.helperText = null
               }


               if (savedCard.cardNumber is CardValidation.Failed){
                   binding.inputCardNumber.apply {
                       requestFocus()
                       helperText = savedCard.cardNumber.message
                   }
               }else{
                   binding.inputCardNumber.helperText = null
               }

               if (savedCard.expiryDate is CardValidation.Failed){
                   binding.inputExpiry.apply {
                       requestFocus()
                       helperText = savedCard.expiryDate.message
                   }
               }else{
                   binding.inputExpiry.helperText = null
               }


               if (savedCard.cvv is CardValidation.Failed){
                   binding.inputCvv.apply {
                       requestFocus()
                       helperText = savedCard.cvv.message
                   }
               }else{
                   binding.inputCvv.helperText = null
               }
           }
       }
    }

    private fun setSaveOrUpdateClickListener() {
        binding.saveorupdatecardbtn.setOnClickListener {
            if (cardArgs.isUpdate){
                cardArgs.savedCard?.let {
                    updateCard(it)
                }

            }else{
                insertCard()
            }
        }
    }

    private fun setToolbar() {
        binding.saveorupdatetoolbar.apply {
            setNavigationIcon(R.drawable.backarrow)
            navigationIcon?.setTint(ContextCompat.getColor(requireContext(),R.color.black))
            setNavigationOnClickListener {
                findNavController().navigate(R.id.action_upsertCard_to_savedCards,null, NavOptions.Builder().setLaunchSingleTop(true).setPopUpTo(R.id.savedCards,true).build())
            }
        }
    }

    private fun checkInsertOrUpdateData() {
        if (cardArgs.isUpdate){
            binding.saveorupdatecardbtn.text = getString(R.string.update_changes)
            binding.saveorupdatetoolbar.setTitle("Edit Card")
            loadCardData()
            setDeleteOnToolbar()
        }else{
            binding.saveorupdatetoolbar.setTitle("Add Debit/Credit Cards")
        }
    }

    private fun setDeleteOnToolbar() {
       val deleteBtn =  binding.saveorupdatetoolbar.menu.findItem(R.id.deletecardbtn)
        binding.saveorupdatetoolbar.menu.forEach {
            val title = SpannableString(it.title)
            title.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(),R.color.red)), 0, title.length, 0)
            it.title = title
        }
        deleteBtn.isVisible = true

        binding.saveorupdatetoolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.deletecardbtn ->{
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Card")
                        .setMessage("Are you sure delete the card ?")
                        .setPositiveButton("Delete"){dialogue,_ ->
                           cardArgs.savedCard?.let {
                               savedCardsViewModel.deleteCardFromFirebase(it)
                           }
                            dialogue.dismiss()
                        }
                        .setNegativeButton("Cancel"){dialogue,_ ->
                            dialogue.dismiss()
                        }.setBackground(ContextCompat.getDrawable(requireContext(),R.drawable.alert_dialogue_bg)).show()

                    true
                }
                else -> false
            }

        }
    }

    private fun loadCardData() {
        val card = cardArgs.savedCard
        binding.apply {
            inputHolderName.editText?.setText(card?.holderName)
            inputCardNumber.editText?.setText(card?.cardNumber.toString())
            inputExpiry.editText?.setText(buildString {
                append(String.format(Locale.US,"%02d",card!!.expiryDate.month))
                append("/")
                append(card.expiryDate.year.toString())
            })
            inputCvv.editText?.setText(card?.cvv.toString())
        }
        setDrawableForCardType()
    }



    private fun insertCard() {
        binding.apply {
            val holderName = inputHolderName.editText?.text.toString().trim().uppercase()
            val cardNumber = inputCardNumber.editText?.text.toString().trim()
            val expiryDate = extractMonthAndYear(inputExpiry.editText?.text.toString().trim())
            val cvv = inputCvv.editText?.text.toString().trim()
            val cardType = getCardType(cardNumber)

            Log.d("expirydatelog",expiryDate.toString())

            val cardDetails = Card("",holderName,cardNumber,expiryDate,cvv,cardType)
            savedCardsViewModel.saveCardDetails(cardDetails)
        }
    }

    private fun updateCard(card: Card) {
        binding.apply {

            val holderName = inputHolderName.editText?.text.toString().trim().uppercase()
            val cardNumber = inputCardNumber.editText?.text.toString().trim()
            val expiryDate = extractMonthAndYear(inputExpiry.editText?.text.toString().trim())
            val cvv = inputCvv.editText?.text.toString().trim()
            val cardType = getCardType(cardNumber)

            val cardDetails = Card(card.cardId,holderName,cardNumber,expiryDate,cvv,cardType)
            savedCardsViewModel.updateCardDetails(cardDetails)
        }
    }

    private fun getCardType(number: String): String {
        if (number.isEmpty()) return ""
        val cleaned = number.replace("\\s".toRegex(), "")
        return when {
            cleaned.matches("^4[0-9]{12}(?:[0-9]{3})?$".toRegex()) -> "Visa"
            cleaned.matches("^5[1-5][0-9]{14}$".toRegex()) -> "MasterCard"
            cleaned.matches("^3[47][0-9]{13}$".toRegex()) -> "American Express"
            cleaned.matches("^6(?:011|5[0-9]{2})[0-9]{12}$".toRegex()) -> "Discover"
            else -> "Unknown"
        }
    }


   private fun extractMonthAndYear(input: String): ExpiryDate {
        val cleaned = input.trim().replace("\\s".toRegex(), "")

        val regex = Regex("^(\\d{2})/?(\\d{4})$")
        val match = regex.find(cleaned)

        if (match != null) {
            val (monthStr, yearStr) = match.destructured
            val month = monthStr.toIntOrNull()
            val year = yearStr.toIntOrNull()

            if (month == null || year == null || month !in 1..12) return ExpiryDate(0,0)

            return ExpiryDate(month, year)
        }else{
            return ExpiryDate(0,0)
        }


    }


}
