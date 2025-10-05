    package com.example.grocerly.adapters

    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import androidx.recyclerview.widget.AsyncListDiffer
    import androidx.recyclerview.widget.DiffUtil
    import androidx.recyclerview.widget.RecyclerView
    import com.example.grocerly.databinding.RcchatlayoutBinding
    import com.example.grocerly.model.ChatMessageModel

    class ChatAdaptor: RecyclerView.Adapter<ChatAdaptor.ChatViewHolder>() {

        private val diffUtil = object :DiffUtil.ItemCallback<ChatMessageModel>(){
            override fun areItemsTheSame(
                oldItem: ChatMessageModel,
                newItem: ChatMessageModel
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: ChatMessageModel,
                newItem: ChatMessageModel
            ): Boolean {
               return oldItem == newItem
            }

        }

        private val asyncDiffer = AsyncListDiffer(this, diffUtil)


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ChatViewHolder {
            return ChatViewHolder(RcchatlayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        }

        override fun onBindViewHolder(
            holder: ChatViewHolder,
            position: Int
        ) {
            val currentMessage = asyncDiffer.currentList[position]
            holder.bindMessage(currentMessage)
        }

        override fun getItemCount(): Int {
            return asyncDiffer.currentList.size
        }

        inner class ChatViewHolder(private val binding: RcchatlayoutBinding): RecyclerView.ViewHolder(binding.root){

            fun bindMessage(message: ChatMessageModel){
                binding.apply {
                    materialcardviewuser.visibility = View.GONE
                    materialcardviewmodel.visibility = View.GONE
                    gifImageViewloading.visibility = View.GONE

                    when(message.role){
                        "user" -> {
                            materialcardviewuser.visibility = View.VISIBLE
                            txtviewuser.text = message.message
                        }
                        "model" -> {
                            if (message.message.isBlank() || message.message.isNullOrEmpty()){
                                Log.d("chatadaptor",message.message.toString())

                                gifImageViewloading.visibility = View.VISIBLE
                                materialcardviewmodel.visibility = View.GONE
                            } else {
                                Log.d("chatadaptor",message.message.toString())

                                materialcardviewmodel.visibility = View.VISIBLE
                                gifImageViewloading.visibility = View.GONE
                                txtviewmodel.text = message.message
                            }
                        }

                    }
                }
            }
        }

        fun setMessages(messages: List<ChatMessageModel>){
            asyncDiffer.submitList(messages)
        }


    }