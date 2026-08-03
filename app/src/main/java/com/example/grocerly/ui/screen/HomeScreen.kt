package com.example.grocerly.ui.screen

import android.Manifest
import android.os.Build
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.toColorInt
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
import com.example.grocerly.utils.QuantityType
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

    var showAddressSheet by remember { mutableStateOf(value = false) }

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
                    onCategoryClick = { onNavigateToCategory(uiState.products[0])},
                    onAddOfferToCart = {productId,partnerId ->
                        homeViewModel.addOfferToCart(productId,partnerId)
                    },
                    onAddToCart = {homeViewModel.addProductToCart(it)},
                    onAddToFav = {homeViewModel.addProductToFavourites(it)},
                    onAddToWishlist = { homeViewModel.addProductToWishlist(it) }
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
            .background(Color.White)
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

                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    thickness = 1.dp,
                    color = Color(0xFFE0E0E0) // @color/light_grey
                )
            }
        }


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

        Row(modifier = Modifier.background(color = Color.White).fillMaxWidth(0.7f).clickable(interactionSource = remember{ MutableInteractionSource()}, indication = null){ onAddressClick()}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painter = painterResource(id = R.drawable.scooter), contentDescription = null, modifier = Modifier.size(40.dp).align(Alignment.CenterVertically))

            Text(text = addressText.ifEmpty { "Select Address" },
                modifier = Modifier.weight(1f, fill = false).padding(start = 2.dp),
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
        Row( modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween , verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = parentCategory.categoryName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "See all",
                color = Color(0xFF2E7D32),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = parentCategory.childCategoryItems) { childProduct ->
                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .wrapContentHeight()
                        .padding(vertical = 4.dp)

                    ,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF4D4D4D))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                       Card( modifier = Modifier.width(110.dp).height(100.dp)
                           , shape = RoundedCornerShape(8.dp)
                       ) {
                           AsyncImage(
                               modifier = Modifier.fillMaxSize(),
                               model = childProduct.image,
                               contentDescription = childProduct.itemName,
                               contentScale = ContentScale.Crop
                           )
                       }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = childProduct.itemName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Image(
                                modifier = Modifier.size(15.dp),
                                painter = painterResource(id = R.drawable.star),
                                contentDescription = "Product Rating"
                            )

                            Text(
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                                text = "${childProduct.itemRating.toString()} (${childProduct.totalRating.toString()})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "₹${childProduct.itemPrice ?: 0}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = convertQuantityIntoString(childProduct.quantityType),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = "₹${childProduct.itemOriginalPrice ?: 0}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                maxLines = 1 ,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = TextDecoration.LineThrough
                            )

                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(colors = CardDefaults.cardColors(Color(0xFFF3F3F3))) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = {
                                            onAddToCart(CartProduct(product = childProduct, quantity = 1))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.favourites),
                                            contentDescription = "Add to Cart",
                                            modifier = Modifier.size(18.dp),
                                            tint  = if (childProduct.isFavourite) Color.Red else Color.Gray
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = {
                                            onAddToCart(CartProduct(product = childProduct, quantity = 1))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            painter = if(childProduct.isFavourite) painterResource(id = R.drawable.wishlist) else painterResource(id = R.drawable.wishlist_done),
                                            contentDescription = "Add to Cart",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.Unspecified
                                        )
                                    }
                                }
                            }


                            IconButton(
                                onClick = {
                                    onAddToCart(CartProduct(product = childProduct, quantity = 1))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    painter = if (childProduct.isInCart) painterResource(R.drawable.checkcircleadded) else painterResource(R.drawable.carthome) ,
                                    contentDescription = "Add to Cart",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Unspecified
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
    offers: List<OfferItem>,
    onAddOfferToCartClick: (String, String) -> Unit
) {
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

    Column {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.White),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(offers) { offer ->
                val offerBgColor = remember(offer.offerBgColor) {
                    try {
                        Color(offer.offerBgColor.toColorInt())
                    } catch (e: Exception) {
                        Color.White
                    }
                }
                val btnBgColor = remember(offer.buttonBgColor) {
                    try {
                        Color(offer.buttonBgColor.toColorInt())
                    } catch (e: Exception) {
                        Color(0xFF4CAF50)
                    }
                }
                val btnTxtColor = remember(offer.buttonTxtColor) {
                    try {
                        Color(offer.buttonTxtColor.toColorInt())
                    } catch (e: Exception) {
                        Color.White
                    }
                }
                val descTextColor = remember(offer.descriptionTextColor) {
                    try {
                        Color(offer.descriptionTextColor.toColorInt())
                    } catch (e: Exception) {
                        Color.Black
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(350.dp)
                        .clickable {
                            val productId = offer.productId
                            val partnerId = offer.partnerId
                            onAddOfferToCartClick(productId, partnerId)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = offerBgColor)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxSize(),
                            model = offer.offerImage,
                            contentDescription = "Offer Banner",
                            contentScale = ContentScale.FillBounds
                        )
                        Column(
                            modifier = Modifier
                                .weight(.9f)
                                .fillMaxHeight()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = offer.descriptionText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = descTextColor,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    onAddOfferToCartClick(offer.productId, offer.partnerId)
                                },
                                colors = buttonColors(
                                    containerColor = btnBgColor,
                                    contentColor = btnTxtColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = offer.buttonText.ifEmpty { "Shop Now" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (offers.size > 1) {
            PageIndicator(
                pageCount = offers.size,
                currentPage = activeIndex.value,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            )
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
        horizontalArrangement = Arrangement.spacedBy(6.dp, alignment = Alignment.CenterHorizontally),
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
        Card(  modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))) {

            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.categoryTitleForFirebase,
                contentScale = ContentScale.FillBounds
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.categoryTitleForFirebase,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun convertQuantityIntoString(quantityType: QuantityType): String{
   return when(quantityType){
        QuantityType.selectQuantity -> "/ Quantity"
        QuantityType.perKilogram -> "/ Kg"
        QuantityType.perLiter -> "/ L"
        QuantityType.perPiece -> "/ Piece"
        QuantityType.perPacket -> "/ Packet"
    }

}


@Preview(showBackground = true, name = "Home Screen - Content Loaded")
@Composable
fun HomeScreenPreview() {
    // Mock Data for Previewing UI
    val mockOffers = listOf(
        OfferItem(
            productId = "1",
            partnerId = "p1",
            offerImage = "",
            descriptionText = "Get 50% Off on Fresh Fruits!",
            descriptionTextColor = "#000000",
            buttonText = "Shop Now",
            buttonBgColor = "#4CAF50",
            buttonTxtColor = "#FFFFFF",
            offerBgColor = "#E8F5E9"
        ),
        OfferItem(
            productId = "2",
            partnerId = "p2",
            offerImage = "",
            descriptionText = "Free Delivery on Orders above ₹500",
            descriptionTextColor = "#000000",
            buttonText = "Order Now",
            buttonBgColor = "#2E7D32",
            buttonTxtColor = "#FFFFFF",
            offerBgColor = "#C8E6C9"
        )
    )

    val mockCategories = listOf(
        Category(id = 1, imageUrl = "").apply { category = ProductCategory.FruitsandVegies },
        Category(id = 2, imageUrl = "").apply { category = ProductCategory.FrozenFoods },
        Category(id = 3, imageUrl = "").apply { category = ProductCategory.BreadandBakery },
        Category(id = 4, imageUrl = "").apply { category = ProductCategory.HealthCare }
    )

    val mockChildProducts = listOf(
        Product(itemName = "Fresh Tomato", itemPrice = 40000, image = ""),
        Product(itemName = "Organic Potato", itemPrice = 30, image = ""),
        Product(itemName = "Green Capsicum", itemPrice = 50, image = "")
    )

    val mockParentCategories = listOf(
        ParentCategoryItem(categoryName = "Daily Essentials", childCategoryItems = mockChildProducts),
        ParentCategoryItem(categoryName = "Fresh Produce", childCategoryItems = mockChildProducts)
    )

    MaterialTheme {
        HomeContent(
            addressText = "61 Hooper Street, Ramnagar",
            cartBadgeCount = 3,
            localOffers = mockOffers,
            categories = mockCategories,
            parentCategories = mockParentCategories,
            onAddressClick = {},
            onCartClick = {},
            onSeeAllClick = {},
            onCategoryClick = {},
            onAddOfferToCart = { _, _ -> },
            onAddToCart = {},
            onAddToFav = {},
            onAddToWishlist = {}
        )
    }
}



