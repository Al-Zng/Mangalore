package com.mangalore.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.mangalore.app.data.Manga
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.theme.ZTheme

/** Loads a manga cover with the Referer/User-Agent headers the source site
 * requires, and shows a spinner/placeholder while loading — the Android
 * equivalent of the iOS `CachedAsyncImage`. */
@Composable
fun MangaCoverImage(url: String?, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) {
        Box(modifier = modifier.background(ZTheme.card), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Book, contentDescription = null, tint = ZTheme.textTertiary)
        }
        return
    }
    val request = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
        .data(url)
        .addHeader("Referer", "https://lekmanga.site")
        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36")
        .crossfade(true)
        .build()

    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(ZTheme.card)
    ) {
        val state = painter.state
        if (state is coil.compose.AsyncImagePainter.State.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZTheme.accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        } else if (state is coil.compose.AsyncImagePainter.State.Error) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Book, contentDescription = null, tint = ZTheme.textTertiary)
            }
        } else {
            SubcomposeAsyncImageContent()
        }
    }
}

@Composable
fun DrawerButton() {
    val nav = LocalNavCoordinator.current
    IconButton(onClick = { nav.openDrawer() }) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = ZTheme.textPrimary)
    }
}

/** Grid card used on Home / Search / New Releases / Library: cover, title,
 * rating + chapter row underneath. */
@Composable
fun MangaGridCard(manga: Manga, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        MangaCoverImage(
            url = manga.highQualityCoverURL,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.2f)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            manga.title, color = ZTheme.textPrimary, style = MaterialTheme.typography.bodyMedium,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            if (manga.rating.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = ZTheme.accentBright, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(manga.rating, color = ZTheme.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Spacer(modifier = Modifier)
            }
            manga.latestChapterNumber?.let {
                Text("Ch. $it", color = ZTheme.textTertiary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = ZTheme.textTertiary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(message, color = ZTheme.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ZTheme.accent)
    }
}
