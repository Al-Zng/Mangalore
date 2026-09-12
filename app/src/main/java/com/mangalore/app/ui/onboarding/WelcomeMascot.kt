package com.mangalore.app.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mangalore.app.ui.theme.ZTheme

@Composable
fun WelcomeMascot(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "mascot")
    val bob by infinite.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val sparkle by infinite.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sparkle"
    )

    Box(modifier = modifier.size(160.dp), contentAlignment = Alignment.Center) {
        val positions = listOf(
            -70f to -60f, 65f to -40f, -55f to 35f, 72f to 20f, 0f to -85f
        )
        val sizes = listOf(10, 14, 8, 12, 9)
        positions.forEachIndexed { i, (x, y) ->
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = ZTheme.accentBright.copy(alpha = sparkle),
                modifier = Modifier
                    .size(sizes[i].dp)
                    .offset(x = x.dp, y = y.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .offset(y = bob.dp)
                    .size(72.dp)
                    .background(ZTheme.goldGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.offset(y = (-6).dp)) {
                    Box(modifier = Modifier.size(width = 6.dp, height = 8.dp).background(ZTheme.bg, RoundedCornerShape(3.dp)))
                    Box(modifier = Modifier.size(width = 6.dp, height = 8.dp).background(ZTheme.bg, RoundedCornerShape(3.dp)))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .offset(y = (bob / 2).dp)
                    .size(width = 46.dp, height = 34.dp)
                    .background(ZTheme.card, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = ZTheme.accentBright, modifier = Modifier.size(14.dp))
            }
        }
    }
}
