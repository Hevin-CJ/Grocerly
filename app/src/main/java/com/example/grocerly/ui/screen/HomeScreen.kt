package com.example.grocerly.ui.screen

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.grocerly.R
import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Category
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.OfferItem
import com.example.grocerly.model.Order
import com.example.grocerly.model.ParentCategoryItem
import com.example.grocerly.model.Product
import com.example.grocerly.model.WishItem
import com.example.grocerly.model.uievents.HomeUiEvents
import com.example.grocerly.utils.ProductCategory
import com.example.grocerly.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlin.collections.emptyList
import kotlin.collections.isNotEmpty


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onNavigateToCart: () -> Unit,
    onNavigateToSeeAll: (ProductCategory) -> Unit,
    onNavigateToCategory: (ParentCategoryItem) -> Unit,
    onNavigateToAddAddress: () -> Unit,
    onNavigateToUpdateAddress: (Address) -> Unit,
    onActionToOrderDetails: (CartProduct, Order) -> Unit,
    notificationOrderId:String?=null,
    notificationProductId:String?=null
){
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    var showAddressSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) {}

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }else{
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(Unit) {
        homeViewModel.uiEvents.collect{events ->
            when(events){
                is HomeUiEvents.ActionToOrderDetails -> {
                    onActionToOrderDetails(events.cartProduct,events.order)
                }
                is HomeUiEvents.ShowMessage -> {
                    Toast.makeText(context, events.message, Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    LaunchedEffect(notificationOrderId,notificationProductId) {
        if (!notificationProductId.isNullOrEmpty() && !notificationOrderId.isNullOrEmpty()){
            homeViewModel.fetchOrderForNotification(notificationOrderId,notificationProductId)
        }
    }

    val initialFeedLoad = uiState.isLoading && uiState.products.isEmpty() && !uiState.isRefreshing

    Box(modifier = Modifier.fillMaxSize()){
        if (initialFeedLoad){
            HomeShimmerLoading()
        }else{
            PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = { homeViewModel.refreshHomeData() }, modifier = Modifier.fillMaxSize()){
                HomeContent(
                    addressText = uiState.homeAddress,
                    cartBadgeCount = uiState.cartItems.size,
                    localOffers = uiState.localOffers,
                    categories = uiState.categoryItems,
                    parentCategories = uiState.products,
                    onAddressClick = { showAddressSheet = true },
                    onCartClick = { onNavigateToCart() },
                    onSeeAllClick = {onNavigateToSeeAll(ProductCategory.selectcatgory) },
                    onCategoryClick = { onNavigateToCategory(uiState.products.get(0))},
                    onAddOfferToCart = {productId,partnerId ->
                        homeViewModel.addOfferToCart(productId,partnerId)
                    },
                    onAddToCart = {homeViewModel.addProductToCart(it)},
                    onAddToFav = {homeViewModel.addProductToFavourites(it)},
                    onAddToWishlist = {homeViewModel.addProductToWishlist(it)}
                )
            }
        }
    }


    if (uiState.isActionLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}



@Composable
private fun HomeContent(
    addressText: String,
    cartBadgeCount: Int,
    localOffers: List<OfferItem>,
    categories: List<Category>,
    parentCategories: List<ParentCategoryItem>,
    onAddressClick: () -> Unit,
    onCartClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onCategoryClick: (ProductCategory) -> Unit,
    onAddOfferToCart: (String, String) -> Unit,
    onAddToCart: (CartProduct) -> Unit,
    onAddToFav: (FavouriteItem) -> Unit,
    onAddToWishlist: (WishItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp), // Mirrors guideline21 (10dp) and guideline22 (10dp)
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- 1. Address Toolbar (@id/addresstoolbar) ---
        item {
            AddressToolbar(
                addressText = addressText,
                cartBadgeCount = cartBadgeCount,
                onAddressClick = onAddressClick,
                onCartClick = onCartClick
            )
        }

        // --- 2. Offers Banner (@id/rcpageoffers) ---
        if (localOffers.isNotEmpty()) {
            item {
                OfferAutoScrollBanner(
                    offers = localOffers,
                    onAddOfferToCartClick = onAddOfferToCart
                )
            }
        }

        // --- 3. Categories Header (@id/linearLayout2) & Categories (@id/rcviewCategory) ---
        if (categories.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "See all",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32), // @color/green
                        modifier = Modifier.clickable { onSeeAllClick() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Replaces @id/rcviewCategory (height = 100.dp)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { category ->
                        CategoryItemCell(category)
                    }
                }

                // Replaces @id/view6 (1dp Divider)
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    thickness = 1.dp,
                    color = Color(0xFFE0E0E0) // @color/light_grey
                )
            }
        }

        // --- 4. Nested Product Feed (@id/nestedrcview) ---
        items(parentCategories) { parentCategory ->
            ParentCategorySection(
                parentCategory = parentCategory,
                onAddToCart = onAddToCart
            )
        }
    }
}
@Composable
fun AddressToolbar(
    addressText: String,
    cartBadgeCount: Int,
    onAddressClick: () -> Unit,
    onCartClick: () -> Unit
) {

    Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

        Row(modifier = Modifier.weight(1f,fill = false).background(color = Color.White).clickable(interactionSource = remember{ MutableInteractionSource()}, indication = null){ onAddressClick()}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painter = painterResource(id = R.drawable.scooter), contentDescription = null, modifier = Modifier.size(40.dp).align(Alignment.CenterVertically))

            Text(text = addressText.ifEmpty { "Select Address" },
                modifier = Modifier.padding(start = 2.dp),
                textAlign = TextAlign.Center, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Image(
                painter = painterResource(id = R.drawable.arrow)
                ,contentDescription = "Change Address",
                modifier = Modifier.size(25.dp)
            )

        }

        BadgedBox(
            badge = {
                if (cartBadgeCount > 0) {
                    Badge(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ) {
                        Text(text = cartBadgeCount.toString())
                    }
                }
            }
        ) {
            IconButton(onClick = onCartClick) {
                Image(
                    painter = painterResource(id = R.drawable.mage_basket),
                    contentDescription = "Cart",
                    modifier = Modifier.size(35.dp)
                )
            }
        }
    }
}

