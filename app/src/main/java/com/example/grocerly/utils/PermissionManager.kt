package com.example.grocerly.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.grocerly.activity.MainActivity
import com.loukwn.stagestepbar.c

class PermissionManager(
    private val context: Fragment
,private val onResult:(Map<String, Boolean>) -> Unit) {

    private  var permissionLauncher: ActivityResultLauncher<Array<String>>

    init {
        permissionLauncher = registerLauncher()
    }


    private fun registerLauncher(): ActivityResultLauncher<Array<String>>{
        return context.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permissions->
            onResult(permissions)
        }
    }

    private fun getPermissionStatus(permissions: Array<String>):Map<String,Boolean>{
        return permissions.associateWith { permission->
            context.requireActivity().checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions(permissions:Array<String>){
        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission( context.requireActivity(),permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()){
            onResult(permissions.associateWith { true })
        }else{
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context.requireActivity(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }else{
            onResult(mapOf(Manifest.permission.POST_NOTIFICATIONS to true))
        }
    }



}