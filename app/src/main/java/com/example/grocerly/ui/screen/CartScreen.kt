package com.example.grocerly.ui.screen


import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.grocerly.R
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Product
import com.example.grocerly.ui.uievents.CartUiEvents
import com.example.grocerly.ui.uistate.CartUiState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.QuantityUtils.convertQuantityIntoString
import com.example.grocerly.viewmodel.CartViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CartScreen(
    cartViewModel: CartViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCheckout: () -> Unit
) {
    val context = LocalContext.current
    val cartUiState by cartViewModel.cartUiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        cartViewModel.cartUiEvents.collectLatest { event ->
            when (event) {
                is CartUiEvents.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CartScreenContent(
        cartUiState = cartUiState,
        onNavigateBack = onNavigateBack,
        onNavigateToCheckout = onNavigateToCheckout,
        onUpdateQuantity = { cartViewModel.updateQuantity(it) },
        onDeleteItem = { cartViewModel.deleteCartItem(it) }
    )
}

@Composable
fun CartScreenContent(
    cartUiState: CartUiState,
    onNavigateBack: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onUpdateQuantity: (CartProduct) -> Unit,
    onDeleteItem: (CartProduct) -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CartTopBar(onNavigateBack)
        }, bottomBar = {
            if (cartUiState.cartItems.isNotEmpty()) {
                FreeDeliveryHeaderAndBottomBar(
                    uiState = cartUiState,
                    onCheckoutClick = {
                        if (cartUiState.cartItems.isNotEmpty()) {
                            onNavigateToCheckout()
                        } else {
                            Toast.makeText(context, "Empty Cart", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color.White)) {

            LazyColumn() {
                itemsIndexed(cartUiState.cartItems) { index, item ->
                    CartItemRow(
                        cartProduct = item,
                        onQuantityChange = onUpdateQuantity,
                        onItemDelete = onDeleteItem
                    )
                }
            }

        }
    }
}

@Composable
fun CartTopBar(onNavigateBack: () -> Unit){
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
       Icon(painter = painterResource(R.drawable.cartback),
           contentDescription = null,
           modifier = Modifier.size(20.dp).clickable{
               onNavigateBack()
           }
       )
        Text(text = "Cart", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
        Icon(painter = painterResource(R.drawable.order),
            contentDescription = null,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
fun FreeDeliveryHeaderAndBottomBar(uiState: CartUiState,
                       onCheckoutClick:() -> Unit) {

    val progress = (uiState.totalAmount / uiState.freeDeliveryThreshold).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "free_delivery_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (uiState.isFreeDeliveryEligible) {
                "🎉 Free Delivery Available!"
            } else {
                "You are ₹${uiState.remainingForFreeDelivery.toInt()} away from free delivery"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(6.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = Color(0xFF2E7D32),
            trackColor = Color.LightGray
        )

        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onCheckoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Go to Checkout(Rs. ${uiState.totalAmount.toInt()})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

}

@Composable
fun CartItemRow(
    cartProduct: CartProduct,
    onQuantityChange: (CartProduct) -> Unit,
    onItemDelete: (CartProduct) -> Unit
) {

    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(all = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cartProduct.product.image,
                contentDescription = cartProduct.product.itemName,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartProduct.product.itemName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "₹${cartProduct.product.itemPrice ?: 0}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )

                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = convertQuantityIntoString(cartProduct.product.quantityType),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "₹${cartProduct.product.itemOriginalPrice ?: 0}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF777777),
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Delivery by ${cartProduct.deliveryDate}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (cartProduct.quantity > 1) {
                            onQuantityChange(cartProduct.copy(quantity = cartProduct.quantity - 1))
                        } else {
                            onItemDelete(cartProduct)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    val iconPainter = if (cartProduct.quantity == 1) {
                        rememberVectorPainter(Icons.Default.Delete)
                    } else {
                        painterResource(id = R.drawable.minus_24)
                    }
                    Icon(
                        painter = iconPainter,
                        contentDescription = "Decrease",
                        tint = if (cartProduct.quantity == 1) Color.Red else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = cartProduct.quantity.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = {
                        onQuantityChange(cartProduct.copy(quantity = cartProduct.quantity + 1))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun CartScreenPreview() {
    val dummyCartItems = listOf(
        CartProduct(
            product = Product(
                itemName = "Fresh Red Apples",
                itemPrice = 180,
                image = ""
            ),
            quantity = 2
        ),
        CartProduct(
            product = Product(
                itemName = "Organic Bananas",
                itemPrice = 60,
                image = ""
            ),
            quantity = 1
        )
    )

    val dummyUiState = CartUiState(
        isLoading = false,
        cartItems = dummyCartItems,
        totalAmount = 420f,
        freeDeliveryThreshold = 500f
    )

    CartScreenContent(
        cartUiState = dummyUiState,
        onNavigateBack = {},
        onNavigateToCheckout = {},
        onUpdateQuantity = {},
        onDeleteItem = {}
    )
}
