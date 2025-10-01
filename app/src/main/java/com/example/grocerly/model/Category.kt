package com.example.grocerly.model

import android.os.Parcelable
import com.example.grocerly.utils.ProductCategory
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    @get:PropertyName("id")
    @set:PropertyName("id")
     var id: Int = 0,


    @get:PropertyName("icon")
    @set:PropertyName("icon")
    var imageUrl: String = ""

): Parcelable{
    @get:Exclude
    @set:Exclude
    var category: ProductCategory = ProductCategory.selectcatgory

    @get:PropertyName("title")
    @set:PropertyName("title")
    var categoryTitleForFirebase: String
        get() = category.displayName
        set(value) {
            category = ProductCategory.fromString(value)
        }

}




