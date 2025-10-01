package com.example.grocerly.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.grocerly.R
import com.example.grocerly.databinding.FragmentEditProfileBinding
import com.example.grocerly.model.Account
import com.example.grocerly.utils.AccountResult
import com.example.grocerly.utils.LoadingDialogue
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.databinding.ChangeEmailLayoutBinding
import com.example.grocerly.utils.Mappers.toAccount

@AndroidEntryPoint
class EditProfile : Fragment() {
    private var editProfile: FragmentEditProfileBinding?=null
    private val binding get() = editProfile!!

    private val profileViewModel by activityViewModels<ProfileViewModel>()

    private lateinit var loadingDialogue: LoadingDialogue

   private var changeEmailDialogue: Dialog? = null

   private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){ uri->
       uri?.let {
           profileViewModel.uploadProfileImage(it)
       }
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.fetchUserDetailsFromFirebase()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       editProfile = FragmentEditProfileBinding.inflate(inflater,container,false)
        (requireActivity() as MainActivity).setTabLayoutVisibility(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialogue = LoadingDialogue(requireContext())

        observeProfileDetails()
        observeUpdateProfileDetails()
        observeProfileValidationErrors()
        actionToSelectImageFromStorage()
        observeUploadedImage()
        changeUserEmailState()
        observeLogoutState()
        setToolbar()
    }

    private fun setToolbar() {
        binding.toolbarprofile.apply {
            setNavigationIcon(R.drawable.backarrow)
            setNavigationIconTint(ContextCompat.getColor(requireContext(),R.color.black))
            setTitle("Profile")
            setTitleTextColor(ContextCompat.getColor(requireContext(),R.color.black))
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun observeLogoutState() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.logoutState.collectLatest {
                when(it){
                    is NetworkResult.Error<*> ->{
                        Toast.makeText(requireContext(),it.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading<*> ->{

                    }
                    is NetworkResult.Success<*> -> {
                        actionToLogout()
                    }
                    is NetworkResult.UnSpecified<*> -> {

                    }
                }
            }
        }
    }

    private fun actionToLogout() {
        lifecycleScope.launch {
            (requireActivity() as MainActivity).setNavigationGraph()
        }
    }

    private fun changeUserEmailState() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.changeUserEmailState.collectLatest { email->
                when(email){
                    is NetworkResult.Error<*> -> {
                        Toast.makeText(requireContext(),email.message, Toast.LENGTH_LONG).show()
                        loadingDialogue.dismiss()
                    }
                    is NetworkResult.Loading<*> -> {
                        loadingDialogue.show()
                    }
                    is NetworkResult.Success<*> -> {
                        changeEmailDialogue?.dismiss()
                        Toast.makeText(requireContext(),"Changed email to ${email.data}", Toast.LENGTH_SHORT).show()
                        profileViewModel.LogOutUserFromFirebase()
                    }
                    is NetworkResult.UnSpecified<*> -> {
                        loadingDialogue.dismiss()
                    }
                }
            }
        }
    }

    private fun observeUploadedImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.uploadImageState.collectLatest { state ->

                when(state){
                    is NetworkResult.Error<*> -> {
                        showProgress(false)
                        Toast.makeText(requireContext(),state.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading<*> -> {
                        showProgress(true)
                    }
                    is NetworkResult.Success<*> ->{

                        state.data?.let {
                            loadPickedImage(it)
                            profileViewModel.setSelectedImage(it)
                        }
                        showProgress(state.data.isNullOrEmpty())

                    }
                    is NetworkResult.UnSpecified<*> -> {
                        showProgress(false)
                    }
                }
            }
        }
    }


    fun showProgress(istrue: Boolean){
        if (istrue){
            binding.imageuploadprogressbar.visibility = View.VISIBLE
            binding.updatebtn.visibility = View.INVISIBLE
        }else{
            binding.imageuploadprogressbar.visibility = View.INVISIBLE
            binding.updatebtn.visibility = View.VISIBLE
        }
    }

    private fun loadPickedImage(url: String) {
        Glide.with(requireContext())
            .load(url)
            .priority(Priority.HIGH)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.imgviewprofilepicture)

    }

    private fun actionToSelectImageFromStorage() {
        binding.apply {
            imgviewprofilepicture.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        }
    }

    private fun observeProfileValidationErrors() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.profileValidation.collectLatest { state ->

                binding.txtinputlayoutfirstname.apply {
                    if (state.firstname is RegisterValidation.Failed) {
                        requestFocus()
                        helperText = state.firstname.message
                    }
                    else{
                        helperText = null
                    }
                }

                binding.txtinputlayoutlastname.apply {
                if (state.lastname is RegisterValidation.Failed){

                        requestFocus()
                        helperText = state.lastname.message

                }else{
                    helperText = null
                }
                }


                binding.txtinputlayoutemail.apply {
                if (state.email is RegisterValidation.Failed){

                        requestFocus()
                        helperText = state.email.message

                }else{
                   helperText =null
                }
                }


                binding.txtinputphonenumber.apply {
                if (state.phoneNo is RegisterValidation.Failed){

                        requestFocus()
                        helperText = state.phoneNo.message

                }else{
                    helperText =null
                }
                }
            }
        }
    }

    private fun observeEmailChangeValidationErrors(binding: ChangeEmailLayoutBinding) {

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.emailChangeValidation.collectLatest { state ->

                binding.txtinputlayoutemail.apply {
                    if (state.email is RegisterValidation.Failed) {
                        requestFocus()
                        helperText = state.email.message
                    }
                    else{
                        helperText = null
                    }
                }

                binding.txtinputlayoutpassword.apply {
                    if (state.password is RegisterValidation.Failed){

                        requestFocus()
                        helperText = state.password.message

                    }else{
                        helperText = null
                    }
                }


                binding.txtinputlayoutnewemail.apply {
                    if (state.newEmail is RegisterValidation.Failed){

                        requestFocus()
                        helperText = state.newEmail.message

                    }else{
                        helperText =null
                    }
                }


            }
        }
    }

    private fun observeUpdateProfileDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.updatedDetails.collectLatest { updated->
                when(updated){
                    is AccountResult.Error<*> -> {
                        Toast.makeText(requireContext(),updated.message, Toast.LENGTH_SHORT).show()
                        loadingDialogue.dismiss()
                    }
                    is AccountResult.Loading<*> -> {
                        loadingDialogue.show()
                    }
                    is AccountResult.Success<*> -> {
                        Toast.makeText(requireContext(),updated.data.toString(), Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_editProfile_to_profile)
                        loadingDialogue.dismiss()
                    }
                    is AccountResult.UnSpecified<*> -> {
                        loadingDialogue.dismiss()
                    }

                    is AccountResult.EmailUpdated<*> -> {
                        loadingDialogue.dismiss()
                        updated.data?.let {
                            showAlertDialogue(it)
                        }

                    }
                }
            }
        }
    }

    private fun showAlertDialogue(email: String) {
        val binding = ChangeEmailLayoutBinding.inflate(LayoutInflater.from(requireContext()))

        changeEmailDialogue = Dialog(requireContext()).apply {
            setContentView(binding.root)

             binding.edttxtnewemail.setText(email)
            observeEmailChangeValidationErrors(binding)

            window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            binding.apply {
                btncancel.setOnClickListener {
                    changeEmailDialogue?.cancel()
                }

                btnsend.setOnClickListener {

                    val oldEmail = edttxtemail.text.toString().trim()
                    val password = edttxtpassword.text.toString().trim()
                    val newEmail = edttxtnewemail.text.toString().trim()

                    profileViewModel.changeUserEmailOfUser(oldEmail,newEmail,password)

                }

            }
        }



        changeEmailDialogue?.show()
    }

    private fun updateProfileDetails(account: Account) {
        binding.apply {
            updatebtn.setOnClickListener {

                val firstname = edttxtname.text.toString().trim()
                val lastname = edttxtlastname.text.toString().trim()
                val email = edttxtemail.text.toString().trim()
                val phoneNo = edttxtphoneno.text.toString().trim()
                val countryCode = countryCodeHolder.selectedCountryCodeWithPlus
                 val imageUrl = profileViewModel.selectedString.value.toString()

                val updatedAccount = Account(account.userId,firstname,lastname,email,imageUrl,countryCode,phoneNo)

                Log.d("btnclicked",updatedAccount.phoneNumber.toString())
                profileViewModel.updateUserDetailsToFirebase(updatedAccount)
            }
        }
    }

    private fun observeProfileDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.getProfileData.collectLatest { details->
                details?.let {
                    setProfileDetails(details.toAccount())
                    updateProfileDetails(details.toAccount())
                }
            }
        }
    }

    private fun setProfileDetails(account: Account) {
        binding.apply {
            edttxtname.setText(account.firstName)
            edttxtlastname.setText(account.lastName)
            edttxtemail.setText(account.email)
            edttxtphoneno.setText(account.phoneNumber)
            Glide.with(requireContext()).load(account.imageUrl).placeholder(R.drawable.profileimg).into(imgviewprofilepicture)
            profileViewModel.setSelectedImage(account.imageUrl)
            val countryCode = account.countryCode.toIntOrNull() ?: 91
            countryCodeHolder.setCountryForPhoneCode(countryCode)
        }
    }


}


