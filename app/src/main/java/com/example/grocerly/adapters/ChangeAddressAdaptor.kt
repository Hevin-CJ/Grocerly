package com.example.grocerly.adapters

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerly.R
import com.example.grocerly.databinding.ChangeaddressLayoutBinding
import com.example.grocerly.interfaces.AddressActionListener
import com.example.grocerly.model.Address

class ChangeAddressAdaptor(private val listener: AddressActionListener): RecyclerView.Adapter<ChangeAddressAdaptor.ChangeAddressViewHolder>() {

    private var addresses: List<Address> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ChangeAddressViewHolder {
       return ChangeAddressViewHolder(ChangeaddressLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: ChangeAddressViewHolder,
        position: Int
    ) {
        val currentAddress = addresses[position]
        holder.setAddress(currentAddress)
    }

    override fun getItemCount(): Int {
        return addresses.size
    }

    inner class ChangeAddressViewHolder(private val changeAddressLayoutBinding: ChangeaddressLayoutBinding): RecyclerView.ViewHolder(changeAddressLayoutBinding.root){

        fun setAddress(address: Address){
            changeAddressLayoutBinding.apply {
                txtviewUserName.text = address.firstName
                txtviewshortAddress.text = address.deliveryAddress
                imgviewdefault.visibility = if (address.default) View.VISIBLE else View.INVISIBLE

                setToDefaultLayout.setOnClickListener {
                    listener.onClickLayoutToMakeDefault(address)
                }

                imgviewmnu.setOnClickListener {

                    val popMenu = PopupMenu(
                        ContextThemeWrapper(
                            changeAddressLayoutBinding.root.context,
                            R.style.CustomPopupMenuStyle
                        ),
                        changeAddressLayoutBinding.imgviewmnu, android.view.Gravity.NO_GRAVITY, 0, R.style.CustomPopupMenuStyle)
                    popMenu.menuInflater.inflate(R.menu.change_address_bottom_menu,popMenu.menu)

                    popMenu.setOnMenuItemClickListener {
                        when(it.itemId){
                            R.id.action_edit ->{
                                listener.onEditRequested(address)
                                true
                            }

                            R.id.action_delete ->{
                                listener.onDeleteRequested(address)
                                true
                            }

                            else -> false
                        }

                    }
                    popMenu.show()
                }
            }
        }
    }

    fun setAddresses(address: List<Address>){
        this.addresses = address
        notifyDataSetChanged()
    }

}