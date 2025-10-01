package com.example.grocerly.utils

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.example.grocerly.databinding.CustomActivityDialogueBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class CustomDialogue(context: Context) {

    private var dialogue: AlertDialog?=null
    private val binding: CustomActivityDialogueBinding = CustomActivityDialogueBinding.inflate(LayoutInflater.from(context))


    init {
        val builder = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(true)

         dialogue =  builder.create()
    }


    fun setMessage(text: String): CustomDialogue{
        binding.txtviewcustommesg.text = text
        return this
    }


    fun setButtonClick(title: String ="Ok",onClick:() -> Unit): CustomDialogue{
        binding.actionbtn.apply {
            text = title
            setOnClickListener {
                onClick()
                dialogue?.dismiss()
            }
        }
        return this
    }

    fun show(){
        dialogue?.show()
    }

    fun dismiss(){
        dialogue?.dismiss()
    }

}