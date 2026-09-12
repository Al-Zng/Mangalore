package com.mangalore.app.ui.onboarding

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mangalore.app.R
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(onFinished: () -> Unit) {
    var appear by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (appear) 1f else 0f, animationSpec = tween(600), label = "alpha")

    LaunchedEffect(Unit) {
        appear = true
        delay(1800)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackground()
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WelcomeMascot(modifier = Modifier.graphicsLayer { this.alpha = alpha })
            Spacer(modifier = Modifier.height(22.dp))
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Mangalore",
                modifier = Modifier.height(60.dp).graphicsLayer { this.alpha = alpha }
            )
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(color = ZTheme.accent, modifier = Modifier.graphicsLayer { this.alpha = alpha })
        }
    }
}
