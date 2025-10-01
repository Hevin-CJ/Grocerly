package com.example.grocerly.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ProductCategory(val displayName:String) : Parcelable {
    selectcatgory("Select a Category"),
    FruitsandVegies("Fruits & Vegies"),
    FrozenFoods("Frozen Foods"),
    BreadandBakery("Bread & Bakery"),
    PersonalCare("Personal Care"),
    Households("House Holds"),
   HealthCare("Health Care"),
    Meat("Meat");

   companion object{
       fun fromString(name: String?): ProductCategory {
           return entries.find { it.displayName == name }
               ?: selectcatgory
       }
   }
}