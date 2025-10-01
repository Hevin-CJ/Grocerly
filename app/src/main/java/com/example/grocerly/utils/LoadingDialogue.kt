package com.example.grocerly.utils


import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.UiThread
import com.example.grocerly.R
import com.example.grocerly.databinding.CustomLoadingScreenBinding

import kotlin.apply


class LoadingDialogue(private val context: Context) {

    private var dialogue:Dialog ?= null
    private var binding: CustomLoadingScreenBinding?=null

    @UiThread
    fun show(){
        if (isShowing()) return

        binding = CustomLoadingScreenBinding.inflate(LayoutInflater.from(context))
        dialogue = Dialog(context).apply {

           requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(binding!!.root)

            window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
        dialogue?.show()
    }

    fun isShowing(): Boolean {
        return dialogue?.isShowing == true
    }

    @UiThread
    fun setText(msg: String = "Loading , Please wait...."){
        binding?.txtviewloading?.text = msg
    }

    @UiThread
    fun dismiss(){
        dialogue?.dismiss()
        binding = null
        dialogue = null
    }

}