package com.example.grocerly.Repository.remote

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.grocerly.model.Address
import com.example.grocerly.model.LocationInfo
import com.example.grocerly.utils.Constants.ADDRESS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SavedAddressRepoImpl @Inject constructor(private val auth: FirebaseAuth,private val db: FirebaseFirestore) {

    private val userId = auth.currentUser?.uid.toString()

    private val addressRef  = db.collection(USERS).document(userId).collection(ADDRESS)


    suspend  fun saveAddressToFirebase(address: Address): NetworkResult<String>{
      return try {


          if (userId.isEmpty()){
            return NetworkResult.Error("User Not Found")
          }


          val defaultAddressSnapshot = addressRef.get().await()

          val isFirstAddress = defaultAddressSnapshot.isEmpty
          val docRef = addressRef.document()

          val addressWithId = address.copy(addressId = docRef.id, default = if (isFirstAddress) true else address.default)
          docRef.set(addressWithId).await()

          NetworkResult.Success(if (isFirstAddress) "Set as Default" else "")

      }catch (e: Exception){
          NetworkResult.Error(e.message)
      }
    }


    fun getAllAddressFromDb(): Flow<NetworkResult<List<Address>>> = callbackFlow {

          val listener = addressRef.addSnapshotListener { snapshot , exception ->

              if (exception!=null){
                  trySend(NetworkResult.Error(exception.message  ?: "Unable to fetch address")).isFailure
                  return@addSnapshotListener
              }

              snapshot?.let {
                  val address = snapshot.documents.mapNotNull { it.toObject(Address::class.java) }
                  trySend(NetworkResult.Success(address)).isSuccess
              }
          }

          awaitClose{
              listener.remove()
          }
    }


    suspend fun setAsDefaultAddressInDb(address: Address): NetworkResult<String> {
        return try {
            if (userId.isEmpty()) return NetworkResult.Error("User Not Found")

            val currentDefaults = addressRef
                .whereEqualTo("default", true)
                .get()
                .await()

            db.runTransaction { transaction ->

                for (doc in currentDefaults.documents) {
                    val updateMap = hashMapOf<String, Any>(
                        "default" to false
                    )

                    transaction.update(doc.reference, updateMap)
                }

                val newDefaultRef = addressRef.document(address.addressId)

                val newDefaultMap = hashMapOf<String, Any>(
                    "default" to true
                )
                transaction.update(newDefaultRef,newDefaultMap)


            }.await()

            NetworkResult.Success("${address.firstName} is set to default" )
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error occurred,Please try later")
        }
    }


    suspend fun deleteAddressFromFirebase(address: Address): NetworkResult<String>{
        return try {

                addressRef.document(address.addressId).delete().await()

                val remainingAddress = addressRef.get().await().documents

                if (remainingAddress.isNotEmpty()){

                    val hasDefaultSet = remainingAddress.any{ it.getBoolean("default") == true}

                    if (!hasDefaultSet){
                        val lastAddress =  remainingAddress.last()
                        lastAddress.reference.update("default",true).await()
                    }

                }
                NetworkResult.Success("Address Deleted")


        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }


    suspend fun updateAddressInFirebase(address: Address): NetworkResult<String>{
        return try {
            if (userId.isEmpty()) return  NetworkResult.Error("User Not Found")

            val documentRef = addressRef.document(address.addressId)

            db.runTransaction { transition ->

                val snapshot = transition.get(documentRef)
                if (snapshot.exists()) {
                    transition.set(documentRef,address)
                }


            }.await()

            NetworkResult.Success("Updated Address")
        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }




    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationForClient(context:Context): NetworkResult<LocationInfo>{
       return try {
           val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
           val location = fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()

           if (location==null){
               return NetworkResult.Error("Unable to retrieve location")
           }

           if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){
               suspendCoroutine { cont->

                   val geocoder = Geocoder(context)

                   geocoder.getFromLocation(location.latitude,location.longitude,1
                   ) { addresses ->
                       val address = addresses.firstOrNull()
                       if (addresses.isNotEmpty()) {
                           val result = LocationInfo(
                               city = address?.locality,
                               state = address?.adminArea,
                               pinCode = address?.postalCode,

                               )

                           cont.resume(NetworkResult.Success(result))
                           Log.d("addrssgot", result.toString())
                       } else {
                           cont.resume(NetworkResult.Error("No Address Found"))
                       }
                   }

               }
           }else{

               val geocoder = Geocoder(context)
               val addresses = geocoder.getFromLocation(location.latitude, location.longitude,1)

               if (!addresses.isNullOrEmpty()) {
                   val address = addresses[0]
                   val result = LocationInfo(
                       city = address.locality,
                       state = address.adminArea,
                       pinCode = address.postalCode
                   )
                   NetworkResult.Success(result)
               } else {
                   NetworkResult.Error("No address found")
               }
           }
       }catch (e: Exception){
           NetworkResult.Error(e.message ?: "Unexpected error")
       }

    }




     fun getDefaultAddressFromDb(): Flow<NetworkResult<Address?>> = callbackFlow{

         val listener = addressRef.addSnapshotListener { snapshot, exception ->
             if (exception != null) {
                 trySend(
                     NetworkResult.Error(
                         exception.message ?: "Something went wrong,Please try later.."
                     )
                 )
             }

             if (snapshot != null && !snapshot.isEmpty) {
                 val address = snapshot.documents
                     .mapNotNull { it.toObject(Address::class.java) }
                     .firstOrNull { it.default }
                 trySend(NetworkResult.Success(address))
             }else{
                 trySend(NetworkResult.Success(null))
             }


         }
         awaitClose {
             listener.remove()
         }

     }
}