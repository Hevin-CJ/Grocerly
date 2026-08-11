package com.example.grocerly.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.grocerly.R
import com.example.grocerly.model.Product
import com.example.grocerly.utils.ProductCategory
import com.example.grocerly.viewmodel.CustomSearchViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.ui.uievents.CustomSearchUiEvents
import com.example.grocerly.ui.uistate.CustomSearchUiState
import kotlinx.coroutines.flow.collectLatest
import kotlin.text.get

@Composable
fun CustomSearchScreen(
    productCategory: ProductCategory? = null,
    viewModel: CustomSearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is CustomSearchUiEvents.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    Log.d("eventsissue", event.message)
                }
            }
        }
    }

    val initialQuery = remember(productCategory) {
        if (productCategory != null && productCategory != ProductCategory.selectcatgory) {
            productCategory.displayName
        } else {
            ""
        }
    }

    LaunchedEffect(productCategory) {
        if (productCategory != null && productCategory != ProductCategory.selectcatgory) {
            viewModel.searchCategory(productCategory)
        } else {
            viewModel.searchItemsInFirebase("")
        }
    }

    CustomSearchContent(
        uiState = uiState,
        initialQuery = initialQuery,
        onNavigateBack = onNavigateBack,
        onQueryChange = { newQuery ->
            viewModel.searchItemsInFirebase(newQuery)
        },
        onAddToCart = { product ->
            viewModel.addProductIntoCartFirebase(CartProduct(product))
        },
        onToggleFavourite = { product ->
            viewModel.addFavouriteIntoCartFirebase(FavouriteItem("", product))
        }
    )
}

@Composable
fun CustomSearchContent(
    uiState: CustomSearchUiState,
    initialQuery: String = "",
    onNavigateBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleFavourite: (Product) -> Unit
) {
    var searchQuery by remember(initialQuery) { mutableStateOf(initialQuery) }

    val searchItemResult = uiState.searchResults
    val categoryProductsResult = uiState.categoryProducts

    val products = remember(searchQuery, categoryProductsResult, searchItemResult) {
        if (searchQuery.isNotBlank() && searchItemResult.isNotEmpty()) {
            searchItemResult
        } else if (categoryProductsResult.isNotEmpty()) {
            categoryProductsResult
        } else {
            searchItemResult
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            SearchHeaderBar(
                query = searchQuery,
                onQueryChange = { newQuery ->
                    searchQuery = newQuery
                    onQueryChange(newQuery)
                },
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        )
        {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(products) {
                    SearchProductGridItem(
                        product = it,
                        isFavourite = it.productId in uiState.favouriteProductIds,
                        cartQuantity = uiState.cartProductMap[it.productId] ?: 0,
                        onAddToCart = {
                            onAddToCart(it)
                        },
                        onToggleFavourite = {
                            onToggleFavourite(it)
                        }
                    )
                }
            }
        }
    }
}






@Composable
private fun SearchHeaderBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onNavigateBack) {
            Image(
                painter = painterResource(id = R.drawable.backarrow),
                contentDescription = "Back",
                modifier = Modifier.size(22.dp)
            )
        }


        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            placeholder = {
                Text(
                    text = "Search Items (eg:Apple)",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5)
            )
        )
    }
}


@Composable
fun SearchProductGridItem(
    product: Product,
    isFavourite: Boolean,
    cartQuantity: Int,
    onAddToCart: () -> Unit,
    onToggleFavourite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.padding(8.dp)) {

            Icon(
                imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favourite",
                tint = if (isFavourite) Color.Red else Color.Gray,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onToggleFavourite() }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(12.dp))


                AsyncImage(
                    model = product.image,
                    contentDescription = product.itemName,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))


                Text(
                    text = product.itemName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Price and Add Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${product.itemPrice ?: 0}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    // Add Button / Quantity Counter
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                            .clickable { onAddToCart() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cartQuantity > 0) {
                            Text(
                                text = cartQuantity.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomSearchPreview() {
    CustomSearchContent(
        uiState = CustomSearchUiState(
            categoryProducts = listOf(
                Product(productId = "1", itemName = "Apple", itemPrice = 100),
                Product(productId = "2", itemName = "Banana", itemPrice = 50),
                Product(productId = "3", itemName = "Orange", itemPrice = 80)
            )
        ),
        onNavigateBack = {},
        onQueryChange = {},
        onAddToCart = {},
        onToggleFavourite = {}
    )
}
