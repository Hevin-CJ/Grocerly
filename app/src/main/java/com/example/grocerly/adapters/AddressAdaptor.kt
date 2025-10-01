package com.example.grocerly.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.grocerly.R
import com.example.grocerly.databinding.SavedaddressRclayoutBinding
import com.example.grocerly.model.Address
import com.example.grocerly.onAddressMenuClickListener
import com.google.api.Context
import java.util.Locale

class AddressAdaptor(private val listener: onAddressMenuClickListener):RecyclerView.Adapter<AddressAdaptor.AddressViewHolder>(){


    private val diffUtil = object :DiffUtil.ItemCallback<Address>(){
        override fun areItemsTheSame(
            oldItem: Address,
            newItem: Address
        ): Boolean {
            return oldItem.addressId == newItem.addressId
        }

        override fun areContentsTheSame(
            oldItem: Address,
            newItem: Address
        ): Boolean {
           return oldItem == newItem
        }

    }

    private val asyncDiffer = AsyncListDiffer(this,diffUtil)


    inner class AddressViewHolder(private val binding: SavedaddressRclayoutBinding):ViewHolder(binding.root){

        fun setAddress(address: Address){
            binding.apply {
                val firstName = address.firstName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }

                toolbarsavedaddress.setTitle(firstName)
                toolbarsavedaddress.menu.clear()
                toolbarsavedaddress.inflateMenu(R.menu.address_toolbar)

                val setDefaultItem = toolbarsavedaddress.menu.findItem(R.id.setDefault)
                setDefaultItem?.isVisible = address.default == false


                toolbarsavedaddress.setOnMenuItemClickListener { menuItem->

                    when(menuItem.itemId){
                        R.id.deleteaddress -> {
                            listener.onDeleteClicked(address)
                            true
                        }

                        R.id.editaddress -> {
                            listener.onEditClicked(address)
                          true
                        }


                        R.id.setDefault -> {
                            listener.onsetDefaultClicked(address)
                            true
                        }

                        else -> false
                    }


                }

                txtviewfulladdress.text = buildString { appendLine(formatSegment(address.deliveryAddress))
                    append(address.city.uppercase(Locale.getDefault()))
                    append(" , ")
                    append(address.state.uppercase(Locale.getDefault()))
                    append(" , ")
                    appendLine(address.pinCode.uppercase(Locale.getDefault()))
                    append(address.landMark)
                }
                txtviewphoneno.text = buildString { append( address.phoneNumber)
                    append(" , ")
                    append( address.alternateNumber)
                }

                txtviewdefault.visibility = if (address.default == true) View.VISIBLE else View.INVISIBLE

            }
        }

        fun formatSegment(text: String): String {
            //fix here

           return text.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }

        }



    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddressViewHolder {
        return AddressViewHolder(SavedaddressRclayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: AddressViewHolder,
        position: Int
    ) {
        val address = asyncDiffer.currentList[position]
        holder.setAddress(address)

    }

    override fun getItemCount(): Int {
        return asyncDiffer.currentList.size
    }


    fun setAddress(address: List<Address>){
        asyncDiffer.submitList(address)
    }


}