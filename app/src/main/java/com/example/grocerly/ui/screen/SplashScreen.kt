package com.example.grocerly.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import com.example.grocerly.R
import com.example.grocerly.model.uievents.SplashDestination
import com.example.grocerly.viewmodel.SplashViewModel


@Composable
fun SplashScreen(
    isSkipped: Boolean = false,
    viewmodel: SplashViewModel = hiltViewModel(),
    onNavigationToHome:() -> Unit,
    onNavigationToLogin:() -> Unit
){


    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewmodel._navigationSplash,lifecycleOwner) {
        viewmodel._navigationSplash.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED).collect {destination ->
            when(destination){
                SplashDestination.Home -> {
                    onNavigationToHome()
                }
                SplashDestination.Login -> {
                    onNavigationToLogin()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewmodel.checkAuthState(isSkipped)
    }
    SplashContent()
}

@Composable
fun SplashContent() {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(AnimatedImageDecoder.Factory())
            }
            .build()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center){

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)){
            AsyncImage(
                model = R.drawable.deliverytuck,
                contentDescription = "Grocerly Image",
                imageLoader = imageLoader,
                modifier = Modifier.size(60.dp)
            )
            Image(
                painter = painterResource(R.drawable.grocerly),
                contentDescription = "Grocerly Image",
                 modifier = Modifier.size(120.dp)
                )

        }

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ShowPreview(){
    SplashContent()
}