@Composable
private fun ParentCategorySection(
    parentCategory: ParentCategoryItem,
    onAddToCart: (CartProduct) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = parentCategory.categoryName ?: "Products",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = parentCategory.childCategoryItems) { childProduct ->
                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = childProduct.image,
                            contentDescription = childProduct.itemName,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = childProduct.itemName ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹${childProduct.itemPrice}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    onAddToCart(
                                        CartProduct(
                                            product = childProduct,
                                            quantity = 1
                                        )
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Add to Cart",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfferAutoScrollBanner(
    offers:List<OfferItem>,
    onAddOfferToCartClick:(String,String) -> Unit
){
    val listState = rememberLazyListState()

    val activeIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(offers) {
        if (offers.size > 1) {
            while (true) {
                delay(3000L)

                if (!listState.isScrollInProgress) {
                    val nextItem = (listState.firstVisibleItemIndex + 1) % offers.size
                    listState.animateScrollToItem(nextItem)
                }
            }
        }

    }

    Column(){
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(offers){offer ->
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .clickable {
                            val productId = offer.productId ?: ""
                            val partnerId = offer.partnerId ?: ""
                            onAddOfferToCartClick(productId, partnerId)
                        },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AsyncImage(
                        model = offer.offerImage,
                        contentDescription = "Offer Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

        }

        if (offers.size>1){
            PageIndicator(offers.size,activeIndex.value, modifier = Modifier.padding(top = 8.dp))
        }
    }

}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage

            val width by animateDpAsState(
                targetValue = if (isSelected) 18.dp else 8.dp,
                label = "indicator_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF4CAF50) else Color.LightGray,
                label = "indicator_color"
            )

            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}


@Composable
private fun HomeShimmerLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun CategoryItemCell(category: Category) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        AsyncImage(
            model = category.imageUrl,
            contentDescription = category.categoryTitleForFirebase,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.categoryTitleForFirebase ?: "",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ShowHomePreview(){
    AddressToolbar("Ramnagar,Kollam",5,{},{})
}


