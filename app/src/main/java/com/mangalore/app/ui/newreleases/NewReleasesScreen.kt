package com.mangalore.app.ui.newreleases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mangalore.app.data.Manga
import com.mangalore.app.data.MangaService
import com.mangalore.app.ui.common.DrawerButton
import com.mangalore.app.ui.common.LoadingSpinner
import com.mangalore.app.ui.common.MangaGridCard
import com.mangalore.app.ui.nav.LocalNavCoordinator
import com.mangalore.app.ui.nav.Routes
import com.mangalore.app.ui.theme.ZTheme
import kotlinx.coroutines.launch

@Composable
fun NewReleasesScreen() {
    val context = LocalContext.current
    val nav = LocalNavCoordinator.current
    val service = remember { MangaService.getInstance(context) }
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }

    fun mergeUnique(existing: List<Manga>, incoming: List<Manga>): List<Manga> {
        val seen = existing.map { it.slug }.toMutableSet()
        val merged = existing.toMutableList()
        for (m in incoming) if (seen.add(m.slug)) merged.add(m)
        return merged
    }

    LaunchedEffect(Unit) {
        try { items = service.fetchNewReleases(1) } catch (e: Exception) { }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(ZTheme.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ZTheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerButton()
            Text("New Releases", color = ZTheme.textPrimary, style = MaterialTheme.typography.titleMedium)
        }

        if (isLoading && items.isEmpty()) {
            LoadingSpinner()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items.forEachIndexed { index, manga ->
                    item(key = manga.slug) {
                        MangaGridCard(manga = manga, onClick = { nav.go(Routes.detail(manga.slug)) })
                        if (index == items.size - 4 && !loadingMore && !ended) {
                            LaunchedEffect(manga.slug) {
                                loadingMore = true
                                val next = page + 1
                                try {
                                    val more = service.fetchNewReleases(next)
                                    page = next
                                    val before = items.size
                                    items = mergeUnique(items, more)
                                    if (more.isEmpty() || items.size == before) ended = true
                                } catch (e: Exception) { }
                                loadingMore = false
                            }
                        }
                    }
                }
                if (loadingMore) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ZTheme.accent, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
