package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.model.CancellationInfo
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order
import com.example.grocerly.utils.CancellationStatus
import com.example.grocerly.utils.CancelledBy
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.PARTNERS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.OrderStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@ActivityRetainedScoped
  class OrderRepoImpl @Inject constructor(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {

    private val userId = auth.currentUser?.uid.toString()

    private val orderRef = db.collection(USERS).document(userId).collection(ORDERS)

    fun getOrdersFromFirebase(): Flow<NetworkResult<List<Order>>> = callbackFlow {
        trySend(NetworkResult.Loading())
       val listener =  orderRef.addSnapshotListener { snapshot,exception ->

            if (exception!=null){
               trySend( NetworkResult.Error(exception.message ?: "Unable to fetch Order, Please try later..."))
                return@addSnapshotListener
            }

           if(snapshot==null ||snapshot.isEmpty){
               trySend(NetworkResult.Success(emptyList()))
               return@addSnapshotListener
           }


               val order = snapshot.documents.mapNotNull { it.toObject(Order::class.java) }.sortedByDescending { it.items.maxOfOrNull { it.orderedTime } }
               Log.d("userdata",order.toString())
               trySend(NetworkResult.Success(order))

        }
        awaitClose {
            listener.remove()
        }

    }


    fun getOrderStatus(cartProduct: CartProduct,order: Order): Flow<NetworkResult<CartProduct>> = callbackFlow {
      val listener =  orderRef.document(order.orderId).addSnapshotListener { snapshot,exception->

          if (exception!=null){
              trySend(NetworkResult.Error(exception.toString()))
              return@addSnapshotListener
          }

          if (snapshot ==null|| !snapshot.exists()){
              return@addSnapshotListener
          }

          val updatedOrder = snapshot.toObject(Order::class.java)

          updatedOrder?.let {
              val matchedOrder = updatedOrder.items.find { it.product.productId == cartProduct.product.productId }

              if (matchedOrder!=null){
                  trySend(NetworkResult.Success(matchedOrder))
              }else{
                  trySend(NetworkResult.Error("Unable to find Cart Product"))
              }
          }
        }

        awaitClose {
            listener.remove()
        }
    }

     internal suspend fun setOrderStateInOrder(cartProduct: CartProduct, order: Order, status: OrderStatus): NetworkResult<Unit> {
        return try {

            val batch = db.batch()
            val globalSnap = db.collection(ORDERS).document(order.orderId).get().await()

            val userRef = orderRef.document(order.orderId)
            val partnerRef = db.collection(PARTNERS).document(cartProduct.product.partnerId).collection(ORDERS).document(order.orderId)
            val fullOrder = globalSnap.toObject(Order::class.java)
            val globalRef = db.collection(ORDERS).document(order.orderId)

            if (fullOrder == null) {
                return NetworkResult.Error("Order not found")
            }


            val updatedItems = fullOrder.items.map {
                if (it.product.partnerId == cartProduct.product.partnerId && it.product.productId == cartProduct.product.productId && it.cancellationInfo.cancellationStatus != CancellationStatus.Cancelled) {
                   if (status == OrderStatus.DELIVERED){
                       it.copy(orderStatus = status, deliveredDate = System.currentTimeMillis())
                   }else{
                       it.copy(orderStatus = status)
                   }
                } else {
                    it
                }
            }

            val updatedGlobalOrder = fullOrder.copy(items = updatedItems)
            //updating global reference
            batch.set(globalRef,updatedGlobalOrder.toMap(),SetOptions.merge())
            //user reference
            batch.set(userRef,updatedGlobalOrder.toMap(),SetOptions.merge())
            //partner reference
            val sellerOrder = fullOrder.copy(items = updatedItems.filter { it.product.partnerId == cartProduct.product.partnerId })
            batch.set(partnerRef,sellerOrder.toMap(), SetOptions.merge())


            batch.commit().await()

            NetworkResult.Success(Unit)
        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }

    suspend fun deleteItemFromOrders(cartProduct: CartProduct,orders: Order,reason: String): NetworkResult<Unit>{
        return try {

            val batch = db.batch()

            val cancellationInfo = CancellationInfo(
                cancellationStatus = CancellationStatus.Cancelled,
                cancelledAt = System.currentTimeMillis(),
                cancelledBy = CancelledBy.USER,
                reason = reason
            )
            Log.d("cancellationgot",cartProduct.cancellationInfo.cancellationStatus.toString())

            val updatedItems = orders.items.map {
                if (it.product.productId == cartProduct.product.productId && it.orderStatus != OrderStatus.SHIPPED&& it.orderStatus != OrderStatus.DELIVERED&& it.orderStatus != OrderStatus.OUTFORDELIVERY && it.cancellationInfo.cancellationStatus != CancellationStatus.Cancelled){
                    it.copy(
                        cancellationInfo = cancellationInfo
                    )
                }else{
                    it
                }
            }
            val updatedOrder = orders.copy(items = updatedItems)

            val userRef = orderRef.document(orders.orderId)
            val globalRef = db.collection(ORDERS).document(orders.orderId)
            val partnerRef = db.collection(PARTNERS).document(cartProduct.product.partnerId).collection(ORDERS).document(orders.orderId)


            batch.set(userRef,updatedOrder.toMap(), SetOptions.merge())
            batch.set(globalRef,updatedOrder.toMap(), SetOptions.merge())

            val sellerOrder = updatedOrder.copy(items = updatedItems.filter { it.product.partnerId == cartProduct.product.partnerId })
            batch.set(partnerRef,sellerOrder.toMap(), SetOptions.merge())

            batch.commit().await()
            NetworkResult.Success(Unit)
        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }
    fun Order.toMap(): Map<String, Any> {
        return mapOf(
            "orderId" to orderId,
            "userId" to userId,
            "address" to address,
            "items" to items.map { it.toMap() },
            "timestamp" to timestamp,
            "totalOrderPrice" to totalOrderPrice,
            "paymentType" to paymentType
        )
    }

    fun CartProduct.toMap(): Map<String, Any?> {
        return mapOf(
            "product" to product,
            "quantity" to quantity,
            "orderedTime" to orderedTime,
            "deliveryDate" to deliveryDate,
            "deliveredDate" to deliveredDate,
            "orderStatus" to  orderStatus,
            "cancellationInfo" to cancellationInfo.toMap()
        )
    }

    fun CancellationInfo.toMap(): Map<String, Any?> {
        return mapOf(
            "cancellationStatus" to cancellationStatus.name,
            "cancelledBy" to cancelledBy.name,
            "cancelledAt" to cancelledAt,
            "reason" to reason
        )
    }
}