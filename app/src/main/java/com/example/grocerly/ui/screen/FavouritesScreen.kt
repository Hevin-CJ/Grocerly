package com.example.grocerly.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.grocerly.R
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.viewmodel.CartViewModel
import com.example.grocerly.viewmodel.FavouriteViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FavouritesScreen(
    favouriteViewModel: FavouriteViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val favouritesState by favouriteViewModel.favouritesList.collectAsState(initial = NetworkResult.UnSpecified())

    LaunchedEffect(Unit) {
        favouriteViewModel.getAllFavouritesFromFirebase()
    }

    LaunchedEffect(Unit) {
        favouriteViewModel.deletedFavourite.collectLatest { result ->
            if (result is NetworkResult.Error) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            } else if (result is NetworkResult.Success) {
                Toast.makeText(context, "Removed from favourites", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Favourites",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (val result = favouritesState) {
            is NetworkResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            }
            is NetworkResult.Success -> {
                val favourites = result.data ?: emptyList()
                if (favourites.isEmpty()) {
                    EmptyFavouritesContent()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favourites) { item ->
                            FavouriteItemRow(
                                item = item,
                                onDeleteClick = { favouriteViewModel.deleteFavouriteFromFirebase(item) },
                                onAddToCartClick = { cartViewModel.addProductIntoCartFirebase(CartProduct(item.product, 1)) }
                            )
                        }
                    }
                }
            }
            is NetworkResult.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = result.message ?: "An error occurred", color = Color.Red)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun FavouriteItemRow(
    item: FavouriteItem,
    onDeleteClick: () -> Unit,
    onAddToCartClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.product.image,
                contentDescription = item.product.itemName,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.itemName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "₹${item.product.itemPrice}",
                    fontSize = 16.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.recycle_bin),
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onAddToCartClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.carthome),
                        contentDescription = "Add to Cart",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFavouritesContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.favourites),
            contentDescription = "No Favourites",
            modifier = Modifier.size(120.dp),
            alpha = 0.5f
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Favourites yet",
            fontSize = 18.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}
