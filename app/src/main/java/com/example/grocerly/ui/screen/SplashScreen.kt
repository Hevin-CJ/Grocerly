package com.example.grocerly.ui.screen

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import coil3.DrawableImage
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import com.example.grocerly.R
import com.example.grocerly.ui.uievents.SplashDestination
import com.example.grocerly.viewmodel.SplashViewModel
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(
    isSkipped: Boolean = false,
    viewmodel: SplashViewModel = hiltViewModel(),
    onNavigationToHome:() -> Unit,
    onNavigationToLogin:() -> Unit
){


    val isLoggedIn by viewmodel.isLoggedIn.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(isLoggedIn,isSkipped) {
       if (isSkipped){
           onNavigationToLogin()
           return@LaunchedEffect
       }

        isLoggedIn?.let { loggedIn ->
            delay(3000L)

            if (loggedIn) {
                onNavigationToHome()
            } else {
                onNavigationToLogin()
            }
        }
    }
    val context = LocalContext.current

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }


    val imageRequest = remember(context) {
        ImageRequest.Builder(context)
            .data(R.drawable.deliverytuck)
            .build()
    }

    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        imageLoader = imageLoader,
        onSuccess = { state ->
            val drawable = (state.result.image as? DrawableImage)?.drawable
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && drawable is AnimatedImageDrawable) {
                drawable.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE)
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center){

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)){
            Image(
                painter = painter,
                contentDescription = "Grocerly Image",
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

@Composable
fun SplashContent() {
    val context = LocalContext.current

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }


    val imageRequest = remember(context) {
        ImageRequest.Builder(context)
            .data(R.drawable.deliverytuck)
            .build()
    }

    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        imageLoader = imageLoader,
        onSuccess = { state ->
            val drawable = (state.result.image as? DrawableImage)?.drawable
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && drawable is AnimatedImageDrawable) {
                drawable.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE)
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center){

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)){
            Image(
                painter = painter,
                contentDescription = "Grocerly Image",
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